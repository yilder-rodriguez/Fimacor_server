document.addEventListener("DOMContentLoaded", function () {
  const maquina = document.getElementById("maquina");
  const origen = document.getElementById("origen");
  const fecha = document.getElementById("fecha");
  const tipoTraslado = document.getElementById("tipoTraslado");
  const sedeDestino = document.getElementById("sedeDestino");
  const areaDestino = document.getElementById("areaDestino");
  const ambienteDestino = document.getElementById("ambienteDestino");
  const destino = document.getElementById("destino");
  const evidencia = document.getElementById("evidencia");
  const quitarEvidencia = document.getElementById("quitarEvidencia");
  const confirmacionEvidencia = document.getElementById("confirmacionEvidenciaExterna");
  const evidenciaConfirmada = document.getElementById("evidenciaConfirmada");
  const textoConfirmacionEvidencia = document.getElementById("textoConfirmacionEvidencia");
  const enviar = document.getElementById("enviarReubicacion");
  const formularioReubicacion = document.querySelector(".reubicacion-form");
  const ayudaDestino = document.getElementById("ayudaDestinoReubicacion");
  const ayudaEvidencia = document.getElementById("ayudaEvidenciaReubicacion");
  const resumenMaquina = document.getElementById("resumenMaquinaReubicacion");
  const destinatarios = document.getElementById("destinatariosReubicacion");

  const normalizar = function (valor) {
    return String(valor || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim().toLowerCase();
  };

  const sedeComplejoSur = function () {
    return Array.from((sedeDestino && sedeDestino.options) || []).find(function (opcion) {
      return normalizar(opcion.textContent) === "complejo sur";
    });
  };

  const actualizarSedePorTipo = function () {
    if (!tipoTraslado || !sedeDestino) return;
    const interno = tipoTraslado.value === "Interno";
    const complejoSur = sedeComplejoSur();
    Array.from(sedeDestino.options).forEach(function (opcion, indice) {
      if (indice === 0) { opcion.hidden = interno; opcion.disabled = interno; return; }
      const permitido = !interno || opcion === complejoSur;
      opcion.hidden = !permitido;
      opcion.disabled = !permitido;
    });
    if (interno && complejoSur) sedeDestino.value = complejoSur.value;
    actualizarAreas();
  };

  const escaparHtml = function (valor) {
    return String(valor || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/\"/g, "&quot;")
      .replace(/'/g, "&#039;");
  };

  const filtrarOpciones = function (select, coincide) {
    if (!select) return 0;
    let visibles = 0;
    Array.from(select.options).forEach(function (opcion, indice) {
      if (indice === 0) {
        opcion.hidden = false;
        opcion.disabled = false;
        return;
      }
      const visible = coincide(opcion);
      opcion.hidden = !visible;
      opcion.disabled = !visible;
      if (visible) visibles += 1;
    });
    return visibles;
  };

  const actualizarAreas = function () {
    if (!sedeDestino || !areaDestino || !ambienteDestino) return;
    areaDestino.value = "";
    ambienteDestino.value = "";
    if (destino) destino.value = "";
    const sede = sedeDestino.value;
    const cantidad = filtrarOpciones(areaDestino, function (opcion) {
      return sede !== "" && opcion.dataset.sede === sede;
    });
    filtrarOpciones(ambienteDestino, function () { return false; });
    actualizarMaquinas();
    if (ayudaDestino) {
      ayudaDestino.textContent = cantidad === 0
        ? "No hay áreas disponibles para esta sede."
        : "Selecciona el área y el ambiente destino. La máquina se filtra solo por sede.";
    }
  };

  const actualizarAmbientes = function () {
    if (!sedeDestino || !areaDestino || !ambienteDestino) return;
    const sede = sedeDestino.value;
    const area = areaDestino.value;
    ambienteDestino.value = "";
    if (destino) destino.value = "";
    const cantidad = filtrarOpciones(ambienteDestino, function (opcion) {
      return sede !== "" && opcion.dataset.sede === sede;
    });
    if (ayudaDestino) {
      ayudaDestino.textContent = !area
        ? "Selecciona un área destino."
        : cantidad === 0
          ? "No hay ambientes registrados para esta sede."
          : "Selecciona el ambiente a donde se envía.";
    }
  };

  const actualizarMaquinas = function () {
    if (!maquina || !sedeDestino) return;
    const sede = sedeDestino.value;
    maquina.value = "";
    const cantidad = filtrarOpciones(maquina, function (opcion) {
      return sede !== "" && opcion.dataset.sede === sede;
    });
    maquina.disabled = cantidad === 0;
    if (resumenMaquina && cantidad === 0 && sede) {
      resumenMaquina.innerHTML = "<strong>No hay máquinas en la sede seleccionada.</strong><span>Elige otra sede para continuar.</span>";
    }
  };

  const actualizarDestino = function () {
    if (!ambienteDestino || !destino) return;
    const selected = ambienteDestino.options[ambienteDestino.selectedIndex];
    destino.value = selected && ambienteDestino.value
      ? (selected.dataset.nombre || selected.textContent || "").trim()
      : "";
  };

  const actualizarFlujo = function () {
    if (!tipoTraslado) return;
    const externo = tipoTraslado.value === "Externo";
    if (evidencia) evidencia.required = true;
    const evidenciaAdjunta = evidencia && evidencia.files.length > 0;
    if (quitarEvidencia) quitarEvidencia.disabled = !evidenciaAdjunta;
    if (confirmacionEvidencia) {
      confirmacionEvidencia.hidden = false;
      confirmacionEvidencia.classList.toggle("evidencia-confirmada", evidenciaAdjunta);
      confirmacionEvidencia.classList.toggle("bg-success-subtle", evidenciaAdjunta);
      confirmacionEvidencia.classList.toggle("border-success", evidenciaAdjunta);
    }
    if (textoConfirmacionEvidencia) {
      textoConfirmacionEvidencia.textContent = evidenciaAdjunta
        ? "Evidencia adjunta y confirmada para enviar a Subdirección."
        : "Adjunta una evidencia obligatoria para continuar.";
    }
    if (evidenciaConfirmada) {
      evidenciaConfirmada.required = false;
      evidenciaConfirmada.checked = evidenciaAdjunta;
    }
    if (enviar) {
      enviar.textContent = "Enviar a Subdirección para firma";
      enviar.disabled = false;
    }
    if (destinatarios) {
      destinatarios.hidden = !maquina || !maquina.value;
      if (maquina && maquina.value) {
        const seleccionada = maquina.options[maquina.selectedIndex];
        const asignado = seleccionada ? seleccionada.dataset.asignado : "No tiene usuario asignado";
        destinatarios.innerHTML = "<strong class=\"d-block\">Personas que seran notificadas</strong><span class=\"d-block\">Usuario asignado: " + escaparHtml(asignado || "No tiene usuario asignado") + "</span><span class=\"d-block\">Subdirección responsable de aprobar y firmar la solicitud.</span>";
      }
    }
    if (ayudaEvidencia) {
      ayudaEvidencia.textContent = "La evidencia es obligatoria. El traslado se ejecuta solo cuando Subdirección lo aprueba y firma electrónicamente.";
    }
  };

  if (fecha && !fecha.value) fecha.value = new Date().toISOString().slice(0, 10);

  if (maquina && origen) {
    origen.readOnly = true;
    const actualizarMaquinaSeleccionada = function () {
      const selected = maquina.options[maquina.selectedIndex];
      const actual = selected ? selected.dataset.origen : "";
      const codigo = selected ? selected.dataset.codigo : "";
      const descripcion = selected ? selected.dataset.descripcion : "";
      const sede = selected ? selected.dataset.sedeNombre : "";
      const area = selected ? selected.dataset.area : "";
      origen.value = actual || "";
      if (resumenMaquina) {
        resumenMaquina.innerHTML = !maquina.value
          ? "<strong>Selecciona una maquina para ver sus datos.</strong><span>Codigo SENA, nombre y ubicacion actual apareceran aqui.</span>"
          : "<strong>Codigo SENA: " + escaparHtml(codigo || "Sin codigo") + "</strong>" +
            "<span>Maquina: " + escaparHtml(descripcion || "Sin descripcion") + "</span>" +
            "<span>Ubicacion actual: " + escaparHtml(actual || "Sin ambiente registrado") + "</span>" +
            "<span>Sede actual: " + escaparHtml(sede || "Sin sede") + " | Area actual: " + escaparHtml(area || "Sin area") + "</span>";
      }
    };
    maquina.addEventListener("change", actualizarMaquinaSeleccionada);
    actualizarMaquinaSeleccionada();
  }

  if (sedeDestino) sedeDestino.addEventListener("change", actualizarAreas);
  if (areaDestino) areaDestino.addEventListener("change", actualizarAmbientes);
  if (ambienteDestino) ambienteDestino.addEventListener("change", actualizarDestino);
  if (tipoTraslado) tipoTraslado.addEventListener("change", function () { actualizarSedePorTipo(); actualizarFlujo(); });
  if (evidencia) evidencia.addEventListener("change", actualizarFlujo);
  if (quitarEvidencia) quitarEvidencia.addEventListener("click", function () { if (evidencia) evidencia.value = ""; actualizarFlujo(); });
  if (evidenciaConfirmada) evidenciaConfirmada.addEventListener("click", function (event) { event.preventDefault(); });
  if (formularioReubicacion) {
    formularioReubicacion.addEventListener("submit", function (event) {
      if (!evidencia || evidencia.files.length === 0) {
        event.preventDefault();
        alert("Debes subir una evidencia antes de enviar la solicitud de reubicación.");
        evidencia.focus();
        return;
      }
      if (!evidenciaConfirmada || !evidenciaConfirmada.checked) {
        event.preventDefault();
        alert("Confirma la evidencia adjunta para continuar.");
      }
    });
  }
  actualizarSedePorTipo();
  actualizarFlujo();
  const abrirModal = function (modal) {
    if (!modal) return;
    modal.classList.add("activo");
    modal.classList.remove("d-none");
    modal.setAttribute("aria-hidden", "false");
  };

  const cerrarModal = function (modal) {
    if (!modal) return;
    modal.classList.remove("activo");
    modal.classList.add("d-none");
    modal.setAttribute("aria-hidden", "true");
  };

  document.querySelectorAll(".fila-detalle-reubicacion").forEach(function (fila) {
    fila.addEventListener("click", function (event) {
      if (event.target.closest("a, button, form, .no-abrir-modal")) return;
      abrirModal(document.getElementById(fila.getAttribute("data-modal")));
    });

    fila.addEventListener("keydown", function (event) {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        abrirModal(document.getElementById(fila.getAttribute("data-modal")));
      }
    });
  });

  document.querySelectorAll(".modal-reubicacion-detalle").forEach(function (modal) {
    modal.addEventListener("click", function (event) {
      if (!modal.querySelector(".modal-content").contains(event.target) || event.target.closest(".cerrar-modal-reubicacion")) {
        cerrarModal(modal);
      }
    });
  });
  document.querySelectorAll(".fila-evidencia-reubicacion").forEach(function (fila) {
    const verEvidencia = function () { abrirModal(document.getElementById("detalle-reubicacion-" + (fila.dataset.detalleReubicacionId || ""))); };
    fila.addEventListener("click", verEvidencia);
    fila.addEventListener("keydown", function (event) { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); verEvidencia(); } });
  });

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") {
      document.querySelectorAll(".modal-reubicacion-detalle.activo").forEach(cerrarModal);
    }
  });

  const filasReubicacion = document.querySelectorAll("[data-reubicacion-row]");
  const checksReubicacion = document.querySelectorAll(".reubicacion-row-check");
  const botonIniciarEliminar = document.querySelector("[data-reubicacion-start-delete]");
  const botonCancelarEliminar = document.querySelector("[data-reubicacion-cancel-delete]");
  const botonEliminarSeleccionadas = document.querySelector("[data-reubicacion-delete-selected]");
  const selectorTodas = document.querySelector("[data-reubicacion-select-all]");
  const contadorVisibles = document.querySelector("[data-reubicacion-visible]");
  const contadorSeleccionadas = document.querySelector("[data-reubicacion-selected]");
  let modoEliminarReubicacion = false;

  const filaVisible = function (fila) {
    return fila && !fila.classList.contains("d-none");
  };

  const actualizarSeleccionReubicacion = function () {
    let visibles = 0;
    let seleccionadas = 0;
    const checksVisibles = [];

    filasReubicacion.forEach(function (fila) {
      if (filaVisible(fila)) {
        visibles += 1;
      }
      const check = fila.querySelector(".reubicacion-row-check");
      if (check && filaVisible(fila) && !check.disabled) {
        checksVisibles.push(check);
      }
      if (check && check.checked) {
        seleccionadas += 1;
      }
    });

    if (contadorVisibles) {
      contadorVisibles.textContent = visibles;
    }
    if (contadorSeleccionadas) {
      contadorSeleccionadas.textContent = seleccionadas;
    }
    if (botonEliminarSeleccionadas) {
      botonEliminarSeleccionadas.disabled = seleccionadas === 0;
    }
    if (selectorTodas) {
      const todasMarcadas = checksVisibles.length > 0 && checksVisibles.every(function (check) {
        return check.checked;
      });
      selectorTodas.checked = todasMarcadas;
      selectorTodas.indeterminate = seleccionadas > 0 && !todasMarcadas;
    }
  };

  const limpiarSeleccionReubicacion = function () {
    checksReubicacion.forEach(function (check) {
      check.checked = false;
    });
    if (selectorTodas) {
      selectorTodas.checked = false;
      selectorTodas.indeterminate = false;
    }
  };

  const cambiarModoEliminarReubicacion = function (activo) {
    modoEliminarReubicacion = activo;
    document.body.classList.toggle("reubicacion-seleccion-activa", activo);
    // Reemplaza las reglas de body.reubicacion-seleccion-activa.
    document.querySelectorAll(".reubicacion-delete-mode-control, .reubicacion-col-check").forEach(function (elemento) {
      elemento.classList.toggle("d-none", !activo);
    });
    document.querySelectorAll("[data-reubicacion-start-delete]").forEach(function (boton) {
      boton.classList.toggle("d-none", activo);
    });
    if (!activo) {
      limpiarSeleccionReubicacion();
    }
    actualizarSeleccionReubicacion();
  };

  if (botonIniciarEliminar) {
    botonIniciarEliminar.addEventListener("click", function () {
      cambiarModoEliminarReubicacion(true);
    });
  }

  if (botonCancelarEliminar) {
    botonCancelarEliminar.addEventListener("click", function () {
      cambiarModoEliminarReubicacion(false);
    });
  }

  if (selectorTodas) {
    selectorTodas.addEventListener("change", function () {
      if (!modoEliminarReubicacion) {
        selectorTodas.checked = false;
        return;
      }
      filasReubicacion.forEach(function (fila) {
        const check = fila.querySelector(".reubicacion-row-check");
        if (check && filaVisible(fila) && !check.disabled) {
          check.checked = selectorTodas.checked;
        }
      });
      actualizarSeleccionReubicacion();
    });
  }

  checksReubicacion.forEach(function (check) {
    check.addEventListener("click", function (event) {
      event.stopPropagation();
    });
    check.addEventListener("change", function () {
      if (!modoEliminarReubicacion) {
        check.checked = false;
        return;
      }
      actualizarSeleccionReubicacion();
    });
  });

  document.querySelectorAll(".reubicacion-bulk-form").forEach(function (formulario) {
    formulario.addEventListener("submit", function (event) {
      if (!modoEliminarReubicacion) {
        event.preventDefault();
        cambiarModoEliminarReubicacion(true);
        return;
      }
      const seleccionadas = formulario.querySelectorAll(".reubicacion-row-check:checked").length;
      if (seleccionadas === 0) {
        event.preventDefault();
        alert("Selecciona al menos un movimiento para eliminar.");
      }
    });
  });

  cambiarModoEliminarReubicacion(false);

  const detalleInicial = new URLSearchParams(window.location.search).get("detalle");
  if (detalleInicial) {
    abrirModal(document.getElementById("detalle-reubicacion-" + detalleInicial));
  }
});
