package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Внешняя нейросеть по HTTP. Формат запроса: OpenAI-совместимый /v1/chat/completions (самый распространённый).
 * Адрес, ключ и модель задаёт игрок в настройках; ничего про NERO API здесь не предполагается.
 * ИИ возвращает ТОЛЬКО структурированное описание проекта (NeroSpec); команды Minecraft всегда генерируются локально и проходят NeroValidator.
 */
public final class HttpProvider implements AIProvider {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private static String system() {
        return "You are the planner of a Minecraft building assistant. Convert the player's request (Russian or English) into ONE JSON object with fields: "
                + "kind (one of " + NeroSpec.KINDS + "), style (one of " + NeroSpec.STYLES + " or null), floors (int, 0 = default), width, depth, length (ints, 0 = default), "
                + "code (int 0-9999), copies (1-4), garage_side (left|right|back), extras (array from " + NeroSpec.EXTRAS + "), "
                + "reply (short Russian message for the player), question (short Russian clarifying question or null). "
                + "If a current project JSON is given, modify it instead of starting over. Return ONLY the JSON object. Never output Minecraft commands.";
    }

    @Override public String name() { return "Внешний ИИ"; }

    @Override
    public CompletableFuture<NeroResponse> process(NeroRequest r) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", NeroConfig.model);
            body.addProperty("temperature", 0.2);
            body.addProperty("max_tokens", 700);
            JsonArray msgs = new JsonArray();
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", system());
            msgs.add(sys);
            JsonObject usr = new JsonObject();
            usr.addProperty("role", "user");
            String ctx = r.current == null ? "" : "\nCurrent project: " + r.current.toJson();
            String pend = r.pending == null ? "" : "\n(The player is answering your previous clarifying question.)";
            usr.addProperty("content", r.text + ctx + pend);
            msgs.add(usr);
            body.add("messages", msgs);

            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(NeroConfig.url))
                    .timeout(Duration.ofSeconds(NeroConfig.timeoutSec))
                    .header("Content-Type", "application/json");
            if (!NeroConfig.key.isEmpty()) b.header("Authorization", "Bearer " + NeroConfig.key);
            HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            return CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpProvider::parse)
                    .exceptionally(e -> NeroResponse.fail("Сеть: " + (e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getClass().getSimpleName()), "Внешний ИИ"));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(NeroResponse.fail("Неверный адрес API", "Внешний ИИ"));
        }
    }

    private static NeroResponse parse(HttpResponse<String> resp) {
        String raw = resp.body() == null ? "" : resp.body();
        if (resp.statusCode() / 100 != 2) return NeroResponse.fail("HTTP " + resp.statusCode(), "Внешний ИИ");
        if (raw.length() > NeroConfig.maxResponse) return NeroResponse.fail("Ответ слишком большой (" + raw.length() + " > " + NeroConfig.maxResponse + ")", "Внешний ИИ");
        try {
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            String content = null;
            if (root.has("choices") && root.get("choices").isJsonArray() && root.getAsJsonArray("choices").size() > 0) {
                JsonObject c0 = root.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (c0.has("message") && c0.getAsJsonObject("message").has("content")) content = c0.getAsJsonObject("message").get("content").getAsString();
                else if (c0.has("text")) content = c0.get("text").getAsString();
            }
            for (String k : new String[]{"content", "response", "text", "output"})
                if (content == null && root.has(k) && root.get(k).isJsonPrimitive()) content = root.get(k).getAsString();
            if (content == null) return NeroResponse.fail("В ответе нет текста", "Внешний ИИ");
            int a = content.indexOf('{'), z = content.lastIndexOf('}');
            if (a < 0 || z <= a) return NeroResponse.fail("ИИ не вернул JSON", "Внешний ИИ");
            JsonObject o = JsonParser.parseString(content.substring(a, z + 1)).getAsJsonObject();
            NeroSpec spec = NeroSpec.fromJson(o);
            if (spec == null) return NeroResponse.fail("ИИ назвал неизвестный тип конструкции", "Внешний ИИ");
            String reply = o.has("reply") && !o.get("reply").isJsonNull() ? o.get("reply").getAsString() : "Проект готов: " + spec.summary();
            String q = o.has("question") && !o.get("question").isJsonNull() ? o.get("question").getAsString() : null;
            if (reply.length() > 400) reply = reply.substring(0, 400);
            if (q != null && q.length() > 300) q = q.substring(0, 300);
            if (q != null && !q.isBlank()) reply = reply + " " + q;
            return new NeroResponse(true, spec, reply, q != null && !q.isBlank() ? "ai" : null, null, "Внешний ИИ");
        } catch (Exception e) {
            return NeroResponse.fail("Не удалось разобрать ответ ИИ", "Внешний ИИ");
        }
    }
}
