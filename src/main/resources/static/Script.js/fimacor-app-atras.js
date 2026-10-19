// FIMACOR · Boton "atras" dentro de la app del iPhone (iOS no tiene boton atras como Android).
// Solo se muestra en la app de iPhone, en las pantallas internas que tienen cabecera y cuando hay una pagina anterior.
// En el navegador y en Android no hace nada: ahi ya existe el boton atras del sistema.
(function () {
  const cap = window.Capacitor;
  if (!cap || typeof cap.getPlatform !== "function" || cap.getPlatform() !== "ios") return;
  if (history.length <= 1 || location.pathname === "/menu" || location.pathname === "/login") return;

  function colocar() {
    const cabecera = document.querySelector("header.sticky-top > div");
    if (!cabecera || cabecera.querySelector("[data-fimacor-atras]")) return;
    const boton = document.createElement("button");
    boton.type = "button";
    boton.className = "btn btn-light border flex-shrink-0";
    boton.setAttribute("data-fimacor-atras", "");
    boton.setAttribute("aria-label", "Volver a la pantalla anterior");
    boton.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"/></svg>';
    boton.addEventListener("click", function () { history.back(); });
    cabecera.insertBefore(boton, cabecera.firstChild);
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", colocar);
  else colocar();
})();
