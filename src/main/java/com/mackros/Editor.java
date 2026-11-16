package com.mackros;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class Editor {
    public record Entry(int x, int y, int z, BlockState s) {}
    public record Change(BlockPos p, BlockState o, BlockState n) {}

    public static BlockPos pos1, pos2;
    public static List<Entry> clip = new ArrayList<>();
    public static int ox, oy, oz;
    public static boolean preview = true, pasteAir = false, symX = false, symZ = false;
    public static int cx, cz, radius = 5, height = 5, step = 1, slot = 1, stackN = 3;
    static final long MAX = 3_000_000L;

    static final ArrayDeque<List<Change>> undo = new ArrayDeque<>();
    static final ArrayDeque<List<Change>> redo = new ArrayDeque<>();
    static final ArrayDeque<String> queue = new ArrayDeque<>();

    // ---------- util ----------
    public static void msg(String s) {
        var p = MinecraftClient.getInstance().player;
        if (p != null) p.sendMessage(Text.literal("§b[Mackros] §f" + s), true);
    }

    public static void cmd(MinecraftClient mc, String c) {
        if (mc.player != null) mc.player.networkHandler.sendChatCommand(c);
    }

    public static BlockPos look(MinecraftClient mc, boolean face) {
        Entity e = mc.getCameraEntity();
        if (e == null) return null;
        HitResult h = e.raycast(128, 1f, false);
        if (h instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
            return face ? b.getBlockPos().offset(b.getSide()) : b.getBlockPos();
        }
        return null;
    }

    public static BlockPos anchor(MinecraftClient mc) {
        BlockPos l = look(mc, true);
        return l == null ? null : l.add(ox, oy, oz);
    }

    public static BlockState held(MinecraftClient mc) {
        ItemStack st = mc.player.getMainHandStack();
        if (st.getItem() instanceof BlockItem bi) return bi.getBlock().getDefaultState();
        return Blocks.AIR.getDefaultState();
    }

    public static void setPos(MinecraftClient mc, int n) {
        BlockPos p = look(mc, false);
        if (p == null) { msg("Нет блока в прицеле"); return; }
        if (n == 1) pos1 = p; else pos2 = p;
        msg("Точка " + n + ": " + p.getX() + " " + p.getY() + " " + p.getZ());
    }

    public static void resetSel() { pos1 = null; pos2 = null; msg("Выделение сброшено"); }

    static boolean sel() {
        if (pos1 == null || pos2 == null) { msg("Сначала поставь точки 1 и 2"); return false; }
        if (vol() > MAX) { msg("Слишком большая область"); return false; }
        return true;
    }

    public static BlockPos min() {
        return new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
    }

    public static BlockPos max() {
        return new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
    }

    public static long vol() {
        if (pos1 == null || pos2 == null) return 0;
        BlockPos a = min(), b = max();
        return (long) (b.getX() - a.getX() + 1) * (b.getY() - a.getY() + 1) * (b.getZ() - a.getZ() + 1);
    }

    public static int[] size() {
        int mx = 0, my = 0, mz = 0;
        for (Entry e : clip) { mx = Math.max(mx, e.x() + 1); my = Math.max(my, e.y() + 1); mz = Math.max(mz, e.z() + 1); }
        return new int[]{mx, my, mz};
    }

    public static void togglePreview() { preview = !preview; msg("Предпросмотр: " + (preview ? "вкл" : "выкл")); }

    public static void nudge(int dx, int dy, int dz) { ox += dx; oy += dy; oz += dz; msg("Смещение вставки: " + ox + " " + oy + " " + oz); }

    public static void resetNudge() { ox = 0; oy = 0; oz = 0; msg("Смещение сброшено"); }

    // ---------- symmetry ----------
    public static void cycleSym(MinecraftClient mc) {
        int mode = (symX ? 1 : 0) + (symZ ? 2 : 0);
        mode = (mode + 1) % 4;
        symX = (mode & 1) != 0; symZ = (mode & 2) != 0;
        cx = mc.player.getBlockPos().getX(); cz = mc.player.getBlockPos().getZ();
        msg("Симметрия: " + (mode == 0 ? "выкл" : (symX ? "X " : "") + (symZ ? "Z" : "")) + " (центр " + cx + " " + cz + ")");
    }

    public static void toggleSymAxis(MinecraftClient mc, boolean xAxis) {
        if (xAxis) symX = !symX; else symZ = !symZ;
        cx = mc.player.getBlockPos().getX(); cz = mc.player.getBlockPos().getZ();
        msg("Симметрия X=" + symX + " Z=" + symZ);
    }

    private static void add(MinecraftClient mc, List<Change> out, BlockPos p, BlockState s) {
        BlockState old = mc.world.getBlockState(p);
        if (old == s) return;
        out.add(new Change(p, old, s));
    }

    static void addSym(MinecraftClient mc, List<Change> out, BlockPos p, BlockState s) {
        add(mc, out, p, s);
        int x = p.getX(), y = p.getY(), z = p.getZ();
        if (symX) add(mc, out, new BlockPos(2 * cx - x, y, z), s.mirror(BlockMirror.FRONT_BACK));
        if (symZ) add(mc, out, new BlockPos(x, y, 2 * cz - z), s.mirror(BlockMirror.LEFT_RIGHT));
        if (symX && symZ) add(mc, out, new BlockPos(2 * cx - x, y, 2 * cz - z), s.mirror(BlockMirror.FRONT_BACK).mirror(BlockMirror.LEFT_RIGHT));
    }

    // ---------- apply / undo ----------
    static void apply(MinecraftClient mc, List<Change> ch) {
        if (ch.isEmpty()) { msg("Нечего менять"); return; }
        undo.push(ch); redo.clear();
        while (undo.size() > 50) undo.removeLast();
        send(mc, ch, false);
        msg("Изменено блоков: " + ch.size());
    }

    static void send(MinecraftClient mc, List<Change> ch, boolean rev) {
        IntegratedServer srv = mc.getServer();
        if (srv != null) {
            ServerWorld w = srv.getWorld(mc.world.getRegistryKey());
            if (w != null) {
                srv.execute(() -> {
                    int n = ch.size();
                    for (int i = 0; i < n; i++) {
                        Change c = ch.get(rev ? n - 1 - i : i);
                        w.setBlockState(c.p(), rev ? c.o() : c.n(), Block.NOTIFY_ALL);
                    }
                });
                return;
            }
        }
        int n = ch.size();
        for (int i = 0; i < n; i++) {
            Change c = ch.get(rev ? n - 1 - i : i);
            BlockPos p = c.p();
            queue.add("setblock " + p.getX() + " " + p.getY() + " " + p.getZ() + " "
                    + BlockArgumentParser.stringifyBlockState(rev ? c.o() : c.n()));
        }
    }

    public static void tickQueue(MinecraftClient mc) {
        for (int i = 0; i < 12 && !queue.isEmpty(); i++) cmd(mc, queue.poll());
    }

    public static void undo(MinecraftClient mc) {
        if (undo.isEmpty()) { msg("Нечего отменять"); return; }
        List<Change> c = undo.pop(); redo.push(c); send(mc, c, true); msg("Отмена");
    }

    public static void redo(MinecraftClient mc) {
        if (redo.isEmpty()) { msg("Нечего повторять"); return; }
        List<Change> c = redo.pop(); undo.push(c); send(mc, c, false); msg("Повтор");
    }

    // ---------- selection ops ----------
    public static void fill(MinecraftClient mc, BlockState s) {
        if (!sel()) return;
        BlockPos a = min(), b = max();
        List<Change> ch = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++)
                    addSym(mc, ch, new BlockPos(x, y, z), s);
        apply(mc, ch);
    }

    public static void hollow(MinecraftClient mc, BlockState s) {
        if (!sel()) return;
        BlockPos a = min(), b = max();
        List<Change> ch = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++) {
                    boolean edge = x == a.getX() || x == b.getX() || y == a.getY() || y == b.getY() || z == a.getZ() || z == b.getZ();
                    if (edge) addSym(mc, ch, new BlockPos(x, y, z), s);
                }
        apply(mc, ch);
    }

    public static void replace(MinecraftClient mc) {
        if (!sel()) return;
        BlockPos t = look(mc, false);
        if (t == null) { msg("Наведись на блок, который заменять"); return; }
        Block target = mc.world.getBlockState(t).getBlock();
        BlockState to = held(mc);
        BlockPos a = min(), b = max();
        List<Change> ch = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (mc.world.getBlockState(p).isOf(target)) addSym(mc, ch, p, to);
                }
        apply(mc, ch);
    }

    public static void moveSel(MinecraftClient mc, int dx, int dy, int dz) {
        if (!sel()) return;
        BlockPos a = min(), b = max();
        List<BlockPos> ps = new ArrayList<>();
        List<BlockState> ss = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    ps.add(p); ss.add(mc.world.getBlockState(p));
                }
        List<Change> ch = new ArrayList<>();
        BlockState air = Blocks.AIR.getDefaultState();
        for (int i = 0; i < ps.size(); i++) if (!ss.get(i).isAir()) ch.add(new Change(ps.get(i), ss.get(i), air));
        for (int i = 0; i < ps.size(); i++) {
            if (ss.get(i).isAir()) continue;
            BlockPos np = ps.get(i).add(dx, dy, dz);
            ch.add(new Change(np, mc.world.getBlockState(np), ss.get(i)));
        }
        pos1 = pos1.add(dx, dy, dz); pos2 = pos2.add(dx, dy, dz);
        apply(mc, ch);
    }

    public static void stack(MinecraftClient mc) {
        if (!sel()) return;
        BlockPos a = min(), b = max();
        Direction d = mc.player.getHorizontalFacing();
        int sx = b.getX() - a.getX() + 1, sz = b.getZ() - a.getZ() + 1;
        List<Change> ch = new ArrayList<>();
        for (int i = 1; i <= stackN; i++)
            for (int x = a.getX(); x <= b.getX(); x++)
                for (int y = a.getY(); y <= b.getY(); y++)
                    for (int z = a.getZ(); z <= b.getZ(); z++) {
                        BlockState s = mc.world.getBlockState(new BlockPos(x, y, z));
                        BlockPos t = new BlockPos(x + d.getOffsetX() * sx * i, y, z + d.getOffsetZ() * sz * i);
                        addSym(mc, ch, t, s);
                    }
        apply(mc, ch);
    }

    // ---------- clipboard ----------
    public static void copy(MinecraftClient mc) {
        if (!sel()) return;
        BlockPos a = min(), b = max();
        List<Entry> n = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++)
                    n.add(new Entry(x - a.getX(), y - a.getY(), z - a.getZ(), mc.world.getBlockState(new BlockPos(x, y, z))));
        clip = n; ox = oy = oz = 0;
        msg("Скопировано блоков: " + clip.size());
    }

    public static void cut(MinecraftClient mc) {
        copy(mc);
        if (!clip.isEmpty()) fill(mc, Blocks.AIR.getDefaultState());
    }

    public static void paste(MinecraftClient mc) {
        if (clip.isEmpty()) { msg("Буфер пуст"); return; }
        BlockPos base = anchor(mc);
        if (base == null) { msg("Нет блока в прицеле"); return; }
        List<Change> ch = new ArrayList<>();
        for (Entry e : clip) {
            if (!pasteAir && e.s().isAir()) continue;
            addSym(mc, ch, base.add(e.x(), e.y(), e.z()), e.s());
        }
        apply(mc, ch);
    }

    public static void rotate() {
        List<Entry> n = new ArrayList<>();
        for (Entry e : clip) n.add(new Entry(-e.z(), e.y(), e.x(), e.s().rotate(BlockRotation.CLOCKWISE_90)));
        clip = normalize(n); msg("Буфер повёрнут на 90°");
    }

    public static void flip(boolean xAxis) {
        List<Entry> n = new ArrayList<>();
        for (Entry e : clip) {
            if (xAxis) n.add(new Entry(-e.x(), e.y(), e.z(), e.s().mirror(BlockMirror.FRONT_BACK)));
            else n.add(new Entry(e.x(), e.y(), -e.z(), e.s().mirror(BlockMirror.LEFT_RIGHT)));
        }
        clip = normalize(n); msg("Буфер отражён по " + (xAxis ? "X" : "Z"));
    }

    private static List<Entry> normalize(List<Entry> l) {
        if (l.isEmpty()) return l;
        int mx = Integer.MAX_VALUE, my = Integer.MAX_VALUE, mz = Integer.MAX_VALUE;
        for (Entry e : l) { mx = Math.min(mx, e.x()); my = Math.min(my, e.y()); mz = Math.min(mz, e.z()); }
        List<Entry> r = new ArrayList<>();
        for (Entry e : l) r.add(new Entry(e.x() - mx, e.y() - my, e.z() - mz, e.s()));
        return r;
    }

    // ---------- shapes ----------
    public static void sphere(MinecraftClient mc, boolean hollow) { sphereImpl(mc, hollow, false); }

    public static void dome(MinecraftClient mc, boolean hollow) { sphereImpl(mc, hollow, true); }

    private static void sphereImpl(MinecraftClient mc, boolean hollow, boolean half) {
        BlockPos c = look(mc, false);
        if (c == null) { msg("Нет блока в прицеле"); return; }
        BlockState s = held(mc);
        int r = radius;
        List<Change> ch = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    if (half && dy < 0) continue;
                    int d = dx * dx + dy * dy + dz * dz;
                    if (d > r * r + 1) continue;
                    if (hollow && d <= (r - 1) * (r - 1)) continue;
                    addSym(mc, ch, c.add(dx, dy, dz), s);
                }
        apply(mc, ch);
    }

    public static void cylinder(MinecraftClient mc) {
        BlockPos c = look(mc, true);
        if (c == null) { msg("Нет блока в прицеле"); return; }
        BlockState s = held(mc);
        int r = radius;
        List<Change> ch = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + 1) continue;
                for (int y = 0; y < height; y++) addSym(mc, ch, c.add(dx, y, dz), s);
            }
        apply(mc, ch);
    }

    // ---------- save / load ----------
    static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("mackros"); }

    public static void save() {
        if (clip.isEmpty()) { msg("Буфер пуст"); return; }
        try {
            Files.createDirectories(dir());
            StringBuilder sb = new StringBuilder();
            for (Entry e : clip)
                sb.append(e.x()).append(' ').append(e.y()).append(' ').append(e.z()).append(' ')
                        .append(BlockArgumentParser.stringifyBlockState(e.s())).append('\n');
            Files.writeString(dir().resolve("slot" + slot + ".mkr"), sb.toString());
            msg("Сохранено в слот " + slot);
        } catch (Exception ex) { msg("Ошибка сохранения: " + ex.getMessage()); }
    }

    public static void load() {
        try {
            Path f = dir().resolve("slot" + slot + ".mkr");
            if (!Files.exists(f)) { msg("Слот " + slot + " пуст"); return; }
            List<Entry> n = new ArrayList<>();
            for (String line : Files.readAllLines(f)) {
                String[] p = line.split(" ", 4);
                if (p.length < 4) continue;
                BlockState s = BlockArgumentParser.block(Registries.BLOCK.getReadOnlyWrapper(), p[3], false).blockState();
                n.add(new Entry(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), s));
            }
            clip = n; ox = oy = oz = 0;
            msg("Загружен слот " + slot + ": " + clip.size() + " блоков");
        } catch (Exception ex) { msg("Ошибка загрузки: " + ex.getMessage()); }
    }


    // ---------- extra tools ----------
    static final java.util.Random RND = new java.util.Random();
    public static BlockPos mark;
    public static String markDim;

    static List<BlockState> palette(MinecraftClient mc) {
        List<BlockState> l = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack st = mc.player.getInventory().getStack(i);
            if (st.getItem() instanceof BlockItem bi) l.add(bi.getBlock().getDefaultState());
        }
        return l;
    }

    public static void paletteFill(MinecraftClient mc, boolean gradient) {
        if (!sel()) return;
        List<BlockState> pal = palette(mc);
        if (pal.isEmpty()) { msg("Положи блоки в хотбар"); return; }
        BlockPos a = min(), b = max();
        int h = Math.max(1, b.getY() - a.getY());
        List<Change> ch = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++) {
                    int idx;
                    if (gradient) {
                        idx = Math.round((y - a.getY()) / (float) h * (pal.size() - 1));
                        if (RND.nextInt(5) == 0) idx += RND.nextBoolean() ? 1 : -1;
                        idx = Math.max(0, Math.min(pal.size() - 1, idx));
                    } else idx = RND.nextInt(pal.size());
                    addSym(mc, ch, new BlockPos(x, y, z), pal.get(idx));
                }
        apply(mc, ch);
    }

    public static void paletteReplace(MinecraftClient mc) {
        if (!sel()) return;
        BlockPos t = look(mc, false);
        if (t == null) { msg("Наведись на заменяемый блок"); return; }
        List<BlockState> pal = palette(mc);
        if (pal.isEmpty()) { msg("Положи блоки в хотбар"); return; }
        Block target = mc.world.getBlockState(t).getBlock();
        BlockPos a = min(), b = max();
        List<Change> ch = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (mc.world.getBlockState(p).isOf(target)) addSym(mc, ch, p, pal.get(RND.nextInt(pal.size())));
                }
        apply(mc, ch);
    }

    public static void line(MinecraftClient mc) {
        if (pos1 == null || pos2 == null) { msg("Сначала поставь точки 1 и 2"); return; }
        BlockState s = held(mc);
        int dx = pos2.getX() - pos1.getX(), dy = pos2.getY() - pos1.getY(), dz = pos2.getZ() - pos1.getZ();
        int n = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        List<Change> ch = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : (double) i / n;
            BlockPos p = new BlockPos((int) Math.round(pos1.getX() + dx * t), (int) Math.round(pos1.getY() + dy * t), (int) Math.round(pos1.getZ() + dz * t));
            addSym(mc, ch, p, s);
        }
        apply(mc, ch);
    }

    public static void ellipsoid(MinecraftClient mc, boolean hollow, boolean dome) {
        if (!sel()) return;
        BlockState s = held(mc);
        BlockPos a = min(), b = max();
        double cx0 = (a.getX() + b.getX()) / 2.0, cz0 = (a.getZ() + b.getZ()) / 2.0;
        double rx = (b.getX() - a.getX() + 1) / 2.0, rz = (b.getZ() - a.getZ() + 1) / 2.0;
        double cy = dome ? a.getY() : (a.getY() + b.getY()) / 2.0;
        double ry = dome ? (b.getY() - a.getY() + 1) : (b.getY() - a.getY() + 1) / 2.0;
        List<Change> ch = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++) {
                    double nx = (x - cx0) / rx, ny = (y - cy) / ry, nz = (z - cz0) / rz;
                    if (nx * nx + ny * ny + nz * nz > 1.0) continue;
                    if (hollow) {
                        double ix = (x - cx0) / Math.max(0.5, rx - 1), iy = (y - cy) / Math.max(0.5, ry - 1), iz = (z - cz0) / Math.max(0.5, rz - 1);
                        if (ix * ix + iy * iy + iz * iz < 1.0) continue;
                    }
                    addSym(mc, ch, new BlockPos(x, y, z), s);
                }
        apply(mc, ch);
    }

    public static void smooth(MinecraftClient mc, int iter) {
        if (!sel()) return;
        BlockPos a = min(), b = max();
        int w = b.getX() - a.getX() + 1, d = b.getZ() - a.getZ() + 1;
        if (w > 512 || d > 512) { msg("Слишком большая область"); return; }
        int[][] h = new int[w][d];
        int[][] orig = new int[w][d];
        for (int i = 0; i < w; i++)
            for (int j = 0; j < d; j++) {
                int top = a.getY() - 1;
                for (int y = b.getY(); y >= a.getY(); y--) {
                    if (!mc.world.getBlockState(new BlockPos(a.getX() + i, y, a.getZ() + j)).isAir()) { top = y; break; }
                }
                h[i][j] = top; orig[i][j] = top;
            }
        for (int it = 0; it < iter; it++) {
            int[][] nh = new int[w][d];
            for (int i = 0; i < w; i++)
                for (int j = 0; j < d; j++) {
                    int sum = 0, cnt = 0;
                    for (int di = -1; di <= 1; di++)
                        for (int dj = -1; dj <= 1; dj++) {
                            int ni = i + di, nj = j + dj;
                            if (ni < 0 || nj < 0 || ni >= w || nj >= d) continue;
                            sum += h[ni][nj]; cnt++;
                        }
                    nh[i][j] = Math.round(sum / (float) cnt);
                }
            h = nh;
        }
        BlockState air = Blocks.AIR.getDefaultState();
        List<Change> ch = new ArrayList<>();
        for (int i = 0; i < w; i++)
            for (int j = 0; j < d; j++) {
                int o = orig[i][j];
                if (o < a.getY()) continue;
                int nt = Math.max(a.getY() - 1, Math.min(b.getY(), h[i][j]));
                if (nt == o) continue;
                int x = a.getX() + i, z = a.getZ() + j;
                BlockState topS = mc.world.getBlockState(new BlockPos(x, o, z));
                BlockState fillS = o - 1 >= a.getY() ? mc.world.getBlockState(new BlockPos(x, o - 1, z)) : topS;
                if (fillS.isAir()) fillS = topS;
                if (nt > o) {
                    for (int y = o; y < nt; y++) addSym(mc, ch, new BlockPos(x, y, z), fillS);
                    addSym(mc, ch, new BlockPos(x, nt, z), topS);
                } else {
                    for (int y = nt + 1; y <= o; y++) addSym(mc, ch, new BlockPos(x, y, z), air);
                    if (nt >= a.getY()) addSym(mc, ch, new BlockPos(x, nt, z), topS);
                }
            }
        apply(mc, ch);
    }

    public static void expand(MinecraftClient mc, int mode, int n) {
        if (pos1 == null || pos2 == null) { msg("Сначала поставь точки 1 и 2"); return; }
        Direction f = mc.player.getHorizontalFacing();
        Direction dir = mode == 1 ? Direction.UP : mode == 2 ? Direction.DOWN : mode == 3 ? f.getOpposite() : f;
        BlockPos a = min(), b = max();
        if (dir.getDirection() == Direction.AxisDirection.POSITIVE) b = b.offset(dir, n); else a = a.offset(dir, n);
        if (a.getX() > b.getX() || a.getY() > b.getY() || a.getZ() > b.getZ()) { msg("Нельзя сузить дальше"); return; }
        pos1 = a; pos2 = b;
        msg("Объём выделения: " + vol());
    }

    public static void transformSel(MinecraftClient mc, int mode) {
        if (!sel()) return;
        BlockPos a = min(), b = max();
        List<Entry> src = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++)
                    src.add(new Entry(x - a.getX(), y - a.getY(), z - a.getZ(), mc.world.getBlockState(new BlockPos(x, y, z))));
        List<Entry> out = new ArrayList<>();
        for (Entry e : src) {
            if (mode == 0) out.add(new Entry(-e.z(), e.y(), e.x(), e.s().rotate(BlockRotation.CLOCKWISE_90)));
            else if (mode == 1) out.add(new Entry(-e.x(), e.y(), e.z(), e.s().mirror(BlockMirror.FRONT_BACK)));
            else out.add(new Entry(e.x(), e.y(), -e.z(), e.s().mirror(BlockMirror.LEFT_RIGHT)));
        }
        out = normalize(out);
        BlockState air = Blocks.AIR.getDefaultState();
        List<Change> ch = new ArrayList<>();
        for (Entry e : src) if (!e.s().isAir()) ch.add(new Change(a.add(e.x(), e.y(), e.z()), e.s(), air));
        int mx = 0, my = 0, mz = 0;
        for (Entry e : out) {
            BlockPos np = a.add(e.x(), e.y(), e.z());
            ch.add(new Change(np, mc.world.getBlockState(np), e.s()));
            mx = Math.max(mx, e.x()); my = Math.max(my, e.y()); mz = Math.max(mz, e.z());
        }
        pos1 = a; pos2 = a.add(mx, my, mz);
        apply(mc, ch);
    }

    public static void setMark(MinecraftClient mc) {
        mark = mc.player.getBlockPos();
        markDim = mc.world.getRegistryKey().getValue().toString();
        msg("Метка сохранена: " + mark.getX() + " " + mark.getY() + " " + mark.getZ() + " (" + markDim + ")");
    }

    public static void gotoMark(MinecraftClient mc) {
        if (mark == null) { msg("Метка не задана"); return; }
        cmd(mc, "execute in " + markDim + " run tp @s " + mark.getX() + " " + mark.getY() + " " + mark.getZ());
    }

    public static String status() {
        return "Т1:" + fmt(pos1) + " Т2:" + fmt(pos2) + " | объём " + vol() + " | буфер " + clip.size()
                + " | шаг " + step + " | симм " + (symX ? "X" : "") + (symZ ? "Z" : "") + (!symX && !symZ ? "-" : "")
                + (queue.isEmpty() ? "" : " | очередь " + queue.size());
    }

    private static String fmt(BlockPos p) { return p == null ? "-" : p.getX() + "," + p.getY() + "," + p.getZ(); }
}
