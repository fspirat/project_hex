<?php
/**
 * FSTWEAK: мод сообщает, кто в игре (ник, версии, сервер), и узнаёт последнюю версию.
 *   POST {"nick":"...","mod":"1.2","mc":"1.21.11","server":"...","lang":"ru_ru","join":1,"use":{"copy":2}}
 *   → {"version":"1.2","url":"https://fspirat.ru/fstweak/","notes":"..."}
 * Последняя версия и текст «что нового» — в fstweak-version.json рядом.
 * Данные лежат в базе статистики (/var/lib/fspirat-stats), видны во вкладке «FSTWEAK» админки.
 */

declare(strict_types=1);

define('FS_STATS', true);
require __DIR__ . '/lib/stats.php';

header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store');

$latest = json_decode((string)@file_get_contents(__DIR__ . '/fstweak-version.json'), true) ?: [];
$lang = substr((string)($_GET['lang'] ?? ''), 0, 2);

function answer(array $latest, string $lang): void
{
    $notes = $latest['notes'] ?? [];
    json_out([
        'version' => (string)($latest['version'] ?? ''),
        'url' => (string)($latest['url'] ?? 'https://fspirat.ru/fstweak/'),
        'notes' => (string)($notes[$lang] ?? $notes['en'] ?? $notes['ru'] ?? ''),
    ]);
    exit;
}

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'POST') answer($latest, $lang ?: 'ru');
if (($_SERVER['HTTP_ORIGIN'] ?? '') !== '') { json_out(['error' => 'origin'], 403); exit; }   // только из игры

$in = json_decode((string)file_get_contents('php://input', false, null, 0, 4096), true);
if (!is_array($in)) answer($latest, 'ru');
$str = fn($v, int $len) => is_string($v) ? mb_substr(preg_replace('/[^\P{C}]/u', '', $v) ?? '', 0, $len) : '';
$lang = substr($str($in['lang'] ?? '', 10), 0, 2) ?: 'ru';

$db = stats_db();
$nick = (string)($in['nick'] ?? '');
$newPlayer = null;
if ($db && preg_match('/^[A-Za-z0-9_]{1,16}$/', $nick)) {
    try {
        mod_tables($db);
        $cfg = stats_config();
        $key = 'mod:' . substr(hash('sha256', $cfg['secret'] . client_ip()), 0, 20);
        if (stats_rate($db, $key, 60, 3600)) {
            $now = time();
            $join = !empty($in['join']) ? 1 : 0;   // заход в мир (а не просто «я ещё играю»)
            $mod = $str($in['mod'] ?? '', 20); $mc = $str($in['mc'] ?? '', 20); $server = $str($in['server'] ?? '', 100);
            $st = $db->prepare('SELECT 1 FROM mod_players WHERE nick = ?'); $st->execute([$nick]);
            $isNew = $st->fetchColumn() === false;
            $db->beginTransaction();
            $db->prepare('INSERT INTO mod_players(nick, first_seen, last_seen, joins, mod, mc, server, lang) VALUES(?,?,?,?,?,?,?,?)
                          ON CONFLICT(nick) DO UPDATE SET last_seen = excluded.last_seen, joins = joins + excluded.joins,
                          mod = excluded.mod, mc = excluded.mc, server = excluded.server, lang = excluded.lang')
               ->execute([$nick, $now, $now, $join, $mod, $mc, $server, $str($in['lang'] ?? '', 10)]);
            // история: с какими версиями и на каких серверах играл
            $db->prepare('INSERT INTO mod_seen(nick, mod, mc, server, first_seen, last_seen) VALUES(?,?,?,?,?,?)
                          ON CONFLICT(nick, mod, mc, server) DO UPDATE SET last_seen = excluded.last_seen')
               ->execute([$nick, $mod, $mc, $server, $now, $now]);
            // счётчики функций мода: {"copy": 3, "fmt.0": 2, …} — только числа, без содержимого
            if (is_array($in['use'] ?? null)) {
                $up = $db->prepare('INSERT INTO mod_usage(day, k, n) VALUES(?,?,?) ON CONFLICT(day, k) DO UPDATE SET n = n + excluded.n');
                foreach (array_slice($in['use'], 0, 40, true) as $k => $n) {
                    if (is_string($k) && preg_match('/^[a-z][a-z0-9_.]{0,30}$/', $k) && is_int($n) && $n > 0)
                        $up->execute([date('Y-m-d', $now), $k, min($n, 1000)]);
                }
            }
            $db->commit();
            if ($isNew) $newPlayer = "🆕 Новый игрок с FSTWEAK: $nick\nFSTWEAK $mod · MC $mc · " . ($server === 'singleplayer' ? 'одиночная игра' : ($server ?: 'меню'));
        }
    } catch (Throwable $e) {
        if ($db->inTransaction()) $db->rollBack();
        error_log('fstweak mod.php: ' . $e->getMessage());
    }
}
$notes = $latest['notes'] ?? [];
json_out([
    'version' => (string)($latest['version'] ?? ''),
    'url' => (string)($latest['url'] ?? 'https://fspirat.ru/fstweak/'),
    'notes' => (string)($notes[$lang] ?? $notes['en'] ?? $notes['ru'] ?? ''),
]);
finish_response();
if ($newPlayer) notify('player', $newPlayer);
if ($db) stats_housekeeping($db);
