package com.mackros;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.mackros.MechDefs.*;

/**
 * Универсальный блок-механизм Mackros. Поведение задаёт запись MechDefs.Def (тип + параметры).
 * ПКМ пустой рукой: уровень 1..8 (присед = назад). ПКМ палкой: повернуть грань вывода.
 * Редстоун-сигнал выключает механизм (у взрывов/молний — наоборот, включает).
 */
public class Mech extends Block {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final IntProperty LEVEL = IntProperty.of("level", 1, 8);
    public static final BooleanProperty LIT = Properties.LIT;
    public static final BooleanProperty PREV = BooleanProperty.of("prev");
    private static final Map<Integer, Long> CD = new HashMap<>();

    public final MechDefs.Def d;

    public static Mech make(MechDefs.Def d) {
        AbstractBlock.Settings s = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK);
        if (d.type == T_LIGHT) s = s.luminance(st -> st.get(LEVEL) * 2 - 1);
        else if (d.type == T_SENS || d.type == T_LOGIC) s = s.luminance(st -> st.get(LIT) ? 6 : 0);
        return new Mech(s, d);
    }

    private Mech(Settings s, MechDefs.Def d) {
        super(s);
        this.d = d;
        setDefaultState(getDefaultState().with(FACING, Direction.UP).with(LEVEL, 3).with(LIT, false).with(PREV, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FACING, LEVEL, LIT, PREV); }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) { return getDefaultState().with(FACING, ctx.getSide()); }

    @Override public boolean emitsRedstonePower(BlockState s) { return d.type == T_SENS || d.type == T_LOGIC; }

    @Override
    public int getWeakRedstonePower(BlockState s, BlockView w, BlockPos p, Direction dir) {
        return emitsRedstonePower(s) && s.get(LIT) ? 15 : 0;
    }

    private boolean eventDriven() { return d.type == T_LOGIC && d.a.equals("delay"); }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState old, boolean notify) {
        super.onBlockAdded(state, world, pos, old, notify);
        if (old.isOf(this)) return;
        if (!world.isClient && d.type != T_LIGHT && !eventDriven()) world.scheduleBlockTick(pos, this, 4);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block src, BlockPos srcPos, boolean notify) {
        super.neighborUpdate(state, world, pos, src, srcPos, notify);
        if (!world.isClient && eventDriven()) world.scheduleBlockTick(pos, this, state.get(LEVEL) * 4);
    }

    private static int cyc(int v, boolean back) {
        v += back ? -1 : 1;
        return v > 8 ? 1 : v < 1 ? 8 : v;
    }

    private static String dirRu(Direction dir) {
        return switch (dir) {
            case UP -> "вверх"; case DOWN -> "вниз"; case NORTH -> "на север";
            case SOUTH -> "на юг"; case WEST -> "на запад"; default -> "на восток";
        };
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (player.getStackInHand(hand).isOf(Items.STICK)) {
            Direction nf = Direction.values()[(state.get(FACING).ordinal() + 1) % 6];
            world.setBlockState(pos, state.with(FACING, nf), Block.NOTIFY_ALL);
            player.sendMessage(Text.literal("§e" + d.ru + " §7вывод: §f" + dirRu(nf)), true);
            return ActionResult.SUCCESS;
        }
        int lv = cyc(state.get(LEVEL), player.isSneaking());
        world.setBlockState(pos, state.with(LEVEL, lv), Block.NOTIFY_ALL);
        if (d.type != T_LIGHT && !eventDriven()) world.scheduleBlockTick(pos, this, 2);
        player.sendMessage(Text.literal("§b" + d.ru + " §fуровень " + lv + "/8 §7| " + d.desc), true);
        return ActionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ тик
    private int period(int lv) {
        return switch (d.type) {
            case T_PUSH -> 2;
            case T_SPAWN -> Math.max(10, d.b / lv);
            case T_DROP, T_XP -> Math.max(4, d.c / lv);
            case T_GEN -> Math.max(4, d.b / lv);
            case T_BREAK -> Math.max(4, 40 / lv);
            case T_GROW -> Math.max(5, 40 / lv);
            case T_ENV -> 40;
            case T_STRIKE, T_BOOM -> Math.max(10, d.b / lv);
            case T_DMG, T_TP, T_ITEM -> 10;
            case T_SENS -> 4;
            case T_FX -> Math.max(2, 20 / lv);
            default -> 20;
        };
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int lv = state.get(LEVEL);
        Direction dir = state.get(FACING);
        boolean rs = world.isReceivingRedstonePower(pos);
        int next = period(lv);
        if (d.type == T_LIGHT) return;
        if (d.type == T_LOGIC) {
            next = logic(state, world, pos, rs, lv);
            if (next < 0) return;
        } else if (d.type == T_SENS) {
            sense(state, world, pos, lv);
        } else if (d.rs ? rs : !rs) {
            work(world, pos, dir, pos.offset(dir), lv);
        }
        if (world.getBlockState(pos).isOf(this)) world.scheduleBlockTick(pos, this, Math.max(1, next));
    }

    private void work(ServerWorld w, BlockPos pos, Direction dir, BlockPos f, int lv) {
        switch (d.type) {
            case T_EFFECT -> effect(w, pos, lv);
            case T_PUSH -> push(w, pos, dir, lv);
            case T_SPAWN -> spawn(w, pos, f);
            case T_DROP -> drop(w, f, dir);
            case T_XP -> ExperienceOrbEntity.spawn(w, Vec3d.ofCenter(f), d.b * lv);
            case T_GEN -> gen(w, f);
            case T_BREAK -> brk(w, pos, dir, lv);
            case T_GROW -> grow(w, pos, lv);
            case T_ENV -> env(w);
            case T_STRIKE -> strike(w, pos, dir, lv);
            case T_BOOM -> boom(w, pos, dir, lv);
            case T_DMG -> dmg(w, pos, lv);
            case T_SERVICE -> service(w, pos, lv);
            case T_TP -> tp(w, pos, dir, lv);
            case T_FX -> fx(w, f, lv);
            case T_ITEM -> item(w, pos, f, lv);
            default -> { }
        }
    }

    private static boolean ok(Entity e, int m) {
        if (e.isSpectator()) return false;
        return switch (m) {
            case 1 -> e instanceof PlayerEntity;
            case 2 -> e instanceof HostileEntity;
            case 3 -> e instanceof ItemEntity;
            case 4 -> e instanceof LivingEntity && !(e instanceof PlayerEntity);
            case 5 -> e instanceof LivingEntity || e instanceof ItemEntity;
            default -> e instanceof LivingEntity;
        };
    }

    private static boolean cool(Entity e, long now, int ticks) {
        Long last = CD.get(e.getId());
        if (last != null && now - last < ticks) return false;
        if (CD.size() > 512) CD.clear();
        CD.put(e.getId(), now);
        return true;
    }

    // ---------- эффекты
    private void effect(ServerWorld w, BlockPos pos, int lv) {
        StatusEffect ef = Registries.STATUS_EFFECT.get(new Identifier(d.a));
        if (ef == null) return;
        int amp = d.b + (lv - 1) / 3;
        int dur = ef == StatusEffects.NIGHT_VISION ? 260 : 60;
        Box box = new Box(pos).expand(2 + lv);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> ok(x, d.e)))
            e.addStatusEffect(new StatusEffectInstance(ef, dur, amp, true, false));
    }

    // ---------- ветер, пады, притяжение
    private void push(ServerWorld w, BlockPos pos, Direction dir, int lv) {
        double s = Math.min(3.0, d.b / 10.0 * (0.4 + 0.15 * lv));
        int range = d.c + lv;
        Vec3d off = new Vec3d(dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ());
        Vec3d c = Vec3d.ofCenter(pos);
        boolean dirMode = d.a.equals("dir");
        Box box;
        if (dirMode && d.f == 1) box = new Box(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1.4, pos.getZ() + 1);
        else if (dirMode) box = new Box(pos.offset(dir)).stretch(off.multiply(range)).expand(0.8);
        else box = new Box(pos).expand(range);
        for (Entity e : w.getEntitiesByClass(Entity.class, box, x -> ok(x, d.e))) {
            Vec3d v = e.getVelocity();
            if (dirMode && d.f == 1) {
                if (!cool(e, w.getTime(), 8)) continue;
                double mag = Math.min(4.0, d.b / 10.0 * (0.6 + 0.1 * lv));
                v = off.multiply(mag);
                w.playSound(null, pos, SoundEvents.ENTITY_SLIME_JUMP, SoundCategory.BLOCKS, 0.6f, 1.2f);
            } else if (dirMode) {
                v = v.add(off.multiply(s * 0.15));
                double along = v.dotProduct(off);
                if (along > s) v = v.subtract(off.multiply(along - s));
            } else {
                Vec3d dv = c.subtract(e.getPos());
                double dist = dv.length();
                if (dist < 0.6) continue;
                Vec3d n = dv.multiply(1.0 / dist);
                double k = s * 0.15 * Math.max(0.1, 1 - dist / (range + 1.0));
                switch (d.a) {
                    case "pull" -> v = v.add(n.multiply(k));
                    case "repel" -> v = v.add(n.multiply(-k));
                    default -> v = v.add(new Vec3d(-n.z, 0.15, n.x).multiply(k)).add(n.multiply(k * 0.3));
                }
            }
            e.setVelocity(v);
            e.velocityModified = true;
            e.fallDistance = 0;
        }
    }

    // ---------- спавн и дроп
    private void spawn(ServerWorld w, BlockPos pos, BlockPos f) {
        EntityType<?> t = Registries.ENTITY_TYPE.get(new Identifier(d.a));
        if (t.getSpawnGroup() == SpawnGroup.MONSTER && w.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!w.getBlockState(f).getCollisionShape(w, f).isEmpty()) return;
        int near = w.getEntitiesByClass(Entity.class, new Box(pos).expand(10), x -> x.getType() == t).size();
        if (near >= d.c) return;
        Entity en = t.create(w);
        if (en == null) return;
        en.refreshPositionAndAngles(f.getX() + 0.5, f.getY(), f.getZ() + 0.5, w.random.nextFloat() * 360f, 0f);
        if (en instanceof MobEntity m) m.initialize(w, w.getLocalDifficulty(f), SpawnReason.SPAWNER, null, null);
        w.spawnEntity(en);
        w.spawnParticles(ParticleTypes.POOF, f.getX() + 0.5, f.getY() + 0.5, f.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.02);
    }

    private void drop(ServerWorld w, BlockPos f, Direction dir) {
        Item it = Registries.ITEM.get(new Identifier(d.a));
        if (w.getEntitiesByClass(ItemEntity.class, new Box(f).expand(6), x -> true).size() > 48) return;
        w.spawnEntity(new ItemEntity(w, f.getX() + 0.5, f.getY() + 0.5, f.getZ() + 0.5, new ItemStack(it, d.b),
                dir.getOffsetX() * 0.12, dir.getOffsetY() * 0.12 + 0.05, dir.getOffsetZ() * 0.12));
    }

    // ---------- генератор блоков
    private void gen(ServerWorld w, BlockPos f) {
        Block b = Registries.BLOCK.get(new Identifier(d.a));
        BlockState fs = w.getBlockState(f);
        if (fs.isOf(b) || !(fs.isAir() || fs.isReplaceable())) return;
        w.setBlockState(f, b.getDefaultState(), Block.NOTIFY_ALL);
        w.playSound(null, f, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.4f, 1.2f);
    }

    // ---------- добыча
    private boolean match(BlockState s) {
        switch (d.a) {
            case "log": return s.isIn(BlockTags.LOGS);
            case "ore": {
                String p = Registries.BLOCK.getId(s.getBlock()).getPath();
                return p.endsWith("_ore") || p.equals("ancient_debris");
            }
            case "plant":
                if (s.getBlock() instanceof CropBlock cb) return cb.isMature(s);
                return s.isIn(BlockTags.FLOWERS) || s.isOf(Blocks.PUMPKIN) || s.isOf(Blocks.MELON);
            case "leaves": return s.isIn(BlockTags.LEAVES);
            case "soft": return s.isIn(BlockTags.SHOVEL_MINEABLE);
            case "stone": return s.isIn(BlockTags.BASE_STONE_OVERWORLD);
            default: return true;
        }
    }

    private boolean breakable(ServerWorld w, BlockPos p, BlockState s) {
        return !s.isAir() && !(s.getBlock() instanceof Mech) && s.getHardness(w, p) >= 0 && s.getFluidState().isEmpty();
    }

    private void brk(ServerWorld w, BlockPos pos, Direction dir, int lv) {
        if (d.c == 0) {
            for (int i = 1; i <= d.b; i++) {
                BlockPos t = pos.offset(dir, i);
                BlockState s = w.getBlockState(t);
                if (s.isAir()) continue;
                if (breakable(w, t, s) && match(s)) w.breakBlock(t, true);
                return;
            }
            return;
        }
        BlockPos c = pos.offset(dir, d.b);
        int r = d.b, left = lv;
        for (int x = -r; x <= r && left > 0; x++)
            for (int y = -r; y <= r && left > 0; y++)
                for (int z = -r; z <= r && left > 0; z++) {
                    BlockPos t = c.add(x, y, z);
                    BlockState s = w.getBlockState(t);
                    if (breakable(w, t, s) && match(s)) { w.breakBlock(t, true); left--; }
                }
    }

    // ---------- рост
    private void grow(ServerWorld w, BlockPos pos, int lv) {
        int r = d.b;
        for (int i = 0; i < lv * 3; i++) {
            BlockPos p = pos.add(w.random.nextInt(2 * r + 1) - r, w.random.nextInt(5) - 2, w.random.nextInt(2 * r + 1) - r);
            BlockState s = w.getBlockState(p);
            if (d.a.equals("bone")) {
                if (BoneMealItem.useOnFertilizable(new ItemStack(Items.BONE_MEAL), w, p))
                    w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
            } else if (s.hasRandomTicks() && (s.getBlock() instanceof Fertilizable || s.isOf(Blocks.SUGAR_CANE)
                    || s.isOf(Blocks.CACTUS) || s.isOf(Blocks.NETHER_WART))) {
                s.randomTick(w, p, w.random);
            }
        }
    }

    // ---------- погода и время
    private void env(ServerWorld w) {
        long t = w.getTimeOfDay(), m = t % 24000, base = t - m;
        switch (d.a) {
            case "clear" -> { if (w.isRaining() || w.isThundering()) w.setWeather(6000, 0, false, false); }
            case "rain" -> { if (!w.isRaining() || w.isThundering()) w.setWeather(0, 6000, true, false); }
            case "thunder" -> { if (!w.isThundering()) w.setWeather(0, 6000, true, true); }
            case "day" -> { if (m > 12000) w.setTimeOfDay(base + 24000 + 1000); }
            case "night" -> { if (m < 13000) w.setTimeOfDay(base + 13000); }
            case "noon" -> { if (Math.abs(m - 6000) > 200) w.setTimeOfDay(base + 6000); }
            default -> { if (Math.abs(m - 18000) > 200) w.setTimeOfDay(base + 18000); }
        }
    }

    // ---------- молнии и взрывы (по редстоуну)
    private void strike(ServerWorld w, BlockPos pos, Direction dir, int lv) {
        BlockPos t = pos.offset(dir, 3 + lv * 2);
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
        if (bolt == null) return;
        bolt.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(t));
        bolt.setCosmetic(d.a.equals("fx"));
        w.spawnEntity(bolt);
    }

    private void boom(ServerWorld w, BlockPos pos, Direction dir, int lv) {
        BlockPos t = pos.offset(dir, 3 + lv);
        double x = t.getX() + 0.5, y = t.getY() + 0.5, z = t.getZ() + 0.5;
        switch (d.a) {
            case "safe" -> w.createExplosion(null, x, y, z, 1.5f + lv * 0.4f, World.ExplosionSourceType.NONE);
            case "fire" -> w.createExplosion(null, x, y, z, 1.5f + lv * 0.4f, true, World.ExplosionSourceType.NONE);
            case "mega" -> w.createExplosion(null, x, y, z, 3f + lv, World.ExplosionSourceType.TNT);
            default -> w.createExplosion(null, x, y, z, 1f + lv * 0.6f, World.ExplosionSourceType.TNT);
        }
    }

    // ---------- урон
    private void dmg(ServerWorld w, BlockPos pos, int lv) {
        Box box = new Box(pos).expand(1 + lv * 0.5);
        for (Entity e : w.getEntitiesByClass(Entity.class, box, x -> ok(x, d.e))) {
            switch (d.a) {
                case "hurt" -> e.damage(w.getDamageSources().magic(), (float) d.b);
                case "fire" -> e.setOnFireFor(d.b);
                case "freeze" -> e.setFrozenTicks(e.getMinFreezeDamageTicks() + 20);
                default -> e.discard();
            }
        }
    }

    // ---------- сервис для игрока
    private static void fix(ItemStack s, int n) {
        if (!s.isEmpty() && s.isDamaged()) s.setDamage(Math.max(0, s.getDamage() - n));
    }

    private void service(ServerWorld w, BlockPos pos, int lv) {
        Box box = new Box(pos).expand(1.0).stretch(0, 2, 0);
        for (PlayerEntity p : w.getEntitiesByClass(PlayerEntity.class, box, x -> !x.isSpectator())) {
            switch (d.a) {
                case "feed" -> p.getHungerManager().add(lv, 0.6f);
                case "heal" -> p.heal(lv);
                case "xp" -> p.addExperience(lv * 2);
                case "air" -> p.setAir(p.getMaxAir());
                case "extinguish" -> p.extinguish();
                case "cleanse" -> p.clearStatusEffects();
                case "repair" -> {
                    for (ItemStack s : p.getInventory().main) fix(s, lv * 2);
                    for (ItemStack s : p.getInventory().armor) fix(s, lv * 2);
                    for (ItemStack s : p.getInventory().offHand) fix(s, lv * 2);
                }
                case "restore" -> {
                    p.heal(lv * 2);
                    p.getHungerManager().add(lv, 0.6f);
                    p.setAir(p.getMaxAir());
                    p.extinguish();
                }
                default -> {
                    if (p instanceof ServerPlayerEntity sp && !pos.up().equals(sp.getSpawnPointPosition())) {
                        sp.setSpawnPoint(w.getRegistryKey(), pos.up(), 0f, true, true);
                    }
                }
            }
        }
    }

    // ---------- телепорты
    private void tp(ServerWorld w, BlockPos pos, Direction dir, int lv) {
        Box box = new Box(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> !x.isSpectator())) {
            if (!cool(e, w.getTime(), 60)) continue;
            double x = e.getX(), y = e.getY(), z = e.getZ();
            switch (d.a) {
                case "up" -> y = w.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                case "spawn" -> {
                    BlockPos sp = e instanceof ServerPlayerEntity pl && pl.getSpawnPointPosition() != null ? pl.getSpawnPointPosition() : w.getSpawnPos();
                    x = sp.getX() + 0.5; y = sp.getY(); z = sp.getZ() + 0.5;
                }
                case "random" -> {
                    int r = lv * 16;
                    int tx = pos.getX() + w.random.nextInt(2 * r + 1) - r, tz = pos.getZ() + w.random.nextInt(2 * r + 1) - r;
                    x = tx + 0.5; z = tz + 0.5;
                    y = w.getTopY(Heightmap.Type.MOTION_BLOCKING, tx, tz);
                }
                case "far" -> {
                    Direction h = dir.getAxis().isHorizontal() ? dir : Direction.NORTH;
                    x += h.getOffsetX() * lv * 6; z += h.getOffsetZ() * lv * 6;
                    y = w.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                }
                default -> {
                    y = Math.min(w.getTopY() - 2, pos.getY() + lv * 24);
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 400, 0, true, false));
                }
            }
            e.requestTeleport(x, y, z);
            e.fallDistance = 0;
            w.playSound(null, pos, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 0.7f, 1.0f);
        }
    }

    // ---------- частицы
    private void fx(ServerWorld w, BlockPos f, int lv) {
        ParticleType<?> pt = Registries.PARTICLE_TYPE.get(new Identifier(d.a));
        if (pt instanceof SimpleParticleType sp)
            w.spawnParticles(sp, f.getX() + 0.5, f.getY() + 0.5, f.getZ() + 0.5, lv * 2, 0.3, 0.3, 0.3, 0.02);
    }

    // ---------- предметы
    private void item(ServerWorld w, BlockPos pos, BlockPos f, int lv) {
        switch (d.a) {
            case "void" -> {
                for (ItemEntity ie : w.getEntitiesByClass(ItemEntity.class, new Box(pos).expand(1 + lv), x -> true)) ie.discard();
            }
            case "collect" -> {
                Vec3d target = Vec3d.ofCenter(f);
                for (ItemEntity ie : w.getEntitiesByClass(ItemEntity.class, new Box(pos).expand(lv + 2), x -> true)) {
                    if (ie.getPos().squaredDistanceTo(target) < 1.0) continue;
                    ie.setPosition(target.x, target.y - 0.3, target.z);
                    ie.setVelocity(Vec3d.ZERO);
                }
            }
            default -> {
                for (ItemEntity ie : w.getEntitiesByClass(ItemEntity.class, new Box(pos.up()).expand(0.5 + lv * 0.2), x -> true)) {
                    ItemStack st = ie.getStack();
                    SimpleInventory inv = new SimpleInventory(st.copyWithCount(1));
                    Optional<RecipeEntry<SmeltingRecipe>> r = w.getRecipeManager().getFirstMatch(RecipeType.SMELTING, inv, w);
                    if (r.isEmpty()) continue;
                    ItemStack out = r.get().value().getResult(w.getRegistryManager()).copy();
                    out.setCount(out.getCount() * st.getCount());
                    ie.discard();
                    w.spawnEntity(new ItemEntity(w, ie.getX(), ie.getY(), ie.getZ(), out));
                    w.spawnParticles(ParticleTypes.FLAME, ie.getX(), ie.getY() + 0.2, ie.getZ(), 5, 0.1, 0.1, 0.1, 0.02);
                    break;
                }
            }
        }
    }

    // ---------- датчики
    private void sense(BlockState st, ServerWorld w, BlockPos pos, int lv) {
        Box box = new Box(pos).expand(2 + lv * 2);
        boolean v = switch (d.a) {
            case "player" -> !w.getEntitiesByClass(PlayerEntity.class, box, p -> !p.isSpectator()).isEmpty();
            case "hostile" -> !w.getEntitiesByClass(HostileEntity.class, box, p -> true).isEmpty();
            case "passive" -> !w.getEntitiesByClass(PassiveEntity.class, box, p -> true).isEmpty();
            case "mob" -> !w.getEntitiesByClass(MobEntity.class, box, p -> true).isEmpty();
            case "item" -> !w.getEntitiesByClass(ItemEntity.class, box, p -> true).isEmpty();
            case "villager" -> !w.getEntitiesByClass(VillagerEntity.class, box, p -> true).isEmpty();
            case "rain" -> w.isRaining();
            case "thunder" -> w.isThundering();
            case "day" -> w.isDay();
            case "night" -> w.isNight();
            case "burn" -> !w.getEntitiesByClass(LivingEntity.class, box, p -> p.isOnFire()).isEmpty();
            case "sneak" -> !w.getEntitiesByClass(PlayerEntity.class, box, p -> p.isSneaking()).isEmpty();
            default -> false;
        };
        chg(w, pos, st, v, st.get(PREV));
    }

    // ---------- редстоун-логика, возвращает тики до следующего вызова (-1 = не планировать)
    private void chg(ServerWorld w, BlockPos pos, BlockState st, boolean lit, boolean prev) {
        BlockState ns = st.with(LIT, lit).with(PREV, prev);
        if (!ns.equals(st)) w.setBlockState(pos, ns, Block.NOTIFY_ALL);
    }

    private int logic(BlockState st, ServerWorld w, BlockPos pos, boolean rs, int lv) {
        boolean lit = st.get(LIT), prev = st.get(PREV);
        switch (d.a) {
            case "clock" -> {
                if (rs) { chg(w, pos, st, false, prev); return 4; }
                if (lit) { chg(w, pos, st, false, prev); return Math.max(2, d.b / lv); }
                chg(w, pos, st, true, prev);
                return d.c;
            }
            case "rand" -> {
                if (rs) { chg(w, pos, st, false, prev); return 4; }
                chg(w, pos, st, w.random.nextInt(9) < lv, prev);
                return d.b;
            }
            case "toggle" -> {
                if (rs && !prev) chg(w, pos, st, !lit, true);
                else if (!rs && prev) chg(w, pos, st, lit, false);
                return 2;
            }
            case "not" -> { chg(w, pos, st, !rs, prev); return 2; }
            case "pulse" -> {
                if (lit) { chg(w, pos, st, false, prev); return 2; }
                if (rs && !prev) { chg(w, pos, st, true, true); return lv * 10; }
                if (!rs && prev) chg(w, pos, st, false, false);
                return 2;
            }
            case "daycycle" -> { chg(w, pos, st, w.isDay(), prev); return 20; }
            default -> { chg(w, pos, st, rs, prev); return -1; }
        }
    }
}
