<?php
/**
 * Скачивание файлов мода с нашего сервера + счётчик.
 *   GET /api/dl.php?f=fstweak-26.2.jar → 302 на /dl/files/fstweak-26.2.jar (файл отдаёт nginx)
 * Считается одно скачивание файла с одного адреса в сутки (адрес хранится только хэшем с суточной солью).
 * Файлы выкладывает workflow «Publish downloads» (.github/workflows/downloads.yml).
 */

declare(strict_types=1);

define('FS_STATS', true);
require __DIR__ . '/lib/stats.php';

header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store');

$f = (string)($_GET['f'] ?? '');
$path = __DIR__ . '/../dl/files/' . $f;
if (!preg_match('/^(fstweak|fslog)-[0-9][0-9.]*\.jar$/', $f) || !is_file($path)) {
    http_response_code(404);
    header('Content-Type: text/plain; charset=utf-8');
    echo "Файл не найден. Страница мода: https://fspirat.ru/fstweak/\n";
    exit;
}

try {
    $db = stats_db();
    if ($db && !isset($_GET['check'])) {        // ?check=1 — проверка после выкладки, не считается
        dl_tables($db);
        $day = gmdate('Y-m-d');
        $salt = (string)(stats_config()['secret'] ?? '') . $day;
        $who = substr(hash('sha256', $salt . ($_SERVER['REMOTE_ADDR'] ?? '')), 0, 24);
        $st = $db->prepare('INSERT OR IGNORE INTO dl_seen(day, file, who) VALUES(?, ?, ?)');
        $st->execute([$day, $f, $who]);
        if ($st->rowCount() > 0) {
            $db->prepare('INSERT INTO dl_days(day, file, n) VALUES(?, ?, 1) ON CONFLICT(day, file) DO UPDATE SET n = n + 1')->execute([$day, $f]);
        }
        if (random_int(1, 50) === 1) $db->prepare('DELETE FROM dl_seen WHERE day < ?')->execute([gmdate('Y-m-d', time() - 2 * 86400)]);
    }
} catch (Throwable $e) {
    error_log('fspirat-dl: ' . $e->getMessage());       // счётчик не должен мешать скачиванию
}

header('Location: /dl/files/' . rawurlencode($f), true, 302);
