package com.mackros;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Поэтапное строительство и редактирование: расчистка -> конструкция -> детали (по элементам), затем команды настройки и командные блоки.
 * Порции за тик, пауза, отмена, откат. Построенный проект хранит исходные состояния блоков, поэтому его можно менять «по разнице»
 * (только изменившиеся блоки и команды), чинить и откатывать целиком.
 */
public final class NeroBuilder {
    public record Old(BlockPos p, BlockState s) { }

    /** Построенный проект: описание, план и исходные состояния всех затронутых блоков. */
    public static final class Project {
        public NeroBuildPlan plan;
        public NeroSpec spec;
        public BlockPos base;
        public String tag = "";
        public final LinkedHashMap<Long, Old> orig = new LinkedHashMap<>();
    }

    /** Одна операция: поставить состояние (st) либо командный блок (cb). */
    private record Op(BlockPos p, BlockState st, NeroBuildPlan.Cb cb) { }

    private enum Phase { BLOCKS, SETUP, CBS, DONE }

    private record Pending(int end, AtomicBoolean missing, int n, boolean cb) { }

    public static Box preview = null;
    public static volatile boolean running = false, paused = false;
    public static volatile int done = 0, total = 0;

    private static Project cur = null;
    private static NeroBuildPlan curPlan = null;
    private static List<Op> blockOps = new ArrayList<>();
    private static List<Op> cbOps = new ArrayList<>();
    private static List<String> setupCmds = new ArrayList<>();
    private static int bi = 0, ci = 0, si = 0, waitTicks = 0;
    private static Phase phase = Phase.DONE;
    private static final AtomicBoolean inFlight = new AtomicBoolean(false);
    private static Pending pending = null;
    private static final Deque<Project> PROJECTS = new ArrayDeque<>();
    private static List<Old> undoJob = null;
    private static int undoIdx = 0;

    public static void showPreview(NeroBuildPlan p) { preview = p == null ? null : p.box(); }
    public static void clearPreview() { preview = null; }
    public static boolean hasHistory() { return !PROJECTS.isEmpty(); }
    public static boolean busy() { return running || undoJob != null; }

    public static long key(int x, int y, int z) { return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | ((long) y & 0xFFF); }

    public static Project findProject(BlockPos base) {
        for (Project p : PROJECTS) if (p.base.equals(base)) return p;
        return null;
    }

    /** Подхватывает проект, построенный в прошлой сессии (исходные блоки неизвестны: откат недоступен, правка и починка работают). */
    public static Project adopt(NeroBuildPlan plan, NeroSpec spec, BlockPos base) {
        Project p = new Project();
        p.plan = plan; p.spec = spec.copy(); p.base = base; p.tag = plan.entityTag;
        PROJECTS.push(p);
        return p;
    }

    // ------------------------------------------------------------------ построение списков операций
    private static LinkedHashMap<Long, BlockState> finalMap(NeroBuildPlan pl) {
        List<NeroBuildPlan.Block> sorted = new ArrayList<>(pl.blocks);
        sorted.sort(Comparator.comparingInt((NeroBuildPlan.Block b) -> b.el()).thenComparingInt(NeroBuildPlan.Block::stage));
        LinkedHashMap<Long, BlockState> m = new LinkedHashMap<>();
        for (NeroBuildPlan.Block b : sorted) {
            long k = key(b.x(), b.y(), b.z());
            m.remove(k);
            m.put(k, b.st());
        }
        return m;
    }

    private static BlockPos posOf(long k, NeroBuildPlan pl, Map<Long, BlockPos> idx) { return idx.get(k); }

    private static Map<Long, BlockPos> index(NeroBuildPlan pl) {
        Map<Long, BlockPos> m = new HashMap<>();
        for (NeroBuildPlan.Block b : pl.blocks) m.put(key(b.x(), b.y(), b.z()), new BlockPos(b.x(), b.y(), b.z()));
        return m;
    }

    private static void fullOps(NeroBuildPlan p) {
        blockOps = new ArrayList<>();
        Map<Long, BlockPos> idx = index(p);
        for (Map.Entry<Long, BlockState> e : finalMap(p).entrySet()) blockOps.add(new Op(idx.get(e.getKey()), e.getValue(), null));
        cbOps = new ArrayList<>();
        for (NeroBuildPlan.Cb c : p.cbs) cbOps.add(new Op(new BlockPos(c.x(), c.y(), c.z()), null, c));
        setupCmds = new ArrayList<>(p.setup);
    }

