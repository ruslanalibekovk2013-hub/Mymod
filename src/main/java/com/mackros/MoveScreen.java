package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Меню переноса: сдвиг, клон и буфер с кнопками направлений, шаги, координаты и быстрые операции. */
public class MoveScreen extends Screen {
    private static final int W = 344;
    private static final int[] STEPS = {1, 2, 5, 10, 16, 32, 64, 128};
    private static String fx = "", fy = "", fz = "";
    private int px, py;
    private TextFieldWidget tx, ty, tz;

    public MoveScreen() { super(Text.literal("Перенос")); }

    private Text label(String s, boolean active) {
        return active ? Text.literal(s).styled(st -> st.withColor(Config.accent())) : Text.literal(s);
    }

    private ButtonWidget btn(String name, String tip, int x, int y, int w, int h, boolean active, boolean refresh, Runnable r) {
        ButtonWidget.Builder b = ButtonWidget.builder(label(name, active), bt -> {
            r.run();
            if (refresh) clearAndInit();
        }).dimensions(x, y, w, h);
        if (!tip.isEmpty()) b.tooltip(Tooltip.of(Text.literal(tip)));
        return addDrawableChild(b.build());
    }

    private static int num(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private TextFieldWidget field(int x, String hint, String val, java.util.function.Consumer<String> on) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, py + 68, 46, 16, Text.literal(hint));
        f.setMaxLength(9);
        f.setTextPredicate(s -> s.matches("-?\\d*"));
        f.setSuggestion(hint);
        f.setText(val);
        f.setChangedListener(s -> { on.accept(s); f.setSuggestion(s.isEmpty() ? hint : ""); });
        f.setSuggestion(val.isEmpty() ? hint : "");
        return addDrawableChild(f);
    }

    private void setFields(BlockPos p) {
        fx = String.valueOf(p.getX()); fy = String.valueOf(p.getY()); fz = String.valueOf(p.getZ());
        clearAndInit();
    }

    @Override
    protected void init() {
        MinecraftClient mc = MinecraftClient.getInstance();
        px = width / 2 - W / 2;
        py = Math.max(4, (height - 196) / 2);
        if (fx.isEmpty() && fy.isEmpty() && fz.isEmpty() && mc.player != null) {
            BlockPos p = mc.player.getBlockPos();
            fx = String.valueOf(p.getX()); fy = String.valueOf(p.getY()); fz = String.valueOf(p.getZ());
        }
        final int step = Editor.step;

        // режимы
        for (int i = 0; i < 3; i++) {
            final int m = i;
            String[] tips = {"Выделение переезжает на шаг", "Копия выделения ставится рядом, исходник остаётся", "Двигается место вставки буфера (как стрелки)"};
            btn(Move.MODES[i], tips[i], px + 6 + i * 66, py + 24, 64, 18, Move.mode == i, true, () -> Move.mode = m);
        }
        btn(Config.relMove ? "По взгляду" : "По миру", "Оси: по взгляду игрока или по сторонам света", px + 206, py + 24, 62, 18, false, true,
                () -> { Config.relMove = !Config.relMove; Config.save(); });
        btn("Воздух: " + (Move.air ? "да" : "нет"), "Клон: копировать и воздух (затирает блоки на месте копии)", px + 270, py + 24, 68, 18, Move.air, true,
                () -> { Move.air = !Move.air; Config.save(); });

        // шаги
        for (int i = 0; i < STEPS.length; i++) {
            final int v = STEPS[i];
            btn(String.valueOf(v), "Шаг " + v + " блоков", px + 6 + i * 41, py + 46, 38, 16, step == v, true, () -> { Editor.step = v; Config.save(); });
        }

        // крестовина направлений
        int x0 = px + 6, y0 = py + 68;
        boolean rel = Config.relMove;
        btn(rel ? "Вперёд" : "С -Z", "Сдвинуть вперёд на шаг", x0 + 44, y0, 42, 20, false, false, () -> Move.go(mc, 0, step));
        btn(rel ? "Влево" : "З -X", "", x0, y0 + 22, 42, 20, false, false, () -> Move.go(mc, 2, step));
        btn("Цель", "Перенести выделение к блоку в прицеле", x0 + 44, y0 + 22, 42, 20, false, false, () -> Move.moveToLook(mc));
        btn(rel ? "Вправо" : "В +X", "", x0 + 88, y0 + 22, 42, 20, false, false, () -> Move.go(mc, 3, step));
        btn(rel ? "Назад" : "Ю +Z", "", x0 + 44, y0 + 44, 42, 20, false, false, () -> Move.go(mc, 1, step));
        btn("Вверх", "", x0 + 132, y0, 42, 20, false, false, () -> Move.go(mc, 4, step));
        btn("Вниз", "", x0 + 132, y0 + 44, 42, 20, false, false, () -> Move.go(mc, 5, step));
        btn("x" + Move.count, "Сколько копий для Клона и Стека", x0 + 132, y0 + 22, 42, 20, false, true,
                () -> { Move.count = Move.count >= 10 ? 1 : Move.count + 1; Config.save(); });

        // координаты
        int x1 = px + 192;
        tx = field(x1, "X", fx, s -> fx = s);
        ty = field(x1 + 50, "Y", fy, s -> fy = s);
        tz = field(x1 + 100, "Z", fz, s -> fz = s);
        btn("Угол в XYZ", "Перенести выделение так, чтобы его угол оказался в этих координатах", x1, py + 90, 72, 20, false, false,
                () -> Move.moveTo(mc, num(fx, 0), num(fy, 0), num(fz, 0)));
        btn("Вставить тут", "Вставить буфер в эти координаты", x1 + 76, py + 90, 72, 20, false, false,
                () -> Move.pasteAt(mc, num(fx, 0), num(fy, 0), num(fz, 0)));
        btn("Мои коорд.", "Взять координаты игрока", x1, py + 112, 72, 20, false, false, () -> setFields(mc.player.getBlockPos()));
        btn("Из прицела", "Взять координаты блока в прицеле", x1 + 76, py + 112, 72, 20, false, false, () -> {
            BlockPos p = Editor.look(mc, false);
            if (p == null) Editor.msg("Нет блока в прицеле"); else setFields(p);
        });

        // действия
        int ya = py + 138, yb = py + 160, w = 41;
        btn("Копир.", "Выделение в буфер (B)", px + 6, ya, w, 20, false, false, () -> Editor.copy(mc));
        btn("Вырез.", "(N)", px + 48, ya, w, 20, false, false, () -> Editor.cut(mc));
        btn("Встав.", "В прицел или в закреплённое место (V)", px + 90, ya, w, 20, false, false, () -> Editor.paste(mc));
        btn("Отмена", "(U)", px + 132, ya, w, 20, false, false, () -> Editor.undo(mc));
        btn("Повтор", "(I)", px + 174, ya, w, 20, false, false, () -> Editor.redo(mc));
        btn("Поворот", "Повернуть буфер на 90° (R)", px + 216, ya, w, 20, false, false, Editor::rotate);
        btn("Отр. X", "Отразить буфер по X (G)", px + 258, ya, w, 20, false, false, () -> Editor.flip(true));
        btn("Отр. Z", "Отразить буфер по Z", px + 300, ya, w, 20, false, false, () -> Editor.flip(false));

        btn("Стек", "Повторить выделение вперёд x" + Move.count, px + 6, yb, w, 20, false, false, () -> { Editor.stackN = Move.count; Editor.stack(mc); });
        btn("Обмен", "Поменять выделение местами с областью впереди на шаг (области не должны пересекаться)", px + 48, yb, w, 20, false, false, () -> {
            int[] v = Move.vec(mc, 0);
            Move.swap(mc, v[0] * step, v[1] * step, v[2] * step);
        });
        btn("На землю", "Опустить выделение, пока не встанет на твёрдый блок", px + 90, yb, w, 20, false, false, () -> Move.drop(mc));
        btn("К игроку", "Поставить выделение на игрока", px + 132, yb, w, 20, false, false, () -> Move.toPlayer(mc));
        btn("Вокруг", "Выделить куб вокруг игрока, радиус = шаг (до 64)", px + 174, yb, w, 20, false, false, () -> Move.around(mc, Math.min(64, step)));
        btn("Чанк", "Выделить чанк игрока (±32 по высоте)", px + 216, yb, w, 20, false, false, () -> Move.chunk(mc, false));
        btn("Т1<>Т2", "Поменять точки местами", px + 258, yb, w, 20, false, false, Move::swapPoints);
        btn("Сброс", "Сбросить выделение", px + 300, yb, w, 20, false, false, Editor::resetSel);

        btn("Меню", "Главное меню Mackros", px + W - 46, py + 2, 44, 16, false, false, () -> mc.setScreen(new MackrosScreen()));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        int acc = 0xFF000000 | Config.accent();
        int top = py, bottom = py + 196;
        ctx.fill(px - 4, top, px + W + 4, bottom, 0xE6101820);
        ctx.fill(px - 4, top, px + W + 4, top + 20, 0xFF123A56);
        ctx.fill(px - 4, top + 20, px + W + 4, top + 22, acc);
        ctx.fill(px + 186, py + 66, px + 187, py + 134, 0xFF2A3A48);
        super.render(ctx, mx, my, d);
        ctx.drawTextWithShadow(textRenderer, "ПЕРЕНОС", px, top + 6, Config.accent());
        ctx.drawTextWithShadow(textRenderer, "размер " + Move.dims() + "  буфер " + Editor.clip.size(), px + 56, top + 6, 0xFFFFFF);
        String[] hint = {"Сдвиг: выделение переезжает на шаг", "Клон: копия рядом, исходник на месте (x" + Move.count + ")", "Буфер: двигаем место вставки (смещ. " + Editor.ox + " " + Editor.oy + " " + Editor.oz + ")"};
        ctx.drawTextWithShadow(textRenderer, hint[Math.max(0, Math.min(2, Move.mode))], px, py + 178, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, Editor.status(), px - 4, Math.min(bottom + 4, height - 11), 0x888888);
    }

    @Override
    public boolean shouldPause() { return false; }
}
