package ru.fspirat.hexgen.fslog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Выгрузка лога: ничего не уходит без нажатия, ровно снимок, одна отправка за раз, пауза, ошибки. */
public final class LogUploadTest {
    private static int failed = 0, passed = 0;

    private static void check(boolean ok, String what) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL: " + what); }
    }

    /** Подделка сети: запоминает отправленные снимки, ответ приходит, когда тест скажет. */
    static final class FakeNet {
        final List<List<String>> sent = new ArrayList<>();
        final List<CompletableFuture<LogUpload.Result>> pending = new ArrayList<>();
        CompletableFuture<LogUpload.Result> send(List<String> s) {
            sent.add(s);
            CompletableFuture<LogUpload.Result> f = new CompletableFuture<>();
            pending.add(f);
            return f;
        }
        void ok(String url) { pending.remove(0).complete(new LogUpload.Result(url, null, 0)); }
        void fail(String code) { pending.remove(0).complete(new LogUpload.Result(null, code, 0)); }
        void offline() { pending.remove(0).completeExceptionally(new java.io.IOException("no route")); }
    }

    public static void main(String[] args) {
        long[] now = {1_000_000};
        LogUpload.Gate gate = new LogUpload.Gate();
        FakeNet net = new FakeNet();
        List<String> buffer = new ArrayList<>(List.of("a", "b", "c"));

        // открыли окно: снимок есть, в сеть ничего не ушло
        LogUpload<String> up = new LogUpload<>(buffer, net::send, Runnable::run, () -> now[0], gate);
        check(net.sent.isEmpty(), "открытие окна ничего не отправляет");
        // во время чтения пришли новые сообщения — снимок не меняется
        buffer.add("d");
        check(up.snapshot().equals(List.of("a", "b", "c")), "новые сообщения не попадают в снимок");
        // закрыли без нажатия — ничего
        up.close();
        check(net.sent.isEmpty(), "закрытие без нажатия ничего не отправляет");

        // новое окно, нажатие: ровно снимок, одна отправка
        LogUpload<String> u2 = new LogUpload<>(List.of("x", "y"), net::send, Runnable::run, () -> now[0], gate);
        check(u2.canStart(), "кнопка доступна");
        check(u2.start() == LogUpload.Start.STARTED, "нажатие запускает отправку");
        check(u2.start() == LogUpload.Start.ALREADY, "двойной клик не отправляет второй раз");
        check(net.sent.size() == 1 && net.sent.get(0).equals(List.of("x", "y")), "отправлен ровно снимок");
        check(u2.state() == LogUpload.State.SENDING && !u2.canStart(), "состояние «отправка», кнопка неактивна");

        // повторно открыли окно, пока идёт отправка, — вторая отправка не начинается
        LogUpload<String> u3 = new LogUpload<>(List.of("z"), net::send, Runnable::run, () -> now[0], gate);
        check(!u3.canStart() && u3.start() == LogUpload.Start.BUSY, "повторное окно во время отправки: занято");
        check(net.sent.size() == 1, "одновременной отправки нет");

        // закрыли окно во время отправки; ответ пришёл — без ошибок, результат уходит в «после закрытия»
        List<LogUpload.Result> afterClose = new ArrayList<>();
        u2.onResultAfterClose(afterClose::add);
        u2.close();
        net.ok("https://example.test/log/?id=AAAAAAAAAA");
        check(u2.state() == LogUpload.State.DONE && "https://example.test/log/?id=AAAAAAAAAA".equals(u2.result().url()), "успех со ссылкой");
        check(afterClose.size() == 1, "ответ после закрытия окна передан (сообщение в чат), окно не открывается");
        check(u2.start() == LogUpload.Start.ALREADY && net.sent.size() == 1, "после успеха тот же снимок не отправляется снова");

        // пауза после успешной отправки
        LogUpload<String> u4 = new LogUpload<>(List.of("q"), net::send, Runnable::run, () -> now[0], gate);
        check(u4.start() == LogUpload.Start.COOLDOWN && u4.refusedWait() == 15, "пауза 15 с после успеха");
        now[0] += 15_000;
        check(u4.canStart() && u4.start() == LogUpload.Start.STARTED, "после паузы — можно");

        // ошибка сети → можно повторить; повтор отправляет тот же снимок
        net.offline();
        check(u4.state() == LogUpload.State.FAILED && "network".equals(u4.result().error()), "ошибка сети → FAILED/network");
        check(u4.canStart(), "после ошибки можно повторить");
        check(u4.start() == LogUpload.Start.STARTED && net.sent.get(net.sent.size() - 1).equals(List.of("q")), "повтор отправляет тот же снимок");
        net.fail("rate");
        check(u4.state() == LogUpload.State.FAILED && "rate".equals(u4.result().error()), "отказ сервера → FAILED/rate");

        // транспорт бросил исключение сразу — без падения
        LogUpload<String> u5 = new LogUpload<>(List.of("w"), s -> { throw new IllegalStateException("boom"); }, Runnable::run, () -> now[0], gate);
        check(u5.start() == LogUpload.Start.STARTED && u5.state() == LogUpload.State.FAILED && !gate.busy(), "исключение транспорта → FAILED, очередь свободна");

        // пустой буфер
        LogUpload<String> empty = new LogUpload<>(List.of(), net::send, Runnable::run, () -> now[0], gate);
        check(!empty.canStart() && empty.start() == LogUpload.Start.EMPTY, "пустой лог не отправляется");

        System.out.println("LogUpload: " + passed + " ok, " + failed + " fail");
        if (failed > 0) System.exit(1);
    }
}
