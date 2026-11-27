package com.mackros;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 15 игровых «оружий-предметов»: держишь в руке и стреляешь правой кнопкой, как удочкой (модель item/handheld_rod).
 * Это вымышленные игровые устройства: урон наносится только враждебным мобам (Monster), игроки, животные и жители не страдают, блоки не разрушаются.
 */
public final class NeroWeaponItems {
    public record WI(String id, String name, int cd, String how) { }

    public static final WI[] ALL = {
            new WI("w_laser_rifle", "Лазерная винтовка", 8, "луч, бьёт первого врага (8 урона, дальность 40)"),
            new WI("w_railgun", "Рельсотрон", 50, "пробивает всех врагов на линии (20 урона, дальность 50)"),
            new WI("w_flamethrower", "Огнемёт", 6, "конус пламени, поджигает врагов впереди"),
            new WI("w_ice_cannon", "Ледяная пушка", 18, "замораживает и замедляет первого врага"),
            new WI("w_tesla_gun", "Тесла-пистолет", 20, "молния по трём ближайшим врагам"),
            new WI("w_grenade_launcher", "Гранатомёт", 35, "взрыв в точке прицела, урон по площади без разрушений"),
            new WI("w_shotgun", "Дробовик", 25, "веер из пяти зарядов"),
            new WI("w_mortar", "Миномёт", 60, "захват цели, удар через секунду"),
            new WI("w_gravity_gun", "Гравитационная пушка", 20, "притягивает и поднимает врагов"),
            new WI("w_plasma_gun", "Плазмомёт", 14, "плазменный сгусток с взрывом (12 урона)"),
            new WI("w_flak_cannon", "Зенитка", 15, "сбивает фантомов и бьёт летающих"),
            new WI("w_mine_layer", "Миноукладчик", 30, "кладёт мину: взрывается под врагом"),
            new WI("w_shield_orb", "Щит-генератор", 200, "10 секунд отталкивает врагов вокруг тебя"),
            new WI("w_orbital_beacon", "Орбитальный маяк", 160, "захват цели, через 3 секунды удар с неба (30 урона)"),
            new WI("w_sniper_rifle", "Снайперская винтовка", 40, "один сильный выстрел (30 урона, дальность 80)")};

    private record Strike(ServerWorld w, UUID owner, UUID target, int ticks, double dmg, boolean beam) { }

    private record Mine(ServerWorld w, BlockPos pos, int ttl) { }

    private record Shield(ServerWorld w, UUID owner, int ticks) { }

    private static final List<Strike> STRIKES = new ArrayList<>();
    private static final List<Mine> MINES = new ArrayList<>();
    private static final List<Shield> SHIELDS = new ArrayList<>();

    public static void init() {
        for (WI w : ALL) Registry.register(Registries.ITEM, new Identifier(MackrosMod.ID, w.id()), new WeaponItem(w, new Item.Settings().maxCount(1)));
        ServerTickEvents.END_SERVER_TICK.register(s -> tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { STRIKES.clear(); MINES.clear(); SHIELDS.clear(); });
    }

    public static Item item(String id) { return Registries.ITEM.get(new Identifier(MackrosMod.ID, id)); }

    private static boolean foe(net.minecraft.entity.Entity e) { return e instanceof Monster && e instanceof LivingEntity le && le.isAlive(); }

