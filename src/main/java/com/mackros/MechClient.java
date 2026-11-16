package com.mackros;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public final class MechClient {
    /** Запрос выдачи механизма из каталога меню. */
    public static void give(String id) {
        PacketByteBuf b = PacketByteBufs.create();
        b.writeString(id, 64);
        ClientPlayNetworking.send(Net.GIVE, b);
    }

    private MechClient() { }
}
