package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.math.BlockPos;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Внешняя нейросеть (по умолчанию OpenRouter, бесплатные модели подбираются сами; подойдёт и любой OpenAI-совместимый сервер, в том числе локальный).
 * ИИ возвращает ЧЕРТЁЖ (kind=custom + blueprint) или готовый тип. Чертёж собирается локально и проверяется NeroValidator;
 * при ошибках ИИ получает список ошибок и исправляет чертёж, при отказе модели берётся следующая бесплатная.
 */
public final class HttpProvider implements AIProvider {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final String N = "Внешний ИИ";

    private static String rigList() {
        StringBuilder b = new StringBuilder();
        for (Rig.Def d : Rig.ALL) {
            b.append("- ").append(d.key()).append(" (").append(d.name()).append("): params [");
            for (int i = 0; i < d.pn().length; i++) b.append(i > 0 ? ", " : "").append(d.pn()[i]).append(' ').append(d.mn()[i]).append('-').append(d.mx()[i]).append(" def ").append(d.df()[i]);
            b.append("]\n");
        }
        return b.toString();
    }

    private static String system() {
        return "You are NERO, the architect of a Minecraft 1.20.4 (Java, singleplayer, cheats on) building assistant. The player writes in Russian or English. "
                + "Return ONE JSON object and nothing else (no markdown). Schema: {\"kind\":\"custom\",\"name\":\"short Russian title\",\"reply\":\"short Russian message\",\"question\":null,\"blueprint\":{...}}. "
                + "Use kind=custom with a blueprint for ANY request (houses, bases, elevators, 3D printers, factories, anything). Only for trivial requests you may instead return a template: kind one of " + NeroSpec.KINDS
                + " with fields style, floors, width, depth, length, code, copies, garage_side, extras. If a current project JSON is given, modify it and return the full new blueprint.\n"
                + "BLUEPRINT = {\"ops\":[...],\"mechs\":[...],\"chains\":[...],\"setup\":[...],\"buttons\":[...],\"help\":\"text\"}.\n"
                + "Coordinates are integers relative to the origin: (0,0,0) is the corner of the build at floor level (floor blocks lie at y=-1); build toward +x,+z; max |x|,|z| 120, y -60..250.\n"
                + "ops are applied in order, later ops overwrite earlier ones (so a box of minecraft:air carves rooms/doors after walls). Op forms: "
                + "{\"op\":\"box\",\"x1\":0,\"y1\":0,\"z1\":0,\"x2\":10,\"y2\":4,\"z2\":8,\"block\":\"minecraft:stone_bricks\",\"mode\":\"solid|hollow|walls\"}, "
                + "{\"op\":\"set\",\"x\":1,\"y\":0,\"z\":1,\"block\":\"minecraft:oak_stairs[facing=north,half=bottom]\"}, "
                + "{\"op\":\"cylinder\",\"cx\":5,\"cz\":5,\"r\":4,\"y1\":0,\"y2\":6,\"block\":\"...\",\"hollow\":true}, {\"op\":\"sphere\",\"cx\":0,\"cy\":5,\"cz\":0,\"r\":6,\"block\":\"...\",\"hollow\":true}, "
                + "{\"op\":\"line\",\"x1\":0,\"y1\":0,\"z1\":0,\"x2\":9,\"y2\":5,\"z2\":0,\"block\":\"...\"}, {\"op\":\"pyramid\",\"x1\":0,\"z1\":0,\"x2\":12,\"z2\":12,\"y\":6,\"block\":\"...\"}. "
                + "Block ids must be real 1.20.4 ids; block states go in [..] (facing, half, shape, open, powered...). NEVER place command blocks, structure blocks or spawners in ops: the engine places all command blocks itself, next to the building, and builds a control wall with buttons.\n"
                + "mechs = ready, verified command-block mechanisms: {\"key\":\"lift\",\"x\":4,\"y\":0,\"z\":4,\"name\":\"Лифт\",\"params\":[floors,floor_height,speed]}. Available keys and params:\n" + rigList()
                + "A mech occupies its own area (a lift needs a free 3x3 shaft plus a 1-block frame, going up as far as floors*height); put it inside/next to your structure and keep that area free of your ops. The engine adds buttons for lift floors, vault code, start/stop etc. on the control wall automatically.\n"
                + "chains = your own command logic: {\"name\":\"...\",\"commands\":[\"cmd1\",\"cmd2\"]}. They run EVERY tick in order (first block repeating, rest chained), so guard each command with scoreboard/execute conditions. "
                + "Placeholders in all commands: {x+N} {y+N} {z+N} become absolute coordinates relative to the origin, {id} becomes a unique id for scoreboard names/tags (e.g. {id}_t). "
                + "Commands have no leading slash. Allowed roots: execute setblock fill clone tp teleport scoreboard tag effect particle playsound title tellraw summon data schedule kill damage weather time gamerule. "
                + "Use scoreboard objectives created in \"setup\" (list of one-time commands, e.g. \"scoreboard objectives add {id}_t dummy\"). A single fill/clone volume must be <= 32768 blocks. Never kill @e without filter.\n"
                + "buttons = control wall buttons: {\"label\":\"Открыть\",\"command\":\"cmd1\\ncmd2\"} (up to 4 commands separated by \\n, run once on click; use them to set scoreboard values your chains react to).\n"
                + "QUALITY: make builds rich and fully functional: real interior (rooms, doors, windows, stairs, lighting, furniture), correct roofs, working mechanisms (use mechs when a matching one exists, otherwise write chains). "
                + "Keep total ops <= 400 and total blocks <= 30000. Prefer few large box/cylinder ops over many set ops. Reply with the JSON only.";
    }

    @Override public String name() { return N; }

