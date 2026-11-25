package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Сетевой клиент NERO Chat: настоящие запросы к настроенной модели (OpenAI-совместимый chat/completions), потоковый ответ (SSE),
 * повтор при временной ошибке, отмена. Ответы никогда не подставляются заранее: если соединения нет, возвращается ошибка.
 */
public final class NeroChatClient {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static volatile CompletableFuture<?> current = null;
    private static volatile boolean cancelled = false;

    public static boolean cancelledFlag() { return cancelled; }

    public static void clearCancel() { cancelled = false; }

    public static void cancel() {
        cancelled = true;
        CompletableFuture<?> c = current;
        if (c != null) c.cancel(true);
    }

    /** @param onDelta вызывается (в потоке сети) для каждого фрагмента ответа; @param onDone ошибка или null при успехе */
    public static void stream(JsonArray messages, Consumer<String> onDelta, Consumer<String> onDone) { stream(messages, false, onDelta, onDone); }

    public static void stream(JsonArray messages, boolean vision, Consumer<String> onDelta, Consumer<String> onDone) {
        cancelled = false;
        CompletableFuture<?> f = OpenRouter.candidates(CLIENT, vision).thenCompose(ms -> attempt(ms, 0, messages, onDelta, 0))
                .whenComplete((err, ex) -> {
                    if (cancelled) { onDone.accept("запрос отменён"); return; }
                    if (ex != null) { onDone.accept("сеть: " + (ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : ex.getClass().getSimpleName())); return; }
                    onDone.accept((String) err);
                });
        current = f;
    }

    /** Проверка подключения: короткий настоящий запрос к выбранной модели, измеряется время ответа. */
    public static void test(Consumer<String> result) {
        JsonArray msgs = new JsonArray();
        JsonObject u = new JsonObject();
        u.addProperty("role", "user");
        u.addProperty("content", "Reply with the single word OK.");
        msgs.add(u);
        long t0 = System.nanoTime();
        StringBuilder got = new StringBuilder();
        stream(msgs, got::append, err -> {
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            if (err == null && got.length() > 0) {
                NeroConfig.status("работает, ответ за " + ms + " мс");
                result.accept("Подключение работает: модель ответила за " + ms + " мс. " + NeroConfig.modelInfo());
            } else {
                if (err != null) NeroConfig.status("ошибка: " + err);
                result.accept("Проверка не пройдена: " + (err != null ? err : "пустой ответ"));
            }
        });
    }

    private static boolean retryable(String e) { return e != null && (e.startsWith("HTTP 429") || e.startsWith("HTTP 5") || e.startsWith("сеть")); }

