package ru.fspirat.hexgen.shots;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;

import ru.fspirat.hexgen.HexCore;
import ru.fspirat.hexgen.HexGenScreen;
import ru.fspirat.hexgen.HexState;

/**
 * &k в генераторе в настоящей игре: ввод с клавиатуры, вставка, отмена/повтор, кнопка «K»
 * и два снимка предпросмотра подряд (символы «бегут» — кадры разные). Системный буфер обмена не трогается.
 */
final class ObfChecks {
    private ObfChecks() {}

    private static final String START = "Цена: ";
    private static final String TYPED = START + "&k12345&r монет";

    static void run(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            HexState.S.command = 0;
            HexState.S.font = 0;
            HexState.S.classic = false;
            HexState.S.setText(START);
            HexState.S.stops = new ArrayList<>(List.of("FFD24D", "FF6A00"));
        });
        ctx.setScreen(() -> new HexGenScreen(null));
        ctx.waitTicks(5);
        if (!focusTextBox(ctx)) {
            LogViewChecks.check(false, "поле текста генератора найдено");
            return;
        }
        // Набор с клавиатуры, как у игрока.
        ctx.getInput().typeChars("&k12345&r монет");
        ctx.waitTicks(3);
        String text = ctx.computeOnClient(mc -> HexState.S.text);
        LogViewChecks.check(TYPED.equals(text), "набранный текст с кодами сохранён как есть («" + text + "»)");
        String visible = ctx.computeOnClient(mc -> HexState.S.visibleText());
        LogViewChecks.check("Цена: 12345 монет".equals(visible), "&k и &r не видны и не считаются символами («" + visible + "»)");
        LogViewChecks.check(ctx.computeOnClient(mc -> obfRange(HexState.S.effectiveMask())).equals("6-10"),
                "«бегут» только 12345 (позиции 6–10)");
        ctx.getInput().setCursorPos(5, 5);
        ctx.waitTicks(10);
        ctx.takeScreenshot("obf-preview-1");
        ctx.waitTicks(3);
        ctx.takeScreenshot("obf-preview-2");

        // Отмена и повтор (Ctrl+Z / Ctrl+Y).
        ctrl(ctx, InputConstants.KEY_Z);
        String undone = ctx.computeOnClient(mc -> HexState.S.text);
        LogViewChecks.check(!TYPED.equals(undone), "Ctrl+Z отменил ввод («" + undone + "»)");
        ctrl(ctx, InputConstants.KEY_Y);
        LogViewChecks.check(TYPED.equals(ctx.computeOnClient(mc -> HexState.S.text)), "Ctrl+Y вернул ввод");

        // Вставка куска текста (то же, что делает Ctrl+V, но без системного буфера обмена — тесты его не трогают).
        focusTextBox(ctx);
        ctx.runOnClient(mc -> {
            if (mc.screen.getFocused() instanceof EditBox b) b.insertText(" &KAB");
        });
        ctx.waitTicks(3);
        String pasted = ctx.computeOnClient(mc -> HexState.S.text);
        LogViewChecks.check(pasted.endsWith("монет &KAB") && ctx.computeOnClient(mc -> HexState.S.visibleText()).endsWith("монет AB"),
                "вставка «&KAB» в поле («" + pasted + "»)");
        ctrl(ctx, InputConstants.KEY_Z);
        LogViewChecks.check(TYPED.equals(ctx.computeOnClient(mc -> HexState.S.text)), "Ctrl+Z отменил вставку");

        // Кнопка K на всём тексте: включить, затем выключить (коды переходят в формат, текст без &k/&r).
        focusTextBox(ctx);
        ctrl(ctx, InputConstants.KEY_A);
        clickK(ctx);
        LogViewChecks.check(ctx.computeOnClient(mc -> obfRange(HexState.S.effectiveMask())).equals("0-16"), "K на всём тексте — обфускация везде");
        focusTextBox(ctx);
        ctrl(ctx, InputConstants.KEY_A);
        clickK(ctx);
        String after = ctx.computeOnClient(mc -> HexState.S.text);
        LogViewChecks.check("Цена: 12345 монет".equals(after) && ctx.computeOnClient(mc -> obfRange(HexState.S.effectiveMask())).isEmpty(),
                "K повторно — обфускация снята, коды убраны из текста («" + after + "»)");
        ctx.setScreen(() -> null);
        ctx.waitTicks(3);
    }

    /** Фокус и курсор в конец основного поля текста (его значение совпадает с текстом генератора). */
    private static boolean focusTextBox(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            String t = HexState.S.text;
            for (GuiEventListener w : mc.screen.children()) {
                if (w instanceof EditBox b && b.getValue().equals(t)) {
                    mc.screen.setFocused(b);
                    b.setCursorPosition(t.length());
                    b.setHighlightPos(t.length());
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Ctrl+клавиша прямо в окно (его настоящий обработчик keyPressed). Через ctx.getInput() зажатый Ctrl
     * в событие не попадает: в CI Ctrl+Z и Ctrl+V приходили как простые Z и V.
     */
    private static void ctrl(ClientGameTestContext ctx, int key) {
        ctx.runOnClient(mc -> mc.screen.keyPressed(new KeyEvent(key, 0, InputConstants.MOD_CONTROL)));
        ctx.waitTicks(3);
    }

    private static void clickK(ClientGameTestContext ctx) {
        int[] p = ctx.computeOnClient(mc -> {
            double scale = mc.getWindow().getGuiScale();
            for (GuiEventListener w : mc.screen.children()) {
                if (w instanceof Button b && b.getMessage().getString().equals("K")) {
                    return new int[]{(int) ((b.getX() + b.getWidth() / 2.0) * scale), (int) ((b.getY() + b.getHeight() / 2.0) * scale)};
                }
            }
            return null;
        });
        if (p == null) {
            LogViewChecks.check(false, "кнопка K есть");
            return;
        }
        ctx.getInput().setCursorPos(p[0], p[1]);
        ctx.waitTicks(1);
        ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        ctx.waitTicks(3);
    }

    /** Где стоит бит обфускации: «6-10», «0-16», «» (нигде) или список через запятую. */
    private static String obfRange(int[] mask) {
        StringBuilder sb = new StringBuilder();
        int start = -1;
        for (int i = 0; i <= mask.length; i++) {
            boolean on = i < mask.length && (mask[i] & HexCore.OBFUSCATED) != 0;
            if (on && start < 0) start = i;
            if (!on && start >= 0) {
                if (sb.length() > 0) sb.append(',');
                sb.append(start).append('-').append(i - 1);
                start = -1;
            }
        }
        return sb.toString();
    }
}
