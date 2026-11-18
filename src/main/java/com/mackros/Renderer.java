package com.mackros;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
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

        if (Config.showOutlines) {
            if (Editor.pos1 != null) box(m, vc, new Box(Editor.pos1), 1f, 0.2f, 0.2f);
            if (Editor.pos2 != null) box(m, vc, new Box(Editor.pos2), 0.2f, 0.4f, 1f);
            if (Editor.pos1 != null && Editor.pos2 != null) {
                BlockPos a = Editor.min(), b = Editor.max();
                box(m, vc, new Box(a.getX(), a.getY(), a.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1), 0.2f, 1f, 0.3f);
            }
        }

        if (NeroBuilder.preview != null) box(m, vc, NeroBuilder.preview, 1f, 0.65f, 0.1f);

        if (Editor.preview && !Editor.clip.isEmpty()) {
            BlockPos base = Editor.placeBase(mc);
            if (base != null) {
                int[] s = Editor.size();
                boolean locked = Editor.lock != null;
                box(m, vc, new Box(base.getX(), base.getY(), base.getZ(), base.getX() + s[0], base.getY() + s[1], base.getZ() + s[2]),
                        locked ? 0.8f : 1f, locked ? 0.3f : 0.9f, locked ? 1f : 0.1f);
                int n = 0;
                for (Editor.Entry e : Editor.clip) {
                    if (e.s().isAir() || !Editor.layerOk(e.y())) continue;
                    if (n++ >= Config.previewLimit) break;
                    float[] col = Config.colorMode == 1 ? color(e.s()) : new float[]{0.2f, 0.9f, 0.9f};
                    box(m, vc, new Box(base.add(e.x(), e.y(), e.z())).contract(0.1), col[0], col[1], col[2]);
                }
            }
        }

        if (Editor.verify) {
            for (Editor.Mark k : Editor.marks) {
                float r = k.kind() == 0 ? 1f : k.kind() == 1 ? 0.2f : 1f;
                float g = k.kind() == 0 ? 0.2f : k.kind() == 1 ? 0.6f : 0.9f;
                float bl = k.kind() == 0 ? 0.2f : k.kind() == 1 ? 1f : 0.1f;
                box(m, vc, new Box(k.p()).expand(0.02), r, g, bl);
            }
        }

        m.pop();
        if (vp instanceof VertexConsumerProvider.Immediate imm) imm.draw(RenderLayer.getLines());
    }

    private static float[] color(BlockState s) {
        int h = s.getBlock().getTranslationKey().hashCode();
        return new float[]{0.3f + 0.7f * ((h >> 16) & 255) / 255f, 0.3f + 0.7f * ((h >> 8) & 255) / 255f, 0.3f + 0.7f * (h & 255) / 255f};
    }

    private static void box(MatrixStack m, VertexConsumer vc, Box b, float r, float g, float bl) {
        WorldRenderer.drawBox(m, vc, b.expand(0.002), r, g, bl, 1f);
    }
}
