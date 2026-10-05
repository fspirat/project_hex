package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import ru.fspirat.hexgen.fslog.ChatBuffer;
import ru.fspirat.hexgen.fslog.FsLog;

/**
 * Обмен пресетами через чат. В чат уходит короткий код ✦Имя[B04DFF,FF8FE0] (его видят все),
 * а у кого стоит FSTWEAK, код заменяется компактной строкой: «✦ Имя ■■ [+]» — имя раскрашено
 * своим градиентом, при наведении видны цвета, клик сохраняет пресет
 * (клиентская команда /fstweak preset …, на сервер ничего не уходит).
 */
public final class PresetShare {
    private PresetShare() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("fstweak").then(ClientCommands.literal("preset")
                        .then(ClientCommands.argument("data", StringArgumentType.greedyString()).executes(ctx -> {
                            HexCore.Preset p = HexCore.parsePresetBody(StringArgumentType.getString(ctx, "data"));
                            if (p == null) {
                                ctx.getSource().sendError(Component.literal(HexUi.tr("preset.bad")));
                                return 0;
                            }
                            HexConfig.addPreset(p.name(), p.colors());
                            ctx.getSource().sendFeedback(Component.literal("✦ ").withStyle(Style.EMPTY.withColor(HexCore.rgb(p.colors()[0])))
                                    .append(named(p))
                                    .append(Component.literal(" — " + HexUi.tr("preset.saved_short")).withStyle(Style.EMPTY.withColor(0x9BE052))));
                            return 1;
                        })))));

        // Системные сообщения (так чат отдают многие серверные плагины) — меняем код на месте.
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : linkify(message));

        // Подписанные сообщения игроков изменить нельзя — прячем оригинал и показываем его копию с готовой строкой.
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, time) -> {
            if (HexCore.findPreset(message.getString()) == null) return true;
            Component shown = linkify(message);
            if (FsLog.active()) ChatBuffer.add(message, true);   // в логе /log — как в оригинале
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> client.gui.hud.getChat().addClientSystemMessage(shown));
            return false;
        });
    }

    private static String command(HexCore.Preset p) {
        return "/fstweak preset " + p.name() + "|" + String.join(",", p.colors());
    }

    /** Компактная строка пресета: ✦ Имя ■■■ [+]. */
    static MutableComponent chip(HexCore.Preset p) {
        StringBuilder hover = new StringBuilder(p.name()).append('\n');
        for (String c : p.colors()) hover.append('#').append(c).append(' ');
        hover.append('\n').append(HexUi.tr("preset.chat.hover", p.name()));
        Style click = Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand(command(p)))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover.toString())));
        MutableComponent c = Component.empty().withStyle(click);
        c.append(Component.literal("✦ ").withStyle(Style.EMPTY.withColor(HexCore.rgb(p.colors()[0]))));
        c.append(named(p));
        MutableComponent dots = Component.literal(" ");
        for (String col : p.colors()) dots.append(Component.literal("■").withStyle(Style.EMPTY.withColor(HexCore.rgb(col))));
        c.append(dots);
        c.append(Component.literal(" [+]").withStyle(Style.EMPTY.withColor(0x9BE052).withBold(true)));
        return c;
    }

    /** Имя пресета, раскрашенное его же градиентом. */
    private static MutableComponent named(HexCore.Preset p) {
        MutableComponent c = Component.empty();
        for (HexCore.Glyph g : HexCore.gradientGlyphs(p.name(), List.of(p.colors()))) {
            c.append(Component.literal(g.ch()).withStyle(Style.EMPTY.withColor(g.rgb()).withBold(false).withUnderlined(false)));
        }
        return c;
    }

    private record Part(String text, Style style) {}

    /** Заменяет каждый код пресета в сообщении компактной строкой, сохраняя остальное оформление. */
    static Component linkify(Component message) {
        String full = message.getString();
        Matcher m = HexCore.PRESET_CODE.matcher(full);
        List<int[]> ranges = new ArrayList<>();
        List<HexCore.Preset> presets = new ArrayList<>();
        while (m.find()) {
            ranges.add(new int[]{m.start(), m.end()});
            presets.add(HexCore.presetOf(m));
        }
        if (ranges.isEmpty()) return message;

        List<Part> parts = new ArrayList<>();
        message.visit((style, text) -> {
            parts.add(new Part(text, style));
            return Optional.empty();
        }, Style.EMPTY);

        MutableComponent out = Component.empty();
        int pos = 0, next = 0;   // next — номер следующего ещё не вставленного кода
        for (Part part : parts) {
            String t = part.text();
            int i = 0;
            while (i < t.length()) {
                int abs = pos + i;
                int r = -1;
                for (int k = 0; k < ranges.size(); k++) if (abs >= ranges.get(k)[0] && abs < ranges.get(k)[1]) r = k;
                if (r >= 0) {
                    // внутри кода: вставляем строку пресета один раз, сам текст кода пропускаем
                    if (r >= next) {
                        out.append(chip(presets.get(r)));
                        next = r + 1;
                    }
                    i = Math.min(t.length(), ranges.get(r)[1] - pos);
                    continue;
                }
                int end = t.length();
                for (int[] range : ranges) if (range[0] > abs) end = Math.min(end, range[0] - pos);
                out.append(Component.literal(t.substring(i, end)).withStyle(part.style()));
                i = end;
            }
            pos += t.length();
        }
        return out;
    }
}
