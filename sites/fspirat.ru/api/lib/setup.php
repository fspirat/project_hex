<?php
/**
 * Первая настройка статистики или смена пароля админки. Запускать ТОЛЬКО на сервере из консоли:
 *
 *   sudo -u www-data php /var/www/fspirat.online/api/lib/setup.php
 *
 * Создаёт /var/lib/fspirat-stats/ (база + config.json) и спрашивает пароль.
 * Повторный запуск меняет пароль; статистика сохраняется.
 */

declare(strict_types=1);

if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
define('FS_STATS', true);
require __DIR__ . '/stats.php';

$dir = stats_dir();
if (!is_dir($dir) && !@mkdir($dir, 0700, true)) {
    fwrite(STDERR, "Не удалось создать $dir. Создайте папку и дайте права пользователю веб-сервера:\n"
        . "  sudo mkdir -p $dir && sudo chown www-data:www-data $dir && sudo chmod 700 $dir\n");
    exit(1);
}
if (!is_writable($dir)) {
    fwrite(STDERR, "Нет прав на запись в $dir. Запустите от пользователя веб-сервера: sudo -u www-data php " . __FILE__ . "\n");
    exit(1);
}

$password = getenv('FSPIRAT_ADMIN_PASSWORD') ?: '';
if ($password === '') {
    $read = function (string $prompt): string {
        fwrite(STDOUT, $prompt);
        if (DIRECTORY_SEPARATOR === '/') system('stty -echo 2>/dev/null');
        $v = rtrim((string)fgets(STDIN), "\r\n");
        if (DIRECTORY_SEPARATOR === '/') system('stty echo 2>/dev/null');
        fwrite(STDOUT, "\n");
        return $v;
    };
    $password = $read('Новый пароль для админки (не меньше 10 символов): ');
    if ($read('Повторите пароль: ') !== $password) { fwrite(STDERR, "Пароли не совпадают.\n"); exit(1); }
}
if (mb_strlen($password) < 10) { fwrite(STDERR, "Пароль слишком короткий: нужно не меньше 10 символов.\n"); exit(1); }

$cfgFile = "$dir/config.json";
$old = json_decode((string)@file_get_contents($cfgFile), true) ?: [];
$cfg = [
    'password_hash' => password_hash($password, PASSWORD_DEFAULT),
    'secret' => $old['secret'] ?? bin2hex(random_bytes(32)),
];
file_put_contents($cfgFile, json_encode($cfg, JSON_PRETTY_PRINT));
chmod($cfgFile, 0600);

$db = stats_db();
if (!$db) { fwrite(STDERR, "Не удалось открыть базу. Установлен ли модуль SQLite для PHP (php-sqlite3)?\n"); exit(1); }
$db->prepare('INSERT INTO audit(ts, ip, action, detail) VALUES(?,?,?,?)')->execute([time(), 'console', empty($old) ? 'setup' : 'password_changed', '']);

echo empty($old) ? "Готово: статистика настроена в $dir\n" : "Готово: пароль изменён.\n";
echo "Админка: https://fspirat.online/admin/\n";
