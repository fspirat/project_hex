package ru.fspirat.hexgen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import ru.fspirat.hexgen.mixin.AbstractContainerScreenAccessor;
import ru.fspirat.hexgen.mixin.ScreenInvoker;

public class HexGenClient implements ClientModInitializer {
    /**
     * Положение кнопки относительно левого верхнего угла инвентаря.
     * По умолчанию — над кнопкой брони (второй ряд над инвентарём).
     * Если кнопка перекрывает иконки других модов — поменяйте эти числа.
     */
    public static final int BUTTON_OFFSET_X = 7;
    public static final int BUTTON_OFFSET_Y = -46;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen inventory)) return;
            AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) (Object) inventory;

            Button button = Button.builder(
                            Component.literal("✦").withStyle(Style.EMPTY.withColor(0xB04DFF)),
                            b -> client.gui.setScreen(new HexGenScreen(inventory)))
                    .bounds(acc.hexgen$getLeftPos() + BUTTON_OFFSET_X, acc.hexgen$getTopPos() + BUTTON_OFFSET_Y, 20, 20)
                    .tooltip(Tooltip.create(Component.literal("HEX генератор")))
                    .build();
            ((ScreenInvoker) (Object) screen).hexgen$addRenderableWidget(button);

            // Книга рецептов сдвигает инвентарь — двигаем кнопку вслед за ним.
            ScreenEvents.afterTick(screen).register(s -> button.setPosition(
                    acc.hexgen$getLeftPos() + BUTTON_OFFSET_X, acc.hexgen$getTopPos() + BUTTON_OFFSET_Y));
        });
    }
}
