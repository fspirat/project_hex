package ru.fspirat.hexgen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;

/**
 * Счётчики использования функций мода — только «сколько раз», без текста, цветов и команд.
 * Уходят вместе с проверкой обновлений (UpdateCheck) и видны в админке сайта.
 */
public final class Usage {
    private Usage() {}

    private static final Map<String, Integer> COUNTS = new ConcurrentHashMap<>();

    public static void inc(String key) {
        COUNTS.merge(key, 1, Integer::sum);
    }

    /** Для «open(Usage.counted("presets", new …Screen(…)))»: посчитать и вернуть то же окно. */
    public static <T> T counted(String key, T screen) {
        inc(key);
        return screen;
    }

    /** Снимок накопленного для отправки. */
    static JsonObject snapshot() {
        JsonObject o = new JsonObject();
        COUNTS.forEach((k, v) -> { if (v > 0) o.addProperty(k, v); });
        return o;
    }

    /** Отправка прошла — вычитаем отправленное (то, что набежало за время запроса, останется). */
    static void sent(JsonObject snap) {
        for (String k : snap.keySet()) {
            int n = snap.get(k).getAsInt();
            COUNTS.computeIfPresent(k, (key, v) -> v - n > 0 ? v - n : null);
        }
    }
}
