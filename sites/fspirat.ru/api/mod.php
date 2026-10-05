<?php
/**
 * FSTWEAK: мод сообщает, кто в игре (ник, версии, сервер), и узнаёт последнюю версию.
 *   POST {"nick":"...","mod":"1.2","mc":"1.21.11","server":"...","lang":"ru_ru"}
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

$in = json_decode((string)file_get_contents('php://input', false, null, 0, 2048), true);
if (!is_array($in)) answer($latest, 'ru');
$str = fn($v, int $len) => is_string($v) ? mb_substr(preg_replace('/[^\P{C}]/u', '', $v) ?? '', 0, $len) : '';
$lang = substr($str($in['lang'] ?? '', 10), 0, 2) ?: 'ru';

$db = stats_db();
$nick = (string)($in['nick'] ?? '');
if ($db && preg_match('/^[A-Za-z0-9_]{1,16}$/', $nick)) {
    try {
        $db->exec('CREATE TABLE IF NOT EXISTS mod_players(
          nick TEXT PRIMARY KEY, first_seen INTEGER NOT NULL, last_seen INTEGER NOT NULL, joins INTEGER NOT NULL DEFAULT 0,
          mod TEXT, mc TEXT, server TEXT, lang TEXT)');
        $cfg = stats_config();
        $key = 'mod:' . substr(hash('sha256', $cfg['secret'] . client_ip()), 0, 20);
        if (stats_rate($db, $key, 60, 3600)) {
            $join = !empty($in['join']) ? 1 : 0;   // заход в мир (а не просто «я ещё играю»)
            $db->prepare('INSERT INTO mod_players(nick, first_seen, last_seen, joins, mod, mc, server, lang) VALUES(?,?,?,?,?,?,?,?)
                          ON CONFLICT(nick) DO UPDATE SET last_seen = excluded.last_seen, joins = joins + excluded.joins,
                          mod = excluded.mod, mc = excluded.mc, server = excluded.server, lang = excluded.lang')
               ->execute([$nick, time(), time(), $join, $str($in['mod'] ?? '', 20), $str($in['mc'] ?? '', 20),
                          $str($in['server'] ?? '', 100), $str($in['lang'] ?? '', 10)]);
        }
    } catch (Throwable $e) {
        error_log('fstweak mod.php: ' . $e->getMessage());
    }
}
answer($latest, $lang);
