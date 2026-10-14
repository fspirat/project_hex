/* fspirat.xyz — главная: управление анимацией персонажа. Без зависимостей.
 *
 * Постер (статичный кадр) есть в HTML и виден всегда, пока движение не началось:
 * без JS, при ошибке видео, при отказе автозапуска и при prefers-reduced-motion.
 *
 *  • Chrome / Edge / Firefox / Android → прозрачный WebM (VP9 + alpha).
 *  • Любой WebKit — Safari, все браузеры на iOS/iPadOS, встроенные WKWebView в приложениях macOS
 *    (WebKit показывает альфу VP9 чёрным прямоугольником) → анимированный WebP; WebM им не отдаётся никогда.
 *    WebP в двух размерах (540 и 720 px): браузер берёт по ширине сцены и плотности экрана (srcset).
 *  • Пауза: кнопка в сцене (выбор запоминается), скрытая вкладка, сцена вне экрана.
 *  • Отказ автозапуска: остаётся постер, скачивание ролика прекращается до нажатия кнопки.
 */
(function () {
  'use strict';

  var scene = document.querySelector('[data-scene]');
  if (!scene) return;
  var media = scene.querySelector('.scene-media');
  var btn = scene.querySelector('[data-motion-toggle]');
  var label = btn && btn.querySelector('[data-motion-label]');
  var root = document.documentElement;

  // Определяем движок, а не бренд «Safari»: у WKWebView в приложениях macOS в UA нет «Safari/».
  var ua = navigator.userAgent || '';
  var isWebKitOnly =
    navigator.vendor === 'Apple Computer, Inc.' ||                          // любой WebKit: Safari, WKWebView, браузеры на iOS
    /iPhone|iPad|iPod/.test(ua) ||
    (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1) ||              // iPadOS в режиме «версия для ПК»
    (/AppleWebKit\//.test(ua) && !/Chrome\/|Chromium\/|CriOS\/|Edg\/|EdgiOS\/|EdgA\/|OPR\/|Firefox\/|FxiOS\//.test(ua));

  var mq = window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : null;
  var KEY = 'fs_motion';
  function readPref() { try { return localStorage.getItem(KEY); } catch (e) { return null; } }
  function writePref(v) { try { if (v) localStorage.setItem(KEY, v); else localStorage.removeItem(KEY); } catch (e) {} }

  var saveData = !!(navigator.connection && navigator.connection.saveData);
  var pref = readPref();
  var userOff = pref === 'off' || (saveData && pref !== 'on');
  var blocked = false;   // браузер отказал в автозапуске — ждём нажатия
  var failed = false;    // источник не загрузился — остаётся постер
  var inView = true;
  var loaded = document.readyState === 'complete';
  var mode = null;       // 'video' | 'image'
  var el = null;
  var showSeq = 0;       // защита от устаревших decode() у картинки

  function reduced() { return !!(mq && mq.matches); }
  function wanted() {
    return loaded && !failed && !reduced() && !userOff && !blocked && inView && !document.hidden;
  }
  function setLive(on) { scene.classList.toggle('is-live', !!on); }

  function canPlayWebm() {
    var v = document.createElement('video');
    return !!(v.canPlayType && v.canPlayType('video/webm; codecs="vp9"'));
  }

  // Ролик, который сейчас не покажем (отказ автозапуска), не докачиваем.
  function unloadVideo() {
    try { el.pause(); el.preload = 'none'; el.removeAttribute('src'); el.load(); } catch (e) {}
  }
  function reloadVideo() {
    if (el && mode === 'video' && !el.getAttribute('src')) {
      el.preload = 'auto';
      el.src = scene.getAttribute('data-video');
    }
  }

  function fail() {
    failed = true;
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
      v.height = 674;
      v.addEventListener('playing', function () { if (wanted()) setLive(true); });
      v.addEventListener('error', function () { if (v.getAttribute('src')) fail(); });   // не реагируем на собственную выгрузку
      v.src = scene.getAttribute('data-video');
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
        // точная ширина сцены: на iPhone (×3) это ≈ 720 px — берётся 720w, на экранах ×2 — 540w
        var w = Math.round(scene.getBoundingClientRect().width);
        img.sizes = (w > 0 ? w : 240) + 'px';
        img.srcset = set;
      }
      img.src = scene.getAttribute('data-anim');
      el = img;
    }
    media.appendChild(el);
  }

  function render() {
    var want = wanted();
    if (want && !el && !failed) build();

    if (el && mode === 'video') {
      if (want) {
        var p = el.play();
        if (p && typeof p.then === 'function') {
          p.then(null, function (err) {
            // NotAllowedError — автозапуск запрещён; AbortError — play() прерван паузой, это нормально
            if (err && err.name === 'NotAllowedError') {
              blocked = true;
              setLive(false);
              if (el && mode === 'video') unloadVideo();
              updateButton();
            }
          });
        }
      } else {
        el.pause();
        // при «уменьшить движение» — статичный постер, а не замерший случайный кадр
        if (reduced()) setLive(false);
      }
    } else if (el && mode === 'image') {
      el.hidden = !want;            // [hidden] = display:none — картинка выходит из отрисовки и не крутится
      if (!want) {
        showSeq++;
        setLive(false);
      } else if (el.complete && el.naturalWidth > 0 && !scene.classList.contains('is-live')) {
        // постер убираем, только когда кадр уже декодирован, — без пустого кадра при возврате
        var seq = ++showSeq, img = el;
        var show = function () { if (seq === showSeq && el === img && wanted()) setLive(true); };
        if (img.decode) img.decode().then(show, show); else show();
      }
    }
    updateButton();
  }

  function isOn() { return !userOff && !blocked; }

  function updateButton() {
    var off = !isOn() || reduced();
    root.classList.toggle('motion-off', off);
    if (!btn) return;
    btn.hidden = reduced() || failed;
    var on = isOn();
    btn.setAttribute('data-state', on ? 'on' : 'off');
    var text = on ? 'Остановить анимацию' : 'Запустить анимацию';
    if (label) label.textContent = text;
    btn.title = text;
  }

  if (btn) {
    btn.addEventListener('click', function () {
      if (isOn()) {
        userOff = true;
        writePref('off');
      } else {
        userOff = false;
        blocked = false;
        writePref(saveData ? 'on' : null);
        reloadVideo();   // если ролик выгружали после отказа автозапуска — вернуть источник до play()
      }
      render();   // синхронно внутри нажатия — play() получает жест пользователя
    });
  }

  if ('IntersectionObserver' in window) {
    new IntersectionObserver(function (entries) {
      inView = entries[entries.length - 1].isIntersecting;
      render();
    }, { rootMargin: '80px 0px' }).observe(scene);
  }
  document.addEventListener('visibilitychange', render);
  window.addEventListener('pageshow', render);
  if (mq) {
    if (mq.addEventListener) mq.addEventListener('change', render);
    else if (mq.addListener) mq.addListener(render);
  }
  if (!loaded) {
    // тяжёлый ролик — только после загрузки страницы: шрифт, текст и постер важнее
    window.addEventListener('load', function () { loaded = true; render(); });
  }
  render();
})();
