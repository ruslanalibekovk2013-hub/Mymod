package com.mackros;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/** Генератор рабочих конструкций из командных блоков (только одиночная игра). */
public final class Cb {
    private record Spec(Block b, String cmd) {}

    private static final List<BlockPos> last = new ArrayList<>();

    private static final String[] SETUP = {
            "gamerule commandBlockOutput false",
            "scoreboard objectives add mk_use minecraft.used:minecraft.carrot_on_a_stick",
            "scoreboard objectives add mk_age dummy",
            "scoreboard objectives add mk_cdt dummy",
            "scoreboard objectives add mk_t dummy",
            "scoreboard players set #mk mk_t 0"
    };

    // ---------- helpers ----------
    private static String q(String s) { return s.replace('\'', '"'); }

    private static String sel(String key) {
        return "@a[scores={mk_use=1..},nbt={SelectedItem:{id:'minecraft:carrot_on_a_stick',tag:{" + key + ":1b}}}]";
    }

    private static Spec rep(String c) { return new Spec(Blocks.REPEATING_COMMAND_BLOCK, q(c)); }

    private static void add(List<Spec> l, String c) { l.add(new Spec(Blocks.CHAIN_COMMAND_BLOCK, q(c))); }

    // ---------- оружейное ядро ----------
    private static List<Spec> engine() {
        List<Spec> l = new ArrayList<>();
        String[][] w = {{"mkg", "mk_gren"}, {"mkp", "mk_pshot"}, {"mkl", "mk_lit"}, {"mkf", "mk_frz"}};
        for (int i = 0; i < w.length; i++) {
            String c = "execute as " + sel(w[i][0]) + " at @s anchored eyes run summon marker ^ ^ ^1 {Tags:['mk_pr','mk_new','" + w[i][1] + "']}";
            if (i == 0) l.add(rep(c)); else add(l, c);
        }
        // телепорт-посох
        String b = sel("mkb");
        add(l, "execute as " + b + " at @s run particle minecraft:portal ~ ~1 ~ 0.3 0.5 0.3 0.5 40 force");
        add(l, "execute as " + b + " at @s if block ^ ^ ^8 air if block ^ ^1 ^8 air run tp @s ^ ^ ^8");
        add(l, "execute as " + b + " at @s run playsound minecraft:entity.enderman.teleport master @a ~ ~ ~ 1 1");
        // направление снарядов и цвет портала
        add(l, "execute as @e[tag=mk_new] at @s rotated as @p run tp @s ~ ~ ~ ~ ~");
        add(l, "execute as @e[tag=mk_new,tag=mk_pshot] if score #mk mk_t matches 0 run tag @s add mk_blue");
        add(l, "execute as @e[tag=mk_new,tag=mk_pshot] if score #mk mk_t matches 1 run tag @s add mk_orange");
        add(l, "execute if entity @e[tag=mk_new,tag=mk_blue] run scoreboard players set #mk mk_t 1");
        add(l, "execute if entity @e[tag=mk_new,tag=mk_orange] run scoreboard players set #mk mk_t 0");
        add(l, "tag @e[tag=mk_new] remove mk_new");
        add(l, "scoreboard players set @a[scores={mk_use=1..}] mk_use 0");
        // столкновения
        add(l, "execute as @e[tag=mk_pr] at @s unless block ^ ^ ^1 air run tag @s add mk_land");
        add(l, "execute as @e[tag=mk_pr,tag=!mk_pshot] at @s if entity @e[distance=..1.5,type=!marker,type=!item,type=!player,type=!tnt] run tag @s add mk_land");
        // эффекты попадания
        add(l, "execute as @e[tag=mk_land,tag=mk_gren] at @s run summon minecraft:tnt ~ ~ ~ {Fuse:1s}");
        add(l, "execute as @e[tag=mk_land,tag=mk_gren] at @s run summon minecraft:tnt ~ ~0.5 ~ {Fuse:1s}");
        add(l, "execute as @e[tag=mk_land,tag=mk_lit] at @s run summon minecraft:lightning_bolt ~ ~ ~");
        add(l, "execute as @e[tag=mk_land,tag=mk_frz] at @s run effect give @e[distance=..5,type=!player,type=!marker,type=!item] minecraft:slowness 10 5 true");
        add(l, "execute as @e[tag=mk_land,tag=mk_frz] at @s run particle minecraft:snowflake ~ ~ ~ 1 1 1 0.05 120 force");
        add(l, "execute as @e[tag=mk_land,tag=mk_frz] at @s run playsound minecraft:block.glass.break master @a ~ ~ ~ 1 1.2");
        // порталы
        add(l, "execute if entity @e[tag=mk_land,tag=mk_blue] run kill @e[tag=mk_pa]");
        add(l, "execute as @e[tag=mk_land,tag=mk_blue] at @s run summon marker ~ ~ ~ {Tags:['mk_pa']}");
        add(l, "execute if entity @e[tag=mk_land,tag=mk_orange] run kill @e[tag=mk_pb]");
        add(l, "execute as @e[tag=mk_land,tag=mk_orange] at @s run summon marker ~ ~ ~ {Tags:['mk_pb']}");
        add(l, "kill @e[tag=mk_land]");
        // полёт
        add(l, "execute as @e[tag=mk_gren] at @s run tp @s ^ ^ ^1 ~ ~2");
        add(l, "execute as @e[tag=mk_pr,tag=!mk_gren] at @s run tp @s ^ ^ ^1");
        // частицы снарядов
        add(l, "execute as @e[tag=mk_gren] at @s run particle minecraft:flame ~ ~ ~ 0 0 0 0.01 2 force");
        add(l, "execute as @e[tag=mk_pshot,tag=mk_blue] at @s run particle minecraft:dust 0.2 0.5 1 1 ~ ~ ~ 0 0 0 0 4 force");
        add(l, "execute as @e[tag=mk_pshot,tag=mk_orange] at @s run particle minecraft:dust 1 0.5 0.1 1 ~ ~ ~ 0 0 0 0 4 force");
        add(l, "execute as @e[tag=mk_lit] at @s run particle minecraft:electric_spark ~ ~ ~ 0.1 0.1 0.1 0.1 3 force");
        add(l, "execute as @e[tag=mk_frz] at @s run particle minecraft:snowflake ~ ~ ~ 0.1 0.1 0.1 0.02 3 force");
        // время жизни
        add(l, "scoreboard players add @e[tag=mk_pr] mk_age 1");
        add(l, "kill @e[tag=mk_pr,scores={mk_age=80..}]");
        // кольца порталов
        add(l, "execute at @e[tag=mk_pa] run particle minecraft:dust 0.2 0.5 1 1.5 ~ ~0.9 ~ 0.3 0.8 0.3 0 10 force");
        add(l, "execute at @e[tag=mk_pb] run particle minecraft:dust 1 0.5 0.1 1.5 ~ ~0.9 ~ 0.3 0.8 0.3 0 10 force");
        // телепортация через порталы
        add(l, "scoreboard players add @a mk_cdt 0");
        add(l, "scoreboard players remove @a[scores={mk_cdt=1..}] mk_cdt 1");
        add(l, "execute as @e[tag=mk_pa] at @s if entity @e[tag=mk_pb] run tag @a[distance=..1.6,scores={mk_cdt=0}] add mk_ab");
        add(l, "execute as @e[tag=mk_pb] at @s if entity @e[tag=mk_pa] run tag @a[distance=..1.6,scores={mk_cdt=0}] add mk_ba");
        add(l, "execute at @e[tag=mk_pb,limit=1] run tp @a[tag=mk_ab] ~ ~ ~");
        add(l, "execute at @e[tag=mk_pa,limit=1] run tp @a[tag=mk_ba] ~ ~ ~");
        add(l, "execute as @a[tag=mk_ab] at @s run playsound minecraft:entity.enderman.teleport master @a ~ ~ ~ 1 1.3");
        add(l, "execute as @a[tag=mk_ba] at @s run playsound minecraft:entity.enderman.teleport master @a ~ ~ ~ 1 1.3");
        add(l, "scoreboard players set @a[tag=mk_ab] mk_cdt 30");
        add(l, "scoreboard players set @a[tag=mk_ba] mk_cdt 30");
        add(l, "tag @a remove mk_ab");
        add(l, "tag @a remove mk_ba");
        return l;
    }

