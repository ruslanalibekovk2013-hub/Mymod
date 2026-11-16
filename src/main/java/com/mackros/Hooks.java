package com.mackros;

import net.minecraft.util.math.BlockPos;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Точки связи общего кода блоков с клиентскими экранами. */
public final class Hooks {
    public static Consumer<BlockPos> elevatorScreen = p -> { };
    public static Runnable calcScreen = () -> { };
    public static Runnable panelScreen = () -> { };
    public static BiConsumer<BlockPos, Boolean> marker = (p, b) -> { };
}
