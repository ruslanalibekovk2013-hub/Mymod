package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class ElevatorScreen extends Screen {
    private final BlockPos pos;
    private List<Integer> floors = new ArrayList<>();
    private int page = 0;
    private static final int PER = 7;

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
        int cx = width / 2, y0 = Math.max(30, height / 2 - 80);
        for (int i = 0; i < PER; i++) {
            int idx = n - 1 - (page * PER + i);
            if (idx < 0) break;
            final int fy = floors.get(idx);
            boolean cur = fy == pos.getY();
            boolean open = ElevatorClient.open(mc.world, new BlockPos(pos.getX(), fy, pos.getZ()));
            String label = (cur ? "> " : "") + "Этаж " + (idx + 1) + "   Y=" + fy + (open || cur ? "" : "  (закрыт)");
            ButtonWidget bw = ButtonWidget.builder(Text.literal(label), b -> {
                ElevatorClient.send(pos, fy);
                close();
            }).dimensions(cx - 80, y0 + i * 22, 160, 20).build();
            bw.active = open && !cur;
            addDrawableChild(bw);
        }
        int by = y0 + PER * 22 + 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("^ выше"), b -> { page = Math.max(0, page - 1); clearAndInit(); })
                .dimensions(cx - 80, by, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close()).dimensions(cx - 25, by, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("v ниже"), b -> { page = Math.min(pages - 1, page + 1); clearAndInit(); })
                .dimensions(cx + 30, by, 50, 20).build());
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