    private static void diffOps(Project old, NeroBuildPlan np, boolean force) {
        NeroBuildPlan op = old.plan;
        LinkedHashMap<Long, BlockState> om = finalMap(op), nm = finalMap(np);
        Map<Long, BlockPos> nidx = index(np), oidx = index(op);
        blockOps = new ArrayList<>();
        for (Map.Entry<Long, BlockState> e : nm.entrySet()) {
            BlockState o = om.get(e.getKey());
            if (force || o == null || !o.equals(e.getValue())) blockOps.add(new Op(nidx.get(e.getKey()), e.getValue(), null));
        }
        Map<Long, NeroBuildPlan.Cb> ocb = new HashMap<>(), ncb = new HashMap<>();
        for (NeroBuildPlan.Cb c : op.cbs) ocb.put(key(c.x(), c.y(), c.z()), c);
        for (NeroBuildPlan.Cb c : np.cbs) ncb.put(key(c.x(), c.y(), c.z()), c);
        for (Map.Entry<Long, BlockState> e : om.entrySet()) {
            if (nm.containsKey(e.getKey()) || ncb.containsKey(e.getKey())) continue;
            Old orig = old.orig.get(e.getKey());
            blockOps.add(new Op(oidx.get(e.getKey()), orig != null ? orig.s() : Blocks.AIR.getDefaultState(), null));
        }
        cbOps = new ArrayList<>();
        for (NeroBuildPlan.Cb c : np.cbs) {
            NeroBuildPlan.Cb o = ocb.get(key(c.x(), c.y(), c.z()));
            if (force || o == null || !o.equals(c)) cbOps.add(new Op(new BlockPos(c.x(), c.y(), c.z()), null, c));
        }
        for (NeroBuildPlan.Cb c : op.cbs) {
            if (ncb.containsKey(key(c.x(), c.y(), c.z()))) continue;
            Old orig = old.orig.get(key(c.x(), c.y(), c.z()));
            blockOps.add(new Op(new BlockPos(c.x(), c.y(), c.z()), orig != null ? orig.s() : Blocks.AIR.getDefaultState(), null));
        }
        setupCmds = new ArrayList<>();
        for (String s : np.setup) if (force || !op.setup.contains(s)) setupCmds.add(s);
    }

    private static boolean begin(MinecraftClient mc, Project pr, NeroBuildPlan plan) {
        if (busy()) { NeroHistory.log("Идёт другая операция: дождись окончания или отмени"); return false; }
        if (mc.getServer() == null) { NeroHistory.log("Строительство доступно только в одиночной игре"); return false; }
        cur = pr;
        curPlan = plan;
        bi = 0; ci = 0; si = 0; waitTicks = 0;
        pending = null;
        total = blockOps.size() + setupCmds.size() + cbOps.size();
        done = 0;
        phase = Phase.BLOCKS;
        running = true;
        paused = false;
        preview = null;
        return true;
    }

    /** Новая постройка. */
    public static boolean start(MinecraftClient mc, NeroBuildPlan p, NeroSpec spec, BlockPos base) {
        Project pr = new Project();
        pr.plan = p; pr.spec = spec.copy(); pr.base = base; pr.tag = p.entityTag;
        fullOps(p);
        if (!begin(mc, pr, p)) return false;
        NeroHistory.log("Старт строительства: " + total + " операций, этапов " + p.stages());
        return true;
    }

    /** Редактирование построенного проекта: применяются только отличия (force = перестроить всё поверх, т.е. починка). */
    public static boolean update(MinecraftClient mc, Project old, NeroBuildPlan np, NeroSpec spec, boolean force) {
        PROJECTS.remove(old);
        diffOps(old, np, force);
        old.spec = spec.copy();
        if (!begin(mc, old, np)) { PROJECTS.push(old); return false; }
        NeroHistory.log((force ? "Починка проекта: " : "Правка проекта: ") + total + " операций вместо полной перестройки");
        if (total == 0) {
            NeroHistory.log("Отличий нет: проект уже соответствует описанию");
            running = false; phase = Phase.DONE;
            old.plan = np;
            PROJECTS.push(old);
        }
        return true;
    }

    public static void pause() { if (running) { paused = true; NeroHistory.log("Строительство приостановлено"); } }
    public static void resume() { if (running) { paused = false; NeroHistory.log("Строительство продолжено"); } }

    private static void finish(boolean completed) {
        running = false;
        paused = false;
        phase = Phase.DONE;
        pending = null;
        if (cur != null) {
            if (completed) cur.plan = curPlan;
            PROJECTS.push(cur);
            while (PROJECTS.size() > 6) PROJECTS.removeLast();
            if (completed) NeroStore.saveBuilt(cur.spec, cur.base);
        }
    }

    public static void cancel() {
        if (!running) return;
        finish(false);
        NeroHistory.log("Строительство отменено на " + done + "/" + total + ". Построенное можно откатить.");
    }

    public static void rollback(MinecraftClient mc) {
        if (busy()) { NeroHistory.log("Сначала дождись окончания или отмени строительство"); return; }
        Project p = PROJECTS.poll();
        if (p == null) { NeroHistory.log("Нечего откатывать"); return; }
        if (p.orig.isEmpty()) { NeroHistory.log("Эта постройка из прошлой сессии: исходные блоки не сохранены, откат недоступен"); return; }
        undoJob = new ArrayList<>(p.orig.values());
        undoIdx = undoJob.size() - 1;
        if (p.tag != null && !p.tag.isEmpty()) Editor.cmd(mc, "kill @e[tag=" + p.tag + "]");
        NeroHistory.log("Откат: возвращаю " + undoJob.size() + " блоков");
    }

