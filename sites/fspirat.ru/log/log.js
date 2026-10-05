/* Просмотр чат-лога FSLOG. Текст выводится только через textContent — HTML из лога не исполняется,
 * ссылки в сообщениях не кликабельны (это чужие сообщения из игры). */
(function () {
  'use strict';
  var $ = function (id) { return document.getElementById(id); };
  var API = /^(www\.)?fspirat\.online$|^(localhost|127\.0\.0\.1)$/.test(location.hostname) ? '/api/chatlog.php' : 'https://fspirat.online/api/chatlog.php';
  var id = new URLSearchParams(location.search).get('id') || '';
  var log = null, kind = 'all', query = '';

  function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
  var pad = function (n) { return (n < 10 ? '0' : '') + n; };
  function time(ms) { var d = new Date(ms); return pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds()); }
  function day(ms) { var d = new Date(ms); return pad(d.getDate()) + '.' + pad(d.getMonth() + 1) + '.' + d.getFullYear(); }
  function hex(c) { return '#' + ('00000' + c.toString(16)).slice(-6); }
  function shadow(c) { return hex(((c >> 16 & 255) >> 2) << 16 | ((c >> 8 & 255) >> 2) << 8 | ((c & 255) >> 2)); }
  var plain = function (m) { return m.s.map(function (s) { return s[0]; }).join(''); };
  var serverName = function (s) { return s === 'singleplayer' ? 'одиночная игра' : s || 'неизвестный сервер'; };

  function fail(title, text) {
    $('state').textContent = '';
    var box = $('error'); box.hidden = false; box.textContent = '';
    box.append(el('h2', null, title), el('p', null, text));
    document.title = 'FSLOG · ' + title;
  }

  if (!/^[A-Za-z0-9]{10}$/.test(id)) { fail('Ссылка неполная', 'Откройте ссылку, которую мод FSLOG прислал в чат после команды /log.'); return; }

  fetch(API + '?id=' + encodeURIComponent(id), { credentials: 'omit', cache: 'no-store' })
    .then(function (r) { if (r.status === 404) throw new Error('404'); if (!r.ok) throw new Error('HTTP'); return r.json(); })
    .then(function (data) { log = data; head(); render(); })
    .catch(function (e) {
      if (e.message === '404') fail('Лог не найден', 'Возможно, ссылка неверная или лог уже удалён: логи хранятся 30 дней.');
      else fail('Не удалось загрузить лог', 'Сервер не отвечает. Попробуйте обновить страницу чуть позже.');
    });

  function head() {
    $('state').textContent = '';
    $('head').hidden = false; $('bar').hidden = false;
    var t = $('title'); t.textContent = 'Чат-лог ';
    if (log.player) { t.appendChild(el('b', null, log.player)); }
    document.title = 'FSLOG · ' + (log.player ? log.player + ' · ' : '') + day(log.created * 1000);
    var meta = $('meta'); meta.textContent = '';
    var first = log.messages[0], last = log.messages[log.messages.length - 1];
    [['Сервер', serverName(log.server)], ['Сообщений', String(log.n)],
     ['Период', first && last ? day(first.t) + ' ' + time(first.t) + ' — ' + (day(first.t) === day(last.t) ? '' : day(last.t) + ' ') + time(last.t) : '—'],
     ['Minecraft', log.mc || '—'], log.expires ? ['Удалится', day(log.expires * 1000)] : ['Хранится', 'бессрочно']
    ].forEach(function (p) { var s = el('span', null, p[0] + ': '); s.appendChild(el('b', null, p[1])); meta.appendChild(s); });
  }

  /** Текст куска с подсветкой найденного. */
  function addText(span, text) {
    if (!query) { span.textContent = text; return; }
    var low = text.toLowerCase(), i = 0, j;
    while ((j = low.indexOf(query, i)) >= 0) {
      if (j > i) span.appendChild(document.createTextNode(text.slice(i, j)));
      span.appendChild(el('mark', null, text.slice(j, j + query.length)));
      i = j + query.length;
    }
    if (i < text.length) span.appendChild(document.createTextNode(text.slice(i)));
  }

  function render() {
    var box = $('chat'); box.textContent = '';
    var shown = 0, lastDay = '', lastSrv = null;
    var frag = document.createDocumentFragment();
    log.messages.forEach(function (m) {
      if (kind === 'player' && !m.p) return;
      if (kind === 'system' && m.p) return;
      if (query && plain(m).toLowerCase().indexOf(query) < 0) return;
      var d = m.t ? day(m.t) : '';
      if (d !== lastDay || m.srv !== lastSrv) {
        frag.appendChild(el('div', 'sep', [d, lastSrv !== null && m.srv === lastSrv ? '' : serverName(m.srv)].filter(Boolean).join(' · ')));
        lastDay = d; lastSrv = m.srv;
      }
      var line = el('div', 'line');
      line.appendChild(el('span', 't', m.t ? time(m.t) : ''));
      var msg = el('span', 'm');
      m.s.forEach(function (s) {
        var span = el('span', [s[2] & 1 ? 'b' : '', s[2] & 2 ? 'i' : '', s[2] & 4 ? 'u' : '', s[2] & 8 ? 's' : '', s[2] & 16 ? 'o' : ''].filter(Boolean).join(' '));
        var c = s[1] >= 0 ? s[1] : 0xFFFFFF;
        span.style.color = hex(c);
        span.style.setProperty('--sh', shadow(c));
        addText(span, s[0]);
        msg.appendChild(span);
      });
      line.appendChild(msg);
      frag.appendChild(line);
      shown++;
    });
    box.appendChild(frag);
    var f = $('found');
    f.hidden = !query && kind === 'all';
    f.textContent = 'Показано ' + shown + ' из ' + log.messages.length;
    if (!shown) box.appendChild(el('p', 'empty', 'Ничего не найдено.'));
  }

  // «мигающий» текст (&k), как в игре
  var OBF = 'ABCDEFGHJKLMNOPQRSTUVWXYZabcdefghkmnopqrstuvwxyz0123456789#$%&?@';
  setInterval(function () {
    var list = document.querySelectorAll('.chat .o');
    for (var k = 0; k < list.length && k < 300; k++) {
      var e = list[k];
      if (!e.dataset.len) e.dataset.len = e.textContent.length;
      var s = ''; for (var n = 0; n < +e.dataset.len; n++) s += OBF[Math.floor(Math.random() * OBF.length)];
      e.textContent = s;
    }
  }, 90);

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
  $('copyAll').addEventListener('click', function () {
    var b = this, done = function () { b.textContent = 'Скопировано ✓'; setTimeout(function () { b.textContent = 'Копировать всё'; }, 1400); };
    if (navigator.clipboard) navigator.clipboard.writeText(asText()).then(done, function () {});
  });
})();