    @Override
    public CompletableFuture<NeroResponse> process(NeroRequest r) {
        try {
            return OpenRouter.candidates(CLIENT).thenCompose(ms -> attempt(r, ms, 0, null));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(NeroResponse.fail("Неверный адрес API", N));
        }
    }

    private static CompletableFuture<NeroResponse> done(NeroResponse r) { return CompletableFuture.completedFuture(r); }

    private CompletableFuture<NeroResponse> attempt(NeroRequest r, List<String> ms, int i, String last) {
        if (i >= ms.size()) return done(NeroResponse.fail(last == null ? "нет доступных моделей" : last, N));
        final String m = ms.get(i);
        NeroHistory.log("ИИ: пробую модель " + m);
        return call(m, r, null).thenCompose(a -> {
            if (!a.ok) return attempt(r, ms, i + 1, a.error);
            List<String> errs = check(a.spec);
            if (errs.isEmpty()) { OpenRouter.markGood(m); return done(a); }
            NeroHistory.log("ИИ: в чертеже ошибки (" + errs.size() + "), прошу исправить");
            return call(m, r, errs).thenCompose(b -> {
                if (b.ok && check(b.spec).isEmpty()) { OpenRouter.markGood(m); return done(b); }
                return attempt(r, ms, i + 1, "чертёж с ошибками: " + errs.get(0));
            });
        });
    }

    /** Пробная сборка чертежа и проверка (без мира): ошибки команд, идентификаторов, цепочек. */
    private static List<String> check(NeroSpec s) {
        if (s == null || s.bp == null) return List.of();
        try {
            NeroBuildPlan p = NeroPlanner.plan(s, new BlockPos(0, 70, 0));
            NeroValidator.Report rep = NeroValidator.validate(p, -64, 320);
            return rep.errors.size() > 8 ? rep.errors.subList(0, 8) : rep.errors;
        } catch (Exception e) {
            return List.of("чертёж не удалось собрать: " + e.getClass().getSimpleName());
        }
    }

    private CompletableFuture<NeroResponse> call(String model, NeroRequest r, List<String> errs) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("temperature", 0.3);
            body.addProperty("max_tokens", 12000);
            JsonArray msgs = new JsonArray();
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", system());
            msgs.add(sys);
            JsonObject usr = new JsonObject();
            usr.addProperty("role", "user");
            String ctx = r.current == null ? "" : "\nCurrent project: " + r.current.toJson();
            String pend = r.pending == null ? "" : "\n(The player is answering your previous clarifying question.)";
            String fix = errs == null ? "" : "\nYour previous blueprint failed validation with these errors, return the corrected full JSON:\n- " + String.join("\n- ", errs);
            usr.addProperty("content", r.text + ctx + pend + fix);
            msgs.add(usr);
            body.add("messages", msgs);

            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(NeroConfig.url))
                    .timeout(Duration.ofSeconds(NeroConfig.timeoutSec))
                    .header("Content-Type", "application/json");
            if (!NeroConfig.key.isEmpty()) b.header("Authorization", "Bearer " + NeroConfig.key);
            if (NeroConfig.isOpenRouter()) { b.header("HTTP-Referer", "https://github.com/mackros-mod"); b.header("X-Title", "Mackros NERO"); }
            HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            return CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpProvider::parse)
                    .exceptionally(e -> NeroResponse.fail("Сеть: " + (e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getClass().getSimpleName()), N));
        } catch (Exception e) {
            return done(NeroResponse.fail("Неверный адрес API", N));
        }
    }

    private static NeroResponse parse(HttpResponse<String> resp) {
        String raw = resp.body() == null ? "" : resp.body();
        if (resp.statusCode() / 100 != 2) return NeroResponse.fail("HTTP " + resp.statusCode(), N);
        if (raw.length() > NeroConfig.maxResponse) return NeroResponse.fail("Ответ слишком большой (" + raw.length() + " > " + NeroConfig.maxResponse + ")", N);
        try {
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            String content = null;
            if (root.has("choices") && root.get("choices").isJsonArray() && root.getAsJsonArray("choices").size() > 0) {
                JsonObject c0 = root.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (c0.has("message") && c0.getAsJsonObject("message").has("content") && !c0.getAsJsonObject("message").get("content").isJsonNull())
                    content = c0.getAsJsonObject("message").get("content").getAsString();
                else if (c0.has("text")) content = c0.get("text").getAsString();
            }
            for (String k : new String[]{"content", "response", "text", "output"})
                if (content == null && root.has(k) && root.get(k).isJsonPrimitive()) content = root.get(k).getAsString();
            if (content == null || content.isBlank()) return NeroResponse.fail("В ответе нет текста", N);
            int a = content.indexOf('{'), z = content.lastIndexOf('}');
            if (a < 0 || z <= a) return NeroResponse.fail("ИИ не вернул JSON", N);
            JsonObject o = JsonParser.parseString(content.substring(a, z + 1)).getAsJsonObject();
            NeroSpec spec = NeroSpec.fromJson(o);
            if (spec == null) return NeroResponse.fail("ИИ назвал неизвестный тип конструкции", N);
            String reply = o.has("reply") && !o.get("reply").isJsonNull() ? o.get("reply").getAsString() : "Проект готов: " + spec.summary();
            String q = o.has("question") && !o.get("question").isJsonNull() ? o.get("question").getAsString() : null;
            if (reply.length() > 400) reply = reply.substring(0, 400);
            if (q != null && q.length() > 300) q = q.substring(0, 300);
            if (q != null && !q.isBlank()) reply = reply + " " + q;
            return new NeroResponse(true, spec, reply, q != null && !q.isBlank() ? "ai" : null, null, N);
        } catch (Exception e) {
            return NeroResponse.fail("Не удалось разобрать ответ ИИ", N);
        }
    }
}
