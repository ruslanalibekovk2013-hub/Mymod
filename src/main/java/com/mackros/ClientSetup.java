package com.mackros;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

public final class ClientSetup {
    public static void init() {
        Hooks.elevatorScreen = pos -> MinecraftClient.getInstance().setScreen(new ElevatorScreen(pos));
        Hooks.calcScreen = () -> MinecraftClient.getInstance().setScreen(new CalculatorScreen());
        Hooks.panelScreen = () -> MinecraftClient.getInstance().setScreen(new NeroHubScreen());
        Hooks.marker = (pos, second) -> {
            if (second) Editor.pos2 = pos; else Editor.pos1 = pos;
            Editor.msg("Маркер " + (second ? "B" : "A") + ": " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        };
        ClientTickEvents.END_CLIENT_TICK.register(ElevatorClient::tick);

        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
            ItemStack st = player.getStackInHand(hand);
            if (st.getItem() instanceof MackrosTool t && t.kind == 0) {
                if (world.isClient) {
                    Editor.pos1 = pos.toImmutable();
                    Editor.msg("Точка 1: " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
                    return ActionResult.SUCCESS;
                }
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack st = player.getStackInHand(hand);
            if (!world.isClient || !(st.getItem() instanceof MackrosTool t)) return TypedActionResult.pass(st);
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean sneak = player.isSneaking();
            switch (t.kind) {
                case 0 -> Editor.setPos(mc, sneak ? 1 : 2);
                case 1 -> { if (sneak) Editor.copy(mc); else Editor.paste(mc); }
                case 2 -> Editor.sphereBlock(mc, Editor.offhandOrPalette(mc), sneak);
                case 3 -> Editor.sphereBlock(mc, Blocks.AIR.getDefaultState(), false);
                default -> mc.setScreen(sneak ? new MoveScreen() : new NeroHubScreen());
            }
            return TypedActionResult.success(st);
        });
    }

    private ClientSetup() { }
}
