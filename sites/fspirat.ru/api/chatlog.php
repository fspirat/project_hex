<?php
/**
 * Чат-логи мода FSLOG.
 *   POST          — мод присылает сообщения чата → {"id": "...", "url": "https://fspirat.online/log/?id=..."}
 *   GET ?id=...   — страница log/ получает лог для показа
 * Хранится в той же базе, что и статистика (/var/lib/fspirat-stats), 30 дней.
 * IP загрузившего не сохраняется — только его хэш с секретом для ограничения частоты.
 */

declare(strict_types=1);

define('FS_STATS', true);
require __DIR__ . '/lib/stats.php';

const LOG_KEEP_DAYS = 30;
const LOG_MAX_MESSAGES = 5000;
const LOG_MAX_BODY = 4 * 1024 * 1024;
const LOG_URL = 'https://fspirat.online/log/?id=';

header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store');
header('X-Robots-Tag: noindex, nofollow');

// Страницы сайта (fspirat.ru / fspirat.online) могут читать логи; чужим сайтам — отказ.
$origin = $_SERVER['HTTP_ORIGIN'] ?? '';
$extra = array_filter(explode(',', getenv('FSPIRAT_STATS_ORIGINS') ?: ''));
if ($origin !== '') {
    if (!in_array($origin, array_merge(TRACK_ORIGINS, $extra), true)) { json_out(['error' => 'origin'], 403); exit; }
    header('Access-Control-Allow-Origin: ' . $origin);
    header('Vary: Origin');
}

$db = stats_db();
if (!$db) { json_out(['error' => 'server'], 503); exit; }
chatlog_table($db);

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

if ($method === 'GET') {
    $id = (string)($_GET['id'] ?? '');
    if (!preg_match('/^[A-Za-z0-9]{10}$/', $id)) { json_out(['error' => 'notfound'], 404); exit; }
    $st = $db->prepare('SELECT * FROM chatlogs WHERE id = ? AND created >= ?');
    $st->execute([$id, time() - LOG_KEEP_DAYS * 86400]);
    $row = $st->fetch();
    if (!$row) { json_out(['error' => 'notfound'], 404); exit; }
    $db->prepare('UPDATE chatlogs SET views = views + 1 WHERE id = ?')->execute([$id]);
    $raw = $row['data'];
    if (function_exists('gzuncompress') && ($u = @gzuncompress($raw)) !== false) $raw = $u;
    json_out([
        'id' => $row['id'], 'created' => (int)$row['created'], 'expires' => (int)$row['created'] + LOG_KEEP_DAYS * 86400,
        'player' => $row['player'], 'server' => $row['server'], 'mc' => $row['mc'], 'n' => (int)$row['n'],
        'messages' => json_decode($raw, true) ?: [],
    ]);
    exit;
}

if ($method !== 'POST') { json_out(['error' => 'method'], 405); exit; }

// --- приём лога от мода ---
$cfg = stats_config();
$ipKey = 'cl:' . substr(hash('sha256', $cfg['secret'] . client_ip()), 0, 20);
foreach ([[6, 600], [40, 86400]] as [$max, $win]) {
    if (!stats_rate($db, $ipKey . ':' . $win, $max, $win)) {
        json_out(['error' => 'rate', 'wait' => $win - time() % $win], 429); exit;
    }
}

$body = file_get_contents('php://input', false, null, 0, LOG_MAX_BODY + 1);
if ($body === false || strlen($body) > LOG_MAX_BODY) { json_out(['error' => 'size'], 413); exit; }
$in = json_decode($body, true);
if (!is_array($in) || ($in['v'] ?? 0) !== 1 || !is_array($in['messages'] ?? null)) { json_out(['error' => 'server'], 400); exit; }
if (count($in['messages']) > LOG_MAX_MESSAGES) { json_out(['error' => 'size'], 413); exit; }

$str = fn($v, int $len) => is_string($v) ? mb_substr(preg_replace('/[\x00-\x08\x0B-\x1F\x7F]/u', '', $v) ?? '', 0, $len) : '';
$messages = [];
$servers = [];
foreach ($in['messages'] as $m) {
    if (!is_array($m) || !is_array($m['s'] ?? null)) continue;
    $segs = []; $chars = 0;
    foreach (array_slice($m['s'], 0, 80) as $s) {
        if (!is_array($s) || count($s) < 3 || !is_string($s[0])) continue;
        $text = $str($s[0], 2000 - $chars);
        if ($text === '') continue;
        $chars += mb_strlen($text);
        $color = is_int($s[1]) && $s[1] >= -1 && $s[1] <= 0xFFFFFF ? $s[1] : -1;
        $flags = is_int($s[2]) ? $s[2] & 31 : 0;
        $segs[] = [$text, $color, $flags];
        if ($chars >= 2000) break;
    }
    if (!$segs) continue;
    $srv = $str($m['srv'] ?? '', 100);
    $servers[$srv] = ($servers[$srv] ?? 0) + 1;
    $t = is_int($m['t'] ?? null) ? $m['t'] : 0;
    $messages[] = ['t' => $t, 'p' => !empty($m['p']) ? 1 : 0, 'srv' => $srv, 's' => $segs];
}
if (!$messages) { json_out(['error' => 'server'], 400); exit; }

arsort($servers);
$server = (string)array_key_first($servers);
$player = preg_match('/^[A-Za-z0-9_]{1,16}$/', (string)($in['player'] ?? '')) ? $in['player'] : '';
$json = json_encode($messages, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
$data = function_exists('gzcompress') ? gzcompress($json, 6) : $json;

$alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
for ($try = 0; $try < 5; $try++) {
    $id = '';
    for ($i = 0; $i < 10; $i++) $id .= $alphabet[random_int(0, strlen($alphabet) - 1)];
    try {
        $st = $db->prepare('INSERT INTO chatlogs(id, created, ip, player, server, mc, mod, n, bytes, data) VALUES(?,?,?,?,?,?,?,?,?,?)');
        $st->bindValue(1, $id); $st->bindValue(2, time(), PDO::PARAM_INT); $st->bindValue(3, $ipKey);
        $st->bindValue(4, $player); $st->bindValue(5, $server); $st->bindValue(6, $str($in['mc'] ?? '', 20));
        $st->bindValue(7, $str($in['mod'] ?? '', 20)); $st->bindValue(8, count($messages), PDO::PARAM_INT);
        $st->bindValue(9, strlen($json), PDO::PARAM_INT); $st->bindValue(10, $data, PDO::PARAM_LOB);
        $st->execute();
        break;
    } catch (PDOException $e) {
        if ($try === 4) { error_log('fslog: ' . $e->getMessage()); json_out(['error' => 'server'], 500); exit; }
    }
}

if (random_int(1, 20) === 1) {
    $db->prepare('DELETE FROM chatlogs WHERE created < ?')->execute([time() - LOG_KEEP_DAYS * 86400]);
}
json_out(['id' => $id, 'url' => LOG_URL . $id, 'n' => count($messages)]);
