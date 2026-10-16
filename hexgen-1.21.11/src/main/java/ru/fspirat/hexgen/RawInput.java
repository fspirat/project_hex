package ru.fspirat.hexgen;

import org.lwjgl.glfw.GLFW;

/**
 * Состояние мыши напрямую из окна: MouseHandler.isLeftPressed() внутри экранов не обновляется.
 * Единственное место, где мод обращается к оконной библиотеке. В Minecraft 26.3 вместо GLFW — SDL,
 * поэтому для неё sync-versions.sh подставляет свою версию этого файла (hexgen-26.3/platform/RawInput.java).
 */
public final class RawInput {
    private RawInput() {}

    /** Зажата ли левая кнопка мыши. */
    public static boolean leftMouseDown() {
        long window = GLFW.glfwGetCurrentContext();
        return window != 0 && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }
}
