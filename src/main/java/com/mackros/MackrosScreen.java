package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MackrosScreen extends Screen {
    private record Act(String name, Runnable run, boolean keep) {}

    private static final String[] CATS = {"Выделение", "Буфер", "Фигуры", "Мир", "Игрок", "Файлы"};
    private static int cat = 0;

    public MackrosScreen() { super(Text.literal("Mackros")); }

    @Override
    protected void init() {
        int x0 = width / 2 - 174, y0 = 36;
        for (int i = 0; i < CATS.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal((cat == i ? "> " : "") + CATS[i]), b -> { cat = idx; clearAndInit(); })
                    .dimensions(x0, y0 + i * 24, 80, 20).build());
        }
        List<Act> acts = actions();
        for (int i = 0; i < acts.size(); i++) {
            final Act a = acts.get(i);
            int col = i % 3, row = i / 3;
            addDrawableChild(ButtonWidget.builder(Text.literal(a.name()), b -> {
                a.run().run();
                if (a.keep()) clearAndInit(); else close();
            }).dimensions(x0 + 90 + col * 86, y0 + row * 24, 82, 20).build());
        }
    }

    private static void add(List<Act> l, String n, Runnable r) { l.add(new Act(n, r, false)); }
    private static void keep(List<Act> l, String n, Runnable r) { l.add(new Act(n, r, true)); }

    private List<Act> actions() {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Act> l = new ArrayList<>();
        int s = Editor.step;
        switch (cat) {
            case 0 -> {
                add(l, "Точка 1", () -> Editor.setPos(mc, 1));
                add(l, "Точка 2", () -> Editor.setPos(mc, 2));
                add(l, "Сброс", Editor::resetSel);
                add(l, "Заполнить", () -> Editor.fill(mc, Editor.held(mc)));
                add(l, "Полый бокс", () -> Editor.hollow(mc, Editor.held(mc)));
                add(l, "Замена", () -> Editor.replace(mc));
                add(l, "Очистить", () -> Editor.fill(mc, net.minecraft.block.Blocks.AIR.getDefaultState()));
                add(l, "Стек x" + Editor.stackN, () -> Editor.stack(mc));
                keep(l, "Стек n: " + Editor.stackN, () -> Editor.stackN = Editor.stackN >= 20 ? 1 : Editor.stackN + 1);
                keep(l, "Шаг: " + s, () -> Editor.step = s == 1 ? 2 : s == 2 ? 5 : s == 5 ? 10 : s == 10 ? 50 : 1);
                keep(l, "Симм X: " + (Editor.symX ? "вкл" : "выкл"), () -> Editor.toggleSymAxis(mc, true));
                keep(l, "Симм Z: " + (Editor.symZ ? "вкл" : "выкл"), () -> Editor.toggleSymAxis(mc, false));
                keep(l, "Сдвиг +X", () -> Editor.moveSel(mc, s, 0, 0));
                keep(l, "Сдвиг -X", () -> Editor.moveSel(mc, -s, 0, 0));
                keep(l, "Сдвиг +Y", () -> Editor.moveSel(mc, 0, s, 0));
                keep(l, "Сдвиг -Y", () -> Editor.moveSel(mc, 0, -s, 0));
                keep(l, "Сдвиг +Z", () -> Editor.moveSel(mc, 0, 0, s));
                keep(l, "Сдвиг -Z", () -> Editor.moveSel(mc, 0, 0, -s));
            }
            case 1 -> {
                add(l, "Копировать", () -> Editor.copy(mc));
                add(l, "Вырезать", () -> Editor.cut(mc));
                add(l, "Вставить", () -> Editor.paste(mc));
                keep(l, "Поворот 90°", Editor::rotate);
                keep(l, "Отразить X", () -> Editor.flip(true));
                keep(l, "Отразить Z", () -> Editor.flip(false));
                keep(l, "Просмотр: " + (Editor.preview ? "вкл" : "выкл"), Editor::togglePreview);
                keep(l, "Воздух: " + (Editor.pasteAir ? "да" : "нет"), () -> Editor.pasteAir = !Editor.pasteAir);
                keep(l, "Сброс смещ.", Editor::resetNudge);
                keep(l, "Смещ. +X", () -> Editor.nudge(s, 0, 0));
                keep(l, "Смещ. -X", () -> Editor.nudge(-s, 0, 0));
                keep(l, "Смещ. +Y", () -> Editor.nudge(0, s, 0));
                keep(l, "Смещ. -Y", () -> Editor.nudge(0, -s, 0));
                keep(l, "Смещ. +Z", () -> Editor.nudge(0, 0, s));
                keep(l, "Смещ. -Z", () -> Editor.nudge(0, 0, -s));
                keep(l, "Отмена", () -> Editor.undo(mc));
                keep(l, "Повтор", () -> Editor.redo(mc));
            }
            case 2 -> {
                add(l, "Сфера", () -> Editor.sphere(mc, false));
                add(l, "Полая сфера", () -> Editor.sphere(mc, true));
                add(l, "Цилиндр", () -> Editor.cylinder(mc));
                keep(l, "Радиус: " + Editor.radius, () -> Editor.radius = Editor.radius >= 64 ? 1 : Editor.radius + 1);
                keep(l, "Радиус -", () -> Editor.radius = Math.max(1, Editor.radius - 1));
                keep(l, "Радиус +5", () -> Editor.radius = Math.min(64, Editor.radius + 5));
                keep(l, "Высота: " + Editor.height, () -> Editor.height = Editor.height >= 100 ? 1 : Editor.height + 1);
                keep(l, "Высота -", () -> Editor.height = Math.max(1, Editor.height - 1));
                keep(l, "Высота +10", () -> Editor.height = Math.min(100, Editor.height + 10));
            }
            case 3 -> {
                add(l, "Верхний мир", () -> Editor.cmd(mc, "execute in minecraft:overworld run tp @s ~ ~ ~"));
                add(l, "Незер", () -> Editor.cmd(mc, "execute in minecraft:the_nether run tp @s ~ ~ ~"));
                add(l, "Энд", () -> Editor.cmd(mc, "execute in minecraft:the_end run tp @s ~ ~ ~"));
                add(l, "ТП к прицелу", () -> {
                    var p = Editor.look(mc, true);
                    if (p != null) Editor.cmd(mc, "tp @s " + p.getX() + " " + p.getY() + " " + p.getZ());
                });
                add(l, "Утро", () -> Editor.cmd(mc, "time set 0"));
                add(l, "День", () -> Editor.cmd(mc, "time set day"));
                add(l, "Вечер", () -> Editor.cmd(mc, "time set 12500"));
                add(l, "Ночь", () -> Editor.cmd(mc, "time set night"));
                add(l, "Ясно", () -> Editor.cmd(mc, "weather clear"));
                add(l, "Дождь", () -> Editor.cmd(mc, "weather rain"));
                add(l, "Гроза", () -> Editor.cmd(mc, "weather thunder"));
            }
            case 4 -> {
                add(l, "Творчество", () -> Editor.cmd(mc, "gamemode creative"));
                add(l, "Спектатор", () -> Editor.cmd(mc, "gamemode spectator"));
                add(l, "Выживание", () -> Editor.cmd(mc, "gamemode survival"));
                add(l, "Ночн. зрение", () -> Editor.cmd(mc, "effect give @s minecraft:night_vision infinite 0 true"));
                add(l, "Скорость", () -> Editor.cmd(mc, "effect give @s minecraft:speed infinite 3 true"));
                add(l, "Снять эффекты", () -> Editor.cmd(mc, "effect clear @s"));
                keep(l, "Полёт x1", () -> mc.player.getAbilities().setFlySpeed(0.05f));
                keep(l, "Полёт x2", () -> mc.player.getAbilities().setFlySpeed(0.10f));
                keep(l, "Полёт x4", () -> mc.player.getAbilities().setFlySpeed(0.20f));
                keep(l, "Полёт x8", () -> mc.player.getAbilities().setFlySpeed(0.40f));
            }
            default -> {
                add(l, "Сохранить", Editor::save);
                add(l, "Загрузить", Editor::load);
                keep(l, "Слот: " + Editor.slot, () -> Editor.slot = Editor.slot >= 9 ? 1 : Editor.slot + 1);
                keep(l, "Слот -", () -> Editor.slot = Math.max(1, Editor.slot - 1));
            }
        }
        return l;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        super.render(ctx, mx, my, d);
        ctx.drawCenteredTextWithShadow(textRenderer, "Mackros — " + CATS[cat], width / 2, 14, 0x55FFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Editor.status(), width / 2, height - 16, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() { return false; }
}