    private static List<LivingEntity> foesAround(ServerWorld w, Vec3d c, double r) {
        List<LivingEntity> l = new ArrayList<>();
        for (net.minecraft.entity.Entity e : w.getOtherEntities(null, new Box(c, c).expand(r), NeroWeaponItems::foe)) l.add((LivingEntity) e);
        l.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(c)));
        return l;
    }

    private static boolean solid(ServerWorld w, Vec3d p) { BlockPos bp = BlockPos.ofFloored(p); return w.getBlockState(bp).isSolidBlock(w, bp); }

    private static void hurt(ServerWorld w, LivingEntity e, double dmg, int kind) {
        var ds = w.getDamageSources();
        e.damage(kind == 1 ? ds.explosion(null, null) : kind == 2 ? ds.lightningBolt() : kind == 3 ? ds.onFire() : kind == 4 ? ds.freeze() : ds.magic(), (float) dmg);
    }

    private static void snd(ServerWorld w, Vec3d p, SoundEvent s, float v, float pitch) { w.playSound(null, p.x, p.y, p.z, s, SoundCategory.PLAYERS, v, pitch); }

    private static void part(ServerWorld w, ParticleEffect fx, Vec3d p, int n, double d, double sp) { w.spawnParticles(fx, p.x, p.y, p.z, n, d, d, d, sp); }

    /** Луч: частицы по пути, возвращает первых попавшихся врагов (pierce = все). */
    private static List<LivingEntity> ray(ServerWorld w, Vec3d from, Vec3d dir, double range, ParticleEffect fx, boolean pierce) {
        List<LivingEntity> hit = new ArrayList<>();
        for (double d = 1.0; d <= range; d += 0.6) {
            Vec3d p = from.add(dir.multiply(d));
            if (solid(w, p)) break;
            part(w, fx, p, 1, 0.0, 0.0);
            for (LivingEntity e : foesAround(w, p, 0.8)) if (!hit.contains(e)) { hit.add(e); if (!pierce) return hit; }
        }
        return hit;
    }

    private static Vec3d aimPoint(ServerWorld w, Vec3d from, Vec3d dir, double range) {
        Vec3d last = from;
        for (double d = 1.0; d <= range; d += 0.5) { Vec3d p = from.add(dir.multiply(d)); if (solid(w, p)) return last; last = p; if (!foesAround(w, p, 1.0).isEmpty()) return p; }
        return last;
    }

    public static class WeaponItem extends Item {
        private final WI wi;

        WeaponItem(WI wi, Settings s) { super(s); this.wi = wi; }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity pl, Hand hand) {
            ItemStack st = pl.getStackInHand(hand);
            if (world.isClient || !(world instanceof ServerWorld w)) return TypedActionResult.success(st, world.isClient());
            if (pl.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.pass(st);
            Vec3d eye = pl.getEyePos(), dir = pl.getRotationVec(1f);
            String msg = fire(w, pl, eye, dir);
            pl.getItemCooldownManager().set(this, wi.cd());
            if (msg != null) pl.sendMessage(Text.literal(wi.name() + ": " + msg), true);
            return TypedActionResult.success(st, false);
        }

        private String fire(ServerWorld w, PlayerEntity pl, Vec3d eye, Vec3d dir) {
            switch (wi.id()) {
                case "w_laser_rifle" -> {
                    snd(w, eye, SoundEvents.ENTITY_GUARDIAN_ATTACK, 1.0f, 1.8f);
                    List<LivingEntity> h = ray(w, eye, dir, 40, ParticleTypes.END_ROD, false);
                    for (LivingEntity e : h) hurt(w, e, 8, 0);
                    return h.isEmpty() ? "мимо" : "попадание";
                }
                case "w_railgun" -> {
                    snd(w, eye, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 2.0f);
                    List<LivingEntity> h = ray(w, eye, dir, 50, ParticleTypes.ELECTRIC_SPARK, true);
                    for (LivingEntity e : h) hurt(w, e, 20, 0);
                    return "поражено: " + h.size();
                }
                case "w_flamethrower" -> {
                    snd(w, eye, SoundEvents.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
                    int n = 0;
                    for (double d = 1; d <= 8; d += 0.7) part(w, ParticleTypes.FLAME, eye.add(dir.multiply(d)), 6, 0.25 + d * 0.04, 0.02);
                    for (LivingEntity e : foesAround(w, eye.add(dir.multiply(4)), 5)) {
                        Vec3d to = e.getPos().add(0, e.getHeight() / 2, 0).subtract(eye);
                        if (to.length() < 8.5 && to.normalize().dotProduct(dir) > 0.78) { e.setOnFireFor(5); hurt(w, e, 3, 3); n++; }
                    }
                    return n > 0 ? "горят: " + n : null;
                }
                case "w_ice_cannon" -> {
                    snd(w, eye, SoundEvents.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
                    List<LivingEntity> h = ray(w, eye, dir, 30, ParticleTypes.SNOWFLAKE, false);
                    for (LivingEntity e : h) { e.setFrozenTicks(300); e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 4)); e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1)); hurt(w, e, 3, 4); }
                    return h.isEmpty() ? "мимо" : "заморожен";
                }
                case "w_tesla_gun" -> {
                    snd(w, eye, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.8f);
                    List<LivingEntity> t = foesAround(w, pl.getPos(), 12);
                    int n = 0;
                    for (LivingEntity e : t) {
                        if (n++ >= 3) break;
                        Vec3d to = e.getPos().add(0, 1, 0);
                        for (int i = 0; i <= 10; i++) part(w, ParticleTypes.ELECTRIC_SPARK, eye.add(to.subtract(eye).multiply(i / 10.0)), 1, 0.15, 0.1);
                        hurt(w, e, 7, 2);
                        e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2));
                    }
                    return n == 0 ? "целей нет" : "ударов: " + Math.min(n, 3);
                }
                case "w_grenade_launcher" -> {
                    Vec3d p = aimPoint(w, eye, dir, 28);
                    for (double d = 1; d < p.subtract(eye).length(); d += 0.8) part(w, ParticleTypes.SMOKE, eye.add(dir.multiply(d)), 1, 0, 0);
                    part(w, ParticleTypes.EXPLOSION_EMITTER, p, 1, 0, 0);
                    snd(w, p, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5f, 1.1f);
                    for (LivingEntity e : foesAround(w, p, 4)) hurt(w, e, 12, 1);
                    return null;
                }
                case "w_shotgun" -> {
                    snd(w, eye, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.8f, 1.8f);
                    int hits = 0;
                    for (int i = -2; i <= 2; i++) {
                        Vec3d d2 = dir.add(pl.getRotationVec(1f).crossProduct(new Vec3d(0, 1, 0)).normalize().multiply(i * 0.07)).normalize();
                        for (LivingEntity e : ray(w, eye, d2, 14, ParticleTypes.CRIT, false)) { hurt(w, e, 6, 0); hits++; }
                    }
                    return hits > 0 ? "попаданий: " + hits : null;
                }
                case "w_mortar", "w_orbital_beacon" -> {
                    boolean orb = wi.id().equals("w_orbital_beacon");
                    List<LivingEntity> t = ray(w, eye, dir, orb ? 40 : 30, ParticleTypes.END_ROD, false);
                    if (t.isEmpty()) return "цель не захвачена";
                    STRIKES.add(new Strike(w, pl.getUuid(), t.get(0).getUuid(), orb ? 60 : 20, orb ? 30 : 15, orb));
                    snd(w, eye, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.2f, 0.6f);
                    return "цель захвачена";
                }
                case "w_gravity_gun" -> {
                    snd(w, eye, SoundEvents.BLOCK_BEACON_AMBIENT, 1.2f, 0.5f);
                    int n = 0;
                    for (LivingEntity e : foesAround(w, pl.getPos(), 14)) {
                        Vec3d to = pl.getPos().subtract(e.getPos());
                        if (to.normalize().dotProduct(dir) > -0.2 || to.length() < 14) { e.addVelocity(to.normalize().multiply(0.7).add(0, 0.35, 0)); e.velocityModified = true; e.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 20, 0)); hurt(w, e, 1, 0); n++; }
                        part(w, ParticleTypes.REVERSE_PORTAL, e.getPos().add(0, 1, 0), 8, 0.3, 0.05);
                    }
                    return n > 0 ? "захвачено: " + n : null;
                }
                case "w_plasma_gun" -> {
                    snd(w, eye, SoundEvents.ENTITY_BLAZE_SHOOT, 1.2f, 0.6f);
                    List<LivingEntity> h = ray(w, eye, dir, 28, ParticleTypes.SOUL_FIRE_FLAME, false);
                    for (LivingEntity e : h) { hurt(w, e, 12, 0); part(w, ParticleTypes.EXPLOSION, e.getPos().add(0, 1, 0), 1, 0, 0); }
                    return h.isEmpty() ? "мимо" : "попадание";
                }
                case "w_flak_cannon" -> {
                    snd(w, eye, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 0.8f);
                    int n = 0;
                    for (LivingEntity e : foesAround(w, eye.add(dir.multiply(12)), 16)) {
                        Vec3d to = e.getPos().subtract(eye);
                        if (to.length() < 26 && to.normalize().dotProduct(dir) > 0.75) { hurt(w, e, e instanceof PhantomEntity ? 10 : 4, 0); part(w, ParticleTypes.EXPLOSION, e.getPos().add(0, 1, 0), 1, 0, 0); n++; }
                    }
                    for (double d = 2; d < 20; d += 2.5) part(w, ParticleTypes.SMOKE, eye.add(dir.multiply(d)), 2, 0.05, 0);
                    return n > 0 ? "сбито: " + n : null;
                }
                case "w_mine_layer" -> {
                    BlockPos pos = BlockPos.ofFloored(aimPoint(w, eye, dir, 6));
                    while (w.getBlockState(pos).isAir() && pos.getY() > w.getBottomY()) pos = pos.down();
                    BlockPos mp = pos.up();
                    if (MINES.size() >= 20) MINES.remove(0);
                    if (w.getBlockState(mp).isAir()) { w.setBlockState(mp, net.minecraft.block.Blocks.STONE_PRESSURE_PLATE.getDefaultState()); MINES.add(new Mine(w, mp, 12000)); snd(w, Vec3d.ofCenter(mp), SoundEvents.BLOCK_STONE_PLACE, 1f, 1f); return "мина положена"; }
                    return "здесь не положить";
                }
                case "w_shield_orb" -> {
                    SHIELDS.removeIf(s -> s.owner().equals(pl.getUuid()));
                    SHIELDS.add(new Shield(w, pl.getUuid(), 200));
                    snd(w, pl.getPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, 1f, 1.4f);
                    return "щит включён на 10 секунд";
                }
                default -> {
                    snd(w, eye, SoundEvents.ENTITY_ARROW_HIT_PLAYER, 1.5f, 0.5f);
                    List<LivingEntity> h = ray(w, eye, dir, 80, ParticleTypes.CRIT, false);
                    for (LivingEntity e : h) hurt(w, e, 30, 0);
                    return h.isEmpty() ? "мимо" : "попадание";
                }
            }
        }
    }

    private static void tick() {
        for (Iterator<Strike> it = STRIKES.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            net.minecraft.entity.Entity t = s.w().getEntity(s.target());
            if (!(t instanceof LivingEntity le) || !le.isAlive()) { it.remove(); continue; }
            int left = s.ticks() - 1;
            if (s.beam()) for (int i = 0; i < 12; i++) part(s.w(), ParticleTypes.END_ROD, le.getPos().add(0, 2 + i * 2, 0), 1, 0.05, 0);
            else part(s.w(), ParticleTypes.SMOKE, le.getPos().add(0, 3, 0), 3, 0.2, 0.01);
            if (left <= 0) {
                part(s.w(), ParticleTypes.EXPLOSION_EMITTER, le.getPos().add(0, 1, 0), 1, 0, 0);
                snd(s.w(), le.getPos(), s.beam() ? SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER : SoundEvents.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
                for (LivingEntity e : foesAround(s.w(), le.getPos(), s.beam() ? 4 : 3)) hurt(s.w(), e, s.dmg(), s.beam() ? 0 : 1);
                it.remove();
            } else { STRIKES.set(STRIKES.indexOf(s), new Strike(s.w(), s.owner(), s.target(), left, s.dmg(), s.beam())); }
        }
        for (Iterator<Mine> it = MINES.iterator(); it.hasNext(); ) {
            Mine m = it.next();
            if (!m.w().getBlockState(m.pos()).isOf(net.minecraft.block.Blocks.STONE_PRESSURE_PLATE)) { it.remove(); continue; }
            Vec3d c = Vec3d.ofCenter(m.pos());
            List<LivingEntity> near = foesAround(m.w(), c, 1.6);
            if (!near.isEmpty()) {
                part(m.w(), ParticleTypes.EXPLOSION_EMITTER, c, 1, 0, 0);
                snd(m.w(), c, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);
                for (LivingEntity e : foesAround(m.w(), c, 3.5)) hurt(m.w(), e, 14, 1);
                m.w().setBlockState(m.pos(), net.minecraft.block.Blocks.AIR.getDefaultState());
                it.remove();
            }
        }
        for (Iterator<Shield> it = SHIELDS.iterator(); it.hasNext(); ) {
            Shield s = it.next();
            net.minecraft.entity.Entity o = s.w().getEntity(s.owner());
            int left = s.ticks() - 1;
            if (o == null || left <= 0) { it.remove(); continue; }
            for (int i = 0; i < 8; i++) { double a = Math.PI * 2 * i / 8 + left * 0.1; part(s.w(), ParticleTypes.END_ROD, o.getPos().add(Math.cos(a) * 5, 1, Math.sin(a) * 5), 1, 0.02, 0); }
            for (LivingEntity e : foesAround(s.w(), o.getPos(), 6)) { Vec3d away = e.getPos().subtract(o.getPos()).normalize().multiply(0.6); e.addVelocity(away.x, 0.1, away.z); e.velocityModified = true; hurt(s.w(), e, 1, 0); }
            SHIELDS.set(SHIELDS.indexOf(s), new Shield(s.w(), s.owner(), left));
        }
    }

    private NeroWeaponItems() { }
}
