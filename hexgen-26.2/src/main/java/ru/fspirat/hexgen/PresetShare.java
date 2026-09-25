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

/**
 * Обмен пресетами через чат: код FSTWEAK{Имя|B04DFF,FF8FE0} становится кнопкой,
 * нажатие на которую сохраняет пресет (клиентская команда /fstweak preset …, на сервер ничего не уходит).
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
                            ctx.getSource().sendFeedback(Component.literal("✦ ").append(named(p))
                                    .append(Component.literal(" — " + HexUi.tr("preset.saved_short")).withStyle(Style.EMPTY.withColor(0x55FF55))));
                            return 1;
                        })))));

        // Системные сообщения (так чат отдают многие серверные плагины) — делаем сам код кнопкой.
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : linkify(message));

        // Подписанные сообщения игроков менять нельзя — добавляем под ними строку с кнопкой.
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) -> {
            HexCore.Preset p = HexCore.findPreset(message.getString());
            if (p == null) return;
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> client.gui.hud.getChat().addClientSystemMessage(Component.literal("✦ ").append(named(p)).append(" ")
                    .append(Component.literal(HexUi.tr("preset.chat.save")).withStyle(clickStyle(p).withColor(0x55FF55)))));
        });
    }

    private static String command(HexCore.Preset p) {
        return "/fstweak preset " + p.name() + "|" + String.join(",", p.colors());
    }

    private static Style clickStyle(HexCore.Preset p) {
        return Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand(command(p)))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(HexUi.tr("preset.chat.hover", p.name()))))
                .withUnderlined(true);
    }

    /** Имя пресета, раскрашенное его же градиентом. */
    private static MutableComponent named(HexCore.Preset p) {
        MutableComponent c = Component.empty();
        for (HexCore.Glyph g : HexCore.gradientGlyphs(p.name(), List.of(p.colors()))) {
            c.append(Component.literal(g.ch()).withStyle(Style.EMPTY.withColor(g.rgb())));
        }
        return c;
    }

    private record Part(String text, Style style) {}

    /** Делает каждый код пресета в сообщении кнопкой, сохраняя остальное оформление сообщения. */
    static Component linkify(Component message) {
        String full = message.getString();
        Matcher m = HexCore.PRESET_CODE.matcher(full);
        List<int[]> ranges = new ArrayList<>();
        List<HexCore.Preset> presets = new ArrayList<>();
        while (m.find()) {
            ranges.add(new int[]{m.start(), m.end()});
            presets.add(new HexCore.Preset(m.group(1).strip(), m.group(2).toUpperCase(java.util.Locale.ROOT).split(",")));
        }
        if (ranges.isEmpty()) return message;

        List<Part> parts = new ArrayList<>();
        message.visit((style, text) -> {
            parts.add(new Part(text, style));
            return Optional.empty();
        }, Style.EMPTY);

        MutableComponent out = Component.empty();
        int pos = 0;
        for (Part part : parts) {
            String t = part.text();
            int i = 0;
            while (i < t.length()) {
                int abs = pos + i, r = -1;
                for (int k = 0; k < ranges.size(); k++) if (abs >= ranges.get(k)[0] && abs < ranges.get(k)[1]) r = k;
                // Кусок до следующей границы (начала или конца кода).
                int end = t.length();
                for (int[] range : ranges) {
                    if (range[0] > abs) end = Math.min(end, range[0] - pos);
                    if (range[1] > abs) end = Math.min(end, range[1] - pos);
                }
                Style st = r >= 0 ? clickStyle(presets.get(r)).applyTo(part.style()) : part.style();
                out.append(Component.literal(t.substring(i, end)).withStyle(st));
                i = end;
            }
            pos += t.length();
        }
        return out;
    }
}
