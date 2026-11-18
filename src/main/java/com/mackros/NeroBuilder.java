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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Поэтапное строительство: расчистка -> конструкция -> детали (по элементам), затем команды настройки и командные блоки.
 * Работает порциями за тик (NeroConfig.blocksPerTick), умеет паузу, отмену и откат с сохранением исходных состояний.
 */
public final class NeroBuilder {
    private record Old(BlockPos p, BlockState s) { }

    private enum Phase { BLOCKS, SETUP, CBS, DONE }

    public static Box preview = null;
    public static volatile boolean running = false, paused = false;
    public static volatile int done = 0, total = 0;

    private static NeroBuildPlan plan;
    private static List<NeroBuildPlan.Block> blocks = new ArrayList<>();
    private static int bi = 0, ci = 0, si = 0, waitTicks = 0;
    private static Phase phase = Phase.DONE;
    private static List<Old> current = new ArrayList<>();
    private static final AtomicBoolean inFlight = new AtomicBoolean(false);
    private static final Deque<List<Old>> HIST = new ArrayDeque<>();
    private static final Deque<String> HIST_TAG = new ArrayDeque<>();
    private static int histOps = 0;
    private static final int HIST_LIMIT = 120000;

    private static List<Old> undoJob = null;
    private static int undoIdx = 0;
    private static String undoTag = null;

    public static NeroBuildPlan plan() { return plan; }
    public static boolean hasHistory() { return !HIST.isEmpty(); }
    public static boolean busy() { return running || undoJob != null; }

    public static void showPreview(NeroBuildPlan p) { preview = p == null ? null : p.box(); }
    public static void clearPreview() { preview = null; }

    /** Запуск строительства. Возвращает false, если уже идёт работа. */
    public static boolean start(MinecraftClient mc, NeroBuildPlan p) {
        if (running || undoJob != null) { NeroHistory.log("Идёт другая операция: дождись окончания или отмени"); return false; }
        if (mc.getServer() == null) { NeroHistory.log("Строительство доступно только в одиночной игре"); return false; }
        plan = p;
        blocks = new ArrayList<>(p.blocks);
        blocks.sort(Comparator.comparingInt((NeroBuildPlan.Block b) -> b.el()).thenComparingInt(NeroBuildPlan.Block::stage));
        bi = 0; ci = 0; si = 0; waitTicks = 0;
        current = new ArrayList<>();
        total = blocks.size() + p.setup.size() + p.cbs.size();
        done = 0;
        phase = Phase.BLOCKS;
        running = true;
        paused = false;
        preview = null;
        NeroHistory.log("Старт строительства: " + total + " операций, этапов " + p.stages());
        return true;
    }

    public static void pause() { if (running) { paused = true; NeroHistory.log("Строительство приостановлено"); } }
    public static void resume() { if (running) { paused = false; NeroHistory.log("Строительство продолжено"); } }

    public static void cancel() {
        if (!running) return;
        running = false;
        paused = false;
        phase = Phase.DONE;
        finishHistory();
        NeroHistory.log("Строительство отменено на " + done + "/" + total + ". Построенное можно откатить.");
    }

    private static void finishHistory() {
        if (!current.isEmpty()) {
            HIST.push(current);
            HIST_TAG.push(plan == null ? "" : plan.entityTag);
            histOps += current.size();
            while (histOps > HIST_LIMIT && HIST.size() > 1) { histOps -= HIST.removeLast().size(); HIST_TAG.removeLast(); }
        }
        current = new ArrayList<>();
    }

    public static void rollback(MinecraftClient mc) {
        if (running) { NeroHistory.log("Сначала приостанови и отмени строительство"); return; }
        if (HIST.isEmpty()) { NeroHistory.log("Нечего откатывать"); return; }
        undoJob = HIST.pop();
        undoTag = HIST_TAG.pop();
        histOps -= undoJob.size();
        undoIdx = undoJob.size() - 1;
        if (undoTag != null && !undoTag.isEmpty()) Editor.cmd(mc, "kill @e[tag=" + undoTag + "]");
        NeroHistory.log("Откат: возвращаю " + undoJob.size() + " блоков");
    }

