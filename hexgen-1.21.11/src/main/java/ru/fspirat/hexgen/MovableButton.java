package ru.fspirat.hexgen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** Кнопка, которую можно перетаскивать с зажатым Ctrl. */
public class MovableButton extends Button {
    private boolean dragging = false;
    private int grabX;
    private int grabY;

    public MovableButton(int x, int y, int w, int h, Component message, OnPress onPress) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (input.hasControlDown()) {
            dragging = true;
            grabX = -1;
            return;
        }
        super.onPress(input);
    }

    public boolean isDragging() {
        return dragging;
    }

    public void stopDragging() {
        dragging = false;
    }

    /** Смещение курсора внутри кнопки фиксируется в первом кадре перетаскивания. */
    public void dragTo(int mouseX, int mouseY) {
        if (grabX < 0) {
            grabX = Math.max(0, Math.min(getWidth() - 1, mouseX - getX()));
            grabY = Math.max(0, Math.min(getHeight() - 1, mouseY - getY()));
        }
        setPosition(mouseX - grabX, mouseY - grabY);
    }
}
