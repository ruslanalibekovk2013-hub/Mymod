package com.mackros;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 3D-редактор проекта: собственная проекция (вращение, масштаб, сдвиг камеры), выбор компонентов, перемещение, поворот, дубль, удаление, отмена/повтор,
 * правка команды выбранного командного блока, просмотр связей. Режимы: голограмма, реализм (цвета блоков с затенением), каркас, инженерия, командные блоки.
 * В мире ничего не строится, пока не нажата «К построению».
 */
public class NeroEditorScreen extends Screen {
    private static final class Vox { int x, y, z, el, rgb; NeroBuildPlan.Cb cb; boolean surface; }

    private static final class Quad { float[] p = new float[8]; int argb; float depth; boolean edge; }

    private static final String[] MODES = {"Голограмма", "Реализм", "Каркас", "Инженерия", "Команды"};
    private static final int[] PAL = {0xE6194B, 0x3CB44B, 0xFFE119, 0x4363D8, 0xF58231, 0x911EB4, 0x46F0F0, 0xF032E6, 0xBCF60C, 0xFABEBE, 0x008080, 0xE6BEFF};

    private NeroBuildPlan plan;
    private final List<Vox> vox = new ArrayList<>();
    private final Map<Long, Vox> occ = new HashMap<>();
    private final List<Quad> quads = new ArrayList<>();
    private double[][] centers = new double[0][];
    private double yaw = 0.8, pitch = 0.55, dist = 40, panX = 0, panY = 0, tx, ty, tz;
    private int mode = 1, selEl = -1, listScroll = 0;
    private Vox selCb = null;
    private boolean dirty = true, truncated = false;
    private double pressX, pressY;
    private String status = "ЛКМ: вращать · ПКМ: сдвиг · колесо: масштаб · клик: выбрать компонент";
    private TextFieldWidget cmdField;
    private ButtonWidget applyCmd;

    public NeroEditorScreen() { super(Text.literal("3D-редактор")); }

    private static long key(int x, int y, int z) { return ((long) (x + 4096) << 40) | ((long) (y + 512) << 20) | (z + 4096); }

    private static int cbColor(NeroBuildPlan.CbType t) { return t == NeroBuildPlan.CbType.IMPULSE ? 0xFFAA33 : t == NeroBuildPlan.CbType.REPEAT ? 0xCC77FF : 0x66FF88; }

