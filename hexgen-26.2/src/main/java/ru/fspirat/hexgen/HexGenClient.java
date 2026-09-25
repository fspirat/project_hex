package ru.fspirat.hexgen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import ru.fspirat.hexgen.mixin.AbstractContainerScreenAccessor;
import ru.fspirat.hexgen.mixin.ScreenInvoker;

public class HexGenClient implements ClientModInitializer {
    private static final int SIZE = 20;

    @Override
    public void onInitializeClient() {
        HexConfig.load();

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen inventory)) return;
            AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) (Object) inventory;

            MovableButton button = new MovableButton(0, 0, SIZE, SIZE,
                    Component.literal("✦").withStyle(Style.EMPTY.withColor(0xB04DFF)),
                    b -> client.gui.setScreen(new HexGenScreen(inventory)),
                    HexConfig::save);
            button.setTooltip(Tooltip.create(Component.literal("FSHEX GENERATOR\n")
                    .append(Component.literal("Ctrl + перетащить (или Ctrl + клик) — переместить кнопку").withStyle(Style.EMPTY.withColor(0xA0A0A0)))));
            place(button, acc, screen.width, screen.height);
            ((ScreenInvoker) (Object) screen).hexgen$addRenderableWidget(button);

            // Перемещение: каждый кадр кнопка следует за курсором.
            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> {
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
