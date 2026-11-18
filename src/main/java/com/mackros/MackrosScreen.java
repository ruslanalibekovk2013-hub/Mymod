package com.mackros;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class MackrosScreen extends Screen {
    private record Act(String name, String tip, Runnable run, boolean keep) {}

    private static final String[] CATS = {"Выделение", "Буфер", "Фигуры", "Рисование", "Схемы", "КБ оружие", "КБ блоки", "Мир", "Игрок", "Файлы",
            "Выдать", "КБ тролль", "КБ механизмы", "Настройки",
            "Зоны эффектов", "Ветер и пады", "Спавнеры", "Дропперы", "Генераторы", "Добыча, рост", "Погода, взрыв",
            "Урон, сервис", "Датчики/логика", "Телепорт/предм", "Свет и FX", "Конструктор", "NERO AI"};
    private static int cat = 0, page = 0, catScroll = 0;
    private static String query = "";
    private static final int PER = 18, VIS = 9;
    private int px, py;
    private TextFieldWidget search;
    private int resultCount = 0, resultPages = 1;

    public MackrosScreen() { super(Text.literal("Mackros")); }

    public static void openCat(int c) {
        cat = c; page = 0; query = "";
        MinecraftClient.getInstance().setScreen(new MackrosScreen());
    }

    private Text lab(String s, boolean on) {
        return on ? Text.literal(s).styled(st -> st.withColor(Config.accent())) : Text.literal(s);
    }

    private List<Act> current() {
        if (query.isEmpty()) return actions(cat);
        String q = query.toLowerCase();
        List<Act> r = new ArrayList<>();
        for (int c = 0; c < CATS.length; c++)
            for (Act a : actions(c))
                if (a.name().toLowerCase().contains(q) || a.tip().toLowerCase().contains(q)) r.add(a);
        return r;
    }

    @Override
    protected void init() {
        px = width / 2 - 186;
        py = Math.max(30, (height - 176) / 2 + 10);
        if (cat < catScroll) catScroll = cat;
        if (cat >= catScroll + VIS) catScroll = cat - VIS + 1;
        for (int i = catScroll; i < Math.min(CATS.length, catScroll + VIS); i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(lab(CATS[i], cat == i && query.isEmpty()), b -> { cat = idx; page = 0; query = ""; clearAndInit(); })
                    .dimensions(px + 4, py + (i - catScroll) * 17, 84, 15).build());
        }
        if (CATS.length > VIS) {
            addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> { catScroll = Math.max(0, catScroll - 1); clearAndInit(); })
                    .dimensions(px + 4, py + VIS * 17, 40, 14).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> { catScroll = Math.min(CATS.length - VIS, catScroll + 1); clearAndInit(); })
                    .dimensions(px + 48, py + VIS * 17, 40, 14).build());
        }

        search = new TextFieldWidget(textRenderer, px + 60, py - 22, 140, 16, Text.literal("поиск"));
        search.setMaxLength(24);
        search.setText(query);
        search.setSuggestion(query.isEmpty() ? "поиск функций" : "");
        search.setChangedListener(t -> { query = t; page = 0; clearAndInit(); });
        addDrawableChild(search);
        if (!query.isEmpty()) setInitialFocus(search);
        addDrawableChild(ButtonWidget.builder(Text.literal("Перенос"), b -> MinecraftClient.getInstance().setScreen(new MoveScreen()))
                .dimensions(px + 204, py - 22, 82, 16).tooltip(Tooltip.of(Text.literal("Меню переноса: сдвиг, клон, координаты (C)"))).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Цвет"), b -> { Config.accent = (Config.accent + 1) % Config.ACCENTS.length; Config.save(); clearAndInit(); })
                .dimensions(px + 290, py - 22, 76, 16).build());

        List<Act> acts = current();
        final int pages = Math.max(1, (acts.size() + PER - 1) / PER);
        if (page >= pages) page = pages - 1;
        int from = page * PER, to = Math.min(acts.size(), from + PER);
        for (int i = from; i < to; i++) {
            final Act a = acts.get(i);
            int k = i - from, col = k % 3, row = k / 3;
            ButtonWidget.Builder bb = ButtonWidget.builder(Text.literal(a.name()), b -> {
                a.run().run();
                if (a.keep()) clearAndInit(); else close();
            }).dimensions(px + 96 + col * 88, py + row * 22, 84, 20);
            if (!a.tip().isEmpty()) bb.tooltip(Tooltip.of(Text.literal(a.tip())));
            addDrawableChild(bb.build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); clearAndInit(); })
                .dimensions(px + 96, py + 6 * 22 + 4, 40, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(pages - 1, page + 1); clearAndInit(); })
                .dimensions(px + 96 + 224, py + 6 * 22 + 4, 40, 18).build());
        resultCount = acts.size();
        resultPages = pages;
    }

    private static void add(List<Act> l, String n, String tip, Runnable r) { l.add(new Act(n, tip, r, false)); }
    private static void keep(List<Act> l, String n, String tip, Runnable r) { l.add(new Act(n, tip, r, true)); }
    private static String onoff(boolean b) { return b ? "вкл" : "выкл"; }

    private List<Act> actions(int c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Act> l = new ArrayList<>();
        final int s = Editor.step;
        switch (c) {
            case 0 -> {
                add(l, "Точка 1", "Угол выделения (Z)", () -> Editor.setPos(mc, 1));
                add(l, "Точка 2", "Противоположный угол (X)", () -> Editor.setPos(mc, 2));
                add(l, "Сброс", "", Editor::resetSel);
                add(l, "Заполнить", "Блоком из руки (J)", () -> Editor.fill(mc, Editor.held(mc)));
                add(l, "Полый бокс", "Только стенки", () -> Editor.hollow(mc, Editor.held(mc)));
                add(l, "Рамка", "Только рёбра", () -> Editor.frame(mc));
                add(l, "Замена", "Блок в прицеле заменить блоком из руки", () -> Editor.replace(mc));
                add(l, "Очистить", "Воздухом (K)", () -> Editor.fill(mc, Blocks.AIR.getDefaultState()));
                add(l, "Стек x" + Editor.stackN, "Повторить выделение вперёд", () -> Editor.stack(mc));
                keep(l, "Стек n: " + Editor.stackN, "Сколько повторов", () -> Editor.stackN = Editor.stackN >= 20 ? 1 : Editor.stackN + 1);
                keep(l, "Шаг: " + s, "Шаг сдвигов", () -> { Editor.step = s == 1 ? 2 : s == 2 ? 5 : s == 5 ? 10 : s == 10 ? 50 : 1; Config.save(); });
                keep(l, "Симм X: " + onoff(Editor.symX), "Зеркалить правки по X", () -> Editor.toggleSymAxis(mc, true));
                keep(l, "Симм Z: " + onoff(Editor.symZ), "Зеркалить правки по Z", () -> Editor.toggleSymAxis(mc, false));
                keep(l, "Сдвиг +X", "Перенести постройку", () -> Editor.moveSel(mc, s, 0, 0));
                keep(l, "Сдвиг -X", "", () -> Editor.moveSel(mc, -s, 0, 0));
                keep(l, "Сдвиг +Y", "", () -> Editor.moveSel(mc, 0, s, 0));
                keep(l, "Сдвиг -Y", "", () -> Editor.moveSel(mc, 0, -s, 0));
                keep(l, "Сдвиг +Z", "", () -> Editor.moveSel(mc, 0, 0, s));
                keep(l, "Сдвиг -Z", "", () -> Editor.moveSel(mc, 0, 0, -s));
                keep(l, "Поворот выд.", "Повернуть постройку на месте", () -> Editor.transformSel(mc, 0));
                keep(l, "Отраж. выд. X", "", () -> Editor.transformSel(mc, 1));
                keep(l, "Отраж. выд. Z", "", () -> Editor.transformSel(mc, 2));
                keep(l, "Шире вперёд", "Расширить на шаг", () -> Editor.expand(mc, 0, s));
                keep(l, "Шире назад", "", () -> Editor.expand(mc, 3, s));
                keep(l, "Шире вверх", "", () -> Editor.expand(mc, 1, s));
                keep(l, "Шире вниз", "", () -> Editor.expand(mc, 2, s));
                keep(l, "Уже вперёд", "Сузить на шаг", () -> Editor.expand(mc, 0, -s));
                keep(l, "Уже сверху", "", () -> Editor.expand(mc, 1, -s));
                keep(l, "Обл. сохр. " + Editor.slot, "Сохранить выделение в слот (слот меняется во вкладке Файлы)", () -> Editor.saveArea(mc));
                keep(l, "Обл. загр. " + Editor.slot, "", () -> Editor.loadArea(mc));
            }
            case 1 -> {
                add(l, "Копировать", "Выделение в буфер (B)", () -> Editor.copy(mc));
                add(l, "Вырезать", "(N)", () -> Editor.cut(mc));
                add(l, "Вставить", "В прицел или в закреплённое место (V)", () -> Editor.paste(mc));
                keep(l, "Поворот 90°", "Повернуть буфер (R)", Editor::rotate);
                keep(l, "Отразить X", "(G)", () -> Editor.flip(true));
                keep(l, "Отразить Z", "", () -> Editor.flip(false));
                keep(l, "Просмотр: " + onoff(Editor.preview), "Контур вставки (P)", () -> { Editor.togglePreview(); Config.save(); });
                keep(l, "Воздух: " + (Editor.pasteAir ? "да" : "нет"), "Вставлять воздух (затирает блоки)", () -> { Editor.pasteAir = !Editor.pasteAir; Config.save(); });
                keep(l, "Сброс смещ.", "", Editor::resetNudge);
                keep(l, "Смещ. +X", "Стрелки: двигать место вставки", () -> Editor.nudge(s, 0, 0));
                keep(l, "Смещ. -X", "", () -> Editor.nudge(-s, 0, 0));
                keep(l, "Смещ. +Y", "", () -> Editor.nudge(0, s, 0));
                keep(l, "Смещ. -Y", "", () -> Editor.nudge(0, -s, 0));
                keep(l, "Смещ. +Z", "", () -> Editor.nudge(0, 0, s));
                keep(l, "Смещ. -Z", "", () -> Editor.nudge(0, 0, -s));
                keep(l, "Отмена", "(U)", () -> Editor.undo(mc));
                keep(l, "Повтор", "(I)", () -> Editor.redo(mc));
            }
            case 2 -> {
                add(l, "Сфера", "Центр в прицеле (H)", () -> Editor.sphere(mc, false));
                add(l, "Полая сфера", "", () -> Editor.sphere(mc, true));
                add(l, "Цилиндр", "", () -> Editor.cylinder(mc));
                add(l, "Купол", "Верхняя половина сферы", () -> Editor.dome(mc, false));
                add(l, "Полый купол", "", () -> Editor.dome(mc, true));
                add(l, "Пирамида", "Основание радиуса R", () -> Editor.pyramid(mc));
                add(l, "Тор", "Бублик", () -> Editor.torus(mc));
                add(l, "Башня", "Полый цилиндр", () -> Editor.tower(mc));
                add(l, "Дом", "7x5x7 из блока в руке, с дверью и окнами", () -> Editor.house(mc));
                add(l, "Мост", "Между точками 1 и 2, шириной 3", () -> Editor.bridge(mc));
                add(l, "Заливка", "Залить связную область блоков (')", () -> Editor.floodFill(mc));
                keep(l, "Радиус: " + Editor.radius, "", () -> { Editor.radius = Editor.radius >= 64 ? 1 : Editor.radius + 1; Config.save(); });
                keep(l, "Радиус -", "", () -> { Editor.radius = Math.max(1, Editor.radius - 1); Config.save(); });
                keep(l, "Радиус +5", "", () -> { Editor.radius = Math.min(64, Editor.radius + 5); Config.save(); });
                keep(l, "Высота: " + Editor.height, "Цилиндр и башня", () -> { Editor.height = Editor.height >= 100 ? 1 : Editor.height + 1; Config.save(); });
                keep(l, "Высота -", "", () -> { Editor.height = Math.max(1, Editor.height - 1); Config.save(); });
                keep(l, "Высота +10", "", () -> { Editor.height = Math.min(100, Editor.height + 10); Config.save(); });
            }
            case 3 -> {
                add(l, "Линия", "Между точками 1 и 2 (L)", () -> Editor.line(mc));
                add(l, "Эллипсоид", "Вписать в выделение", () -> Editor.ellipsoid(mc, false, false));
                add(l, "Полый элл.", "", () -> Editor.ellipsoid(mc, true, false));
                add(l, "Купол выд.", "Купол по размерам выделения", () -> Editor.ellipsoid(mc, false, true));
                add(l, "Случайно", "Рандом из блоков хотбара (O)", () -> Editor.paletteFill(mc, false));
                add(l, "Градиент", "Слои по высоте из хотбара (;)", () -> Editor.paletteFill(mc, true));
                add(l, "Замена палитрой", "Блок в прицеле -> рандом из хотбара", () -> Editor.paletteReplace(mc));
                add(l, "Сгладить", "Смягчить рельеф (M)", () -> Editor.smooth(mc, 1));
                add(l, "Сгладить x3", "", () -> Editor.smooth(mc, 3));
            }
            case 4 -> {
                keep(l, Editor.lock == null ? "Закрепить" : "Снять замок", "Зафиксировать место схемы как в Litematica (\\)", () -> Editor.lockPlacement(mc));
                keep(l, "Проверка: " + onoff(Editor.verify), "Подсвечивать ошибки постройки относительно схемы", Editor::toggleVerify);
                keep(l, "Отчёт", "Сколько блоков верно и сколько ошибок", () -> Editor.verifyReport(mc));
                keep(l, "Материалы", "Сколько блоков нужно и сколько есть", () -> mc.setScreen(new MaterialsScreen()));
                keep(l, "Слои: " + (Editor.layerMode == 0 ? "все" : Editor.layerMode == 1 ? "один" : "до N"), "Показывать и вставлять по слоям", Editor::cycleLayerMode);
                keep(l, "Слой +", "(.)", () -> Editor.layerStep(1));
                keep(l, "Слой -", "(,)", () -> Editor.layerStep(-1));
                keep(l, "Слой: " + Editor.layer, "Сбросить на 0", () -> { Editor.layer = 0; });
                add(l, "Вставить", "Вставляет только выбранные слои", () -> Editor.paste(mc));
                keep(l, "Масштаб x2", "Увеличить схему в 2 раза", () -> Editor.scaleClip(2));
                keep(l, "Масштаб x3", "", () -> Editor.scaleClip(3));
                keep(l, "Без воздуха", "Обрезать пустоту в буфере", Editor::trimAir);
                keep(l, "Очист. буфер", "", Editor::clearClip);
            }
            case 5 -> {
                add(l, "Построить ядро", "Ряд КБ вдоль +X от прицела. Нужны читы, одиночная игра", () -> Cb.buildEngine(mc));
                add(l, "Выдать всё", "Все 5 посохов", () -> Cb.giveAll(mc));
                add(l, "Удалить КБ", "Убрать последнюю постройку", () -> Cb.removeLast(mc));
                add(l, "Гранатомёт", "ПКМ: граната-ТНТ по дуге", () -> Cb.give(mc, "mkg"));
                add(l, "Портал-ган", "ПКМ: синий и оранжевый порталы по очереди", () -> Cb.give(mc, "mkp"));
                add(l, "Телепорт-посох", "ПКМ: рывок на 8 блоков", () -> Cb.give(mc, "mkb"));
                add(l, "Молния-посох", "ПКМ: удар молнии", () -> Cb.give(mc, "mkl"));
                add(l, "Заморозка", "ПКМ: замедление мобов", () -> Cb.give(mc, "mkf"));
                add(l, "Сброс снарядов", "Убрать порталы и снаряды", () -> Cb.clearShots(mc));
            }
            case 6 -> {
                add(l, "Калькулятор", "Работает через /trigger, подсказка придёт в чат", () -> Cb.buildUtil(mc, "calc", "Калькулятор"));
                add(l, "Часы", "Время суток над хотбаром", () -> Cb.buildUtil(mc, "clock", "Часы"));
                add(l, "Кубик", "/trigger mk_roll", () -> Cb.buildUtil(mc, "dice", "Кубик"));
                add(l, "Зона лечения", "Регенерация в радиусе 8", () -> Cb.buildUtil(mc, "heal", "Зона лечения"));
                add(l, "Турель", "Бьёт врагов в радиусе 12", () -> Cb.buildUtil(mc, "turret", "Турель"));
                add(l, "Авто-день", "Всегда день", () -> Cb.buildUtil(mc, "day", "Авто-день"));
                add(l, "Лифт", "Поднимает игроков рядом", () -> Cb.buildUtil(mc, "lift", "Лифт"));
                add(l, "Магнит", "Притягивает предметы", () -> Cb.buildUtil(mc, "magnet", "Магнит"));
                add(l, "Подброс", "Подбрасывает вверх", () -> Cb.buildUtil(mc, "bounce", "Подброс"));
                add(l, "Ускорение", "Скорость рядом", () -> Cb.buildUtil(mc, "speed", "Ускорение"));
                add(l, "Мина", "Взрыв под игроком, осторожно", () -> Cb.buildUtil(mc, "mine", "Мина"));
                add(l, "Табло смертей", "", () -> {
                    Editor.cmd(mc, "scoreboard objectives add mk_deaths deathCount");
                    Editor.cmd(mc, "scoreboard objectives setdisplay sidebar mk_deaths");
                });
                add(l, "Табло убийств", "", () -> {
                    Editor.cmd(mc, "scoreboard objectives add mk_kills totalKillCount");
                    Editor.cmd(mc, "scoreboard objectives setdisplay sidebar mk_kills");
                });
                add(l, "Убрать табло", "", () -> Editor.cmd(mc, "scoreboard objectives setdisplay sidebar"));
                add(l, "Удалить КБ", "Убрать последнюю постройку", () -> Cb.removeLast(mc));
                add(l, "Выдать КБ", "Командный, повторяющий, цепной", () -> Cb.giveBlocks(mc));
            }
            case 7 -> {
                add(l, "Верхний мир", "", () -> Editor.cmd(mc, "execute in minecraft:overworld run tp @s ~ ~ ~"));
                add(l, "Незер", "", () -> Editor.cmd(mc, "execute in minecraft:the_nether run tp @s ~ ~ ~"));
                add(l, "Энд", "", () -> Editor.cmd(mc, "execute in minecraft:the_end run tp @s ~ ~ ~"));
                add(l, "ТП к прицелу", "", () -> {
                    var p = Editor.look(mc, true);
                    if (p != null) Editor.cmd(mc, "tp @s " + p.getX() + " " + p.getY() + " " + p.getZ());
                });
                add(l, "Метка здесь", "Запомнить место и мир", () -> Editor.setMark(mc));
                add(l, "К метке", "Вернуться, даже из другого мира", () -> Editor.gotoMark(mc));
                add(l, "Утро", "", () -> Editor.cmd(mc, "time set 0"));
                add(l, "День", "", () -> Editor.cmd(mc, "time set day"));
                add(l, "Вечер", "", () -> Editor.cmd(mc, "time set 12500"));
                add(l, "Ночь", "", () -> Editor.cmd(mc, "time set night"));
                add(l, "Ясно", "", () -> Editor.cmd(mc, "weather clear"));
                add(l, "Дождь", "", () -> Editor.cmd(mc, "weather rain"));
                add(l, "Гроза", "", () -> Editor.cmd(mc, "weather thunder"));
                add(l, "Убрать предметы", "", () -> Editor.cmd(mc, "kill @e[type=item]"));
                add(l, "Убить мобов", "", () -> Editor.cmd(mc, "kill @e[type=!player,type=!item,type=!armor_stand,type=!marker,type=!item_frame,type=!painting]"));
                add(l, "Мир: мирно", "", () -> Editor.cmd(mc, "difficulty peaceful"));
                add(l, "Мир: легко", "", () -> Editor.cmd(mc, "difficulty easy"));
                add(l, "Мир: нормально", "", () -> Editor.cmd(mc, "difficulty normal"));
                add(l, "Мир: сложно", "", () -> Editor.cmd(mc, "difficulty hard"));
                add(l, "Мобы не ломают", "gamerule mobGriefing false", () -> Editor.cmd(mc, "gamerule mobGriefing false"));
                add(l, "Мобы ломают", "gamerule mobGriefing true", () -> Editor.cmd(mc, "gamerule mobGriefing true"));
                add(l, "Вещи сохр. вкл", "keepInventory", () -> Editor.cmd(mc, "gamerule keepInventory true"));
                add(l, "Вещи сохр. выкл", "", () -> Editor.cmd(mc, "gamerule keepInventory false"));
                add(l, "Время стоит", "doDaylightCycle false", () -> Editor.cmd(mc, "gamerule doDaylightCycle false"));
                add(l, "Время идёт", "doDaylightCycle true", () -> Editor.cmd(mc, "gamerule doDaylightCycle true"));
                add(l, "Погода стоит", "doWeatherCycle false", () -> Editor.cmd(mc, "gamerule doWeatherCycle false"));
                add(l, "Погода идёт", "doWeatherCycle true", () -> Editor.cmd(mc, "gamerule doWeatherCycle true"));
                add(l, "Огонь не горит", "doFireTick false", () -> Editor.cmd(mc, "gamerule doFireTick false"));
                add(l, "Огонь горит", "doFireTick true", () -> Editor.cmd(mc, "gamerule doFireTick true"));
                add(l, "Без спавна мобов", "doMobSpawning false", () -> Editor.cmd(mc, "gamerule doMobSpawning false"));
                add(l, "Спавн мобов", "doMobSpawning true", () -> Editor.cmd(mc, "gamerule doMobSpawning true"));
                add(l, "Спавн мира тут", "setworldspawn", () -> Editor.cmd(mc, "setworldspawn ~ ~ ~"));
                add(l, "Моя кровать тут", "spawnpoint", () -> Editor.cmd(mc, "spawnpoint @s ~ ~ ~"));
                add(l, "Граница 1000", "worldborder", () -> Editor.cmd(mc, "worldborder set 1000"));
                add(l, "Граница снять", "", () -> Editor.cmd(mc, "worldborder set 59999968"));
                add(l, "Мобы рядом -", "Убить мобов в радиусе 50", () -> Editor.cmd(mc, "kill @e[distance=..50,type=!player,type=!item,type=!armor_stand,type=!marker,type=!item_frame,type=!painting]"));
            }
            case 8 -> {
                add(l, "Творчество", "", () -> Editor.cmd(mc, "gamemode creative"));
                add(l, "Спектатор", "", () -> Editor.cmd(mc, "gamemode spectator"));
                add(l, "Выживание", "", () -> Editor.cmd(mc, "gamemode survival"));
                add(l, "Ночн. зрение", "", () -> Editor.cmd(mc, "effect give @s minecraft:night_vision infinite 0 true"));
                add(l, "Скорость", "", () -> Editor.cmd(mc, "effect give @s minecraft:speed infinite 3 true"));
                add(l, "Защита", "Сопротивление урону", () -> Editor.cmd(mc, "effect give @s minecraft:resistance infinite 4 true"));
                add(l, "Снять эффекты", "", () -> Editor.cmd(mc, "effect clear @s"));
                keep(l, "Полёт x1", "", () -> mc.player.getAbilities().setFlySpeed(0.05f));
                keep(l, "Полёт x2", "", () -> mc.player.getAbilities().setFlySpeed(0.10f));
                keep(l, "Полёт x4", "", () -> mc.player.getAbilities().setFlySpeed(0.20f));
                keep(l, "Полёт x8", "", () -> mc.player.getAbilities().setFlySpeed(0.40f));
                add(l, "Накормить", "", () -> Editor.cmd(mc, "effect give @s minecraft:saturation 1 20 true"));
                add(l, "Вылечить", "", () -> Editor.cmd(mc, "effect give @s minecraft:instant_health 1 10 true"));
                add(l, "Огнеупорность", "", () -> Editor.cmd(mc, "effect give @s minecraft:fire_resistance infinite 0 true"));
                add(l, "Дыхание воды", "", () -> Editor.cmd(mc, "effect give @s minecraft:water_breathing infinite 0 true"));
                add(l, "Супер-прыжок", "", () -> Editor.cmd(mc, "effect give @s minecraft:jump_boost infinite 4 true"));
                add(l, "Мед. падение", "", () -> Editor.cmd(mc, "effect give @s minecraft:slow_falling infinite 0 true"));
                add(l, "Свечение", "", () -> Editor.cmd(mc, "effect give @s minecraft:glowing infinite 0 true"));
                add(l, "Невидимость", "", () -> Editor.cmd(mc, "effect give @s minecraft:invisibility infinite 0 true"));
                add(l, "Сила", "", () -> Editor.cmd(mc, "effect give @s minecraft:strength infinite 3 true"));
                add(l, "Скорость копания", "", () -> Editor.cmd(mc, "effect give @s minecraft:haste infinite 4 true"));
                add(l, "Опыт +10 ур", "", () -> Editor.cmd(mc, "xp add @s 10 levels"));
                add(l, "Опыт -10 ур", "", () -> Editor.cmd(mc, "xp add @s -10 levels"));
                add(l, "Элитра", "", () -> Editor.cmd(mc, "give @s minecraft:elytra"));
                add(l, "Фейерверки", "64 шт", () -> Editor.cmd(mc, "give @s minecraft:firework_rocket 64"));
                add(l, "Еда", "64 стейка", () -> Editor.cmd(mc, "give @s minecraft:cooked_beef 64"));
                add(l, "Нез. кирка", "", () -> Editor.cmd(mc, "give @s minecraft:netherite_pickaxe"));
                add(l, "Нез. меч", "", () -> Editor.cmd(mc, "give @s minecraft:netherite_sword"));
                add(l, "Нез. топор", "", () -> Editor.cmd(mc, "give @s minecraft:netherite_axe"));
                add(l, "Нез. лопата", "", () -> Editor.cmd(mc, "give @s minecraft:netherite_shovel"));
                add(l, "Факелы", "64 шт", () -> Editor.cmd(mc, "give @s minecraft:torch 64"));
            }
            case 9 -> {
                add(l, "Сохранить", "Буфер в файл слота", Editor::save);
                add(l, "Загрузить", "", Editor::load);
                keep(l, "Слот: " + Editor.slot, "", () -> Editor.slot = Editor.slot >= 9 ? 1 : Editor.slot + 1);
                keep(l, "Слот -", "", () -> Editor.slot = Math.max(1, Editor.slot - 1));
            }
            case 10 -> {
                add(l, "+ Лифт", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:elevator"));
                add(l, "+ Калькулятор", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:calculator"));
                add(l, "+ Пульт", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:panel"));
                add(l, "+ Телепад", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:teleport_pad"));
                add(l, "+ Батут", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:trampoline"));
                add(l, "+ Ускоритель", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:booster"));
                add(l, "+ Вентилятор", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:fan"));
                add(l, "+ Магнит", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:magnet"));
                add(l, "+ Лечебная", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:heal_pad"));
                add(l, "+ Лампа", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:lamp"));
                add(l, "+ Маркер A", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:marker_a"));
                add(l, "+ Маркер B", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:marker_b"));
                add(l, "+ Жезл", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:wand"));
                add(l, "+ Вставка", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:paster"));
                add(l, "+ Кисть", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:brush"));
                add(l, "+ Ластик", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:eraser"));
                add(l, "+ Жезл меню", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:menu_wand"));
                add(l, "+ Таймер", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:timer"));
                add(l, "+ Передатчик", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:transmitter"));
                add(l, "+ Приёмник", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:receiver"));
                add(l, "+ Логика", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:logic"));
                add(l, "+ Датчик", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:sensor"));
                add(l, "+ Катапульта", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:launcher"));
                add(l, "+ Конвейер", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:conveyor"));
                add(l, "+ Модуль", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:module"));
                add(l, "+ Пульт-перекл.", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:remote"));
                add(l, "+ Пульт-импульс", "Выдать предмет (нужны читы)", () -> Editor.cmd(mc, "give @s mackros:remote_pulse"));
                add(l, "Выдать КБ", "Командный, повторяющий, цепной", () -> Cb.giveBlocks(mc));
            }
            case 11 -> {
                add(l, "Построить пульт", "Ряд КБ вдоль +X от прицела: движок пульта-тролля (одиночная игра, читы)", () -> Cb.buildRemote(mc));
                add(l, "Выдать пульт", "Пульт-тролль: ПКМ по цели в прицеле (до 4 блоков)", () -> Cb.giveRemote(mc));
                add(l, "Сброс целей", "Снять метки целей", () -> Cb.clearTargets(mc));
                add(l, "Удалить КБ", "Убрать последнюю постройку", () -> Cb.removeLast(mc));
                for (int i = 0; i < Cb.REMOTE_MODES.length; i++) {
                    final int n = i + 1;
                    add(l, n + " " + Cb.REMOTE_MODES[i], "Режим пульта " + n + " (или /trigger mkr_tm set " + n + ")", () -> Cb.remoteMode(mc, n));
                }
            }
            case 12 -> {
                add(l, "Варп-точки", "5 точек: /trigger mk_wset set N сохранить, /trigger mk_warp set N телепорт", () -> Cb.buildUtil(mc, "warp", "Варп-точки"));
                add(l, "Сигнализация", "Тревога, если рядом чужой игрок (радиус 14)", () -> Cb.buildUtil(mc, "alarm", "Сигнализация"));
                add(l, "Доверять игроку", "Дать метку mk_trust ближайшему игроку", () -> Editor.cmd(mc, "tag @p add mk_trust"));
                add(l, "Ловушка-анвил", "Встань вплотную к первому КБ ряда", () -> Cb.buildUtil(mc, "anvil", "Ловушка-анвил"));
                add(l, "Диско", "Цветные частицы", () -> Cb.buildUtil(mc, "disco", "Диско"));
                add(l, "Ночная зона", "Ночное зрение в радиусе 30", () -> Cb.buildUtil(mc, "nightzone", "Ночная зона"));
                add(l, "Зона без падения", "Плавное падение в радиусе 16", () -> Cb.buildUtil(mc, "nofall", "Зона без падения"));
                add(l, "Зона прыжков", "Супер-прыжок в радиусе 8", () -> Cb.buildUtil(mc, "jumpzone", "Зона прыжков"));
                add(l, "Мирная зона", "Убивает врагов в радиусе 20", () -> Cb.buildUtil(mc, "safezone", "Мирная зона"));
                add(l, "Удалить КБ", "Убрать последнюю постройку", () -> Cb.removeLast(mc));
                for (int i = 1; i <= 5; i++) {
                    final int n = i;
                    add(l, "Варп: сохр. " + n, "Запомнить место под ногами как точку " + n, () -> Editor.cmd(mc, "trigger mk_wset set " + n));
                }
                for (int i = 1; i <= 5; i++) {
                    final int n = i;
                    add(l, "Варп: ТП " + n, "Телепорт на точку " + n, () -> Editor.cmd(mc, "trigger mk_warp set " + n));
                }
            }
            case 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 -> {
                for (MechDefs.Def df : MechDefs.inGroup(c - 14)) {
                    final String did = df.id;
                    add(l, df.ru, df.desc + " [клик: выдать блок]", () -> MechClient.give(did));
                }
            }
            case 25 -> {
                add(l, "Конструктор", "Меню механизмов из КБ с картой выбора места (клавиша ` )", () -> MinecraftClient.getInstance().setScreen(new RigScreen()));
                for (Rig.Def rd : Rig.ALL) {
                    final String rk = rd.key();
                    add(l, rd.name(), rd.desc(), () -> MinecraftClient.getInstance().setScreen(new RigScreen(rk)));
                }
            }
            case 26 -> {
                add(l, "Открыть NERO AI", "Пиши обычным языком, что построить: дом, лифт, секретную комнату... (клавиша F9)", () -> MinecraftClient.getInstance().setScreen(new NeroScreen()));
                add(l, "Настройки NERO AI", "Подключение внешней нейросети: URL, ключ, модель, тайм-аут", () -> MinecraftClient.getInstance().setScreen(new NeroSettingsScreen()));
                add(l, "NERO: пример лифта", "Построй лифт на 10 этажей", () -> MinecraftClient.getInstance().setScreen(new NeroScreen()));
            }
            default -> {
                keep(l, "Ось: " + (Config.relMove ? "по взгляду" : "по миру"), "Направления в меню переноса", () -> { Config.relMove = !Config.relMove; Config.save(); });
                keep(l, "Автовыбор: " + onoff(Config.autoSelect), "После вставки выделять вставленную постройку", () -> { Config.autoSelect = !Config.autoSelect; Config.save(); });
                keep(l, "HUD компакт: " + onoff(Config.hudCompact), "Только строка статуса", () -> { Config.hudCompact = !Config.hudCompact; Config.save(); });
                keep(l, "Цвет меню", "Сменить акцентный цвет", () -> { Config.accent = (Config.accent + 1) % Config.ACCENTS.length; Config.save(); });
                keep(l, "Клон воздух: " + (Move.air ? "да" : "нет"), "Клон копирует и воздух", () -> { Move.air = !Move.air; Config.save(); });
                keep(l, "HUD: " + onoff(Config.showHud), "Строка статуса на экране", () -> { Config.showHud = !Config.showHud; Config.save(); });
                keep(l, "Контуры: " + onoff(Config.showOutlines), "Рамки выделения", () -> { Config.showOutlines = !Config.showOutlines; Config.save(); });
                keep(l, "Цвет: " + (Config.colorMode == 1 ? "по блоку" : "один"), "Цвета контуров схемы", () -> { Config.colorMode = 1 - Config.colorMode; Config.save(); });
                keep(l, "Лимит конт.: " + Config.previewLimit, "Сколько контуров рисовать", () -> { Config.previewLimit = Config.cycle(Config.previewLimit, 500, 1000, 2500, 5000, 10000); Config.save(); });
                keep(l, "Дальность: " + Config.reach, "Дальность прицела", () -> { Config.reach = Config.cycle(Config.reach, 32, 64, 128, 256); Config.save(); });
                keep(l, "Отмен: " + Config.undoLimit, "Глубина отмены", () -> { Config.undoLimit = Config.cycle(Config.undoLimit, 10, 25, 50, 100, 200); Config.save(); });
                keep(l, "Команд/тик: " + Config.cmdRate, "Скорость правок на серверах", () -> { Config.cmdRate = Config.cycle(Config.cmdRate, 4, 8, 12, 20, 40); Config.save(); });
                keep(l, "Объём: " + (Config.maxVolume >= 1_000_000 ? Config.maxVolume / 1_000_000 + "М" : Config.maxVolume / 1000 + "к"), "Максимум блоков за операцию",
                        () -> { Config.maxVolume = Config.maxVolume <= 100_000 ? 1_000_000 : Config.maxVolume <= 1_000_000 ? 3_000_000 : Config.maxVolume <= 3_000_000 ? 10_000_000 : 100_000; Config.save(); });
                keep(l, "Замена: " + (Config.fillMode == 0 ? "всё" : Config.fillMode == 1 ? "воздух" : "блоки"), "Что менять: всё, только воздух или только твёрдые блоки", () -> { Config.fillMode = (Config.fillMode + 1) % 3; Config.save(); });
                keep(l, "Воздух: " + (Editor.pasteAir ? "да" : "нет"), "Вставлять воздух", () -> { Editor.pasteAir = !Editor.pasteAir; Config.save(); });
                keep(l, "Просмотр: " + onoff(Editor.preview), "", () -> { Editor.preview = !Editor.preview; Config.save(); });
                keep(l, "Сброс настроек", "", Config::reset);
            }
        }
        return l;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        int acc = 0xFF000000 | Config.accent();
        int w = 372, top = py - 24, bottom = py + VIS * 17 + 22;
        ctx.fill(px - 4, top, px + w, bottom, 0xE6101820);
        ctx.fill(px - 4, top, px + w, top + 20, 0xFF123A56);
        ctx.fill(px - 4, top + 20, px + w, top + 22, acc);
        ctx.fill(px + 90, top + 26, px + 91, bottom - 4, 0xFF2A3A48);
        super.render(ctx, mx, my, d);
        ctx.drawTextWithShadow(textRenderer, "MACKROS", px, top + 6, Config.accent());
        String title = query.isEmpty() ? CATS[cat] : "Поиск: " + resultCount;
        ctx.drawCenteredTextWithShadow(textRenderer, title + "  " + (page + 1) + "/" + resultPages, px + 228, py + 6 * 22 + 9, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Editor.status(), px - 4, Math.min(bottom + 4, height - 12), 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() { return false; }
}
