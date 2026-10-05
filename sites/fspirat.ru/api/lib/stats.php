<?php
/**
 * Статистика fspirat: общие функции для приёма событий (api/t.php) и админ-панели (admin/).
 *
 * Данные лежат ВНЕ папки сайта (деплой через rsync --delete их не тронет):
 *   /var/lib/fspirat-stats/stats.sqlite  — база
 *   /var/lib/fspirat-stats/config.json   — хэш пароля админки и секрет
 * Папку и пароль создаёт один раз команда на сервере:  sudo -u www-data php api/lib/setup.php
 *
 * Посетители анонимные: случайный номер из браузера, IP не сохраняется.
 * IP пишется только в журнал входов в админку (защита от подбора пароля).
 */

declare(strict_types=1);

if (!defined('FS_STATS')) { http_response_code(404); exit; }

const STATS_KEEP_DAYS = 90;
const STATS_TZ = 'Europe/Moscow';
const TRACK_ORIGINS = [
    'https://fspirat.ru', 'https://www.fspirat.ru',
    'https://fspirat.online', 'https://www.fspirat.online',
];
const ADMIN_HOSTS = ['fspirat.online', 'www.fspirat.online'];

date_default_timezone_set(STATS_TZ);

function stats_dir(): string
{
    return rtrim(getenv('FSPIRAT_STATS_DIR') ?: '/var/lib/fspirat-stats', '/');
}

function stats_config(): ?array
{
    static $cfg = false;
    if ($cfg === false) {
        $raw = @file_get_contents(stats_dir() . '/config.json');
        $cfg = $raw === false ? null : json_decode($raw, true);
        if (!is_array($cfg) || empty($cfg['password_hash']) || empty($cfg['secret'])) $cfg = null;
    }
    return $cfg;
}

function stats_db(): ?PDO
{
    static $db = null;
    if ($db) return $db;
    $file = stats_dir() . '/stats.sqlite';
    if (!is_dir(stats_dir()) || !stats_config()) return null;
    try {
        $db = new PDO('sqlite:' . $file, null, null, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]);
        $db->exec('PRAGMA journal_mode=WAL; PRAGMA busy_timeout=4000; PRAGMA synchronous=NORMAL;');
        stats_schema($db);
    } catch (Throwable $e) {
        error_log('fspirat-stats: ' . $e->getMessage());
        $db = null;
    }
    return $db;
}

function stats_schema(PDO $db): void
{
    $db->exec(<<<SQL
CREATE TABLE IF NOT EXISTS events(
  id INTEGER PRIMARY KEY, ts INTEGER NOT NULL, vid TEXT NOT NULL, sid TEXT NOT NULL,
  host TEXT, path TEXT, ev TEXT NOT NULL, data TEXT);
CREATE INDEX IF NOT EXISTS ev_ts ON events(ts);
CREATE INDEX IF NOT EXISTS ev_vid ON events(vid, ts);
CREATE INDEX IF NOT EXISTS ev_name ON events(ev, ts);
CREATE TABLE IF NOT EXISTS sessions(
  sid TEXT PRIMARY KEY, vid TEXT NOT NULL, started INTEGER NOT NULL, last_seen INTEGER NOT NULL,
  host TEXT, entry TEXT, last_path TEXT, ref TEXT, source TEXT, device TEXT, browser TEXT, os TEXT,
  n INTEGER NOT NULL DEFAULT 0, visit INTEGER NOT NULL DEFAULT 1);
CREATE INDEX IF NOT EXISTS s_started ON sessions(started);
CREATE INDEX IF NOT EXISTS s_last ON sessions(last_seen);
CREATE INDEX IF NOT EXISTS s_vid ON sessions(vid);
CREATE TABLE IF NOT EXISTS visitors(
  vid TEXT PRIMARY KEY, first_seen INTEGER NOT NULL, last_seen INTEGER NOT NULL,
  visits INTEGER NOT NULL DEFAULT 0, source TEXT, device TEXT, browser TEXT, os TEXT);
CREATE INDEX IF NOT EXISTS v_first ON visitors(first_seen);
CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY, ts INTEGER NOT NULL, ip TEXT, action TEXT NOT NULL, detail TEXT);
CREATE INDEX IF NOT EXISTS a_ts ON audit(ts);
CREATE TABLE IF NOT EXISTS rl(k TEXT PRIMARY KEY, win INTEGER NOT NULL, n INTEGER NOT NULL);
SQL);
}

/** Таблица чат-логов мода FSLOG (api/chatlog.php, вкладка «Чатлоги» в админке). */
function chatlog_table(PDO $db): void
{
    $db->exec('CREATE TABLE IF NOT EXISTS chatlogs(
      id TEXT PRIMARY KEY, created INTEGER NOT NULL, ip TEXT, player TEXT, server TEXT, mc TEXT, mod TEXT,
      n INTEGER NOT NULL, bytes INTEGER NOT NULL, data BLOB NOT NULL, views INTEGER NOT NULL DEFAULT 0)');
    $db->exec('CREATE INDEX IF NOT EXISTS cl_created ON chatlogs(created)');
}

