<?php
/**
 * API админ-панели. Работает только на VPS (fspirat.online), где лежит база;
 * страница админки (admin/index.html) открывается и с fspirat.ru, и с fspirat.online и ходит сюда.
 *   POST ?q=login   {"password": "..."}  → {"token": "..."}
 *   POST ?q=logout  (с токеном)
 *   GET  ?q=summary|live|events|visitor|audit  (с токеном: Authorization: Bearer ...)
 */

declare(strict_types=1);

define('FS_STATS', true);
require __DIR__ . '/../api/lib/stats.php';

if (!admin_host_ok() || !($db = stats_db())) { http_response_code(404); exit; }
admin_cors();
admin_headers();
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if ($method === 'OPTIONS') { http_response_code(204); exit; }

/** Откуда открыта админка — для журнала входов. */
function from_site(): string
{
    $o = parse_url($_SERVER['HTTP_ORIGIN'] ?? '', PHP_URL_HOST) ?: ($_SERVER['HTTP_HOST'] ?? '');
    return preg_replace('~^www\.~', '', (string)$o);
}

$q = $_GET['q'] ?? '';
if ($method === 'POST' && $q === 'login') {
    $in = json_decode((string)file_get_contents('php://input', false, null, 0, 2048), true);
    $pw = is_array($in) && is_string($in['password'] ?? null) ? $in['password'] : '';
    $ua = substr($_SERVER['HTTP_USER_AGENT'] ?? '', 0, 200);
    if ($wait = login_locked($db)) {
        audit($db, 'login_blocked', from_site() . ' · ' . $ua);
        json_out(['error' => 'locked', 'wait' => $wait], 429); exit;
    }
    if ($pw !== '' && password_verify($pw, stats_config()['password_hash'])) {
        audit($db, 'login_ok', from_site() . ' · ' . $ua);
        json_out(['token' => token_issue($db)]); exit;
    }
    audit($db, 'login_fail', from_site() . ' · ' . $ua);
    usleep(random_int(300000, 700000));   // замедляем подбор
    $st = $db->prepare("SELECT COUNT(*) FROM audit WHERE action = 'login_fail' AND ip = ? AND ts > ?");
    $st->execute([client_ip(), time() - LOGIN_LOCK_SEC]);
    $left = LOGIN_MAX_FAILS - (int)$st->fetchColumn();
    json_out($left > 0 ? ['error' => 'password', 'left' => $left] : ['error' => 'locked', 'wait' => LOGIN_LOCK_SEC], $left > 0 ? 403 : 429); exit;
}
if (!admin_logged_in($db)) { json_out(['error' => 'auth'], 401); exit; }
if ($method === 'POST' && $q === 'logout') { audit($db, 'logout', from_site()); token_revoke($db); json_out(['ok' => 1]); exit; }
if ($method !== 'GET') { json_out(['error' => 'method'], 405); exit; }

$off = (int)date('Z');   // смещение часового пояса (Москва) для группировки по дням

function host_filter(string $col, array &$args): string
{
    $h = $_GET['host'] ?? '';
    if (!in_array($h, ['fspirat.ru', 'fspirat.online'], true)) return '';
    $args[] = $h;
    return " AND $col = ?";
}

function rows(PDO $db, string $sql, array $args = []): array
{
    $st = $db->prepare($sql); $st->execute($args); return $st->fetchAll();
}
function one(PDO $db, string $sql, array $args = [])
{
    $st = $db->prepare($sql); $st->execute($args); return $st->fetchColumn();
}

