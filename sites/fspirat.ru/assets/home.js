/* fspirat.xyz — главная: управление анимацией персонажа. Без зависимостей.
 *
 * Постер (статичный кадр) есть в HTML и виден всегда, пока движение не началось:
 * без JS, при ошибке видео, при отказе автозапуска и при prefers-reduced-motion.
 *
 *  • Chrome / Edge / Firefox / Android → прозрачный WebM (VP9 + alpha), 540, 720 или 1080 px — см. pickVideo().
 *  • Любой WebKit — Safari, все браузеры на iOS/iPadOS, встроенные WKWebView в приложениях macOS
 *    (WebKit показывает альфу VP9 чёрным прямоугольником) → анимированный WebP; WebM им не отдаётся никогда.
 *    WebP в трёх размерах (540, 720 и 900 px): браузер берёт по ширине сцены и плотности экрана (srcset) —
 *    так же, как для ролика, без растяжения: оно мылит пиксели скина при смене чёткого постера на анимацию.
 *    После поворота планшета или расширения окна файл берётся заново, только больший — см. fit().
 *  • Ролик качается после load и только когда сам персонаж на экране (не при открытии /#projects).
 *  • Кнопки паузы нет (решение владельца, 14.10.2026). Движение останавливается само: скрытая вкладка, сцена вне
 *    экрана, «уменьшить движение» (в том числе при смене настройки на открытой странице) — тогда постер.
 *  • Экономия трафика (Save-Data) и отказ автозапуска: остаётся постер, ролик не качается.
 *  • Смена «постер ↔ движение» — растворением (home.css), кроме «уменьшить движение».
 */
