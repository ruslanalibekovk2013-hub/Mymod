package com.mackros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Порталы Portal Gun (сервер): размещение с проверкой стены, видимая светящаяся поверхность (block_display), кольцо частиц, телепортация.
 * Порталы сохраняются в папке мира (mackros_portals.json) и переживают перезапуск.
 */
public final class NeroPortals {
    private static final class Portal { ServerWorld world; BlockPos bottom; Direction side; }

    private static final class Pair { Portal blue, orange; }

    private static final Map<UUID, Pair> PAIRS = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MinecraftServer srv = null;
    private static int tick = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(NeroPortals::tick);
        ServerLifecycleEvents.SERVER_STARTED.register(NeroPortals::load);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { PAIRS.clear(); COOLDOWN.clear(); srv = null; });
    }

    private static void run(ServerWorld w, String cmd) {
        MinecraftServer s = w.getServer();
        s.getCommandManager().executeWithPrefix(s.getCommandSource().withWorld(w).withLevel(4).withSilent(), cmd);
    }

    private static String tag(UUID owner, boolean orange) { return "nero_portal_" + owner.toString().replace("-", "") + (orange ? "_o" : "_b"); }

    /** Убирает все порталы и их поверхности. */
    public static void clear() {
        for (Map.Entry<UUID, Pair> e : PAIRS.entrySet()) {
            if (e.getValue().blue != null) run(e.getValue().blue.world, "kill @e[type=minecraft:block_display,tag=" + tag(e.getKey(), false) + "]");
            if (e.getValue().orange != null) run(e.getValue().orange.world, "kill @e[type=minecraft:block_display,tag=" + tag(e.getKey(), true) + "]");
        }
        MinecraftServer s = srv;
        PAIRS.clear();
        if (s != null) save(s);
    }

    private static boolean wall(ServerWorld w, BlockPos p) { BlockState s = w.getBlockState(p); return s.isSolidBlock(w, p); }

    private static boolean freeAir(ServerWorld w, BlockPos p) { return w.getBlockState(p).isAir(); }

    private static String f(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    /** Светящаяся плёнка портала у стены. */
    private static void surface(UUID owner, boolean orange, Portal p) {
        String tg = tag(owner, orange);
        run(p.world, "kill @e[type=minecraft:block_display,tag=" + tg + "]");
        double tx, tz, sx, sz;
        switch (p.side) {
            case NORTH -> { tx = 0.05; tz = 0.9; sx = 0.9; sz = 0.06; }
            case SOUTH -> { tx = 0.05; tz = 0.04; sx = 0.9; sz = 0.06; }
            case WEST -> { tx = 0.9; tz = 0.05; sx = 0.06; sz = 0.9; }
            default -> { tx = 0.04; tz = 0.05; sx = 0.06; sz = 0.9; }
        }
        String block = orange ? "minecraft:orange_stained_glass" : "minecraft:light_blue_stained_glass";
        run(p.world, "summon minecraft:block_display " + p.bottom.getX() + " " + p.bottom.getY() + " " + p.bottom.getZ() + " {block_state:{Name:\"" + block
                + "\"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[" + f(tx) + "f,0.05f," + f(tz) + "f],scale:[" + f(sx) + "f,1.9f," + f(sz) + "f]},Tags:[\"" + tg + "\",\"nero_portal\"]}");
    }

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
        surface(pl.getUuid(), orange, p);
        srv = w.getServer();
        save(srv);
        w.playSound(null, bottom, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, orange ? 1.4f : 0.8f);
        String msg = (orange ? "Оранжевый портал поставлен" : "Синий портал поставлен") + (orange ? (pr.blue == null ? ". Поставь ещё синий (ПКМ)" : "") : (pr.orange == null ? ". Поставь ещё оранжевый (Shift+ПКМ)" : ""));
        pl.sendMessage(net.minecraft.text.Text.literal(msg), true);
        return null;
    }

    // ------------------------------------------------------------------ сохранение
    private static Path file(MinecraftServer s) { return s.getSavePath(WorldSavePath.ROOT).resolve("mackros_portals.json"); }

    private static void entry(JsonArray a, UUID owner, boolean orange, Portal p) {
        if (p == null) return;
        JsonObject o = new JsonObject();
        o.addProperty("owner", owner.toString());
        o.addProperty("orange", orange);
        o.addProperty("world", p.world.getRegistryKey().getValue().toString());
        o.addProperty("x", p.bottom.getX()); o.addProperty("y", p.bottom.getY()); o.addProperty("z", p.bottom.getZ());
        o.addProperty("side", p.side.getName());
        a.add(o);
    }

    private static void save(MinecraftServer s) {
        try {
            JsonArray a = new JsonArray();
            for (Map.Entry<UUID, Pair> e : PAIRS.entrySet()) { entry(a, e.getKey(), false, e.getValue().blue); entry(a, e.getKey(), true, e.getValue().orange); }
            Files.writeString(file(s), GSON.toJson(a), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }

    private static void load(MinecraftServer s) {
        srv = s;
        try {
            Path f = file(s);
            if (!Files.exists(f)) return;
            for (JsonElement e : JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                ServerWorld w = s.getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(o.get("world").getAsString())));
                Direction d = Direction.byName(o.get("side").getAsString());
                if (w == null || d == null) continue;
                Portal p = new Portal();
                p.world = w; p.bottom = new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt()); p.side = d;
                Pair pr = PAIRS.computeIfAbsent(UUID.fromString(o.get("owner").getAsString()), k -> new Pair());
                if (o.get("orange").getAsBoolean()) pr.orange = p; else pr.blue = p;
            }
        } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------------ тик
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
        pl.teleport(to.world, c.x + to.side.getOffsetX() * 0.7, to.bottom.getY(), c.z + to.side.getOffsetZ() * 0.7, to.side.asRotation(), pl.getPitch());
        to.world.playSound(null, to.bottom, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.6f, 1.0f);
        COOLDOWN.put(pl.getUuid(), 20);
    }

    private static void tick(MinecraftServer server) {
        srv = server;
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
