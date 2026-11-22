package com.mackros;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** Portal Gun: ПКМ ставит синий портал, Shift+ПКМ оранжевый на вертикальную стену. Два портала игрока соединены. */
public class PortalGunItem extends Item {
    public PortalGunItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack st = player.getStackInHand(hand);
        if (!world.isClient && player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
            boolean orange = player.isSneaking();
            HitResult h = player.raycast(64.0, 0.0f, false);
            if (h.getType() == HitResult.Type.BLOCK) {
                BlockHitResult b = (BlockHitResult) h;
                String err = NeroPortals.place(sw, sp, b.getBlockPos(), b.getSide(), orange);
                if (err != null) player.sendMessage(Text.literal("Portal Gun: " + err), true);
            } else player.sendMessage(Text.literal("Portal Gun: нет поверхности в зоне досягаемости (64 блока)"), true);
        }
        return TypedActionResult.success(st, world.isClient());
    }
}
