package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NERO AI: одно окно-диалог. Пишешь, что построить (или задаёшь вопрос), прикрепляешь файлы, видишь, как модель думает,
 * затем «Предпросмотр» и «Построить». Всё редкое спрятано в «Ещё».
 */
public class NeroScreen extends Screen {
    private static BlockPos base = null;
    private static NeroBuildPlan plan = null;
    private static NeroValidator.Report report = null;
    private static int replaced = 0, unloaded = 0;
    private static boolean confirm = false;
    private static String draft = "";
    private static boolean askMode = false, more = false;
    private static final StringBuilder CHAT_BUF = new StringBuilder();
    private static volatile boolean chatStreaming = false;

    private TextFieldWidget field, tokenField;
    private ButtonWidget pauseBtn, modeBtn, sendBtn;
    private final List<ButtonWidget> moreBtns = new ArrayList<>();
    private final List<int[]> chips = new ArrayList<>();
    private final List<Object[]> rows = new ArrayList<>();
    private List<OrderedText> thoughtLines = new ArrayList<>();
    private int ox, oy, W, H, chatTop, scrollUp = 0, lastLog = -1, lastThought = -1, lastW = -1;
    private boolean lastChat = false;

    public NeroScreen() {
        super(Text.literal("NERO AI"));
        MinecraftClient mc = MinecraftClient.getInstance();
        if (base == null) base = (NeroStore.builtBase != null && NeroAI.project != null && NeroStore.builtSpec != null && NeroAI.project.kind.equals(NeroStore.builtSpec.kind)) ? NeroStore.builtBase : defaultBase(mc);
    }

    /** Открывает NERO сразу в режиме вопросов по проекту (бывший «NERO Chat»). */
    public static NeroScreen chat() {
        askMode = true;
        return new NeroScreen();
    }

    private static BlockPos defaultBase(MinecraftClient mc) {
        BlockPos t = Editor.look(mc, true);
        if (t != null) return t;
        if (mc.player != null) return mc.player.getBlockPos().offset(mc.player.getHorizontalFacing(), 5);
        return BlockPos.ORIGIN;
    }

