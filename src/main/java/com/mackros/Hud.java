package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class Hud {
    public static void render(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || !Config.showHud) return;
        ctx.drawTextWithShadow(mc.textRenderer, "Mackros  [Right Shift — меню]", 4, 4, 0x55FFFF);
        ctx.drawTextWithShadow(mc.textRenderer, Editor.status(), 4, 15, 0xDDDDDD);
    }
}
