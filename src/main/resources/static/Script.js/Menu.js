function toggleSidebar() {
  const sidebar     = document.getElementById("sidebar");
  const mainContent = document.getElementById("mainContent");
  if (!sidebar || !mainContent) return;

  sidebar.classList.toggle("collapsed");
  mainContent.classList.toggle("expanded");
  // Escritorio: la barra se ve por defecto (d-lg-block). Movil: oculta por defecto (d-none).
  if (window.matchMedia("(min-width: 992px)").matches) {
    sidebar.classList.toggle("d-lg-block");
  } else {
    sidebar.classList.toggle("d-none");
  }
}

// Panel de notificaciones: offcanvas de Bootstrap (se abre con data-bs-toggle desde la campana).
function panelNotificaciones() {
  const notifPanel = document.getElementById("notifPanel");
  if (!notifPanel || !window.bootstrap) return null;
  return bootstrap.Offcanvas.getOrCreateInstance(notifPanel);
}

function toggleNotif() {
  const panel = panelNotificaciones();
  if (panel) panel.toggle();
}

function cerrarNotificaciones() {
  const panel = panelNotificaciones();
  if (panel) panel.hide();
}

function abrirSolicitudReubicacion(id) {
  if (!id) return;
  const modal = document.getElementById("modalSolicitudReubicacion" + id);
  if (!modal) return;
  modal.classList.add("activo");
  modal.classList.remove("d-none");
  modal.setAttribute("aria-hidden", "false");
  cerrarNotificaciones();
}

function cerrarSolicitudReubicacion(modal) {
  if (!modal) return;
  modal.classList.remove("activo");
  modal.classList.add("d-none");
  modal.setAttribute("aria-hidden", "true");
}

function abrirDetalleReubicacionMenu(id) {
  if (!id) return;
  const modal = document.getElementById("detalleMenuReubicacion" + id);
  if (!modal) return;
  modal.classList.add("activo");
  modal.classList.remove("d-none");
  modal.setAttribute("aria-hidden", "false");
}

function cerrarDetalleReubicacionMenu(modal) {
  if (!modal) return;
  modal.classList.remove("activo");
  modal.classList.add("d-none");
  modal.setAttribute("aria-hidden", "true");
}

function clearNotifs() {
  const notifPanel = document.getElementById("notifPanel");

  marcarNotificacionesLeidas();

  cerrarNotificaciones();
}

function obtenerClaveNotificaciones() {
  const notifPanel = document.getElementById("notifPanel");
  const notifBadge = document.getElementById("notifBadge");

  return (notifPanel && notifPanel.dataset.readKey) ||
         (notifBadge && notifBadge.dataset.readKey) ||
         "";
}

function aplicarEstadoNotificaciones() {
  const notifBadge = document.getElementById("notifBadge");
  const clave = obtenerClaveNotificaciones();
  const leidas = obtenerNotificacionesLeidas(clave);
  let pendientes = 0;

  document.querySelectorAll(".notif-link").forEach(function (item) {
    const id = item.dataset.notifId || "";
    const estaLeida = Boolean(id && leidas[id]);

    item.classList.toggle("notif-read", estaLeida);
    item.classList.toggle("notif-unread", !estaLeida);
    item.classList.toggle("opacity-50", estaLeida);
    item.classList.toggle("bg-success-subtle", !estaLeida);

    if (!estaLeida) {
      pendientes++;
    }
  });

  if (notifBadge) {
    notifBadge.textContent = String(pendientes);
    notifBadge.classList.toggle("d-none", pendientes === 0);
  }

  organizarPanelNotificaciones();
}

function organizarPanelNotificaciones() {
  const notifPanel = document.getElementById("notifPanel");

  if (!notifPanel) return;

  const items = Array.from(notifPanel.querySelectorAll(".notif-link"));

  if (!items.length) return;

  notifPanel.querySelectorAll(".notif-section-title, .notif-group-title").forEach(function (titulo) {
    titulo.remove();
  });

  items.sort(function (a, b) {
    const prioridadA = Number(a.dataset.priority || "9");
    const prioridadB = Number(b.dataset.priority || "9");

    if (prioridadA !== prioridadB) {
      return prioridadA - prioridadB;
    }

    const tituloA = a.querySelector("strong");
    const tituloB = b.querySelector("strong");

    return (tituloA ? tituloA.textContent : "")
      .localeCompare(tituloB ? tituloB.textContent : "", "es");
  });

  const nuevas = items.filter(function (item) {
    return !item.classList.contains("notif-read");
  });
  const leidas = items.filter(function (item) {
    return item.classList.contains("notif-read");
  });

  const contenedor = notifPanel.querySelector(".offcanvas-body") || notifPanel;
  agregarSeccionNotificaciones(contenedor, "Nuevas", nuevas);
  agregarSeccionNotificaciones(contenedor, "Leídas", leidas);
}