    private ButtonWidget btn(String label, int x, int y, int w, int h, Runnable r) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), bb -> r.run()).dimensions(x, y, w, h).build();
        addDrawableChild(b);
        return b;
    }

    private ButtonWidget moreBtn(String label, int x, int y, int w, Runnable r) {
        ButtonWidget b = btn(label, x, y, w, 15, r);
        b.visible = more;
        moreBtns.add(b);
        return b;
    }

    private int actionY() { return oy + H - 21; }

    private int inputY() { return actionY() - 23; }

    @Override
    protected void init() {
        W = Math.min(width - 12, 420);
        H = Math.min(height - 10, 256);
        ox = (width - W) / 2;
        oy = (height - H) / 2;
        moreBtns.clear();
        boolean ready = NeroConfig.remoteReady();
        chatTop = oy + 22;
        tokenField = null;
        if (!ready) {
            tokenField = new TextFieldWidget(textRenderer, ox + 8, oy + 49, W - 112, 16, Text.literal("токен"));
            tokenField.setMaxLength(300);
            tokenField.setSuggestion("sk-or-v1-… вставь токен OpenRouter");
            tokenField.setChangedListener(t -> tokenField.setSuggestion(t.isEmpty() ? "sk-or-v1-… вставь токен OpenRouter" : ""));
            tokenField.setRenderTextProvider((s, i) -> OrderedText.styledForwardsVisitedString("*".repeat(s.length()), Style.EMPTY));
            addDrawableChild(tokenField);
            btn("Подключить", ox + W - 100, oy + 49, 92, 16, this::connect);
            chatTop = oy + 72;
        }
        btn("Настройки", ox + W - 76, oy + 3, 70, 14, () -> MinecraftClient.getInstance().setScreen(new NeroSettingsScreen()));

        int iy = inputY();
        modeBtn = btn("", ox + 8, iy, 58, 18, () -> { askMode = !askMode; refreshLabels(); });
        btn("Файл", ox + 70, iy, 40, 18, () -> MinecraftClient.getInstance().setScreen(new NeroFilePickerScreen(this)));
        field = new TextFieldWidget(textRenderer, ox + 114, iy, W - 114 - 88, 18, Text.literal("запрос"));
        field.setMaxLength(2000);
        field.setText(draft);
        field.setChangedListener(t -> { draft = t; refreshLabels(); });
        addDrawableChild(field);
        if (tokenField == null) setInitialFocus(field); else setInitialFocus(tokenField);
        sendBtn = btn("", ox + W - 80, iy, 72, 18, this::primary);

        int ay = actionY();
        btn("Построить", ox + 8, ay, 92, 16, this::build);
        btn("Предпросмотр", ox + 104, ay, 92, 16, this::preview);
        btn("Откатить", ox + 200, ay, 66, 16, () -> NeroBuilder.rollback(MinecraftClient.getInstance()));
        pauseBtn = btn("Пауза", ox + 270, ay, 66, 16, () -> { if (NeroBuilder.paused) NeroBuilder.resume(); else NeroBuilder.pause(); });
        btn("Ещё", ox + 340, ay, W - 348, 16, () -> { more = !more; for (ButtonWidget b : moreBtns) b.visible = more; });

        int my = iy - 19;
        moreBtn("Исправить", ox + 8, my, 62, this::fix);
        moreBtn("Отчёт", ox + 74, my, 44, this::reportToLog);
        moreBtn("Редактор", ox + 122, my, 56, () -> MinecraftClient.getInstance().setScreen(new NeroEditorScreen()));
        moreBtn("КБ", ox + 182, my, 30, () -> {
            if (plan == null && NeroAI.project != null) replan(false);
            if (plan == null) { NeroHistory.log("Сначала сгенерируй проект"); return; }
            MinecraftClient.getInstance().setScreen(new NeroCbScreen(plan, report));
        });
        moreBtn("Проекты", ox + 216, my, 52, () -> MinecraftClient.getInstance().setScreen(new NeroProjectsScreen()));
        moreBtn("Каталог", ox + 272, my, 50, () -> MinecraftClient.getInstance().setScreen(new NeroCatalogScreen()));
        moreBtn("Стоп стройки", ox + 326, my, W - 334, NeroBuilder::cancel);
        int py = my - 17;
        int x = ox + 8;
        x = pt("Я", x, py, 24, () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) { base = mc.player.getBlockPos().offset(mc.player.getHorizontalFacing(), 5); afterMove(); }
        });
        x = pt("X-", x, py, 24, () -> { base = base.add(-1, 0, 0); afterMove(); });
        x = pt("X+", x, py, 24, () -> { base = base.add(1, 0, 0); afterMove(); });
        x = pt("Z-", x, py, 24, () -> { base = base.add(0, 0, -1); afterMove(); });
        x = pt("Z+", x, py, 24, () -> { base = base.add(0, 0, 1); afterMove(); });
        x = pt("Y-", x, py, 24, () -> { base = base.add(0, -1, 0); afterMove(); });
        x = pt("Y+", x, py, 24, () -> { base = base.add(0, 1, 0); afterMove(); });
        moreBtn("Очистить", x + 8, py, 56, () -> {
            NeroAI.clear(); plan = null; report = null; confirm = false; NeroBuilder.clearPreview(); NeroBuilder.labelPlan = null; NeroHistory.clearLog(); NeroAttach.clear(); draft = ""; field.setText("");
        });
        moreBtn("Мышление", x + 68, py, 62, () -> { NeroConfig.deepThink = !NeroConfig.deepThink; NeroConfig.save(); NeroHistory.log("Глубокое мышление: " + (NeroConfig.deepThink ? "включено (сначала план, потом чертёж)" : "выключено (быстрее, но проще)")); });
        refreshLabels();
    }

    private int pt(String label, int x, int y, int w, Runnable r) {
        moreBtn(label, x, y, w, r);
        return x + w + 2;
    }

    private void refreshLabels() {
        if (modeBtn != null) modeBtn.setMessage(Text.literal(askMode ? "Вопрос" : "Строить"));
        if (sendBtn != null) sendBtn.setMessage(Text.literal(NeroAI.busy() || chatStreaming ? "Стоп" : (askMode ? "Спросить" : "Создать")));
        if (field != null) field.setSuggestion(field.getText().isEmpty() ? (askMode ? "Спроси про проект или попроси изменить часть" : "Что построить? Например: «лифт на 10 этажей со стеклом»") : "");
    }

    private void afterMove() {
        confirm = false;
        NeroBuilder.clearPreview();
        if (NeroAI.project != null) replan(false);
    }

    private void connect() {
        String t = tokenField == null ? "" : tokenField.getText().trim();
        if (t.isEmpty()) return;
        NeroConfig.key = t;
        NeroConfig.enabled = true;
        if (NeroConfig.url.isEmpty()) NeroConfig.url = OpenRouter.CHAT_URL;
        NeroConfig.save();
        NeroHistory.log("Токен сохранён. Проверяю подключение настоящим запросом…");
        MinecraftClient mc = MinecraftClient.getInstance();
        NeroChatClient.test(msg -> mc.execute(() -> NeroHistory.log(msg)));
        mc.setScreen(new NeroScreen());
    }

    private void primary() {
        if (NeroAI.busy() || chatStreaming) { stop(); return; }
        submit();
    }

    private void stop() {
        if (NeroAI.busy()) NeroAI.cancel();
        if (chatStreaming) NeroChatClient.cancel();
    }

    private void submit() {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            if (!askMode && NeroAI.project != null) replan(true);
            return;
        }
        field.setText("");
        draft = "";
        scrollUp = 0;
        confirm = false;
        if (askMode) ask(text); else generate(text);
    }

    private void generate(String text) {
        NeroAI.ask(text, r -> {
            if (r.ok && NeroAI.project != null && r.spec != null) { replan(true); postReport(); }
        });
    }

    private static String chatId() { return NeroAI.project != null && !NeroAI.project.projectId.isEmpty() ? NeroAI.project.projectId : "_general"; }

    /** Вопросы и точечные правки проекта: настоящий потоковый запрос к модели с контекстом проекта и вложениями. */
    private void ask(String text) {
        String id = chatId();
        NeroHistory.log("Вы: " + text);
        NeroProjects.appendChat(id, "user", text);
        if (!NeroConfig.remoteReady()) {
            NeroHistory.log("NERO: вопросы идут через модель. Вставь токен OpenRouter вверху, ответы не имитируются.");
            return;
        }
        JsonArray msgs = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", NeroChatClient.systemPrompt(NeroAI.project, plan, report, text) + NeroAttach.textBlock());
        msgs.add(sys);
        List<NeroProjects.Msg> hist = NeroProjects.chat(id);
        int from = Math.max(0, hist.size() - 20);
        for (int i = from; i < hist.size(); i++) {
            NeroProjects.Msg m = hist.get(i);
            if (!m.role().equals("user") && !m.role().equals("assistant")) continue;
            JsonObject o = new JsonObject();
            o.addProperty("role", m.role());
            JsonArray withImg = (i == hist.size() - 1 && m.role().equals("user")) ? NeroAttach.userContent(m.text()) : null;
            if (withImg != null) o.add("content", withImg); else o.addProperty("content", m.text());
            msgs.add(o);
        }
        synchronized (CHAT_BUF) { CHAT_BUF.setLength(0); }
        chatStreaming = true;
        NeroBrain.reset("Думаю над ответом");
        long t0 = System.nanoTime();
        MinecraftClient mc = MinecraftClient.getInstance();
        NeroChatClient.stream(msgs, NeroAttach.hasImages(), d -> { synchronized (CHAT_BUF) { CHAT_BUF.append(d); } }, err -> mc.execute(() -> finishChat(id, err, t0)));
    }

    private static void finishChat(String id, String err, long t0) {
        String full;
        synchronized (CHAT_BUF) { full = CHAT_BUF.toString(); }
        chatStreaming = false;
        NeroBrain.finish();
        NeroPerf.put("Чат: ответ модели", (System.nanoTime() - t0) / 1_000_000L);
        if (err != null && full.isBlank()) { NeroHistory.log("Ошибка чата: " + err); return; }
        String patch = NeroPatch.extract(full);
        String text = NeroPatch.stripPatch(full);
        if (!text.isBlank()) {
            String t = text + (err != null ? "\n(ответ оборван: " + err + ")" : "");
            NeroHistory.log("NERO: " + t);
            NeroProjects.appendChat(id, "assistant", t);
        }
        if (patch == null || NeroAI.project == null) return;
        String res = NeroEditor.apply(patch, "Чат");
        NeroProjects.appendChat(id, "system", res);
        NeroHistory.log("NERO: " + res);
    }

    private void reportToLog() {
        if (NeroAI.project == null) { NeroHistory.log("Проект не открыт"); return; }
        if (plan == null) replan(false);
        for (String l : NeroReport.make(NeroAI.project, plan, report)) NeroHistory.log("Отчёт: " + l);
    }

    public void filesDragged(List<Path> paths) { drop(paths); }

    public void onFilesDropped(List<Path> paths) { drop(paths); }

    private void drop(List<Path> paths) {
        for (Path p : paths) NeroHistory.log(NeroAttach.add(p));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            if (tokenField != null && tokenField.isFocused()) { connect(); return true; }
            if (field != null && field.isFocused()) { if (!NeroAI.busy() && !chatStreaming) submit(); return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int[] c : chips) {
            if (mx >= c[0] && mx <= c[2] && my >= c[1] && my <= c[3]) { NeroAttach.remove(c[4]); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void scrollBy(int d) { scrollUp = Math.max(0, scrollUp - d); }

    public boolean mouseScrolled(double mx, double my, double amount) { scrollBy(amount > 0 ? 3 : -3); return true; }

    public boolean mouseScrolled(double mx, double my, double h, double v) { scrollBy(v > 0 ? 3 : -3); return true; }

    private static int colorOf(String l) {
        if (l.startsWith("Вы:")) return NeroUi.USER;
        if (l.startsWith("NERO") || l.startsWith("План NERO")) return NeroUi.OK;
        if (l.startsWith("ОШИБКА") || l.startsWith("Ошибка") || l.startsWith("Постройка отклонена") || l.startsWith("Модель недоступна")) return NeroUi.ERR;
        if (l.startsWith("Внимание") || l.startsWith("Вложение")) return NeroUi.WARN;
        if (l.startsWith("Проверено") || l.startsWith("Проверка пройдена") || l.startsWith("Прикреплён") || l.startsWith("Подключение работает")) return NeroUi.OK;
        return NeroUi.DIM;
    }

    private void rebuildRows() {
        rows.clear();
        int wrap = W - 30;
        for (String l : NeroHistory.log()) {
            int c = colorOf(l);
            for (String para : l.split("\n")) {
                for (OrderedText o : textRenderer.wrapLines(Text.literal(para), wrap)) rows.add(new Object[]{o, c});
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (pauseBtn != null) {
            pauseBtn.setMessage(Text.literal(NeroBuilder.paused ? "Продолжить" : "Пауза"));
            pauseBtn.active = NeroBuilder.running;
        }
        boolean chatNow = NeroAI.busy() || chatStreaming;
        if (chatNow != lastChat) { lastChat = chatNow; refreshLabels(); }
        int n = NeroHistory.log().size();
        if (n != lastLog || lastW != W) { lastLog = n; lastW = W; rebuildRows(); }
        String th = chatStreaming ? snapshotChat() : NeroBrain.thought();
        if (chatNow && th.length() != lastThought) {
            lastThought = th.length();
            thoughtLines = new ArrayList<>(textRenderer.wrapLines(Text.literal(th.replace('\n', ' ')), W - 34));
        } else if (!chatNow && !thoughtLines.isEmpty()) { thoughtLines = new ArrayList<>(); lastThought = -1; }
    }

    private static String snapshotChat() { synchronized (CHAT_BUF) { return CHAT_BUF.toString(); } }

    /** Пересчитывает план для текущей точки и проверяет его. */
    static NeroBuildPlan plan() { return plan; }

    static NeroValidator.Report report() { return report; }

    static BlockPos base() { return base; }

    /** Открывает проект из библиотеки без повторной генерации. */
    static void loadProject(NeroSpec s) {
        NeroAI.setProject(s);
        plan = null;
        report = null;
        confirm = false;
        NeroBuilder.clearPreview();
        if (MinecraftClient.getInstance().world != null) replan(true);
    }

    /** Запуск генерации из каталога (меню): тот же путь, что и поле запроса. */
    static void generateFromText(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        NeroAI.forceLocal = true;
        NeroAI.ask(text, r -> { if (r.ok && NeroAI.project != null && r.spec != null) { replan(true); postReport(); } });
        mc.setScreen(new NeroScreen());
    }

    /** После генерации: подробный отчёт попадает в чат проекта (там же можно сразу попросить исправить). */
    private static void postReport() {
        if (plan == null || NeroAI.project == null) return;
        List<String> rep = NeroReport.make(NeroAI.project, plan, report);
        String id = NeroAI.project.projectId.isEmpty() ? "_general" : NeroAI.project.projectId;
        NeroProjects.appendChat(id, "report", String.join("\n", rep));
        int found = 0;
        for (NeroRequirements.Req q : NeroAI.project.reqs) if (!q.status.startsWith("Не найдено")) found++;
        NeroHistory.log("Отчёт готов: требований " + NeroAI.project.reqs.size() + ", найдено в плане " + found + ". Подробности: «Ещё» → «Отчёт». " + NeroPerf.summary());
    }

    static void replan(boolean verbose) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (NeroAI.project == null || mc.world == null) return;
        long tp = System.nanoTime();
        plan = NeroPlanner.plan(NeroAI.project, base);
        NeroPerf.put("Планирование и сборка плана", (System.nanoTime() - tp) / 1_000_000L);
        NeroBuilder.labelPlan = plan;
        ClientWorld w = mc.world;
        long tv = System.nanoTime();
        report = NeroValidator.validate(plan, w.getBottomY(), w.getTopY());
        NeroPerf.put("Проверка", (System.nanoTime() - tv) / 1_000_000L);
        replaced = 0;
        Set<Long> unl = new HashSet<>();
        for (NeroBuildPlan.Block b : plan.blocks) {
            if (b.st().isAir()) continue;
            if (!w.getChunkManager().isChunkLoaded(b.x() >> 4, b.z() >> 4)) { unl.add(((long) (b.x() >> 4) << 32) ^ (b.z() >> 4)); continue; }
            if (!w.getBlockState(new BlockPos(b.x(), b.y(), b.z())).isAir()) replaced++;
        }
        unloaded = unl.size();
        if (verbose) {
            NeroHistory.log("План «" + plan.title + "»: " + plan.sx() + "x" + plan.sy() + "x" + plan.sz() + ", блоков " + plan.solid() + ", КБ " + plan.cbs.size() + ", механизмов " + plan.mechanisms());
            for (String e : report.errors.subList(0, Math.min(6, report.errors.size()))) NeroHistory.log("ОШИБКА: " + e);
            for (String e : report.warnings) NeroHistory.log("Внимание: " + e);
            for (String e : report.passed) NeroHistory.log("Проверено: " + e);
            if (replaced > 0) NeroHistory.log("Будет заменено существующих блоков: " + replaced);
            if (unloaded > 0) NeroHistory.log("Не загружено чанков в области: " + unloaded + " (подойди ближе)");
            if (report.ok()) NeroHistory.log("Проверка пройдена: проект можно строить");
        }
    }

    private void preview() {
        confirm = false;
        if (NeroAI.project == null) { NeroHistory.log("Сначала сгенерируй проект"); return; }
        replan(true);
        if (plan != null) {
            NeroBuilder.showPreview(plan);
            NeroHistory.log("Область подсвечена оранжевой рамкой. Нажми F9, чтобы вернуться в NERO.");
            close();
        }
    }

    private void build() {
        if (NeroAI.project == null) { NeroHistory.log("Сначала сгенерируй проект"); return; }
        replan(true);
        if (plan == null) return;
        if (!report.ok()) {
            NeroHistory.log("Проект не прошёл проверку, пробую исправить автоматически");
            autoFix();
            if (!report.ok()) { NeroHistory.log("Постройка отклонена: проект не прошёл проверку"); confirm = false; return; }
        }
        if (unloaded > 0) { NeroHistory.log("Часть области не загружена: подойди ближе и повтори"); return; }
        NeroBuilder.Project ex = existing();
        if (ex == null && (replaced > 200 || plan.ops() > 3000) && !confirm) {
            confirm = true;
            NeroHistory.log("Большое изменение (" + plan.ops() + " операций, заменит " + replaced + " блоков). Нажми «Построить» ещё раз для подтверждения.");
            return;
        }
        confirm = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (ex != null) {
            NeroHistory.log("Проект уже построен здесь: изменяю только отличия");
            NeroBuilder.update(mc, ex, plan, NeroAI.project, false);
        } else NeroBuilder.start(mc, plan, NeroAI.project, base);
    }

    /** Построенный в этой точке проект (в этой или прошлой сессии), если есть. */
    private static NeroBuilder.Project existing() {
        NeroBuilder.Project ex = NeroBuilder.findProject(base);
        if (ex == null && NeroStore.hasBuilt(base)) ex = NeroBuilder.adopt(NeroPlanner.plan(NeroStore.builtSpec, base), NeroStore.builtSpec, base);
        return ex;
    }

    /** Исправление: чинит ошибки проекта (размеры, высота) и перестраивает повреждённые части уже построенного проекта. */
    private void fix() {
        if (NeroAI.project == null) { NeroHistory.log("Нечего исправлять: проекта нет"); return; }
        replan(true);
        if (plan == null) return;
        boolean hadErrors = !report.ok();
        if (hadErrors) autoFix();
        if (!report.ok()) { NeroHistory.log("Автоисправление не помогло. Измени описание проекта."); return; }
        MinecraftClient mc = MinecraftClient.getInstance();
        NeroBuilder.Project ex = existing();
        if (ex != null) {
            NeroHistory.log("Починка: заново ставлю все блоки и команды построенного проекта");
            NeroBuilder.update(mc, ex, plan, NeroAI.project, true);
        } else if (!hadErrors) NeroHistory.log("Проверка пройдена, ошибок нет. Построенного проекта здесь пока нет.");
    }

    /** Автоисправление по отчёту валидатора: сдвиг по высоте, уменьшение этажей/размеров, лимит лифта. */
    private void autoFix() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || NeroAI.project == null) return;
        int guard = 0;
        while (report != null && !report.ok() && guard++ < 8) {
            String all = String.join("|", report.errors);
            NeroSpec s = NeroAI.project;
            ClientWorld w = mc.world;
            if (all.contains("за высоту мира")) {
                if (plan.minY < w.getBottomY()) {
                    base = base.add(0, w.getBottomY() - plan.minY, 0);
                    NeroHistory.log("Автоисправление: точка поднята на " + (w.getBottomY() - plan.minY) + " блоков");
                } else {
                    int over = plan.maxY - (w.getTopY() - 1);
                    int cut = Math.max(1, (over + 4) / 5);
                    if (s.fl() - cut >= 1 && (s.kind.equals("house") || s.kind.equals("tower") || s.kind.equals("hotel"))) {
                        s.floors = s.fl() - cut;
                        NeroHistory.log("Автоисправление: этажей " + s.floors + " (не помещалось по высоте)");
                    } else {
                        base = base.add(0, -over, 0);
                        NeroHistory.log("Автоисправление: точка опущена на " + over + " блоков");
                    }
                }
            } else if (all.contains("Слишком большой проект")) {
                if (s.fl() > 1 && !s.kind.equals("lift")) { s.floors = s.fl() - 1; NeroHistory.log("Автоисправление: этажей " + s.floors + " (проект слишком большой)"); }
                else if (s.w > 9) { s.w -= 4; s.d = Math.max(9, s.d - 4); NeroHistory.log("Автоисправление: размер уменьшен"); }
                else if (s.len > 20) { s.len -= 20; NeroHistory.log("Автоисправление: длина уменьшена"); }
                else break;
            } else if (all.contains("Лифт") && s.fl() > 14) {
                s.floors = 14;
                NeroHistory.log("Автоисправление: лифт ограничен 14 этажами");
            } else break;
            replan(false);
        }
        if (report != null && report.ok()) NeroHistory.log("Автоисправление: проверка пройдена");
        else if (report != null) for (String e : report.errors.subList(0, Math.min(4, report.errors.size()))) NeroHistory.log("ОШИБКА: " + e);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        NeroUi.box(ctx, ox, oy, ox + W, oy + H, NeroUi.BG, NeroUi.LINE);
        ctx.fill(ox + 1, oy + 1, ox + W - 1, oy + 20, NeroUi.PANEL2);
        NeroSpec s = NeroAI.project;
        String title = "NERO AI" + (s != null ? "  ·  " + s.title() : "");
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(title, W - 250), ox + 8, oy + 6, NeroUi.ACCENT);
        boolean ready = NeroConfig.remoteReady();
        boolean busy = NeroAI.busy() || chatStreaming;
        String chip = busy ? NeroUi.spinner() + " думает" : NeroBuilder.running ? (NeroBuilder.paused ? "● пауза" : "● строю") : ready ? "● " + (NeroConfig.lockedModel.isEmpty() ? "модель: авто" : NeroConfig.lockedModel) : "● локальный режим";
        int chipCol = busy ? NeroUi.WARN : ready ? NeroUi.OK : NeroUi.ERR;
        String chipT = textRenderer.trimToWidth(chip, 150);
        ctx.drawTextWithShadow(textRenderer, chipT, ox + W - 84 - textRenderer.getWidth(chipT), oy + 6, chipCol);

        if (tokenField != null) {
            NeroUi.box(ctx, ox + 6, oy + 23, ox + W - 6, oy + 69, 0xF0261E12, 0xFF6B5320);
            int y = oy + 27;
            String warn = "Вставь токен OpenRouter, и NERO заработает как умная нейросеть: будет думать над запросом, проектировать с нуля и принимать файлы. Без токена есть только простой локальный режим по готовым шаблонам.";
            int n = 0;
            for (OrderedText l : textRenderer.wrapLines(Text.literal(warn), W - 24)) { if (n++ >= 2) break; ctx.drawTextWithShadow(textRenderer, l, ox + 10, y, NeroUi.WARN); y += 9; }
        }

        int iy = inputY();
        chips.clear();
        boolean hasChips = NeroAttach.any();
        int chatBottom = iy - 4 - (hasChips ? 14 : 0);
        int thoughtH = (NeroAI.busy() || chatStreaming) ? 62 : 0;

        ctx.fill(ox + 6, chatTop, ox + W - 6, chatBottom, 0x70000000);
        int top = chatTop + 3, bottomText = chatBottom - thoughtH - 2;
        int vis = Math.max(1, (bottomText - top) / 9);
        int total = rows.size();
        if (scrollUp > Math.max(0, total - vis)) scrollUp = Math.max(0, total - vis);
        if (total == 0 && thoughtH == 0) {
            String[] tip = {"Опиши, что построить, и NERO спроектирует это с нуля, а не из шаблона.",
                    "• «Построй автоматическую ферму с воротами и пультом»",
                    "• Прикрепи файл кнопкой «Файл» или перетащи его в окно: текст, код, json, docx, картинка",
                    "• Режим «Вопрос»: обсудить проект или поменять одну часть",
                    "• Дальше «Предпросмотр» и «Построить»"};
            int y = top + 4;
            for (int i = 0; i < tip.length; i++) { ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(tip[i], W - 24), ox + 12, y, i == 0 ? NeroUi.TEXT : NeroUi.DIM); y += 11; }
        } else {
            int start = Math.max(0, total - vis - scrollUp);
            int y = top;
            for (int i = start; i < Math.min(total, start + vis); i++) {
                Object[] r = rows.get(i);
                ctx.fill(ox + 8, y, ox + 10, y + 9, 0xFF000000 | ((Integer) r[1] & 0xFFFFFF));
                ctx.drawTextWithShadow(textRenderer, (OrderedText) r[0], ox + 14, y, (Integer) r[1]);
                y += 9;
            }
        }
        if (thoughtH > 0) {
            int ty = chatBottom - thoughtH;
            NeroUi.box(ctx, ox + 8, ty, ox + W - 8, chatBottom - 2, NeroUi.PANEL2, NeroUi.LINE);
            long sec = Math.max(0, (System.currentTimeMillis() - NeroBrain.startedAt) / 1000);
            String st = (NeroBrain.stage.isEmpty() ? "Думаю" : NeroBrain.stage) + "  " + sec + " с" + (NeroAI.progress > 0 && NeroAI.busy() ? "  ·  чертёж: " + NeroAI.progress + " симв." : "");
            ctx.drawTextWithShadow(textRenderer, NeroUi.spinner() + " " + textRenderer.trimToWidth(st, W - 40), ox + 14, ty + 4, NeroUi.ACCENT);
            int from = Math.max(0, thoughtLines.size() - 5);
            int y = ty + 15;
            for (int i = from; i < thoughtLines.size(); i++) { ctx.drawTextWithShadow(textRenderer, thoughtLines.get(i), ox + 14, y, chatStreaming ? NeroUi.OK : NeroUi.DIM); y += 9; }
        }
        if (scrollUp > 0) ctx.drawTextWithShadow(textRenderer, "↓ новее", ox + W - 50, chatBottom - thoughtH - 11, NeroUi.DIM);

        if (hasChips) {
            int x = ox + 8, y = iy - 14;
            List<NeroAttach.Item> it = NeroAttach.items();
            for (int i = 0; i < it.size(); i++) {
                String t = textRenderer.trimToWidth(it.get(i).name(), 90) + "  x";
                int w = textRenderer.getWidth(t) + 8;
                if (x + w > ox + W - 8) break;
                NeroUi.box(ctx, x, y, x + w, y + 12, NeroUi.PANEL2, it.get(i).kind().equals("image") ? NeroUi.WARN : NeroUi.ACCENT);
                ctx.drawTextWithShadow(textRenderer, t, x + 4, y + 2, NeroUi.TEXT);
                chips.add(new int[]{x, y, x + w, y + 12, i});
                x += w + 4;
            }
        }

        if (more) NeroUi.box(ctx, ox + 5, iy - 41, ox + W - 5, iy - 2, NeroUi.PANEL2, NeroUi.LINE);

        if (NeroBuilder.running || NeroBuilder.total > 0) {
            int bw = W - 12;
            ctx.fill(ox + 6, oy + H - 4, ox + 6 + bw, oy + H - 2, 0xFF333333);
            int fw = NeroBuilder.total == 0 ? 0 : (int) ((long) bw * NeroBuilder.done / NeroBuilder.total);
            ctx.fill(ox + 6, oy + H - 4, ox + 6 + fw, oy + H - 2, NeroBuilder.paused ? 0xFFFFAA00 : 0xFF39D0E0);
        }
        super.render(ctx, mx, my, d);
    }

    @Override
    public boolean shouldPause() { return false; }
}
