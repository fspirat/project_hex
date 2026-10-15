package ru.fspirat.hexgen.shots;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import com.mojang.blaze3d.platform.InputConstants;

import ru.fspirat.hexgen.fslog.ChatBuffer;
import ru.fspirat.hexgen.fslog.LogUpload;
import ru.fspirat.hexgen.fslog.LogViewScreen;

/**
 * Окно /log в настоящей игре. Сайт заменён локальной заглушкой (-Dfslog.endpoint), настоящие логи никуда не уходят.
 * Проверяет: открытие и закрытие ничего не отправляют; по кнопке уходит ровно показанный снимок;
 * двойной клик, ошибка сети и «Повторить», пауза после выгрузки, закрытие во время отправки.
 */
final class LogViewChecks {
    private LogViewChecks() {}

    private static final List<String> FAILS = new ArrayList<>();

    static void check(boolean ok, String what) {
        System.out.println("[checks] " + (ok ? "OK   " : "FAIL ") + what);
        if (!ok) FAILS.add(what);
    }

    static List<String> fails() {
        return FAILS;
    }

    /** Заглушка chatlog.php: считает запросы и запоминает тела. */
    private static final class Stub implements AutoCloseable {
        enum Mode { OK, DOWN, SLOW }
        final HttpServer server;
        final ExecutorService pool = Executors.newCachedThreadPool();
        final AtomicInteger requests = new AtomicInteger();
        final List<String> bodies = Collections.synchronizedList(new ArrayList<>());
        volatile Mode mode = Mode.OK;

        Stub() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(pool);
            server.createContext("/api/chatlog.php", ex -> {
                String body;
                try (InputStream in = ex.getRequestBody()) {
                    body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                int n = requests.incrementAndGet();
                bodies.add(body);
                Mode m = mode;
                if (m == Mode.SLOW) {
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                }
                byte[] out = m == Mode.DOWN ? new byte[0]
                        : ("{\"url\":\"" + base() + "/l/test" + n + "\"}").getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(m == Mode.DOWN ? 503 : 200, out.length == 0 ? -1 : out.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(out);
                }
            });
            server.start();
        }

        String base() { return "http://127.0.0.1:" + server.getAddress().getPort(); }

        String lastBody() { return bodies.isEmpty() ? "" : bodies.get(bodies.size() - 1); }

        @Override
        public void close() {
            server.stop(0);
            pool.shutdownNow();
        }
    }

    static void run(ClientGameTestContext ctx, TestSingleplayerContext sp) {
        Stub stub;
        try {
            stub = new Stub();
        } catch (IOException e) {
            check(false, "заглушка сайта запустилась: " + e);
            return;
        }
        // Остаётся и после теста: заглушка закрыта — любой лишний запрос просто не дойдёт, на боевой сайт тоже.
        System.setProperty("fslog.endpoint", stub.base() + "/api/chatlog.php");
        try (stub) {
            steps(ctx, sp, stub);
        }
    }