function agregarSeccionNotificaciones(panel, titulo, items) {
  if (!items.length) return;

  const seccion = document.createElement("div");
  seccion.className = "notif-section-title small fw-bold text-uppercase text-success mt-2 mb-1";
  seccion.textContent = titulo;
  panel.appendChild(seccion);

  let grupoActual = "";

  items.forEach(function (item) {
    const grupo = item.dataset.group || "General";

    if (grupo !== grupoActual) {
      grupoActual = grupo;
      const grupoTitulo = document.createElement("div");
      grupoTitulo.className = "notif-group-title small fw-semibold text-secondary mt-1";
      grupoTitulo.textContent = grupo;
      panel.appendChild(grupoTitulo);
    }

    panel.appendChild(item);
  });
}

function marcarNotificacionesLeidas() {
  const clave = obtenerClaveNotificaciones();

  if (clave) {
    const leidas = obtenerNotificacionesLeidas(clave);

    document.querySelectorAll(".notif-link").forEach(function (item) {
      const id = item.dataset.notifId || "";
      if (id) {
        leidas[id] = true;
      }
    });

    guardarNotificacionesLeidas(clave, leidas);
  }

  aplicarEstadoNotificaciones();
}

function obtenerNotificacionesLeidas(clave) {
  if (!clave) return {};

  try {
    return JSON.parse(localStorage.getItem(clave) || "{}") || {};
  } catch (error) {
    return {};
  }
}

function guardarNotificacionesLeidas(clave, leidas) {
  if (!clave) return;
  localStorage.setItem(clave, JSON.stringify(leidas || {}));
}

function marcarNotificacionLeida(id) {
  const clave = obtenerClaveNotificaciones();
  if (!clave || !id) return;

  const leidas = obtenerNotificacionesLeidas(clave);
  leidas[id] = true;
  guardarNotificacionesLeidas(clave, leidas);
  aplicarEstadoNotificaciones();
}

function activarSolicitudesReubicacion() {
  document.querySelectorAll(".notif-link[data-reubicacion-id]").forEach(function (item) {
    const idReubicacion = item.dataset.reubicacionId || "";
    if (!idReubicacion) return;
    item.addEventListener("click", function (event) {
      event.preventDefault();
      marcarNotificacionLeida(item.dataset.notifId || "");
      abrirSolicitudReubicacion(idReubicacion);
    });
  });

  document.querySelectorAll(".modal-autorizacion-reubicacion").forEach(function (modal) {
    modal.addEventListener("click", function (event) {
      if (!modal.querySelector(".modal-content").contains(event.target) || event.target.closest(".cerrar-modal-autorizacion")) {
        cerrarSolicitudReubicacion(modal);
      }
    });
  });

  document.querySelectorAll(".btn-ver-detalles-reubicacion").forEach(function (boton) {
    boton.addEventListener("click", function () {
      abrirDetalleReubicacionMenu(boton.dataset.detalleReubicacionId || "");
    });
  });

  document.querySelectorAll(".modal-reubicacion-detalle-menu").forEach(function (modal) {
    modal.addEventListener("click", function (event) {
      if (!modal.querySelector(".modal-content").contains(event.target) || event.target.closest(".cerrar-modal-reubicacion-menu")) {
        cerrarDetalleReubicacionMenu(modal);
      }
    });
  });

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") {
      document.querySelectorAll(".modal-autorizacion-reubicacion.activo").forEach(cerrarSolicitudReubicacion);
      document.querySelectorAll(".modal-reubicacion-detalle-menu.activo").forEach(cerrarDetalleReubicacionMenu);
    }
  });

  const inicial = document.body.dataset.solicitudReubicacionInicial || "";
  if (inicial) {
    abrirSolicitudReubicacion(inicial);
  }
}

function filterModules() {
  const input = document.getElementById("searchInput").value.toLowerCase();
  const cards = document.querySelectorAll(".module-card");

  cards.forEach(function (card) {
    const name = card.dataset.name.toLowerCase();

    card.classList.toggle("d-none", !name.includes(input));
  });
}

function actualizarHora() {
  const horaActual = document.getElementById("horaActual");

  if (!horaActual) return;

  const fecha = new Date();

  horaActual.textContent = fecha.toLocaleTimeString("es-CO", {
    hour:   "2-digit",
    minute: "2-digit",
    second: "2-digit"
  });
}

function generarAvatar() {
  const nombreUsuario = document.getElementById("nombreUsuario");
  const avatar        = document.querySelector(".avatar-top");

  if (nombreUsuario && avatar) {
    avatar.title = nombreUsuario.textContent.trim();
  }
}

function cerrarPaneles() {
  // El offcanvas de notificaciones se cierra solo al tocar fuera (fondo de Bootstrap).
}



const btnPerfil    = document.getElementById("btnPerfil");
const perfilModal  = document.getElementById("perfilModal");
const cerrarPerfil = document.getElementById("cerrarPerfil");

if (btnPerfil) {
  btnPerfil.addEventListener("click", function () {
    perfilModal.classList.remove("d-none");
  });
}

if (cerrarPerfil) {
  cerrarPerfil.addEventListener("click", function () {
    perfilModal.classList.add("d-none");
  });
}

