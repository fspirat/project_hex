package ru.fspirat.hexgen;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

/**
 * Связь с fspirat.online: при заходе в мир и раз в 10 минут мод сообщает ник, версии и сервер
 * (список игроков во вкладке «FSTWEAK» админки) и узнаёт последнюю версию мода.
 * Если вышла новая — один раз за запуск игры пишет об этом в чат.
 */
final class UpdateCheck {
    private UpdateCheck() {}

    private static final String ENDPOINT = "https://fspirat.online/api/mod.php";
    private static final int HEARTBEAT_TICKS = 20 * 60 * 10;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static boolean notified;
    private static int ticks;

    static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ticks = 0;
            report(true);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (++ticks >= HEARTBEAT_TICKS) {
                ticks = 0;
                report(false);
            }
        });
    }

    private static String version(String id) {
        return FabricLoader.getInstance().getModContainer(id).map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
    }

    private static void report(boolean join) {
        Minecraft mc = Minecraft.getInstance();
        ServerData data = mc.getCurrentServer();
        String mine = version("hexgen");
        JsonObject o = new JsonObject();
        o.addProperty("nick", mc.getUser().getName());
        o.addProperty("mod", mine);
        o.addProperty("mc", version("minecraft"));
        o.addProperty("server", data != null ? data.ip : mc.hasSingleplayerServer() ? "singleplayer" : "");
        o.addProperty("lang", mc.options.languageCode);
        o.addProperty("join", join ? 1 : 0);
        HttpRequest req = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "FSTWEAK/" + mine + " (Minecraft " + version("minecraft") + ")")
                .POST(HttpRequest.BodyPublishers.ofString(o.toString(), StandardCharsets.UTF_8))
                .build();
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenAccept(r -> {
            if (r.statusCode() != 200 || notified) return;
            try {
                JsonObject a = JsonParser.parseString(r.body()).getAsJsonObject();
                String latest = a.has("version") ? a.get("version").getAsString() : "";
                if (latest.isEmpty() || compare(latest, mine) <= 0) return;
                notified = true;
                String url = a.has("url") ? a.get("url").getAsString() : "https://fspirat.ru/fstweak/";
                String notes = a.has("notes") ? a.get("notes").getAsString() : "";
                mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(message(latest, mine, url, notes)));
            } catch (RuntimeException ignored) {
                // ответ не разобрать — просто молчим
            }
        }).exceptionally(e -> null);
    }

    private static Component message(String latest, String mine, String url, String notes) {
        Style link = Style.EMPTY.withColor(0x55FFFF).withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(url)));
        Component whatsNew = Component.literal(HexUi.tr("update.notes")).withStyle(Style.EMPTY.withColor(0xA0A0A0)
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(notes.isEmpty() ? "—" : notes))));
        return Component.empty()
                .append(Component.literal("[FSTWEAK] ").withStyle(Style.EMPTY.withColor(0x9BE052).withBold(true)))
                .append(Component.literal(HexUi.tr("update.available", latest, mine) + " ").withStyle(Style.EMPTY.withColor(0xE8EEE6)))
                .append(Component.literal("[" + HexUi.tr("update.download") + "]").withStyle(link))
                .append(Component.literal(" · "))
                .append(whatsNew);
    }

    /** Сравнение версий вида 1.2 / 1.10.1: >0 — a новее b. */
    static int compare(String a, String b) {
        String[] x = a.split("[.+-]"), y = b.split("[.+-]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int d = num(i < x.length ? x[i] : "0") - num(i < y.length ? y[i] : "0");
            if (d != 0) return d;
        }
        return 0;
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
