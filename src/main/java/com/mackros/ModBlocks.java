package com.mackros;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final Block ELEVATOR = reg("elevator", new MBlocks.Elevator(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));
    public static final Block CALCULATOR = reg("calculator", new MBlocks.Calculator(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));
    public static final Block PANEL = reg("panel", new MBlocks.Panel(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));
    public static final Block TELEPORT_PAD = reg("teleport_pad", new MBlocks.TeleportPad(AbstractBlock.Settings.copy(Blocks.OBSIDIAN)));
    public static final Block TRAMPOLINE = reg("trampoline", new MBlocks.Trampoline(AbstractBlock.Settings.copy(Blocks.SLIME_BLOCK)));
    public static final Block BOOSTER = reg("booster", new MBlocks.Booster(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));
    public static final Block FAN = reg("fan", new MBlocks.Fan(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));
    public static final Block MAGNET = reg("magnet", new MBlocks.Magnet(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));
    public static final Block HEAL_PAD = reg("heal_pad", new MBlocks.HealPad(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));
    public static final Block LAMP = reg("lamp", new Block(AbstractBlock.Settings.copy(Blocks.GLOWSTONE)));
    public static final Block MARKER_A = reg("marker_a", new MBlocks.Marker(false, AbstractBlock.Settings.copy(Blocks.STONE)));
    public static final Block MARKER_B = reg("marker_b", new MBlocks.Marker(true, AbstractBlock.Settings.copy(Blocks.STONE)));

    private static Block reg(String id, Block block) {
        Identifier key = new Identifier(MackrosMod.ID, id);
        Registry.register(Registries.BLOCK, key, block);
        Registry.register(Registries.ITEM, key, new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void init() { }
}
