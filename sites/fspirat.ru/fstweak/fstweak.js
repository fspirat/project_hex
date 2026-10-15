/* /fstweak/ — галерея скриншотов и окна справки.
 * Ничего не движется само: нет циклов, таймеров и requestAnimationFrame, поэтому кнопка паузы не нужна.
 * Аналитика — только прежнее событие fsTrack('fstweak.more', {id}) при открытии окна; новых событий нет. */
(function () {
  'use strict';
  var doc = document, root = doc.documentElement;

  // Плавная прокрутка по якорям — только после загрузки: /fstweak/#download открывается сразу на месте.
  // При «уменьшить движение» CSS (site.css) её не включает.
  function smooth() { root.classList.add('smooth'); }
  if (doc.readyState === 'complete') smooth(); else addEventListener('load', smooth);

  /* ── Галерея: кнопки, номера/названия, ← → Home End внутри #screens, свайп по снимку ── */
  (function () {
    var screens = doc.getElementById('screens'), view = doc.getElementById('gView');
    if (!screens || !view) return;
    var img = view.querySelector('img');
    var title = doc.getElementById('gTitle'), text = doc.getElementById('gText');
    var count = doc.getElementById('gCount'), countSr = doc.getElementById('gCountSr');
    var orig = doc.getElementById('gOrig');
    var dots = [].slice.call(doc.querySelectorAll('#gDots [data-shot]'));
    var shots = dots.map(function (b) {
      return { src: b.getAttribute('data-shot'), w: b.getAttribute('data-w'), h: b.getAttribute('data-h'),
               t: b.getAttribute('data-title'), d: b.getAttribute('data-text') };
    });
    var n = shots.length, cur = 0, warm = {};
    if (!n) return;
    warm[shots[0].src] = true;

    function alt(s) { return s.t + ': ' + s.d.charAt(0).toLowerCase() + s.d.slice(1); }
    // Остальные снимки не грузятся заранее: только соседние, и только после первого переключения.
    function prefetch(i) {
      var s = shots[(i + n) % n];
      if (warm[s.src]) return;
      warm[s.src] = true;
      var im = new Image();
      im.decoding = 'async';
      im.src = s.src;
    }
    function show(i) {
      cur = (i + n) % n;
      var s = shots[cur];
      if (img.getAttribute('src') !== s.src) {
        view.setAttribute('aria-busy', 'true');
        img.src = s.src;
        img.width = +s.w;
        img.height = +s.h;
      }
      img.alt = alt(s);
      title.textContent = s.t;
      text.textContent = s.d;
      count.textContent = (cur + 1) + ' / ' + n;
      countSr.textContent = 'Скриншот ' + (cur + 1) + ' из ' + n + '.';
      orig.href = s.src;
      dots.forEach(function (b, k) { b.setAttribute('aria-pressed', k === cur ? 'true' : 'false'); });
      prefetch(cur + 1);
      prefetch(cur - 1);
    }
    function ready() { view.removeAttribute('aria-busy'); }
    img.addEventListener('load', ready);
    img.addEventListener('error', ready);

    doc.getElementById('gPrev').addEventListener('click', function () { show(cur - 1); });
    doc.getElementById('gNext').addEventListener('click', function () { show(cur + 1); });
    dots.forEach(function (b, k) { b.addEventListener('click', function () { show(k); }); });

    // Клавиши — только пока фокус внутри раздела «Скриншоты» (не на всей странице, как раньше).
    screens.addEventListener('keydown', function (e) {
      if (e.altKey || e.ctrlKey || e.metaKey || e.shiftKey) return;
      var k = e.key;
      var to = k === 'ArrowLeft' ? cur - 1 : k === 'ArrowRight' ? cur + 1 : k === 'Home' ? 0 : k === 'End' ? n - 1 : null;
      if (to === null) return;
      e.preventDefault();
      var onDot = dots.indexOf(doc.activeElement) !== -1;
      show(to);
      if (onDot) dots[cur].focus();
    });

    // Свайп: горизонтальный жест длиннее 40 px; вертикальная прокрутка страницы не мешает (touch-action: pan-y).
    var x0 = null, y0 = 0;
    view.addEventListener('touchstart', function (e) {
      if (e.touches.length !== 1) { x0 = null; return; }
      x0 = e.touches[0].clientX;
      y0 = e.touches[0].clientY;
    }, { passive: true });
    view.addEventListener('touchend', function (e) {
      if (x0 === null) return;
      var t = e.changedTouches[0], dx = t.clientX - x0, dy = t.clientY - y0;
      x0 = null;
      if (Math.abs(dx) > 40 && Math.abs(dx) > Math.abs(dy)) show(cur + (dx < 0 ? 1 : -1));
    });
    view.addEventListener('touchcancel', function () { x0 = null; });
  })();

  /* ── Окна справки: showModal, Escape и клик по фону закрывают, фокус возвращается на кнопку ── */
  (function () {
    var opener = null;
    [].forEach.call(doc.querySelectorAll('[data-open]'), function (b) {
      b.addEventListener('click', function () {
        var d = doc.getElementById(b.dataset.open);
        if (!d || d.open) return;
        opener = b;
        d.showModal();
        doc.body.classList.add('fst-locked');
        if (window.fsTrack) try { fsTrack('fstweak.more', { id: b.dataset.open }); } catch (e) {}
      });
    });
    [].forEach.call(doc.querySelectorAll('dialog.fst-sheet'), function (d) {
      d.addEventListener('close', function () {
        doc.body.classList.remove('fst-locked');
        var o = opener;
        opener = null;
        if (o && doc.contains(o)) o.focus();
      });
      d.addEventListener('click', function (e) { if (e.target === d) d.close(); });
      var x = d.querySelector('[data-close]');
      if (x) x.addEventListener('click', function () { d.close(); });
    });
  })();
})();
