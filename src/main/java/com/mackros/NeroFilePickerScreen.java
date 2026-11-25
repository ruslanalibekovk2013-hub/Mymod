package com.mackros;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Выбор файла для вложения: свой проводник внутри игры (работает и там, где нет окна выбора файлов). */
public class NeroFilePickerScreen extends Screen {
    private static Path last = null;
    private final Screen parent;
    private Path dir;
    private final List<Path> entries = new ArrayList<>();
    private int scroll = 0, ox, oy, W, H;
    private static final int ROW = 12;

    public NeroFilePickerScreen(Screen parent) {
        super(Text.literal("NERO: вложение"));
        this.parent = parent;
        this.dir = last != null && Files.isDirectory(last) ? last : NeroAttach.dir();
        list();
    }

    private void list() {
        entries.clear();
        scroll = 0;
        try (Stream<Path> st = Files.list(dir)) {
            List<Path> all = new ArrayList<>();
            st.forEach(p -> { if (!p.getFileName().toString().startsWith(".")) all.add(p); });
            all.sort(Comparator.<Path, Boolean>comparing(p -> !Files.isDirectory(p)).thenComparing(p -> p.getFileName().toString().toLowerCase()));
            entries.addAll(all);
        } catch (Exception ignored) { }
        last = dir;
    }

    private void go(Path p) {
        if (p != null && Files.isDirectory(p)) { dir = p; list(); }
    }

    private static Path firstExisting(String... c) {
        for (String s : c) { Path p = Path.of(s); if (Files.isDirectory(p)) return p; }
        return null;
    }

    private ButtonWidget btn(String label, int x, int y, int w, Runnable r) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), bb -> r.run()).dimensions(x, y, w, 16).build();
        addDrawableChild(b);
        return b;
    }

    @Override
    protected void init() {
        W = Math.min(width - 12, 420);
        H = Math.min(height - 10, 250);
        ox = (width - W) / 2;
        oy = (height - H) / 2;
        String home = System.getProperty("user.home", "");
        int x = ox + 8, y = oy + 24;
        btn("Вверх", x, y, 50, () -> { Path p = dir.getParent(); if (p != null) go(p); }); x += 54;
        btn("Вложения", x, y, 66, () -> go(NeroAttach.dir())); x += 70;
        btn("Загрузки", x, y, 62, () -> {
            Path p = firstExisting(home + "/Downloads", home + "/Загрузки", "/storage/emulated/0/Download", "/sdcard/Download");
            if (p != null) go(p);
        }); x += 66;
        btn("Игра", x, y, 44, () -> go(FabricLoader.getInstance().getGameDir())); x += 48;
        btn("Дом", x, y, 44, () -> { if (!home.isEmpty()) go(Path.of(home)); });
        btn("Закрыть", ox + W - 68, oy + H - 22, 60, () -> MinecraftClient.getInstance().setScreen(parent));
    }

    private int rows() { return (H - 72) / ROW; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int top = oy + 46;
        if (mx >= ox + 8 && mx <= ox + W - 8 && my >= top && my < top + rows() * ROW) {
            int idx = scroll + (int) ((my - top) / ROW);
            if (idx >= 0 && idx < entries.size()) {
                Path p = entries.get(idx);
                if (Files.isDirectory(p)) go(p);
                else {
                    NeroHistory.log(NeroAttach.add(p));
                    MinecraftClient.getInstance().setScreen(parent);
                }
                return true;
            }
        }
        return false;
    }

    private void scrollBy(int d) { scroll = Math.max(0, Math.min(Math.max(0, entries.size() - rows()), scroll + d)); }

    public boolean mouseScrolled(double mx, double my, double amount) { scrollBy(amount > 0 ? -3 : 3); return true; }

    public boolean mouseScrolled(double mx, double my, double h, double v) { scrollBy(v > 0 ? -3 : 3); return true; }

    private static String size(long b) { return b > 1_000_000 ? (b / 100_000) / 10.0 + " МБ" : b > 1000 ? (b / 100) / 10.0 + " КБ" : b + " Б"; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        NeroUi.box(ctx, ox, oy, ox + W, oy + H, NeroUi.BG, NeroUi.LINE);
        ctx.fill(ox + 1, oy + 1, ox + W - 1, oy + 20, NeroUi.PANEL2);
        ctx.drawTextWithShadow(textRenderer, "Выбери файл для NERO", ox + 8, oy + 6, NeroUi.ACCENT);
        ctx.drawTextWithShadow(textRenderer, NeroAttach.items().size() + "/" + NeroAttach.MAX_FILES, ox + W - 30, oy + 6, NeroUi.DIM);
        int top = oy + 46;
        ctx.fill(ox + 6, top - 2, ox + W - 6, top + rows() * ROW + 2, 0x80000000);
        for (int i = 0; i < rows(); i++) {
            int idx = scroll + i;
            if (idx >= entries.size()) break;
            Path p = entries.get(idx);
            boolean dirP = Files.isDirectory(p);
            int y = top + i * ROW;
            if (mx >= ox + 8 && mx <= ox + W - 8 && my >= y && my < y + ROW) ctx.fill(ox + 7, y, ox + W - 7, y + ROW, 0x40FFFFFF);
            String n = p.getFileName().toString();
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth((dirP ? "[папка] " : "") + n, W - 100), ox + 10, y + 2, dirP ? NeroUi.ACCENT : NeroUi.TEXT);
            if (!dirP) {
                long sz = 0;
                try { sz = Files.size(p); } catch (Exception ignored) { }
                String s = size(sz);
                ctx.drawTextWithShadow(textRenderer, s, ox + W - 12 - textRenderer.getWidth(s), y + 2, NeroUi.DIM);
            }
        }
        if (entries.isEmpty()) ctx.drawTextWithShadow(textRenderer, "Папка пуста. Положи файлы в папку mackros_attachments в папке игры.", ox + 10, top + 4, NeroUi.DIM);
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(dir.toString(), W - 90), ox + 8, oy + H - 18, NeroUi.DIM);
        super.render(ctx, mx, my, d);
    }

    @Override
    public boolean shouldPause() { return false; }
}