    private static CompletableFuture<String> attempt(List<String> ms, int i, JsonArray messages, Consumer<String> onDelta, int tries) {
        if (i >= ms.size()) return CompletableFuture.completedFuture("нет доступных моделей");
        String model = ms.get(i);
        final boolean[] got = {false};
        return one(model, messages, d -> { got[0] = true; onDelta.accept(d); }).thenCompose(err -> {
            if (err == null || cancelled) { if (err == null) OpenRouter.markGood(model); return CompletableFuture.completedFuture(err); }
            if (got[0]) return CompletableFuture.completedFuture(err);
            if (retryable(err) && tries < 1)
                return CompletableFuture.supplyAsync(() -> null, CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS)).thenCompose(x -> attempt(ms, i, messages, onDelta, tries + 1));
            if (ms.size() > 1 && i + 1 < ms.size()) return attempt(ms, i + 1, messages, onDelta, 0);
            NeroConfig.status("недоступна: " + err);
            return CompletableFuture.completedFuture("модель «" + model + "» недоступна (" + err + "). Повтори, проверь подключение в «Настройки AI» или напиши «модель auto» для выбора другой");
        });
    }

    private static CompletableFuture<String> one(String model, JsonArray messages, Consumer<String> onDelta) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("temperature", 0.4);
            body.addProperty("max_tokens", 4000);
            body.addProperty("stream", true);
            body.add("messages", messages);
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(NeroConfig.url)).timeout(Duration.ofSeconds(Math.max(120, NeroConfig.timeoutSec))).header("Content-Type", "application/json");
            if (!NeroConfig.key.isEmpty()) b.header("Authorization", "Bearer " + NeroConfig.key);
            if (NeroConfig.isOpenRouter()) { b.header("HTTP-Referer", "https://github.com/mackros-mod"); b.header("X-Title", "Mackros NERO"); }
            return CLIENT.sendAsync(b.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofLines()).thenApply(resp -> {
                if (resp.statusCode() / 100 != 2) return "HTTP " + resp.statusCode();
                final String[] error = {null};
                try {
                    resp.body().forEach(line -> {
                        if (cancelled) throw new RuntimeException("cancelled");
                        if (!line.startsWith("data:")) return;
                        String d = line.substring(5).trim();
                        if (d.isEmpty() || d.equals("[DONE]")) return;
                        try {
                            JsonObject o = JsonParser.parseString(d).getAsJsonObject();
                            if (o.has("error")) { error[0] = "ошибка API"; return; }
                            JsonArray ch = o.getAsJsonArray("choices");
                            if (ch == null || ch.size() == 0) return;
                            JsonObject delta = ch.get(0).getAsJsonObject().getAsJsonObject("delta");
                            if (delta != null && delta.has("reasoning") && delta.get("reasoning").isJsonPrimitive()) NeroBrain.think(delta.get("reasoning").getAsString());
                            if (delta != null && delta.has("content") && !delta.get("content").isJsonNull()) onDelta.accept(delta.get("content").getAsString());
                        } catch (Exception ignored) { }
                    });
                } catch (RuntimeException e) {
                    return cancelled ? "запрос отменён" : "сеть";
                }
                return error[0];
            }).exceptionally(e -> cancelled ? "запрос отменён" : "сеть");
        } catch (Exception e) {
            return CompletableFuture.completedFuture("неверный адрес API");
        }
    }

    /** Системный запрос чата: контекст открытого проекта и протокол точечных правок. */
    public static String systemPrompt(NeroSpec p, NeroBuildPlan plan, NeroValidator.Report rep, String userText) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are NERO Chat, the engineering assistant inside the Mackros Minecraft mod (Java Edition 1.20.4, Fabric). You discuss the player's current project with them. ")
                .append("Answer in the player's language (Russian by default), concretely and briefly. Do not invent facts about the project that are not in the context below; if something is unknown, say so. ")
                .append("If a request is ambiguous, ask ONE concrete question.\n");
        if (p == null) sb.append("No project is open now: you can discuss ideas; to create something the player uses the Build Request field.\n");
        else {
            sb.append("CURRENT PROJECT: ").append(p.title()).append('\n');
            if (p.desc != null && !p.desc.isBlank()) sb.append("Original request: ").append(p.desc, 0, Math.min(p.desc.length(), 600)).append('\n');
            if (!p.reqs.isEmpty()) {
                sb.append("Requirements:\n");
                for (NeroRequirements.Req r : p.reqs) sb.append("- ").append(r.id).append(" [").append(r.category).append("] ").append(r.text).append(" -> ").append(r.status).append('\n');
            }
            List<String> parts = NeroPatch.parts(p);
            if (!parts.isEmpty()) sb.append("Editable parts (use exactly these names in patches): ").append(String.join(", ", parts)).append('\n');
            if (plan != null) {
                sb.append("Plan: ").append(plan.sx()).append('x').append(plan.sy()).append('x').append(plan.sz()).append(", blocks ").append(plan.solid()).append(", command blocks ").append(plan.cbs.size()).append(". Components: ");
                for (NeroBuildPlan.Element e : plan.elements) sb.append(e.name()).append("; ");
                sb.append("\nCommand block labels: ");
                int n = 0;
                for (NeroBuildPlan.Cb k : plan.cbs) { if (n++ >= 25) break; sb.append(k.label()).append("; "); }
                sb.append('\n');
            }
            sb.append("Validation: ").append(rep == null ? "not run" : rep.ok() ? "no errors" : "ERRORS: " + String.join(" | ", rep.errors.subList(0, Math.min(4, rep.errors.size())))).append('\n');
            sb.append("Timings: ").append(NeroPerf.summary()).append('\n');
            sb.append("To CHANGE the project, explain briefly in words and append EXACTLY ONE fenced block:\n```nero-patch\n{\"ops\":[{\"type\":\"replace_part\",\"part\":\"Name\",\"ops\":[ ...blueprint ops... ]}]}\n```\n")
                    .append("Patch types: replace_part (part, ops), add_part (part, ops), remove_part (part), set_button (label, command), remove_button (label), set_chain (name, commands:[{cmd,label,role}]), remove_chain (name), set_mech (key,x,y,z,params), add_setup (commands). ")
                    .append("Change ONLY what the player asked for and keep all other parts untouched (never regenerate the whole project; use the existing part names). Blueprint op forms: ")
                    .append("{\"op\":\"box\",\"x1\":0,\"y1\":0,\"z1\":0,\"x2\":3,\"y2\":3,\"z2\":3,\"block\":\"minecraft:stone\",\"mode\":\"solid|hollow|walls\"}, {\"op\":\"set\",\"x\":0,\"y\":0,\"z\":0,\"block\":\"minecraft:oak_stairs[facing=north]\"}, cylinder (cx,cz,r,y1,y2,hollow), sphere (cx,cy,cz,r,hollow), line (x1..z2), pyramid (x1,z1,x2,z2,y). ")
                    .append("Coordinates are relative to the project origin (floor level y=0, floor blocks at y=-1). Command placeholders {x+N} {y+N} {z+N} {id} {tag}. Never place command blocks in ops. If the project has no editable blueprint, say so and suggest recreating it. If you cannot do something in Minecraft 1.20.4, say so honestly.\n");
        }
        sb.append(NeroLibrary.promptSection(userText));
        return sb.toString();
    }

    private NeroChatClient() { }
}
