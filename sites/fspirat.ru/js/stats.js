/* Статистика посещений fspirat — анонимно, без cookies.
 * Посетитель — случайный номер в localStorage, никаких личных данных и содержимого форм.
 * События уходят на свой сервер (fspirat.online/api/t.php), сторонних счётчиков нет.
 * Другие скрипты могут сообщить действие: window.fsTrack('gen.copy', {fmt:'item'}). */
(function () {
  'use strict';
  var me = document.currentScript;
  var EP = (me && me.getAttribute('data-endpoint')) || 'https://fspirat.online/api/t.php';
  if (/^(localhost|127\.0\.0\.1)$/.test(location.hostname)) EP = '/api/t.php';   // локальная проверка
  var SESSION_GAP = 30 * 60 * 1000, PING = 60 * 1000, MAX_EVENTS = 400;
  var sent = 0;

  function rnd() {
    var a = new Uint8Array(8), s = '';
    (window.crypto || window.msCrypto).getRandomValues(a);
    for (var i = 0; i < a.length; i++) s += (a[i] < 16 ? '0' : '') + a[i].toString(16);
    return s;
  }
  function get(k) { try { return localStorage.getItem(k); } catch (e) { return null; } }
  function put(k, v) { try { localStorage.setItem(k, v); } catch (e) {} }

  var vid = get('fs_vid');
  if (!/^[a-f0-9]{16}$/.test(vid || '')) { vid = rnd(); put('fs_vid', vid); }
  var memSession = null;   // если localStorage недоступен
  /** Визит: общий для всех вкладок, заканчивается после 30 минут без действий. */
  function sid() {
    var now = Date.now(), o = null;
    try { o = JSON.parse(get('fs_sid') || 'null'); } catch (e) {}
    if (!o) o = memSession;
    if (!o || !/^[a-f0-9]{16}$/.test(o.id) || now - o.t > SESSION_GAP) o = { id: rnd() };
    o.t = now; memSession = o; put('fs_sid', JSON.stringify(o));
    return o.id;
  }
  var utm = null;
  try { utm = new URLSearchParams(location.search).get('utm_source'); } catch (e) {}

  function send(ev, data, extra) {
    if (sent++ > MAX_EVENTS) return;
    var o = { v: vid, s: sid(), e: ev, p: location.pathname };
    if (data) o.d = data;
    if (extra) for (var k in extra) o[k] = extra[k];
    var body = JSON.stringify(o);
    try {
      if (navigator.sendBeacon && navigator.sendBeacon(EP, new Blob([body], { type: 'text/plain' }))) return;
    } catch (e) {}
    try { fetch(EP, { method: 'POST', body: body, keepalive: true, mode: 'no-cors', credentials: 'omit', headers: { 'Content-Type': 'text/plain' } }).catch(function () {}); } catch (e) {}
  }

  window.fsTrack = function (ev, data) {
    if (typeof ev !== 'string' || !/^[a-z][a-z0-9_.:-]{0,39}$/.test(ev) || ev === 'pageview' || ev === 'ping') return;
    send(ev, data && typeof data === 'object' ? data : null);
  };

  send('pageview', null, { r: document.referrer || undefined, u: utm ? utm.slice(0, 40) : undefined });

  // «Сейчас на сайте»: раз в минуту, пока вкладка открыта и видна
  setInterval(function () { if (!document.hidden) send('ping'); }, PING);

  // Переходы на другие сайты и скачивания
  document.addEventListener('click', function (e) {
    var a = e.target && e.target.closest ? e.target.closest('a[href]') : null;
    if (!a) return;
    var u; try { u = new URL(a.href, location.href); } catch (err) { return; }
    if (!/^https?:$/.test(u.protocol)) return;
    var file = (u.pathname.match(/[^\/]+\.(jar|zip|rar|7z|exe|mcpack|mrpack)$/i) || [])[0];
    if (file || a.hasAttribute('download')) send('download', { file: (file || a.getAttribute('download') || u.pathname.split('/').pop()).slice(0, 80) });
    else if (u.host !== location.host) send('link', { to: u.host.replace(/^www\./, '').slice(0, 80) });
  }, true);
})();