/** Сводные числа за период [from, to). */
function totals(PDO $db, int $from, int $to): array
{
    $a = [$from, $to]; $hf = host_filter('host', $a);
    $s = rows($db, "SELECT COUNT(*) visits, COUNT(DISTINCT vid) visitors, SUM(visit = 1) fresh,
                    AVG(MIN(last_seen - started, 3600)) dur FROM sessions WHERE started >= ? AND started < ?$hf", $a)[0];
    $b = [$from, $to]; $hf = host_filter('host', $b);
    $views = (int)one($db, "SELECT COUNT(*) FROM events WHERE ev = 'pageview' AND ts >= ? AND ts < ?$hf", $b);
    return ['visitors' => (int)$s['visitors'], 'visits' => (int)$s['visits'], 'views' => $views,
            'fresh' => (int)$s['fresh'], 'dur' => (int)round((float)$s['dur'])];
}

try {
    switch ($q) {
    case 'summary': {
        $days = (int)($_GET['days'] ?? 7);
        if (!in_array($days, [1, 7, 30, 90], true)) $days = 7;
        $now = time();
        $from = strtotime('today') - ($days - 1) * 86400;
        $span = $now - $from;
        $cur = totals($db, $from, $now + 1);
        $prev = totals($db, $from - $days * 86400, $now - $days * 86400 + 1);

        $a = [$now - 300]; $hf = host_filter('host', $a);
        $online = (int)one($db, "SELECT COUNT(DISTINCT vid) FROM sessions WHERE last_seen >= ?$hf", $a);

        // график: по часам за сегодня, иначе по дням
        $fmt = $days === 1 ? '%H' : '%Y-%m-%d';
        $a = [$from]; $hf = host_filter('host', $a);
        $vis = rows($db, "SELECT strftime('$fmt', started + $off, 'unixepoch') k, COUNT(DISTINCT vid) n FROM sessions WHERE started >= ?$hf GROUP BY k", $a);
        $a = [$from]; $hf = host_filter('host', $a);
        $pv = rows($db, "SELECT strftime('$fmt', ts + $off, 'unixepoch') k, COUNT(*) n FROM events WHERE ev = 'pageview' AND ts >= ?$hf GROUP BY k", $a);
        $series = [];
        if ($days === 1) for ($h = 0; $h <= (int)date('G'); $h++) $series[sprintf('%02d', $h)] = [0, 0];
        else for ($t = $from; $t <= $now; $t += 86400) $series[date('Y-m-d', $t)] = [0, 0];
        foreach ($vis as $r) if (isset($series[$r['k']])) $series[$r['k']][0] = (int)$r['n'];
        foreach ($pv as $r) if (isset($series[$r['k']])) $series[$r['k']][1] = (int)$r['n'];

        $group = function (string $col, string $table = 'sessions', string $where = '', int $limit = 10) use ($db, $from) {
            $tcol = $table === 'sessions' ? 'started' : 'ts';
            $a = [$from]; $hf = host_filter('host', $a);
            $cnt = $table === 'sessions' ? 'COUNT(*)' : 'COUNT(*)';
            return rows($db, "SELECT $col k, $cnt n, COUNT(DISTINCT vid) u FROM $table WHERE $tcol >= ?$hf $where
                              GROUP BY k HAVING k IS NOT NULL AND k != '' ORDER BY n DESC LIMIT $limit", $a);
        };
        json_out([
            'days' => $days, 'cur' => $cur, 'prev' => $prev, 'online' => $online, 'span' => $span,
            'series' => array_map(fn($k, $v) => [$k, $v[0], $v[1]], array_keys($series), $series),
            'tables' => [
                'pages'     => $group('path', 'events', "AND ev = 'pageview'"),
                'sources'   => $group('source'),
                'devices'   => $group('device'),
                'browsers'  => $group('browser'),
                'os'        => $group('os'),
                'hosts'     => $group('host'),
                'actions'   => $group('ev', 'events', "AND ev NOT IN ('pageview')", 20),
                'formats'   => $group("json_extract(data, '$.fmt')", 'events', "AND ev = 'gen.copy'"),
                'palettes'  => $group("json_extract(data, '$.name')", 'events', "AND ev = 'gen.palette'"),
                'downloads' => $group("json_extract(data, '$.file')", 'events', "AND ev = 'download'"),
                'links'     => $group("json_extract(data, '$.to')", 'events', "AND ev = 'link'"),
            ],
        ]);
        break;
    }
    case 'live': {
        $a = [time() - 300]; $hf = host_filter('host', $a);
        $list = rows($db, "SELECT s.sid, s.vid, s.host, s.last_path, s.started, s.last_seen, s.device, s.browser, s.os, s.source, s.n, s.visit,
                  (SELECT ev || '|' || IFNULL(data, '') FROM events e WHERE e.sid = s.sid ORDER BY e.id DESC LIMIT 1) last_ev
                  FROM sessions s WHERE s.last_seen >= ?$hf ORDER BY s.last_seen DESC LIMIT 200", $a);
        foreach ($list as &$r) {
            [$ev, $data] = array_pad(explode('|', (string)$r['last_ev'], 2), 2, '');
            $r['last_ev'] = $ev; $r['last_data'] = $data !== '' ? json_decode($data, true) : null;
            unset($r['sid']);
        }
        json_out(['now' => time(), 'list' => $list]);
        break;
    }
    case 'events': {
        $where = []; $a = [];
        if (preg_match('/^\d{4}-\d{2}-\d{2}$/', $_GET['day'] ?? '')) {
            $d0 = strtotime($_GET['day'] . ' 00:00:00');
            $where[] = 'e.ts >= ? AND e.ts < ?'; $a[] = $d0; $a[] = $d0 + 86400;
        }
        if (preg_match('/^[a-z][a-z0-9_.:-]{0,39}$/', $_GET['ev'] ?? '')) { $where[] = 'e.ev = ?'; $a[] = $_GET['ev']; }
        else $where[] = "e.ev != 'ping'";
        if (($p = trim((string)($_GET['path'] ?? ''))) !== '') { $where[] = "e.path LIKE ? ESCAPE '\\'"; $a[] = addcslashes(mb_substr($p, 0, 200), '%_\\') . '%'; }
        if (preg_match('/^[a-f0-9]{1,16}$/', strtolower(ltrim($_GET['vid'] ?? '', '#')))) { $where[] = 'e.vid LIKE ?'; $a[] = strtolower(ltrim($_GET['vid'], '#')) . '%'; }
        if ((int)($_GET['before'] ?? 0) > 0) { $where[] = 'e.id < ?'; $a[] = (int)$_GET['before']; }
        $hf = host_filter('e.host', $a);
        $list = rows($db, 'SELECT e.id, e.ts, e.vid, e.host, e.path, e.ev, e.data, s.device, s.browser, s.os, s.source, s.visit
                  FROM events e LEFT JOIN sessions s ON s.sid = e.sid WHERE ' . implode(' AND ', $where) . "$hf ORDER BY e.id DESC LIMIT 60", $a);
        foreach ($list as &$r) $r['data'] = $r['data'] ? json_decode($r['data'], true) : null;
        json_out(['list' => $list, 'types' => array_column(rows($db, 'SELECT DISTINCT ev FROM events ORDER BY ev'), 'ev')]);
        break;
    }
    case 'visitor': {
        $vid = strtolower((string)($_GET['vid'] ?? ''));
        if (!preg_match('/^[a-f0-9]{16}$/', $vid)) { json_out(['error' => 'bad'], 400); break; }
        $v = rows($db, 'SELECT * FROM visitors WHERE vid = ?', [$vid])[0] ?? null;
        if (!$v) { json_out(['error' => 'notfound'], 404); break; }
        $sessions = rows($db, 'SELECT sid, host, entry, started, last_seen, source, device, browser, os, n, visit FROM sessions WHERE vid = ? ORDER BY started DESC LIMIT 60', [$vid]);
        $events = rows($db, "SELECT id, ts, sid, host, path, ev, data FROM events WHERE vid = ? ORDER BY id DESC LIMIT 500", [$vid]);
        foreach ($events as &$r) $r['data'] = $r['data'] ? json_decode($r['data'], true) : null;
        // номера визитов вместо внутренних ключей сессий
        $num = []; foreach ($sessions as $s) $num[$s['sid']] = (int)$s['visit'];
        foreach ($events as &$r) { $r['visit'] = $num[$r['sid']] ?? null; unset($r['sid']); }
        foreach ($sessions as &$s) unset($s['sid']);
        json_out(['visitor' => $v, 'sessions' => $sessions, 'events' => $events]);
        break;
    }
    case 'server': {
        // Краткое состояние VPS: процессор, память, диск. Читается из /proc (Linux), ничего не запускается.
        $cpuTimes = function () {
            $l = @file('/proc/stat', FILE_IGNORE_NEW_LINES)[0] ?? '';
            $p = array_map('intval', preg_split('/\s+/', trim(substr($l, 3))));
            return count($p) >= 4 ? [array_sum($p), ($p[3] ?? 0) + ($p[4] ?? 0)] : null;   // всего, простой (idle+iowait)
        };
        $a = $cpuTimes(); usleep(250000); $b = $cpuTimes();
        $cpu = ($a && $b && $b[0] > $a[0]) ? round(100 * (1 - ($b[1] - $a[1]) / ($b[0] - $a[0])), 1) : null;
        $mem = [];
        foreach (@file('/proc/meminfo', FILE_IGNORE_NEW_LINES) ?: [] as $l)
            if (preg_match('/^(\w+):\s+(\d+)/', $l, $m)) $mem[$m[1]] = (int)$m[2] * 1024;
        $cpuinfo = (string)@file_get_contents('/proc/cpuinfo');
        preg_match('/^model name\s*:\s*(.+)$/m', $cpuinfo, $mm);
        $os = '';
        if (preg_match('/^PRETTY_NAME="?([^"\n]+)/m', (string)@file_get_contents('/etc/os-release'), $om)) $os = $om[1];
        $load = array_map('floatval', array_slice(explode(' ', (string)@file_get_contents('/proc/loadavg')), 0, 3));
        $up = (int)(float)@file_get_contents('/proc/uptime');
        $dbFile = stats_dir() . '/stats.sqlite';
        json_out([
            'cpu' => ['usage' => $cpu, 'cores' => max(1, preg_match_all('/^processor\s*:/m', $cpuinfo)), 'model' => trim($mm[1] ?? ''), 'load' => $load],
            'ram' => ['total' => $mem['MemTotal'] ?? null, 'available' => $mem['MemAvailable'] ?? null],
            'swap' => ['total' => $mem['SwapTotal'] ?? 0, 'free' => $mem['SwapFree'] ?? 0],
            'disk' => ['total' => @disk_total_space('/') ?: null, 'free' => @disk_free_space('/') ?: null],
            'uptime' => $up, 'os' => $os, 'php' => PHP_VERSION,
            'db' => (@filesize($dbFile) ?: 0) + (@filesize($dbFile . '-wal') ?: 0),
        ]);
        break;
    }
    case 'audit':
        json_out(['list' => rows($db, 'SELECT ts, ip, action, detail FROM audit ORDER BY id DESC LIMIT 300')]);
        break;
    default:
        json_out(['error' => 'unknown'], 400);
    }
} catch (Throwable $e) {
    error_log('fspirat-stats admin: ' . $e->getMessage());
    json_out(['error' => 'server'], 500);
}
