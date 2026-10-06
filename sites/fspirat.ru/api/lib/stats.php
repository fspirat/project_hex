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

// Ошибки PHP наших скриптов — в свой файл рядом с базой (его показывает вкладка «Сервис» в админке).
if (is_dir(stats_dir()) && is_writable(stats_dir())) ini_set('error_log', stats_dir() . '/php-errors.log');

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
CREATE TABLE IF NOT EXISTS meta(k TEXT PRIMARY KEY, v TEXT);
SQL);
}

/** Таблица чат-логов мода FSLOG (api/chatlog.php, вкладка «Чатлоги» в админке). */
function chatlog_table(PDO $db): void
{
    $db->exec('CREATE TABLE IF NOT EXISTS chatlogs(
      id TEXT PRIMARY KEY, created INTEGER NOT NULL, ip TEXT, player TEXT, server TEXT, mc TEXT, mod TEXT,
      n INTEGER NOT NULL, bytes INTEGER NOT NULL, data BLOB NOT NULL, views INTEGER NOT NULL DEFAULT 0)');
    $db->exec('CREATE INDEX IF NOT EXISTS cl_created ON chatlogs(created)');
    // «Закреплённый» лог не удаляется через 30 дней (для жалоб и споров) — колонка добавлена позже.
    $cols = array_column($db->query('PRAGMA table_info(chatlogs)')->fetchAll(), 'name');
    if (!in_array('pinned', $cols, true)) $db->exec('ALTER TABLE chatlogs ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0');
}

/** Игроки FSTWEAK (api/mod.php): последнее состояние, история версий и серверов, счётчики функций. */
function mod_tables(PDO $db): void
{
    $db->exec('CREATE TABLE IF NOT EXISTS mod_players(
      nick TEXT PRIMARY KEY, first_seen INTEGER NOT NULL, last_seen INTEGER NOT NULL, joins INTEGER NOT NULL DEFAULT 0,
      mod TEXT, mc TEXT, server TEXT, lang TEXT)');
    $db->exec('CREATE TABLE IF NOT EXISTS mod_seen(
      nick TEXT NOT NULL, mod TEXT NOT NULL, mc TEXT NOT NULL, server TEXT NOT NULL,
      first_seen INTEGER NOT NULL, last_seen INTEGER NOT NULL, PRIMARY KEY(nick, mod, mc, server))');
    $db->exec('CREATE TABLE IF NOT EXISTS mod_usage(day TEXT NOT NULL, k TEXT NOT NULL, n INTEGER NOT NULL, PRIMARY KEY(day, k))');
}

function meta_get(PDO $db, string $k): ?string
{
    $st = $db->prepare('SELECT v FROM meta WHERE k = ?'); $st->execute([$k]);
    $v = $st->fetchColumn();
    return $v === false ? null : (string)$v;
}

function meta_set(PDO $db, string $k, string $v): void
{
    $db->prepare('INSERT INTO meta(k, v) VALUES(?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v')->execute([$k, $v]);
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
    $db->prepare('DELETE FROM mod_usage WHERE day < ?')->execute([date('Y-m-d', $cut)]);
}

// ---------------------------------------------------------------------------
// Уведомления в Telegram. Настройки — в notify.json рядом с базой (токен бота наружу не отдаётся).

const NOTIFY_EVENTS = [
    'player'   => 'Новый игрок поставил FSTWEAK',
    'log'      => 'Кто-то сохранил чат командой /log',
    'disk'     => 'Диск VPS заполнен больше чем на 90%',
    'login'    => 'Вход в админку заблокирован (5 неверных паролей)',
    'login_ok' => 'Успешный вход в админку',
    'backup'   => 'Ежедневная копия базы — файлом в этот чат (на случай, если сервер пропадёт)',
];
const NOTIFY_DEFAULT = ['player' => true, 'log' => true, 'disk' => true, 'login' => true, 'login_ok' => false, 'backup' => true];
const NOTIFY_MAX_PER_HOUR = 10;   // защита от спама: не больше 10 уведомлений одного вида в час

function notify_cfg(): array
{
    $c = json_decode((string)@file_get_contents(stats_dir() . '/notify.json'), true);
    return is_array($c) ? $c : [];
}

