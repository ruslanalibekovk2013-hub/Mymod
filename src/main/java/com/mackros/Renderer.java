package com.mackros;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
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
        drawPlan(mc, m, vc, vp, c.camera(), cam);

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
        if (vp instanceof VertexConsumerProvider.Immediate imm) { imm.draw(RenderLayer.getLines()); imm.draw(); }
    }

    private static void text(MinecraftClient mc, MatrixStack m, VertexConsumerProvider vp, Camera cam, double x, double y, double z, String s, int color, float scale) {
        m.push();
        m.translate(x, y, z);
        m.multiply(cam.getRotation());
        m.scale(-scale, -scale, scale);
        float w = -mc.textRenderer.getWidth(s) / 2f;
        mc.textRenderer.draw(s, w, 0, color, false, m.peek().getPositionMatrix(), vp, TextRenderer.TextLayerType.SEE_THROUGH, 0x80000000, 0xF000F0);
        m.pop();
    }

    private static String cut(String s, int n) { return s.length() > n ? s.substring(0, n - 1) + "…" : s; }

    /** 3D-предпросмотр проекта (блоки + командные блоки с подписями) и подписи рядом с командными блоками построенного/планируемого проекта. */
    private static void drawPlan(MinecraftClient mc, MatrixStack m, VertexConsumer vc, VertexConsumerProvider vp, Camera camera, Vec3d cam) {
        NeroBuildPlan full = NeroBuilder.previewPlan;
        NeroBuildPlan pl = full != null ? full : NeroBuilder.labelPlan;
        if (pl == null) return;
        if (full != null) {
            int n = 0;
            for (NeroBuildPlan.Block b : pl.blocks) {
                if (b.st().isAir()) continue;
                if (n++ >= Config.previewLimit) break;
                float[] col = color(b.st());
                box(m, vc, new Box(new BlockPos(b.x(), b.y(), b.z())).contract(0.12), col[0], col[1], col[2]);
            }
        }
        double range = full != null ? 56 : 12;
        BlockPos look = Editor.look(mc, false);
        int shown = 0;
        for (NeroBuildPlan.Cb k : pl.cbs) {
            double d2 = cam.squaredDistanceTo(k.x() + 0.5, k.y() + 0.5, k.z() + 0.5);
            boolean sel = k == NeroBuilder.selectedCb || (NeroBuilder.selectedCb != null && NeroBuilder.selectedCb.x() == k.x() && NeroBuilder.selectedCb.y() == k.y() && NeroBuilder.selectedCb.z() == k.z());
            if (!sel && d2 > range * range) continue;
            float r = k.type() == NeroBuildPlan.CbType.IMPULSE ? 1f : k.type() == NeroBuildPlan.CbType.REPEAT ? 0.7f : 0.2f;
            float g = k.type() == NeroBuildPlan.CbType.IMPULSE ? 0.6f : k.type() == NeroBuildPlan.CbType.REPEAT ? 0.3f : 1f;
            float bl = k.type() == NeroBuildPlan.CbType.IMPULSE ? 0.1f : k.type() == NeroBuildPlan.CbType.REPEAT ? 1f : 0.4f;
            if (sel) { r = 1f; g = 1f; bl = 1f; }
            box(m, vc, new Box(new BlockPos(k.x(), k.y(), k.z())).expand(sel ? 0.12 : 0.04), r, g, bl);
            boolean hover = look != null && look.getX() == k.x() && look.getY() == k.y() && look.getZ() == k.z();
            if (!sel && !hover && (shown >= 60 || d2 > (full != null ? 1600.0 : 64.0))) continue;
            shown++;
            int color = k.type() == NeroBuildPlan.CbType.IMPULSE ? 0xFFAA33 : k.type() == NeroBuildPlan.CbType.REPEAT ? 0xCC77FF : 0x66FF88;
            text(mc, m, vp, camera, k.x() + 0.5, k.y() + 1.35, k.z() + 0.5, cut(k.label(), 44), color, 0.022f);
            if (hover || sel) {
                text(mc, m, vp, camera, k.x() + 0.5, k.y() + 1.75, k.z() + 0.5, cut(k.role(), 90), 0xFFFFFF, 0.016f);
                text(mc, m, vp, camera, k.x() + 0.5, k.y() + 2.05, k.z() + 0.5, cut(k.cmd(), 90), 0xAAAAAA, 0.014f);
            }
        }
    }

    private static float[] color(BlockState s) {
        int h = s.getBlock().getTranslationKey().hashCode();
        return new float[]{0.3f + 0.7f * ((h >> 16) & 255) / 255f, 0.3f + 0.7f * ((h >> 8) & 255) / 255f, 0.3f + 0.7f * (h & 255) / 255f};
    }

    private static void box(MatrixStack m, VertexConsumer vc, Box b, float r, float g, float bl) {
        WorldRenderer.drawBox(m, vc, b.expand(0.002), r, g, bl, 1f);
    }
}
