package ru.fspirat.hexgen;

/** Уведомление об обновлении: автоматически — раз за запуск, вручную из админки — ещё раз; только при старой версии. */
public final class UpdateNoticeTest {
    private static int failed = 0, passed = 0;

    private static void check(boolean ok, String what) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL: " + what); }
    }

    public static void main(String[] args) {
        UpdateNotice n = new UpdateNotice();
        check(n.onAnswer("1.2.4", "1.2.4", 0) == false, "та же версия — молчим");
        check(n.onAnswer("1.2.4", "1.2.4", 1) == false, "рассылка, но версия свежая — молчим");

        UpdateNotice a = new UpdateNotice();
        check(a.onAnswer("1.2.5", "1.2.4", 3), "вышла новая — пишем (автоматически)");
        check(!a.onAnswer("1.2.5", "1.2.4", 3), "в тот же запуск второй раз — молчим");
        check(!a.onAnswer("1.2.5", "1.2.4", 3), "и через 10 минут — молчим");
        check(a.onAnswer("1.2.5", "1.2.4", 4), "нажали «Разослать» — пишем ещё раз");
        check(!a.onAnswer("1.2.5", "1.2.4", 4), "та же рассылка — один раз");
        check(!a.onAnswer("1.2.5", "1.2.4", 0), "номер рассылки сбросился (база очищена) — не спамим");
        check(a.onAnswer("1.2.5", "1.2.4", 5), "следующая рассылка — снова пишем");

        UpdateNotice b = new UpdateNotice();
        check(b.onAnswer("1.2.5", "1.2.4", 7), "игру запустили после рассылки — одно уведомление");
        check(!b.onAnswer("1.2.5", "1.2.4", 7), "без новой рассылки — не повторяем");

        UpdateNotice c = new UpdateNotice();
        check(!c.onAnswer("", "1.2.4", 9), "версии на сайте нет — молчим");
        check(!c.onAnswer("1.2.3", "1.2.4", 10), "на сайте старее, рассылка — молчим");
        check(c.onAnswer("1.2.10", "1.2.4", 10), "1.2.10 новее 1.2.4 (числа, не строки)");

        check(UpdateNotice.compare("1.10", "1.9") > 0 && UpdateNotice.compare("1.2", "1.2.0") == 0, "сравнение версий");
        System.out.println("UpdateNotice: " + passed + " ok, " + failed + " fail");
        if (failed > 0) System.exit(1);
    }
}
