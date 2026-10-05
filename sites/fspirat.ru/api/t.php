<?php
/**
 * Приём событий статистики с fspirat.ru и fspirat.online (js/stats.js шлёт их через sendBeacon).
 * Всегда отвечает 204 без тела — посторонним нечего узнать из ответа.
 * Без cookies; посетитель — случайный номер из браузера, IP не сохраняется.
 */

declare(strict_types=1);

define('FS_STATS', true);
require __DIR__ . '/lib/stats.php';

function done(): void { http_response_code(204); exit; }

$origin = $_SERVER['HTTP_ORIGIN'] ?? '';
$extraOrigins = array_filter(explode(',', getenv('FSPIRAT_STATS_ORIGINS') ?: ''));
$allowed = array_merge(TRACK_ORIGINS, $extraOrigins);
if ($origin === '') {
    // Некоторые браузеры не шлют Origin для запросов на свой же сайт — тогда смотрим Referer.
    $ref = $_SERVER['HTTP_REFERER'] ?? '';
    $p = parse_url($ref);
    if (!empty($p['scheme']) && !empty($p['host'])) $origin = $p['scheme'] . '://' . $p['host'] . (isset($p['port']) ? ':' . $p['port'] : '');
}
if (!in_array($origin, $allowed, true)) done();
header('Access-Control-Allow-Origin: ' . $origin);
header('Vary: Origin');
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') done();

$ua = substr($_SERVER['HTTP_USER_AGENT'] ?? '', 0, 400);
if (is_bot($ua)) done();

$raw = file_get_contents('php://input', false, null, 0, 4097);
if ($raw === false || strlen($raw) > 4096) done();
$in = json_decode($raw, true);
if (!is_array($in)) done();

$vid = is_string($in['v'] ?? null) ? $in['v'] : '';
$sid = is_string($in['s'] ?? null) ? $in['s'] : '';
$ev = is_string($in['e'] ?? null) ? $in['e'] : '';
if (!preg_match('/^[a-f0-9]{16}$/', $vid) || !preg_match('/^[a-f0-9]{16}$/', $sid)) done();
if (!preg_match('/^[a-z][a-z0-9_.:-]{0,39}$/', $ev)) done();

$path = is_string($in['p'] ?? null) ? $in['p'] : '/';
$path = '/' . ltrim(mb_substr(preg_replace('/[?#].*$/s', '', $path), 0, 200), '/');
$host = strtolower(preg_replace('~^https?://~', '', $origin));
$host = preg_replace('~^www\.~', '', $host);

// Дополнительные данные события: до 12 простых полей, строки до 80 символов
$data = null;
if (isset($in['d']) && is_array($in['d'])) {
    $d = [];
    foreach ($in['d'] as $k => $v) {
        if (count($d) >= 12 || !is_string($k) || !preg_match('/^[a-z0-9_]{1,24}$/', $k)) continue;
        if (is_bool($v)) $v = $v ? 1 : 0;
        if (is_int($v) || is_float($v)) $d[$k] = $v;
        elseif (is_string($v)) $d[$k] = mb_substr($v, 0, 80);
    }
    if ($d) $data = json_encode($d, JSON_UNESCAPED_UNICODE);
}

$db = stats_db();
if (!$db) done();
$cfg = stats_config();

try {
    // Защита от флуда: по IP (хэш с секретом, меняется каждый день — сам IP не хранится) и по сессии
    $ipKey = 'ip:' . substr(hash('sha256', $cfg['secret'] . date('Y-m-d') . client_ip()), 0, 20);
    if (!stats_rate($db, $ipKey, 240, 60) || !stats_rate($db, 's:' . $sid, 3000, 86400)) done();

    $now = time();
    $db->beginTransaction();

    $st = $db->prepare('SELECT vid FROM sessions WHERE sid = ?');
    $st->execute([$sid]);
    $sess = $st->fetch();

    if ($ev === 'ping') {
        // «я ещё на сайте» — только продлеваем сессию, в журнал не пишем
        if ($sess) $db->prepare('UPDATE sessions SET last_seen = ? WHERE sid = ?')->execute([$now, $sid]);
        $db->commit();
        done();
    }

    if (!$sess) {
        [$device, $browser, $os] = parse_ua($ua);
        $refHost = null;
        if (is_string($in['r'] ?? null) && $in['r'] !== '') {
            $rh = parse_url($in['r'], PHP_URL_HOST);
            if (is_string($rh)) {
                $rh = strtolower(preg_replace('~^www\.~', '', $rh));
                $own = ['fspirat.ru', 'fspirat.online'];
                if (!in_array($rh, $own, true)) $refHost = mb_substr($rh, 0, 100);
            }
        }
        $utm = is_string($in['u'] ?? null) ? mb_substr($in['u'], 0, 40) : null;
        $source = classify_source($refHost, $utm);

        $db->prepare('INSERT INTO visitors(vid, first_seen, last_seen, visits, source, device, browser, os) VALUES(?,?,?,1,?,?,?,?)
                      ON CONFLICT(vid) DO UPDATE SET last_seen = excluded.last_seen, visits = visits + 1,
                      device = excluded.device, browser = excluded.browser, os = excluded.os')
           ->execute([$vid, $now, $now, $source, $device, $browser, $os]);
        $st = $db->prepare('SELECT visits FROM visitors WHERE vid = ?'); $st->execute([$vid]);
        $visit = (int)$st->fetchColumn();
        $db->prepare('INSERT INTO sessions(sid, vid, started, last_seen, host, entry, last_path, ref, source, device, browser, os, n, visit)
                      VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0,?)')
           ->execute([$sid, $vid, $now, $now, $host, $path, $path, $refHost, $source, $device, $browser, $os, $visit]);
    } elseif ($sess['vid'] !== $vid) {
        $db->rollBack();
        done();
    }

    $db->prepare('UPDATE sessions SET last_seen = ?, n = n + 1' . ($ev === 'pageview' ? ', last_path = ?' : '') . ' WHERE sid = ?')
       ->execute($ev === 'pageview' ? [$now, $path, $sid] : [$now, $sid]);
    $db->prepare('UPDATE visitors SET last_seen = ? WHERE vid = ?')->execute([$now, $vid]);
    $db->prepare('INSERT INTO events(ts, vid, sid, host, path, ev, data) VALUES(?,?,?,?,?,?,?)')
       ->execute([$now, $vid, $sid, $host, $path, $ev, $data]);
    $db->commit();

    if (random_int(1, 300) === 1) stats_cleanup($db);
} catch (Throwable $e) {
    if ($db->inTransaction()) $db->rollBack();
    error_log('fspirat-stats: ' . $e->getMessage());
}
done();
