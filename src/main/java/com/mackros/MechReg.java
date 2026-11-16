package com.mackros;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/** Регистрация всех механизмов из MechDefs и выдача предметов из каталога меню. */
public final class MechReg {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

    public static void init() {
        for (MechDefs.Def d : MechDefs.ALL) {
            Identifier key = new Identifier(MackrosMod.ID, d.id);
            Block b = Mech.make(d);
            Registry.register(Registries.BLOCK, key, b);
            Registry.register(Registries.ITEM, key, new BlockItem(b, new Item.Settings()));
            BLOCKS.put(d.id, b);
        }
    }

    /** Сервер проверяет права: креатив или оператор. */
    public static void give(ServerPlayerEntity p, String id) {
        Block b = BLOCKS.get(id);
        if (b == null) return;
        if (!(p.isCreative() || p.hasPermissionLevel(2))) {
            p.sendMessage(Text.literal("§cВыдача из каталога: только креатив или оператор"), true);
            return;
        }
        p.getInventory().offerOrDrop(new ItemStack(b.asItem()));
        p.sendMessage(Text.literal("§aВыдано: ").append(b.getName()), true);
    }

    private MechReg() { }
}
