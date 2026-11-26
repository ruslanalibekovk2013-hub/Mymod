package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Меню NERO: всё разделено по категориям (механизмы-строители, оружие, лифты и транспорт, устройства, постройки, инструменты). */
public class NeroCatalogScreen extends Screen {
    private record Item(String name, String desc, String request) { }

    private record Cat(String name, List<Item> items) { }

    private static final int ROWS = 5, RH = 32;
    private static int cat = 0, scroll = 0;
    private final List<Cat> cats = new ArrayList<>();
    private int ox, oy;

    public NeroCatalogScreen() { super(Text.literal("NERO · Каталог")); }

    private static Item it(String n, String d, String r) { return new Item(n, d, r); }

    private void build() {
        cats.clear();
        List<Item> mach = new ArrayList<>();
        mach.add(it("Автобашня", "11 этажей с окнами и шпилем, пульт и таблички", "Создай автобашню с пультом"));
        mach.add(it("Мост-раскладушка", "выдвигает мост по секциям, СНОС складывает", "Создай мост-раскладушку с пультом"));
        mach.add(it("Дом-принтер", "печатает дом: фундамент, стены, крыша, интерьер, сад", "Создай дом-принтер с пультом"));
        mach.add(it("Крепость-автосборка", "стены, 4 башни, ворота, колодец и донжон", "Создай крепость-автосборку с пультом"));
        mach.add(it("Пирамида-генератор", "15 ярусов, вход, зал с сокровищами", "Создай пирамиду-генератор с пультом"));
        cats.add(new Cat("Механизмы-строители", mach));
        List<Item> weap = new ArrayList<>();
        for (NeroWeapons.W w : NeroWeapons.ALL) weap.add(it(w.name(), w.how(), "Создай " + w.name()));
        for (NeroArsenal.WR w : NeroArsenal.ALL) weap.add(it(w.name(), w.how(), "Создай " + w.name()));
        cats.add(new Cat("Оружие (игровое)", weap));
        List<Item> lift = new ArrayList<>();
        lift.add(it("Лифт на 5 этажей", "кабина, стеклянные двери, очередь вызовов, табло", "Создай лифт на 5 этажей"));
        lift.add(it("Роскошный лифт на 8 этажей", "стиль Luxury: золото и чёрный камень", "Создай роскошный лифт на 8 этажей"));
        lift.add(it("Футуристичный лифт на 10 этажей", "стиль Futuristic: белый бетон и голубое стекло", "Создай футуристичный лифт на 10 этажей"));
        lift.add(it("Панорамный лифт на 6 этажей", "шахта из стекла", "Создай панорамный лифт на 6 этажей"));
        lift.add(it("Поезд", "пути с питанием, станция и состав-модель", "Создай поезд"));
        lift.add(it("Подвесной мост", "башни, тросы, фонари", "Создай подвесной мост 40 блоков"));
        cats.add(new Cat("Лифты и транспорт", lift));
        List<Item> dev = new ArrayList<>();
        dev.add(it("3D-принтер", "две платформы, миниатюра из мини-блоков 1:2, 1:4, 1:8", "Создай 3D-принтер"));
        dev.add(it("Portal Gun", "предмет: ПКМ синий портал, Shift+ПКМ оранжевый", "Создай Portal Gun"));
        dev.add(it("Портал", "две арки с телепортацией", "Создай портал"));
        dev.add(it("Компьютер", "монитор, клавиатура, заставка на экране", "Создай компьютер"));
        dev.add(it("Робот", "руки поднимаются, глаза включаются", "Создай робота"));
        cats.add(new Cat("Устройства", dev));
        List<Item> bld = new ArrayList<>();
        bld.add(it("База", "здание, лифт, ворота, сигнализация, пульт", "Создай базу с лифтом, воротами и сигнализацией"));
        bld.add(it("Дом с гаражом и бассейном", "двухэтажный, современный", "Построй современный двухэтажный дом с гаражом и бассейном"));
        bld.add(it("Космический корабль", "корпус, кабина, двигатели, люк", "Создай космический корабль"));
        bld.add(it("Фабрика", "здание, конвейер, ферма, бур", "Создай фабрику с конвейером и фермой"));
        cats.add(new Cat("Постройки", bld));
        List<Item> tools = new ArrayList<>();
        tools.add(it("Главный экран NERO", "поле запроса, предпросмотр, построить", "@main"));
        tools.add(it("NERO: вопрос", "спросить о проекте или поменять часть", "@chat"));
        tools.add(it("Мои проекты", "версии, восстановление, экспорт и импорт", "@projects"));
        tools.add(it("3D-редактор", "камера, выбор, перемещение, поворот, режимы", "@editor"));
        tools.add(it("Командные блоки", "список, подписи, связи", "@cb"));
        tools.add(it("Настройки модели", "ключ OpenRouter, проверка подключения", "@settings"));
        cats.add(new Cat("Инструменты", tools));
    }

