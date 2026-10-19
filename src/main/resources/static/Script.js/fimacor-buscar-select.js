// FIMACOR · Caja de busqueda para listas desplegables largas (maquinas, cuentadantes...).
// Uso: agregar el atributo data-buscable al <select>. Opcional: data-buscable-texto="Buscar ..." para el texto de la caja.
// Escribir filtra las opciones; si lo escrito deja de coincidir con la opcion elegida, se conserva solo si sigue visible.
(function () {
  function normalizar(valor) {
    return (valor || "").toString().normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();
  }

  document.querySelectorAll("select[data-buscable]").forEach(function (select) {
    const opciones = Array.prototype.slice.call(select.options).map(function (o) { return o.cloneNode(true); });
    const placeholder = opciones.filter(function (o) { return o.value === ""; })[0] || null;
    const datos = opciones.filter(function (o) { return o.value !== ""; });
    if (datos.length < 2) return; // con una sola opcion no hace falta buscar

    const caja = document.createElement("input");
    caja.type = "search";
    caja.className = "form-control mb-2";
    caja.placeholder = select.getAttribute("data-buscable-texto") || "Buscar...";
    caja.autocomplete = "off";
    caja.setAttribute("aria-label", caja.placeholder);
    select.parentNode.insertBefore(caja, select);

    caja.addEventListener("input", function () {
      const termino = normalizar(caja.value);
      const elegido = select.value;
      const coinciden = termino ? datos.filter(function (o) {
        return normalizar(o.getAttribute("data-buscar") || o.textContent).indexOf(termino) >= 0;
      }) : datos;

      select.innerHTML = "";
      if (placeholder) {
        const base = placeholder.cloneNode(true);
        if (termino && coinciden.length === 0) base.textContent = "Sin resultados para esa búsqueda";
        select.appendChild(base);
      }
      coinciden.forEach(function (o) {
        const copia = o.cloneNode(true);
        copia.selected = copia.value === elegido;
        select.appendChild(copia);
      });
      if (!coinciden.some(function (o) { return o.value === elegido; })) select.value = "";
    });
  });
})();
