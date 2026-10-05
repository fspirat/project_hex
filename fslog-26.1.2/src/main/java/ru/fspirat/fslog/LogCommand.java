package ru.fspirat.fslog;

import java.net.URI;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Клиентские команды (на сервер Minecraft не уходят):
 *   /log           — выгрузить весь накопленный чат
 *   /log 200       — выгрузить последние 200 сообщений
 *   /log clear     — очистить накопленное
 * /chatlog — то же самое (на случай, если у сервера есть своя /log).
 */
final class LogCommand {
    private LogCommand() {}

    private static final int GREEN = 0x9BE052, GRAY = 0xA0A0A0, RED = 0xFF5555, LINK = 0x55FFFF;
    private static final long COOLDOWN_MS = 15_000;
    private static volatile boolean busy;
    private static volatile long lastUpload;

    static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String name : new String[]{"log", "chatlog"}) {
                dispatcher.register(ClientCommands.literal(name)
                        .executes(ctx -> upload(ctx.getSource(), ChatBuffer.MAX))
                        .then(ClientCommands.literal("clear").executes(ctx -> {
                            ChatBuffer.clear();
                            ctx.getSource().sendFeedback(prefix().append(Component.translatable("fslog.cleared").withStyle(Style.EMPTY.withColor(GRAY))));
                            return 1;
                        }))
                        .then(ClientCommands.argument("count", IntegerArgumentType.integer(1, ChatBuffer.MAX))
                                .executes(ctx -> upload(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))));
            }
        });
    }

    private static int upload(FabricClientCommandSource source, int count) {
        List<ChatBuffer.Entry> entries = ChatBuffer.last(count);
        if (entries.isEmpty()) {
            source.sendFeedback(prefix().append(Component.translatable("fslog.empty").withStyle(Style.EMPTY.withColor(GRAY))));
            return 0;
        }
        long wait = lastUpload + COOLDOWN_MS - System.currentTimeMillis();
        if (busy || wait > 0) {
            source.sendFeedback(prefix().append(Component.translatable("fslog.wait", Math.max(1, (wait + 999) / 1000)).withStyle(Style.EMPTY.withColor(GRAY))));
            return 0;
        }
        busy = true;
        source.sendFeedback(prefix().append(Component.translatable("fslog.uploading", entries.size()).withStyle(Style.EMPTY.withColor(GRAY))));
        Uploader.upload(entries).whenComplete((result, error) -> {
            busy = false;
            Minecraft client = Minecraft.getInstance();
            Component msg;
            if (error == null && result.url() != null) {
                lastUpload = System.currentTimeMillis();
                msg = prefix().append(Component.translatable("fslog.done", entries.size()).withStyle(Style.EMPTY.withColor(GREEN)))
                        .append(" ").append(link(result.url()))
                        .append(" ").append(copy(result.url()));
            } else {
                String why = error != null ? Component.translatable("fslog.error.network").getString()
                        : Component.translatable("fslog.error." + result.error(), result.waitSec()).getString();
                msg = prefix().append(Component.translatable("fslog.failed", why).withStyle(Style.EMPTY.withColor(RED)));
            }
            client.execute(() -> client.gui.getChat().addClientSystemMessage(msg));
        });
        return 1;
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
