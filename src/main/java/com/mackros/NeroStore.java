package com.mackros;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Запоминает последний построенный проект (описание + место), чтобы его можно было редактировать после перезапуска игры. */
public final class NeroStore {
    public static NeroSpec builtSpec = null;
    public static BlockPos builtBase = null;

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("mackros_nero_project.json"); }

    public static boolean hasBuilt(BlockPos base) { return builtSpec != null && builtBase != null && builtBase.equals(base); }

    public static void saveBuilt(NeroSpec spec, BlockPos base) {
        builtSpec = spec.copy();
        builtBase = base;
        try {
            JsonObject o = new JsonObject();
            o.add("spec", spec.toJson());
            o.addProperty("x", base.getX());
            o.addProperty("y", base.getY());
            o.addProperty("z", base.getZ());
            Files.writeString(file(), o.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }

    public static void load() {
        try {
            if (!Files.exists(file())) return;
            JsonObject o = JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8)).getAsJsonObject();
            NeroSpec s = NeroSpec.fromJson(o.getAsJsonObject("spec"));
            if (s == null) return;
            builtSpec = s;
            builtBase = new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
            if (NeroAI.project == null) NeroAI.project = s.copy();
        } catch (Exception ignored) { }
    }

    private NeroStore() { }
}
