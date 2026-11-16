package com.mackros;

import net.minecraft.item.Item;

public class MackrosTool extends Item {
    /** 0 жезл выделения, 1 жезл вставки, 2 кисть, 3 ластик, 4 жезл меню */
    public final int kind;

    public MackrosTool(int kind) {
        super(new Item.Settings().maxCount(1));
        this.kind = kind;
    }
}