/** Удаление данных старше 90 дней. */
function stats_cleanup(PDO $db): void
{
    $cut = time() - STATS_KEEP_DAYS * 86400;
    $db->prepare('DELETE FROM events WHERE ts < ?')->execute([$cut]);
    $db->prepare('DELETE FROM sessions WHERE last_seen < ?')->execute([$cut]);
    $db->prepare('DELETE FROM visitors WHERE last_seen < ?')->execute([$cut]);
    $db->prepare('DELETE FROM audit WHERE ts < ?')->execute([$cut]);
    $db->prepare('DELETE FROM rl WHERE win < ?')->execute([time() - 3600]);
}

/** Ограничение частоты: не больше $max событий за $window секунд по ключу. */
function stats_rate(PDO $db, string $key, int $max, int $window): bool
{
    $win = intdiv(time(), $window) * $window;
    $db->prepare('INSERT INTO rl(k,win,n) VALUES(?,?,1) ON CONFLICT(k) DO UPDATE SET n = CASE WHEN win = excluded.win THEN n + 1 ELSE 1 END, win = excluded.win')
       ->execute([$key, $win]);
    $st = $db->prepare('SELECT n FROM rl WHERE k = ?'); $st->execute([$key]);
    return (int)$st->fetchColumn() <= $max;
}

function client_ip(): string
{
    return $_SERVER['REMOTE_ADDR'] ?? '';
}

// ---------------------------------------------------------------------------
// Разбор User-Agent и источника перехода

function is_bot(string $ua): bool
{
    return $ua === '' || (bool)preg_match('~bot|crawl|spider|slurp|headless|lighthouse|preview|facebookexternalhit|curl|wget|python|httpclient|java/|go-http|okhttp|monitor|uptime|scan~i', $ua);
}

function parse_ua(string $ua): array
{
    $device = preg_match('~iPad|Tablet|Tab(?!le)|Kindle|SM-T~i', $ua) ? 'Планшет'
        : (preg_match('~Mobi|Android|iPhone|iPod|Phone~i', $ua) ? 'Телефон' : 'ПК');
    $browser = 'Другой';
    foreach ([
        'Яндекс Браузер' => '~YaBrowser~', 'Edge' => '~Edg/~', 'Opera' => '~OPR/|Opera~', 'Samsung' => '~SamsungBrowser~',
        'Firefox' => '~Firefox/|FxiOS~', 'Chrome' => '~Chrome/|CriOS~', 'Safari' => '~Safari/~',
    ] as $name => $re) {
        if (preg_match($re, $ua)) { $browser = $name; break; }
    }
    $os = 'Другая';
    foreach ([
        'Android' => '~Android~', 'iOS' => '~iPhone|iPad|iPod~', 'Windows' => '~Windows~',
        'macOS' => '~Mac OS X|Macintosh~', 'Linux' => '~Linux|X11~',
    ] as $name => $re) {
        if (preg_match($re, $ua)) { $os = $name; break; }
    }
    return [$device, $browser, $os];
}

/** Откуда пришёл посетитель: метка utm_source или домен ссылки. */
function classify_source(?string $refHost, ?string $utm): string
{
    if ($utm) {
        $u = strtolower($utm);
        foreach (['telegram' => 'Telegram', 'tg' => 'Telegram', 'discord' => 'Discord', 'vk' => 'ВКонтакте', 'youtube' => 'YouTube', 'twitch' => 'Twitch'] as $k => $v)
            if (str_contains($u, $k)) return $v;
        return 'Метка: ' . mb_substr($utm, 0, 30);
    }
    if (!$refHost) return 'Прямой заход';
    $h = strtolower(preg_replace('~^www\.~', '', $refHost));
    $map = [
        'Telegram' => '~(^|\.)(t\.me|telegram\.org|telegram\.me)$~',
        'Discord' => '~(^|\.)(discord\.com|discord\.gg|discordapp\.com)$~',
        'Google' => '~(^|\.)google\.[a-z.]+$~',
        'Яндекс' => '~(^|\.)(yandex\.[a-z.]+|ya\.ru)$~',
        'ВКонтакте' => '~(^|\.)(vk\.com|vk\.ru|vk\.cc)$~',
        'YouTube' => '~(^|\.)(youtube\.com|youtu\.be)$~',
        'Twitch' => '~(^|\.)twitch\.tv$~',
        'GitHub' => '~(^|\.)github\.(com|io)$~',
        'Bing' => '~(^|\.)bing\.com$~',
        'DuckDuckGo' => '~(^|\.)duckduckgo\.com$~',
    ];
    foreach ($map as $name => $re) if (preg_match($re, $h)) return $name;
    return $h;
}

function json_out($data, int $code = 200): void
{
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    header('X-Content-Type-Options: nosniff');
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
}

// ---------------------------------------------------------------------------
// Админка: вход, журнал входов

function audit(PDO $db, string $action, string $detail = ''): void
{
    $db->prepare('INSERT INTO audit(ts, ip, action, detail) VALUES(?,?,?,?)')
       ->execute([time(), client_ip(), $action, mb_substr($detail, 0, 300)]);
}

