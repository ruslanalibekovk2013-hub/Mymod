package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** NERO Chat: отдельный сетевой чат с моделью про текущий проект (история хранится в проекте). Не то же самое, что поле Build Request. */
public class NeroChatScreen extends Screen {
    private static final int ROWS = 16;
    private static volatile boolean streaming = false;
    private static final StringBuilder BUF = new StringBuilder();
    private static volatile String streamOwner = "";

    private TextFieldWidget field;
    private int ox, oy, scroll = 0;
    private final List<Object[]> lines = new ArrayList<>(); // {OrderedText, Integer цвет}
    private int lastCount = -1;

    public NeroChatScreen() { super(Text.literal("NERO Chat")); }

    private static String chatId() { return NeroAI.project != null && !NeroAI.project.projectId.isEmpty() ? NeroAI.project.projectId : "_general"; }

    @Override
    protected void init() {
        ox = width / 2 - 190;
        oy = Math.max(4, (height - 214) / 2);
        field = new TextFieldWidget(textRenderer, ox, oy + 166, 372, 18, Text.literal("сообщение"));
        field.setMaxLength(1000);
        field.setSuggestion("Спроси про проект или попроси изменить компонент");
        field.setChangedListener(t -> field.setSuggestion(t.isEmpty() ? "Спроси про проект или попроси изменить компонент" : ""));
        addDrawableChild(field);
        setInitialFocus(field);
        addDrawableChild(ButtonWidget.builder(Text.literal("Отправить"), b -> send()).dimensions(ox, oy + 188, 70, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Стоп"), b -> stop()).dimensions(ox + 74, oy + 188, 44, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Отчёт"), b -> report()).dimensions(ox + 122, oy + 188, 50, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Проекты"), b -> MinecraftClient.getInstance().setScreen(new NeroProjectsScreen())).dimensions(ox + 176, oy + 188, 60, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Настройки"), b -> MinecraftClient.getInstance().setScreen(new NeroSettingsScreen())).dimensions(ox + 240, oy + 188, 62, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> MinecraftClient.getInstance().setScreen(new NeroScreen())).dimensions(ox + 306, oy + 188, 66, 18).build());
        rebuild();
    }

    private void rebuild() {
        lines.clear();
        List<NeroProjects.Msg> msgs = new ArrayList<>(NeroProjects.chat(chatId()));
        if (msgs.isEmpty()) msgs.add(new NeroProjects.Msg("system", "Здесь можно обсуждать проект с подключённой моделью: спрашивать, как он устроен, и просить изменить отдельный компонент. Новый проект создаётся в поле запроса на главном экране NERO.", ""));
        for (NeroProjects.Msg m : msgs) addMsg(m.role(), m.text());
        if (streaming && chatId().equals(streamOwner)) addMsg("assistant", BUF.length() == 0 ? "…" : BUF.toString());
        lastCount = lines.size();
    }

    private void addMsg(String role, String text) {
        String prefix = switch (role) { case "user" -> "Вы: "; case "assistant" -> "NERO: "; case "report" -> "Отчёт: "; default -> "• "; };
        int color = switch (role) { case "user" -> 0x55FFFF; case "assistant" -> 0x88FF88; case "report" -> 0xFFD055; default -> 0xAAAAAA; };
        for (String para : (prefix + text).split("\n"))
            for (OrderedText o : textRenderer.wrapLines(Text.literal(para), 366)) lines.add(new Object[]{o, color});
    }

    private void scrollBy(int d) { scroll = Math.max(0, Math.min(Math.max(0, lines.size() - ROWS), scroll + d)); }

    public boolean mouseScrolled(double mx, double my, double amount) { scrollBy(amount > 0 ? -3 : 3); return true; }

    public boolean mouseScrolled(double mx, double my, double h, double v) { scrollBy(v > 0 ? -3 : 3); return true; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && field.isFocused()) { send(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (streaming || lines.size() != lastCount) { rebuild(); scroll = Math.max(0, lines.size() - ROWS); }
    }

    private void stop() {
        if (streaming) { NeroChatClient.cancel(); }
    }

    private void report() {
        if (NeroAI.project == null) { NeroProjects.appendChat(chatId(), "system", "Проект не открыт."); rebuild(); return; }
        if (NeroScreen.plan() == null) NeroScreen.replan(false);
        NeroBuildPlan pl = NeroScreen.plan();
        List<String> rep = NeroReport.make(NeroAI.project, pl, NeroScreen.report());
        NeroProjects.appendChat(chatId(), "report", String.join("\n", rep));
        rebuild();
        scroll = Math.max(0, lines.size() - ROWS);
    }

    private void send() {
        String text = field.getText().trim();
        if (text.isEmpty() || streaming) return;
        String id = chatId();
        field.setText("");
        NeroProjects.appendChat(id, "user", text);
        if (!NeroConfig.remoteReady()) {
            NeroProjects.appendChat(id, "system", "Чат работает через подключённую модель, сейчас она не подключена. Вставь ключ OpenRouter в «Настройки AI» (или адрес своего сервера). Ответы не имитируются.");
            rebuild();
            return;
        }
        JsonArray msgs = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", NeroChatClient.systemPrompt(NeroAI.project, NeroScreen.plan(), NeroScreen.report(), text));
        msgs.add(sys);
        List<NeroProjects.Msg> hist = NeroProjects.chat(id);
        int from = Math.max(0, hist.size() - 20);
        for (int i = from; i < hist.size(); i++) {
            NeroProjects.Msg m = hist.get(i);
            if (!m.role().equals("user") && !m.role().equals("assistant")) continue;
            JsonObject o = new JsonObject();
            o.addProperty("role", m.role());
            o.addProperty("content", m.text());
            msgs.add(o);
        }
        BUF.setLength(0);
        streaming = true;
        streamOwner = id;
        long t0 = System.nanoTime();
        MinecraftClient mc = MinecraftClient.getInstance();
        NeroChatClient.stream(msgs, d -> BUF.append(d), err -> mc.execute(() -> finish(id, err, t0)));
        rebuild();
    }

    private static Map<String, String> partHashes(NeroSpec s) {
        Map<String, String> m = new LinkedHashMap<>();
        if (s == null || s.bp == null || !s.bp.has("ops")) return m;
        Map<String, StringBuilder> b = new LinkedHashMap<>();
        for (com.google.gson.JsonElement e : s.bp.getAsJsonArray("ops")) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String p = o.has("part") ? o.get("part").getAsString() : "Конструкция";
            b.computeIfAbsent(p, k -> new StringBuilder()).append(o);
        }
        for (Map.Entry<String, StringBuilder> e : b.entrySet()) m.put(e.getKey(), Integer.toHexString(e.getValue().toString().hashCode()));
        return m;
    }

    private static void finish(String id, String err, long t0) {
        String full = BUF.toString();
        streaming = false;
        NeroPerf.put("Чат: ответ модели", (System.nanoTime() - t0) / 1_000_000L);
        if (err != null && full.isBlank()) {
            NeroProjects.appendChat(id, "system", "Ошибка чата: " + err);
            return;
        }
        String patch = NeroPatch.extract(full);
        String text = NeroPatch.stripPatch(full);
        if (!text.isBlank()) NeroProjects.appendChat(id, "assistant", text + (err != null ? "\n(ответ оборван: " + err + ")" : ""));
        if (patch == null || NeroAI.project == null) return;
        NeroProjects.appendChat(id, "system", NeroEditor.apply(patch, "Чат"));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        String pn = NeroAI.project == null ? "проект не открыт" : NeroAI.project.title();
        ctx.drawTextWithShadow(textRenderer, "NERO Chat · " + textRenderer.trimToWidth(pn, 190) + (streaming ? " · печатает…" : ""), ox, oy, 0x55FFFF);
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(NeroConfig.modelInfo(), 372), ox, oy + 154, 0x777777);
        super.render(ctx, mx, my, d);
        ctx.fill(ox - 2, oy + 12, ox + 374, oy + 14 + ROWS * 9 - 2, 0x66000000);
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= lines.size()) break;
            Object[] l = lines.get(idx);
            ctx.drawTextWithShadow(textRenderer, (OrderedText) l[0], ox, oy + 14 + i * 9, (Integer) l[1]);
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
