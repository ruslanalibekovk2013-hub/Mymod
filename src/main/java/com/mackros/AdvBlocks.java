package com.mackros;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Продвинутые настраиваемые блоки: настройка по ПКМ (присед + пустая рука — в обратную сторону / вторая настройка). */
public final class AdvBlocks {

    private static void bar(PlayerEntity p, String s) { p.sendMessage(Text.literal(s), true); }

    private static int cyc(int v, int min, int max, boolean back) {
        v += back ? -1 : 1;
        if (v > max) v = min;
        if (v < min) v = max;
        return v;
    }

    /** База: блок, который сам себя тикает (как вентилятор и магнит). */
    abstract static class Ticking extends Block {
        Ticking(Settings s) { super(s); }
        abstract int period();

        @Override
        public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
            super.onBlockAdded(state, world, pos, oldState, notify);
            if (!world.isClient) world.scheduleBlockTick(pos, this, period());
        }
    }

    // ---------- Таймер ----------
    public static class Timer extends Ticking {
        public static final IntProperty DELAY = IntProperty.of("delay", 1, 15);
        public static final BooleanProperty POWERED = Properties.POWERED;

        public Timer(Settings s) { super(s); setDefaultState(getDefaultState().with(DELAY, 2).with(POWERED, false)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(DELAY, POWERED); }
        @Override int period() { return 20; }
        @Override public boolean emitsRedstonePower(BlockState s) { return true; }
        @Override public int getWeakRedstonePower(BlockState s, BlockView w, BlockPos p, Direction d) { return s.get(POWERED) ? 15 : 0; }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient) {
                int d = cyc(state.get(DELAY), 1, 15, player.isSneaking());
                world.setBlockState(pos, state.with(DELAY, d), Block.NOTIFY_ALL);
                world.scheduleBlockTick(pos, this, 2);
                bar(player, "§bТаймер: импульс каждые §f" + d + " §bсек");
            }
            return ActionResult.SUCCESS;
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            if (state.get(POWERED)) {
                world.setBlockState(pos, state.with(POWERED, false), Block.NOTIFY_ALL);
                world.scheduleBlockTick(pos, this, Math.max(2, state.get(DELAY) * 20 - 4));
            } else {
                world.setBlockState(pos, state.with(POWERED, true), Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3f, 1.5f);
                world.scheduleBlockTick(pos, this, 4);
            }
        }
    }

    // ---------- Передатчик ----------
    public static class Transmitter extends Ticking {
        public static final IntProperty CH = IntProperty.of("channel", 0, 15);

        public Transmitter(Settings s) { super(s); setDefaultState(getDefaultState().with(CH, 0)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(CH); }
        @Override int period() { return 2; }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient) {
                int c = cyc(state.get(CH), 0, 15, player.isSneaking());
                world.setBlockState(pos, state.with(CH, c), Block.NOTIFY_ALL);
                world.scheduleBlockTick(pos, this, 2);
                bar(player, "§bПередатчик: канал §f" + (c + 1));
            }
            return ActionResult.SUCCESS;
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            if (world.isReceivingRedstonePower(pos)) {
                Chan.pulse(state.get(CH), world.getTime() + 8);
                world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.02);
            }
            world.scheduleBlockTick(pos, this, 2);
        }
    }

    // ---------- Приёмник ----------
    public static class Receiver extends Ticking {
        public static final IntProperty CH = IntProperty.of("channel", 0, 15);
        public static final BooleanProperty POWERED = Properties.POWERED;

        public Receiver(Settings s) { super(s); setDefaultState(getDefaultState().with(CH, 0).with(POWERED, false)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(CH, POWERED); }
        @Override int period() { return 2; }
        @Override public boolean emitsRedstonePower(BlockState s) { return true; }
        @Override public int getWeakRedstonePower(BlockState s, BlockView w, BlockPos p, Direction d) { return s.get(POWERED) ? 15 : 0; }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient) {
                int c = cyc(state.get(CH), 0, 15, player.isSneaking());
                world.setBlockState(pos, state.with(CH, c), Block.NOTIFY_ALL);
                world.scheduleBlockTick(pos, this, 2);
                bar(player, "§bПриёмник: канал §f" + (c + 1));
            }
            return ActionResult.SUCCESS;
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            boolean on = Chan.active(state.get(CH), world.getTime());
            if (on != state.get(POWERED)) world.setBlockState(pos, state.with(POWERED, on), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, 2);
        }
    }

    // ---------- Логический блок ----------
    public static class Logic extends Block {
        public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
        public static final IntProperty MODE = IntProperty.of("mode", 0, 9);
        public static final BooleanProperty POWERED = Properties.POWERED;
        public static final BooleanProperty LAST = BooleanProperty.of("last");
        public static final String[] NAMES = {"И (A и B)", "ИЛИ (A или B)", "Искл. ИЛИ (XOR)", "И-НЕ (NAND)", "ИЛИ-НЕ (NOR)",
                "XNOR (равны)", "НЕ A (инвертор)", "A и не B", "Триггер: A вкл, B выкл", "T-триггер по фронту A"};

        public Logic(Settings s) {
            super(s);
            setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(MODE, 0).with(POWERED, false).with(LAST, false));
        }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FACING, MODE, POWERED, LAST); }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
        }

        @Override public boolean emitsRedstonePower(BlockState s) { return true; }

        @Override
        public int getWeakRedstonePower(BlockState s, BlockView w, BlockPos p, Direction d) {
            return s.get(POWERED) && d == s.get(FACING).getOpposite() ? 15 : 0;
        }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient) {
                int m = cyc(state.get(MODE), 0, 9, player.isSneaking());
                world.setBlockState(pos, state.with(MODE, m), Block.NOTIFY_ALL);
                world.scheduleBlockTick(pos, this, 1);
                bar(player, "§bЛогика: §f" + NAMES[m] + " §7(A слева, B справа, выход вперёд)");
            }
            return ActionResult.SUCCESS;
        }

        @Override
        public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
            super.onBlockAdded(state, world, pos, oldState, notify);
            if (!world.isClient) world.scheduleBlockTick(pos, this, 1);
        }

        @Override
        public void neighborUpdate(BlockState state, World world, BlockPos pos, Block source, BlockPos sourcePos, boolean notify) {
            if (!world.isClient) world.scheduleBlockTick(pos, this, 1);
        }

        private static boolean in(World w, BlockPos p, Direction d) { return w.getEmittedRedstonePower(p.offset(d), d) > 0; }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            Direction f = state.get(FACING);
            boolean a = in(world, pos, f.rotateYCounterclockwise());
            boolean b = in(world, pos, f.rotateYClockwise());
            boolean cur = state.get(POWERED);
            boolean out;
            switch (state.get(MODE)) {
                case 0 -> out = a && b;
                case 1 -> out = a || b;
                case 2 -> out = a != b;
                case 3 -> out = !(a && b);
                case 4 -> out = !(a || b);
                case 5 -> out = a == b;
                case 6 -> out = !a;
                case 7 -> out = a && !b;
                case 8 -> out = b ? false : (a ? true : cur);
                default -> out = (a && !state.get(LAST)) ? !cur : cur;
            }
            if (out != cur || a != state.get(LAST)) world.setBlockState(pos, state.with(POWERED, out).with(LAST, a), Block.NOTIFY_ALL);
        }
    }

    // ---------- Датчик ----------
    public static class Sensor extends Ticking {
        public static final IntProperty POWER = Properties.POWER;
        public static final IntProperty RADIUS = IntProperty.of("radius", 1, 15);
        public static final IntProperty MODE = IntProperty.of("mode", 0, 3);
        public static final String[] NAMES = {"игроки", "враждебные мобы", "предметы", "любые живые"};

        public Sensor(Settings s) { super(s); setDefaultState(getDefaultState().with(POWER, 0).with(RADIUS, 4).with(MODE, 0)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(POWER, RADIUS, MODE); }
        @Override int period() { return 5; }
        @Override public boolean emitsRedstonePower(BlockState s) { return true; }
        @Override public int getWeakRedstonePower(BlockState s, BlockView w, BlockPos p, Direction d) { return s.get(POWER); }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient) {
                BlockState n;
                if (player.isSneaking()) n = state.with(MODE, cyc(state.get(MODE), 0, 3, false));
                else n = state.with(RADIUS, cyc(state.get(RADIUS), 1, 15, false));
                world.setBlockState(pos, n, Block.NOTIFY_ALL);
                world.scheduleBlockTick(pos, this, 2);
                bar(player, "§bДатчик: §f" + NAMES[n.get(MODE)] + "§b, радиус §f" + n.get(RADIUS) + " §7(присед — режим)");
            }
            return ActionResult.SUCCESS;
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            Box box = new Box(pos).expand(state.get(RADIUS));
            int n;
            switch (state.get(MODE)) {
                case 0 -> n = world.getEntitiesByClass(PlayerEntity.class, box, e -> !e.isSpectator()).size();
                case 1 -> n = world.getEntitiesByClass(HostileEntity.class, box, e -> true).size();
                case 2 -> n = world.getEntitiesByClass(ItemEntity.class, box, e -> true).size();
                default -> n = world.getEntitiesByClass(LivingEntity.class, box, e -> !e.isSpectator()).size();
            }
            n = Math.min(15, n);
            if (n != state.get(POWER)) world.setBlockState(pos, state.with(POWER, n), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, period());
        }
    }

    // ---------- Катапульта ----------
    public static class Launcher extends Block {
        public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
        public static final IntProperty POWER = IntProperty.of("strength", 1, 8);

        public Launcher(Settings s) { super(s); setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(POWER, 4)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FACING, POWER); }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
        }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient) {
                BlockState n;
                if (player.isSneaking()) n = state.with(FACING, state.get(FACING).rotateYClockwise());
                else n = state.with(POWER, cyc(state.get(POWER), 1, 8, false));
                world.setBlockState(pos, n, Block.NOTIFY_ALL);
                bar(player, "§bКатапульта: сила §f" + n.get(POWER) + "§b, направление §f" + n.get(FACING).asString() + " §7(присед — повернуть)");
            }
            return ActionResult.SUCCESS;
        }

        @Override
        public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
            super.onSteppedOn(world, pos, state, entity);
            if (!(entity instanceof LivingEntity) && !(entity instanceof ItemEntity)) return;
            Direction f = state.get(FACING);
            double p = state.get(POWER);
            entity.setVelocity(f.getOffsetX() * p * 0.3, 0.5 + p * 0.12, f.getOffsetZ() * p * 0.3);
            entity.velocityModified = true;
            entity.fallDistance = 0;
            if (!world.isClient) {
                world.playSound(null, pos, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.BLOCKS, 0.7f, 1.0f);
            }
        }
    }

    // ---------- Конвейер ----------
    public static class Conveyor extends Ticking {
        public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
        public static final IntProperty SPEED = IntProperty.of("speed", 1, 4);

        public Conveyor(Settings s) { super(s); setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(SPEED, 2)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FACING, SPEED); }
        @Override int period() { return 2; }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
        }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient) {
                BlockState n;
                if (player.isSneaking()) n = state.with(FACING, state.get(FACING).rotateYClockwise());
                else n = state.with(SPEED, cyc(state.get(SPEED), 1, 4, false));
                world.setBlockState(pos, n, Block.NOTIFY_ALL);
                world.scheduleBlockTick(pos, this, 2);
                bar(player, "§bКонвейер: скорость §f" + n.get(SPEED) + "§b, направление §f" + n.get(FACING).asString() + " §7(присед — повернуть)");
            }
            return ActionResult.SUCCESS;
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            Direction f = state.get(FACING);
            double t = 0.08 * state.get(SPEED);
            for (Entity e : world.getEntitiesByClass(Entity.class, new Box(pos.up()), en -> !en.isSpectator() && !en.isSneaking())) {
                Vec3d v = e.getVelocity();
                e.setVelocity(v.x + (f.getOffsetX() * t - v.x) * 0.6, v.y, v.z + (f.getOffsetZ() * t - v.z) * 0.6);
                e.velocityModified = true;
            }
            world.scheduleBlockTick(pos, this, 2);
        }
    }

    // ---------- Универсальный модуль ----------
    public static class Module extends Ticking {
        public static final IntProperty MODE = IntProperty.of("mode", 0, 15);
        public static final IntProperty RADIUS = IntProperty.of("radius", 1, 16);
        public static final BooleanProperty POWERED = Properties.POWERED;
        public static final String[] NAMES = {"Лечение", "Скорость", "Прыжок", "Ночное зрение", "Плавное падение", "Сила",
                "Защита", "Огнеупорность", "Дыхание под водой", "Подсветка всех", "Замедление мобов", "Отталкивание",
                "Притяжение", "Антигравитация", "Невидимость", "Поджог врагов"};

        public Module(Settings s) { super(s); setDefaultState(getDefaultState().with(MODE, 0).with(RADIUS, 6).with(POWERED, false)); }

        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(MODE, RADIUS, POWERED); }
        @Override int period() { return 10; }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient) {
                BlockState n;
                if (player.isSneaking()) n = state.with(RADIUS, cyc(state.get(RADIUS), 1, 16, false));
                else n = state.with(MODE, cyc(state.get(MODE), 0, 15, false));
                world.setBlockState(pos, n, Block.NOTIFY_ALL);
                world.scheduleBlockTick(pos, this, 2);
                bar(player, "§bМодуль: §f" + NAMES[n.get(MODE)] + "§b, радиус §f" + n.get(RADIUS) + " §7(присед — радиус, редстоун выключает)");
            }
            return ActionResult.SUCCESS;
        }

        @Override
        public void neighborUpdate(BlockState state, World world, BlockPos pos, Block source, BlockPos sourcePos, boolean notify) {
            if (world.isClient) return;
            boolean p = world.isReceivingRedstonePower(pos);
            if (p != state.get(POWERED)) world.setBlockState(pos, state.with(POWERED, p), Block.NOTIFY_ALL);
        }

        private static void eff(ServerWorld w, Box b, StatusEffect e, int amp, boolean mobs) {
            for (LivingEntity le : w.getEntitiesByClass(LivingEntity.class, b, x -> mobs ? x instanceof HostileEntity : x instanceof PlayerEntity))
                le.addStatusEffect(new StatusEffectInstance(e, 60, amp, true, false));
        }

        private static void push(ServerWorld w, BlockPos pos, Box b, double k) {
            Vec3d c = Vec3d.ofCenter(pos);
            for (LivingEntity le : w.getEntitiesByClass(LivingEntity.class, b, x -> !x.isSpectator())) {
                Vec3d d = le.getPos().subtract(c);
                if (d.lengthSquared() < 0.01) continue;
                Vec3d n = d.normalize().multiply(k);
                le.addVelocity(n.x, n.y * 0.3 + (k > 0 ? 0.05 : 0), n.z);
                le.velocityModified = true;
            }
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            if (!state.get(POWERED)) {
                Box box = new Box(pos).expand(state.get(RADIUS));
                switch (state.get(MODE)) {
                    case 0 -> eff(world, box, StatusEffects.REGENERATION, 1, false);
                    case 1 -> eff(world, box, StatusEffects.SPEED, 2, false);
                    case 2 -> eff(world, box, StatusEffects.JUMP_BOOST, 3, false);
                    case 3 -> eff(world, box, StatusEffects.NIGHT_VISION, 0, false);
                    case 4 -> eff(world, box, StatusEffects.SLOW_FALLING, 0, false);
                    case 5 -> eff(world, box, StatusEffects.STRENGTH, 1, false);
                    case 6 -> eff(world, box, StatusEffects.RESISTANCE, 1, false);
                    case 7 -> eff(world, box, StatusEffects.FIRE_RESISTANCE, 0, false);
                    case 8 -> eff(world, box, StatusEffects.WATER_BREATHING, 0, false);
                    case 9 -> {
                        for (LivingEntity le : world.getEntitiesByClass(LivingEntity.class, box, x -> !x.isSpectator()))
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, true, false));
                    }
                    case 10 -> eff(world, box, StatusEffects.SLOWNESS, 3, true);
                    case 11 -> push(world, pos, box, 0.5);
                    case 12 -> push(world, pos, box, -0.25);
                    case 13 -> eff(world, box, StatusEffects.LEVITATION, 0, false);
                    case 14 -> eff(world, box, StatusEffects.INVISIBILITY, 0, false);
                    default -> {
                        for (HostileEntity h : world.getEntitiesByClass(HostileEntity.class, box, x -> true)) h.setOnFireFor(3);
                    }
                }
                world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.01);
            }
            world.scheduleBlockTick(pos, this, period());
        }
    }

    private AdvBlocks() { }
}
