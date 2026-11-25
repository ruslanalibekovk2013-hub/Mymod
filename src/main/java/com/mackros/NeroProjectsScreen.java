package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** «Мои проекты»: библиотека проектов, версии, копия, переименование, удаление, экспорт и импорт. */
public class NeroProjectsScreen extends Screen {
    private static final int VIS = 12, RH = 12;
    private int ox, oy, scroll = 0;
    private String sel = "";
    private boolean versionsMode = false, deleteArmed = false, renaming = false;
    private int vsel = -1;
    private List<NeroProjects.Meta> metas = new ArrayList<>();
    private List<NeroProjects.Version> vers = new ArrayList<>();
    private TextFieldWidget nameField;
    private String note = "";

    public NeroProjectsScreen() { super(Text.literal("Мои проекты")); }

    @Override
    protected void init() {
        ox = width / 2 - 190;
        oy = Math.max(4, (height - 206) / 2);
        metas = NeroProjects.list();
        if (sel.isEmpty() && !metas.isEmpty()) sel = NeroProjects.currentId.isEmpty() ? metas.get(0).id() : NeroProjects.currentId;
        clearChildren();
        int bx = ox + 196, y = oy + 18;
        if (!versionsMode) {
            btn("Открыть", bx, y, 176, this::open); y += 20;
            btn("Версии", bx, y, 176, () -> { versionsMode = true; vsel = -1; vers = NeroProjects.versions(sel); init(); }); y += 20;
            btn("Копия", bx, y, 176, () -> { String n = NeroProjects.duplicate(sel); note = n == null ? "Не удалось создать копию" : "Копия создана"; init(); }); y += 20;
            btn("Переименовать", bx, y, 176, () -> { renaming = true; init(); }); y += 20;
            btn(deleteArmed ? "Точно удалить?" : "Удалить", bx, y, 176, () -> {
                if (!deleteArmed) { deleteArmed = true; init(); return; }
                NeroProjects.delete(sel); sel = ""; deleteArmed = false; note = "Проект удалён"; init();
            }); y += 20;
            btn("Экспорт", bx, y, 176, () -> { String p = NeroProjects.export(sel); note = p == null ? "Не удалось экспортировать" : "Экспорт: " + p; init(); }); y += 20;
            btn("Импорт из папки экспорта", bx, y, 176, () -> { int n = NeroProjects.importAll(); note = "Импортировано проектов: " + n + " (папка config/mackros/nero_export)"; init(); });
        } else {
            btn("Восстановить версию", bx, y, 176, this::restore); y += 20;
            btn("К списку проектов", bx, y, 176, () -> { versionsMode = false; init(); });
        }
        if (renaming) {
            nameField = new TextFieldWidget(textRenderer, ox, oy + 168, 300, 18, Text.literal("имя"));
            nameField.setMaxLength(60);
            for (NeroProjects.Meta m : metas) if (m.id().equals(sel)) nameField.setText(m.name());
            addDrawableChild(nameField);
            setInitialFocus(nameField);
            btn("OK", ox + 304, oy + 168, 68, () -> { NeroProjects.rename(sel, nameField.getText()); renaming = false; note = "Переименовано"; init(); });
        }
        btn("Назад", ox + 296, oy + 188, 76, () -> MinecraftClient.getInstance().setScreen(new NeroScreen()));
        btn("Чат", ox + 216, oy + 188, 76, () -> MinecraftClient.getInstance().setScreen(NeroScreen.chat()));
    }

    private void btn(String label, int x, int y, int w, Runnable r) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> r.run()).dimensions(x, y, w, 18).build());
    }

    private void open() {
        NeroSpec s = NeroProjects.open(sel);
        if (s == null) { note = "Не удалось открыть проект"; init(); return; }
        NeroScreen.loadProject(s);
        NeroHistory.log("Открыт проект «" + s.title() + "»: можно продолжить редактирование без повторной генерации");
        MinecraftClient.getInstance().setScreen(new NeroScreen());
    }

    private void restore() {
        if (vsel < 0 || vsel >= vers.size()) { note = "Выбери версию слева"; return; }
        NeroSpec s = NeroProjects.restore(sel, vers.get(vsel).n());
        if (s == null) { note = "Не удалось восстановить"; return; }
        NeroScreen.loadProject(s);
        NeroHistory.log("Восстановлена версия " + vers.get(vsel).n() + " проекта «" + s.title() + "» (как новая версия, история сохранена)");
        MinecraftClient.getInstance().setScreen(new NeroScreen());
    }

    public boolean mouseScrolled(double mx, double my, double amount) { scroll = Math.max(0, scroll + (amount > 0 ? -2 : 2)); return true; }

    public boolean mouseScrolled(double mx, double my, double h, double v) { scroll = Math.max(0, scroll + (v > 0 ? -2 : 2)); return true; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= ox && mx <= ox + 190 && my >= oy + 16) {
            int r = (int) ((my - (oy + 16)) / RH);
            int idx = scroll + r;
            if (r >= 0 && r < VIS) {
                if (!versionsMode && idx < metas.size()) { sel = metas.get(idx).id(); deleteArmed = false; return true; }
                if (versionsMode && idx < vers.size()) { vsel = idx; return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        ctx.drawTextWithShadow(textRenderer, versionsMode ? "Версии проекта (ничего не перезаписывается)" : "Мои проекты: " + metas.size(), ox, oy, 0x55FFFF);
        super.render(ctx, mx, my, d);
        ctx.fill(ox - 2, oy + 14, ox + 192, oy + 16 + VIS * RH, 0x66000000);
        for (int i = 0; i < VIS; i++) {
            int idx = scroll + i;
            int y = oy + 16 + i * RH;
            if (!versionsMode) {
                if (idx >= metas.size()) break;
                NeroProjects.Meta m = metas.get(idx);
                if (m.id().equals(sel)) ctx.fill(ox, y - 1, ox + 190, y + RH - 2, 0x8833AAFF);
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(m.name() + " · " + m.modified().replaceFirst("^\\d{4}-", ""), 188), ox + 1, y, m.id().equals(NeroProjects.currentId) ? 0x88FF88 : 0xFFFFFF);
            } else {
                if (idx >= vers.size()) break;
                NeroProjects.Version v = vers.get(idx);
                if (idx == vsel) ctx.fill(ox, y - 1, ox + 190, y + RH - 2, 0x8833AAFF);
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth("v" + v.n() + " · " + v.time().replaceFirst("^\\d{4}-", "") + " · " + v.note(), 188), ox + 1, y, 0xFFFFFF);
            }
        }
        if (!versionsMode) for (NeroProjects.Meta m : metas)
            if (m.id().equals(sel)) {
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth("Создан: " + m.created(), 180), ox + 196, oy + 142, 0xAAAAAA);
                ctx.drawTextWithShadow(textRenderer, "Версий: " + m.versions(), ox + 196, oy + 152, 0xAAAAAA);
            }
        if (!note.isEmpty()) ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(note, 372), ox, oy + 156, 0xFFD055);
        if (metas.isEmpty() && !versionsMode) ctx.drawTextWithShadow(textRenderer, "Проектов пока нет: создай первый на главном экране NERO", ox + 2, oy + 20, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() { return false; }
}
