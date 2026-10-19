// FIMACOR · comportamiento Material 3 de las pantallas publicas del QR:
// entradas escalonadas, ripple, barra superior que se eleva al desplazarse,
// contador de caracteres, codigo en mayusculas y estado "enviando" del formulario.
(function () {
  const raiz = document.documentElement;
  const sinMovimiento = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (!sinMovimiento) raiz.classList.add("md-motion");

  document.querySelectorAll("[data-md-stagger]").forEach(function (grupo) {
    Array.prototype.forEach.call(grupo.children, function (hijo, i) {
      hijo.style.setProperty("--md-i", i);
    });
  });

  document.addEventListener("pointerdown", function (evento) {
    if (sinMovimiento) return;
    const objetivo = evento.target.closest(".md-btn, .md-filter-chip > span");
    if (!objetivo) return;
    const caja = objetivo.getBoundingClientRect();
    const tamano = Math.max(caja.width, caja.height) * 2.2;
    const onda = document.createElement("span");
    onda.className = "md-ripple";
    onda.style.width = onda.style.height = tamano + "px";
    onda.style.left = (evento.clientX - caja.left - tamano / 2) + "px";
    onda.style.top = (evento.clientY - caja.top - tamano / 2) + "px";
    objetivo.appendChild(onda);
    onda.addEventListener("animationend", function () { onda.remove(); });
  });

  const barra = document.querySelector(".md-top-app-bar");
  if (barra) {
    const revisar = function () { barra.classList.toggle("is-scrolled", window.scrollY > 4); };
    window.addEventListener("scroll", revisar, { passive: true });
    revisar();
  }

  document.querySelectorAll("[data-md-counter]").forEach(function (campo) {
    const salida = document.getElementById(campo.dataset.mdCounter);
    if (!salida) return;
    const actualizar = function () { salida.textContent = campo.value.length + " / " + campo.maxLength; };
    campo.addEventListener("input", actualizar);
    actualizar();
  });

  document.querySelectorAll("[data-md-mayusculas]").forEach(function (campo) {
    campo.addEventListener("input", function () {
      const pos = campo.selectionStart;
      campo.value = campo.value.toUpperCase();
      campo.setSelectionRange(pos, pos);
    });
  });

  // El evento submit solo llega cuando el formulario ya paso la validacion del navegador.
  document.querySelectorAll("form[data-md-enviando]").forEach(function (formulario) {
    formulario.addEventListener("submit", function () {
      const boton = formulario.querySelector("[type=submit]");
      if (!boton) return;
      boton.classList.add("is-loading");
      const etiqueta = boton.querySelector(".md-btn__label");
      if (etiqueta) etiqueta.textContent = formulario.dataset.mdEnviando;
    });
  });
})();