window.addEventListener("click", function (event) {
  if (perfilModal && !perfilModal.classList.contains("d-none") && !perfilModal.querySelector(".modal-content").contains(event.target) && !(btnPerfil && btnPerfil.contains(event.target))) {
    perfilModal.classList.add("d-none");
  }
});



const btnCerrarSesion = document.getElementById("btnCerrarSesion");
const modalCerrarSesion = document.getElementById("modalCerrarSesion");
const btnCancelarCerrarSesion = document.getElementById("btnCancelarCerrarSesion");
const btnConfirmarCerrarSesion = document.getElementById("btnConfirmarCerrarSesion");
const modalSalidaSesion = document.getElementById("modalSalidaSesion");

if (btnCerrarSesion) {
  btnCerrarSesion.addEventListener("click", function (event) {
    // En el panel principal se confirma con el modal; en el resto de vistas el enlace cierra sesion directo.
    event.preventDefault();
    modalCerrarSesion.classList.remove("d-none");
  });
}

if (btnCancelarCerrarSesion) {
  btnCancelarCerrarSesion.addEventListener("click", function () {
    modalCerrarSesion.classList.add("d-none");
  });
}

if (btnConfirmarCerrarSesion) {
  btnConfirmarCerrarSesion.addEventListener("click", function (event) {
    event.preventDefault();
    const destino = btnConfirmarCerrarSesion.href;

    if (modalCerrarSesion) {
      modalCerrarSesion.classList.add("d-none");
    }

    if (modalSalidaSesion) {
      modalSalidaSesion.classList.remove("d-none");
    }

    setTimeout(function () {
      window.location.href = destino;
    }, 1400);
  });
}

window.addEventListener("click", function (event) {
  if (modalCerrarSesion && !modalCerrarSesion.classList.contains("d-none") && !modalCerrarSesion.querySelector(".modal-content").contains(event.target) && !(btnCerrarSesion && btnCerrarSesion.contains(event.target))) {
    modalCerrarSesion.classList.add("d-none");
  }
});

document.addEventListener("click", cerrarPaneles);
document.querySelectorAll(".notif-link").forEach(function (item) {
  item.addEventListener("click", function () {
    marcarNotificacionLeida(item.dataset.notifId);
  });
});

setInterval(actualizarHora, 1000);

actualizarHora();
generarAvatar();
aplicarEstadoNotificaciones();
activarSolicitudesReubicacion();

document.querySelectorAll(".form-decision-reubicacion").forEach(function (formulario) {
  const lienzo = formulario.querySelector(".lienzo-firma-reubicacion");
  const destino = formulario.querySelector(".firma-digital-dato");
  const limpiar = formulario.querySelector(".limpiar-firma-reubicacion");
  const accionDecision = formulario.querySelector(".accion-decision-reubicacion");
  if (!lienzo || !destino || !limpiar) return;

  const contexto = lienzo.getContext("2d");
  contexto.strokeStyle = "#123f2a";
  contexto.lineWidth = 2.2;
  contexto.lineCap = "round";
  contexto.lineJoin = "round";
  let dibujando = false;
  let tieneTrazo = false;

  const punto = function (evento) {
    const caja = lienzo.getBoundingClientRect();
    return {
      x: (evento.clientX - caja.left) * (lienzo.width / caja.width),
      y: (evento.clientY - caja.top) * (lienzo.height / caja.height)
    };
  };
  const iniciar = function (evento) {
    dibujando = true;
    const p = punto(evento);
    contexto.beginPath();
    contexto.moveTo(p.x, p.y);
    lienzo.setPointerCapture(evento.pointerId);
    evento.preventDefault();
  };
  const trazar = function (evento) {
    if (!dibujando) return;
    const p = punto(evento);
    contexto.lineTo(p.x, p.y);
    contexto.stroke();
    tieneTrazo = true;
    evento.preventDefault();
  };
  const terminar = function () { dibujando = false; };
  lienzo.addEventListener("pointerdown", iniciar);
  lienzo.addEventListener("pointermove", trazar);
  lienzo.addEventListener("pointerup", terminar);
  lienzo.addEventListener("pointercancel", terminar);
  limpiar.addEventListener("click", function () {
    contexto.clearRect(0, 0, lienzo.width, lienzo.height);
    tieneTrazo = false;
    destino.value = "";
  });
  formulario.querySelectorAll("[data-accion-decision]").forEach(function (boton) {
    boton.addEventListener("click", function () {
      if (accionDecision) accionDecision.value = boton.dataset.accionDecision;
    });
  });
  formulario.addEventListener("submit", function (evento) {
    if (!tieneTrazo) {
      evento.preventDefault();
      alert("Debes realizar la firma manuscrita antes de registrar la decisión.");
      return;
    }
    if (!accionDecision || !accionDecision.value) {
      evento.preventDefault();
      alert("Selecciona si deseas aprobar o rechazar la solicitud.");
      return;
    }
    destino.value = lienzo.toDataURL("image/png");
  });
});