    // ---------- простые КБ-блоки ----------
    private static List<Spec> util(String kind) {
        List<Spec> l = new ArrayList<>();
        switch (kind) {
            case "heal" -> {
                l.add(rep("effect give @a[distance=..8] minecraft:regeneration 3 1 true"));
                add(l, "particle minecraft:heart ~ ~1.5 ~ 3 0.3 3 0 1");
            }
            case "turret" -> {
                String[] mobs = {"zombie", "husk", "drowned", "skeleton", "stray", "creeper", "spider"};
                for (int i = 0; i < mobs.length; i++) {
                    String c = "execute as @e[type=minecraft:" + mobs[i] + ",distance=..12] at @s run damage @s 5 minecraft:magic";
                    if (i == 0) l.add(rep(c)); else add(l, c);
                }
            }
            case "day" -> l.add(rep("time set day"));
            case "lift" -> {
                l.add(rep("effect give @a[distance=..2] minecraft:levitation 1 6 true"));
                add(l, "particle minecraft:end_rod ~ ~1 ~ 0.3 1.5 0.3 0.02 4");
            }
            case "magnet" -> l.add(rep("execute as @e[type=minecraft:item,distance=..12] at @s run tp @s @p"));
            case "bounce" -> {
                l.add(rep("effect give @a[distance=..1.6] minecraft:levitation 1 5 true"));
                add(l, "particle minecraft:cloud ~ ~0.3 ~ 0.4 0.1 0.4 0.02 3");
            }
            case "speed" -> {
                l.add(rep("effect give @a[distance=..2] minecraft:speed 2 6 true"));
                add(l, "particle minecraft:flame ~ ~0.3 ~ 0.4 0.1 0.4 0.02 3");
            }
            case "mine" -> l.add(rep("execute if entity @a[distance=..1.2] unless entity @e[type=minecraft:tnt,distance=..4] run summon minecraft:tnt ~ ~ ~ {Fuse:30s}"));
            case "dice" -> {
                l.add(rep("scoreboard players enable @a mk_roll"));
                add(l, "execute as @a[scores={mk_roll=1..}] run random roll 1..6");
                add(l, "scoreboard players set @a[scores={mk_roll=1..}] mk_roll 0");
            }
            case "clock" -> {
                l.add(rep("execute store result score #t clk_t run time query daytime"));
                add(l, "scoreboard players operation #h clk_t = #t clk_t");
                add(l, "scoreboard players operation #h clk_t /= #1000 clk_t");
                add(l, "scoreboard players operation #h clk_t += #6 clk_t");
                add(l, "scoreboard players operation #h clk_t %= #24 clk_t");
                add(l, "scoreboard players operation #m clk_t = #t clk_t");
                add(l, "scoreboard players operation #m clk_t %= #1000 clk_t");
                add(l, "scoreboard players operation #m clk_t *= #60 clk_t");
                add(l, "scoreboard players operation #m clk_t /= #1000 clk_t");
                add(l, "execute if score #m clk_t matches 0..9 run title @a actionbar [{'text':'Время ','color':'gold'},{'score':{'name':'#h','objective':'clk_t'},'color':'yellow'},{'text':':0','color':'yellow'},{'score':{'name':'#m','objective':'clk_t'},'color':'yellow'}]");
                add(l, "execute if score #m clk_t matches 10.. run title @a actionbar [{'text':'Время ','color':'gold'},{'score':{'name':'#h','objective':'clk_t'},'color':'yellow'},{'text':':','color':'yellow'},{'score':{'name':'#m','objective':'clk_t'},'color':'yellow'}]");
            }
            case "calc" -> {
                l.add(rep("scoreboard players enable @a calc_a"));
                add(l, "scoreboard players enable @a calc_b");
                add(l, "scoreboard players enable @a calc_op");
                add(l, "scoreboard players enable @a calc_go");
                add(l, "execute as @a[scores={calc_go=1..}] run scoreboard players operation @s calc_r = @s calc_a");
                add(l, "execute as @a[scores={calc_go=1..,calc_op=1}] run scoreboard players operation @s calc_r += @s calc_b");
                add(l, "execute as @a[scores={calc_go=1..,calc_op=2}] run scoreboard players operation @s calc_r -= @s calc_b");
                add(l, "execute as @a[scores={calc_go=1..,calc_op=3}] run scoreboard players operation @s calc_r *= @s calc_b");
                add(l, "execute as @a[scores={calc_go=1..,calc_op=4}] run scoreboard players operation @s calc_r /= @s calc_b");
                add(l, "execute as @a[scores={calc_go=1..,calc_op=5}] run scoreboard players operation @s calc_r %= @s calc_b");
                add(l, "execute as @a[scores={calc_go=1..}] run tellraw @s [{'text':'[Калькулятор] ','color':'aqua'},{'score':{'name':'@s','objective':'calc_a'}},{'text':' (оп ','color':'gray'},{'score':{'name':'@s','objective':'calc_op'}},{'text':') ','color':'gray'},{'score':{'name':'@s','objective':'calc_b'}},{'text':' = ','color':'yellow'},{'score':{'name':'@s','objective':'calc_r'},'color':'green','bold':true}]");
                add(l, "scoreboard players set @a[scores={calc_go=1..}] calc_go 0");
            }
            default -> { }
        }
        return l;
    }

