package com.mackros;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.List;

/** Меню конструктора: выбор механизма, параметры, информация и карта для выбора места постройки. */
public class RigScreen extends Screen {
    private static final int N = 48;
    private static final int[] ZOOM = {1, 2, 4, 8};
    private static final int CS = 2;

    private Rig.Def def;
    private int[] prm;
    private Rig.Plan prev;
    private int zi = 1, ctrX, ctrZ, selX, selZ, yoff, scroll;
    private boolean has;
    private final int[] cols = new int[N * N];
    private int ox, oy;

    public RigScreen() { this("lift"); }

    public RigScreen(String key) {
        super(Text.literal("Конструктор Mackros"));
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) { ctrX = mc.player.getBlockX(); ctrZ = mc.player.getBlockZ(); }
        setDef(Rig.find(key));
        int idx = Rig.ALL.indexOf(def);
        scroll = Math.max(0, Math.min(idx, Rig.ALL.size() - 9));
    }

    private void setDef(Rig.Def d) {
        def = d;
        prm = Rig.defaults(d);
        replan();
    }

    private void replan() { prev = def.gen().make(new BlockPos(0, 0, 0), "pv", prm); }

    private BlockPos base() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int y = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, selX, selZ);
        return new BlockPos(selX, y + yoff, selZ);
    }

    private void refresh() {
        ClientWorld w = MinecraftClient.getInstance().world;
        if (w == null) return;
        int z0 = ZOOM[zi];
        for (int cz = 0; cz < N; cz++)
            for (int cx = 0; cx < N; cx++) {
                int x = ctrX + (cx - N / 2) * z0, z = ctrZ + (cz - N / 2) * z0;
                if (!w.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) { cols[cz * N + cx] = 0xFF1A1A1A; continue; }
                int y = w.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
                BlockPos p = new BlockPos(x, y - 1, z);
                BlockState s = w.getBlockState(p);
                int rgb = s.getMapColor(w, p).color;
                if (rgb == 0) rgb = 0x333333;
                int yn = w.getTopY(Heightmap.Type.WORLD_SURFACE, x, z - z0);
                double k = y > yn ? 1.18 : y < yn ? 0.82 : 1.0;
                int r = Math.min(255, (int) (((rgb >> 16) & 255) * k)), g = Math.min(255, (int) (((rgb >> 8) & 255) * k)), b = Math.min(255, (int) ((rgb & 255) * k));
                cols[cz * N + cx] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
    }

    private ButtonWidget btn(String label, int x, int y, int w, int h, Runnable r) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), bb -> r.run()).dimensions(x, y, w, h).build();
        addDrawableChild(b);
        return b;
    }

    @Override
    protected void init() {
        ox = width / 2 - 190;
        oy = Math.max(6, (height - 196) / 2);
        refresh();
        int total = Rig.ALL.size();
        for (int i = 0; i < 9; i++) {
            int idx = scroll + i;
            if (idx >= total) break;
            final Rig.Def d = Rig.ALL.get(idx);
            String label = (d == def ? "> " : "") + textRenderer.trimToWidth(d.name(), 100);
            btn(label, ox, oy + 12 + i * 16, 118, 15, () -> { setDef(d); clearAndInit(); });
        }
        btn("^", ox, oy + 158, 56, 14, () -> { scroll = Math.max(0, scroll - 3); clearAndInit(); });
        btn("v", ox + 62, oy + 158, 56, 14, () -> { scroll = Math.min(Math.max(0, total - 9), scroll + 3); clearAndInit(); });

        int mx = ox + 122, ny = oy + 12 + N * CS + 2;
        int sh = 8 * ZOOM[zi];
        btn("<", mx, ny, 13, 14, () -> { ctrX -= sh; refresh(); });
        btn(">", mx + 14, ny, 13, 14, () -> { ctrX += sh; refresh(); });
        btn("^", mx + 28, ny, 13, 14, () -> { ctrZ -= sh; refresh(); });
        btn("v", mx + 42, ny, 13, 14, () -> { ctrZ += sh; refresh(); });
        btn("Я", mx + 56, ny, 13, 14, () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) { ctrX = mc.player.getBlockX(); ctrZ = mc.player.getBlockZ(); }
            refresh();
        });
        btn("-", mx + 70, ny, 13, 14, () -> { zi = Math.min(ZOOM.length - 1, zi + 1); refresh(); });
        btn("+", mx + 84, ny, 13, 14, () -> { zi = Math.max(0, zi - 1); refresh(); });

        for (int k = 0; k < prm.length; k++) {
            final int kk = k;
            int y = oy + 128 + k * 15;
            btn("-", mx, y, 12, 13, () -> { prm[kk] = Math.max(def.mn()[kk], prm[kk] - def.st()[kk]); replan(); });
            btn("+", mx + 86, y, 12, 13, () -> { prm[kk] = Math.min(def.mx()[kk], prm[kk] + def.st()[kk]); replan(); });
        }

        int rx = ox + 222;
        btn("Построить", rx, oy + 110, 74, 14, () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!has) { Editor.msg("Сначала кликни по карте: выбери место"); return; }
            Rig.build(mc, def, prm, base());
        });
        btn("Отменить", rx + 76, oy + 110, 74, 14, () -> Rig.undo(MinecraftClient.getInstance()));
        String[] sn = {"Выделение", "Буфер", "Схемы", "Выдать", "Меню Mackros", "Закрыть"};
        for (int i = 0; i < sn.length; i++) {
            final int ii = i;
            btn(sn[i], rx + (i % 2) * 76, oy + 128 + (i / 2) * 16, 74, 14, () -> {
                switch (ii) {
                    case 0 -> MackrosScreen.openCat(0);
                    case 1 -> MackrosScreen.openCat(1);
                    case 2 -> MackrosScreen.openCat(4);
                    case 3 -> MackrosScreen.openCat(10);
                    case 4 -> MinecraftClient.getInstance().setScreen(new MackrosScreen());
                    default -> close();
                }
            });
        }
        btn("Y-", rx, oy + 176, 36, 14, () -> yoff--);
        btn("Y+", rx + 38, oy + 176, 36, 14, () -> yoff++);
        btn("На меня", rx + 76, oy + 176, 74, 14, () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) { selX = mc.player.getBlockX(); selZ = mc.player.getBlockZ(); has = true; }
        });
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x0 = ox + 122, y0 = oy + 12;
        if (mx >= x0 && my >= y0 && mx < x0 + N * CS && my < y0 + N * CS) {
            int cx = (int) ((mx - x0) / CS), cz = (int) ((my - y0) / CS);
            selX = ctrX + (cx - N / 2) * ZOOM[zi];
            selZ = ctrZ + (cz - N / 2) * ZOOM[zi];
            has = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private int cellX(int wx) { return Math.floorDiv(wx - ctrX, ZOOM[zi]) + N / 2; }
    private int cellZ(int wz) { return Math.floorDiv(wz - ctrZ, ZOOM[zi]) + N / 2; }

    @Override
    public void render(DrawContext ctx, int mxp, int myp, float d) {
        renderBackground(ctx, mxp, myp, d);
        int x0 = ox + 122, y0 = oy + 12;
        ctx.drawTextWithShadow(textRenderer, "Конструктор механизмов", ox, oy, 0x55FFFF);
        ctx.drawTextWithShadow(textRenderer, "Карта (кликни место), 1 клетка = " + ZOOM[zi] + " бл.", x0, oy, 0xAAAAAA);

        ctx.fill(x0 - 1, y0 - 1, x0 + N * CS + 1, y0 + N * CS + 1, 0xFFFFFFFF);
        for (int cz = 0; cz < N; cz++)
            for (int cx = 0; cx < N; cx++)
                ctx.fill(x0 + cx * CS, y0 + cz * CS, x0 + cx * CS + CS, y0 + cz * CS + CS, cols[cz * N + cx]);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            int pcx = cellX(mc.player.getBlockX()), pcz = cellZ(mc.player.getBlockZ());
            if (pcx >= 0 && pcx < N && pcz >= 0 && pcz < N) ctx.fill(x0 + pcx * CS - 1, y0 + pcz * CS - 1, x0 + pcx * CS + 3, y0 + pcz * CS + 3, 0xFF00FFFF);
        }
        if (has) {
            int a = Math.max(0, Math.min(N - 1, cellX(selX + prev.minX))), b = Math.max(0, Math.min(N - 1, cellX(selX + prev.maxX)));
            int c = Math.max(0, Math.min(N - 1, cellZ(selZ + prev.minZ))), e = Math.max(0, Math.min(N - 1, cellZ(selZ + prev.maxZ)));
            ctx.fill(x0 + a * CS, y0 + c * CS, x0 + b * CS + CS, y0 + e * CS + CS, 0x66FF3030);
            int scx = cellX(selX), scz = cellZ(selZ);
            if (scx >= 0 && scx < N && scz >= 0 && scz < N) ctx.fill(x0 + scx * CS - 1, y0 + scz * CS - 1, x0 + scx * CS + 3, y0 + scz * CS + 3, 0xFFFFFF00);
        }

        for (int k = 0; k < prm.length; k++) {
            String t = textRenderer.trimToWidth(def.pn()[k] + ": " + prm[k], 70);
            ctx.drawTextWithShadow(textRenderer, t, x0 + 15, oy + 131 + k * 15, 0xFFFFFF);
        }

        int rx = ox + 222, y = oy + 12, lim = oy + 106;
        ctx.drawTextWithShadow(textRenderer, def.name(), rx, y, 0xFFFF55);
        y += 10;
        ctx.drawTextWithShadow(textRenderer, def.cat(), rx, y, 0xAAAAAA);
        y += 10;
        for (OrderedText l : textRenderer.wrapLines(Text.literal(def.desc()), 150)) {
            if (y > lim - 9) break;
            ctx.drawTextWithShadow(textRenderer, l, rx, y, 0xFFFFFF);
            y += 9;
        }
        String st = "КБ: " + prev.cb + "  блоков: " + prev.other + "  размер " + (prev.maxX - prev.minX + 1) + "x" + (prev.maxY - prev.minY + 1) + "x" + (prev.maxZ - prev.minZ + 1);
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(st, 150), rx, Math.min(y + 2, lim - 9), 0x55FF55);
        String pt = has ? "Точка: " + selX + " " + base().getY() + " " + selZ : "Точка не выбрана";
        ctx.drawTextWithShadow(textRenderer, pt, rx, oy + 177 + 15, has ? 0xFFAA00 : 0xFF5555);
        super.render(ctx, mxp, myp, d);
    }
}
