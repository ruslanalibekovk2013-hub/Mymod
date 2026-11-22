package com.mackros;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.math.BlockPos;

public class MackrosMod implements ModInitializer {
    public static final String ID = "mackros";

    @Override
    public void onInitialize() {
        ModBlocks.init();
        MechReg.init();
        ModItems.init();
        ModGroup.init();
        NeroPrint.register();
        NeroPortals.register();
        ServerPlayNetworking.registerGlobalReceiver(Net.GIVE, (server, player, handler, buf, sender) -> {
            String id = buf.readString(64);
            server.execute(() -> MechReg.give(player, id));
        });
        ServerPlayNetworking.registerGlobalReceiver(Net.ELEVATOR, (server, player, handler, buf, sender) -> {
            BlockPos src = buf.readBlockPos();
            int ty = buf.readInt();
            server.execute(() -> MBlocks.Elevator.travel(player, src, ty));
        });
    }
}