    private void load() {
        vox.clear(); occ.clear();
        plan = NeroScreen.plan();
        if (plan == null && NeroAI.project != null) { NeroScreen.replan(false); plan = NeroScreen.plan(); }
        if (plan == null) return;
        List<NeroBuildPlan.Block> bl = new ArrayList<>(plan.blocks);
        bl.sort(Comparator.comparingInt(NeroBuildPlan.Block::el).thenComparingInt(NeroBuildPlan.Block::stage));
        Map<Long, Vox> m = new HashMap<>();
        for (NeroBuildPlan.Block b : bl) {
            long k = key(b.x(), b.y(), b.z());
            if (b.st().isAir()) { m.remove(k); continue; }
            Vox v = new Vox();
            v.x = b.x(); v.y = b.y(); v.z = b.z(); v.el = b.el();
            BlockState st = b.st();
            int c = 0;
            try { c = st.getMapColor(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).color; } catch (Exception ignored) { }
            v.rgb = c == 0 ? 0x999999 : c;
            m.put(k, v);
        }
        for (NeroBuildPlan.Cb cb : plan.cbs) {
            Vox v = new Vox();
            v.x = cb.x(); v.y = cb.y(); v.z = cb.z(); v.el = cb.el(); v.cb = cb; v.rgb = cbColor(cb.type());
            m.put(key(cb.x(), cb.y(), cb.z()), v);
        }
        vox.addAll(m.values());
        occ.putAll(m);
        int[] d = {1, 0, 0, -1, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, 1, 0, 0, -1};
        double sx = 0, sy = 0, sz = 0;
        int ne = plan.elements.size();
        centers = new double[ne][4];
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Vox v : vox) {
            for (int i = 0; i < 6; i++) if (!occ.containsKey(key(v.x + d[i * 3], v.y + d[i * 3 + 1], v.z + d[i * 3 + 2]))) { v.surface = true; break; }
            sx += v.x; sy += v.y; sz += v.z;
            minX = Math.min(minX, v.x); minY = Math.min(minY, v.y); minZ = Math.min(minZ, v.z);
            maxX = Math.max(maxX, v.x); maxY = Math.max(maxY, v.y); maxZ = Math.max(maxZ, v.z);
            if (v.el >= 0 && v.el < ne) { centers[v.el][0] += v.x + 0.5; centers[v.el][1] += v.y + 0.5; centers[v.el][2] += v.z + 0.5; centers[v.el][3]++; }
        }
        if (!vox.isEmpty()) {
            tx = (minX + maxX + 1) / 2.0; ty = (minY + maxY + 1) / 2.0; tz = (minZ + maxZ + 1) / 2.0;
            dist = Math.max(10, Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) * 1.7 + 8);
        }
        dirty = true;
    }

    @Override
    protected void init() {
        if (plan == null) load();
        clearChildren();
        int bw = Math.max(44, (width - 190) / 5);
        for (int i = 0; i < MODES.length; i++) { final int mm = i; addDrawableChild(ButtonWidget.builder(Text.literal(MODES[i]), b -> { mode = mm; dirty = true; }).dimensions(4 + i * bw, 3, bw - 2, 16).build()); }
        int y = height - 44, x = 4;
        String[][] mv = {{"X-", "-1,0,0"}, {"X+", "1,0,0"}, {"Y-", "0,-1,0"}, {"Y+", "0,1,0"}, {"Z-", "0,0,-1"}, {"Z+", "0,0,1"}};
        for (String[] m : mv) {
            final String[] p = m[1].split(",");
            addDrawableChild(ButtonWidget.builder(Text.literal(m[0]), b -> act(() -> NeroEditor.movePart(selName(), Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])))).dimensions(x, y, 28, 18).build());
            x += 30;
        }
        x += 6;
        addDrawableChild(ButtonWidget.builder(Text.literal("Поворот 90°"), b -> act(() -> NeroEditor.rotatePart(selName()))).dimensions(x, y, 70, 18).build()); x += 72;
        addDrawableChild(ButtonWidget.builder(Text.literal("Дубль"), b -> act(() -> NeroEditor.dupPart(selName(), 2, 0, 2))).dimensions(x, y, 44, 18).build()); x += 46;
        addDrawableChild(ButtonWidget.builder(Text.literal("Удалить"), b -> act(() -> NeroEditor.removePart(selName()))).dimensions(x, y, 54, 18).build());
        int y2 = height - 22;
        addDrawableChild(ButtonWidget.builder(Text.literal("Отмена"), b -> { status = NeroAI.doUndo(true); reload(); }).dimensions(4, y2, 56, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Повтор"), b -> { status = NeroAI.doUndo(false); reload(); }).dimensions(62, y2, 56, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Камера"), b -> { yaw = 0.8; pitch = 0.55; panX = panY = 0; load(); }).dimensions(120, y2, 56, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("К построению"), b -> MinecraftClient.getInstance().setScreen(new NeroScreen())).dimensions(178, y2, 84, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Чат"), b -> MinecraftClient.getInstance().setScreen(new NeroChatScreen())).dimensions(264, y2, 40, 18).build());
        cmdField = new TextFieldWidget(textRenderer, width - 182, height - 68, 178, 16, Text.literal("команда"));
        cmdField.setMaxLength(2000);
        cmdField.setVisible(false);
        addDrawableChild(cmdField);
        applyCmd = ButtonWidget.builder(Text.literal("Применить команду"), b -> act(this::setCmd)).dimensions(width - 182, height - 50, 178, 16).build();
        applyCmd.visible = false;
        addDrawableChild(applyCmd);
    }

    private String selName() { return plan != null && selEl >= 0 && selEl < plan.elements.size() ? plan.elements.get(selEl).name() : ""; }

    private void act(java.util.function.Supplier<String> s) {
        if (selName().isEmpty()) { status = "Сначала выбери компонент (клик по блоку или в списке справа)"; return; }
        if (!NeroPatch.parts(NeroAI.project).contains(selName())) { status = "Компонент «" + selName() + "» — механизм или пульт из каталога: его положение меняется кнопками X/Y/Z проекта"; return; }
        status = s.get();
        reload();
    }

    private String setCmd() {
        if (selCb == null || selCb.cb == null) return "Выбери командный блок";
        int idx = 0;
        for (NeroBuildPlan.Cb k : plan.cbs) { if (k == selCb.cb) break; if (k.el() == selCb.cb.el()) idx++; }
        String chain = selCb.cb.el() >= 0 && selCb.cb.el() < plan.elements.size() ? plan.elements.get(selCb.cb.el()).name() : "";
        return NeroEditor.setCmd(chain, idx, cmdField.getText());
    }

    private void reload() { plan = null; load(); selCb = null; }

    // ------------------------------------------------------------------ проекция
    private double cy, sy, cp, sp;

    private double[] proj(double x, double y, double z) {
        double dx = x - tx, dy = y - ty, dz = z - tz;
        double x1 = dx * cy - dz * sy, z1 = dx * sy + dz * cy;
        double y2 = dy * cp - z1 * sp, z2 = dy * sp + z1 * cp;
        double depth = z2 + dist;
        if (depth < 0.5) return null;
        double f = height * 0.9;
        return new double[]{width * 0.38 + x1 * f / depth + panX, height * 0.5 - y2 * f / depth + panY, depth};
    }

    private static final int[][][] FACE = {
            {{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}}, {{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}},
            {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}}, {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}},
            {{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}}, {{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}}};
    private static final int[][] NRM = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final double[] SHADE = {0.75, 0.75, 1.0, 0.55, 0.85, 0.85};

    private int shade(int rgb, double s, int a) {
        int r = (int) (((rgb >> 16) & 255) * s), g = (int) (((rgb >> 8) & 255) * s), b = (int) ((rgb & 255) * s);
        return (a << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }

    private void rebuild() {
        quads.clear();
        truncated = false;
        cy = Math.cos(yaw); sy = Math.sin(yaw); cp = Math.cos(pitch); sp = Math.sin(pitch);
        int cap = mode == 2 ? 6000 : 26000;
        for (Vox v : vox) {
            if (!v.surface) continue;
            boolean isCb = v.cb != null;
            boolean sel = (selEl >= 0 && v.el == selEl) || (selCb != null && selCb == v);
            for (int f = 0; f < 6; f++) {
                if (occ.containsKey(key(v.x + NRM[f][0], v.y + NRM[f][1], v.z + NRM[f][2]))) continue;
                // видимость грани: нормаль в системе камеры
                double nx = NRM[f][0] * cy - NRM[f][2] * sy, nz0 = NRM[f][0] * sy + NRM[f][2] * cy;
                double nz = NRM[f][1] * sp + nz0 * cp;
                if (nz >= 0) continue;
                if (quads.size() >= cap) { truncated = true; break; }
                Quad q = new Quad();
                double dsum = 0;
                boolean ok = true;
                for (int i = 0; i < 4; i++) {
                    double[] p = proj(v.x + FACE[f][i][0], v.y + FACE[f][i][1], v.z + FACE[f][i][2]);
                    if (p == null) { ok = false; break; }
                    q.p[i * 2] = (float) p[0]; q.p[i * 2 + 1] = (float) p[1]; dsum += p[2];
                }
                if (!ok) continue;
                q.depth = (float) (dsum / 4);
                int rgb = v.rgb;
                switch (mode) {
                    case 0 -> { q.argb = (sel ? 0xB0 : 0x55) << 24 | (sel ? 0xFFE060 : 0x50DCFF); }
                    case 2 -> { q.argb = 0; q.edge = true; }
                    case 3 -> q.argb = shade(PAL[Math.floorMod(v.el, PAL.length)], SHADE[f], 235);
                    case 4 -> q.argb = isCb ? shade(rgb, SHADE[f], 255) : shade(0x888888, SHADE[f], 40);
                    default -> q.argb = shade(rgb, SHADE[f], 255);
                }
                if (sel && mode != 0) q.argb = shade(0xFFE060, SHADE[f], 255);
                quads.add(q);
            }
        }
        quads.sort((a, b) -> Float.compare(b.depth, a.depth));
        dirty = false;
    }

    private void line(BufferBuilder bb, Matrix4f m, float x1, float y1, float x2, float y2, float w, int argb) {
        float dx = x2 - x1, dy = y2 - y1, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float nx = -dy / len * w / 2, ny = dx / len * w / 2;
        int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        bb.vertex(m, x1 + nx, y1 + ny, 0).color(r, g, b, a).next();
        bb.vertex(m, x1 - nx, y1 - ny, 0).color(r, g, b, a).next();
        bb.vertex(m, x2 - nx, y2 - ny, 0).color(r, g, b, a).next();
        bb.vertex(m, x2 + nx, y2 + ny, 0).color(r, g, b, a).next();
    }

    private void drawScene(DrawContext ctx) {
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        Tessellator t = Tessellator.getInstance();
        BufferBuilder bb = t.getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Quad q : quads) {
            if (!q.edge && (q.argb >>> 24) > 0) {
                int a = q.argb >>> 24, r = (q.argb >> 16) & 255, g = (q.argb >> 8) & 255, b = q.argb & 255;
                for (int i = 0; i < 4; i++) bb.vertex(m, q.p[i * 2], q.p[i * 2 + 1], 0).color(r, g, b, a).next();
            }
            if (mode == 0 || mode == 2) {
                int ec = mode == 2 ? 0xFFB8E6FF : 0xC040E0FF;
                for (int i = 0; i < 4; i++) line(bb, m, q.p[i * 2], q.p[i * 2 + 1], q.p[((i + 1) % 4) * 2], q.p[((i + 1) % 4) * 2 + 1], 1f, ec);
            }
        }
        // связи командных блоков и компонентов
        if ((mode == 4 || mode == 3) && plan != null) {
            long time = System.currentTimeMillis();
            if (mode == 4) {
                NeroBuildPlan.Cb prev = null;
                for (NeroBuildPlan.Cb k : plan.cbs) {
                    if (prev != null && prev.el() == k.el()) {
                        double[] a = proj(prev.x() + 0.5, prev.y() + 0.5, prev.z() + 0.5), b = proj(k.x() + 0.5, k.y() + 0.5, k.z() + 0.5);
                        if (a != null && b != null) {
                            line(bb, m, (float) a[0], (float) a[1], (float) b[0], (float) b[1], 1.2f, 0xC040FFC0);
                            double ph = (time % 1500) / 1500.0;
                            float px = (float) (a[0] + (b[0] - a[0]) * ph), py = (float) (a[1] + (b[1] - a[1]) * ph);
                            line(bb, m, px - 2, py, px + 2, py, 4f, 0xFFFFFFFF);
                        }
                    }
                    prev = k;
                }
            } else {
                for (int e = 0; e < plan.elements.size(); e++)
                    for (int dd : plan.elements.get(e).deps()) {
                        if (dd < 0 || dd >= centers.length || centers[e][3] == 0 || centers[dd][3] == 0) continue;
                        double[] a = proj(centers[e][0] / centers[e][3], centers[e][1] / centers[e][3], centers[e][2] / centers[e][3]), b = proj(centers[dd][0] / centers[dd][3], centers[dd][1] / centers[dd][3], centers[dd][2] / centers[dd][3]);
                        if (a != null && b != null) line(bb, m, (float) a[0], (float) a[1], (float) b[0], (float) b[1], 1.2f, 0xB0FFFFFF);
                    }
            }
        }
        t.draw();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        ctx.fill(0, 0, width, height, 0xFF0E1118);
        if (dirty) rebuild();
        drawScene(ctx);
        // подписи
        if (plan != null) {
            if (mode == 3) for (int e = 0; e < centers.length; e++) {
                if (centers[e][3] == 0) continue;
                double[] p = proj(centers[e][0] / centers[e][3], centers[e][1] / centers[e][3], centers[e][2] / centers[e][3]);
                if (p != null) ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(plan.elements.get(e).name(), 110), (int) p[0] - 20, (int) p[1], e == selEl ? 0xFFE060 : 0xFFFFFF);
            }
            if (mode == 4) {
                int n = 0;
                for (NeroBuildPlan.Cb k : plan.cbs) {
                    if (n >= 70) break;
                    double[] p = proj(k.x() + 0.5, k.y() + 1.2, k.z() + 0.5);
                    if (p == null || p[2] > dist * 1.5) continue;
                    ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(k.label(), 90), (int) p[0] - 18, (int) p[1], cbColor(k.type()));
                    n++;
                }
            }
        }
        // правая панель
        int px = width - 184;
        ctx.fill(px - 2, 22, width, height - 70, 0xAA000000);
        ctx.drawTextWithShadow(textRenderer, "Компоненты", px, 24, 0x55FFFF);
        int rows = Math.max(3, (height - 70 - 120 - 34) / 10);
        if (plan != null) for (int i = 0; i < rows; i++) {
            int idx = listScroll + i;
            if (idx >= plan.elements.size()) break;
            int y = 36 + i * 10;
            if (idx == selEl) ctx.fill(px - 1, y - 1, width - 2, y + 9, 0x8833AAFF);
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(plan.elements.get(idx).name(), 176), px, y, centers.length > idx && centers[idx][3] > 0 ? 0xFFFFFF : 0x999999);
        }
        int iy = 36 + rows * 10 + 4;
        ctx.fill(px - 2, iy - 2, width, iy + 112, 0x66000000);
        if (selCb != null && selCb.cb != null) {
            NeroBuildPlan.Cb k = selCb.cb;
            drawLines(ctx, px, iy, new String[]{"КБ: " + k.label(), k.role(), "Команда: " + k.cmd()}, 0xFFD055);
        } else if (selEl >= 0 && plan != null && selEl < plan.elements.size()) {
            NeroBuildPlan.Element e = plan.elements.get(selEl);
            drawLines(ctx, px, iy, new String[]{e.name(), "Запуск: " + e.activation(), "Работа: " + e.logic(), "Блоков в нём: " + countEl(selEl)}, 0xFFD055);
        } else drawLines(ctx, px, iy, new String[]{"Выбери компонент в списке или кликом по блоку"}, 0xAAAAAA);
        boolean cbSel = selCb != null && selCb.cb != null;
        cmdField.setVisible(cbSel);
        applyCmd.visible = cbSel;
        if (cbSel && !cmdField.isFocused() && cmdField.getText().isEmpty()) cmdField.setText(selCb.cb.cmd());
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(status, width - 200), 4, height - 64, 0xFFD055);
        String info = MODES[mode] + (truncated ? " · показана часть граней" : "") + " · блоков " + vox.size();
        ctx.drawTextWithShadow(textRenderer, info, 4, 22, 0x88AAFF);
        super.render(ctx, mx, my, d);
    }

    private int countEl(int e) { int n = 0; for (Vox v : vox) if (v.el == e && v.cb == null) n++; return n; }

    private void drawLines(DrawContext ctx, int x, int y, String[] lines, int color) {
        int yy = y;
        for (String l : lines)
            for (net.minecraft.text.OrderedText o : textRenderer.wrapLines(Text.literal(l), 176)) {
                if (yy > y + 104) return;
                ctx.drawTextWithShadow(textRenderer, o, x, yy, color);
                yy += 9;
            }
    }

    // ------------------------------------------------------------------ ввод
    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (mx > width - 186) return super.mouseDragged(mx, my, button, dx, dy);
        if (button == 0) { yaw += dx * 0.01; pitch = Math.max(-1.5, Math.min(1.5, pitch + dy * 0.01)); dirty = true; return true; }
        if (button == 1) { panX += dx; panY += dy; return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    public boolean mouseScrolled(double mx, double my, double amount) { return zoom(mx, amount); }

    public boolean mouseScrolled(double mx, double my, double h, double v) { return zoom(mx, v); }

    private boolean zoom(double mx, double amount) {
        if (mx > width - 186) { listScroll = Math.max(0, listScroll + (amount > 0 ? -2 : 2)); return true; }
        dist = Math.max(4, Math.min(400, dist * Math.pow(0.9, amount)));
        dirty = true;
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        pressX = mx; pressY = my;
        if (super.mouseClicked(mx, my, button)) return true;
        int px = width - 184;
        if (mx >= px && my >= 36 && plan != null) {
            int rows = Math.max(3, (height - 70 - 120 - 34) / 10);
            int r = (int) ((my - 36) / 10);
            if (r >= 0 && r < rows && listScroll + r < plan.elements.size()) { selEl = listScroll + r; selCb = null; cmdField.setText(""); dirty = true; return true; }
        }
        return mx < px;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && mx < width - 186 && my > 22 && my < height - 48 && Math.abs(mx - pressX) < 4 && Math.abs(my - pressY) < 4) pick(mx, my);
        return super.mouseReleased(mx, my, button);
    }

    private void pick(double mx, double my) {
        Vox best = null;
        double bd = Double.MAX_VALUE;
        for (Vox v : vox) {
            if (!v.surface) continue;
            if (mode == 4 && v.cb == null) continue;
            double[] p = proj(v.x + 0.5, v.y + 0.5, v.z + 0.5);
            if (p == null) continue;
            double half = Math.max(3, height * 0.9 / p[2] * 0.55);
            if (Math.abs(p[0] - mx) <= half && Math.abs(p[1] - my) <= half && p[2] < bd) { bd = p[2]; best = v; }
        }
        if (best == null) { selEl = -1; selCb = null; cmdField.setText(""); }
        else { selEl = best.el; selCb = best.cb != null ? best : null; cmdField.setText(""); status = best.cb != null ? "Командный блок: " + best.cb.label() : "Компонент: " + selName(); }
        dirty = true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (cmdField != null && cmdField.isFocused()) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == 263) { yaw -= 0.1; dirty = true; return true; }
        if (keyCode == 262) { yaw += 0.1; dirty = true; return true; }
        if (keyCode == 265) { pitch = Math.min(1.5, pitch + 0.1); dirty = true; return true; }
        if (keyCode == 264) { pitch = Math.max(-1.5, pitch - 0.1); dirty = true; return true; }
        if (keyCode == 61 || keyCode == 334) { dist = Math.max(4, dist * 0.9); dirty = true; return true; }
        if (keyCode == 45 || keyCode == 333) { dist = Math.min(400, dist * 1.1); dirty = true; return true; }
        if (keyCode == 261 && !selName().isEmpty()) { act(() -> NeroEditor.removePart(selName())); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
