/* Админ-панель fspirat. Все данные выводятся через textContent — никакого HTML из базы. */
(function () {
  'use strict';
  var $ = function (id) { return document.getElementById(id); };

  /** h('div', {class:'x'}, 'текст', child) — создать элемент */
  function h(tag, attrs) {
    var el = document.createElement(tag);
    if (attrs) for (var k in attrs) {
      if (attrs[k] == null) continue;
      if (k === 'class') el.className = attrs[k];
      else if (k === 'text') el.textContent = attrs[k];
      else if (k === 'style') el.style.cssText = attrs[k];   // через CSSOM — разрешено политикой CSP
      else if (k.slice(0, 2) === 'on') el.addEventListener(k.slice(2), attrs[k]);
      else el.setAttribute(k, attrs[k]);
    }
    for (var i = 2; i < arguments.length; i++) {
      var c = arguments[i];
      if (c == null || c === false) continue;
      el.appendChild(typeof c === 'string' || typeof c === 'number' ? document.createTextNode(String(c)) : c);
    }
    return el;
  }
  var svgNS = 'http://www.w3.org/2000/svg';
  function s(tag, attrs) { var el = document.createElementNS(svgNS, tag); for (var k in attrs) el.setAttribute(k, attrs[k]); return el; }

  if (window.top !== window.self) { try { window.top.location = window.location.href; } catch (e) {} }

  // Данные лежат на VPS. С fspirat.online — свой адрес, с fspirat.ru — полный адрес VPS.
  var LOCAL = /^(www\.)?fspirat\.online$|^(localhost|127\.0\.0\.1)$/.test(location.hostname);
  var API = LOCAL ? 'api.php' : 'https://fspirat.online/admin/api.php';
  var TOKEN_KEY = 'fs_admin_token';
  function token(v) {
    try { if (v === undefined) return sessionStorage.getItem(TOKEN_KEY) || ''; if (v) sessionStorage.setItem(TOKEN_KEY, v); else sessionStorage.removeItem(TOKEN_KEY); } catch (e) {}
    return v || '';
  }

  function api(params, post) {
    var q = Object.keys(params).filter(function (k) { return params[k] !== '' && params[k] != null; })
      .map(function (k) { return encodeURIComponent(k) + '=' + encodeURIComponent(params[k]); }).join('&');
    var headers = { Accept: 'application/json' };
    if (token()) headers.Authorization = 'Bearer ' + token();
    if (post) headers['Content-Type'] = 'application/json';
    return fetch(API + '?' + q, { method: post ? 'POST' : 'GET', body: post ? JSON.stringify(post) : undefined,
      credentials: 'omit', cache: 'no-store', headers: headers }).then(function (r) {
      if (r.status === 401 && params.q !== 'login') { token(null); showLogin('Вход истёк — войдите снова.'); throw new Error('auth'); }
      return r.json().catch(function () { return {}; }).then(function (j) {
        if (!r.ok) { var e = new Error('HTTP ' + r.status); e.data = j; e.status = r.status; throw e; }
        return j;
      });
    });
  }

  // ---------- вход и выход ----------
  function showLogin(msg) {
    clearTimeout(timer);
    document.body.className = 'login';
    $('appView').hidden = true; $('loginView').hidden = false;
    $('loginErr').hidden = !msg; $('loginErr').textContent = msg || '';
    $('pw').value = ''; $('pw').focus();
  }
  function startApp() {
    document.body.className = 'app';
    $('loginView').hidden = true; $('appView').hidden = false;
    show('overview');
  }
  $('loginForm').addEventListener('submit', function (e) {
    e.preventDefault();
    var btn = $('loginBtn'); btn.disabled = true; $('loginErr').hidden = true;
    api({ q: 'login' }, { password: $('pw').value }).then(function (r) {
      token(r.token); startApp();
    }).catch(function (err) {
      var d = err.data || {}, msg;
      if (d.error === 'locked') msg = 'Слишком много неверных попыток. Вход с этого адреса закрыт ещё на ' + Math.ceil((d.wait || 900) / 60) + ' мин.';
      else if (d.error === 'password') msg = 'Неверный пароль. Осталось попыток: ' + d.left + '.';
      else msg = 'Сервер статистики не отвечает. Попробуйте позже.';
      $('loginErr').textContent = msg; $('loginErr').hidden = false; $('pw').select();
    }).then(function () { btn.disabled = false; });
  });
  $('logout').addEventListener('click', function () {
    api({ q: 'logout' }, {}).catch(function () {}).then(function () { token(null); showLogin(); });
  });

  // ---------- подписи ----------
  var PAGES = { '/': 'Главная', '/index.html': 'Главная', '/hex_generator/': 'Генератор градиентов', '/hex_generator/index.html': 'Генератор градиентов', '/fstweak/': 'FSTWEAK', '/fstweak/index.html': 'FSTWEAK' };
  var FMT = {
    'item': 'Предмет (AgeMagic)', '/itemname': '/itemname', '/itemlore': '/itemlore', 'sponsor': '/sponsor prefix', 'sponsoredit': '/sponsor editprefix', 'am-raw': 'Без команды (AgeMagic)',
    'c:item': 'Предмет (/colors)', 'c:/itemname': '/itemname (/colors)', 'c:/itemlore': '/itemlore (/colors)', 'c:raw': '&-коды (/colors)',
    'fmt:nickname': 'Nickname', 'fmt:chat': 'Chat', 'fmt:legacy': 'Legacy', 'fmt:console': 'Console', 'fmt:bbcode': 'BBCode', 'fmt:minimessage': 'MiniMessage', 'fmt:birdflop': 'BirdFlop'
  };
  var SRC = { paste: 'вставил из буфера', file: 'выбрал файл', drop: 'перетащил файл' };
  var EV_NAME = {
    'pageview': 'Просмотр страницы', 'link': 'Переход на другой сайт', 'download': 'Скачивание',
    'gen.format': 'Генератор: смена формата', 'gen.font': 'Генератор: смена шрифта', 'gen.palette': 'Генератор: палитра',
    'gen.copy': 'Генератор: копирование', 'gen.random': 'Генератор: случайный градиент', 'gen.pal_save': 'Генератор: сохранил палитру',
    'gen.shot_load': 'Генератор: загрузил скриншот', 'gen.shot_apply': 'Генератор: цвета со скриншота'
  };
  var AUDIT = { login_ok: 'Вход', login_fail: 'Неверный пароль', login_blocked: 'Вход заблокирован', logout: 'Выход', setup: 'Первая настройка', password_changed: 'Пароль изменён' };
  var pageName = function (p) { return PAGES[p] || p; };
  var fmtName = function (f) { return FMT[f] || f; };
  var evName = function (e) { return EV_NAME[e] || e; };

  /** Человеческое описание действия */
  function describe(ev, d) {
    d = d || {};
    switch (ev) {
      case 'pageview': return 'открыл страницу';
      case 'link': return 'перешёл на ' + (d.to || 'другой сайт');
      case 'download': return 'скачал ' + (d.file || 'файл');
      case 'gen.format': return 'выбрал формат: ' + fmtName(d.fmt);
      case 'gen.font': return 'выбрал шрифт: ' + d.font;
      case 'gen.palette': return 'применил палитру «' + (d.name || '?') + '»' + (d.own ? ' (свою)' : '');
      case 'gen.copy': return (d.all ? 'скопировал все команды' : 'скопировал команду') + ' · ' + fmtName(d.fmt) + (d.len ? ' · ' + d.len + ' симв.' : '');
      case 'gen.random': return 'нажал «Случайный градиент»';
      case 'gen.pal_save': return 'сохранил свою палитру (' + d.colors + ' цв.)';
      case 'gen.shot_load': return 'загрузил скриншот: ' + (SRC[d.src] || d.src || '') + ', строк найдено: ' + d.lines + (d.exact ? '' : ' (сжатый/фото)');
      case 'gen.shot_apply': return 'применил цвета со скриншота (' + d.colors + ' цв.' + (d.simple ? ', без повторов' : '') + ')';
    }
    return ev + (Object.keys(d).length ? ' ' + JSON.stringify(d) : '');
  }

  var shortId = function (vid) { return '#' + String(vid).slice(0, 6).toUpperCase(); };
  var pad = function (n) { return (n < 10 ? '0' : '') + n; };
  // время всегда по Москве — как и группировка по дням на сервере
  var TZ = 'Europe/Moscow', fT, fD;
  try { fT = new Intl.DateTimeFormat('ru-RU', { timeZone: TZ, hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
        fD = new Intl.DateTimeFormat('ru-RU', { timeZone: TZ, day: '2-digit', month: '2-digit', year: 'numeric' }); } catch (e) {}
  function time(ts) { var d = new Date(ts * 1000); return fT ? fT.format(d) : pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds()); }
  function date(ts) { var d = new Date(ts * 1000); return fD ? fD.format(d) : pad(d.getDate()) + '.' + pad(d.getMonth() + 1) + '.' + d.getFullYear(); }
  function ago(sec) { return sec < 60 ? sec + ' с' : sec < 3600 ? Math.floor(sec / 60) + ' мин' : Math.floor(sec / 3600) + ' ч ' + Math.floor(sec % 3600 / 60) + ' мин'; }
  function dur(sec) { return Math.floor(sec / 60) + ':' + pad(sec % 60); }
  var num = function (n) { return Number(n || 0).toLocaleString('ru-RU'); };

  function vidButton(vid) { return h('button', { type: 'button', class: 'vid', title: 'Вся история посетителя', onclick: function () { openVisitor(vid); } }, shortId(vid)); }
  function who(r) {
    var tags = h('span');
    if (r.device) tags.appendChild(h('span', { class: 'tag', text: r.device }));
    if (r.browser) tags.appendChild(h('span', { class: 'tag', text: r.browser }));
    if (r.source) tags.appendChild(h('span', { class: 'tag', text: r.source }));
    if (r.visit === 1) tags.appendChild(h('span', { class: 'tag new', text: 'новый' }));
    else if (r.visit) tags.appendChild(h('span', { class: 'tag', text: r.visit + '-й визит' }));
    return tags;
  }

  // ---------- вкладки ----------
  var current = 'overview', timer = null;
  function show(tab) {
    current = tab;
    document.querySelectorAll('[data-pane]').forEach(function (p) { p.hidden = p.getAttribute('data-pane') !== tab; });
    document.querySelectorAll('.tabs button').forEach(function (b) { b.setAttribute('aria-selected', String(b.getAttribute('data-tab') === tab)); });
    load();
  }
  document.querySelectorAll('.tabs button').forEach(function (b) { b.addEventListener('click', function () { show(b.getAttribute('data-tab')); }); });
  function load() {
    clearTimeout(timer);
    var next = { overview: 60000, live: 15000 }[current];
    var p = current === 'overview' ? loadOverview() : current === 'live' ? loadLive() : current === 'journal' ? loadJournal(false) : current === 'audit' ? loadAudit() : null;
    if (next && p) p.then(function () { timer = setTimeout(function () { if (!document.hidden) load(); else timer = setTimeout(load, next); }, next); }, function () {});
  }
  document.addEventListener('visibilitychange', function () { if (!document.hidden && (current === 'overview' || current === 'live')) load(); });

  // ---------- обзор ----------
  var days = 7;
  document.querySelectorAll('#range button').forEach(function (b) {
    b.addEventListener('click', function () {
      days = +b.getAttribute('data-days');
      document.querySelectorAll('#range button').forEach(function (x) { x.setAttribute('aria-pressed', String(x === b)); });
      loadOverview();
    });
  });
  $('hostFilter').addEventListener('change', function () { load(); });
  var host = function () { return $('hostFilter').value; };

  function delta(cur, prev) {
    if (!prev) return null;
    var p = Math.round((cur - prev) / prev * 100);
    return h('span', { class: p >= 0 ? 'up' : 'down', text: (p >= 0 ? '▲ ' : '▼ ') + Math.abs(p) + '%' });
  }
  function card(label, value, d, extra) {
    return h('div', { class: 'card' + (extra ? ' ' + extra : '') }, h('div', { class: 'l', text: label }), h('div', { class: 'v', text: value }),
      h('div', { class: 'd' }, d || '\u00a0'));
  }

  function loadOverview() {
    loadServer();
    return api({ q: 'summary', days: days, host: host() }).then(function (r) {
      var c = r.cur, p = r.prev, per = { 1: 'чем вчера', 7: 'чем 7 дней назад', 30: 'чем прошлые 30 дней', 90: 'чем прошлые 90 дней' }[r.days];
      var cards = $('cards'); cards.textContent = '';
      var withD = function (cur, prev) { var x = delta(cur, prev); return x ? h('span', null, x, ' ' + per) : 'за прошлый период данных нет'; };
      cards.append(
        card('Сейчас на сайте', num(r.online), 'за последние 5 минут', 'live'),
        card('Посетители', num(c.visitors), withD(c.visitors, p.visitors)),
        card('Визиты', num(c.visits), withD(c.visits, p.visits)),
        card('Просмотры страниц', num(c.views), withD(c.views, p.views)),
        card('Новые посетители', num(c.fresh), c.visits ? Math.round(c.fresh / c.visits * 100) + '% визитов' : null),
        card('Среднее время визита, мин:с', dur(c.dur || 0), c.visits ? (c.views / c.visits).toFixed(1).replace('.', ',') + ' стр. за визит' : null)
      );
      chart(r.series, r.days);
      tables(r.tables);
      $('upd').textContent = 'обновлено в ' + time(Date.now() / 1000);
    }).catch(function (e) { if (e.message !== 'auth') $('upd').textContent = 'не удалось загрузить данные'; throw e; });
  }

  // ---------- сервер ----------
  function bytes(n) {
    if (n == null) return '—';
    var u = ['Б', 'КБ', 'МБ', 'ГБ', 'ТБ'], i = 0;
    while (n >= 1024 && i < u.length - 1) { n /= 1024; i++; }
    return (n >= 10 || i === 0 ? Math.round(n) : n.toFixed(1).replace('.', ',')) + ' ' + u[i];
  }
  function uptime(s) { var d = Math.floor(s / 86400), hh = Math.floor(s % 86400 / 3600); return (d ? d + ' д ' : '') + hh + ' ч ' + Math.floor(s % 3600 / 60) + ' мин'; }
  function meter(label, pct, value, sub, words) {
    var lvl = pct == null ? '' : pct >= 90 ? 'crit' : pct >= 75 ? 'warn' : '';
    var state = lvl ? ' · ' + words[lvl === 'crit' ? 1 : 0] : '';
    return h('div', { class: 'meter ' + lvl },
      h('div', { class: 'mtop' }, h('span', null, label, h('span', { class: 'state', text: state })), h('b', { text: pct == null ? '—' : Math.round(pct) + '%' })),
      h('div', { class: 'track', role: 'meter', 'aria-valuemin': 0, 'aria-valuemax': 100, 'aria-valuenow': pct == null ? 0 : Math.round(pct), 'aria-label': label },
        h('div', { class: 'fill', style: 'width:' + Math.max(0, Math.min(100, pct || 0)) + '%' })),
      h('div', { class: 'sub', text: value }), sub ? h('div', { class: 'sub', text: sub }) : null);
  }
  function loadServer() {
    return api({ q: 'server' }).then(function (r) {
      var box = $('srv'); box.textContent = '';
      var ramUsed = r.ram.total != null && r.ram.available != null ? r.ram.total - r.ram.available : null;
      var diskUsed = r.disk.total != null && r.disk.free != null ? r.disk.total - r.disk.free : null;
      var load = r.cpu.load.map(function (v) { return v.toFixed(2).replace('.', ','); }).join(' / ');
      box.append(
        meter('Процессор', r.cpu.usage, r.cpu.cores + ' ' + (r.cpu.cores === 1 ? 'ядро' : r.cpu.cores < 5 ? 'ядра' : 'ядер') + ' · нагрузка ' + load, r.cpu.model || null, ['высокая нагрузка', 'перегружен']),
        meter('Память (RAM)', ramUsed != null ? ramUsed / r.ram.total * 100 : null, bytes(ramUsed) + ' из ' + bytes(r.ram.total),
          r.swap.total ? 'подкачка: ' + bytes(r.swap.total - r.swap.free) + ' из ' + bytes(r.swap.total) : 'подкачки нет', ['занято много', 'почти закончилась']),
        meter('Диск', diskUsed != null ? diskUsed / r.disk.total * 100 : null, bytes(diskUsed) + ' из ' + bytes(r.disk.total), 'свободно ' + bytes(r.disk.free) + ' · база статистики ' + bytes(r.db), ['занято много', 'почти заполнен'])
      );
      $('srvInfo').textContent = [r.os, 'PHP ' + r.php, 'работает ' + uptime(r.uptime)].filter(Boolean).join(' · ');
    }).catch(function (e) { if (e.message !== 'auth') { $('srv').textContent = 'Не удалось получить данные сервера.'; } });
  }

  var SERIES = [{ name: 'Посетители', color: 'var(--s-visitors)', i: 1 }, { name: 'Просмотры', color: 'var(--s-views)', i: 2 }];
  function chart(series, d) {
    var box = $('chart'); box.textContent = '';
    var leg = $('legend'); leg.textContent = '';
    SERIES.forEach(function (S) { leg.appendChild(h('span', null, h('i', { style: 'background:' + S.color }), S.name)); });
    var total = series.reduce(function (a, r) { return a + r[1] + r[2]; }, 0);
    if (!total) { box.appendChild(h('p', { class: 'empty', text: 'За этот период посещений ещё нет.' })); return; }
    var W = box.clientWidth || 800, H = box.clientHeight || 240, L = 40, R = 12, T = 10, B = 26;
    var max = Math.max(1, Math.max.apply(null, series.map(function (r) { return Math.max(r[1], r[2]); })));
    var stepY = niceStep(max / 4), top = Math.ceil(max / stepY) * stepY;
    var n = series.length, x = function (i) { return L + (n === 1 ? (W - L - R) / 2 : i * (W - L - R) / (n - 1)); };
    var y = function (v) { return T + (H - T - B) * (1 - v / top); };
    var svg = s('svg', { viewBox: '0 0 ' + W + ' ' + H, role: 'img', 'aria-label': 'График посетителей и просмотров' });
    var g = s('g', { class: 'grid' });
    for (var v = 0; v <= top; v += stepY) {
      g.appendChild(s('line', { x1: L, x2: W - R, y1: y(v), y2: y(v) }));
      var t = s('text', { x: L - 8, y: y(v) + 4, 'text-anchor': 'end', class: 'ax' }); t.textContent = v; svg.appendChild(t);
    }
    svg.insertBefore(g, svg.firstChild);
    var label = function (k) { return d === 1 ? k + ':00' : k.slice(8, 10) + '.' + k.slice(5, 7); };
    var every = Math.max(1, Math.ceil(n / Math.max(2, Math.floor((W - L - R) / 64))));
    series.forEach(function (r, i) {
      if ((i % every && i !== n - 1) || (i !== n - 1 && x(n - 1) - x(i) < 46)) return;   // не налезать на последнюю подпись
      var t = s('text', { x: x(i), y: H - 6, 'text-anchor': i === 0 && n > 1 ? 'start' : i === n - 1 && n > 1 ? 'end' : 'middle', class: 'ax' });
      t.textContent = label(r[0]); svg.appendChild(t);
    });
    // площадь под посетителями — едва заметная, линии 2px
    var pts = function (idx) { return series.map(function (r, i) { return x(i) + ',' + y(r[idx]); }).join(' '); };
    svg.appendChild(s('polygon', { points: x(0) + ',' + y(0) + ' ' + pts(1) + ' ' + x(n - 1) + ',' + y(0), fill: 'var(--s-visitors)', opacity: '.12' }));
    SERIES.slice().reverse().forEach(function (S) {
      svg.appendChild(s('polyline', { points: pts(S.i), fill: 'none', stroke: S.color, 'stroke-width': 2, 'stroke-linejoin': 'round', 'stroke-linecap': 'round' }));
      if (n === 1) svg.appendChild(s('circle', { cx: x(0), cy: y(series[0][S.i]), r: 4, fill: S.color }));
    });
    // наведение: перекрестие + подсказка
    var cross = s('line', { class: 'cross', y1: T, y2: H - B, x1: 0, x2: 0, visibility: 'hidden' });
    var dots = SERIES.map(function (S) { return s('circle', { r: 4.5, fill: S.color, stroke: '#0d120d', 'stroke-width': 2, cx: 0, cy: 0, visibility: 'hidden' }); });
    svg.appendChild(cross); dots.forEach(function (c) { svg.appendChild(c); });
    var tip = h('div', { class: 'tip', hidden: '' });
    var hit = s('rect', { class: 'hit', x: L, y: T, width: W - L - R, height: H - T - B });
    function move(ev) {
      var rect = svg.getBoundingClientRect(), px = (ev.clientX - rect.left) * W / rect.width;
      var i = n === 1 ? 0 : Math.max(0, Math.min(n - 1, Math.round((px - L) / ((W - L - R) / (n - 1))))), r = series[i];
      cross.setAttribute('x1', x(i)); cross.setAttribute('x2', x(i));
      SERIES.forEach(function (S, j) { dots[j].setAttribute('cx', x(i)); dots[j].setAttribute('cy', y(r[S.i])); });
      cross.setAttribute('visibility', 'visible'); dots.forEach(function (c) { c.setAttribute('visibility', 'visible'); });
      tip.textContent = '';
      tip.appendChild(h('b', { text: d === 1 ? 'Сегодня, ' + r[0] + ':00–' + r[0] + ':59' : label(r[0]) + '.' + r[0].slice(0, 4) }));
      SERIES.forEach(function (S) { tip.appendChild(h('div', null, h('i', { style: 'background:' + S.color }), S.name + ': ' + num(r[S.i]))); });
      tip.hidden = false;
      var bx = x(i) * rect.width / W, tw = tip.offsetWidth;
      tip.style.left = Math.max(0, Math.min(rect.width - tw, bx + 12 > rect.width - tw ? bx - tw - 12 : bx + 12)) + 'px';
      tip.style.top = '8px';
    }
    function leave() { tip.hidden = true; cross.setAttribute('visibility', 'hidden'); dots.forEach(function (c) { c.setAttribute('visibility', 'hidden'); }); }
    hit.addEventListener('pointermove', move); hit.addEventListener('pointerdown', move); hit.addEventListener('pointerleave', leave);
    svg.appendChild(hit);
    box.append(svg, tip);
  }
  function niceStep(raw) {
    var p = Math.pow(10, Math.floor(Math.log10(Math.max(raw, 1)))), m = raw / p;
    return Math.max(1, (m <= 1 ? 1 : m <= 2 ? 2 : m <= 5 ? 5 : 10) * p);
  }

  var TABLES = [
    ['pages', 'Страницы', 'просмотры', pageName],
    ['sources', 'Откуда пришли', 'визиты'],
    ['actions', 'Действия', 'раз', evName],
    ['formats', 'Копирование команд по форматам', 'раз', fmtName],
    ['palettes', 'Популярные палитры', 'раз'],
    ['downloads', 'Скачивания', 'раз'],
    ['links', 'Переходы на другие сайты', 'раз'],
    ['devices', 'Устройства', 'визиты'],
    ['browsers', 'Браузеры', 'визиты'],
    ['os', 'Системы', 'визиты'],
    ['hosts', 'Сайт', 'визиты']
  ];
  function tables(t) {
    var box = $('tables'); box.textContent = '';
    TABLES.forEach(function (cfg) {
      var list = t[cfg[0]] || [], max = list.reduce(function (a, r) { return Math.max(a, r.n); }, 0);
      var ul = h('ul', { class: 'top-list' });
      list.forEach(function (r) {
        var name = cfg[3] ? cfg[3](r.k) : r.k;
        ul.appendChild(h('li', { title: String(r.k) }, h('i', { class: 'b', style: 'width:' + Math.max(2, r.n / max * 100) + '%' }),
          h('span', { class: 'k', text: name }), h('span', { class: 'n', text: num(r.n) }), h('span', { class: 'u', text: num(r.u) })));
      });
      box.appendChild(h('div', { class: 'panel' }, h('div', { class: 'ph' }, h('h2', { text: cfg[1] })),
        list.length ? h('div', { class: 'top-head' }, h('span', { text: cfg[2] }), h('span', { text: 'людей' })) : null,
        list.length ? ul : h('p', { class: 'empty', text: 'Пока пусто' })));
    });
  }

  // ---------- сейчас на сайте ----------
  function loadLive() {
    return api({ q: 'live', host: host() }).then(function (r) {
      $('liveN').textContent = r.list.length ? '· ' + r.list.length : '';
      var box = $('live'); box.textContent = '';
      if (!r.list.length) { box.appendChild(h('p', { class: 'empty', text: 'Сейчас на сайте никого нет.' })); return; }
      var tb = h('tbody');
      r.list.forEach(function (x) {
        tb.appendChild(h('tr', null,
          h('td', { class: 'who' }, vidButton(x.vid)),
          h('td', null, h('div', { class: 'ev', text: pageName(x.last_path) }), h('small', { class: 'muted', text: x.last_ev && x.last_ev !== 'pageview' ? 'последнее: ' + describe(x.last_ev, x.last_data) : x.host })),
          h('td', null, who(x)),
          h('td', { class: 't' }, 'на сайте ' + ago(r.now - x.started), h('br'), h('small', { class: 'muted', text: 'активен ' + ago(Math.max(0, r.now - x.last_seen)) + ' назад' }))));
      });
      box.appendChild(h('table', { class: 'feed' }, h('thead', null, h('tr', null, h('th', { text: 'Посетитель' }), h('th', { text: 'Где сейчас' }), h('th', { text: 'Кто' }), h('th', { text: 'Время' }))), tb));
    });
  }

  // ---------- журнал ----------
  var lastId = 0, lastDay = '';
  function filters() { return { q: 'events', day: $('fDay').value, ev: $('fEv').value, path: $('fPath').value.trim(), vid: $('fVid').value.trim(), host: host() }; }
  function loadJournal(more) {
    var f = filters(); if (more) f.before = lastId;
    return api(f).then(function (r) {
      var sel = $('fEv'), cur = sel.value;
      if (sel.options.length - 1 !== r.types.length) {
        sel.length = 1;
        r.types.filter(function (t) { return t !== 'ping'; }).forEach(function (t) { sel.appendChild(h('option', { value: t, text: evName(t) })); });
        sel.value = cur;
      }
      var box = $('journal');
      if (!more) { box.textContent = ''; lastDay = ''; box.appendChild(h('table', { class: 'feed' }, h('thead', null, h('tr', null, h('th', { text: 'Время' }), h('th', { text: 'Посетитель' }), h('th', { text: 'Действие' }), h('th', { text: 'Страница' }), h('th', { text: 'Кто' }))), h('tbody'))); }
      var tb = box.querySelector('tbody');
      if (!more && !r.list.length) tb.appendChild(h('tr', null, h('td', { colspan: 5, class: 'empty', text: 'Ничего не найдено.' })));
      r.list.forEach(function (x) {
        var dd = date(x.ts);
        if (dd !== lastDay) { lastDay = dd; tb.appendChild(h('tr', { class: 'day-sep' }, h('td', { colspan: 5, text: dd }))); }
        tb.appendChild(h('tr', null, h('td', { class: 't', text: time(x.ts) }), h('td', { class: 'who' }, vidButton(x.vid)),
          h('td', { class: 'ev', text: describe(x.ev, x.data) }), h('td', null, pageName(x.path), h('br'), h('small', { class: 'muted', text: x.host })), h('td', null, who(x))));
        lastId = x.id;
      });
      $('jMore').hidden = r.list.length < 60;
    });
  }
  var jt = null;
  ['fDay', 'fEv'].forEach(function (id) { $(id).addEventListener('change', function () { loadJournal(false); }); });
  ['fPath', 'fVid'].forEach(function (id) { $(id).addEventListener('input', function () { clearTimeout(jt); jt = setTimeout(function () { loadJournal(false); }, 350); }); });
  $('fReset').addEventListener('click', function () { ['fDay', 'fEv', 'fPath', 'fVid'].forEach(function (id) { $(id).value = ''; }); loadJournal(false); });
  $('jMore').addEventListener('click', function () { loadJournal(true); });

  // ---------- посетитель ----------
  var backTo = 'journal';
  function openVisitor(vid) {
    if (current !== 'visitor') backTo = current;
    current = 'visitor'; clearTimeout(timer);
    document.querySelectorAll('[data-pane]').forEach(function (p) { p.hidden = p.getAttribute('data-pane') !== 'visitor'; });
    var box = $('visitor'); box.textContent = 'Загрузка…';
    api({ q: 'visitor', vid: vid }).then(function (r) {
      var v = r.visitor; box.textContent = '';
      box.appendChild(h('div', { class: 'ph' }, h('h2', null, 'Посетитель ', h('b', { text: shortId(v.vid) })), h('span', { class: 'muted', text: 'полный номер ' + v.vid })));
      var info = [['Первый визит', date(v.first_seen) + ' ' + time(v.first_seen)], ['Последняя активность', date(v.last_seen) + ' ' + time(v.last_seen)],
        ['Визитов', num(v.visits)], ['Пришёл впервые из', v.source || '—'], ['Устройство', [v.device, v.browser, v.os].filter(Boolean).join(' · ')]];
      var vh = h('div', { class: 'vhead' }); box.appendChild(vh);
      info.forEach(function (i) { vh.appendChild(h('div', null, h('small', { text: i[0] }), i[1])); });
      var tb = h('tbody'), day = '';
      r.events.forEach(function (x) {
        var dd = date(x.ts);
        if (dd !== day) { day = dd; tb.appendChild(h('tr', { class: 'day-sep' }, h('td', { colspan: 4, text: dd }))); }
        tb.appendChild(h('tr', null, h('td', { class: 't', text: time(x.ts) }), h('td', null, x.visit ? h('span', { class: 'tag', text: 'визит ' + x.visit }) : ''),
          h('td', { class: 'ev', text: describe(x.ev, x.data) }), h('td', null, pageName(x.path), h('br'), h('small', { class: 'muted', text: x.host }))));
      });
      box.appendChild(h('table', { class: 'feed' }, h('thead', null, h('tr', null, h('th', { text: 'Время' }), h('th', { text: 'Визит' }), h('th', { text: 'Действие' }), h('th', { text: 'Страница' }))), tb));
      if (r.events.length >= 500) box.appendChild(h('p', { class: 'muted', text: 'Показаны последние 500 действий.' }));
    }).catch(function () { box.textContent = 'Посетитель не найден (данные хранятся 90 дней).'; });
  }
  $('vBack').addEventListener('click', function () { show(backTo); });

  // ---------- журнал входов ----------
  function loadAudit() {
    return api({ q: 'audit' }).then(function (r) {
      var box = $('audit'); box.textContent = '';
      var tb = h('tbody');
      r.list.forEach(function (x) {
        var bad = x.action === 'login_fail' || x.action === 'login_blocked';
        tb.appendChild(h('tr', null, h('td', { class: 't', text: date(x.ts) + ' ' + time(x.ts) }),
          h('td', null, h('span', { class: 'tag' + (bad ? ' bad' : x.action === 'login_ok' ? ' new' : ''), text: AUDIT[x.action] || x.action })),
          h('td', { text: x.ip || '' }), h('td', null, h('small', { class: 'muted', text: x.detail || '' }))));
      });
      if (!r.list.length) tb.appendChild(h('tr', null, h('td', { colspan: 4, class: 'empty', text: 'Записей нет.' })));
      box.appendChild(h('table', { class: 'feed' }, h('thead', null, h('tr', null, h('th', { text: 'Когда' }), h('th', { text: 'Событие' }), h('th', { text: 'IP' }), h('th', { text: 'Сайт · браузер' }))), tb));
    });
  }

  var rt = null;
  addEventListener('resize', function () { clearTimeout(rt); rt = setTimeout(function () { if (current === 'overview') loadOverview(); }, 300); });
  if (token()) startApp(); else showLogin();
})();