function admin_host_ok(): bool
{
    $extra = array_filter(explode(',', getenv('FSPIRAT_STATS_ADMIN_HOSTS') ?: ''));
    $host = strtolower($_SERVER['HTTP_HOST'] ?? '');
    return in_array($host, array_merge(ADMIN_HOSTS, $extra), true);
}

/** Страница админки открыта с одного из наших сайтов — разрешаем ей обращаться к API; чужим сайтам — отказ. */
function admin_cors(): void
{
    $origin = $_SERVER['HTTP_ORIGIN'] ?? '';
    $extra = array_filter(explode(',', getenv('FSPIRAT_STATS_ORIGINS') ?: ''));
    $self = (($_SERVER['HTTPS'] ?? '') !== '' && ($_SERVER['HTTPS'] ?? '') !== 'off' ? 'https://' : 'http://') . ($_SERVER['HTTP_HOST'] ?? '');
    if ($origin !== '' && $origin !== $self && !in_array($origin, array_merge(TRACK_ORIGINS, $extra), true)) {
        http_response_code(403); exit;
    }
    if (in_array($origin, array_merge(TRACK_ORIGINS, $extra), true)) {
        header('Access-Control-Allow-Origin: ' . $origin);
        header('Access-Control-Allow-Methods: GET, POST');
        header('Access-Control-Allow-Headers: Authorization, Content-Type');
        header('Access-Control-Max-Age: 600');
    }
    header('Vary: Origin');
}

function admin_headers(): void
{
    header('X-Content-Type-Options: nosniff');
    header('Referrer-Policy: no-referrer');
    header('Cache-Control: no-store');
    header('X-Robots-Tag: noindex, nofollow');
}

/*
 * Вход по токену, а не по cookie: страница админки открывается и с fspirat.ru, и с fspirat.online,
 * а данные лежат на VPS. Cookie между разными сайтами браузеры режут, токен в заголовке — нет.
 * Токен хранится во вкладке (sessionStorage), в базе — только его хэш.
 * Живёт 12 часов, без действий — 2 часа; смена пароля (setup.php) завершает все входы.
 */
const TOKEN_TTL = 43200;
const TOKEN_IDLE = 7200;

function pw_version(): string
{
    return substr(hash('sha256', (string)(stats_config()['password_hash'] ?? '')), 0, 12);
}

function token_table(PDO $db): void
{
    $db->exec('CREATE TABLE IF NOT EXISTS tokens(hash TEXT PRIMARY KEY, created INTEGER NOT NULL, seen INTEGER NOT NULL, pwv TEXT NOT NULL, ip TEXT, ua TEXT)');
}

function token_issue(PDO $db): string
{
    token_table($db);
    $db->prepare('DELETE FROM tokens WHERE created < ? OR seen < ?')->execute([time() - TOKEN_TTL, time() - TOKEN_IDLE]);
    $tok = bin2hex(random_bytes(32));
    $db->prepare('INSERT INTO tokens(hash, created, seen, pwv, ip, ua) VALUES(?,?,?,?,?,?)')
       ->execute([hash('sha256', $tok), time(), time(), pw_version(), client_ip(), substr($_SERVER['HTTP_USER_AGENT'] ?? '', 0, 200)]);
    return $tok;
}

function bearer(): string
{
    $h = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
    return preg_match('/^Bearer ([a-f0-9]{64})$/', $h, $m) ? $m[1] : '';
}

/** Проверить токен из заголовка Authorization (и продлить его). */
function admin_logged_in(PDO $db): bool
{
    $tok = bearer();
    if ($tok === '') return false;
    token_table($db);
    $st = $db->prepare('SELECT created, seen, pwv FROM tokens WHERE hash = ?');
    $st->execute([hash('sha256', $tok)]);
    $r = $st->fetch();
    $now = time();
    if (!$r || $now - $r['created'] > TOKEN_TTL || $now - $r['seen'] > TOKEN_IDLE || !hash_equals($r['pwv'], pw_version())) return false;
    $db->prepare('UPDATE tokens SET seen = ? WHERE hash = ?')->execute([$now, hash('sha256', $tok)]);
    return true;
}

function token_revoke(PDO $db): void
{
    if (($tok = bearer()) !== '') { token_table($db); $db->prepare('DELETE FROM tokens WHERE hash = ?')->execute([hash('sha256', $tok)]); }
}

const LOGIN_MAX_FAILS = 5;
const LOGIN_LOCK_SEC = 900;

/** Сколько секунд ещё заблокирован вход с этого IP (0 — не заблокирован). */
function login_locked(PDO $db): int
{
    $st = $db->prepare("SELECT COUNT(*), MAX(ts) FROM audit WHERE action = 'login_fail' AND ip = ? AND ts > ?");
    $st->execute([client_ip(), time() - LOGIN_LOCK_SEC]);
    [$n, $last] = $st->fetch(PDO::FETCH_NUM);
    return (int)$n >= LOGIN_MAX_FAILS ? max(1, (int)$last + LOGIN_LOCK_SEC - time()) : 0;
}
