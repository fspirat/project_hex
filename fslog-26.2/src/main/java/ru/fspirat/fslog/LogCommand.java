package ru.fspirat.fslog;

import java.net.URI;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Клиентские команды (на сервер Minecraft не уходят):
 *   /log           — открыть накопленный чат для чтения
 *   /log 200       — открыть последние 200 сообщений
 *   /log clear     — очистить накопленное
 * /chatlog — то же самое (на случай, если у сервера есть своя /log).
 * Сама команда ничего не отправляет: на сайт лог уходит только по кнопке в окне.
 */
final class LogCommand {
    private LogCommand() {}

    private static final int GREEN = 0x9BE052, GRAY = 0xA0A0A0, RED = 0xFF5555, LINK = 0x55FFFF;
    /** Окно открывается на следующем тике: сразу после команды игра сама закрывает чат. */
    private static int pendingCount;

    static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String name : new String[]{"log", "chatlog"}) {
                dispatcher.register(ClientCommands.literal(name)
                        .executes(ctx -> open(ChatBuffer.MAX))
                        .then(ClientCommands.literal("clear").executes(ctx -> {
                            ChatBuffer.clear();
                            ctx.getSource().sendFeedback(prefix().append(Component.translatable("fslog.cleared").withStyle(Style.EMPTY.withColor(GRAY))));
                            return 1;
                        }))
                        .then(ClientCommands.argument("count", IntegerArgumentType.integer(1, ChatBuffer.MAX))
                                .executes(ctx -> open(IntegerArgumentType.getInteger(ctx, "count")))));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingCount <= 0) return;
            int n = pendingCount;
            pendingCount = 0;
            client.gui.setScreen(new LogViewScreen(n));
        });
    }

    private static int open(int count) {
        pendingCount = count;
        return 1;
    }

    /** Ответ сайта пришёл, когда окно уже закрыли: ссылка или причина ошибки — сообщением в чат. */
    static void announce(LogUpload.Result r, int count) {
        Component msg;
        if (r.url() != null) {
            msg = prefix().append(Component.translatable("fslog.done", count).withStyle(Style.EMPTY.withColor(GREEN)))
                    .append(" ").append(link(r.url()))
                    .append(" ").append(copy(r.url()));
        } else {
            msg = prefix().append(Component.translatable("fslog.failed", reason(r)).withStyle(Style.EMPTY.withColor(RED)))
                    .append(" ").append(Component.translatable("fslog.view.reopen").withStyle(Style.EMPTY.withColor(GRAY)));
        }
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(msg);
    }

    /** Причина ошибки выгрузки — понятным текстом. */
    static String reason(LogUpload.Result r) {
        String code = r == null || r.error() == null ? "server" : r.error();
        return Component.translatable("fslog.error." + code, r == null ? 0 : r.waitSec()).getString();
    }

    private static MutableComponent prefix() {
        // жирный только у самой метки: дальше текст добавляется к пустому корню и не наследует её стиль
        return Component.empty().append(Component.literal("[FSLOG] ").withStyle(Style.EMPTY.withColor(GREEN).withBold(true)));
    }

    private static Component link(String url) {
        return Component.literal(url).withStyle(Style.EMPTY.withColor(LINK).withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("fslog.open"))));
    }

    private static Component copy(String url) {
        return Component.translatable("fslog.copy").withStyle(Style.EMPTY.withColor(GRAY)
                .withClickEvent(new ClickEvent.CopyToClipboard(url))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("fslog.copy.hover"))));
    }
}
