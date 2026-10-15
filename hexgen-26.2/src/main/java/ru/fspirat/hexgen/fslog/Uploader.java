package ru.fspirat.hexgen.fslog;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

/** Отправка лога на fspirat.xyz в фоне (игра не подвисает). Вызывается только из {@link LogUpload#start()}. */
final class Uploader {
    private Uploader() {}

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    static CompletableFuture<LogUpload.Result> upload(List<ChatBuffer.Entry> entries) {
        String body = json(entries).toString();
        HttpRequest req = HttpRequest.newBuilder(URI.create(FsLog.endpoint()))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "FSLOG/" + FsLog.version("hexgen") + " (Minecraft " + FsLog.version("minecraft") + ")")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(r -> {
            // сервер логов не настроен или временно недоступен
            if (r.statusCode() == 404 || r.statusCode() == 502 || r.statusCode() == 503) return new LogUpload.Result(null, "offline", 0);
            JsonObject o;
            try {
                o = JsonParser.parseString(r.body()).getAsJsonObject();
            } catch (RuntimeException e) {
                return new LogUpload.Result(null, "server", 0);
            }
            if (r.statusCode() == 200 && o.has("url")) {
                String url = o.get("url").getAsString();
                return isWebLink(url) ? new LogUpload.Result(url, null, 0) : new LogUpload.Result(null, "server", 0);
            }
            String err = o.has("error") ? o.get("error").getAsString() : "server";
            if (!err.matches("rate|size|server|offline")) err = "server";
            return new LogUpload.Result(null, err, o.has("wait") ? o.get("wait").getAsInt() : 0);
        });
    }

    /** Ссылку показываем и открываем, только если это обычный адрес http(s). */
    private static boolean isWebLink(String url) {
        try {
            URI u = URI.create(url);
            return ("https".equals(u.getScheme()) || "http".equals(u.getScheme())) && u.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static JsonObject json(List<ChatBuffer.Entry> entries) {
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        root.addProperty("mod", FsLog.version("hexgen"));
        root.addProperty("mc", FsLog.version("minecraft"));
        root.addProperty("player", Minecraft.getInstance().getUser().getName());
        JsonArray list = new JsonArray();
        for (ChatBuffer.Entry e : entries) {
            JsonObject m = new JsonObject();
            m.addProperty("t", e.time());
            m.addProperty("p", e.player() ? 1 : 0);
            m.addProperty("srv", e.server());
            JsonArray segs = new JsonArray();
            for (ChatBuffer.Seg s : e.segs()) {
                JsonArray seg = new JsonArray();
                seg.add(s.text());
                seg.add(s.color());
                seg.add(s.flags());
                segs.add(seg);
            }
            m.add("s", segs);
            list.add(m);
        }
        root.add("messages", list);
        return root;
    }
}