    private static void steps(ClientGameTestContext ctx, TestSingleplayerContext sp, Stub stub) {
        String ver = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("hexgen")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
        check("1.2.4".equals(ver), "версия мода 1.2.4 (" + ver + ")");

        // /log clear — очищает и окно не открывает.
        command(ctx, "/log clear");
        ctx.waitTicks(5);
        check(ctx.computeOnClient(mc -> mc.screen == null), "/log clear не открывает окно");
        check(ctx.computeOnClient(mc -> ChatBuffer.size()) == 0, "/log clear очищает буфер");

        // Пустое окно: выгрузка выключена, кнопка ничего не делает.
        LogViewScreen empty = open(ctx, "/log");
        check(empty.upload().snapshot().isEmpty() && !empty.upload().canStart(), "пустой лог: выгрузка выключена");
        click(ctx, I18n.get("fslog.view.upload"));
        ctx.waitTicks(5);
        check(stub.requests.get() == 0 && empty.upload().state() == LogUpload.State.READY, "пустой лог: нажатие ничего не отправило");
        ctx.takeScreenshot("log-empty");
        escape(ctx);

        // Чат: цвета, жирный, длинная строка с переносом, &k и строка с командой по клику.
        long before = ctx.computeOnClient(mc -> ChatBuffer.added());
        int n = 0;
        for (int i = 1; i <= 30; i++, n++) {
            sp.getServer().runCommand("tellraw @a [{\"text\":\"[Игрок" + i + "] \",\"color\":\"#7FBF3A\"},{\"text\":\"сообщение номер " + i + "\",\"color\":\"white\"}]");
        }
        sp.getServer().runCommand("tellraw @a [{\"text\":\"Жирный \",\"bold\":true,\"color\":\"gold\"},{\"text\":\"курсив \",\"italic\":true},"
                + "{\"text\":\"подчёркнутый \",\"underlined\":true},{\"text\":\"зачёркнутый\",\"strikethrough\":true}]"); n++;
        sp.getServer().runCommand("tellraw @a [{\"text\":\"Цена: \"},{\"text\":\"OBFSECRET\",\"obfuscated\":true,\"color\":\"aqua\"},{\"text\":\" монет\"}]"); n++;
        sp.getServer().runCommand("tellraw @a {\"text\":\"Очень длинное сообщение, которое не помещается в одну строку окна и должно аккуратно перенестись на следующие строки без обрезки текста — проверка переноса по словам.\",\"color\":\"yellow\"}"); n++;
        sp.getServer().runCommand("tellraw @a {\"text\":\"Кликни сюда (в окне лога клик ничего не делает)\",\"color\":\"red\",\"click_event\":{\"action\":\"run_command\",\"command\":\"/say CLICKED\"}}"); n++;
        final int sent = n;
        check(waitUntil(ctx, mc -> ChatBuffer.added() - before >= sent, 200), "все " + sent + " тестовых сообщений дошли до буфера");

        // /log открывает окно и ничего не отправляет.
        LogViewScreen s1 = open(ctx, "/log");
        int snap = s1.upload().snapshot().size();
        int buffered = ctx.computeOnClient(mc -> ChatBuffer.size());
        check(snap == buffered && snap >= sent, "снимок = весь буфер (" + snap + " из " + buffered + ")");
        check(hasObfuscated(s1.upload().snapshot(), "OBFSECRET"), "&k из чата сохранён в буфере (флаг 16)");
        check(waitUntil(ctx, mc -> s1.laidOut(), 200), "разметка окна завершилась");
        ctx.waitTicks(20);
        check(stub.requests.get() == 0, "открытие /log ничего не отправило");
        ctx.takeScreenshot("log-view");

        // Клик по строке с командой: в окне лога ничего не выполняется.
        clickNewestLine(ctx);
        ctx.waitTicks(20);
        check(!bufferContains(ctx, "CLICKED"), "клик по сообщению в окне ничего не выполнил");

        // Прокрутка к самому старому и обратно.
        ctx.getInput().pressKey(InputConstants.KEY_HOME);
        ctx.waitTicks(3);
        ctx.takeScreenshot("log-view-top");
        ctx.getInput().pressKey(InputConstants.KEY_END);
        ctx.waitTicks(2);

        // Поиск: Ctrl+F, регистр не важен; пустой запрос — снова все сообщения; в сеть ничего не уходит.
        ctrl(ctx, InputConstants.KEY_F);
        ctx.getInput().typeChars("НОМЕР 1");
        ctx.waitTicks(3);
        check(s1.shownCount() == 11, "поиск «НОМЕР 1» нашёл 11 сообщ. (" + s1.shownCount() + ")");
        waitUntil(ctx, mc -> s1.laidOut(), 100);
        ctx.waitTicks(3);
        ctx.takeScreenshot("log-search");
        ctx.getInput().typeChars("zzz");
        ctx.waitTicks(3);
        check(s1.shownCount() == 0, "поиск без совпадений — пустой список");
        ctx.takeScreenshot("log-search-none");
        ctrl(ctx, InputConstants.KEY_A);
        ctx.runOnClient(mc -> mc.screen.keyPressed(new KeyEvent(InputConstants.KEY_BACKSPACE, 0, 0)));
        ctx.waitTicks(3);
        check(s1.shownCount() == snap, "поиск очищен — снова все " + snap + " сообщ. (" + s1.shownCount() + ")");
        check(stub.requests.get() == 0, "поиск ничего не отправил");

        // Новое сообщение во время чтения в снимок не попадает.
        sp.getServer().runCommand("tellraw @a {\"text\":\"NEWAFTERSNAPSHOT\"}");
        waitUntil(ctx, mc -> bufferContainsNow("NEWAFTERSNAPSHOT"), 100);
        check(s1.upload().snapshot().size() == snap && !snapshotContains(s1.upload().snapshot(), "NEWAFTERSNAPSHOT"),
                "новое сообщение во время чтения не попало в снимок");

        // Закрыли без нажатия — ничего не ушло.
        escape(ctx);
        ctx.waitTicks(20);
        check(stub.requests.get() == 0, "закрытие без нажатия ничего не отправило");

        // Открыли снова: новое сообщение уже в снимке. Пока читаем, приходит ещё одно.
        LogViewScreen s2 = open(ctx, "/log");
        int snap2 = s2.upload().snapshot().size();
        check(snap2 > snap && snapshotContains(s2.upload().snapshot(), "NEWAFTERSNAPSHOT"), "повторное открытие — свежий снимок (" + snap2 + ")");
        sp.getServer().runCommand("tellraw @a {\"text\":\"NEWWHILEREADING\"}");
        waitUntil(ctx, mc -> bufferContainsNow("NEWWHILEREADING"), 100);

        // Поиск во время выгрузки: показана часть, а на сайт уходит весь снимок.
        ctrl(ctx, InputConstants.KEY_F);
        ctx.getInput().typeChars("номер 2");
        ctx.waitTicks(3);
        int found2 = s2.shownCount();
        check(found2 > 0 && found2 < snap2, "поиск перед выгрузкой: показано " + found2 + " из " + snap2);

        // Двойной клик (ответ сайта медленный): ушёл один запрос.
        stub.mode = Stub.Mode.SLOW;
        click(ctx, I18n.get("fslog.view.upload"));
        ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        ctx.runOnClient(mc -> s2.pressUpload());   // третье нажатие во время отправки
        check(ctx.computeOnClient(mc -> s2.upload().state()) == LogUpload.State.SENDING, "после нажатия — «Отправка…»");
        ctx.takeScreenshot("log-sending");
        waitUntil(ctx, mc -> s2.upload().state() != LogUpload.State.SENDING, 400);
        ctx.waitTicks(5);
        check(stub.requests.get() == 1, "двойной клик — один запрос (было " + stub.requests.get() + ")");
        check(s2.upload().state() == LogUpload.State.DONE && s2.upload().result().url().startsWith(stub.base()),
                "успех: ссылка получена");
        JsonArray msgs = messages(stub.lastBody());
        check(msgs != null && msgs.size() == snap2, "отправлен весь снимок, поиск не влияет: " + snap2 + " сообщ. (" + (msgs == null ? "—" : msgs.size()) + ")");
        check(stub.lastBody().contains("NEWAFTERSNAPSHOT") && !stub.lastBody().contains("NEWWHILEREADING"),
                "отправлен показанный снимок, без сообщений после открытия");
        check(segFlags(msgs, "OBFSECRET") == 16, "&k уходит на сайт с флагом 16");
        ctx.takeScreenshot("log-done");

        // «Обновить» берёт новый снимок; повторная выгрузка — только после паузы.
        click(ctx, I18n.get("fslog.view.refresh"));
        ctx.waitTicks(3);
        LogViewScreen s3 = current(ctx);
        check(s3 == s2 && s2.upload().state() == LogUpload.State.READY && snapshotContains(s2.upload().snapshot(), "NEWWHILEREADING"),
                "«Обновить» — новый снимок с новыми сообщениями");
        ctx.runOnClient(mc -> s2.pressUpload());
        ctx.waitTicks(5);
        check(stub.requests.get() == 1 && s2.upload().refusedWait() > 0, "пауза после выгрузки: второй запрос не ушёл");
        ctx.takeScreenshot("log-cooldown");
        escape(ctx);

        // Сайт недоступен → ошибка и «Повторить».
        waitCooldown(ctx);
        stub.mode = Stub.Mode.DOWN;
        LogViewScreen s4 = open(ctx, "/log 5");
        check(s4.upload().snapshot().size() == 5, "/log 5 — последние 5 сообщений");
        click(ctx, I18n.get("fslog.view.upload"));
        waitUntil(ctx, mc -> s4.upload().state() != LogUpload.State.SENDING, 400);
        ctx.waitTicks(3);
        check(s4.upload().state() == LogUpload.State.FAILED && "offline".equals(s4.upload().result().error()),
                "сайт недоступен — ошибка «offline»");
        check(stub.requests.get() == 2, "ошибка — один запрос");
        ctx.takeScreenshot("log-failed");
        stub.mode = Stub.Mode.OK;
        click(ctx, I18n.get("fslog.view.retry"));
        waitUntil(ctx, mc -> s4.upload().state() != LogUpload.State.SENDING, 400);
        ctx.waitTicks(3);
        JsonArray retry = messages(stub.lastBody());
        check(s4.upload().state() == LogUpload.State.DONE && stub.requests.get() == 3 && retry != null && retry.size() == 5,
                "«Повторить» отправил тот же снимок из 5 сообщ.");
        escape(ctx);

        // Закрыли во время отправки: окно само не откроется, повтора нет, ссылка придёт в чат.
        waitCooldown(ctx);
        stub.mode = Stub.Mode.SLOW;
        LogViewScreen s5 = open(ctx, "/log 3");
        click(ctx, I18n.get("fslog.view.upload"));
        check(ctx.computeOnClient(mc -> s5.upload().state()) == LogUpload.State.SENDING, "отправка началась");
        escape(ctx);
        // Открыли снова, пока идёт отправка: второй запрос не уходит.
        LogViewScreen s6 = open(ctx, "/log");
        check(!s6.upload().canStart(), "пока идёт прошлая отправка, кнопка выключена");
        ctx.runOnClient(mc -> s6.pressUpload());
        ctx.waitTicks(3);
        check(stub.requests.get() == 4, "повторно открытое окно не отправило второй запрос");
        escape(ctx);
        waitUntil(ctx, mc -> s5.upload().state() != LogUpload.State.SENDING, 400);
        ctx.waitTicks(20);
        check(s5.upload().state() == LogUpload.State.DONE, "ответ после закрытия окна получен");
        check(ctx.computeOnClient(mc -> mc.screen == null), "окно само не открылось");
        check(stub.requests.get() == 4, "после закрытия повторной отправки нет");
        ctx.getInput().pressKey(InputConstants.KEY_T);
        ctx.waitTicks(5);
        ctx.takeScreenshot("log-closed-announce");
        escape(ctx);
    }

