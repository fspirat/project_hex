package ru.fspirat.hexgen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Кнопка, которую можно двигать с Ctrl:
 * Ctrl + зажать и тащить — кнопка ставится там, где отпустили;
 * Ctrl + клик — кнопка следует за курсором до следующего клика.
 */
public class MovableButton extends Button.Plain {
    private final Runnable onMoved;
    private boolean dragging = false;
    private boolean moved = false;
    private boolean wasDown = false;
    private int grabX;
    private int grabY;
    private int startX;
    private int startY;

    public MovableButton(int x, int y, int w, int h, Component message, OnPress onPress, Runnable onMoved) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
        this.onMoved = onMoved;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (dragging) {
            // Клик во время перемещения ставит кнопку на место.
            stop();
            return;
        }
        if (input.hasControlDown()) {
            dragging = true;
            moved = false;
            wasDown = true;
            grabX = -1;
            startX = getX();
            startY = getY();
            return;
        }
        super.onPress(input);
    }

    public boolean isDragging() {
        return dragging;
    }

    public void stop() {
        if (!dragging) return;
        dragging = false;
        onMoved.run();
    }

    /** Вызывается каждый кадр: кнопка следует за курсором. */
    public void follow(int mouseX, int mouseY) {
        if (!dragging) return;
        if (grabX < 0) {
            grabX = Math.max(0, Math.min(getWidth() - 1, mouseX - getX()));
            grabY = Math.max(0, Math.min(getHeight() - 1, mouseY - getY()));
        }
        setPosition(mouseX - grabX, mouseY - grabY);
        if (Math.abs(getX() - startX) + Math.abs(getY() - startY) > 2) moved = true;

        // Состояние ЛКМ читаем напрямую: MouseHandler.isLeftPressed() внутри экранов не обновляется.
        long window = GLFW.glfwGetCurrentContext();
        boolean down = window != 0 && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean released = wasDown && !down;
        wasDown = down;
        if (released && moved) stop();
    }
}
