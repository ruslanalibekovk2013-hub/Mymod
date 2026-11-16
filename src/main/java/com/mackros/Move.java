package com.mackros;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/** Логика меню переноса: сдвиг, клон, обмен, координаты, посадка на землю, быстрые выделения. */
public final class Move {
    public static final String[] MODES = {"Сдвиг", "Клон", "Буфер"};
    /** 0 сдвиг выделения, 1 клон выделения, 2 смещение места вставки буфера */
    public static int mode = 0;
    public static int count = 1;
    public static boolean air = false;

    /** k: 0 вперёд, 1 назад, 2 влево, 3 вправо, 4 вверх, 5 вниз. */
    public static int[] vec(MinecraftClient mc, int k) {
        if (k == 4) return new int[]{0, 1, 0};
        if (k == 5) return new int[]{0, -1, 0};
        Direction d;
        if (Config.relMove) {
            Direction f = mc.player.getHorizontalFacing();
            d = k == 0 ? f : k == 1 ? f.getOpposite() : k == 2 ? f.rotateYCounterclockwise() : f.rotateYClockwise();
        } else {
            d = k == 0 ? Direction.NORTH : k == 1 ? Direction.SOUTH : k == 2 ? Direction.WEST : Direction.EAST;
        }
        return new int[]{d.getOffsetX(), 0, d.getOffsetZ()};
    }

    public static void go(MinecraftClient mc, int k, int n) {
        int[] v = vec(mc, k);
        int dx = v[0] * n, dy = v[1] * n, dz = v[2] * n;
        if (mode == 0) Editor.moveSel(mc, dx, dy, dz);
        else if (mode == 1) clone(mc, v[0] * n, v[1] * n, v[2] * n, count);
        else Editor.nudge(dx, dy, dz);
    }

    /** Копирует выделение n раз со сдвигом (dx,dy,dz) каждый раз. Исходник не трогает. */
    public static void clone(MinecraftClient mc, int dx, int dy, int dz, int n) {
        if (!Editor.sel()) return;
        if (Editor.vol() * n > Config.maxVolume) { Editor.msg("Слишком большой клон"); return; }
        BlockPos a = Editor.min(), b = Editor.max();
        List<Editor.Change> ch = new ArrayList<>();
        for (int i = 1; i <= n; i++)
            for (int x = a.getX(); x <= b.getX(); x++)
                for (int y = a.getY(); y <= b.getY(); y++)
                    for (int z = a.getZ(); z <= b.getZ(); z++) {
                        BlockState s = mc.world.getBlockState(new BlockPos(x, y, z));
                        if (s.isAir() && !air) continue;
                        Editor.addSym(mc, ch, new BlockPos(x + dx * i, y + dy * i, z + dz * i), s);
                    }
        Editor.apply(mc, ch);
    }

