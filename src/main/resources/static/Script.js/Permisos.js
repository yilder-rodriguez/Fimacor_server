// Matriz de permisos (paginas de Permisos y Roles): contadores por modulo, marcar modulo completo,
// filtro y aviso de cambios sin guardar. Cada formulario con data-matriz-permisos es independiente.
document.addEventListener("DOMContentLoaded", function () {
  document.querySelectorAll("[data-matriz-permisos]").forEach(function (form) {
    const idRol = form.dataset.rol;
    const casillas = Array.from(form.querySelectorAll("input[name='permisos']"));
    const contadorActivos = form.querySelector("[data-contador-activos]");
    const contadorRol = idRol ? document.querySelector("[data-contador-rol='" + idRol + "']") : null;
    const avisoSinGuardar = form.querySelector("[data-sin-guardar]");
    const filtro = form.querySelector("[data-filtro-permisos]");
    const sinResultados = form.querySelector("[data-sin-resultados]");
    const inicial = casillas.map(function (c) { return c.checked; });

    function actualizar() {
      form.querySelectorAll("[data-modulo]").forEach(function (modulo) {
        const propias = Array.from(modulo.querySelectorAll("input[name='permisos']"));
        const marcadas = propias.filter(function (c) { return c.checked; }).length;
        const interruptor = modulo.querySelector("[data-toggle-modulo]");
        modulo.querySelector("[data-contador-modulo]").textContent = marcadas + "/" + propias.length;
        interruptor.checked = marcadas === propias.length && propias.length > 0;
        interruptor.indeterminate = marcadas > 0 && marcadas < propias.length;
      });
      const total = casillas.filter(function (c) { return c.checked; }).length;
      if (contadorActivos) contadorActivos.textContent = total;
      if (contadorRol) contadorRol.textContent = total;
      const cambiado = casillas.some(function (c, i) { return c.checked !== inicial[i]; });
      if (avisoSinGuardar) avisoSinGuardar.classList.toggle("d-none", !cambiado);
    }

    form.addEventListener("change", function (evento) {
      const interruptor = evento.target.closest("[data-toggle-modulo]");
      if (interruptor) {
        interruptor.closest("[data-modulo]").querySelectorAll("[data-permiso-item]:not(.d-none) input[name='permisos']")
          .forEach(function (c) { c.checked = interruptor.checked; });
      }
      actualizar();
    });

    form.querySelectorAll("[data-marcar]").forEach(function (boton) {
      boton.addEventListener("click", function () {
        const marcar = boton.dataset.marcar === "todos";
        form.querySelectorAll("[data-permiso-item]:not(.d-none) input[name='permisos']")
          .forEach(function (c) { c.checked = marcar; });
        actualizar();
      });
    });

    form.addEventListener("reset", function () {
      setTimeout(actualizar, 0);
    });

    // Cerrar el modal sin guardar descarta los cambios hechos en las casillas.
    form.addEventListener("click", function (evento) {
      if (evento.target.closest(".modalmaquina-cerrar") && form.tagName === "FORM") {
        form.reset();
        if (filtro) { filtro.value = ""; filtro.dispatchEvent(new Event("input")); }
      }
    });

    if (filtro) {
      filtro.addEventListener("input", function () {
        const termino = filtro.value.trim().toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
        let visibles = 0;
        form.querySelectorAll("[data-modulo]").forEach(function (modulo) {
          let visiblesModulo = 0;
          modulo.querySelectorAll("[data-permiso-item]").forEach(function (item) {
            const texto = (item.dataset.texto || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "");
            const coincide = !termino || texto.includes(termino);
            item.classList.toggle("d-none", !coincide);
            if (coincide) visiblesModulo++;
          });
          modulo.classList.toggle("d-none", visiblesModulo === 0);
          visibles += visiblesModulo;
        });
        if (sinResultados) sinResultados.classList.toggle("d-none", visibles > 0);
      });
    }

    actualizar();
  });
});
