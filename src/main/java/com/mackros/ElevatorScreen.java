package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Панель лифта: мини-кнопки по этажам, сеткой, как настоящий пульт. */
public class ElevatorScreen extends Screen {
    private final BlockPos pos;
    private List<Integer> floors = new ArrayList<>();
    private int page = 0;

    private static final int COLS = 6;
    private static final int ROWS = 5;
    private static final int PER = COLS * ROWS;
    private static final int CELL = 24;
    private static final int GAP = 4;

    public ElevatorScreen(BlockPos pos) {
        super(Text.literal("Лифт"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        MinecraftClient mc = MinecraftClient.getInstance();
        floors = ElevatorClient.floors(mc.world, pos);
        final int n = floors.size();
        final int pages = Math.max(1, (n + PER - 1) / PER);
        if (page >= pages) page = pages - 1;

        int gridW = COLS * CELL + (COLS - 1) * GAP;
        int gridH = ROWS * CELL + (ROWS - 1) * GAP;
        int gx = width / 2 - gridW / 2;
        int gy = Math.max(34, height / 2 - gridH / 2 - 6);

        for (int i = 0; i < PER; i++) {
            int idx = n - 1 - (page * PER + i);
            if (idx < 0) break;
            final int fy = floors.get(idx);
            final boolean cur = fy == pos.getY();
            final boolean open = ElevatorClient.open(mc.world, new BlockPos(pos.getX(), fy, pos.getZ()));

            int col = i % COLS, row = i / COLS;
            int bx = gx + col * (CELL + GAP), by = gy + row * (CELL + GAP);

            String label = (cur ? ">" : "") + (idx + 1);
            ButtonWidget bw = new ButtonWidget(bx, by, CELL, CELL, Text.literal(label), b -> {
                ElevatorClient.send(pos, fy);
                close();
            }, ButtonWidget.DEFAULT_NARRATION_SUPPLIER) {
                @Override
                public void renderWidget(DrawContext ctx, int mx, int my, float d) {
                    int base = cur ? 0x335555FF : (open ? 0x3300AA55 : 0x33AA3333);
                    ctx.fill(getX(), getY(), getX() + width, getY() + height, base);
                    int border = cur ? 0xFF55AAFF : (open ? 0xFF55FF88 : 0xFF884444);
                    ctx.drawBorder(getX(), getY(), width, height, border);
                    int col2 = (!active) ? 0xFF888888 : (cur ? 0xFFFFFFFF : 0xFFEEEEEE);
                    ctx.drawCenteredTextWithShadow(textRenderer, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, col2);
                }
            };
            bw.active = open && !cur;
            addDrawableChild(bw);
        }

        int by2 = gy + gridH + 8;
        int cx = width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("^ выше"), b -> { page = Math.max(0, page - 1); clearAndInit(); })
                .dimensions(cx - 80, by2, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close()).dimensions(cx - 25, by2, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("v ниже"), b -> { page = Math.min(pages - 1, page + 1); clearAndInit(); })
                .dimensions(cx + 30, by2, 50, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        super.render(ctx, mx, my, d);
        ctx.drawCenteredTextWithShadow(textRenderer, "Лифт: этажей " + floors.size() + "  (прыжок = вверх, присед = вниз)", width / 2, 12, 0x55FFFF);
    }

    @Override
    public boolean shouldPause() { return false; }
}
