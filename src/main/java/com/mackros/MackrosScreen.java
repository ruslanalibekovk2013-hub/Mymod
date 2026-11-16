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

    private static final String[] CATS = {"Выделение", "Буфер", "Фигуры", "Рисование", "КБ оружие", "КБ блоки", "Мир", "Игрок", "Файлы"};
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
                    .dimensions(px + 4, py + i * 19, 84, 17).build());
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

    private List<Act> actions() {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Act> l = new ArrayList<>();
        final int s = Editor.step;
        switch (cat) {
            case 0 -> {
                add(l, "Точка 1", "Угол выделения (клавиша Z)", () -> Editor.setPos(mc, 1));
                add(l, "Точка 2", "Противоположный угол (X)", () -> Editor.setPos(mc, 2));
                add(l, "Сброс", "", Editor::resetSel);
                add(l, "Заполнить", "Блоком из руки (J)", () -> Editor.fill(mc, Editor.held(mc)));
                add(l, "Полый бокс", "Только стенки", () -> Editor.hollow(mc, Editor.held(mc)));
                add(l, "Замена", "Блок в прицеле заменить блоком из руки", () -> Editor.replace(mc));
                add(l, "Очистить", "Заполнить воздухом (K)", () -> Editor.fill(mc, Blocks.AIR.getDefaultState()));
                add(l, "Стек x" + Editor.stackN, "Повторить выделение вперёд", () -> Editor.stack(mc));
                keep(l, "Стек n: " + Editor.stackN, "Сколько повторов", () -> Editor.stackN = Editor.stackN >= 20 ? 1 : Editor.stackN + 1);
                keep(l, "Шаг: " + s, "Шаг сдвигов", () -> Editor.step = s == 1 ? 2 : s == 2 ? 5 : s == 5 ? 10 : s == 10 ? 50 : 1);
                keep(l, "Симм X: " + (Editor.symX ? "вкл" : "выкл"), "Зеркалить все правки по X", () -> Editor.toggleSymAxis(mc, true));
                keep(l, "Симм Z: " + (Editor.symZ ? "вкл" : "выкл"), "Зеркалить все правки по Z", () -> Editor.toggleSymAxis(mc, false));
                keep(l, "Сдвиг +X", "Перенести постройку", () -> Editor.moveSel(mc, s, 0, 0));
                keep(l, "Сдвиг -X", "", () -> Editor.moveSel(mc, -s, 0, 0));
                keep(l, "Сдвиг +Y", "", () -> Editor.moveSel(mc, 0, s, 0));
                keep(l, "Сдвиг -Y", "", () -> Editor.moveSel(mc, 0, -s, 0));
                keep(l, "Сдвиг +Z", "", () -> Editor.moveSel(mc, 0, 0, s));
                keep(l, "Сдвиг -Z", "", () -> Editor.moveSel(mc, 0, 0, -s));
                keep(l, "Поворот выд.", "Повернуть постройку на месте на 90°", () -> Editor.transformSel(mc, 0));
                keep(l, "Отраж. выд. X", "Отразить постройку на месте", () -> Editor.transformSel(mc, 1));
                keep(l, "Отраж. выд. Z", "", () -> Editor.transformSel(mc, 2));
                keep(l, "Шире вперёд", "Расширить выделение на шаг", () -> Editor.expand(mc, 0, s));
                keep(l, "Шире назад", "", () -> Editor.expand(mc, 3, s));
                keep(l, "Шире вверх", "", () -> Editor.expand(mc, 1, s));
                keep(l, "Шире вниз", "", () -> Editor.expand(mc, 2, s));
                keep(l, "Уже вперёд", "Сузить выделение на шаг", () -> Editor.expand(mc, 0, -s));
                keep(l, "Уже сверху", "", () -> Editor.expand(mc, 1, -s));
            }
            case 1 -> {
                add(l, "Копировать", "Выделение в буфер (B)", () -> Editor.copy(mc));
                add(l, "Вырезать", "(N)", () -> Editor.cut(mc));
                add(l, "Вставить", "В место прицела (V)", () -> Editor.paste(mc));
                keep(l, "Поворот 90°", "Повернуть буфер (R)", Editor::rotate);
                keep(l, "Отразить X", "(G)", () -> Editor.flip(true));
                keep(l, "Отразить Z", "", () -> Editor.flip(false));
                keep(l, "Просмотр: " + (Editor.preview ? "вкл" : "выкл"), "Контур вставки (P)", Editor::togglePreview);
                keep(l, "Воздух: " + (Editor.pasteAir ? "да" : "нет"), "Вставлять воздух (затирает блоки)", () -> Editor.pasteAir = !Editor.pasteAir);
                keep(l, "Сброс смещ.", "", Editor::resetNudge);
                keep(l, "Смещ. +X", "Двигать место вставки (стрелки)", () -> Editor.nudge(s, 0, 0));
                keep(l, "Смещ. -X", "", () -> Editor.nudge(-s, 0, 0));
                keep(l, "Смещ. +Y", "", () -> Editor.nudge(0, s, 0));
                keep(l, "Смещ. -Y", "", () -> Editor.nudge(0, -s, 0));
                keep(l, "Смещ. +Z", "", () -> Editor.nudge(0, 0, s));
                keep(l, "Смещ. -Z", "", () -> Editor.nudge(0, 0, -s));
                keep(l, "Отмена", "(U)", () -> Editor.undo(mc));
                keep(l, "Повтор", "(I)", () -> Editor.redo(mc));
            }
            case 2 -> {
                add(l, "Сфера", "Блок из руки, центр в прицеле (H)", () -> Editor.sphere(mc, false));
                add(l, "Полая сфера", "", () -> Editor.sphere(mc, true));
                add(l, "Цилиндр", "", () -> Editor.cylinder(mc));
                add(l, "Купол", "Верхняя половина сферы", () -> Editor.dome(mc, false));
                add(l, "Полый купол", "", () -> Editor.dome(mc, true));
                keep(l, "Радиус: " + Editor.radius, "", () -> Editor.radius = Editor.radius >= 64 ? 1 : Editor.radius + 1);
                keep(l, "Радиус -", "", () -> Editor.radius = Math.max(1, Editor.radius - 1));
                keep(l, "Радиус +5", "", () -> Editor.radius = Math.min(64, Editor.radius + 5));
                keep(l, "Высота: " + Editor.height, "Для цилиндра", () -> Editor.height = Editor.height >= 100 ? 1 : Editor.height + 1);
                keep(l, "Высота -", "", () -> Editor.height = Math.max(1, Editor.height - 1));
                keep(l, "Высота +10", "", () -> Editor.height = Math.min(100, Editor.height + 10));
            }
            case 3 -> {
                add(l, "Линия", "Между точками 1 и 2 (L)", () -> Editor.line(mc));
                add(l, "Эллипсоид", "Вписать в выделение", () -> Editor.ellipsoid(mc, false, false));
                add(l, "Полый элл.", "", () -> Editor.ellipsoid(mc, true, false));
                add(l, "Купол выд.", "Купол по размерам выделения", () -> Editor.ellipsoid(mc, false, true));
                add(l, "Случайно", "Рандом из блоков хотбара (O)", () -> Editor.paletteFill(mc, false));
                add(l, "Градиент", "Слои по высоте из хотбара", () -> Editor.paletteFill(mc, true));
                add(l, "Замена палитрой", "Блок в прицеле -> рандом из хотбара", () -> Editor.paletteReplace(mc));
                add(l, "Сгладить", "Смягчить рельеф в выделении (M)", () -> Editor.smooth(mc, 1));
                add(l, "Сгладить x3", "", () -> Editor.smooth(mc, 3));
            }
            case 4 -> {
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
            case 5 -> {
                add(l, "Зона лечения", "Регенерация в радиусе 8", () -> Cb.buildUtil(mc, "heal", "Зона лечения"));
                add(l, "Турель", "Бьёт врагов в радиусе 12", () -> Cb.buildUtil(mc, "turret", "Турель"));
                add(l, "Авто-день", "Всегда день", () -> Cb.buildUtil(mc, "day", "Авто-день"));
                add(l, "Лифт", "Поднимает игроков рядом", () -> Cb.buildUtil(mc, "lift", "Лифт"));
                add(l, "Магнит", "Притягивает предметы", () -> Cb.buildUtil(mc, "magnet", "Магнит"));
                add(l, "Удалить КБ", "Убрать последнюю постройку", () -> Cb.removeLast(mc));
                add(l, "Выдать КБ", "Командный, повторяющий, цепной", () -> Cb.giveBlocks(mc));
            }
            case 6 -> {
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
            case 7 -> {
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
            default -> {
                add(l, "Сохранить", "Буфер в файл слота", Editor::save);
                add(l, "Загрузить", "", Editor::load);
                keep(l, "Слот: " + Editor.slot, "", () -> Editor.slot = Editor.slot >= 9 ? 1 : Editor.slot + 1);
                keep(l, "Слот -", "", () -> Editor.slot = Math.max(1, Editor.slot - 1));
            }
        }
        return l;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        int w = 372, top = py - 24, bottom = py + CATS.length * 19 + 6;
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
