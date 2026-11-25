package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/** Настройки NERO: достаточно вставить токен OpenRouter. Остальное спрятано в «Дополнительно». */
public class NeroSettingsScreen extends Screen {
    private static boolean adv = false;
    private TextFieldWidget key, url, model, timeout, perTick;
    private ButtonWidget qualityBtn, thinkBtn;
    private int quality;
    private boolean deep;
    private String msg = "";
    private int ox, oy, W, H;

    public NeroSettingsScreen() { super(Text.literal("NERO AI: подключение")); }

    private TextFieldWidget tf(int y, String val, int max) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, ox + 110, y, W - 120, 16, Text.literal(""));
        f.setMaxLength(max);
        f.setText(val);
        addDrawableChild(f);
        return f;
    }

    private ButtonWidget btn(String label, int x, int y, int w, int h, Runnable r) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), bb -> r.run()).dimensions(x, y, w, h).build();
        addDrawableChild(b);
        return b;
    }

    @Override
    protected void init() {
        W = Math.min(width - 12, 400);
        H = adv ? 262 : 178;
        ox = (width - W) / 2;
        oy = Math.max(4, (height - H) / 2);
        quality = NeroConfig.quality;
        deep = NeroConfig.deepThink;
        key = new TextFieldWidget(textRenderer, ox + 10, oy + 46, W - 20, 18, Text.literal("токен"));
        key.setMaxLength(300);
        key.setText(NeroConfig.key);
        key.setRenderTextProvider((s, i) -> OrderedText.styledForwardsVisitedString("*".repeat(s.length()), Style.EMPTY));
        addDrawableChild(key);
        setInitialFocus(key);
        int half = (W - 28) / 2;
        qualityBtn = btn("", ox + 10, oy + 72, half, 18, () -> { quality = quality == 0 ? 1 : 0; refresh(); });
        thinkBtn = btn("", ox + 18 + half, oy + 72, half, 18, () -> { deep = !deep; refresh(); });
        btn(adv ? "Дополнительно: скрыть" : "Дополнительно", ox + 10, oy + 96, 130, 16, () -> { save(); adv = !adv; MinecraftClient.getInstance().setScreen(new NeroSettingsScreen()); });
        if (adv) {
            url = tf(oy + 118, NeroConfig.url.isEmpty() ? OpenRouter.CHAT_URL : NeroConfig.url, 300);
            model = tf(oy + 138, NeroConfig.model.isEmpty() ? "auto" : NeroConfig.model, 100);
            timeout = tf(oy + 158, String.valueOf(NeroConfig.timeoutSec), 4);
            perTick = tf(oy + 178, String.valueOf(NeroConfig.blocksPerTick), 5);
        }
        int by = oy + H - 24, bw = (W - 28) / 3;
        btn("Проверить", ox + 10, by, bw, 18, this::test);
        btn("Сохранить", ox + 14 + bw, by, bw, 18, () -> { save(); close(); });
        btn("Назад", ox + 18 + 2 * bw, by, bw, 18, () -> { save(); MinecraftClient.getInstance().setScreen(new NeroScreen()); });
        refresh();
    }

    private void refresh() {
        qualityBtn.setMessage(Text.literal(quality == 0 ? "Модели: бесплатные (авто)" : "Модели: лучшие (платные)"));
        thinkBtn.setMessage(Text.literal("Глубокое мышление: " + (deep ? "ВКЛ" : "ВЫКЛ")));
    }

    private static int num(String s, int d) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return d; } }

    private void save() {
        String nk = key.getText().trim();
        if (!nk.isEmpty() && !nk.equals(NeroConfig.key)) NeroConfig.enabled = true;
        NeroConfig.key = nk;
        NeroConfig.quality = quality;
        NeroConfig.deepThink = deep;
        if (nk.isEmpty()) NeroConfig.enabled = false;
        if (adv && url != null) {
            NeroConfig.url = url.getText().trim().isEmpty() ? OpenRouter.CHAT_URL : url.getText().trim();
            NeroConfig.model = model.getText().trim().isEmpty() ? "auto" : model.getText().trim();
            NeroConfig.timeoutSec = NeroSpec.clamp(num(timeout.getText(), 90), 3, 300);
            NeroConfig.blocksPerTick = NeroSpec.clamp(num(perTick.getText(), 300), 20, 2000);
        }
        NeroConfig.save();
    }

    private void test() {
        save();
        if (!NeroConfig.remoteReady()) { msg = "Сначала вставь токен OpenRouter."; return; }
        msg = "Проверяю: отправляю настоящий запрос модели…";
        MinecraftClient mc = MinecraftClient.getInstance();
        NeroChatClient.test(m -> mc.execute(() -> msg = m));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        NeroUi.box(ctx, ox, oy, ox + W, oy + H, NeroUi.BG, NeroUi.LINE);
        ctx.fill(ox + 1, oy + 1, ox + W - 1, oy + 20, NeroUi.PANEL2);
        ctx.drawTextWithShadow(textRenderer, "NERO AI · подключение", ox + 8, oy + 6, NeroUi.ACCENT);
        int y = oy + 25;
        for (OrderedText l : textRenderer.wrapLines(Text.literal("Вставь токен OpenRouter (openrouter.ai/keys) и нажми «Сохранить». Адрес и модель выбираются сами."), W - 20)) { ctx.drawTextWithShadow(textRenderer, l, ox + 10, y, NeroUi.DIM); y += 9; }
        ctx.drawTextWithShadow(textRenderer, "Токен", ox + 10, oy + 36, NeroUi.TEXT);
        if (adv) {
            String[] lab = {"Адрес API", "Модель", "Тайм-аут, сек", "Блоков за тик"};
            for (int i = 0; i < lab.length; i++) ctx.drawTextWithShadow(textRenderer, lab[i], ox + 10, oy + 122 + i * 20, NeroUi.TEXT);
        }
        int sy = oy + (adv ? 200 : 118);
        String st = msg.isEmpty() ? NeroConfig.modelInfo() : msg;
        for (OrderedText l : textRenderer.wrapLines(Text.literal(st), W - 20)) { if (sy > oy + H - 30) break; ctx.drawTextWithShadow(textRenderer, l, ox + 10, sy, msg.isEmpty() ? NeroUi.DIM : NeroUi.TEXT); sy += 9; }
        super.render(ctx, mx, my, d);
    }
}