    public static void tick(MinecraftClient mc) {
        IntegratedServer srv = mc.getServer();
        if (srv == null || mc.world == null) {
            if (running) { running = false; phase = Phase.DONE; NeroHistory.log("Мир закрыт: строительство остановлено"); }
            return;
        }
        ServerWorld w = srv.getWorld(mc.world.getRegistryKey());
        if (w == null || inFlight.get()) return;
        int per = NeroConfig.blocksPerTick;

        if (undoJob != null) {
            final List<Old> job = undoJob;
            final int from = undoIdx;
            final int to = Math.max(-1, from - per);
            inFlight.set(true);
            srv.execute(() -> {
                try { for (int i = from; i > to; i--) w.setBlockState(job.get(i).p(), job.get(i).s(), 3); }
                finally { inFlight.set(false); }
            });
            undoIdx = to;
            if (undoIdx < 0) { undoJob = null; NeroHistory.log("Откат завершён"); }
            return;
        }

        if (!running || paused) return;

        if (phase == Phase.BLOCKS) {
            if (pendingBatch != null) return;
            int end = Math.min(blocks.size(), bi + per);
            final List<NeroBuildPlan.Block> batch = new ArrayList<>(blocks.subList(bi, end));
            inFlight.set(true);
            final AtomicBoolean missing = new AtomicBoolean(false);
            final List<Old> rec = new ArrayList<>();
            srv.execute(() -> {
                try {
                    for (NeroBuildPlan.Block b : batch) {
                        BlockPos p = new BlockPos(b.x(), b.y(), b.z());
                        if (!w.isChunkLoaded(p)) { missing.set(true); return; }
                    }
                    for (NeroBuildPlan.Block b : batch) {
                        BlockPos p = new BlockPos(b.x(), b.y(), b.z());
                        rec.add(new Old(p, w.getBlockState(p)));
                        w.setBlockState(p, b.st(), 3);
                    }
                } finally { inFlight.set(false); }
            });
            // результат применим на следующем тике, когда inFlight освободится
            pendingBatch = new Pending(end, missing, rec, batch.size(), false);
            return;
        }
        if (phase == Phase.SETUP) {
            if (si < plan.setup.size()) {
                Editor.cmd(mc, plan.setup.get(si++));
                done++;
                if (si % 6 == 0) waitTicks = 2;
                return;
            }
            phase = Phase.CBS;
            return;
        }
        if (phase == Phase.CBS) {
            if (waitTicks > 0) { waitTicks--; return; }
            if (pendingBatch != null) return;
            int end = Math.min(plan.cbs.size(), ci + Math.min(per, 80));
            final List<NeroBuildPlan.Cb> batch = new ArrayList<>(plan.cbs.subList(ci, end));
            final List<Old> rec = new ArrayList<>();
            inFlight.set(true);
            srv.execute(() -> {
                try {
                    for (NeroBuildPlan.Cb c : batch) {
                        BlockPos p = new BlockPos(c.x(), c.y(), c.z());
                        rec.add(new Old(p, w.getBlockState(p)));
                        Block cb = switch (c.type()) {
                            case REPEAT -> Blocks.REPEATING_COMMAND_BLOCK;
                            case CHAIN -> Blocks.CHAIN_COMMAND_BLOCK;
                            default -> Blocks.COMMAND_BLOCK;
                        };
                        w.setBlockState(p, cb.getDefaultState().with(CommandBlock.FACING, c.f()).with(CommandBlock.CONDITIONAL, c.cond()), 3);
                        if (w.getBlockEntity(p) instanceof CommandBlockBlockEntity be) {
                            be.getCommandExecutor().setCommand(c.cmd());
                            be.setAuto(c.auto());
                            be.markDirty();
                        }
                    }
                } finally { inFlight.set(false); }
            });
            pendingBatch = new Pending(end, new AtomicBoolean(false), rec, batch.size(), true);
        }
    }

    private record Pending(int end, AtomicBoolean missing, List<Old> rec, int n, boolean cb) { }
    private static Pending pendingBatch = null;

    /** Применение результата блочного пакета (вызывается в начале следующего тика). */
    public static void afterBatch(MinecraftClient mc) {
        if (pendingBatch == null || inFlight.get()) return;
        Pending pb = pendingBatch;
        pendingBatch = null;
        if (pb.missing().get()) {
            waitTicks++;
            if (waitTicks > 200) {
                running = false; phase = Phase.DONE; finishHistory();
                NeroHistory.log("Чанки не загрузились: подойди ближе к месту стройки. Построенное можно откатить.");
            }
            return;
        }
        waitTicks = 0;
        current.addAll(pb.rec());
        done += pb.n();
        if (pb.cb()) {
            ci = pb.end();
            if (ci >= plan.cbs.size()) { phase = Phase.DONE; running = false; finishHistory(); NeroHistory.log("Строительство завершено: " + done + " операций"); }
        } else {
            bi = pb.end();
            if (bi >= blocks.size()) phase = Phase.SETUP;
        }
    }

    public static void tickAll(MinecraftClient mc) {
        afterBatch(mc);
        tick(mc);
    }

    private NeroBuilder() { }
}
