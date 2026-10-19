// FIMACOR · Zoom fijo al 100 %.
// Se carga en el <head> (sin defer) del login y las pantallas publicas siempre, y en las pantallas
// internas solo si el Administrador lo activo en Configuracion > Preferencias.
//  1. Celulares: viewport sin escala, sin pellizco ni doble toque (Android, iOS).
//  2. Escritorio: bloquea Ctrl/Cmd + rueda, pellizco del touchpad, Ctrl/Cmd + "+" / "-" / "0"
//     (Chrome, Edge, Opera, Firefox, Safari).
//  3. Si la pagina igual queda con otro zoom (lo recuerda el navegador o se cambio desde su menu),
//     se compensa para que se vea exactamente como al 100 %.
(function () {
  if (window.__fimacorZoomFijo) return;
  window.__fimacorZoomFijo = true;

  const raiz = document.documentElement;
  const CLAVE_ESCALA = "fimacorEscalaPantalla";
  const ESCALAS_SISTEMA = [1, 1.1, 1.2, 1.25, 1.5, 1.75, 2, 2.25, 2.5, 3, 3.5, 4];

  // 1. Viewport de celular sin zoom.
  let viewport = document.querySelector('meta[name="viewport"]');
  if (!viewport) {
    viewport = document.createElement("meta");
    viewport.name = "viewport";
    (document.head || raiz).appendChild(viewport);
  }
  viewport.setAttribute("content", "width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover");
  raiz.style.touchAction = "pan-x pan-y";
  raiz.style.webkitTextSizeAdjust = "100%";
  raiz.style.textSizeAdjust = "100%";

  const bloquear = function (evento) {
    if (evento.cancelable) evento.preventDefault();
  };

  // 2a. Ctrl/Cmd + rueda y pellizco del touchpad (llega como rueda con ctrlKey).
  window.addEventListener("wheel", function (evento) {
    if (evento.ctrlKey || evento.metaKey) bloquear(evento);
  }, { passive: false, capture: true });

  // 2b. Teclado: "+", "=", "-", "_" y "0" con Ctrl o Cmd (fila principal y teclado numerico).
  window.addEventListener("keydown", function (evento) {
    if (!(evento.ctrlKey || evento.metaKey)) return;
    const teclas = ["+", "=", "-", "_", "0"];
    const codigos = ["Equal", "Minus", "Digit0", "NumpadAdd", "NumpadSubtract", "Numpad0"];
    if (teclas.indexOf(evento.key) >= 0 || codigos.indexOf(evento.code) >= 0) bloquear(evento);
  }, { capture: true });

  // 2c. Safari (macOS e iOS): gestos de pellizco.
  ["gesturestart", "gesturechange", "gestureend"].forEach(function (tipo) {
    document.addEventListener(tipo, bloquear, { passive: false });
  });

  // 2d. Pantallas tactiles: dos dedos (pellizco) y doble toque rapido.
  document.addEventListener("touchmove", function (evento) {
    if (evento.touches && evento.touches.length > 1) bloquear(evento);
  }, { passive: false });
  let ultimoToque = 0;
  document.addEventListener("touchend", function (evento) {
    const ahora = Date.now();
    if (ahora - ultimoToque < 300 && !evento.target.closest("input, textarea, select, [contenteditable]")) bloquear(evento);
    ultimoToque = ahora;
  }, { passive: false });

  // 3. Compensacion del zoom que el navegador ya tenga aplicado.
  function leerEscala() {
    try { return parseFloat(window.localStorage.getItem(CLAVE_ESCALA)) || 0; } catch (e) { return 0; }
  }

  function guardarEscala(valor) {
    try { window.localStorage.setItem(CLAVE_ESCALA, String(valor)); } catch (e) { /* sin almacenamiento */ }
  }

  // Zoom del navegador (1 = 100 %). Con outerWidth ~ innerWidth el zoom es 100 % y se guarda la escala
  // de pantalla del equipo (devicePixelRatio); despues el zoom exacto es devicePixelRatio / escala.
  // Sin escala guardada solo se confia en outerWidth / innerWidth si cuadra con una escala real del
  // sistema operativo; si no (paneles laterales, emulacion), se asume 100 % y no se toca nada.
  function zoomNavegador() {
    const dpr = window.devicePixelRatio || 1;
    const proporcion = window.outerWidth && window.innerWidth ? window.outerWidth / window.innerWidth : 0;
    if (proporcion > 0.98 && proporcion < 1.025) {
      guardarEscala(dpr);
      return 1;
    }
    const escala = leerEscala();
    if (escala) return dpr / escala;
    if (!proporcion) return 1;
    const estimada = dpr / proporcion;
    return ESCALAS_SISTEMA.some(function (e) { return Math.abs(e - estimada) < 0.03; }) ? proporcion : 1;
  }

  // Solo escritorio: en celulares el viewport ya fija la escala y outerWidth no es confiable.
  const escritorio = !window.matchMedia || window.matchMedia("(pointer: fine)").matches;

  function compensar() {
    if (!escritorio) return;
    const zoom = zoomNavegador();
    if (Math.abs(zoom - 1) > 0.03) {
      const factor = Math.max(0.25, Math.min(4, 1 / zoom));
      raiz.style.zoom = String(factor);
      raiz.style.setProperty("--fm-zoom", String(factor));
    } else if (raiz.style.zoom) {
      raiz.style.zoom = "";
      raiz.style.removeProperty("--fm-zoom");
    }
  }

  // Con la compensacion, las unidades vh tambien se encogen (el sidebar de 100vh quedaba corto y dejaba
  // un hueco blanco abajo). Se divide la altura por el mismo factor para que vuelva a llenar la ventana.
  const estiloAltura = document.createElement("style");
  estiloAltura.textContent =
    ".vh-100{height:calc(100vh / var(--fm-zoom,1))!important}" +
    ".min-vh-100{min-height:calc(100vh / var(--fm-zoom,1))!important}";
  (document.head || raiz).appendChild(estiloAltura);

  compensar();
  window.addEventListener("resize", compensar);
  if (window.matchMedia) {
    // Cambios de zoom o de monitor cambian devicePixelRatio aunque no siempre disparan resize.
    const vigilarDpr = function () {
      const consulta = window.matchMedia("(resolution: " + (window.devicePixelRatio || 1) + "dppx)");
      const alCambiar = function () {
        if (consulta.removeEventListener) consulta.removeEventListener("change", alCambiar);
        compensar();
        vigilarDpr();
      };
      if (consulta.addEventListener) consulta.addEventListener("change", alCambiar);
    };
    vigilarDpr();
  }
})();
