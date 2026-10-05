package ru.fspirat.hexgen.fslog;

import net.fabricmc.loader.api.FabricLoader;

/**
 * FSLOG — логгер чата: запоминает весь входящий чат и по команде /log выгружает его
 * на fspirat.online, а в чат приходит готовая ссылка.
 *
 * Всё лежит в пакете ru.fspirat.fslog и запускается одним вызовом {@link #init()},
 * поэтому позже его можно встроить в FSTWEAK: скопировать пакет и вызвать FsLog.init()
 * из клиентской точки входа FSTWEAK.
 */
public final class FsLog {
    private FsLog() {}

    public static final String ENDPOINT = "https://fspirat.online/api/chatlog.php";

    private static boolean started;

    public static void init() {
        if (started) return;
        started = true;
        ChatBuffer.register();
        LogCommand.register();
    }

    static String version(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }
}
