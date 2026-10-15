package ru.fspirat.fslog;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Выгрузка одного снимка лога на сайт — по нажатию кнопки в окне просмотра, и только так.
 * Без Minecraft (проверяется тестами в tests/logic): снимок, состояния, защита от двойных нажатий,
 * одна отправка за раз на всю игру и пауза после успешной выгрузки.
 *
 * Все методы вызываются на клиентском потоке; ответ сети тоже возвращается туда (executor).
 */
public final class LogUpload<E> {
    /** url — ссылка на лог; иначе error — код причины (rate, size, server, offline, network), waitSec — сколько ждать. */
    public record Result(String url, String error, int waitSec) {}

    public enum State { READY, SENDING, DONE, FAILED }

    /** Что вышло из нажатия «Выгрузить». */
    public enum Start { STARTED, ALREADY, EMPTY, BUSY, COOLDOWN }

    /** Общее для всех окон: идёт ли сейчас отправка и когда была последняя успешная. */
    public static final class Gate {
        public static final long COOLDOWN_MS = 15_000;
        private boolean busy;
        private long lastSuccess = Long.MIN_VALUE / 2;

        public boolean busy() { return busy; }

        /** Сколько секунд ещё ждать после прошлой успешной выгрузки (0 — можно). */
        public int waitSec(long now) {
            long left = lastSuccess + COOLDOWN_MS - now;
            return left > 0 ? (int) ((left + 999) / 1000) : 0;
        }
    }

    /** Одна очередь на всю игру: повторно открытое окно не отправит лог, пока идёт прошлая отправка. */
    public static final Gate GATE = new Gate();

    private final List<E> snapshot;
    private final Function<List<E>, CompletableFuture<Result>> transport;
    private final Executor client;
    private final LongSupplier clock;
    private final Gate gate;
    private State state = State.READY;
    private Result result;
    private int refusedWait;
    private boolean open = true;
    private Consumer<Result> afterClose = r -> {};

    /**
     * @param snapshot  что увидел игрок; копируется и больше не меняется — новые сообщения сюда не попадут
     * @param transport отправка (Uploader.upload); вызывается только из {@link #start()}
     * @param client    куда вернуть ответ (клиентский поток Minecraft)
     */
    public LogUpload(List<E> snapshot, Function<List<E>, CompletableFuture<Result>> transport, Executor client,
                     LongSupplier clock, Gate gate) {
        this.snapshot = List.copyOf(snapshot);
        this.transport = transport;
        this.client = client;
        this.clock = clock;
        this.gate = gate;
    }

    public List<E> snapshot() { return snapshot; }
    public State state() { return state; }
    public Result result() { return result; }
    /** Секунды ожидания из последнего отказа COOLDOWN. */
    public int refusedWait() { return refusedWait; }
    public boolean sending() { return state == State.SENDING; }

    /** Можно ли сейчас нажать «Выгрузить» (или «Повторить»). */
    public boolean canStart() {
        return !snapshot.isEmpty() && (state == State.READY || state == State.FAILED) && !gate.busy() && gate.waitSec(clock.getAsLong()) == 0;
    }

    /** Ответ, пришедший после закрытия окна, — например, сообщение со ссылкой в чат. */
    public void onResultAfterClose(Consumer<Result> c) {
        afterClose = c == null ? r -> {} : c;
    }

    /** Окно закрыли: отправка (если идёт) спокойно завершится, окно само не откроется. */
    public void close() { open = false; }

    /** Окно снова показано (например, вернулись из подтверждения ссылки). */
    public void reopen() { open = true; }

    /** Нажатие «Выгрузить на сайт» / «Повторить». */
    public Start start() {
        if (snapshot.isEmpty()) return Start.EMPTY;
        if (state == State.SENDING || state == State.DONE) return Start.ALREADY;
        if (gate.busy) return Start.BUSY;
        int wait = gate.waitSec(clock.getAsLong());
        if (wait > 0) {
            refusedWait = wait;
            return Start.COOLDOWN;
        }
        gate.busy = true;
        state = State.SENDING;
        result = null;
        CompletableFuture<Result> f;
        try {
            f = transport.apply(snapshot);
        } catch (RuntimeException e) {
            f = CompletableFuture.failedFuture(e);
        }
        if (f == null) f = CompletableFuture.completedFuture(null);
        f.whenComplete((r, err) -> client.execute(() -> finish(r, err)));
        return Start.STARTED;
    }

    private void finish(Result r, Throwable err) {
        gate.busy = false;
        Result res = err != null ? new Result(null, "network", 0) : r != null ? r : new Result(null, "server", 0);
        result = res;
        if (res.url() != null) {
            gate.lastSuccess = clock.getAsLong();
            state = State.DONE;
        } else {
            state = State.FAILED;
        }
        if (!open) afterClose.accept(res);
    }
}