    private static String[] setupFor(String kind) {
        return switch (kind) {
            case "calc" -> new String[]{"scoreboard objectives add calc_a trigger", "scoreboard objectives add calc_b trigger",
                    "scoreboard objectives add calc_op trigger", "scoreboard objectives add calc_go trigger", "scoreboard objectives add calc_r dummy"};
            case "clock" -> new String[]{"scoreboard objectives add clk_t dummy", "scoreboard players set #1000 clk_t 1000",
                    "scoreboard players set #60 clk_t 60", "scoreboard players set #24 clk_t 24", "scoreboard players set #6 clk_t 6"};
            case "dice" -> new String[]{"scoreboard objectives add mk_roll trigger"};
            default -> new String[0];
        };
    }

    private static String helpFor(String kind) {
        return switch (kind) {
            case "calc" -> "Калькулятор: /trigger calc_a set 12, /trigger calc_op set 1 (1 плюс, 2 минус, 3 умножить, 4 делить, 5 остаток), /trigger calc_b set 5, затем /trigger calc_go";
            case "dice" -> "Кубик: команда /trigger mk_roll";
            case "clock" -> "Часы показывают время в панели действий над хотбаром";
            case "mine" -> "Мина: взрывается под игроком, строй осторожно";
            default -> "";
        };
    }

