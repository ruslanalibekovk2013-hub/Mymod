package com.mackros;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/** Рисует текст на лицевой стороне блока DigitalDisplay (клиент). */
public class DisplayRenderer implements BlockEntityRenderer<NeroBlocks.DisplayBE> {
    private final TextRenderer font;

    public DisplayRenderer(BlockEntityRendererFactory.Context ctx) { this.font = ctx.getTextRenderer(); }

    @Override
    public void render(NeroBlocks.DisplayBE be, float tickDelta, MatrixStack m, VertexConsumerProvider vp, int light, int overlay) {
        String t = be.text;
        if (t == null || t.isEmpty()) return;
        Direction d = be.getCachedState().get(NeroBlocks.FACING);
        m.push();
        m.translate(0.5, 0.5, 0.5);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-d.asRotation()));
        m.translate(0, 0, -0.365);
        float s = Math.min(0.04f, 0.85f / Math.max(1, font.getWidth(t)));
        m.scale(s, -s, s);
        font.draw(t, -font.getWidth(t) / 2f, -4f, be.color, false, m.peek().getPositionMatrix(), vp, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        m.pop();
    }
}
