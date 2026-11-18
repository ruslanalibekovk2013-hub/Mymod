package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;

/** Настройки внешнего ИИ. Ключ хранится только в config/mackros_nero.properties на этом компьютере. */
public class NeroSettingsScreen extends Screen {
    private TextFieldWidget url, key, model, timeout, maxResp, perTick;
    private ButtonWidget toggle;
    private boolean enabled;
    private int ox, oy;

    public NeroSettingsScreen() { super(Text.literal("NERO AI: настройки")); }

    private TextFieldWidget tf(int y, String hint, String val, int max) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, ox + 110, y, 262, 16, Text.literal(hint));
        f.setMaxLength(max);
        f.setText(val);
        addDrawableChild(f);
        return f;
    }

    @Override
    protected void init() {
        ox = width / 2 - 190;
        oy = Math.max(8, (height - 190) / 2);
        enabled = NeroConfig.enabled;
        url = tf(oy + 14, "API URL", NeroConfig.url, 300);
        key = tf(oy + 36, "API Key", NeroConfig.key, 300);
        key.setRenderTextProvider((s, i) -> OrderedText.styledForwardsVisitedString("*".repeat(s.length()), Style.EMPTY));
        model = tf(oy + 58, "Модель", NeroConfig.model, 100);
        timeout = tf(oy + 80, "Тайм-аут", String.valueOf(NeroConfig.timeoutSec), 4);
        maxResp = tf(oy + 102, "Макс. ответ", String.valueOf(NeroConfig.maxResponse), 7);
        perTick = tf(oy + 124, "Блоков за тик", String.valueOf(NeroConfig.blocksPerTick), 5);
        toggle = ButtonWidget.builder(Text.literal(""), b -> { enabled = !enabled; refresh(); }).dimensions(ox, oy + 148, 120, 18).build();
        addDrawableChild(toggle);
        refresh();
        addDrawableChild(ButtonWidget.builder(Text.literal("Проверить"), b -> test()).dimensions(ox + 124, oy + 148, 80, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), b -> { save(); close(); }).dimensions(ox + 208, oy + 148, 80, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> { save(); MinecraftClient.getInstance().setScreen(new NeroScreen()); }).dimensions(ox + 292, oy + 148, 80, 18).build());
    }

    private void refresh() { toggle.setMessage(Text.literal("Внешний ИИ: " + (enabled ? "ВКЛ" : "ВЫКЛ"))); }

    private static int num(String s, int d) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return d; } }

    private void save() {
        NeroConfig.enabled = enabled;
        NeroConfig.url = url.getText().trim();
        NeroConfig.key = key.getText().trim();
        NeroConfig.model = model.getText().trim();
        NeroConfig.timeoutSec = NeroSpec.clamp(num(timeout.getText(), 30), 3, 120);
        NeroConfig.maxResponse = NeroSpec.clamp(num(maxResp.getText(), 20000), 1000, 200000);
        NeroConfig.blocksPerTick = NeroSpec.clamp(num(perTick.getText(), 300), 20, 2000);
        NeroConfig.save();
    }

    private void test() {
        save();
        if (!NeroConfig.remoteReady()) { NeroHistory.log("Проверка: включи ИИ и укажи URL (http:// или https://)"); return; }
        NeroHistory.log("Проверка подключения...");
        MinecraftClient mc = MinecraftClient.getInstance();
        new HttpProvider().process(new NeroRequest("Построй лифт на 3 этажа", null, null, List.of())).whenComplete((r, e) -> mc.execute(() -> {
            if (e != null || r == null) NeroHistory.log("Проверка: сбой запроса");
            else if (r.ok) NeroHistory.log("Проверка пройдена: ИИ вернул проект «" + r.spec.title() + "»");
            else NeroHistory.log("Проверка не пройдена: " + r.error);
        }));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        ctx.drawTextWithShadow(textRenderer, "NERO AI: подключение внешней нейросети (необязательно)", ox, oy - 2, 0x55FFFF);
        String[] lab = {"API URL", "API Key", "Модель", "Тайм-аут, сек", "Макс. ответ", "Блоков за тик"};
        for (int i = 0; i < lab.length; i++) ctx.drawTextWithShadow(textRenderer, lab[i], ox, oy + 18 + i * 22, 0xFFFFFF);
        String h = "Формат запроса: OpenAI-совместимый chat/completions (адрес вида https://хост/v1/chat/completions). Ключ хранится только локально и никому не отправляется, кроме указанного адреса. Без ИИ работает локальный режим.";
        int y = oy + 172;
        for (OrderedText l : textRenderer.wrapLines(Text.literal(h), 372)) { ctx.drawTextWithShadow(textRenderer, l, ox, y, 0xAAAAAA); y += 9; }
        super.render(ctx, mx, my, d);
    }
}
