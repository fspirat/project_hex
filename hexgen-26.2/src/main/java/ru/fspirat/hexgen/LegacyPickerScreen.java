package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Цвета /colors для обычных игроков: 16 классических цветов Minecraft или готовые наборы, как на сайте.
 * pickColor — выбор одного цвета (по нажатию на полоску цвета), pickSet — выбор набора (кнопка «Пресеты»).
 */
public class LegacyPickerScreen extends Screen {
    private static final int W = 300;
    private static final int CELL_W = 36;
    private static final int CELL_H = 20;

    private final Screen parent;
    private final Consumer<String> pickColor;
    private final Consumer<List<String>> pickSet;
    private final List<Button> colorButtons = new ArrayList<>();
    private final List<Button> setButtons = new ArrayList<>();
    private int left;
    private int top;
    private int h;

    public LegacyPickerScreen(Screen parent, Consumer<String> pickColor, Consumer<List<String>> pickSet) {
        super(Component.literal(HexUi.tr("legacy.title")));
        this.parent = parent;
        this.pickColor = pickColor;
        this.pickSet = pickSet;
    }

    @Override
    protected void init() {
        colorButtons.clear();
        setButtons.clear();
        h = pickColor != null ? 90 : 24 + 4 * 22 + 30;
        left = (this.width - W) / 2;
        top = Math.max(8, (this.height - h) / 2);

        if (pickColor != null) {
            for (int i = 0; i < 16; i++) {
                final String hex = SiteData.LEGACY_HEX[i];
                Button b = Button.builder(Component.empty(), btn -> { pickColor.accept(hex); this.onClose(); })
                        .bounds(left + (i % 8) * (CELL_W + 2), top + 18 + (i / 8) * (CELL_H + 2), CELL_W, CELL_H)
                        .tooltip(HexUi.tip("&" + SiteData.LEGACY_CODES.charAt(i) + " — " + HexUi.tr("legacy." + SiteData.LEGACY_CODES.charAt(i))))
                        .build();
                colorButtons.add(b);
                this.addRenderableWidget(b);
            }
        } else {
            for (int i = 0; i < SiteData.CLASSIC.size(); i++) {
                SiteData.Classic c = SiteData.CLASSIC.get(i);
                List<String> hexes = new ArrayList<>();
                for (char code : c.codes().toCharArray()) hexes.add(SiteData.LEGACY_HEX[SiteData.LEGACY_CODES.indexOf(code)]);
                Button b = Button.builder(Component.literal(HexUi.tr("classic." + c.key())), btn -> { pickSet.accept(hexes); this.onClose(); })
                        .bounds(left + (i % 4) * 75, top + 24 + (i / 4) * 22, 73, 20)
                        .tooltip(HexUi.tip(c.codes().chars().mapToObj(ch -> "&" + (char) ch).reduce("", (a, s) -> a + s + " ").strip()))
                        .build();
                setButtons.add(b);
                this.addRenderableWidget(b);
            }
        }
        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("back")), b -> this.onClose())
                .bounds(left + W / 2 - 50, top + h - 20, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, h);
        super.extractRenderState(g, mouseX, mouseY, delta);
        HexUi.skin(this, g, mouseX, mouseY);
        Component t = HexUi.gradientTitle(HexUi.tr(pickColor != null ? "legacy.title" : "legacy.sets"));
        g.text(this.font, t, (this.width - this.font.width(t)) / 2, top + 4, 0xFFFFFFFF, true);
        for (int i = 0; i < colorButtons.size(); i++) {
            Button b = colorButtons.get(i);
            HexUi.drawSwatch(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), HexCore.rgb(SiteData.LEGACY_HEX[i]), b.isHovered(), false);
            String code = "&" + SiteData.LEGACY_CODES.charAt(i);
            g.text(this.font, Component.literal(code), b.getX() + 3, b.getY() + b.getHeight() - 9, 0xFFFFFFFF, true);
        }
        // полоска цветов набора под каждой кнопкой
        for (int i = 0; i < setButtons.size(); i++) {
            Button b = setButtons.get(i);
            String codes = SiteData.CLASSIC.get(i).codes();
            int w = b.getWidth() - 6, n = codes.length();
            for (int k = 0; k < n; k++) {
                int x0 = b.getX() + 3 + w * k / n, x1 = b.getX() + 3 + w * (k + 1) / n;
                g.fill(x0, b.getY() + b.getHeight() - 4, x1, b.getY() + b.getHeight() - 2,
                        0xFF000000 | HexCore.rgb(SiteData.LEGACY_HEX[SiteData.LEGACY_CODES.indexOf(codes.charAt(k))]));
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