    // ---------- публичное API ----------
    public static void buildEngine(MinecraftClient mc) {
        for (String s : SETUP) Editor.cmd(mc, s);
        build(mc, engine(), "Оружейное ядро");
    }

    public static void buildUtil(MinecraftClient mc, String kind, String title) {
        for (String st : setupFor(kind)) Editor.cmd(mc, st);
        build(mc, util(kind), title);
        String h = helpFor(kind);
        if (!h.isEmpty()) Editor.say(h);
    }

    private static void build(MinecraftClient mc, List<Spec> specs, String title) {
        IntegratedServer srv = mc.getServer();
        if (srv == null) { Editor.msg("КБ-конструкции работают только в одиночной игре"); return; }
        BlockPos base = Editor.look(mc, true);
        if (base == null) { Editor.msg("Наведись на землю, где строить"); return; }
        ServerWorld w = srv.getWorld(mc.world.getRegistryKey());
        if (w == null) return;
        last.clear();
        for (int i = 0; i < specs.size(); i++) last.add(base.add(i, 0, 0));
        final List<BlockPos> ps = new ArrayList<>(last);
        srv.execute(() -> {
            for (int i = 0; i < specs.size(); i++) {
                Spec sp = specs.get(i);
                BlockPos p = ps.get(i);
                w.setBlockState(p, sp.b().getDefaultState().with(CommandBlock.FACING, Direction.EAST), 3);
                if (w.getBlockEntity(p) instanceof CommandBlockBlockEntity be) {
                    be.getCommandExecutor().setCommand(sp.cmd());
                    be.setAuto(true);
                    be.markDirty();
                }
            }
        });
        Editor.msg(title + ": построено " + specs.size() + " КБ (ряд вдоль +X)");
    }

    public static void removeLast(MinecraftClient mc) {
        IntegratedServer srv = mc.getServer();
        if (srv == null || last.isEmpty()) { Editor.msg("Нечего удалять"); return; }
        ServerWorld w = srv.getWorld(mc.world.getRegistryKey());
        if (w == null) return;
        final List<BlockPos> ps = new ArrayList<>(last);
        srv.execute(() -> { for (BlockPos p : ps) w.setBlockState(p, Blocks.AIR.getDefaultState(), 3); });
        last.clear();
        Editor.msg("Последняя КБ-конструкция удалена");
    }

    private static void giveWeapon(MinecraftClient mc, String key, String title, String color, int model) {
        Editor.cmd(mc, "give @s minecraft:carrot_on_a_stick{display:{Name:'{\"text\":\"" + title + "\",\"color\":\"" + color
                + "\",\"italic\":false}'}," + key + ":1b,Unbreakable:1b,CustomModelData:" + model + "} 1");
    }

    public static void give(MinecraftClient mc, String key) {
        switch (key) {
            case "mkg" -> giveWeapon(mc, "mkg", "Гранатомёт", "red", 101);
            case "mkp" -> giveWeapon(mc, "mkp", "Портал-ган", "aqua", 102);
            case "mkb" -> giveWeapon(mc, "mkb", "Телепорт-посох", "light_purple", 103);
            case "mkl" -> giveWeapon(mc, "mkl", "Молния-посох", "yellow", 104);
            case "mkf" -> giveWeapon(mc, "mkf", "Заморозка-посох", "blue", 105);
            default -> { }
        }
    }

    public static void giveAll(MinecraftClient mc) {
        for (String k : new String[]{"mkg", "mkp", "mkb", "mkl", "mkf"}) give(mc, k);
    }

    public static void giveBlocks(MinecraftClient mc) {
        Editor.cmd(mc, "give @s minecraft:command_block 1");
        Editor.cmd(mc, "give @s minecraft:repeating_command_block 1");
        Editor.cmd(mc, "give @s minecraft:chain_command_block 1");
    }

    public static void clearShots(MinecraftClient mc) {
        Editor.cmd(mc, "kill @e[tag=mk_pr]");
        Editor.cmd(mc, "kill @e[tag=mk_pa]");
        Editor.cmd(mc, "kill @e[tag=mk_pb]");
        Editor.cmd(mc, "scoreboard players set #mk mk_t 0");
        Editor.msg("Снаряды и порталы убраны");
    }
}
