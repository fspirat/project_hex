package ru.fspirat.hexgen;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Ввод имени для своего пресета. */
public class NamePresetScreen extends Screen {
    private static final int W = 240;
    private static final int H = 96;
    private static final int MAX_NAME = 24;

    private final Screen parent;
    private final String[] colors;
    private final Consumer<String> saved;
    private String name;
    private EditBox nameBox;
    private Button saveButton;
    private int left;
    private int top;

    public NamePresetScreen(Screen parent, String[] colors, Consumer<String> saved) {
        super(Component.literal("Имя пресета"));
        this.parent = parent;
        this.colors = colors;
        this.saved = saved;
        this.name = HexConfig.nextPresetName();
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = Math.max(8, (this.height - H) / 2);

        nameBox = new SelectableEditBox(this.font, left, top + 30, W, 18, Component.literal("Имя"));
        nameBox.setMaxLength(MAX_NAME);
        nameBox.setValue(name);
        nameBox.setResponder(v -> {
            name = v;
            updateSaveButton();
        });
        this.addRenderableWidget(nameBox);
        this.setInitialFocus(nameBox);
        nameBox.setCursorPosition(name.length());
        nameBox.setHighlightPos(0);

        int bw = (W - 4) / 2;
        saveButton = Button.builder(Component.literal("Сохранить"), b -> save())
                .bounds(left, top + H - 20, bw, 20).build();
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> this.onClose())
                .bounds(left + bw + 4, top + H - 20, bw, 20).build());
        updateSaveButton();
    }

    private void updateSaveButton() {
        if (saveButton == null) return;
        String n = name.strip();
        saveButton.active = !n.isEmpty();
        saveButton.setMessage(Component.literal(HexConfig.hasPreset(n) ? "Заменить" : "Сохранить"));
    }

    private void save() {
        String n = name.strip();
        if (n.isEmpty()) return;
        HexConfig.addPreset(n, colors);
        this.onClose();
        saved.accept(n);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, H);
        super.render(g, mouseX, mouseY, delta);

        Component t = HexUi.gradientTitle("Сохранить пресет");
        g.drawString(this.font, t, (this.width - this.font.width(t)) / 2, top + 1, 0xFFFFFFFF, true);
        g.drawString(this.font, Component.literal("Название:"), left, top + 19, 0xFFA0A0A0, false);
        HexUi.drawGradient(g, left, top + 54, W, 6, List.of(colors));
        if (HexConfig.hasPreset(name.strip())) {
            g.drawString(this.font, Component.literal("Пресет с таким именем будет заменён"), left, top + 64, 0xFFFFD24D, false);
        } else if (HexConfig.USER_PRESETS.size() >= HexConfig.MAX_PRESETS) {
            g.drawString(this.font, Component.literal("Лимит " + HexConfig.MAX_PRESETS + " — самый старый пресет удалится"),
                    left, top + 64, 0xFFFFD24D, false);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
