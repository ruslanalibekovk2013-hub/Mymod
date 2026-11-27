package com.mackros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Главное меню Mackros на правом Shift: все механизмы в одном месте, по категориям, с поиском.
 * Карточки: «Создать» (генератор), «Выдать» (оружие-предмет в инвентарь), «Открыть» (экран).
 */
public class NeroHubScreen extends Screen {
    private record Card(String name, String desc, String tag, String action) { }

    private record Cat(String name, int color, List<Card> cards) { }

    private static final int SIDE = 128, HEAD = 34, FOOT = 20, CH = 48, GAP = 6;
    private static final List<Cat> CATS = new ArrayList<>();
    private static int cat = 0, scroll = 0, sideScroll = 0;

    private TextFieldWidget search;
    private int hoverCard = -1, hoverCat = -1;
    private List<Card> view = new ArrayList<>();
    private List<String> viewCat = new ArrayList<>();

    public NeroHubScreen() { super(Text.literal("Mackros · Хаб")); }

    private static Card gen(String name, String desc, String tag, String request) { return new Card(name, desc, tag, "gen:" + request); }

    private static void build() {
        CATS.clear();
        String[][] groups = {{"Ходячие роботы", "0x55FF99"}, {"Транспорт", "0xFFAA33"}, {"Летающие", "0x55CCFF"}, {"Вращающиеся машины", "0xFF77AA"}};
        for (String[] g : groups) {
            List<Card> l = new ArrayList<>();
            for (NeroMega.V v : NeroMega.ALL) if (v.cat().equals(g[0])) l.add(gen(v.name(), v.desc(), "~100 КБ · пульт и таблички", "Создай " + v.name()));
            CATS.add(new Cat(g[0], Integer.decode(g[1]), l));
        }
        List<Card> b = new ArrayList<>();
        b.add(gen("Автобашня", "11 этажей с окнами, светом и шпилем", "~115 КБ · пульт", "Создай автобашню с пультом"));
        b.add(gen("Мост-раскладушка", "выдвигает мост по секциям, СНОС складывает", "~125 КБ · пульт", "Создай мост-раскладушку с пультом"));
        b.add(gen("Дом-принтер", "печатает дом: стены слоями, крыша, интерьер, сад", "~110 КБ · пульт", "Создай дом-принтер с пультом"));
        b.add(gen("Крепость-автосборка", "стены, 4 башни, ворота, колодец, донжон", "~125 КБ · пульт", "Создай крепость-автосборку с пультом"));
        b.add(gen("Пирамида-генератор", "15 ярусов, вход и зал с сокровищами", "~120 КБ · пульт", "Создай пирамиду-генератор с пультом"));
        for (NeroMega.V v : NeroMega.ALL) if (v.cat().equals("Автостроители")) b.add(gen(v.name(), v.desc(), "~100 КБ · пульт и таблички", "Создай " + v.name()));
        CATS.add(new Cat("Автостроители", 0xFFD055, b));
        List<Card> sh = new ArrayList<>();
        for (NeroMega.V v : NeroMega.ALL) if (v.cat().equals("Световые шоу")) sh.add(gen(v.name(), v.desc(), "~100 КБ · пульт", "Создай " + v.name()));
        CATS.add(new Cat("Световые шоу", 0xCC77FF, sh));
        List<Card> wi = new ArrayList<>();
        for (NeroWeaponItems.WI w : NeroWeaponItems.ALL) wi.add(new Card(w.name(), w.how(), "предмет · ПКМ как удочкой", "give:" + w.id()));
        CATS.add(new Cat("Оружие-предметы", 0xFF5555, wi));
        List<Card> ws = new ArrayList<>();
        for (NeroWeapons.W w : NeroWeapons.ALL) ws.add(gen(w.name(), w.how(), "установка на КБ · радиус " + w.range(), "Создай " + w.name()));
        CATS.add(new Cat("Оружие-установки", 0xFF8855, ws));
        List<Card> wr = new ArrayList<>();
        for (NeroArsenal.WR w : NeroArsenal.ALL) wr.add(gen(w.name(), w.how(), "удилище + мозг из КБ · ПКМ", "Создай " + w.name()));
        CATS.add(new Cat("Оружие-удилища (27)", 0xFFCC44, wr));
        List<Card> mc = new ArrayList<>();
        for (NeroMech.K x : NeroMech.ALL) mc.add(gen(x.name(), x.how(), "около 100 командных блоков", "Создай " + x.name()));
        CATS.add(new Cat("Механизмы на КБ (25)", 0x66DDAA, mc));
        List<Card> lf = new ArrayList<>();
        lf.add(gen("Лифт на 5 этажей", "кабина, стеклянные двери, очередь, табло", "лифт · КБ", "Создай лифт на 5 этажей"));
        lf.add(gen("Роскошный лифт на 8 этажей", "стиль Luxury: золото и чёрный камень", "лифт · КБ", "Создай роскошный лифт на 8 этажей"));
        lf.add(gen("Футуристичный лифт на 10 этажей", "белый бетон и голубое стекло", "лифт · КБ", "Создай футуристичный лифт на 10 этажей"));
        lf.add(gen("Панорамный лифт на 6 этажей", "шахта из стекла", "лифт · КБ", "Создай панорамный лифт на 6 этажей"));
        lf.add(gen("Индустриальный лифт на 6 этажей", "железо и серый бетон", "лифт · КБ", "Создай индустриальный лифт на 6 этажей"));
        lf.add(gen("Минималистичный лифт на 4 этажа", "белый и чистый", "лифт · КБ", "Создай минималистичный лифт на 4 этажа"));
        CATS.add(new Cat("Лифты", 0x66DDDD, lf));
        List<Card> dv = new ArrayList<>();
        dv.add(gen("3D-принтер", "две платформы, миниатюра из мини-блоков 1:2, 1:4, 1:8", "устройство · КБ", "Создай 3D-принтер"));
        dv.add(new Card("Portal Gun (предмет)", "ПКМ: синий портал, Shift+ПКМ: оранжевый", "предмет", "give:portal_gun"));
        dv.add(gen("Portal Gun: подставка", "модель пушки из блоков и пульт", "устройство", "Создай Portal Gun"));
        dv.add(gen("Портал", "две арки с телепортацией", "устройство · КБ", "Создай портал"));
        dv.add(gen("Компьютер", "монитор, клавиатура, заставка", "устройство · КБ", "Создай компьютер"));
        dv.add(gen("Робот", "руки поднимаются, глаза включаются", "устройство · КБ", "Создай робота"));
        dv.add(gen("Космический корабль", "корпус, двигатели, люк", "устройство · КБ", "Создай космический корабль"));
        dv.add(gen("Поезд", "пути, станция, вагонетки", "устройство · КБ", "Создай поезд"));
        dv.add(gen("Подвесной мост", "башни, тросы, фонари", "постройка", "Создай подвесной мост 40 блоков"));
        CATS.add(new Cat("Устройства", 0x88AAFF, dv));
        List<Card> bl = new ArrayList<>();
        bl.add(gen("База", "здание, лифт, ворота, сигнализация, пульт", "постройка · КБ", "Создай базу с лифтом, воротами и сигнализацией"));
        bl.add(gen("Дом с гаражом и бассейном", "двухэтажный, современный", "постройка", "Построй современный двухэтажный дом с гаражом и бассейном"));
        bl.add(gen("Фабрика", "здание, конвейер, ферма, бур", "постройка · КБ", "Создай фабрику с конвейером и фермой"));
        CATS.add(new Cat("Постройки", 0xAADD55, bl));
        List<Card> rg = new ArrayList<>();
        for (Rig.Def d : Rig.ALL) rg.add(new Card(d.name(), d.desc(), "каталог Rig · " + d.cat(), "rig:" + d.key()));
        CATS.add(new Cat("Механизмы Rig (старые)", 0xB0B8C8, rg));
        List<Card> tl = new ArrayList<>();
        tl.add(new Card("NERO: главный экран", "запрос, предпросмотр, построить", "NERO", "screen:main"));
        tl.add(new Card("NERO: вопрос", "спросить о проекте или поменять часть", "NERO", "screen:chat"));
        tl.add(new Card("Мои проекты", "версии, восстановление, экспорт и импорт", "NERO", "screen:projects"));
        tl.add(new Card("3D-редактор", "камера, выбор, перемещение, поворот", "NERO", "screen:editor"));
        tl.add(new Card("Командные блоки проекта", "список, подписи, связи", "NERO", "screen:cb"));
        tl.add(new Card("Настройки модели", "ключ OpenRouter, проверка подключения", "NERO", "screen:settings"));
        tl.add(new Card("Каталог NERO", "прежний каталог по категориям", "NERO", "screen:catalog"));
        tl.add(new Card("Старое меню Mackros", "все прежние функции: выделение, буфер, фигуры", "Mackros", "screen:old"));
        CATS.add(new Cat("Инструменты", 0xFFFFFF, tl));
    }

