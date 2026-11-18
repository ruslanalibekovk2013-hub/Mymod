package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Интерфейс NERO AI: запрос, диалог, информация о проекте, предпросмотр, постройка, журнал. */
public class NeroScreen extends Screen {
    private static BlockPos base = null;
    private static BlockPos lastBuiltBase = null;
    private static NeroBuildPlan plan = null;
    private static NeroValidator.Report report = null;
    private static int replaced = 0, unloaded = 0;
    private static boolean confirm = false, buildAfterRollback = false;
    private static String draft = "";

    private TextFieldWidget field;
    private ButtonWidget pauseBtn, buildBtn;
    private int ox, oy;

    public NeroScreen() {
        super(Text.literal("NERO AI"));
        MinecraftClient mc = MinecraftClient.getInstance();
        if (base == null) base = defaultBase(mc);
    }

    private static BlockPos defaultBase(MinecraftClient mc) {
        BlockPos t = Editor.look(mc, true);
        if (t != null) return t;
        if (mc.player != null) return mc.player.getBlockPos().offset(mc.player.getHorizontalFacing(), 5);
        return BlockPos.ORIGIN;
    }

    private ButtonWidget btn(String label, int x, int y, int w, int h, Runnable r) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), bb -> r.run()).dimensions(x, y, w, h).build();
        addDrawableChild(b);
        return b;
    }

    @Override
    protected void init() {
        ox = width / 2 - 190;
        oy = Math.max(4, (height - 206) / 2);
        field = new TextFieldWidget(textRenderer, ox, oy + 12, 372, 18, Text.literal("запрос"));
        field.setMaxLength(400);
        field.setText(draft);
        field.setSuggestion(draft.isEmpty() ? "Опиши, что построить: «Построй лифт на 10 этажей»" : "");
        field.setChangedListener(t -> { draft = t; field.setSuggestion(t.isEmpty() ? "Опиши, что построить: «Построй лифт на 10 этажей»" : ""); });
        addDrawableChild(field);
        setInitialFocus(field);

        int y1 = oy + 34, y2 = oy + 54, y3 = oy + 74;
        btn("Сгенерировать", ox, y1, 90, 18, this::generate);
        btn("Предпросмотр", ox + 94, y1, 90, 18, this::preview);
        buildBtn = btn("Построить", ox + 188, y1, 90, 18, this::build);
        btn("Откатить", ox + 282, y1, 90, 18, () -> NeroBuilder.rollback(MinecraftClient.getInstance()));
        pauseBtn = btn("Пауза", ox, y2, 90, 18, () -> { if (NeroBuilder.paused) NeroBuilder.resume(); else NeroBuilder.pause(); });
        btn("Отменить постройку", ox + 94, y2, 90, 18, NeroBuilder::cancel);
        btn("Очистить", ox + 188, y2, 90, 18, () -> {
            NeroAI.clear(); plan = null; report = null; confirm = false; NeroBuilder.clearPreview(); NeroHistory.clearLog(); draft = ""; field.setText("");
        });
        btn("Настройки AI", ox + 282, y2, 90, 18, () -> MinecraftClient.getInstance().setScreen(new NeroSettingsScreen()));

        int x = ox;
        x = row(x, y3, "Цель: я", 60, () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) { base = mc.player.getBlockPos().offset(mc.player.getHorizontalFacing(), 5); afterMove(); }
        });
        x = row(x, y3, "X-", 28, () -> { base = base.add(-1, 0, 0); afterMove(); });
        x = row(x, y3, "X+", 28, () -> { base = base.add(1, 0, 0); afterMove(); });
        x = row(x, y3, "Z-", 28, () -> { base = base.add(0, 0, -1); afterMove(); });
        x = row(x, y3, "Z+", 28, () -> { base = base.add(0, 0, 1); afterMove(); });
        x = row(x, y3, "Y-", 28, () -> { base = base.add(0, -1, 0); afterMove(); });
        x = row(x, y3, "Y+", 28, () -> { base = base.add(0, 1, 0); afterMove(); });
        x = row(x, y3, "Меню", 54, () -> MinecraftClient.getInstance().setScreen(new MackrosScreen()));
        row(x, y3, "Закрыть", 56, this::close);

        List<String> h = NeroHistory.requests();
        for (int i = 0; i < 3; i++) {
            int idx = h.size() - 1 - i;
            if (idx < 0) break;
            final String q = h.get(idx);
            btn(textRenderer.trimToWidth(q, 112), ox + i * 125, oy + 172, 122, 14, () -> { field.setText(q); setFocused(field); });
        }
    }

    private int row(int x, int y, String label, int w, Runnable r) {
        btn(label, x, y, w, 16, r);
        return x + w + 2;
    }

    private void afterMove() {
        confirm = false;
        NeroBuilder.clearPreview();
        if (NeroAI.project != null) replan(false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && field.isFocused()) { generate(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (pauseBtn != null) {
            pauseBtn.setMessage(Text.literal(NeroBuilder.paused ? "Продолжить" : "Пауза"));
            pauseBtn.active = NeroBuilder.running;
        }
        if (buildAfterRollback && !NeroBuilder.busy()) {
            buildAfterRollback = false;
            doStart();
        }
    }

    private void generate() {
        String text = field.getText().trim();
        confirm = false;
        if (text.isEmpty()) {
            if (NeroAI.project != null) replan(true); else NeroHistory.log("Введи запрос");
            return;
        }
        NeroAI.ask(text, r -> {
            draft = "";
            if (field != null) field.setText("");
            if (r.ok && NeroAI.project != null && r.spec != null) replan(true);
        });
    }

    /** Пересчитывает план для текущей точки и проверяет его. */
    private static void replan(boolean verbose) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (NeroAI.project == null || mc.world == null) return;
        plan = NeroPlanner.plan(NeroAI.project, base);
        ClientWorld w = mc.world;
        report = NeroValidator.validate(plan, w.getBottomY(), w.getTopY());
        replaced = 0;
        Set<Long> unl = new HashSet<>();
        for (NeroBuildPlan.Block b : plan.blocks) {
            if (b.st().isAir()) continue;
            if (!w.getChunkManager().isChunkLoaded(b.x() >> 4, b.z() >> 4)) { unl.add(((long) (b.x() >> 4) << 32) ^ (b.z() >> 4)); continue; }
            if (!w.getBlockState(new BlockPos(b.x(), b.y(), b.z())).isAir()) replaced++;
        }
        unloaded = unl.size();
        if (verbose) {
            NeroHistory.log("План «" + plan.title + "»: " + plan.sx() + "x" + plan.sy() + "x" + plan.sz() + ", блоков " + plan.solid() + ", КБ " + plan.cbs.size() + ", механизмов " + plan.mechanisms());
            for (String e : report.errors.subList(0, Math.min(6, report.errors.size()))) NeroHistory.log("ОШИБКА: " + e);
            for (String e : report.warnings) NeroHistory.log("Внимание: " + e);
            for (String e : report.passed) NeroHistory.log("Проверено: " + e);
            if (replaced > 0) NeroHistory.log("Будет заменено существующих блоков: " + replaced);
            if (unloaded > 0) NeroHistory.log("Не загружено чанков в области: " + unloaded + " (подойди ближе)");
            if (report.ok()) NeroHistory.log("Проверка пройдена: проект можно строить");
        }
    }

    private void preview() {
        confirm = false;
        if (NeroAI.project == null) { NeroHistory.log("Сначала сгенерируй проект"); return; }
        replan(true);
        if (plan != null) {
            NeroBuilder.showPreview(plan);
            NeroHistory.log("Область подсвечена оранжевой рамкой. Нажми F9, чтобы вернуться в NERO.");
            close();
        }
    }

    private void build() {
        if (NeroAI.project == null) { NeroHistory.log("Сначала сгенерируй проект"); return; }
        replan(true);
        if (plan == null) return;
        if (!report.ok()) { NeroHistory.log("Постройка отклонена: проект не прошёл проверку"); confirm = false; return; }
        if (unloaded > 0) { NeroHistory.log("Часть области не загружена: подойди ближе и повтори"); return; }
        if ((replaced > 200 || plan.ops() > 3000) && !confirm) {
            confirm = true;
            NeroHistory.log("Большое изменение (" + plan.ops() + " операций, заменит " + replaced + " блоков). Нажми «Построить» ещё раз для подтверждения.");
            return;
        }
        confirm = false;
        if (lastBuiltBase != null && lastBuiltBase.equals(base) && NeroBuilder.hasHistory()) {
            NeroHistory.log("Изменение уже построенного проекта: сначала откатываю прошлую версию");
            NeroBuilder.rollback(MinecraftClient.getInstance());
            buildAfterRollback = true;
            return;
        }
        doStart();
    }

    private void doStart() {
        if (NeroAI.project == null) return;
        replan(false);
        if (plan == null || !report.ok()) return;
        if (NeroBuilder.start(MinecraftClient.getInstance(), plan)) lastBuiltBase = base;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        ctx.drawTextWithShadow(textRenderer, "NERO AI — конструктор", ox, oy, 0x55FFFF);
        String prov = NeroConfig.remoteReady() ? "ИИ: внешний" : "ИИ: локальный";
        String st = NeroAI.busy() ? "думаю..." : NeroBuilder.running ? (NeroBuilder.paused ? "пауза" : "строю") : "готов";
        ctx.drawTextWithShadow(textRenderer, prov + " | " + st, ox + 372 - textRenderer.getWidth(prov + " | " + st), oy, 0xAAAAAA);
        super.render(ctx, mx, my, d);

        int ix = ox, iy = oy + 96;
        ctx.fill(ix - 2, iy - 2, ix + 182, iy + 74, 0x66000000);
        List<String> info = new ArrayList<>();
        NeroSpec s = NeroAI.project;
        if (s == null) info.add("Проект: нет");
        else {
            info.add("Проект: " + s.title());
            if (s.floors > 0) info.add("Этажи: " + s.floors);
            if (s.kind.equals("house")) {
                info.add("Гараж: " + (s.extras.contains("garage") ? "есть (" + s.garageSide + ")" : "нет") + "  Бассейн: " + (s.extras.contains("pool") ? "есть" : "нет"));
                info.add("Освещение: " + (s.extras.contains("lighting") ? "автоматическое" : "внутреннее") + "  Ворота: " + (s.extras.contains("autogate") || s.extras.contains("garage") ? "авто" : "нет"));
            }
            if (plan != null) {
                info.add("Размер: " + plan.sx() + "x" + plan.sy() + "x" + plan.sz() + "  Механизмов: " + plan.mechanisms());
                info.add("Блоков: " + plan.solid() + "  КБ: " + plan.cbs.size());
                info.add("Этапов: " + plan.stages() + " (порции по " + NeroConfig.blocksPerTick + ")");
                info.add("Точка: " + base.getX() + " " + base.getY() + " " + base.getZ());
                info.add(report == null ? "" : (report.ok() ? "Проверка: ОК" : "Проверка: ошибок " + report.errors.size()) + (replaced > 0 ? "  замена: " + replaced : ""));
            } else info.add("Точка: " + base.getX() + " " + base.getY() + " " + base.getZ());
        }
        int y = iy;
        for (String l : info) {
            if (y > iy + 64) break;
            int color = l.startsWith("Проверка: ошибок") ? 0xFF5555 : (l.startsWith("Проверка: ОК") ? 0x55FF55 : 0xFFFFFF);
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(l, 178), ix, y, color);
            y += 9;
        }

        int jx = ox + 188;
        ctx.fill(jx - 2, iy - 2, jx + 186, iy + 74, 0x66000000);
        List<OrderedText> lines = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        for (String l : NeroHistory.log()) {
            int c = l.startsWith("Вы:") ? 0x55FFFF : l.startsWith("NERO") ? 0x55FF55 : (l.startsWith("ОШИБКА") || l.startsWith("Ошибка") || l.startsWith("Постройка отклонена")) ? 0xFF5555 : l.startsWith("Внимание") ? 0xFFAA00 : 0xAAAAAA;
            for (OrderedText o : textRenderer.wrapLines(Text.literal(l), 180)) { lines.add(o); cols.add(c); }
        }
        int from = Math.max(0, lines.size() - 8);
        int jy = iy;
        for (int i = from; i < lines.size(); i++) { ctx.drawTextWithShadow(textRenderer, lines.get(i), jx, jy, cols.get(i)); jy += 9; }

        if (NeroBuilder.running || NeroBuilder.total > 0) {
            int bw = 372;
            ctx.fill(ox, oy + 190, ox + bw, oy + 195, 0xFF333333);
            int fw = NeroBuilder.total == 0 ? 0 : (int) ((long) bw * NeroBuilder.done / NeroBuilder.total);
            ctx.fill(ox, oy + 190, ox + fw, oy + 195, NeroBuilder.paused ? 0xFFFFAA00 : 0xFF00C8E0);
            ctx.drawTextWithShadow(textRenderer, NeroBuilder.done + "/" + NeroBuilder.total, ox + bw - 60, oy + 197, 0xAAAAAA);
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
