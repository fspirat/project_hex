package ru.fspirat.hexgen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Поле ввода, в котором текст можно выделять, протягивая мышью (в ванильном — только Shift + стрелки). */
public class SelectableEditBox extends EditBox {
    public SelectableEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        // Клик с Shift в ванильном поле расширяет выделение до места клика — делаем так на каждом шаге протягивания.
        MouseButtonInfo info = event.buttonInfo();
        this.onClick(new MouseButtonEvent(event.x(), event.y(),
                new MouseButtonInfo(info.button(), info.modifiers() | GLFW.GLFW_MOD_SHIFT)), false);
    }
}
