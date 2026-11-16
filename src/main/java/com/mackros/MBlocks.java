package com.mackros;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Все механизмы мода. */
public final class MBlocks {

    // ---------- Лифт ----------
    public static class Elevator extends Block {
        public Elevator(Settings s) { super(s); }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (world.isClient) Hooks.elevatorScreen.accept(pos.toImmutable());
            return ActionResult.SUCCESS;
        }

        public static void travel(ServerPlayerEntity p, BlockPos src, int ty) {
            ServerWorld w = p.getServerWorld();
            if (p.squaredDistanceTo(Vec3d.ofCenter(src)) > 144) return;
            if (!w.getBlockState(src).isOf(ModBlocks.ELEVATOR)) return;
            BlockPos dst = new BlockPos(src.getX(), ty, src.getZ());
            if (!w.getBlockState(dst).isOf(ModBlocks.ELEVATOR)) return;
            BlockPos a = dst.up(), b = dst.up(2);
            if (!w.getBlockState(a).getCollisionShape(w, a).isEmpty() || !w.getBlockState(b).getCollisionShape(w, b).isEmpty()) return;
            w.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 25, 0.3, 0.6, 0.3, 0.3);
            p.requestTeleport(dst.getX() + 0.5, dst.getY() + 1.0, dst.getZ() + 0.5);
            p.fallDistance = 0;
            w.playSound(null, dst, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 0.8f, 1.2f);
            w.spawnParticles(ParticleTypes.PORTAL, dst.getX() + 0.5, dst.getY() + 2, dst.getZ() + 0.5, 25, 0.3, 0.6, 0.3, 0.3);
        }
    }

    // ---------- Калькулятор ----------
    public static class Calculator extends Block {
        public Calculator(Settings s) { super(s); }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (world.isClient) Hooks.calcScreen.run();
            return ActionResult.SUCCESS;
        }
    }

    // ---------- Пульт ----------
    public static class Panel extends Block {
        public Panel(Settings s) { super(s); }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (world.isClient) Hooks.panelScreen.run();
            return ActionResult.SUCCESS;
        }
    }

    // ---------- Телепорт-плита ----------
    public static class TeleportPad extends Block {
        private static final Map<UUID, Long> LAST = new HashMap<>();

        public TeleportPad(Settings s) { super(s); }

        @Override
        public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
            super.onSteppedOn(world, pos, state, entity);
            if (world.isClient || !(entity instanceof ServerPlayerEntity sp)) return;
            long now = world.getTime();
            Long last = LAST.get(sp.getUuid());
            if (last != null && now - last < 60) return;
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            for (int dx = -32; dx <= 32; dx++)
                for (int dz = -32; dz <= 32; dz++)
                    for (int dy = -16; dy <= 16; dy++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos p = pos.add(dx, dy, dz);
                        if (!world.isChunkLoaded(p)) continue;
                        if (world.getBlockState(p).isOf(this)) {
                            double d = p.getSquaredDistance(pos);
                            if (d < bd) { bd = d; best = p; }
                        }
                    }
            if (best == null) return;
            LAST.put(sp.getUuid(), now);
            ServerWorld sw = (ServerWorld) world;
            sw.spawnParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1, sp.getZ(), 30, 0.3, 0.6, 0.3, 0.4);
            sp.requestTeleport(best.getX() + 0.5, best.getY() + 1.0, best.getZ() + 0.5);
            sw.playSound(null, best, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1f, 1f);
            sw.spawnParticles(ParticleTypes.PORTAL, best.getX() + 0.5, best.getY() + 2, best.getZ() + 0.5, 30, 0.3, 0.6, 0.3, 0.4);
        }
    }

    // ---------- Батут ----------
    public static class Trampoline extends Block {
        public Trampoline(Settings s) { super(s); }

        @Override
        public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
            if (entity.bypassesLandingEffects()) super.onLandedUpon(world, state, pos, entity, fallDistance);
            else entity.handleFallDamage(fallDistance, 0.0F, world.getDamageSources().fall());
        }

        @Override
        public void onEntityLand(BlockView world, Entity entity) {
            if (entity.bypassesLandingEffects()) { super.onEntityLand(world, entity); return; }
            Vec3d v = entity.getVelocity();
            if (v.y < -0.1) {
                entity.setVelocity(v.x, Math.min(2.2, -v.y * 1.35 + 0.15), v.z);
                entity.velocityModified = true;
            }
        }
    }

    // ---------- Ускоряющая плита ----------
    public static class Booster extends Block {
        public Booster(Settings s) { super(s); }

        @Override
        public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
            super.onSteppedOn(world, pos, state, entity);
            if (entity instanceof LivingEntity) {
                Vec3d look = entity.getRotationVector();
                entity.setVelocity(look.x * 1.2, Math.max(entity.getVelocity().y, 0.2), look.z * 1.2);
                entity.velocityModified = true;
            }
        }
    }

    // ---------- Вентилятор ----------
    public static class Fan extends Block {
        public Fan(Settings s) { super(s); }

        @Override
        public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
            super.onBlockAdded(state, world, pos, oldState, notify);
            if (!world.isClient) world.scheduleBlockTick(pos, this, 4);
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            Box box = new Box(pos.up()).stretch(0, 8, 0);
            for (Entity e : world.getEntitiesByClass(Entity.class, box, en -> !en.isSpectator())) {
                Vec3d v = e.getVelocity();
                e.setVelocity(v.x, Math.min(v.y + 0.12, 0.6), v.z);
                e.fallDistance = 0;
                e.velocityModified = true;
            }
            world.scheduleBlockTick(pos, this, 2);
        }
    }

    // ---------- Магнит предметов ----------
    public static class Magnet extends Block {
        public Magnet(Settings s) { super(s); }

        @Override
        public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
            super.onBlockAdded(state, world, pos, oldState, notify);
            if (!world.isClient) world.scheduleBlockTick(pos, this, 4);
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            Vec3d target = Vec3d.ofCenter(pos.up());
            for (ItemEntity it : world.getEntitiesByClass(ItemEntity.class, new Box(pos).expand(8), en -> true)) {
                Vec3d d = target.subtract(it.getPos());
                if (d.length() > 0.8) {
                    it.setVelocity(d.normalize().multiply(0.35));
                    it.velocityModified = true;
                }
            }
            world.scheduleBlockTick(pos, this, 2);
        }
    }

    // ---------- Лечебная плита ----------
    public static class HealPad extends Block {
        public HealPad(Settings s) { super(s); }

        @Override
        public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
            super.onSteppedOn(world, pos, state, entity);
            if (!world.isClient && world.getTime() % 20 == 0 && entity instanceof LivingEntity le) {
                le.heal(1.0f);
                ((ServerWorld) world).spawnParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + 1.2, entity.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    // ---------- Маркеры выделения ----------
    public static class Marker extends Block {
        private final boolean second;

        public Marker(boolean second, Settings s) { super(s); this.second = second; }

        @Override
        public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
            super.onPlaced(world, pos, state, placer, stack);
            if (world.isClient) Hooks.marker.accept(pos.toImmutable(), second);
        }
    }

    private MBlocks() { }
}
