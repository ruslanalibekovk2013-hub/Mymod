package com.mackros;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class Renderer {
    public static void render(WorldRenderContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        VertexConsumerProvider vp = c.consumers();
        if (vp == null) return;
        MatrixStack m = c.matrixStack();
        Vec3d cam = c.camera().getPos();
        VertexConsumer vc = vp.getBuffer(RenderLayer.getLines());
        m.push();
        m.translate(-cam.x, -cam.y, -cam.z);

        if (Editor.pos1 != null) box(m, vc, new Box(Editor.pos1), 1f, 0.2f, 0.2f);
        if (Editor.pos2 != null) box(m, vc, new Box(Editor.pos2), 0.2f, 0.4f, 1f);
        if (Editor.pos1 != null && Editor.pos2 != null) {
            BlockPos a = Editor.min(), b = Editor.max();
            box(m, vc, new Box(a.getX(), a.getY(), a.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1), 0.2f, 1f, 0.3f);
        }

        if (Editor.preview && !Editor.clip.isEmpty()) {
            BlockPos base = Editor.anchor(mc);
            if (base != null) {
                int[] s = Editor.size();
                box(m, vc, new Box(base.getX(), base.getY(), base.getZ(), base.getX() + s[0], base.getY() + s[1], base.getZ() + s[2]), 1f, 0.9f, 0.1f);
                int n = 0;
                for (Editor.Entry e : Editor.clip) {
                    if (e.s().isAir()) continue;
                    if (n++ > 2500) break;
                    box(m, vc, new Box(base.add(e.x(), e.y(), e.z())).contract(0.1), 0.2f, 0.9f, 0.9f);
                }
            }
        }
        m.pop();
        if (vp instanceof VertexConsumerProvider.Immediate imm) imm.draw(RenderLayer.getLines());
    }

    private static void box(MatrixStack m, VertexConsumer vc, Box b, float r, float g, float bl) {
        WorldRenderer.drawBox(m, vc, b.expand(0.002), r, g, bl, 1f);
    }
}
