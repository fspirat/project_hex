package ru.fspirat.hexgen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import ru.fspirat.hexgen.mixin.AbstractContainerScreenAccessor;
import org.lwjgl.glfw.GLFW;
import ru.fspirat.hexgen.mixin.ScreenInvoker;

public class HexGenClient implements ClientModInitializer {
    /** Как кнопки с предметами у других модов: 18×18, иконка 16×16. */
    private static final int SIZE = 18;

    /** Клавиша открытия генератора из игры (по умолчанию H, меняется в «Управлении»). */
    public static KeyMapping OPEN_KEY;

    @Override
    public void onInitializeClient() {
        HexConfig.load();
        PresetShare.register();
        UpdateCheck.register();
        // Логгер чата /log. Если отдельно стоит мод FSLOG — работает он, встроенный не включаем.
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("fslog")) ru.fspirat.hexgen.fslog.FsLog.init();

        OPEN_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.fstweak.open_hexgen", GLFW.GLFW_KEY_H, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_KEY.consumeClick()) {
                if (client.player != null) client.gui.setScreen(new HexGenScreen(null));
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            // обычный и творческий инвентарь (у творческого — своё место кнопки)
            boolean creative = screen instanceof CreativeModeInventoryScreen;
            if (!(screen instanceof InventoryScreen) && !creative) return;
            AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) (Object) screen;

            MovableButton button = new MovableButton(0, 0, SIZE, SIZE,
                    Component.empty(),
                    b -> client.gui.setScreen(new HexGenScreen(screen)),
                    HexConfig::save);
            button.setTooltip(!HexConfig.showHints ? null : Tooltip.create(Component.literal(HexUi.TITLE + "\n")
                    .append(Component.literal(HexUi.tr("button.move_hint")).withStyle(Style.EMPTY.withColor(0xA0A0A0)))));
            // стандартную кнопку прячем (почти прозрачная), рисуем свою — как кнопки с предметами рядом
            button.setAlpha(0.03f);
            place(button, acc, screen.width, screen.height, creative);
            ((ScreenInvoker) (Object) screen).hexgen$addRenderableWidget(button);

            // Перемещение: каждый кадр кнопка следует за курсором.
            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> {
                HexUi.drawInventoryIcon(g, button.getX(), button.getY(), SIZE, button.isHovered() || button.isDragging());
                if (!button.isDragging()) return;
                button.follow(mouseX, mouseY);
                button.setPosition(Math.max(0, Math.min(s.width - SIZE, button.getX())),
                        Math.max(0, Math.min(s.height - SIZE, button.getY())));
                if (creative) {
                    HexConfig.creativeOffsetX = button.getX() - acc.hexgen$getLeftPos();
                    HexConfig.creativeOffsetY = button.getY() - acc.hexgen$getTopPos();
                } else {
                    HexConfig.buttonOffsetX = button.getX() - acc.hexgen$getLeftPos();
                    HexConfig.buttonOffsetY = button.getY() - acc.hexgen$getTopPos();
                }
                if (!button.isDragging()) HexConfig.save();
            });

            // Книга рецептов сдвигает инвентарь — двигаем кнопку вслед за ним.
            ScreenEvents.afterTick(screen).register(s -> {
                if (!button.isDragging()) place(button, acc, s.width, s.height, creative);
            });

            // Инвентарь закрыли посреди перемещения — сохраняем, где кнопка оказалась.
            ScreenEvents.remove(screen).register(s -> button.stop());
        });
    }

    /** Ставит кнопку по сохранённому смещению, не давая ей уйти за край экрана. */
    private static void place(MovableButton button, AbstractContainerScreenAccessor acc, int width, int height, boolean creative) {
        int x = acc.hexgen$getLeftPos() + (creative ? HexConfig.creativeOffsetX : HexConfig.buttonOffsetX);
        int y = acc.hexgen$getTopPos() + (creative ? HexConfig.creativeOffsetY : HexConfig.buttonOffsetY);
        button.setPosition(Math.max(0, Math.min(width - SIZE, x)), Math.max(0, Math.min(height - SIZE, y)));
    }
}