    @Override
    protected void init() {
        if (CATS.isEmpty()) build();
        search = new TextFieldWidget(textRenderer, width - 170, 8, 160, 16, Text.literal("поиск"));
        search.setMaxLength(40);
        search.setSuggestion("Поиск по всем механизмам…");
        search.setChangedListener(t -> { search.setSuggestion(t.isEmpty() ? "Поиск по всем механизмам…" : ""); scroll = 0; refresh(); });
        addDrawableChild(search);
        refresh();
    }

    private void refresh() {
        view.clear();
        viewCat.clear();
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) { for (Card c : CATS.get(Math.min(cat, CATS.size() - 1)).cards()) { view.add(c); viewCat.add(""); } }
        else for (Cat c : CATS) for (Card k : c.cards()) if ((k.name() + " " + k.desc() + " " + k.tag()).toLowerCase(Locale.ROOT).contains(q)) { view.add(k); viewCat.add(c.name()); }
    }

    private static int hsv(float h, float s, float v) {
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6), p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float[] c = switch (i) { case 0 -> new float[]{v, t, p}; case 1 -> new float[]{q, v, p}; case 2 -> new float[]{p, v, t}; case 3 -> new float[]{p, q, v}; case 4 -> new float[]{t, p, v}; default -> new float[]{v, p, q}; };
        return 0xFF000000 | ((int) (c[0] * 255) << 16) | ((int) (c[1] * 255) << 8) | (int) (c[2] * 255);
    }

    private int cols() { return width - SIDE - 24 >= 340 ? 2 : 1; }

    private int cardW() { int area = width - SIDE - 24; return (area - (cols() - 1) * GAP) / cols(); }

    private int rows() { return (view.size() + cols() - 1) / cols(); }

    private int contentH() { return rows() * (CH + GAP); }

    private int viewH() { return height - HEAD - FOOT - 6; }

    private void clamp() { scroll = Math.max(0, Math.min(Math.max(0, contentH() - viewH()), scroll)); }

    private static String actionLabel(String a) { return a.startsWith("gen:") ? "Создать" : a.startsWith("give:") ? "Выдать" : "Открыть"; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        long t = System.currentTimeMillis();
        ctx.fillGradient(0, 0, width, height, 0xF00A101E, 0xF01B2A48);
        for (int i = 0; i < 46; i++) {
            double sx = (i * 977 % 1000) / 1000.0 * width, sy = ((i * 613 % 1000) / 1000.0 * height + t * (0.004 + (i % 5) * 0.002)) % height;
            ctx.fill((int) sx, (int) sy, (int) sx + 1 + (i % 3 == 0 ? 1 : 0), (int) sy + 1 + (i % 3 == 0 ? 1 : 0), 0x22FFFFFF | ((i % 4) * 0x08 << 24));
        }
        ctx.fillGradient(0, 0, width, HEAD, 0xFF16264A, 0xFF0F1A33);
        ctx.fill(0, HEAD, width, HEAD + 1, hsv((t % 6000) / 6000f, 0.55f, 0.95f));
        ctx.getMatrices().push();
        ctx.getMatrices().scale(1.5f, 1.5f, 1f);
        String title = "MACKROS · ХАБ";
        int tx = 8;
        for (int i = 0; i < title.length(); i++) {
            String ch = title.substring(i, i + 1);
            ctx.drawTextWithShadow(textRenderer, ch, tx, 5, hsv((((t / 40) + i * 12) % 360) / 360f, 0.5f, 1f));
            tx += textRenderer.getWidth(ch);
        }
        ctx.getMatrices().pop();
        int total = 0;
        for (Cat c : CATS) total += c.cards().size();
        ctx.drawTextWithShadow(textRenderer, "Всё в одном меню · " + total + " позиций · правый Shift закрывает", 8, 24, 0x7F93B8);
        // боковая панель
        hoverCat = -1;
        int rh = 22, y0 = HEAD + 6;
        ctx.fill(4, HEAD + 3, SIDE + 4, height - FOOT, 0x99060B16);
        for (int i = 0; i < CATS.size(); i++) {
            int y = y0 + i * rh - sideScroll;
            if (y < HEAD + 3 || y + rh > height - FOOT) continue;
            Cat c = CATS.get(i);
            boolean sel = i == cat && (search == null || search.getText().isBlank()), hov = mx >= 4 && mx < SIDE + 4 && my >= y && my < y + rh - 2;
            if (hov) hoverCat = i;
            ctx.fill(8, y, SIDE, y + rh - 2, sel ? 0xCC22385E : hov ? 0x88203050 : 0x55101A30);
            ctx.fill(8, y, 11, y + rh - 2, 0xFF000000 | c.color());
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(c.name(), SIDE - 44), 16, y + 6, sel ? 0xFFFFFF : 0xB8C4DA);
            String n = String.valueOf(c.cards().size());
            ctx.drawTextWithShadow(textRenderer, n, SIDE - 4 - textRenderer.getWidth(n), y + 6, 0xFF000000 | c.color());
        }
        // карточки
        clamp();
        hoverCard = -1;
        int ax = SIDE + 12, cw = cardW();
        ctx.enableScissor(ax - 2, HEAD + 3, width - 4, height - FOOT);
        for (int i = 0; i < view.size(); i++) {
            int col = i % cols(), row = i / cols();
            int x = ax + col * (cw + GAP), y = HEAD + 6 + row * (CH + GAP) - scroll;
            if (y + CH < HEAD || y > height - FOOT) continue;
            Card c = view.get(i);
            Cat parent = null;
            String pn = viewCat.get(i);
            for (Cat k : CATS) if (pn.isEmpty() ? k == CATS.get(Math.min(cat, CATS.size() - 1)) : k.name().equals(pn)) parent = k;
            int acc = parent == null ? 0xFFFFFF : parent.color();
            boolean hov = mx >= x && mx < x + cw && my >= y && my < y + CH && my > HEAD && my < height - FOOT;
            if (hov) hoverCard = i;
            ctx.fillGradient(x, y, x + cw, y + CH, hov ? 0xEE1F3358 : 0xCC152238, hov ? 0xEE182848 : 0xCC0F1A2E);
            ctx.fill(x, y, x + cw, y + 2, 0xFF000000 | acc);
            ctx.fill(x, y, x + 1, y + CH, 0x66000000 | acc);
            if (hov) { ctx.fill(x + cw - 1, y, x + cw, y + CH, 0xFF000000 | acc); ctx.fill(x, y + CH - 1, x + cw, y + CH, 0xFF000000 | acc); }
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(c.name(), cw - 84), x + 6, y + 6, 0xFFFFFF);
            int ly = y + 17;
            int n = 0;
            for (OrderedText o : textRenderer.wrapLines(Text.literal(c.desc()), cw - 84)) { if (n++ >= 2) break; ctx.drawTextWithShadow(textRenderer, o, x + 6, ly, 0x9DB0CF); ly += 9; }
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth((pn.isEmpty() ? "" : pn + " · ") + c.tag(), cw - 84), x + 6, y + CH - 11, 0xFF000000 | (acc & 0xBBBBBB) | 0x404040);
            String lb = actionLabel(c.action());
            int bx = x + cw - 70, by = y + CH / 2 - 9;
            ctx.fill(bx, by, bx + 64, by + 18, hov ? 0xFF000000 | acc : 0x88000000 | (acc & 0x7F7F7F));
            ctx.drawCenteredTextWithShadow(textRenderer, lb, bx + 32, by + 5, hov ? 0xFF0A1020 : 0xFFFFFF);
        }
        ctx.disableScissor();
        if (contentH() > viewH()) {
            int bh = Math.max(16, viewH() * viewH() / contentH()), by = HEAD + 6 + (viewH() - bh) * scroll / Math.max(1, contentH() - viewH());
            ctx.fill(width - 6, HEAD + 6, width - 4, HEAD + 6 + viewH(), 0x44FFFFFF);
            ctx.fill(width - 6, by, width - 4, by + bh, 0xCCFFFFFF);
        }
        if (view.isEmpty()) ctx.drawTextWithShadow(textRenderer, "Ничего не найдено", ax, HEAD + 14, 0x9DB0CF);
        ctx.fill(0, height - FOOT, width, height, 0xFF0F1A33);
        ctx.drawTextWithShadow(textRenderer, "Клик по карточке: создать, выдать или открыть · колесо: прокрутка · Esc: закрыть", 8, height - 14, 0x6F82A6);
        super.render(ctx, mx, my, d);
    }

    private void run(Card c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String a = c.action();
        if (a.startsWith("gen:")) { NeroScreen.generateFromText(a.substring(4)); return; }
        if (a.startsWith("give:")) { give(a.substring(5)); return; }
        if (a.startsWith("rig:")) { mc.setScreen(new RigScreen(a.substring(4))); return; }
        switch (a.substring(7)) {
            case "main" -> mc.setScreen(new NeroScreen());
            case "chat" -> mc.setScreen(NeroScreen.chat());
            case "projects" -> mc.setScreen(new NeroProjectsScreen());
            case "editor" -> mc.setScreen(new NeroEditorScreen());
            case "cb" -> { if (NeroScreen.plan() == null && NeroAI.project != null) NeroScreen.replan(false); if (NeroScreen.plan() != null) mc.setScreen(new NeroCbScreen(NeroScreen.plan(), NeroScreen.report())); else mc.setScreen(new NeroScreen()); }
            case "settings" -> mc.setScreen(new NeroSettingsScreen());
            case "catalog" -> mc.setScreen(new NeroCatalogScreen());
            default -> mc.setScreen(new MackrosScreen());
        }
    }

    private void give(String id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MinecraftServer s = mc.getServer();
        if (s == null || mc.player == null) { if (mc.player != null) mc.player.sendMessage(Text.literal("Выдача предмета работает в одиночной игре"), true); return; }
        java.util.UUID u = mc.player.getUuid();
        s.execute(() -> {
            ServerPlayerEntity sp = s.getPlayerManager().getPlayer(u);
            if (sp != null) s.getCommandManager().executeWithPrefix(sp.getCommandSource().withLevel(4).withSilent(), "give @s mackros:" + id);
        });
        mc.player.sendMessage(Text.literal("Выдано в инвентарь: " + id), true);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        if (hoverCat >= 0 && mx < SIDE + 4) { cat = hoverCat; scroll = 0; search.setText(""); refresh(); return true; }
        if (hoverCard >= 0 && hoverCard < view.size()) { Card c = view.get(hoverCard); run(c); return true; }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double amount) { return wheel(mx, amount); }

    public boolean mouseScrolled(double mx, double my, double h, double v) { return wheel(mx, v); }

    private boolean wheel(double mx, double v) {
        if (mx < SIDE + 4) { sideScroll = Math.max(0, Math.min(Math.max(0, CATS.size() * 22 - (height - HEAD - FOOT - 12)), sideScroll + (v > 0 ? -22 : 22))); return true; }
        scroll += v > 0 ? -(CH + GAP) : (CH + GAP);
        clamp();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 344 && (search == null || !search.isFocused())) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
