package com.mackros;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class MackrosScreen extends Screen {
    private record Act(String name, String tip, Runnable run, boolean keep) {}

    private static final String[] CATS = {"Выделение", "Буфер", "Фигуры", "Рисование", "Схемы", "КБ оружие", "КБ блоки", "Мир", "Игрок", "Файлы", "Настройки"};
    private static int cat = 0, page = 0;
    private static final int PER = 18;
    private int px, py;

    public MackrosScreen() { super(Text.literal("Mackros")); }

    @Override
    protected void init() {
        px = width / 2 - 186;
        py = Math.max(30, (height - 176) / 2 + 10);
        for (int i = 0; i < CATS.length; i++) {
            final int idx = i;
            Text t = Text.literal(CATS[i]).formatted(cat == i ? Formatting.AQUA : Formatting.WHITE);
            addDrawableChild(ButtonWidget.builder(t, b -> { cat = idx; page = 0; clearAndInit(); })
                    .dimensions(px + 4, py + i * 17, 84, 15).build());
        }
        List<Act> acts = actions();
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
        if (pages > 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); clearAndInit(); })
                    .dimensions(px + 96, py + 6 * 22 + 4, 40, 18).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(pages - 1, page + 1); clearAndInit(); })
                    .dimensions(px + 96 + 224, py + 6 * 22 + 4, 40, 18).build());
        }
    }

    private static void add(List<Act> l, String n, String tip, Runnable r) { l.add(new Act(n, tip, r, false)); }
    private static void keep(List<Act> l, String n, String tip, Runnable r) { l.add(new Act(n, tip, r, true)); }
    private static String onoff(boolean b) { return b ? "вкл" : "выкл"; }

    private List<Act> actions() {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Act> l = new ArrayList<>();
        final int s = Editor.step;
        switch (cat) {
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
            }
            case 9 -> {
                add(l, "Сохранить", "Буфер в файл слота", Editor::save);
                add(l, "Загрузить", "", Editor::load);
                keep(l, "Слот: " + Editor.slot, "", () -> Editor.slot = Editor.slot >= 9 ? 1 : Editor.slot + 1);
                keep(l, "Слот -", "", () -> Editor.slot = Math.max(1, Editor.slot - 1));
            }
            default -> {
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
        int w = 372, top = py - 24, bottom = py + CATS.length * 17 + 6;
        ctx.fill(px - 4, top, px + w, bottom, 0xE6101820);
        ctx.fill(px - 4, top, px + w, top + 20, 0xFF123A56);
        ctx.fill(px - 4, top + 20, px + w, top + 22, 0xFF00C8E0);
        ctx.fill(px + 90, top + 26, px + 91, bottom - 4, 0xFF2A3A48);
        super.render(ctx, mx, my, d);
        ctx.drawTextWithShadow(textRenderer, "MACKROS", px, top + 6, 0x55FFFF);
        ctx.drawTextWithShadow(textRenderer, CATS[cat] + (page > 0 ? "  стр " + (page + 1) : ""), px + 70, top + 6, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Editor.status(), px - 4, Math.min(bottom + 4, height - 12), 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() { return false; }
}