    // ------------------------------------------------------------------ тики
    public static void tickAll(MinecraftClient mc) {
        afterBatch();
        tick(mc);
    }

    private static void afterBatch() {
        if (pending == null || inFlight.get()) return;
        Pending pb = pending;
        pending = null;
        if (pb.missing().get()) {
            waitTicks++;
            if (waitTicks > 200) {
                NeroHistory.log("Чанки не загрузились: подойди ближе к месту стройки. Построенное можно откатить.");
                finish(false);
            }
            return;
        }
        waitTicks = 0;
        done += pb.n();
        if (pb.cb()) {
            ci = pb.end();
            if (ci >= cbOps.size()) { finish(true); NeroHistory.log("Готово: " + done + " операций"); }
        } else {
            bi = pb.end();
            if (bi >= blockOps.size()) phase = Phase.SETUP;
        }
    }

    private static void apply(ServerWorld w, Op o) {
        BlockPos p = o.p();
        cur.orig.putIfAbsent(key(p.getX(), p.getY(), p.getZ()), new Old(p, w.getBlockState(p)));
        if (o.cb() == null) { w.setBlockState(p, o.st(), 3); return; }
        NeroBuildPlan.Cb c = o.cb();
        Block cb = switch (c.type()) {
            case REPEAT -> Blocks.REPEATING_COMMAND_BLOCK;
            case CHAIN -> Blocks.CHAIN_COMMAND_BLOCK;
            default -> Blocks.COMMAND_BLOCK;
        };
        w.setBlockState(p, cb.getDefaultState().with(CommandBlock.FACING, c.f()).with(CommandBlock.CONDITIONAL, c.cond()), 3);
        if (w.getBlockEntity(p) instanceof CommandBlockBlockEntity be) {
            be.getCommandExecutor().setCommand(c.cmd());
            be.setAuto(c.auto());
            if (be.getCommandExecutor().getCommand().isEmpty()) be.getCommandExecutor().setCommand(c.cmd());
            be.markDirty();
            BlockState now = w.getBlockState(p);
            w.updateListeners(p, now, now, 3);
        }
    }

    private static void tick(MinecraftClient mc) {
        IntegratedServer srv = mc.getServer();
        if (srv == null || mc.world == null) {
            if (running) { NeroHistory.log("Мир закрыт: строительство остановлено"); finish(false); }
            return;
        }
        ServerWorld w = srv.getWorld(mc.world.getRegistryKey());
        if (w == null || inFlight.get()) return;
        int per = NeroConfig.blocksPerTick;

        if (undoJob != null) {
            final List<Old> job = undoJob;
            final int from = undoIdx, to = Math.max(-1, from - per);
            inFlight.set(true);
            srv.execute(() -> {
                try { for (int i = from; i > to; i--) w.setBlockState(job.get(i).p(), job.get(i).s(), 3); }
                finally { inFlight.set(false); }
            });
            undoIdx = to;
            if (undoIdx < 0) { undoJob = null; NeroHistory.log("Откат завершён"); }
            return;
        }
        if (!running || paused || pending != null) return;

        if (phase == Phase.BLOCKS) {
            int end = Math.min(blockOps.size(), bi + per);
            final List<Op> batch = new ArrayList<>(blockOps.subList(bi, end));
            final AtomicBoolean missing = new AtomicBoolean(false);
            inFlight.set(true);
            pending = new Pending(end, missing, batch.size(), false);
            srv.execute(() -> {
                try {
                    for (Op o : batch) if (!w.isChunkLoaded(o.p())) { missing.set(true); return; }
                    for (Op o : batch) apply(w, o);
                } finally { inFlight.set(false); }
            });
            return;
        }
        if (phase == Phase.SETUP) {
            if (si < setupCmds.size()) {
                Editor.cmd(mc, setupCmds.get(si++));
                done++;
                if (si % 6 == 0) waitTicks = 2;
                return;
            }
            phase = Phase.CBS;
            return;
        }
        if (phase == Phase.CBS) {
            if (waitTicks > 0) { waitTicks--; return; }
            if (cbOps.isEmpty()) { finish(true); NeroHistory.log("Готово: " + done + " операций"); return; }
            int end = Math.min(cbOps.size(), ci + Math.min(per, 80));
            final List<Op> batch = new ArrayList<>(cbOps.subList(ci, end));
            inFlight.set(true);
            pending = new Pending(end, new AtomicBoolean(false), batch.size(), true);
            srv.execute(() -> {
                try { for (Op o : batch) apply(w, o); }
                finally { inFlight.set(false); }
            });
        }
    }

    private NeroBuilder() { }
}