function notify_store(array $c): bool
{
    $file = stats_dir() . '/notify.json';
    $tmp = $file . '.tmp';
    if (@file_put_contents($tmp, json_encode($c, JSON_UNESCAPED_UNICODE), LOCK_EX) === false) return false;
    @chmod($tmp, 0600);
    return @rename($tmp, $file);
}

/** Вызов Bot API. Null — нет связи или ответ не разобрать. */
function tg_call(string $token, string $method, array $params = [], int $timeout = 5): ?array
{
    $url = 'https://api.telegram.org/bot' . $token . '/' . $method;
    $body = http_build_query($params);
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        curl_setopt_array($ch, [CURLOPT_POST => true, CURLOPT_POSTFIELDS => $body, CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CONNECTTIMEOUT => $timeout, CURLOPT_TIMEOUT => $timeout]);
        $raw = curl_exec($ch);
        curl_close($ch);
    } else {
        $raw = @file_get_contents($url, false, stream_context_create(['http' => [
            'method' => 'POST', 'header' => 'Content-Type: application/x-www-form-urlencoded', 'content' => $body,
            'timeout' => $timeout, 'ignore_errors' => true]]));
    }
    $j = is_string($raw) ? json_decode($raw, true) : null;
    return is_array($j) ? $j : null;
}

/** Отправить уведомление, если оно включено. Ответ клиенту лучше отдать до вызова (fastcgi_finish_request). */
function notify(string $event, string $text): bool
{
    $c = notify_cfg();
    if (empty($c['token']) || empty($c['chat']) || !($c['events'][$event] ?? NOTIFY_DEFAULT[$event] ?? false)) return false;
    // кто угодно может слать запросы на api/ — без лимита бота можно было бы засыпать сообщениями
    if (($db = stats_db()) && !stats_rate($db, 'nt:' . $event, NOTIFY_MAX_PER_HOUR, 3600)) return false;
    $r = tg_call($c['token'], 'sendMessage', ['chat_id' => $c['chat'], 'text' => $text, 'disable_web_page_preview' => 'true']);
    if (!($r['ok'] ?? false)) error_log('fspirat notify ' . $event . ': ' . ($r['description'] ?? 'нет связи с api.telegram.org'));
    return (bool)($r['ok'] ?? false);
}

/** Отдать ответ и продолжить работу без ожидания клиента (php-fpm). */
function finish_response(): void
{
    if (function_exists('fastcgi_finish_request')) fastcgi_finish_request();
}

// ---------------------------------------------------------------------------
// Фоновые задачи раз в час: проверка диска и ежедневная резервная копия базы (хранится 7 штук).

const BACKUP_KEEP = 7;

function backup_dir(): string
{
    return stats_dir() . '/backups';
}

/**
 * Копия базы целиком (VACUUM INTO — согласованный снимок без остановки записи).
 * Через отдельное соединение: на основном могут быть незакрытые SELECT, и тогда SQLite
 * отказывает «cannot VACUUM - SQL statements in progress» (так копии не делались с 05.10).
 */
