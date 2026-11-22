package com.mackros;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Серверная часть 3D-принтера. Команда (её выполняют командные блоки пульта):
 * nero print sx sy sz w h d dx dy dz den id : сканирует область-оригинал и слой за слоем строит на платформе B уменьшенную копию 1:den
 * из настоящих мини-блоков (block_display с масштабом 1/den, сохраняются тип блока, цвет и ориентация), двигая печатающую головку.
 * nero stop id [hx hy hz] : останавливает печать и возвращает головку.
 */
public final class NeroPrint {
    private record Cell(int x, int y, int z, BlockState st) { }

    private static final class Job {
        ServerWorld w;
        int sx, sy, sz, sw, sh, sd, dx, dy, dz, den;
        String id;
        List<Cell> cells = new ArrayList<>();
        int phase = 0, layer = 0, idx = 0, tick = 0;
    }

    private static final Map<String, Job> JOBS = new HashMap<>();
    private static final int MAX_CELLS = 8000, PER_TICK = 5;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ArgumentBuilder<ServerCommandSource, ?> cur = CommandManager.argument("den", IntegerArgumentType.integer(2, 8))
                    .then(CommandManager.argument("id", StringArgumentType.word()).executes(NeroPrint::start));
            for (String n : new String[]{"dz", "dy", "dx", "d", "h", "w", "sz", "sy", "sx"})
                cur = CommandManager.argument(n, IntegerArgumentType.integer(-30000000, 30000000)).then(cur);
            dispatcher.register(CommandManager.literal("nero").requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.literal("portals").then(CommandManager.literal("clear").executes(c -> { NeroPortals.clear(); return 1; })))
                    .then(CommandManager.literal("print").then(cur))
                    .then(CommandManager.literal("stop").then(CommandManager.argument("id", StringArgumentType.word()).executes(c -> stop(c, false))
                            .then(CommandManager.argument("hx", IntegerArgumentType.integer(-30000000, 30000000))
                                    .then(CommandManager.argument("hy", IntegerArgumentType.integer(-30000000, 30000000))
                                            .then(CommandManager.argument("hz", IntegerArgumentType.integer(-30000000, 30000000)).executes(c -> stop(c, true))))))));
        });
        ServerTickEvents.END_SERVER_TICK.register(NeroPrint::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> JOBS.clear());
    }

    private static int geti(CommandContext<ServerCommandSource> c, String n) { return IntegerArgumentType.getInteger(c, n); }

    private static void run(Job j, String cmd) {
        MinecraftServer s = j.w.getServer();
        ServerCommandSource src = s.getCommandSource().withWorld(j.w).withSilent().withLevel(4);
        s.getCommandManager().executeWithPrefix(src, cmd);
    }

    private static String f(double v) { return String.format(Locale.ROOT, "%.4f", v); }

    private static int start(CommandContext<ServerCommandSource> c) {
        Job j = new Job();
        j.w = c.getSource().getWorld();
        j.sx = geti(c, "sx"); j.sy = geti(c, "sy"); j.sz = geti(c, "sz");
        j.sw = Math.min(geti(c, "w"), 24); j.sh = Math.min(geti(c, "h"), 24); j.sd = Math.min(geti(c, "d"), 24);
        j.dx = geti(c, "dx"); j.dy = geti(c, "dy"); j.dz = geti(c, "dz");
        j.den = geti(c, "den");
        j.id = StringArgumentType.getString(c, "id");
        for (int y = 0; y < j.sh; y++)
            for (int x = 0; x < j.sw; x++)
                for (int z = 0; z < j.sd; z++) {
                    BlockState st = j.w.getBlockState(new BlockPos(j.sx + x, j.sy + y, j.sz + z));
                    if (!st.isAir()) j.cells.add(new Cell(x, y, z, st));
                }
        if (j.cells.isEmpty()) { c.getSource().sendError(Text.literal("3D-принтер: на платформе A нет блоков для печати")); return 0; }
        if (j.cells.size() > MAX_CELLS) { c.getSource().sendError(Text.literal("3D-принтер: слишком большая постройка (" + j.cells.size() + " блоков, максимум " + MAX_CELLS + ")")); return 0; }
        run(j, "kill @e[type=minecraft:block_display,tag=nero_mini_" + j.id + "]");
        JOBS.put(j.id, j);
        final int n = j.cells.size(), den = j.den;
        c.getSource().sendFeedback(() -> Text.literal("3D-принтер: найдено блоков " + n + ", печать в масштабе 1:" + den), false);
        screen(j, "Сканирование...", "yellow");
        return 1;
    }

    private static int stop(CommandContext<ServerCommandSource> c, boolean home) {
        String id = StringArgumentType.getString(c, "id");
        Job j = JOBS.remove(id);
        if (j == null) { c.getSource().sendFeedback(() -> Text.literal("3D-принтер: печать не запущена"), false); return 0; }
        if (home) {
            int hx = geti(c, "hx"), hy = geti(c, "hy"), hz = geti(c, "hz");
            run(j, "tp @e[type=minecraft:block_display,tag=nero_head_" + id + ",limit=1] " + hx + " " + hy + " " + hz);
            run(j, "tp @e[type=minecraft:block_display,tag=nero_beam_" + id + ",limit=1] " + hx + " " + (hy + 1) + " " + (hz - 8));
        }
        screen(j, "Остановлено", "red");
        return 1;
    }

    private static void screen(Job j, String text, String color) {
        run(j, "data modify entity @e[type=minecraft:text_display,tag=nero_screen_" + j.id + ",limit=1] text set value '{\"text\":\"ПРИНТЕР: " + text + "\",\"color\":\"" + color + "\"}'");
    }

    private static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        for (Iterator<Job> it = JOBS.values().iterator(); it.hasNext(); ) {
            Job j = it.next();
            try {
                if (!step(j)) it.remove();
            } catch (Exception e) {
                it.remove();
            }
        }
    }

    /** @return false, когда работа закончена. */
    private static boolean step(Job j) {
        j.tick++;
        double cx = j.sx + j.sw / 2.0, cz = j.sz + j.sd / 2.0;
        if (j.phase == 0) {
            if (j.tick % 2 != 0) return true;
            run(j, "particle minecraft:electric_spark " + f(cx) + " " + f(j.sy + j.layer + 0.5) + " " + f(cz) + " " + f(j.sw / 2.0) + " 0.05 " + f(j.sd / 2.0) + " 0 24");
            j.layer++;
            if (j.layer % 3 == 0) screen(j, "Сканирование " + Math.min(j.layer, j.sh) + "/" + j.sh, "yellow");
            if (j.layer >= j.sh) { j.phase = 1; j.idx = 0; screen(j, "Печать 0%", "aqua"); }
            return true;
        }
        if (j.phase == 1) {
            double s = 1.0 / j.den;
            for (int k = 0; k < PER_TICK && j.idx < j.cells.size(); k++, j.idx++) {
                Cell c = j.cells.get(j.idx);
                double mx = j.dx + c.x() * s, my = j.dy + c.y() * s, mz = j.dz + c.z() * s;
                String bs = NbtHelper.fromBlockState(c.st()).toString();
                run(j, "summon minecraft:block_display " + f(mx) + " " + f(my) + " " + f(mz) + " {block_state:" + bs
                        + ",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[" + f(s) + "f," + f(s) + "f," + f(s) + "f]},Tags:[\"nero_mini\",\"nero_mini_" + j.id + "\"]}");
                run(j, "particle minecraft:end_rod " + f(mx + s / 2) + " " + f(my + s) + " " + f(mz + s / 2) + " 0.03 0.03 0.03 0.01 2");
                double hx = mx + s / 2 - 0.35, hy = my + s + 0.5, hz = mz + s / 2 - 0.35;
                run(j, "tp @e[type=minecraft:block_display,tag=nero_head_" + j.id + ",limit=1] " + f(hx) + " " + f(hy) + " " + f(hz));
                run(j, "tp @e[type=minecraft:block_display,tag=nero_beam_" + j.id + ",limit=1] " + f(hx + 0.15) + " " + f(j.dy + 10) + " " + f(j.dz - 4));
                if (j.tick % 4 == 0) run(j, "particle minecraft:electric_spark " + f((hx + j.dx - 9) / 2) + " " + f((hy + j.dy + 3) / 2) + " " + f((hz + j.dz + 4) / 2) + " 0.5 0.3 0.5 0 3");
            }
            if (j.tick % 8 == 0) screen(j, "Печать " + (100 * j.idx / j.cells.size()) + "%", "aqua");
            if (j.idx >= j.cells.size()) j.phase = 2;
            return true;
        }
        run(j, "tp @e[type=minecraft:block_display,tag=nero_head_" + j.id + ",limit=1] " + f(j.dx + 6) + " " + f(j.dy + 9) + " " + f(j.dz + 4));
        run(j, "tp @e[type=minecraft:block_display,tag=nero_beam_" + j.id + ",limit=1] " + f(j.dx + 6) + " " + f(j.dy + 10) + " " + f(j.dz - 4));
        screen(j, "готово, блоков: " + j.cells.size(), "green");
        run(j, "tellraw @a {\"text\":\"3D-принтер: печать завершена (блоков: " + j.cells.size() + ", масштаб 1:" + j.den + ")\",\"color\":\"green\"}");
        return false;
    }

    private NeroPrint() { }
}
