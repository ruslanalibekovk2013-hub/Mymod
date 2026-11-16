package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class Hud {
    public static void render(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || !Config.showHud) return;
        if (Config.hudCompact) {
            ctx.drawTextWithShadow(mc.textRenderer, Editor.status(), 4, 4, 0xDDDDDD);
            return;
        }
        ctx.drawTextWithShadow(mc.textRenderer, "Mackros  [Right Shift — меню, C — перенос]", 4, 4, Config.accent());
        ctx.drawTextWithShadow(mc.textRenderer, Editor.status(), 4, 15, 0xDDDDDD);
    }
}
