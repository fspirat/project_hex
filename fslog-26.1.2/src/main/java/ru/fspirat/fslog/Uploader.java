package ru.fspirat.fslog;

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

/** Отправка лога на fspirat.online в фоне (игра не подвисает). */
final class Uploader {
    private Uploader() {}

    /** url — ссылка на лог; иначе error — код причины (rate, size, server), waitSec — сколько секунд подождать. */
    record Result(String url, String error, int waitSec) {}

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    static CompletableFuture<Result> upload(List<ChatBuffer.Entry> entries) {
        String body = json(entries).toString();
        HttpRequest req = HttpRequest.newBuilder(URI.create(FsLog.ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "FSLOG/" + FsLog.version("fslog") + " (Minecraft " + FsLog.version("minecraft") + ")")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(r -> {
            JsonObject o;
            try {
                o = JsonParser.parseString(r.body()).getAsJsonObject();
            } catch (RuntimeException e) {
                return new Result(null, "server", 0);
            }
            if (r.statusCode() == 200 && o.has("url")) return new Result(o.get("url").getAsString(), null, 0);
            String err = o.has("error") ? o.get("error").getAsString() : "server";
            if (!err.matches("rate|size|server")) err = "server";
            return new Result(null, err, o.has("wait") ? o.get("wait").getAsInt() : 0);
        });
    }

    private static JsonObject json(List<ChatBuffer.Entry> entries) {
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        root.addProperty("mod", FsLog.version("fslog"));
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
