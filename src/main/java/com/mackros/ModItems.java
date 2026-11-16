package com.mackros;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item WAND = reg("wand", new MackrosTool(0));
    public static final Item PASTER = reg("paster", new MackrosTool(1));
    public static final Item BRUSH = reg("brush", new MackrosTool(2));
    public static final Item ERASER = reg("eraser", new MackrosTool(3));
    public static final Item MENU_WAND = reg("menu_wand", new MackrosTool(4));

    private static Item reg(String id, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MackrosMod.ID, id), item);
    }

    public static void init() { }
}
