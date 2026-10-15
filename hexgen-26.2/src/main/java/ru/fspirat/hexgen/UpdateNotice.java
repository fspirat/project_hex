package ru.fspirat.hexgen;

/**
 * Когда писать в чат «вышла новая версия». Без Minecraft — проверяется тестами в tests/logic.
 *   - автоматически: один раз за запуск игры, если на сайте версия новее;
 *   - вручную: в админке нажали «Разослать уведомление» — номер рассылки на сайте вырос,
 *     и мод пишет ещё раз (тоже только если его версия старее).
 */
final class UpdateNotice {
    private boolean shown;
    /** Номер ручной рассылки из прошлого ответа сайта; -1 — ответов ещё не было. */
    private long push = -1;

    /** Ответ сайта: последняя версия и номер рассылки. true — показать уведомление сейчас. */
    synchronized boolean onAnswer(String latest, String mine, long pushId) {
        boolean manual = push >= 0 && pushId > push;
        if (push < 0 || pushId > push) push = pushId;
        if (latest == null || latest.isEmpty() || compare(latest, mine) <= 0) return false;
        if (shown && !manual) return false;
        shown = true;
        return true;
    }

    /** Сравнение версий вида 1.2 / 1.10.1: >0 — a новее b. */
    static int compare(String a, String b) {
        String[] x = a.split("[.+-]"), y = b.split("[.+-]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int d = num(i < x.length ? x[i] : "0") - num(i < y.length ? y[i] : "0");
            if (d != 0) return d;
        }
        return 0;
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