(function () {
  'use strict';

  // Плавная прокрутка к якорям (html.smooth в CSS) — только после загрузки: переход по ссылке /#contacts
  // при открытии страницы мгновенный, а не «проезд» через hero, во время которого начинал качаться ролик.
  (function () {
    function add() { document.documentElement.classList.add('smooth'); }
    function on() { if (window.requestAnimationFrame) window.requestAnimationFrame(add); else add(); }
    if (document.readyState === 'complete') on(); else window.addEventListener('load', on);
  })();

  var scene = document.querySelector('[data-scene]');
  if (!scene) return;
  var media = scene.querySelector('.scene-media');
  var root = document.documentElement;

  // Определяем движок, а не бренд «Safari»: у WKWebView в приложениях macOS в UA нет «Safari/».
  var ua = navigator.userAgent || '';
  var isWebKitOnly =
    navigator.vendor === 'Apple Computer, Inc.' ||                          // любой WebKit: Safari, WKWebView, браузеры на iOS
    /iPhone|iPad|iPod/.test(ua) ||
    (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1) ||              // iPadOS в режиме «версия для ПК»
    (/AppleWebKit\//.test(ua) && !/Chrome\/|Chromium\/|CriOS\/|Edg\/|EdgiOS\/|EdgA\/|OPR\/|Firefox\/|FxiOS\//.test(ua));

  var mq = window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : null;

  var saveData = !!(navigator.connection && navigator.connection.saveData);   // экономия трафика — только постер
  var blocked = false;   // браузер отказал в автозапуске — остаётся постер
  var failed = false;    // источник не загрузился — остаётся постер
  // Сцена «видна» только по ответу IntersectionObserver (первый приходит сразу после observe), а не заранее:
  // иначе, если load опередит этот ответ, ролик начнёт качаться для сцены, которая уже за краем экрана.
  // inView — с запасом 80 px: пауза и продолжение уже загруженного ролика. onScreen — без запаса: только он
  // разрешает скачивание. При открытии /#projects низ сцены на 14–43 px выше экрана, внутри запаса
  var hasIO = 'IntersectionObserver' in window;
  var inView = !hasIO;
  var onScreen = !hasIO;
  var loaded = document.readyState === 'complete';
  var mode = null;       // 'video' | 'image'
  var el = null;
  var showSeq = 0;       // защита от устаревших decode() у картинки
  var FADE = 360;        // растворение «постер ↔ движение» в home.css: 0.12 с задержки + 0.2 с, с запасом
  var hideTimer = 0;     // картинку после растворения в постер убираем из отрисовки
  var swapTimer = 0;     // смена файла на больший (fit) ждёт, пока сцена растворится в постер

  function reduced() { return !!(mq && mq.matches); }
  // движения нет совсем: «уменьшить движение», экономия трафика, отказ автозапуска или ошибка
  function stopped() { return failed || reduced() || saveData || blocked; }
  function wanted() {
    return loaded && !stopped() && inView && !document.hidden;
  }
  function isLive() { return scene.classList.contains('is-live'); }
  function setLive(on) { scene.classList.toggle('is-live', !!on); }
  function sceneWidth() { return scene.getBoundingClientRect().width; }
  function dpr() { return window.devicePixelRatio || 1; }

  function canPlayWebm() {
    var v = document.createElement('video');
    return !!(v.canPlayType && v.canPlayType('video/webm; codecs="vp9"'));
  }

  // Какой ролик: наименьший из 540 / 720 / 1080 px, которого хватает на ширину сцены × devicePixelRatio.
  // Растягивать нельзя ни на сколько: любое растяжение уже мылит края пикселей скина (замер против 1080: края
  // мягче на 9–14 % уже при ×1.02 и на 12–18 % при ×1.33), а постер браузер берёт 1080w с той же границы 540 —
  // иначе при смене «постер → ролик» персонаж заметно мутнел бы (ноутбук с масштабом 125 %: сцена 440 px =
  // 550 точек). 720 — телефоны Android и планшеты ×2 (сцена 576–720 точек): тот же вид, файл на 0.39 МБ легче.
  var videoSrc = null;
  function videoTiers() {
    return [scene.getAttribute('data-video'), scene.getAttribute('data-video-md'), scene.getAttribute('data-video-hd')];
  }
  function pickVideo() {
    var t = videoTiers();
    var px = Math.round(sceneWidth() * dpr());
    if (px <= 540 || (!t[1] && !t[2])) return t[0];
    if (t[1] && (px <= 720 || !t[2])) return t[1];
    return t[2];
  }
  function tier(src) { return videoTiers().indexOf(src); }

  // Варианты анимированного WebP: [[адрес, ширина], …] из data-anim-srcset; ширина показанного сейчас
  function animSet() {
    return (scene.getAttribute('data-anim-srcset') || '').split(',').map(function (s) {
      var p = s.trim().split(/\s+/);
      return [p[0] || '', parseInt(p[1], 10) || 0];
    });
  }
  function animWidth() {
    var cur = el.currentSrc || '', w = 0;
    animSet().forEach(function (c) { if (c[0] && cur.slice(-c[0].length) === c[0]) w = c[1]; });
    return w;
  }

  // Ролик, который не покажем (отказ автозапуска), не докачиваем.
  function unloadVideo() {
    try { el.pause(); el.preload = 'none'; el.removeAttribute('src'); el.load(); } catch (e) {}
  }

  function fail() {
    failed = true;
    clearTimeout(swapTimer); swapTimer = 0;
    clearTimeout(hideTimer);
    setLive(false);
    if (el) {
      if (mode === 'video') { try { el.pause(); el.removeAttribute('src'); el.load(); } catch (e) {} }
      if (el.parentNode) el.parentNode.removeChild(el);
      el = null;
    }
    render();
  }

  function build() {
    if (!isWebKitOnly && canPlayWebm()) {
      mode = 'video';
      var v = document.createElement('video');
      v.className = 'scene-motion';
      v.muted = true;
      v.defaultMuted = true;
      v.loop = true;
      v.playsInline = true;
      v.setAttribute('muted', '');
      v.setAttribute('loop', '');
      v.setAttribute('playsinline', '');
      v.setAttribute('aria-hidden', 'true');
      v.setAttribute('tabindex', '-1');
      v.setAttribute('disablepictureinpicture', '');
      v.setAttribute('disableremoteplayback', '');
      v.preload = 'auto';
      v.width = 540;
      v.height = 675;
      v.addEventListener('playing', function () { if (wanted() && !swapTimer) setLive(true); });
      v.addEventListener('error', function () { if (v.getAttribute('src')) fail(); });   // не реагируем на собственную выгрузку
      videoSrc = pickVideo();
      v.src = videoSrc;
      el = v;
    } else {
      mode = 'image';
      var img = new Image();
      img.className = 'scene-motion';
      img.alt = '';
      img.setAttribute('aria-hidden', 'true');
      img.width = 540;
      img.height = 675;
      img.decoding = 'async';
      img.addEventListener('load', render);
      img.addEventListener('error', fail);
      var set = scene.getAttribute('data-anim-srcset');
      if (set) {
        // точная ширина сцены в sizes: браузер берёт наименьший файл, которого хватает на ширину × плотность.
        // iPhone ×3 (сцена 237 px ≈ 711 точек) — 720w, iPhone ×2 — 540w, iPad вертикально (288 × 2) — 720w,
        // iPad горизонтально и Mac Retina (сцена 427–440 px ≈ 854–880 точек) — 900w
        var w = Math.round(sceneWidth());
        img.sizes = (w > 0 ? w : 240) + 'px';
        img.srcset = set;
      }
      img.src = scene.getAttribute('data-anim');
      el = img;
    }
    media.appendChild(el);
  }

  // Файл движения подобран по ширине сцены × плотности экрана при первом показе. После поворота планшета,
  // расширения окна или смены масштаба его может не хватить (iPad 820 → 1180: WebP 720 растянут ×1.19) — тогда
  // берём больший. Только вверх: при сужении меньший не качаем. Сначала сцена растворяется в постер (= кадр 0,
  // браузер уже взял ему 1080w), новый файл начинается с кадра 0 — без скачка позы и без растянутого кадра.
  function fit() {
    if (!el || failed) return;
    var change;
    if (mode === 'video') {
      var next = pickVideo();
      if (tier(next) <= tier(videoSrc)) return;
      videoSrc = next;
      if (!el.getAttribute('src')) return;   // выгружен после отказа автозапуска — больше не показываем
      change = function () { el.src = videoSrc; };
    } else {
      var w = Math.round(sceneWidth()), cur = animWidth(), max = 0;
      animSet().forEach(function (c) { if (c[1] > max) max = c[1]; });
      if (!w || !cur || cur >= max || Math.round(w * dpr()) <= cur || el.sizes === w + 'px') return;
      // браузер сам возьмёт из srcset больший файл; пока он грузится, complete = false и показа нет,
      // по load — render(): decode() и растворение из постера
      change = function () { el.sizes = w + 'px'; };
    }
    showSeq++;
    clearTimeout(swapTimer);
    var target = el;
    var go = function () {
      swapTimer = 0;
      if (el !== target || failed) return;
      change();
      render();
    };
    if (isLive()) {
      setLive(false);
      swapTimer = setTimeout(go, FADE);
    } else {
      go();
    }
  }

  function render() {
    var want = wanted();
    if (want && onScreen && !el && !failed) build();
    if (want && el && !swapTimer) fit();

    if (el && mode === 'video') {
      if (swapTimer) {
        if (!want) el.pause();       // ждём растворения в постер, потом новый источник (fit)
      } else if (want) {
        // виден постер (= кадр 0), а ролик стоит на середине цикла (было «уменьшить движение»): с начала,
        // иначе при смене «постер → ролик» персонаж скачком меняет позу. Вне экрана и в скрытой вкладке
        // is-live остаётся — там на экране и так этот кадр, продолжаем с него
        if (!scene.classList.contains('is-live') && el.currentTime > 0) {
          try { el.currentTime = 0; } catch (e) {}
        }
        var p = el.play();
        if (p && typeof p.then === 'function') {
          p.then(null, function (err) {
            // NotAllowedError — автозапуск запрещён; AbortError — play() прерван паузой, это нормально
            if (err && err.name === 'NotAllowedError') {
              blocked = true;
              setLive(false);
              if (el && mode === 'video') unloadVideo();
            }
          });
        }
      } else {
        el.pause();
        // при «уменьшить движение» — статичный постер, а не замерший случайный кадр
        if (reduced()) setLive(false);
      }
    } else if (el && mode === 'image') {
      var img = el;
      if (want) {
        clearTimeout(hideTimer);
        img.hidden = false;
        if (!swapTimer && img.complete && img.naturalWidth > 0 && !isLive()) {
          // постер убираем, только когда кадр уже декодирован, — без пустого кадра при возврате
          var seq = ++showSeq;
          var show = function () { if (seq === showSeq && el === img && !swapTimer && wanted()) setLive(true); };
          if (img.decode) img.decode().then(show, show); else show();
        }
      } else if (stopped() && !img.hidden) {
        // «уменьшить движение»: постер (растворением), потом картинку убираем из отрисовки
        // ([hidden] = display:none — она не крутится). Продолжит она не с кадра 0 — снова растворением
        showSeq++;
        setLive(false);
        clearTimeout(hideTimer);
        hideTimer = setTimeout(function () { if (el === img && !isLive()) img.hidden = true; }, FADE);
      }
      // Скрытая вкладка, сцена вне экрана — ничего не меняем: анимированную картинку в фоновой вкладке и за
      // краем экрана браузеры (WebKit, Chrome) сами не крутят, а смена «WebP → постер → WebP» давала скачок позы:
      // постер — кадр 0, а картинка продолжает с другого кадра
    }
  }

  if (hasIO) {
    new IntersectionObserver(function (entries) {
      inView = entries[entries.length - 1].isIntersecting;
      render();
    }, { rootMargin: '80px 0px' }).observe(scene);
    // отдельный наблюдатель без запаса: у первого при прокрутке внутри запаса новых ответов нет,
    // и проверка положения в render() не заметила бы, что сцена появилась на экране.
    // Смотрим не на весь кадр, а на полосу, где сам персонаж (10–67 % высоты, .scene-probe): низ кадра пустой
    // и заходит под «Проекты» — при открытии /#projects иначе качался бы ролик, которого не видно
    var probe = document.createElement('div');
    probe.className = 'scene-probe';
    probe.setAttribute('aria-hidden', 'true');
    scene.appendChild(probe);
    new IntersectionObserver(function (entries) {
      onScreen = entries[entries.length - 1].isIntersecting;
      render();
    }).observe(probe);
  }
  document.addEventListener('visibilitychange', render);
  window.addEventListener('pageshow', render);
  // поворот, ширина окна, масштаб: после того как размер устоялся, render() → fit() проверит, хватает ли файла
  // (на паузе и вне экрана — при следующем показе)
  var resizeTimer = 0;
  function onResize() { clearTimeout(resizeTimer); resizeTimer = setTimeout(render, 250); }
  window.addEventListener('resize', onResize);
  window.addEventListener('orientationchange', onResize);
  if (mq) {
    if (mq.addEventListener) mq.addEventListener('change', render);
    else if (mq.addListener) mq.addListener(render);
  }
  if (!loaded) {
    // тяжёлый ролик — только после загрузки страницы: шрифт, текст и постер важнее
    window.addEventListener('load', function () { loaded = true; render(); });
  }

  // Появление сцены (CSS-анимация rise на .enter .hero-scene) — один раз за визит. Класс .enter ставит скрипт
  // в <head> до первой отрисовки; снимаем его, когда анимация закончилась или не нужна. Пока класс висит,
  // смена «уменьшить движение» → «без ограничений» снова применила бы animation, и сцена мигнула бы
  (function () {
    var box = scene.parentNode;   // .hero-scene
    var ended = false;
    function done() { if (!ended) { ended = true; root.classList.remove('enter'); } }
    if (reduced() || !box || !box.classList || !box.classList.contains('hero-scene')) { done(); return; }
    if (box.getAnimations) {
      var list = box.getAnimations();
      if (!list.length) { done(); return; }
      Promise.all(list.map(function (a) { return a.finished; })).then(done, done);
    } else {
      box.addEventListener('animationend', function (e) { if (e.target === box) done(); });
      box.addEventListener('animationcancel', function (e) { if (e.target === box) done(); });
      setTimeout(done, 1500);   // если animationend уже прошёл до запуска скрипта
    }
  })();

  render();
})();
