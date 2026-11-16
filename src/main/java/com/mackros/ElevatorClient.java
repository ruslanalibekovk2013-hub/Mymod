package com.mackros;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class ElevatorClient {
    private static boolean pj, ps;
    private static int cd;

    public static List<Integer> floors(World w, BlockPos p) {
        List<Integer> l = new ArrayList<>();
        for (int y = w.getBottomY(); y < w.getTopY(); y++)
            if (w.getBlockState(new BlockPos(p.getX(), y, p.getZ())).isOf(ModBlocks.ELEVATOR)) l.add(y);
        return l;
    }

    public static boolean open(World w, BlockPos f) {
        BlockPos a = f.up(), b = f.up(2);
        return w.getBlockState(a).getCollisionShape(w, a).isEmpty() && w.getBlockState(b).getCollisionShape(w, b).isEmpty();
    }

    public static void send(BlockPos src, int ty) {
        PacketByteBuf b = PacketByteBufs.create();
        b.writeBlockPos(src);
        b.writeInt(ty);
        ClientPlayNetworking.send(Net.ELEVATOR, b);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) { pj = false; ps = false; return; }
        if (cd > 0) cd--;
        BlockPos below = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 0.1, mc.player.getZ());
        boolean on = mc.world.getBlockState(below).isOf(ModBlocks.ELEVATOR);
        boolean j = mc.options.jumpKey.isPressed(), s = mc.options.sneakKey.isPressed();
        if (on && cd == 0) {
            if (j && !pj) go(mc, below, 1);
            else if (s && !ps) go(mc, below, -1);
        }
        pj = j; ps = s;
    }

    private static void go(MinecraftClient mc, BlockPos cur, int dir) {
        List<Integer> fl = floors(mc.world, cur);
        Integer target = null;
        for (int y : fl) {
            if (!open(mc.world, new BlockPos(cur.getX(), y, cur.getZ()))) continue;
            if (dir > 0 && y > cur.getY()) { target = y; break; }
            if (dir < 0 && y < cur.getY()) target = y;
        }
        if (target == null) return;
        cd = 10;
        send(cur, target);
    }

    private ElevatorClient() { }
}
