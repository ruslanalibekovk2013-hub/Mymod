package com.mackros;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModGroup {
    public static void init() {
        ItemGroup g = FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModBlocks.ELEVATOR))
                .displayName(Text.translatable("itemGroup.mackros.main"))
                .entries((ctx, e) -> {
                    e.add(ModItems.WAND);
                    e.add(ModItems.PASTER);
                    e.add(ModItems.BRUSH);
                    e.add(ModItems.ERASER);
                    e.add(ModItems.MENU_WAND);
                    e.add(ModItems.PORTAL_GUN);
                    e.add(ModBlocks.MARKER_A);
                    e.add(ModBlocks.MARKER_B);
                    e.add(ModBlocks.ELEVATOR);
                    e.add(ModBlocks.CALCULATOR);
                    e.add(ModBlocks.PANEL);
                    e.add(ModBlocks.TELEPORT_PAD);
                    e.add(ModBlocks.TRAMPOLINE);
                    e.add(ModBlocks.BOOSTER);
                    e.add(ModBlocks.FAN);
                    e.add(ModBlocks.MAGNET);
                    e.add(ModBlocks.HEAL_PAD);
                    e.add(ModBlocks.LAMP);
                    e.add(ModItems.REMOTE);
                    e.add(ModItems.REMOTE_PULSE);
                    e.add(ModBlocks.TIMER);
                    e.add(ModBlocks.TRANSMITTER);
                    e.add(ModBlocks.RECEIVER);
                    e.add(ModBlocks.LOGIC);
                    e.add(ModBlocks.SENSOR);
                    e.add(ModBlocks.LAUNCHER);
                    e.add(ModBlocks.CONVEYOR);
                    e.add(ModBlocks.MODULE);
                })
                .build();
        Registry.register(Registries.ITEM_GROUP, new Identifier(MackrosMod.ID, "main"), g);
        ItemGroup m = FabricItemGroup.builder()
                .icon(() -> new ItemStack(MechReg.BLOCKS.get("spawn_cow")))
                .displayName(Text.translatable("itemGroup.mackros.machines"))
                .entries((ctx, e) -> { for (net.minecraft.block.Block b : MechReg.BLOCKS.values()) e.add(b); })
                .build();
        Registry.register(Registries.ITEM_GROUP, new Identifier(MackrosMod.ID, "machines"), m);
    }
}