    @Override
    protected void init() {
        if (cats.isEmpty()) build();
        ox = width / 2 - 190;
        oy = Math.max(4, (height - 206) / 2);
        clearChildren();
        for (int i = 0; i < cats.size(); i++) {
            final int ci = i;
            String lbl = (i == cat ? "▶ " : "") + cats.get(i).name();
            addDrawableChild(ButtonWidget.builder(Text.literal(lbl), b -> { cat = ci; scroll = 0; init(); }).dimensions(ox, oy + 18 + i * 22, 118, 20).build());
        }
        List<Item> items = cats.get(cat).items();
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= items.size()) break;
            Item item = items.get(idx);
            boolean action = item.request().startsWith("@");
            addDrawableChild(ButtonWidget.builder(Text.literal(action ? "Открыть" : "Создать"), b -> run(item)).dimensions(ox + 312, oy + 20 + i * RH + 4, 60, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("▲"), b -> { scroll = Math.max(0, scroll - ROWS); init(); }).dimensions(ox + 124, oy + 184, 30, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▼"), b -> { if (scroll + ROWS < cats.get(cat).items().size()) scroll += ROWS; init(); }).dimensions(ox + 156, oy + 184, 30, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> MinecraftClient.getInstance().setScreen(new NeroScreen())).dimensions(ox + 296, oy + 184, 76, 18).build());
    }

    private void run(Item item) {
        MinecraftClient mc = MinecraftClient.getInstance();
        switch (item.request()) {
            case "@main" -> mc.setScreen(new NeroScreen());
            case "@chat" -> mc.setScreen(NeroScreen.chat());
            case "@projects" -> mc.setScreen(new NeroProjectsScreen());
            case "@editor" -> mc.setScreen(new NeroEditorScreen());
            case "@cb" -> {
                if (NeroScreen.plan() == null && NeroAI.project != null) NeroScreen.replan(false);
                if (NeroScreen.plan() != null) mc.setScreen(new NeroCbScreen(NeroScreen.plan(), NeroScreen.report()));
                else mc.setScreen(new NeroScreen());
            }
            case "@settings" -> mc.setScreen(new NeroSettingsScreen());
            default -> NeroScreen.generateFromText(item.request());
        }
    }

    public boolean mouseScrolled(double mx, double my, double amount) { return wheel(amount); }

    public boolean mouseScrolled(double mx, double my, double h, double v) { return wheel(v); }

    private boolean wheel(double v) {
        int max = Math.max(0, cats.get(cat).items().size() - ROWS);
        scroll = Math.max(0, Math.min(max, scroll + (v > 0 ? -1 : 1)));
        init();
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        ctx.fill(ox - 6, oy - 4, ox + 378, oy + 206, 0xCC0B1018);
        ctx.fill(ox - 6, oy - 4, ox + 378, oy + 12, 0xFF1B2A44);
        ctx.drawTextWithShadow(textRenderer, "NERO · Каталог: всё разделено по категориям", ox, oy, 0x55FFFF);
        ctx.fill(ox + 122, oy + 14, ox + 124, oy + 180, 0x66FFFFFF);
        List<Item> items = cats.get(cat).items();
        ctx.drawTextWithShadow(textRenderer, cats.get(cat).name() + " · " + items.size(), ox + 128, oy + 16, 0xFFD055);
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= items.size()) break;
            Item item = items.get(idx);
            int y = oy + 20 + i * RH;
            ctx.fill(ox + 126, y, ox + 308, y + RH - 3, (i & 1) == 0 ? 0x44224466 : 0x44335577);
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(item.name(), 176), ox + 130, y + 3, 0xFFFFFF);
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(item.desc(), 176), ox + 130, y + 15, 0x9DB4D0);
        }
        ctx.drawTextWithShadow(textRenderer, (scroll / ROWS + 1) + "/" + Math.max(1, (items.size() + ROWS - 1) / ROWS), ox + 192, oy + 188, 0x888888);
        super.render(ctx, mx, my, d);
    }

    @Override
    public boolean shouldPause() { return false; }
}