function backup_make(PDO $db, ?string $name = null): ?string
{
    $dir = backup_dir();
    if (!is_dir($dir) && !@mkdir($dir, 0700)) return null;
    $name = $name ?? 'stats-' . date('Y-m-d') . '.sqlite';
    $path = $dir . '/' . $name;
    if (is_file($path)) @unlink($path);
    $src = new PDO('sqlite:' . stats_dir() . '/stats.sqlite', null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
    $src->exec('PRAGMA busy_timeout=10000');
    $src->exec('VACUUM INTO ' . $src->quote($path));
    $src = null;
    @chmod($path, 0600);
    $all = glob($dir . '/stats-*.sqlite') ?: [];
    rsort($all);
    foreach (array_slice($all, BACKUP_KEEP) as $old) @unlink($old);
    return $path;
}

/** Копия базы — сжатым файлом в Telegram-чат уведомлений (копия вне сервера). */
function backup_send(string $path): bool
{
    $c = notify_cfg();
    if (empty($c['token']) || empty($c['chat']) || !($c['events']['backup'] ?? NOTIFY_DEFAULT['backup'])) return false;
    $gz = gzencode((string)file_get_contents($path), 9);
    if ($gz === false || strlen($gz) > 45 * 1048576) return false;
    $b = '----fspirat' . bin2hex(random_bytes(8));
    $fields = ['chat_id' => $c['chat'], 'caption' => '🗄 Копия базы fspirat за ' . date('d.m.Y') . ' (' . round(strlen($gz) / 1024) . ' КБ). Восстановить: распаковать .gz → /var/lib/fspirat-stats/stats.sqlite',
               'disable_notification' => 'true'];
    $body = '';
    foreach ($fields as $k => $v) $body .= "--$b\r\nContent-Disposition: form-data; name=\"$k\"\r\n\r\n$v\r\n";
    $body .= "--$b\r\nContent-Disposition: form-data; name=\"document\"; filename=\"" . basename($path) . ".gz\"\r\nContent-Type: application/gzip\r\n\r\n$gz\r\n--$b--\r\n";
    $raw = @file_get_contents('https://api.telegram.org/bot' . $c['token'] . '/sendDocument', false, stream_context_create(['http' => [
        'method' => 'POST', 'header' => "Content-Type: multipart/form-data; boundary=$b", 'content' => $body,
        'timeout' => 30, 'ignore_errors' => true]]));
    $ok = (bool)(json_decode((string)$raw, true)['ok'] ?? false);
    if (!$ok) error_log('fspirat backup_send: ' . substr((string)$raw, 0, 200));
    return $ok;
}

function stats_housekeeping(PDO $db): void
{
    $now = time();
    try {
        if ((int)meta_get($db, 'hk_last') > $now - 3600) return;
        meta_set($db, 'hk_last', (string)$now);
        $t = @disk_total_space('/'); $f = @disk_free_space('/');
        if ($t && $f !== false) {
            $pct = 100 * (1 - $f / $t);
            if ($pct >= 90 && (int)meta_get($db, 'disk_alert') < $now - 86400) {
                meta_set($db, 'disk_alert', (string)$now);
                notify('disk', sprintf("💾 Диск VPS заполнен на %d%%: свободно %.1f ГБ из %.1f ГБ.", $pct, $f / 1073741824, $t / 1073741824));
            }
        }
        if (!is_file(backup_dir() . '/stats-' . date('Y-m-d') . '.sqlite') && ($path = backup_make($db))) backup_send($path);
    } catch (Throwable $e) {
        error_log('fspirat housekeeping: ' . $e->getMessage());
    }
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
    if ($now - $r['seen'] > 60) {   // продление; если база занята — не страшно, продлим при следующем запросе
        try { $db->prepare('UPDATE tokens SET seen = ? WHERE hash = ?')->execute([$now, hash('sha256', $tok)]); }
        catch (Throwable $e) { error_log('fspirat token touch: ' . $e->getMessage()); }
    }
    return true;
}

function token_revoke(PDO $db): void
{
    if (($tok = bearer()) !== '') { token_table($db); $db->prepare('DELETE FROM tokens WHERE hash = ?')->execute([hash('sha256', $tok)]); }
}

const LOGIN_MAX_FAILS = 5;
const LOGIN_MAX_FAILS_ALL = 30;   // со всех адресов вместе: подбор через много IP
const LOGIN_LOCK_SEC = 900;

/** Сколько секунд ещё заблокирован вход с этого IP или для всех (0 — не заблокирован). */
function login_locked(PDO $db): int
{
    $st = $db->prepare("SELECT COUNT(*), MAX(ts) FROM audit WHERE action = 'login_fail' AND ip = ? AND ts > ?");
    $st->execute([client_ip(), time() - LOGIN_LOCK_SEC]);
    [$n, $last] = $st->fetch(PDO::FETCH_NUM);
    $st->closeCursor();
    if ((int)$n >= LOGIN_MAX_FAILS) return max(1, (int)$last + LOGIN_LOCK_SEC - time());
    $st = $db->prepare("SELECT COUNT(*), MAX(ts) FROM audit WHERE action = 'login_fail' AND ts > ?");
    $st->execute([time() - LOGIN_LOCK_SEC]);
    [$n, $last] = $st->fetch(PDO::FETCH_NUM);
    $st->closeCursor();
    return (int)$n >= LOGIN_MAX_FAILS_ALL ? max(1, (int)$last + LOGIN_LOCK_SEC - time()) : 0;
}

// ---------------------------------------------------------------------------
// Подтверждение входа в админку кодом из Telegram (бот уведомлений). Код — только на новом устройстве;
// подтверждённое устройство помнится 30 дней (ключ в localStorage браузера, в базе — его хэш).
// Бот не настроен — вход без кода, как раньше.

const ADMIN_DEV_TTL = 30 * 86400;
const ADMIN_CODE_TTL = 300;
const ADMIN_CODE_TRIES = 5;

function admin2fa_tables(PDO $db): void
{
    $db->exec('CREATE TABLE IF NOT EXISTS admin_devices(hash TEXT PRIMARY KEY, created INTEGER NOT NULL, seen INTEGER NOT NULL, ip TEXT, ua TEXT)');
    $db->exec('CREATE TABLE IF NOT EXISTS admin_codes(hash TEXT PRIMARY KEY, code TEXT NOT NULL, created INTEGER NOT NULL, tries INTEGER NOT NULL DEFAULT 0)');
}

function admin2fa_enabled(): bool { $c = notify_cfg(); return !empty($c['token']) && !empty($c['chat']); }

function admin_device_ok(PDO $db, string $dev): bool
{
    if (!preg_match('/^[a-f0-9]{64}$/', $dev)) return false;
    admin2fa_tables($db);
    $st = $db->prepare('SELECT seen FROM admin_devices WHERE hash = ?'); $st->execute([hash('sha256', $dev)]);
    $seen = $st->fetchColumn(); $st->closeCursor();
    if ($seen === false || (int)$seen + ADMIN_DEV_TTL < time()) return false;
    $db->prepare('UPDATE admin_devices SET seen = ? WHERE hash = ?')->execute([time(), hash('sha256', $dev)]);
    return true;
}

function admin_device_add(PDO $db): string
{
    admin2fa_tables($db);
    $dev = bin2hex(random_bytes(32));
    $db->prepare('DELETE FROM admin_devices WHERE seen < ?')->execute([time() - ADMIN_DEV_TTL]);
    $db->prepare('INSERT INTO admin_devices(hash, created, seen, ip, ua) VALUES(?,?,?,?,?)')
       ->execute([hash('sha256', $dev), time(), time(), client_ip(), substr($_SERVER['HTTP_USER_AGENT'] ?? '', 0, 200)]);
    return $dev;
}

/** Отправить код. Возвращает ключ ожидания (для второго запроса) или null, если Telegram не ответил. */
function admin_code_start(PDO $db, string $site): ?string
{
    $c = notify_cfg();
    $code = sprintf('%06d', random_int(0, 999999));
    $ua = $_SERVER['HTTP_USER_AGENT'] ?? '';
    $br = preg_match('~(Edg|OPR|YaBrowser|Firefox|Chrome|Safari)/~', $ua, $m) ? $m[1] : 'браузер';
    $r = tg_call($c['token'], 'sendMessage', ['chat_id' => $c['chat'], 'text' => "🔐 Код входа в админку fspirat: $code\n\nНовое устройство: $br · IP " . client_ip()
        . " · $site\nКод действует 5 минут. Если это не ты — никому его не сообщай и смени пароль (setup.php)."]);
    if (!($r['ok'] ?? false)) return null;
    admin2fa_tables($db);
    $pend = bin2hex(random_bytes(32));
    $db->prepare('DELETE FROM admin_codes WHERE created < ?')->execute([time() - ADMIN_CODE_TTL]);
    $db->prepare('INSERT INTO admin_codes(hash, code, created) VALUES(?,?,?)')->execute([hash('sha256', $pend), password_hash($code, PASSWORD_DEFAULT), time()]);
    return $pend;
}

/** 'ok' | 'bad' (попытка засчитана) | 'expired' */
function admin_code_check(PDO $db, string $pend, string $code): string
{
    if (!preg_match('/^[a-f0-9]{64}$/', $pend)) return 'expired';
    admin2fa_tables($db);
    $h = hash('sha256', $pend);
    $st = $db->prepare('SELECT code FROM admin_codes WHERE hash = ? AND created >= ? AND tries < ?');
    $st->execute([$h, time() - ADMIN_CODE_TTL, ADMIN_CODE_TRIES]);
    $hash = $st->fetchColumn(); $st->closeCursor();
    if ($hash === false) return 'expired';
    if (!preg_match('/^\d{6}$/', $code) || !password_verify($code, (string)$hash)) {
        $db->prepare('UPDATE admin_codes SET tries = tries + 1 WHERE hash = ?')->execute([$h]);
        return 'bad';
    }
    $db->prepare('DELETE FROM admin_codes WHERE hash = ?')->execute([$h]);
    return 'ok';
}