    // ---------------------------------------------------------------- помощники

    private static void command(ClientGameTestContext ctx, String cmd) {
        ctx.getInput().pressKey(InputConstants.KEY_T);
        ctx.waitTicks(3);
        ctx.getInput().typeChars(cmd);
        ctx.getInput().pressKey(InputConstants.KEY_RETURN);
        ctx.waitTicks(2);
    }

    /** Набирает команду в чате и ждёт окно лога; не открылось — дальше проверять нечего. */
    private static LogViewScreen open(ClientGameTestContext ctx, String cmd) {
        command(ctx, cmd);
        boolean ok = waitUntil(ctx, mc -> mc.screen instanceof LogViewScreen, 100);
        check(ok, cmd + " открывает окно лога");
        if (!ok) throw new AssertionError(cmd + " не открыл окно лога");
        ctx.waitTicks(3);
        return current(ctx);
    }

    /** Ждёт условие не дольше ticks тиков; в отличие от waitFor не роняет тест. */
    private static boolean waitUntil(ClientGameTestContext ctx, java.util.function.Predicate<net.minecraft.client.Minecraft> cond, int ticks) {
        for (int i = 0; i < ticks; i++) {
            if (ctx.computeOnClient(cond::test)) return true;
            ctx.waitTick();
        }
        return ctx.computeOnClient(cond::test);
    }

