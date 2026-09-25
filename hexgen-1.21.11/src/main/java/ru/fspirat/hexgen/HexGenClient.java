package ru.fspirat.hexgen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import ru.fspirat.hexgen.mixin.AbstractContainerScreenAccessor;
import org.lwjgl.glfw.GLFW;
import ru.fspirat.hexgen.mixin.ScreenInvoker;

public class HexGenClient implements ClientModInitializer {
    private static final int SIZE = 20;

    /** Клавиша открытия генератора из игры (по умолчанию H, меняется в «Управлении»). */
    public static KeyMapping OPEN_KEY;

    @Override
    public void onInitializeClient() {
        HexConfig.load();

        OPEN_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.fstweak.open_hexgen", GLFW.GLFW_KEY_H, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_KEY.consumeClick()) {
                if (client.player != null) client.setScreen(new HexGenScreen(null));
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen inventory)) return;
            AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) (Object) inventory;

            MovableButton button = new MovableButton(0, 0, SIZE, SIZE,
                    Component.literal("✦").withStyle(Style.EMPTY.withColor(0xB04DFF)),
                    b -> client.setScreen(new HexGenScreen(inventory)),
                    HexConfig::save);
            button.setTooltip(!HexConfig.showHints ? null : Tooltip.create(Component.literal(HexUi.TITLE + "\n")
                    .append(Component.literal(HexUi.tr("button.move_hint")).withStyle(Style.EMPTY.withColor(0xA0A0A0)))));
            place(button, acc, screen.width, screen.height);
            ((ScreenInvoker) (Object) screen).hexgen$addRenderableWidget(button);

            // Перемещение: каждый кадр кнопка следует за курсором.
            ScreenEvents.afterRender(screen).register((s, g, mouseX, mouseY, delta) -> {
                if (!button.isDragging()) return;
                button.follow(mouseX, mouseY);
                button.setPosition(Math.max(0, Math.min(s.width - SIZE, button.getX())),
                        Math.max(0, Math.min(s.height - SIZE, button.getY())));
                HexConfig.buttonOffsetX = button.getX() - acc.hexgen$getLeftPos();
                HexConfig.buttonOffsetY = button.getY() - acc.hexgen$getTopPos();
                if (!button.isDragging()) HexConfig.save();
            });

            // Книга рецептов сдвигает инвентарь — двигаем кнопку вслед за ним.
            ScreenEvents.afterTick(screen).register(s -> {
                if (!button.isDragging()) place(button, acc, s.width, s.height);
            });

            // Инвентарь закрыли посреди перемещения — сохраняем, где кнопка оказалась.
            ScreenEvents.remove(screen).register(s -> button.stop());
        });
    }

    /** Ставит кнопку по сохранённому смещению, не давая ей уйти за край экрана. */
    private static void place(MovableButton button, AbstractContainerScreenAccessor acc, int width, int height) {
        int x = acc.hexgen$getLeftPos() + HexConfig.buttonOffsetX;
        int y = acc.hexgen$getTopPos() + HexConfig.buttonOffsetY;
        button.setPosition(Math.max(0, Math.min(width - SIZE, x)), Math.max(0, Math.min(height - SIZE, y)));
    }
}
