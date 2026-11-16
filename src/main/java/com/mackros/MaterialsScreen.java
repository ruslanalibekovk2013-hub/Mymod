package com.mackros;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

public class MaterialsScreen extends Screen {
    private List<Map.Entry<Block, Integer>> list;
    private int page = 0;
    private static final int PER = 10;

    public MaterialsScreen() { super(Text.literal("Материалы")); }

    @Override
    protected void init() {
        list = Editor.materials(MinecraftClient.getInstance());
        final int pages = Math.max(1, (list.size() + PER - 1) / PER);
        int cx = width / 2, y = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> page = Math.max(0, page - 1)).dimensions(cx - 110, y, 40, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close()).dimensions(cx - 50, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> page = Math.min(pages - 1, page + 1)).dimensions(cx + 70, y, 40, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        super.render(ctx, mx, my, d);
        MinecraftClient mc = MinecraftClient.getInstance();
        int x = width / 2 - 150, y0 = 40;
        ctx.drawCenteredTextWithShadow(textRenderer, "Список материалов, стр " + (page + 1), width / 2, 12, 0x55FFFF);
        ctx.drawTextWithShadow(textRenderer, "Блок", x, y0 - 12, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Нужно", x + 120, y0 - 12, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Есть", x + 240, y0 - 12, 0xAAAAAA);
        int from = page * PER;
        for (int i = from; i < Math.min(list.size(), from + PER); i++) {
            Map.Entry<Block, Integer> e = list.get(i);
            int need = e.getValue();
            int have = Editor.have(mc, e.getKey());
            int col = have >= need ? 0x55FF55 : 0xFF5555;
            int yy = y0 + (i - from) * 14;
            ctx.drawTextWithShadow(textRenderer, e.getKey().getName(), x, yy, 0xFFFFFF);
            ctx.drawTextWithShadow(textRenderer, need + " (" + (need / 64) + " ст + " + (need % 64) + ")", x + 120, yy, col);
            ctx.drawTextWithShadow(textRenderer, String.valueOf(have), x + 240, yy, col);
        }
        if (list.isEmpty()) ctx.drawCenteredTextWithShadow(textRenderer, "Пусто: скопируй постройку или выдели область", width / 2, 60, 0xFFAA00);
    }

    @Override
    public boolean shouldPause() { return false; }
}
