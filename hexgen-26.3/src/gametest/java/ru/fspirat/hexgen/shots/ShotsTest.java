package ru.fspirat.hexgen.shots;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.mojang.blaze3d.platform.InputConstants;

import ru.fspirat.hexgen.ColorPickerScreen;
import ru.fspirat.hexgen.FormatScreen;
import ru.fspirat.hexgen.HexConfig;
import ru.fspirat.hexgen.HexCore;
import ru.fspirat.hexgen.HexGenScreen;
import ru.fspirat.hexgen.HexState;
import ru.fspirat.hexgen.MovableButton;
import ru.fspirat.hexgen.PresetsScreen;
import ru.fspirat.hexgen.SettingsScreen;
import ru.fspirat.hexgen.SymbolsScreen;

/**
 * Снимает окна FSTWEAK для страницы мода на сайте и проверяет &k и окно /log в живой игре.
 * Запуск: gradle runClientGameTest — файлы появятся в build/run/clientGameTest/screenshots,
 * итоги проверок — строки «[checks]» в логе.
 */
public class ShotsTest implements FabricClientGameTest {
    private static final int SCALE = 3;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        // Сначала мир в маленьком окне: на виртуальном экране CI отрисовка программная и медленная.
        ctx.runOnClient(mc -> {
            mc.options.renderDistance().set(2);
            mc.options.simulationDistance().set(5);
            mc.options.framerateLimit().set(10);   // меньше кадров — больше процессора серверу мира
        });
        // Экран загрузки мира рисуется на 60 кадрах при любом лимите — крошечное окно почти не стоит процессора.
        ctx.getInput().resizeWindow(320, 240);
        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            sp.getConnection().waitForChunksRender();
            ctx.runOnClient(mc -> {
                mc.options.languageCode = "ru_ru";
                mc.getLanguageManager().setSelected("ru_ru");
            });
            ctx.runOnClient(Minecraft::reloadResourcePacks);
            ctx.waitFor(mc -> mc.gui.overlay() == null, 20 * 300);
            ctx.runOnClient(mc -> mc.options.framerateLimit().set(60));
            ctx.getInput().resizeWindow(1920, 1200);
            ctx.waitTicks(5);
            // Масштаб — только в большом окне: в крошечном окне загрузки он упирается в ×1.
            ctx.runOnClient(mc -> mc.options.guiScale().set(SCALE));
            ctx.waitTicks(20);
            sp.getServer().runCommand("gamemode survival @a");
            sp.getServer().runCommand("time set noon");
            sp.getServer().runCommand("tp @a ~ ~ ~ 40 22");
            for (String item : List.of("netherite_sword", "netherite_pickaxe", "bow", "golden_apple 16", "ender_pearl 16",
                    "diamond_helmet", "elytra", "name_tag 8", "writable_book", "torch 64", "oak_log 32", "experience_bottle 24")) {
                sp.getServer().runCommand("give @a minecraft:" + item);
            }
            ctx.waitTicks(40);
            clean(ctx);

            ctx.runOnClient(mc -> {
                HexState s = HexState.S;
                s.command = 0;
                s.font = 0;
                s.classic = false;
                s.setText("Меч дракона");
                s.stops = new ArrayList<>(List.of("7FBF3A", "D4FF9A", "FFD24D"));
                HexConfig.USER_PRESETS.clear();
                HexConfig.USER_PRESETS.add(new HexCore.Preset("Мой закат", new String[]{"FF5E62", "FF9966", "FFD24D"}));
                HexConfig.USER_PRESETS.add(new HexCore.Preset("Лёд", new String[]{"E0F7FF", "6DD5FA", "2980B9"}));
                HexConfig.USER_PRESETS.add(new HexCore.Preset("Фиалка", new String[]{"B04DFF", "FF8FE0"}));
            });

            shot(ctx, "main", () -> new HexGenScreen(null));
            shot(ctx, "palette", () -> new ColorPickerScreen(null, "7FBF3A", c -> {}));
            shot(ctx, "presets", () -> new PresetsScreen(null, c -> {}));
            shot(ctx, "symbols", () -> new SymbolsScreen(null, c -> {}));
            shot(ctx, "format", () -> new FormatScreen(null, i -> {}));
            shot(ctx, "settings", () -> new SettingsScreen(null));

            // Инвентарь с кнопкой ✦ и подсказкой при наведении.
            ctx.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
            ctx.waitTicks(5);
            int[] pos = ctx.computeOnClient(mc -> {
                for (GuiEventListener w : mc.gui.screen().children()) {
                    if (w instanceof MovableButton b) return new int[]{b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
                }
                return null;
            });
            System.out.println("[shots] inventory button at " + (pos == null ? "none" : pos[0] + "," + pos[1]));
            if (pos != null) {
                ctx.getInput().setCursorPos(pos[0] * SCALE - 2, pos[1] * SCALE - 2);
                ctx.waitTicks(2);
                ctx.getInput().moveCursor(2, 2);
            }
            ctx.waitTicks(20);
            ctx.takeScreenshot("inventory");
            ctx.getInput().setCursorPos(10, 10);
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);

            clean(ctx);
            // Чат: код пресета превращается в строку с кнопкой [+], ниже набирается /log.
            String code = HexCore.presetCode("Закат", new String[]{"FAE8F3", "FF8F5A", "B04DFF"});
            sp.getServer().runCommand("tellraw @a {\"text\":\"[Сервер] Пресет дня: " + code + "\",\"color\":\"gray\"}");
            ctx.getInput().pressKey(InputConstants.KEY_T);
            ctx.waitTicks(3);
            ctx.getInput().typeChars("Лови пресет " + HexCore.presetCode("Изумруд", new String[]{"7FBF3A", "D4FF9A"}));
            ctx.getInput().pressKey(InputConstants.KEY_RETURN);
            ctx.waitTicks(10);
            ctx.getInput().pressKey(InputConstants.KEY_T);
            ctx.waitTicks(3);
            ctx.getInput().typeChars("/log 200");
            ctx.waitTicks(10);
            ctx.takeScreenshot("chat");
            ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
            ctx.waitTicks(3);

            // Клавиша H из игры открывает генератор (в 26.3 коды клавиш другие — проверяем нажатием).
            ctx.getInput().pressKey(InputConstants.KEY_H);
            for (int i = 0; i < 40 && !ctx.computeOnClient(mc -> mc.gui.screen() instanceof HexGenScreen); i++) ctx.waitTick();
            LogViewChecks.check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof HexGenScreen), "клавиша H открывает генератор");
            ctx.setScreen(() -> null);
            ctx.waitTicks(3);

            // Проверки в живой игре: &k в генераторе и окно /log (сайт — локальная заглушка).
            ObfChecks.run(ctx);
            LogViewChecks.run(ctx, sp);
        }
        if (!LogViewChecks.fails().isEmpty()) throw new AssertionError("Не прошли проверки: " + LogViewChecks.fails());
    }

    /** Убирает всплывающие уведомления о рецептах и достижениях и их строки в чате. */
    private static void clean(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            mc.gui.toastManager().clear();
            mc.gui.hud.getChat().clearMessages(false);
        });
    }

    private static void shot(ClientGameTestContext ctx, String name, Supplier<Screen> screen) {
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
        ctx.setScreen(screen);
        ctx.waitTicks(5);
        ctx.takeScreenshot(name);
    }
}