    /** Обмен содержимого выделения и такой же области, сдвинутой на (dx,dy,dz). Области не должны пересекаться. */
    public static void swap(MinecraftClient mc, int dx, int dy, int dz) {
        if (!Editor.sel()) return;
        BlockPos a = Editor.min(), b = Editor.max();
        int sx = b.getX() - a.getX() + 1, sy = b.getY() - a.getY() + 1, sz = b.getZ() - a.getZ() + 1;
        if (Math.abs(dx) < sx && Math.abs(dy) < sy && Math.abs(dz) < sz) { Editor.msg("Области пересекаются: увеличь шаг"); return; }
        List<Editor.Change> ch = new ArrayList<>();
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int y = a.getY(); y <= b.getY(); y++)
                for (int z = a.getZ(); z <= b.getZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z), q = p.add(dx, dy, dz);
                    BlockState sp = mc.world.getBlockState(p), sq = mc.world.getBlockState(q);
                    if (sp == sq) continue;
                    ch.add(new Editor.Change(p, sp, sq));
                    ch.add(new Editor.Change(q, sq, sp));
                }
        Editor.apply(mc, ch);
    }

    /** Перенос выделения так, чтобы его нижний угол оказался в (x,y,z). */
    public static void moveTo(MinecraftClient mc, int x, int y, int z) {
        if (!Editor.sel()) return;
        BlockPos a = Editor.min();
        Editor.moveSel(mc, x - a.getX(), y - a.getY(), z - a.getZ());
    }

    public static void moveToLook(MinecraftClient mc) {
        BlockPos p = Editor.look(mc, true);
        if (p == null) { Editor.msg("Нет блока в прицеле"); return; }
        moveTo(mc, p.getX(), p.getY(), p.getZ());
    }

    /** Вставка буфера с углом в (x,y,z), независимо от прицела. */
    public static void pasteAt(MinecraftClient mc, int x, int y, int z) {
        if (Editor.clip.isEmpty()) { Editor.msg("Буфер пуст"); return; }
        BlockPos base = new BlockPos(x, y, z);
        List<Editor.Change> ch = new ArrayList<>();
        for (Editor.Entry e : Editor.clip) {
            if (!Editor.pasteAir && e.s().isAir()) continue;
            if (!Editor.layerOk(e.y())) continue;
            Editor.addSym(mc, ch, base.add(e.x(), e.y(), e.z()), e.s());
        }
        Editor.apply(mc, ch);
        if (Config.autoSelect) {
            int[] sz = Editor.size();
            Editor.pos1 = base;
            Editor.pos2 = base.add(Math.max(0, sz[0] - 1), Math.max(0, sz[1] - 1), Math.max(0, sz[2] - 1));
        }
    }

    /** Опустить выделение вниз, пока оно не встанет на твёрдый блок. */
    public static void drop(MinecraftClient mc) {
        if (!Editor.sel()) return;
        BlockPos a = Editor.min(), b = Editor.max();
        int best = Integer.MAX_VALUE;
        for (int x = a.getX(); x <= b.getX(); x++)
            for (int z = a.getZ(); z <= b.getZ(); z++) {
                int d = 0;
                for (int y = a.getY() - 1; y >= mc.world.getBottomY() && d < 192; y--, d++) {
                    BlockState s = mc.world.getBlockState(new BlockPos(x, y, z));
                    if (!s.isAir() && !s.isLiquid()) break;
                }
                best = Math.min(best, d);
            }
        if (best == Integer.MAX_VALUE || best <= 0) { Editor.msg("Уже стоит на земле"); return; }
        Editor.moveSel(mc, 0, -best, 0);
    }

    /** Поставить центр выделения на игрока (по горизонтали), низ на уровне ног. */
    public static void toPlayer(MinecraftClient mc) {
        if (!Editor.sel()) return;
        BlockPos a = Editor.min(), b = Editor.max(), p = mc.player.getBlockPos();
        int cx = (a.getX() + b.getX()) / 2, cz = (a.getZ() + b.getZ()) / 2;
        Editor.moveSel(mc, p.getX() - cx, p.getY() - a.getY(), p.getZ() - cz);
    }

    public static void around(MinecraftClient mc, int r) {
        BlockPos p = mc.player.getBlockPos();
        Editor.pos1 = p.add(-r, -r, -r);
        Editor.pos2 = p.add(r, r, r);
        Editor.msg("Выделено вокруг игрока, радиус " + r);
    }

    public static void chunk(MinecraftClient mc, boolean full) {
        BlockPos p = mc.player.getBlockPos();
        int x0 = (p.getX() >> 4) << 4, z0 = (p.getZ() >> 4) << 4;
        int y0 = full ? mc.world.getBottomY() : p.getY() - 32, y1 = full ? mc.world.getTopY() - 1 : p.getY() + 32;
        Editor.pos1 = new BlockPos(x0, y0, z0);
        Editor.pos2 = new BlockPos(x0 + 15, y1, z0 + 15);
        Editor.msg("Выделен чанк " + (x0 >> 4) + " " + (z0 >> 4));
    }

    public static void swapPoints() {
        BlockPos t = Editor.pos1;
        Editor.pos1 = Editor.pos2;
        Editor.pos2 = t;
        Editor.msg("Точки 1 и 2 поменяны");
    }

    public static String dims() {
        if (Editor.pos1 == null || Editor.pos2 == null) return "нет выделения";
        BlockPos a = Editor.min(), b = Editor.max();
        return (b.getX() - a.getX() + 1) + "x" + (b.getY() - a.getY() + 1) + "x" + (b.getZ() - a.getZ() + 1);
    }

    private Move() { }
}
