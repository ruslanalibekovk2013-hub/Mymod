package com.mackros;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Беспроводной пульт. ПКМ: переключатель (или импульс). Приседание + ПКМ: следующий канал. */
public class Remote extends Item {
    private final boolean pulse;

    public Remote(boolean pulse) {
        super(new Item.Settings().maxCount(1));
        this.pulse = pulse;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack st = player.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(st);
        int ch = st.getOrCreateNbt().getInt("ch");
        if (player.isSneaking()) {
            ch = (ch + 1) % Chan.N;
            st.getOrCreateNbt().putInt("ch", ch);
            player.sendMessage(Text.literal("§bПульт: канал §f" + (ch + 1)), true);
        } else if (pulse) {
            Chan.pulse(ch, world.getTime() + 10);
            player.sendMessage(Text.literal("§bКанал " + (ch + 1) + ": §eимпульс"), true);
        } else {
            boolean on = Chan.toggle(ch);
            player.sendMessage(Text.literal("§bКанал " + (ch + 1) + ": " + (on ? "§aвключен" : "§cвыключен")), true);
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.6f, 1.2f);
        return TypedActionResult.success(st);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        int ch = stack.hasNbt() ? stack.getNbt().getInt("ch") : 0;
        tooltip.add(Text.literal("§7Канал: §f" + (ch + 1) + " §7(присед + ПКМ — сменить)"));
        tooltip.add(Text.literal(pulse ? "§7ПКМ: короткий импульс" : "§7ПКМ: включить / выключить"));
    }
}
