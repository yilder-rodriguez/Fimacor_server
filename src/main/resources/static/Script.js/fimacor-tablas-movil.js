// Pone en cada celda su etiqueta (el titulo de su columna) para la vista de tarjetas en celular.
// No toca las celdas que ya traen data-label ni las filas con celdas combinadas (mensajes de "no hay datos").
(function () {
  document.querySelectorAll("table.tablemaquinasregistradas").forEach(function (tabla) {
    var titulos = Array.prototype.map.call(tabla.querySelectorAll("thead th"), function (th) {
      return th.textContent.trim();
    });
    tabla.querySelectorAll("tbody tr").forEach(function (fila) {
      Array.prototype.forEach.call(fila.children, function (celda, i) {
        if (celda.tagName !== "TD" || celda.hasAttribute("colspan") || celda.hasAttribute("data-label")) return;
        if (titulos[i]) celda.setAttribute("data-label", titulos[i]);
      });
    });
  });
})();
