package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Режим просмотра командных блоков: список компонентов проекта по механизмам, карточка выбранного (или наведённого) блока. */
public class NeroCbScreen extends Screen {
    private final NeroBuildPlan plan;
    private final NeroValidator.Report report;
    private final List<int[]> rows = new ArrayList<>(); // {0 заголовок механизма | 1 командный блок, индекс}
    private static final int VIS = 15, RH = 11;
    private int ox, oy, scroll = 0, sel = -1;

    public NeroCbScreen(NeroBuildPlan plan, NeroValidator.Report report) {
        super(Text.literal("Командные блоки"));
        this.plan = plan;
        this.report = report;
    }

    @Override
    protected void init() {
        ox = width / 2 - 190;
        oy = Math.max(4, (height - 206) / 2);
        rows.clear();
        for (int e = 0; e < plan.elements.size(); e++) {
            boolean head = false;
            for (int i = 0; i < plan.cbs.size(); i++) {
                if (plan.cbs.get(i).el() != e) continue;
                if (!head) { rows.add(new int[]{0, e}); head = true; }
                rows.add(new int[]{1, i});
            }
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> MinecraftClient.getInstance().setScreen(new NeroScreen())).dimensions(ox, oy + 186, 60, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▲"), b -> scroll(-5)).dimensions(ox + 64, oy + 186, 22, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▼"), b -> scroll(5)).dimensions(ox + 88, oy + 186, 22, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Показать в мире"), b -> {
            NeroBuilder.showPreview(plan);
            if (sel >= 0 && sel < plan.cbs.size()) NeroBuilder.selectedCb = plan.cbs.get(sel);
            NeroHistory.log("Предпросмотр включён: у командных блоков видны подписи. F9 вернёт в NERO.");
            close();
        }).dimensions(ox + 192, oy + 186, 110, 16).build());
    }

    private void scroll(int d) { scroll = Math.max(0, Math.min(Math.max(0, rows.size() - VIS), scroll + d)); }

    public boolean mouseScrolled(double mx, double my, double amount) { scroll(amount > 0 ? -2 : 2); return true; }

    public boolean mouseScrolled(double mx, double my, double h, double v) { scroll(v > 0 ? -2 : 2); return true; }

    private int rowAt(double mx, double my) {
        if (mx < ox || mx > ox + 186 || my < oy + 16) return -1;
        int r = (int) ((my - (oy + 16)) / RH);
        if (r < 0 || r >= VIS) return -1;
        int idx = scroll + r;
        return idx < rows.size() ? idx : -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int r = rowAt(mx, my);
        if (r >= 0 && rows.get(r)[0] == 1) { sel = rows.get(r)[1]; return true; }
        return super.mouseClicked(mx, my, button);
    }

    private String lbl(int i) { return i < 0 ? "нет" : plan.cbs.get(i).label(); }

    private static String modeRu(NeroBuildPlan.Cb k) {
        String t = k.type() == NeroBuildPlan.CbType.IMPULSE ? "Импульсный (запуск сигналом, например кнопкой)" : k.type() == NeroBuildPlan.CbType.REPEAT ? "Повторяющийся (работает каждый тик)" : "Цепочка (срабатывает после предыдущего)";
        return t + (k.auto() ? ", всегда активен" : ", нужен сигнал") + (k.cond() ? ", условный" : "");
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        ctx.drawTextWithShadow(textRenderer, "Командные блоки проекта: " + plan.cbs.size() + "  (наведи на строку или нажми)", ox, oy, 0x55FFFF);
        super.render(ctx, mx, my, d);
        ctx.fill(ox - 2, oy + 14, ox + 188, oy + 14 + VIS * RH + 3, 0x66000000);
        int hover = rowAt(mx, my);
        for (int i = 0; i < VIS; i++) {
            int idx = scroll + i;
            if (idx >= rows.size()) break;
            int[] row = rows.get(idx);
            int y = oy + 16 + i * RH;
            if (row[0] == 0) {
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth("▌ " + plan.elements.get(row[1]).name(), 184), ox, y, 0xFFD055);
            } else {
                NeroBuildPlan.Cb k = plan.cbs.get(row[1]);
                if (row[1] == sel) ctx.fill(ox, y - 1, ox + 186, y + RH - 2, 0x8833AAFF);
                else if (idx == hover) ctx.fill(ox, y - 1, ox + 186, y + RH - 2, 0x55FFFFFF);
                int col = k.type() == NeroBuildPlan.CbType.IMPULSE ? 0xFFAA33 : k.type() == NeroBuildPlan.CbType.REPEAT ? 0xCC77FF : 0x66FF88;
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth("  " + k.label(), 184), ox, y, col);
            }
        }
        int show = hover >= 0 && rows.get(hover)[0] == 1 ? rows.get(hover)[1] : sel;
        int px = ox + 192;
        ctx.fill(px - 2, oy + 14, px + 182, oy + 14 + VIS * RH + 3, 0x66000000);
        if (show < 0 || show >= plan.cbs.size()) {
            ctx.drawTextWithShadow(textRenderer, "Выбери командный блок слева:", px, oy + 18, 0xAAAAAA);
            ctx.drawTextWithShadow(textRenderer, "увидишь название, за что отвечает,", px, oy + 30, 0xAAAAAA);
            ctx.drawTextWithShadow(textRenderer, "команду, связи и часть механизма.", px, oy + 41, 0xAAAAAA);
            return;
        }
        NeroBuildPlan.Cb k = plan.cbs.get(show);
        int prev = -1, next = -1;
        for (int i = show - 1; i >= 0; i--) if (plan.cbs.get(i).el() == k.el()) { prev = i; break; }
        for (int i = show + 1; i < plan.cbs.size(); i++) if (plan.cbs.get(i).el() == k.el()) { next = i; break; }
        NeroBuildPlan.Element e = k.el() >= 0 && k.el() < plan.elements.size() ? plan.elements.get(k.el()) : null;
        List<String[]> info = new ArrayList<>();
        info.add(new String[]{"Название: " + k.label(), "FFD055"});
        info.add(new String[]{"Отвечает за: " + k.role(), "FFFFFF"});
        info.add(new String[]{"Команда: " + k.cmd(), "AAAAAA"});
        info.add(new String[]{"Режим: " + modeRu(k), "FFFFFF"});
        info.add(new String[]{"Позиция: " + k.x() + " " + k.y() + " " + k.z(), "AAAAAA"});
        info.add(new String[]{"Связи: до «" + lbl(prev) + "», после «" + lbl(next) + "»", "FFFFFF"});
        if (e != null) {
            String deps = "";
            for (int dI : e.deps()) if (dI >= 0 && dI < plan.elements.size()) deps += (deps.isEmpty() ? "" : ", ") + plan.elements.get(dI).name();
            info.add(new String[]{"Часть механизма: " + e.name() + (deps.isEmpty() ? "" : " (связан с: " + deps + ")"), "FFFFFF"});
            if (e.activation() != null && !e.activation().equals("-")) info.add(new String[]{"Запуск: " + e.activation(), "AAAAAA"});
        }
        info.add(new String[]{report == null ? "Проверка: не выполнялась" : report.ok() ? "Проверка проекта: команды корректны" : "Проверка проекта: есть ошибки, постройка не готова", report == null || !report.ok() ? "FF8855" : "55FF55"});
        int y = oy + 17;
        for (String[] l : info) {
            int color = Integer.parseInt(l[1], 16);
            for (OrderedText o : textRenderer.wrapLines(Text.literal(l[0]), 178)) {
                if (y > oy + 14 + VIS * RH - 6) return;
                ctx.drawTextWithShadow(textRenderer, o, px, y, color);
                y += 9;
            }
            y += 2;
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
