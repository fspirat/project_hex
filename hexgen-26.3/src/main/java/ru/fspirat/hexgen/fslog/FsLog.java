package ru.fspirat.hexgen.fslog;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Логгер чата FSTWEAK (FSLOG): запоминает весь входящий чат; /log открывает его для чтения прямо в игре,
 * а кнопка «Выгрузить на сайт» сохраняет показанное на fspirat.xyz и даёт ссылку.
 * Запускается одним вызовом {@link #init()} из HexGenClient.
 */
public final class FsLog {
    private FsLog() {}

    public static final String ENDPOINT = "https://fspirat.xyz/api/chatlog.php";

    /** Куда отправлять лог. В тестах подменяется: -Dfslog.endpoint=http://127.0.0.1:…/ */
    static String endpoint() {
        String s = System.getProperty("fslog.endpoint");
        return s == null || s.isBlank() ? ENDPOINT : s;
    }

    private static boolean started;

    public static void init() {
        if (started) return;
        started = true;
        ChatBuffer.register();
        LogCommand.register();
    }

    /** Включён ли логгер (у встроенной в FSTWEAK копии — только если нет отдельного мода FSLOG). */
    public static boolean active() {
        return started;
    }

    static String version(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }
}
