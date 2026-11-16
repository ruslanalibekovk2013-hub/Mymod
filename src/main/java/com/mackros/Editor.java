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
        HitResult h = e.raycast(Config.reach, 1f, false);
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
        if (vol() > Config.maxVolume) { msg("Слишком большая область"); return false; }
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

    public static void nudge(int dx, int dy, int dz) {
        if (lock != null) { lock = lock.add(dx, dy, dz); msg("Закреплено: " + lock.getX() + " " + lock.getY() + " " + lock.getZ()); return; }
        ox += dx; oy += dy; oz += dz; msg("Смещение вставки: " + ox + " " + oy + " " + oz);
    }

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
        if (Config.fillMode == 1 && !old.isAir()) return;
        if (Config.fillMode == 2 && old.isAir()) return;
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
        while (undo.size() > Config.undoLimit) undo.removeLast();
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
        for (int i = 0; i < Config.cmdRate && !queue.isEmpty(); i++) cmd(mc, queue.poll());
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
        BlockPos base = placeBase(mc);
        if (base == null) { msg("Нет блока в прицеле"); return; }
        List<Change> ch = new ArrayList<>();
        for (Entry e : clip) {
            if (!pasteAir && e.s().isAir()) continue;
            if (!layerOk(e.y())) continue;
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


    // ---------- Litematica-стиль: замок, слои, проверка, материалы ----------
    public record Mark(BlockPos p, int kind) {}
    public static BlockPos lock;
    public static int layerMode = 0, layer = 0, vOk = 0, vBad = 0;
    public static boolean verify = false;
    public static List<Mark> marks = new ArrayList<>();
    static int vt = 0;
    static BlockPos[] area1 = new BlockPos[10], area2 = new BlockPos[10];

    public static void say(String t) {
        var p = MinecraftClient.getInstance().player;
        if (p != null) p.sendMessage(Text.literal("§b[Mackros] §f" + t), false);
    }

    public static BlockPos placeBase(MinecraftClient mc) { return lock != null ? lock : anchor(mc); }

    public static boolean layerOk(int y) {
        if (layerMode == 1) return y == layer;
        if (layerMode == 2) return y <= layer;
        return true;
    }

    public static void lockPlacement(MinecraftClient mc) {
        if (lock != null) { lock = null; marks = new ArrayList<>(); msg("Замок снят"); return; }
        BlockPos b = anchor(mc);
        if (b == null) { msg("Нет блока в прицеле"); return; }
        lock = b;
        msg("Размещение закреплено: " + b.getX() + " " + b.getY() + " " + b.getZ());
    }

    public static void cycleLayerMode() {
        layerMode = (layerMode + 1) % 3;
        msg("Слои: " + (layerMode == 0 ? "все" : layerMode == 1 ? "один слой" : "до слоя"));
    }

    public static void layerStep(int d) {
        int top = Math.max(0, size()[1] - 1);
        layer = Math.max(0, Math.min(top, layer + d));
        if (layerMode == 0) layerMode = 1;
        msg("Слой " + layer + " из " + top);
    }

    public static void toggleVerify() {
        verify = !verify;
        if (!verify) marks = new ArrayList<>();
        msg("Верификатор: " + (verify ? "вкл (нужен замок)" : "выкл"));
    }

    public static void tickVerify(MinecraftClient mc) {
        if (!verify || lock == null || clip.isEmpty()) {
            if (!marks.isEmpty()) marks = new ArrayList<>();
            return;
        }
        if (clip.size() > 300000) { verify = false; msg("Схема слишком большая для проверки"); return; }
        if (++vt < 10) return;
        vt = 0;
        List<Mark> n = new ArrayList<>();
        int ok = 0, bad = 0;
        for (Entry e : clip) {
            if (!layerOk(e.y())) continue;
            BlockState want = e.s();
            if (want.isAir() && !pasteAir) continue;
            BlockPos p = lock.add(e.x(), e.y(), e.z());
            BlockState have = mc.world.getBlockState(p);
            if (have == want) { ok++; continue; }
            bad++;
            if (n.size() < Config.previewLimit) n.add(new Mark(p, have.isAir() ? 1 : have.isOf(want.getBlock()) ? 2 : 0));
        }
        marks = n; vOk = ok; vBad = bad;
    }

    public static void verifyReport(MinecraftClient mc) {
        if (lock == null || clip.isEmpty()) { msg("Сначала закрепи размещение (Схемы -> Закрепить)"); return; }
        verify = true; vt = 99;
        tickVerify(mc);
        say("Верно: " + vOk + ", ошибок: " + vBad + ". Красный = не тот блок, синий = нет блока, жёлтый = другое состояние.");
    }

    public static List<java.util.Map.Entry<Block, Integer>> materials(MinecraftClient mc) {
        java.util.Map<Block, Integer> m = new java.util.HashMap<>();
        if (!clip.isEmpty()) {
            for (Entry e : clip) { if (!e.s().isAir()) m.merge(e.s().getBlock(), 1, Integer::sum); }
        } else if (pos1 != null && pos2 != null && vol() <= Config.maxVolume) {
            BlockPos a = min(), b = max();
            for (int x = a.getX(); x <= b.getX(); x++)
                for (int y = a.getY(); y <= b.getY(); y++)
                    for (int z = a.getZ(); z <= b.getZ(); z++) {
                        BlockState st = mc.world.getBlockState(new BlockPos(x, y, z));
                        if (!st.isAir()) m.merge(st.getBlock(), 1, Integer::sum);
                    }
        }
        List<java.util.Map.Entry<Block, Integer>> l = new ArrayList<>(m.entrySet());
        l.removeIf(en -> en.getKey().asItem() == net.minecraft.item.Items.AIR);
        l.sort((u, v) -> v.getValue() - u.getValue());
        return l;
    }

    public static int have(MinecraftClient mc, Block b) {
        int c = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (st.isOf(b.asItem())) c += st.getCount();
        }
        return c;
    }

    public static void clearClip() { clip = new ArrayList<>(); lock = null; marks = new ArrayList<>(); msg("Буфер очищен"); }

    public static void scaleClip(int f) {
        if (clip.isEmpty()) { msg("Буфер пуст"); return; }
        if ((long) clip.size() * f * f * f > 2_000_000L) { msg("Слишком большой результат"); return; }
        List<Entry> n = new ArrayList<>();
        for (Entry e : clip)
            for (int dx = 0; dx < f; dx++)
                for (int dy = 0; dy < f; dy++)
                    for (int dz = 0; dz < f; dz++)
                        n.add(new Entry(e.x() * f + dx, e.y() * f + dy, e.z() * f + dz, e.s()));
        clip = n;
        msg("Буфер увеличен в " + f + " раза: " + clip.size() + " блоков");
    }

    public static void trimAir() {
        List<Entry> n = new ArrayList<>();
        for (Entry e : clip) if (!e.s().isAir()) n.add(e);
        clip = normalize(n);
        msg("Воздух убран, блоков: " + clip.size());
    }

    public static void saveArea(MinecraftClient mc) {
        if (pos1 == null || pos2 == null) { msg("Нет выделения"); return; }
        area1[slot] = pos1; area2[slot] = pos2;
        msg("Область сохранена в слот " + slot);
    }

    public static void loadArea(MinecraftClient mc) {
        if (area1[slot] == null) { msg("Слот " + slot + " пуст"); return; }
        pos1 = area1[slot]; pos2 = area2[slot];
        msg("Область загружена из слота " + slot);
    }

    // ---------- новые инструменты ----------
    public static void floodFill(MinecraftClient mc) {
        BlockPos start = look(mc, false);
        if (start == null) { msg("Нет блока в прицеле"); return; }
        BlockState target = mc.world.getBlockState(start), to = held(mc);
        if (target == to) { msg("Там уже этот блок"); return; }
        java.util.HashSet<BlockPos> seen = new java.util.HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        List<Change> ch = new ArrayList<>();
        q.add(start); seen.add(start);
        while (!q.isEmpty() && seen.size() < 30000) {
            BlockPos p = q.poll();
            addSym(mc, ch, p, to);
            for (Direction d : Direction.values()) {
                BlockPos n = p.offset(d);
                if (seen.contains(n) || n.getSquaredDistance(start) > 4096) continue;
                if (mc.world.getBlockState(n).isOf(target.getBlock())) { seen.add(n); q.add(n); }
            }
        }
        apply(mc, ch);
    }

    public static void frame(MinecraftClient mc) {
        if (!sel()) return;
        BlockState s = held(mc);
        BlockPos a = min(), b = max();
        List<Change> ch = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++) {
                    int c = (x == a.getX() || x == b.getX() ? 1 : 0) + (y == a.getY() || y == b.getY() ? 1 : 0) + (z == a.getZ() || z == b.getZ() ? 1 : 0);
                    if (c >= 2) addSym(mc, ch, new BlockPos(x, y, z), s);
                }
        apply(mc, ch);
    }

    public static void pyramid(MinecraftClient mc) {
        BlockPos c = look(mc, true);
        if (c == null) { msg("Нет блока в прицеле"); return; }
        BlockState s = held(mc);
        List<Change> ch = new ArrayList<>();
        for (int y = 0; y <= radius; y++) {
            int hf = radius - y;
            for (int dx = -hf; dx <= hf; dx++)
                for (int dz = -hf; dz <= hf; dz++) addSym(mc, ch, c.add(dx, y, dz), s);
        }
        apply(mc, ch);
    }

    public static void torus(MinecraftClient mc) {
        BlockPos c = look(mc, true);
        if (c == null) { msg("Нет блока в прицеле"); return; }
        BlockState s = held(mc);
        int R = radius, t = Math.max(1, radius / 3);
        List<Change> ch = new ArrayList<>();
        for (int dx = -(R + t); dx <= R + t; dx++)
            for (int dz = -(R + t); dz <= R + t; dz++)
                for (int dy = -t; dy <= t; dy++) {
                    double dd = Math.sqrt(dx * dx + dz * dz) - R;
                    if (dd * dd + dy * dy <= t * t + 0.5) addSym(mc, ch, c.add(dx, dy + t, dz), s);
                }
        apply(mc, ch);
    }

    public static void tower(MinecraftClient mc) {
        BlockPos c = look(mc, true);
        if (c == null) { msg("Нет блока в прицеле"); return; }
        BlockState s = held(mc);
        int r = radius;
        List<Change> ch = new ArrayList<>();
        for (int y = 0; y < height; y++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    int d = dx * dx + dz * dz;
                    if (d > r * r + 1 || d <= (r - 1) * (r - 1)) continue;
                    addSym(mc, ch, c.add(dx, y, dz), s);
                }
        apply(mc, ch);
    }

    public static void house(MinecraftClient mc) {
        BlockPos c = look(mc, true);
        if (c == null) { msg("Нет блока в прицеле"); return; }
        BlockState wall = held(mc);
        if (wall.isAir()) wall = Blocks.OAK_PLANKS.getDefaultState();
        BlockState glass = Blocks.GLASS.getDefaultState(), air = Blocks.AIR.getDefaultState();
        List<Change> ch = new ArrayList<>();
        for (int x = 0; x < 7; x++)
            for (int y = 0; y < 5; y++)
                for (int z = 0; z < 7; z++) {
                    boolean edge = x == 0 || x == 6 || z == 0 || z == 6 || y == 0 || y == 4;
                    BlockState st = edge ? wall : air;
                    if (x == 3 && z == 0 && (y == 1 || y == 2)) st = air;
                    if (y == 2 && (((z == 0 || z == 6) && (x == 1 || x == 5)) || ((x == 0 || x == 6) && z == 3))) st = glass;
                    addSym(mc, ch, c.add(x - 3, y, z - 3), st);
                }
        apply(mc, ch);
    }

    public static void bridge(MinecraftClient mc) {
        if (pos1 == null || pos2 == null) { msg("Сначала поставь точки 1 и 2"); return; }
        BlockState s = held(mc);
        int dx = pos2.getX() - pos1.getX(), dy = pos2.getY() - pos1.getY(), dz = pos2.getZ() - pos1.getZ();
        int n = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        List<Change> ch = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : (double) i / n;
            BlockPos p = new BlockPos((int) Math.round(pos1.getX() + dx * t), (int) Math.round(pos1.getY() + dy * t), (int) Math.round(pos1.getZ() + dz * t));
            addSym(mc, ch, p, s);
            addSym(mc, ch, p.add(1, 0, 0), s);
            addSym(mc, ch, p.add(-1, 0, 0), s);
            addSym(mc, ch, p.add(0, 0, 1), s);
            addSym(mc, ch, p.add(0, 0, -1), s);
        }
        apply(mc, ch);
    }

    public static String status() {
        return "Т1:" + fmt(pos1) + " Т2:" + fmt(pos2) + " | объём " + vol() + " | буфер " + clip.size()
                + " | шаг " + step + " | симм " + (symX ? "X" : "") + (symZ ? "Z" : "") + (!symX && !symZ ? "-" : "")
                + (lock != null ? " | замок" : "") + (layerMode != 0 ? " | слой " + layer : "") + (queue.isEmpty() ? "" : " | очередь " + queue.size());
    }

    private static String fmt(BlockPos p) { return p == null ? "-" : p.getX() + "," + p.getY() + "," + p.getZ(); }
}
