package com.mackros;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Промежуточное представление проекта: блоки, командные блоки, команды, механизмы, зависимости, этапы. Координаты абсолютные. */
public final class NeroBuildPlan {
    public enum CbType { IMPULSE, CHAIN, REPEAT }

    /** stage: 0 расчистка, 1 конструкция, 2 детали. */
    public record Block(int x, int y, int z, BlockState st, int el, int stage) { }

    public record Cb(int x, int y, int z, Direction f, CbType type, boolean auto, boolean cond, String cmd, int el) { }

    public record Element(String name, String kind, List<Integer> deps, String activation, String logic, Map<String, Integer> meta) { }

    public String title = "";
    public BlockPos origin = BlockPos.ORIGIN;
    public String entityTag = "";
    public final List<Block> blocks = new ArrayList<>();
    public final List<Cb> cbs = new ArrayList<>();
    public final List<String> setup = new ArrayList<>();
    public final List<Element> elements = new ArrayList<>();
    public final List<String> help = new ArrayList<>();
    public final List<String> notes = new ArrayList<>();
    public final Map<String, String> ids = new HashMap<>();
    public int minX, minY, minZ, maxX, maxY, maxZ;

    public int solid() {
        int n = 0;
        for (Block b : blocks) if (!b.st().isAir()) n++;
        return n;
    }

    public int ops() { return blocks.size() + cbs.size(); }

    public int mechanisms() {
        int n = 0;
        for (Element e : elements) if (e.kind().equals("mechanism")) n++;
        return n;
    }

    public void computeBounds() {
        boolean first = true;
        for (Block b : blocks) {
            if (first) { minX = maxX = b.x(); minY = maxY = b.y(); minZ = maxZ = b.z(); first = false; }
            ext(b.x(), b.y(), b.z());
        }
        for (Cb c : cbs) {
            if (first) { minX = maxX = c.x(); minY = maxY = c.y(); minZ = maxZ = c.z(); first = false; }
            ext(c.x(), c.y(), c.z());
        }
    }

    private void ext(int x, int y, int z) {
        minX = Math.min(minX, x); maxX = Math.max(maxX, x);
        minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
    }

    public int sx() { return maxX - minX + 1; }
    public int sy() { return maxY - minY + 1; }
    public int sz() { return maxZ - minZ + 1; }

    public Box box() { return new Box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1); }

    /** Этапы: расчистка/конструкция/детали по элементам + настройка + командные блоки. */
    public int stages() { return elements.size() * 3 + 2; }
}
