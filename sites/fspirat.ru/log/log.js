/* Просмотр чат-лога FSLOG. Текст выводится только через textContent — HTML из лога не исполняется,
 * ссылки в сообщениях не кликабельны (это чужие сообщения из игры).
 * Состояние страницы — main[data-log-state]: incomplete | loading | notfound | error | success.
 * Аналитики отсюда нет: id лога никуда не отправляется (stats.js шлёт только путь /log/). */
(function () {
  'use strict';
  var $ = function (id) { return document.getElementById(id); };
  var API = /^(www\.)?fspirat\.(xyz|online|ru)$|^(localhost|127\.0\.0\.1)$/.test(location.hostname) ? '/api/chatlog.php' : 'https://fspirat.xyz/api/chatlog.php';
  var id = new URLSearchParams(location.search).get('id') || '';
  var log = null, kind = 'all', query = '';
  var main = $('main');

  function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
  var pad = function (n) { return (n < 10 ? '0' : '') + n; };
  function time(ms) { var d = new Date(ms); return pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds()); }
  function day(ms) { var d = new Date(ms); return pad(d.getDate()) + '.' + pad(d.getMonth() + 1) + '.' + d.getFullYear(); }
  function hex(c) { return '#' + ('00000' + c.toString(16)).slice(-6); }
  function shadow(c) { return hex(((c >> 16 & 255) >> 2) << 16 | ((c >> 8 & 255) >> 2) << 8 | ((c & 255) >> 2)); }
  var plain = function (m) { return m.s.map(function (s) { return s[0]; }).join(''); };
  var serverName = function (s) { return s === 'singleplayer' ? 'одиночная игра' : s || 'неизвестный сервер'; };

  /* ── состояние страницы ── */
  function setState(name) {
    main.setAttribute('data-log-state', name);
    if (name === 'loading') main.setAttribute('aria-busy', 'true'); else main.removeAttribute('aria-busy');
  }
  /** #state — role=status: виден только во время загрузки, в остальных состояниях — только для чтения с экрана. */
  function status(text, visible) {
    var s = $('state');
    s.textContent = text;
    s.classList.toggle('visually-hidden', !visible);
  }

  function fail(state, title, lines, retry) {
    setState(state);
    status(title, false);
    // ничего от удачной загрузки не остаётся: ни пустого окна чата, ни шапки лога
    $('head').hidden = true; $('bar').hidden = true; $('chat').hidden = true; $('chat').textContent = '';
    $('found').textContent = '';
    stopObf();
    var box = $('error'); box.textContent = '';
    box.className = 'pane log-error log-error--' + state;
    box.appendChild(el('h2', 'log-error-title', title));
    lines.forEach(function (t) { box.appendChild(el('p', null, t)); });
    if (retry) {
      var b = el('button', 'btn btn-secondary btn-sm log-retry', 'Повторить');
      b.type = 'button'; b.id = 'retry';
      b.addEventListener('click', function () { load(true); });
      box.appendChild(b);
    }
    box.hidden = false;
    document.title = 'FSLOG · ' + title;
  }

  /* ── загрузка ── */
  function load(again) {
    setState('loading');
    status('Загрузка лога…', true);
    $('error').hidden = true; $('error').textContent = '';
    document.title = 'FSLOG · Чат-лог';
    fetch(API + '?id=' + encodeURIComponent(id), { credentials: 'omit', cache: 'no-store' })
      .then(function (r) { if (r.status === 404) throw new Error('404'); if (!r.ok) throw new Error('HTTP'); return r.json(); },
            function () { throw new Error('NET'); })
      .then(function (data) {
        log = data; head(); render();
        setState('success');
        status('Лог загружен, сообщений: ' + log.messages.length, false);
        if (again) main.focus({ preventScroll: true });
      })
      .catch(function (e) {
        if (e.message === '404') {
          fail('notfound', 'Лог не найден', ['Возможно, ссылка неверная или лог уже удалён.', 'Обычно логи хранятся 30 дней с момента отправки.'], false);
        } else if (e.message === 'NET') {
          fail('error', 'Не удалось загрузить лог', ['Нет связи с сервером. Проверь подключение к интернету и попробуй ещё раз.'], true);
        } else {
          fail('error', 'Не удалось загрузить лог', ['Сервер не смог отдать лог. Попробуй ещё раз чуть позже.'], true);
        }
        if (again && $('retry')) $('retry').focus();
      });
  }

  function head() {
    var msgs = log.messages;
    if (!Array.isArray(msgs)) throw new Error('shape');
    var t = $('title'); t.textContent = 'Чат-лог ';
    if (log.player) { t.appendChild(el('span', 'accent', log.player)); }
    document.title = 'FSLOG · ' + (log.player ? log.player + ' · ' : '') + day(log.created * 1000);
    var meta = $('meta'); meta.textContent = '';
    var timed = msgs.filter(function (m) { return m.t; }), first = timed[0], last = timed[timed.length - 1];
    [['Сервер', serverName(log.server)], ['Сообщений', String(log.n)],
     ['Период', first ? day(first.t) + ' ' + time(first.t) + ' — ' + (day(first.t) === day(last.t) ? '' : day(last.t) + ' ') + time(last.t) : '—'],
     ['Minecraft', log.mc || '—'],
     ['Хранение', log.expires ? 'Удалится ' + day(log.expires * 1000) : 'Закреплён — хранится, пока его не открепят']
    ].forEach(function (p) {
      var row = el('div', 'log-meta-item');
      row.appendChild(el('dt', null, p[0]));
      row.appendChild(el('dd', null, p[1]));
      meta.appendChild(row);
    });
    $('head').hidden = false; $('bar').hidden = false;
  }

  /* ── поиск: совпадения ищутся по тексту всего сообщения, поэтому подсвечиваются и в градиентах,
        где каждая буква — отдельный кусок ── */
  function matches(low, q) {
    var out = [], i = 0, j;
    if (!q) return out;
    while ((j = low.indexOf(q, i)) >= 0) { out.push([j, j + q.length]); i = j + q.length; }
    return out;
  }
  /** Текст куска с подсветкой: hits — совпадения в координатах всего сообщения, off — начало куска. */
  function addText(span, text, off, hits) {
    if (!hits || !hits.length) { span.textContent = text; return; }
    var i = 0, end = off + text.length;
    hits.forEach(function (h) {
      var a = Math.max(h[0], off) - off, b = Math.min(h[1], end) - off;
      if (a >= b) return;
      if (a > i) span.appendChild(document.createTextNode(text.slice(i, a)));
      span.appendChild(el('mark', null, text.slice(a, b)));
      i = b;
    });
    if (i < text.length) span.appendChild(document.createTextNode(text.slice(i)));
  }

  function render() {
    var box = $('chat');
    stopObf();
    box.textContent = '';
    var shown = 0, lastDay = '', lastSrv = null;
    var frag = document.createDocumentFragment();
    log.messages.forEach(function (m) {
      if (kind === 'player' && !m.p) return;
      if (kind === 'system' && m.p) return;
      var text = plain(m), low = query ? text.toLowerCase() : '';
      if (query && low.indexOf(query) < 0) return;
      // сообщение без времени (t = 0) не начинает новый день: без пустого разделителя
      var d = m.t ? day(m.t) : lastDay;
      if (d !== lastDay || m.srv !== lastSrv) {
        frag.appendChild(el('div', 'sep', [d, lastSrv !== null && m.srv === lastSrv ? '' : serverName(m.srv)].filter(Boolean).join(' · ')));
        lastDay = d; lastSrv = m.srv;
      }
      var line = el('div', 'line');
      line.appendChild(el('span', m.t ? 't' : 't t-none', m.t ? time(m.t) : '—:—:—'));
      var msg = el('span', 'm');
      // toLowerCase() может изменить длину (редкие буквы) — тогда подсветка по кускам, как раньше
      var whole = query && low.length === text.length ? matches(low, query) : null;
      var off = 0;
      m.s.forEach(function (s) {
        var span = el('span', [s[2] & 1 ? 'b' : '', s[2] & 2 ? 'i' : '', s[2] & 4 ? 'u' : '', s[2] & 8 ? 's' : '', s[2] & 16 ? 'o' : ''].filter(Boolean).join(' '));
        var c = s[1] >= 0 ? s[1] : 0xFFFFFF;
        span.style.color = hex(c);
        span.style.setProperty('--sh', shadow(c));
        if (s[2] & 16) {
          // &k: в игре настоящий текст не виден — показываем случайные знаки той же длины
          span.setAttribute('aria-hidden', 'true');
          span.setAttribute('data-len', s[0].length);
          span.textContent = glyphs(s[0].length);
          msg.appendChild(span);
          msg.appendChild(el('span', 'visually-hidden', ' (скрытый текст) '));
        } else {
          var segLow = query && !whole ? s[0].toLowerCase() : '';
          addText(span, s[0], whole ? off : 0, whole || (query && segLow.length === s[0].length ? matches(segLow, query) : null));
          msg.appendChild(span);
        }
        off += s[0].length;
      });
      line.appendChild(msg);
      frag.appendChild(line);
      shown++;
    });
    box.appendChild(frag);
    $('found').textContent = !query && kind === 'all' ? '' : 'Показано ' + shown + ' из ' + log.messages.length;
    if (!shown) box.appendChild(el('p', 'empty', log.messages.length ? 'Ничего не найдено.' : 'В этом логе нет сообщений.'));
    box.hidden = false;
    syncMotion();
  }

  /* ── «мигающий» текст (&k), как в игре ──
     Крутится только если движение не уменьшено в системе и не выключено переключателем «анимация &k»
     (выбор хранится в fs_log_motion — только для этой страницы; настройку главной fs_motion не трогаем).
     Обновляются лишь видимые на экране куски (не больше 300). */
  var OBF = 'ABCDEFGHJKLMNOPQRSTUVWXYZabcdefghkmnopqrstuvwxyz0123456789#$%&?@';
  var MOTION_KEY = 'fs_log_motion';
  var reduceMQ = window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : null;
  var timer = null, io = null, onScreen = new Set();

  function glyphs(n) { var s = ''; for (var k = 0; k < n; k++) s += OBF[Math.floor(Math.random() * OBF.length)]; return s; }
  function motionPref() { try { return localStorage.getItem(MOTION_KEY) !== 'off'; } catch (e) { return true; } }
  function setMotionPref(on) { try { if (on) localStorage.removeItem(MOTION_KEY); else localStorage.setItem(MOTION_KEY, 'off'); } catch (e) {} }
  function reduced() { return !!(reduceMQ && reduceMQ.matches); }

  function stopObf() {
    if (timer) { clearInterval(timer); timer = null; }
    if (io) { io.disconnect(); io = null; }
    onScreen.clear();
  }
  function tick() {
    var n = 0;
    onScreen.forEach(function (e) { if (n++ < 300) e.textContent = glyphs(+e.getAttribute('data-len')); });
  }
  function syncMotion() {
    stopObf();
    var list = $('chat').querySelectorAll('.o');
    var toggle = $('animObf');
    $('animObfWrap').hidden = !list.length || reduced();   // при «уменьшить движение» текст неподвижен, переключатель не нужен
    toggle.checked = motionPref();
    if (!list.length || reduced() || !toggle.checked) return;
    if ('IntersectionObserver' in window) {
      io = new IntersectionObserver(function (entries) {
        entries.forEach(function (en) { if (en.isIntersecting) onScreen.add(en.target); else onScreen.delete(en.target); });
      });
      list.forEach(function (o) { io.observe(o); });
    } else {
      for (var k = 0; k < list.length && k < 300; k++) onScreen.add(list[k]);
    }
    timer = setInterval(tick, 90);
  }
  if (reduceMQ) {
    if (reduceMQ.addEventListener) reduceMQ.addEventListener('change', function () { if (log) syncMotion(); });
    else if (reduceMQ.addListener) reduceMQ.addListener(function () { if (log) syncMotion(); });
  }
  $('animObf').addEventListener('change', function () { setMotionPref(this.checked); syncMotion(); });

  /* ── панель инструментов ── */
  var qt = null;
  $('q').addEventListener('input', function () { clearTimeout(qt); qt = setTimeout(function () { query = $('q').value.trim().toLowerCase(); render(); }, 150); });
  document.querySelectorAll('.seg button').forEach(function (b) {
    b.addEventListener('click', function () {
      kind = b.getAttribute('data-kind');
      document.querySelectorAll('.seg button').forEach(function (x) { x.setAttribute('aria-pressed', String(x === b)); });
      render();
    });
  });
  $('showTime').addEventListener('change', function () { $('chat').classList.toggle('notime', !this.checked); });
  $('mcFont').addEventListener('change', function () { $('chat').classList.toggle('mc', this.checked); });

  function asText() {
    return log.messages.map(function (m) { return (m.t ? '[' + day(m.t) + ' ' + time(m.t) + '] ' : '') + plain(m); }).join('\n') + '\n';
  }
  $('download').addEventListener('click', function () {
    var a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([asText()], { type: 'text/plain;charset=utf-8' }));
    a.download = 'chatlog-' + (log.player ? log.player + '-' : '') + id + '.txt';
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(function () { URL.revokeObjectURL(a.href); }, 1000);
  });

  // «Скопировано» — только когда браузер подтвердил запись; иначе честно сообщаем, что не вышло
  var copyTimer = null;
  function copyNote(text, ok) {
    var m = $('copyMsg');
    clearTimeout(copyTimer);
    m.textContent = text;
    m.className = 'log-copy-msg ' + (ok ? 'is-ok' : 'is-fail');
    copyTimer = setTimeout(function () { m.textContent = ''; }, ok ? 3000 : 6000);
  }
  $('copyAll').addEventListener('click', function () {
    var failed = function () { copyNote('Не удалось скопировать. Выдели текст вручную или скачай .txt.', false); };
    if (!navigator.clipboard || !navigator.clipboard.writeText) { failed(); return; }
    try {
      navigator.clipboard.writeText(asText()).then(function () { copyNote('Скопировано', true); }, failed);
    } catch (e) { failed(); }
  });

  /* ── старт ── */
  if (!/^[A-Za-z0-9]{10}$/.test(id)) {
    fail('incomplete', 'Ссылка неполная', ['Открой ссылку, которую мод FSLOG прислал в чат после команды /log.',
      'Если ссылку копировали вручную, проверь, что после «id=» стоят все 10 букв и цифр.'], false);
    return;
  }
  load(false);
})();
