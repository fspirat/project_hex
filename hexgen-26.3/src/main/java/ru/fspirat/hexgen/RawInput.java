package ru.fspirat.hexgen;

import java.nio.FloatBuffer;

import org.lwjgl.sdl.SDLMouse;

/**
 * Версия для Minecraft 26.3: окно игры работает на SDL, а не на GLFW.
 * Копируется поверх общего RawInput.java скриптом sync-versions.sh.
 */
public final class RawInput {
    private RawInput() {}

    /** Зажата ли левая кнопка мыши. */
    public static boolean leftMouseDown() {
        return (SDLMouse.SDL_GetMouseState((FloatBuffer) null, (FloatBuffer) null) & SDLMouse.SDL_BUTTON_LMASK) != 0;
    }
}
