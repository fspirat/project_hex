package ru.fspirat.fslog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Последние {@link #MAX} сообщений чата: текст с цветами и оформлением, время и сервер.
 * Хранится только в памяти игры; на диск и в сеть уходит лишь по команде /log.
 */
public final class ChatBuffer {
    private ChatBuffer() {}

    public static final int MAX = 5000;
    private static final int MAX_CHARS = 2000;   // одно сообщение

    /** Кусок текста одного стиля. color = -1 — цвет по умолчанию; flags: 1 жирный, 2 курсив, 4 подчёркнутый, 8 зачёркнутый, 16 «мигающий». */
    public record Seg(String text, int color, int flags) {}

    /** player = сообщение игрока (подписанный чат), иначе системное (плагины сервера, команды). */
    public record Entry(long time, boolean player, String server, List<Seg> segs) {}

    private static final ArrayDeque<Entry> BUF = new ArrayDeque<>();

    static void register() {
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) -> add(message, true));
        // overlay — надпись над хотбаром (action bar), её в лог не берём
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) add(message, false);
        });
    }

    static void add(Component message, boolean player) {
        List<Seg> segs = segments(message);
        if (segs.isEmpty()) return;
        Entry e = new Entry(System.currentTimeMillis(), player, currentServer(), segs);
        synchronized (BUF) {
            BUF.addLast(e);
            while (BUF.size() > MAX) BUF.removeFirst();
        }
    }

    /** Последние n сообщений (или все, если их меньше). */
    public static List<Entry> last(int n) {
        synchronized (BUF) {
            List<Entry> all = new ArrayList<>(BUF);
            return all.subList(Math.max(0, all.size() - n), all.size());
        }
    }

    public static int size() {
        synchronized (BUF) {
            return BUF.size();
        }
    }

    public static void clear() {
        synchronized (BUF) {
            BUF.clear();
        }
    }

    private static List<Seg> segments(Component message) {
        List<Seg> out = new ArrayList<>();
        int[] chars = {0};
        message.visit((style, text) -> {
            if (text.isEmpty() || chars[0] >= MAX_CHARS) return Optional.empty();
            if (chars[0] + text.length() > MAX_CHARS) text = text.substring(0, MAX_CHARS - chars[0]);
            chars[0] += text.length();
            int color = colorOf(style), flags = flagsOf(style);
            Seg prev = out.isEmpty() ? null : out.get(out.size() - 1);
            if (prev != null && prev.color() == color && prev.flags() == flags) {
                out.set(out.size() - 1, new Seg(prev.text() + text, color, flags));
            } else {
                out.add(new Seg(text, color, flags));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static int colorOf(Style style) {
        TextColor c = style.getColor();
        return c == null ? -1 : c.getValue() & 0xFFFFFF;
    }

    private static int flagsOf(Style style) {
        return (style.isBold() ? 1 : 0) | (style.isItalic() ? 2 : 0) | (style.isUnderlined() ? 4 : 0)
                | (style.isStrikethrough() ? 8 : 0) | (style.isObfuscated() ? 16 : 0);
    }

    private static String currentServer() {
        Minecraft mc = Minecraft.getInstance();
        ServerData data = mc.getCurrentServer();
        if (data != null) return data.ip;
        return mc.hasSingleplayerServer() ? "singleplayer" : "";
    }
}
