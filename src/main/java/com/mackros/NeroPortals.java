package com.mackros;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Система порталов Portal Gun (сервер): размещение с проверкой поверхности, частицы-кольцо и телепортация между двумя порталами владельца.
 * Порталы существуют, пока работает сервер (после перезапуска мира их надо выстрелить заново).
 */
public final class NeroPortals {
    private static final class Portal {
        ServerWorld world;
        BlockPos bottom;
        Direction side;
    }

    private static final class Pair { Portal blue, orange; }

    private static final Map<UUID, Pair> PAIRS = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    private static int tick = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(NeroPortals::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { PAIRS.clear(); COOLDOWN.clear(); });
    }

    public static void clear() { PAIRS.clear(); }

    private static boolean wall(ServerWorld w, BlockPos p) { BlockState s = w.getBlockState(p); return s.isSolidBlock(w, p); }

    private static boolean freeAir(ServerWorld w, BlockPos p) { return w.getBlockState(p).isAir(); }

    /** @return текст ошибки или null, если портал поставлен */
    public static String place(ServerWorld w, ServerPlayerEntity pl, BlockPos hit, Direction side, boolean orange) {
        if (side.getAxis().isVertical()) return "портал ставится только на вертикальную стену";
        BlockPos front = hit.offset(side);
        BlockPos bottom = null;
        for (BlockPos b : new BlockPos[]{front, front.down()}) {
            BlockPos t = b.up();
            if (freeAir(w, b) && freeAir(w, t) && wall(w, b.offset(side.getOpposite())) && wall(w, t.offset(side.getOpposite()))) { bottom = b; break; }
        }
        if (bottom == null) return "здесь не хватает места: нужна ровная стена 1x2 и свободное место перед ней";
        Portal p = new Portal();
        p.world = w; p.bottom = bottom; p.side = side;
        Pair pr = PAIRS.computeIfAbsent(pl.getUuid(), k -> new Pair());
        if (orange) pr.orange = p; else pr.blue = p;
        w.playSound(null, bottom, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, orange ? 1.4f : 0.8f);
        String msg = (orange ? "Оранжевый портал поставлен" : "Синий портал поставлен") + (orange ? (pr.blue == null ? ". Поставь ещё синий (ПКМ)" : "") : (pr.orange == null ? ". Поставь ещё оранжевый (Shift+ПКМ)" : ""));
        pl.sendMessage(net.minecraft.text.Text.literal(msg), true);
        return null;
    }

    private static Vec3d center(Portal p) { return new Vec3d(p.bottom.getX() + 0.5, p.bottom.getY() + 1.0, p.bottom.getZ() + 0.5).add(p.side.getOffsetX() * 0.45, 0, p.side.getOffsetZ() * 0.45); }

    private static void ring(Portal p, boolean orange) {
        Vec3d c = center(p);
        Direction h = p.side.rotateYClockwise();
        DustParticleEffect fx = new DustParticleEffect(orange ? new Vector3f(1f, 0.55f, 0.1f) : new Vector3f(0.2f, 0.55f, 1f), 1.0f);
        for (int i = 0; i < 18; i++) {
            double a = Math.PI * 2 * i / 18;
            double u = Math.cos(a) * 0.42, v = Math.sin(a) * 0.95;
            p.world.spawnParticles(fx, c.x + h.getOffsetX() * u, c.y + v, c.z + h.getOffsetZ() * u, 1, 0, 0, 0, 0);
        }
    }

    private static boolean inside(Portal p, ServerPlayerEntity pl) {
        Box b = new Box(p.bottom.getX(), p.bottom.getY(), p.bottom.getZ(), p.bottom.getX() + 1, p.bottom.getY() + 2, p.bottom.getZ() + 1)
                .expand(Math.abs(p.side.getOffsetX()) * 0.1, 0, Math.abs(p.side.getOffsetZ()) * 0.1);
        return pl.getWorld() == p.world && pl.getBoundingBox().intersects(b);
    }

    private static void go(ServerPlayerEntity pl, Portal to) {
        Vec3d c = center(to);
        double x = c.x + to.side.getOffsetX() * 0.7, z = c.z + to.side.getOffsetZ() * 0.7;
        pl.teleport(to.world, x, to.bottom.getY(), z, to.side.asRotation(), pl.getPitch());
        to.world.playSound(null, to.bottom, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.6f, 1.0f);
        COOLDOWN.put(pl.getUuid(), 20);
    }

    private static void tick(MinecraftServer server) {
        tick++;
        if (PAIRS.isEmpty()) return;
        COOLDOWN.replaceAll((k, v) -> v - 1);
        COOLDOWN.values().removeIf(v -> v <= 0);
        for (Pair pr : PAIRS.values()) {
            if (tick % 4 == 0) {
                if (pr.blue != null) ring(pr.blue, false);
                if (pr.orange != null) ring(pr.orange, true);
            }
            if (pr.blue == null || pr.orange == null) continue;
            for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
                if (COOLDOWN.containsKey(pl.getUuid())) continue;
                if (inside(pr.blue, pl)) go(pl, pr.orange);
                else if (inside(pr.orange, pl)) go(pl, pr.blue);
            }
        }
    }

    private NeroPortals() { }
}