    private static LogViewScreen current(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> mc.screen instanceof LogViewScreen v ? v : null);
    }

    /** Ctrl+клавиша прямо в окно: зажатый Ctrl из ctx.getInput() в событие не попадает (см. ObfChecks). */
    private static void ctrl(ClientGameTestContext ctx, int key) {
        ctx.runOnClient(mc -> mc.screen.keyPressed(new KeyEvent(key, 0, InputConstants.MOD_CONTROL)));
        ctx.waitTicks(2);
    }

    private static void escape(ClientGameTestContext ctx) {
        ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
        ctx.waitTicks(3);
    }

    /** Наводит курсор на кнопку с такой надписью (начало надписи) и нажимает левую кнопку мыши. */
    private static void click(ClientGameTestContext ctx, String label) {
        int[] p = ctx.computeOnClient(mc -> {
            double scale = mc.getWindow().getGuiScale();
            for (GuiEventListener w : mc.screen.children()) {
                if (w instanceof Button b && b.getMessage().getString().startsWith(label)) {
                    return new int[]{(int) ((b.getX() + b.getWidth() / 2.0) * scale), (int) ((b.getY() + b.getHeight() / 2.0) * scale)};
                }
            }
            return null;
        });
        if (p == null) {
            check(false, "кнопка «" + label + "» есть на экране");
            return;
        }
        ctx.getInput().setCursorPos(p[0], p[1]);
        ctx.waitTicks(1);
        ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        ctx.waitTicks(1);
    }

    /** Клик по самой нижней строке окна (самое новое сообщение). Разметка — как в LogViewScreen.init. */
    private static void clickNewestLine(ClientGameTestContext ctx) {
        int[] p = ctx.computeOnClient(mc -> {
            double scale = mc.getWindow().getGuiScale();
            int width = mc.screen.width, height = mc.screen.height;
            int w = Math.min(width - 16, 520), left = (width - w) / 2, h = height - 16;
            int areaY = 8 + 26, areaH = Math.max(36, h - 76);
            int x = left + 6 + mc.font.width("00:00:00 ") + 20, y = areaY + areaH - 3 - 10 + 4;
            return new int[]{(int) (x * scale), (int) (y * scale)};
        });
        ctx.getInput().setCursorPos(p[0], p[1]);
        ctx.waitTicks(1);
        ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static void waitCooldown(ClientGameTestContext ctx) {
        waitUntil(ctx, mc -> LogUpload.GATE.waitSec(System.currentTimeMillis()) == 0 && !LogUpload.GATE.busy(), 20 * 30);
    }

    private static boolean bufferContains(ClientGameTestContext ctx, String text) {
        return ctx.computeOnClient(mc -> bufferContainsNow(text));
    }

    private static boolean bufferContainsNow(String text) {
        return snapshotContains(ChatBuffer.last(ChatBuffer.MAX), text);
    }

    private static boolean snapshotContains(List<ChatBuffer.Entry> list, String text) {
        for (ChatBuffer.Entry e : list) {
            for (ChatBuffer.Seg s : e.segs()) if (s.text().contains(text)) return true;
        }
        return false;
    }

    private static boolean hasObfuscated(List<ChatBuffer.Entry> list, String text) {
        for (ChatBuffer.Entry e : list) {
            for (ChatBuffer.Seg s : e.segs()) if (s.text().contains(text) && (s.flags() & 16) != 0) return true;
        }
        return false;
    }

    private static JsonArray messages(String body) {
        try {
            JsonObject o = JsonParser.parseString(body).getAsJsonObject();
            return o.getAsJsonArray("messages");
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Флаги куска с этим текстом в отправленном JSON (-1 — не найден). */
    private static int segFlags(JsonArray msgs, String text) {
        if (msgs == null) return -1;
        for (JsonElement m : msgs) {
            for (JsonElement s : m.getAsJsonObject().getAsJsonArray("s")) {
                JsonArray seg = s.getAsJsonArray();
                if (seg.get(0).getAsString().contains(text)) return seg.get(2).getAsInt();
            }
        }
        return -1;
    }
}
