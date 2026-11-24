package com.mackros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Библиотека скачанных моделей и текстур (локальные копии). Правила безопасности: только https, не локальные адреса, только .json и .png,
 * не больше 2 МБ, проверка содержимого (сигнатура PNG, корректный JSON), без повторной загрузки одного файла (SHA-256).
 * Лицензия обязательна: файлы без известной лицензии сохраняются, но помечаются «нельзя использовать» и не применяются.
 * Файлы лежат в config/mackros/assets и сами в игре не подключаются: это хранилище с индексом.
 */
public final class NeroAssets {
    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX = 2 * 1024 * 1024;

    private static Path dir() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("mackros").resolve("assets");
        try { Files.createDirectories(p); } catch (Exception ignored) { }
        return p;
    }

    private static String s(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    public static synchronized List<JsonObject> index() {
        List<JsonObject> r = new ArrayList<>();
        try {
            Path f = dir().resolve("index.json");
            if (Files.exists(f)) for (JsonElement e : JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonArray()) if (e.isJsonObject()) r.add(e.getAsJsonObject());
        } catch (Exception ignored) { }
        return r;
    }

    private static synchronized void save(List<JsonObject> l) {
        try {
            JsonArray a = new JsonArray();
            for (JsonObject o : l) a.add(o);
            Files.writeString(dir().resolve("index.json"), GSON.toJson(a), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }

    private static boolean licenseKnown(String lic) {
        String l = lic == null ? "" : lic.trim().toLowerCase(Locale.ROOT);
        return !l.isEmpty() && !l.contains("неизвест") && !l.contains("unknown") && !l.equals("не указана") && !l.equals("none");
    }

    private static String hex(byte[] b) { StringBuilder sb = new StringBuilder(); for (byte x : b) sb.append(String.format("%02x", x)); return sb.toString(); }

    /** @return сообщение для пользователя */
    public static CompletableFuture<String> download(String url, String license, String author) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI u = URI.create(url.trim());
                if (!"https".equalsIgnoreCase(u.getScheme())) return "Отказ: разрешён только https.";
                InetAddress a = InetAddress.getByName(u.getHost());
                if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()) return "Отказ: адрес указывает на локальную сеть.";
                String path = u.getPath() == null ? "" : u.getPath().toLowerCase(Locale.ROOT);
                String ext = path.endsWith(".json") ? "json" : path.endsWith(".png") ? "png" : null;
                if (ext == null) return "Отказ: разрешены только файлы .json (модели) и .png (текстуры).";
                HttpResponse<InputStream> r = CLIENT.send(HttpRequest.newBuilder(u).timeout(Duration.ofSeconds(25)).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
                if (r.statusCode() / 100 != 2) return "Не удалось скачать: HTTP " + r.statusCode();
                byte[] data;
                try (InputStream in = r.body()) { data = in.readNBytes(MAX + 1); }
                if (data.length > MAX) return "Отказ: файл больше 2 МБ.";
                if (ext.equals("png") && !(data.length > 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G')) return "Отказ: файл не является PNG.";
                if (ext.equals("json")) { try { JsonParser.parseString(new String(data, StandardCharsets.UTF_8)); } catch (Exception e) { return "Отказ: файл не является корректным JSON."; } }
                String sha = hex(MessageDigest.getInstance("SHA-256").digest(data));
                List<JsonObject> idx = index();
                for (JsonObject o : idx) if (s(o, "sha256").equals(sha)) return "Этот файл уже есть в библиотеке («" + s(o, "name") + "»), повторно не скачиваю.";
                Files.write(dir().resolve(sha + "." + ext), data);
                JsonObject e = new JsonObject();
                String nm = path.substring(path.lastIndexOf('/') + 1);
                e.addProperty("id", sha.substring(0, 12));
                e.addProperty("name", nm);
                e.addProperty("url", url);
                e.addProperty("author", author == null || author.isBlank() ? "не указан" : author.trim());
                e.addProperty("license", licenseKnown(license) ? license.trim() : "не указана");
                e.addProperty("format", ext);
                e.addProperty("size", data.length);
                e.addProperty("sha256", sha);
                e.addProperty("added", LocalDate.now().toString());
                e.addProperty("usable", licenseKnown(license));
                e.add("usedIn", new JsonArray());
                idx.add(e);
                save(idx);
                return "Сохранено: " + nm + " (" + data.length + " байт, " + ext + ")" + (licenseKnown(license) ? ", лицензия: " + license.trim() + "." : ". Лицензия не указана: файл помечен «нельзя использовать» и применяться не будет.");
            } catch (Exception e) {
                return "Не удалось скачать: " + e.getClass().getSimpleName();
            }
        });
    }

    public static String list(String q) {
        List<JsonObject> l = index();
        if (l.isEmpty()) return "Библиотека скачанных файлов пуста. Команда: «загрузи модель https://... лицензия <лицензия> автор <автор>».";
        StringBuilder sb = new StringBuilder("Скачанные файлы (" + l.size() + "):");
        int n = 0;
        for (JsonObject o : l) {
            if (q != null && !q.isBlank() && !(s(o, "name") + " " + s(o, "author") + " " + s(o, "license")).toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))) continue;
            if (n++ >= 12) { sb.append("\n…"); break; }
            sb.append("\n• ").append(s(o, "name")).append(" · ").append(s(o, "format")).append(" · ").append(s(o, "size")).append(" байт · лицензия: ").append(s(o, "license"))
                    .append(o.get("usable").getAsBoolean() ? " · можно использовать" : " · НЕЛЬЗЯ использовать (лицензия неизвестна)");
        }
        return sb.toString();
    }

    private NeroAssets() { }
}
