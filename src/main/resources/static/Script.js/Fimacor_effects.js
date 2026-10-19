document.addEventListener("DOMContentLoaded", function () {
  const transition = crearTransicionGlobal();

  activarTransicionesDeModulo(transition);
  activarCargasDeFormulariosFimacor(transition);
  activarMejorasProfesionalesFimacor();
  activarQrActasReubicacionFimacor();
  activarVigilanciaSesionFimacor();

  // Cuando el navegador vuelve con Atrás, la página puede restaurarse desde
  // memoria con la capa de carga visible. Se oculta de inmediato.
  window.addEventListener("pageshow", function () { transition.hide(); });
  window.addEventListener("popstate", function () { transition.hide(); });
});

// Si esta cuenta inicia sesion en otro dispositivo, Spring Security invalida esta sesion
// en el servidor pero el navegador no se entera hasta su siguiente peticion -- se quedaba
// usando la pagina con normalidad hasta que el usuario navegaba a otro lado. Este ping
// periodico hace que se note casi al instante en vez de solo "a la proxima recargada".
function activarVigilanciaSesionFimacor() {
  // Paginas publicas (sin sesion): no hay sesion que vigilar y el ping, al no estar autenticado,
  // redirigia al login a quien estaba escribiendo en el registro o en "olvide mi contrasena".
  if (/^\/(login|registro|recuperar)(\/|$)/i.test(window.location.pathname)) return;
  setInterval(function () {
    fetch("/sesion/estado", { credentials: "same-origin" })
      .then(function (respuesta) {
        if (respuesta.redirected && respuesta.url.indexOf("/login") !== -1) {
          window.location.href = respuesta.url;
        }
      })
      .catch(function () { /* sin conexion momentanea: no forzar nada */ });
  }, 20000);
}

function activarCargasDeFormulariosFimacor(transition) {
  document.addEventListener("submit", function (evento) {
    const formulario = evento.target;
    if (!formulario || formulario.dataset.sinCargaFimacor === "true" || evento.defaultPrevented) return;
    const boton = formulario.querySelector("button[type='submit']:focus, input[type='submit']:focus") || formulario.querySelector("button[type='submit'], input[type='submit']");
    const texto = boton ? (boton.textContent || boton.value || "Procesando") : "Procesando operación";
    transition.show("Procesando solicitud", texto.trim() + "... Espera un momento.");
    // setTimeout a proposito: si el boton que disparo el submit (name="accion" value="aceptar",
    // como en la aprobacion de reubicacion) queda disabled en este mismo evento sincrono, el
    // navegador excluye su propio name/value del envio y el servidor recibe la peticion sin ese
    // parametro (400 Bad Request). Con el setTimeout, el navegador ya capturo los datos del
    // formulario para el envio en curso antes de que el boton se deshabilite.
    setTimeout(function () {
      formulario.querySelectorAll("button[type='submit'], input[type='submit']").forEach(function (control) { control.disabled = true; });
    }, 0);
  });
}

// Robot de carga FIMACOR: SVG con animaciones nativas (SMIL), sin hojas de estilo.
// Colores de marca: verde #2E7D32 / #4CAF50, azul noche #0D1B2A, verde claro #E8F5E9.
const ROBOT_CARGA_SVG = `
  <svg class="d-block mx-auto" width="168" height="148" viewBox="0 0 168 148" role="img" aria-label="Robot FIMACOR trabajando">
    <circle cx="84" cy="76" r="62" fill="#E8F5E9"/>
    <circle cx="84" cy="76" r="62" fill="none" stroke="#2E7D32" stroke-opacity="0.25" stroke-width="2">
      <animate attributeName="r" values="60;68;60" dur="2.6s" repeatCount="indefinite"/>
      <animate attributeName="stroke-opacity" values="0.35;0;0.35" dur="2.6s" repeatCount="indefinite"/>
    </circle>
    <g>
      <animateTransform attributeName="transform" type="translate" values="0 0;0 -2;0 0" dur="2.6s" repeatCount="indefinite"/>
      <line x1="84" y1="22" x2="84" y2="34" stroke="#0D1B2A" stroke-width="3" stroke-linecap="round"/>
      <circle cx="84" cy="19" r="5" fill="#4CAF50">
        <animate attributeName="opacity" values="1;0.35;1" dur="1.4s" repeatCount="indefinite"/>
      </circle>
      <rect x="49" y="47" width="7" height="16" rx="3.5" fill="#2E7D32"/>
      <rect x="112" y="47" width="7" height="16" rx="3.5" fill="#2E7D32"/>
      <rect x="55" y="34" width="58" height="42" rx="15" fill="#FFFFFF" stroke="#0D1B2A" stroke-width="3"/>
      <rect x="63" y="44" width="42" height="21" rx="9" fill="#0D1B2A"/>
      <ellipse cx="75" cy="54.5" rx="4.2" ry="4.2" fill="#5CFF7E">
        <animate attributeName="ry" values="4.2;4.2;0.6;4.2;4.2" keyTimes="0;0.44;0.5;0.56;1" dur="3.4s" repeatCount="indefinite"/>
      </ellipse>
      <ellipse cx="93" cy="54.5" rx="4.2" ry="4.2" fill="#5CFF7E">
        <animate attributeName="ry" values="4.2;4.2;0.6;4.2;4.2" keyTimes="0;0.44;0.5;0.56;1" dur="3.4s" repeatCount="indefinite"/>
      </ellipse>
      <rect x="76" y="76" width="16" height="7" rx="2" fill="#0D1B2A"/>
    </g>
    <rect x="52" y="82" width="64" height="26" rx="5" fill="#2E7D32"/>
    <circle cx="84" cy="95" r="4" fill="#E8F5E9" opacity="0.9"/>
    <rect x="40" y="107" width="88" height="9" rx="4" fill="#0D1B2A"/>
    <ellipse cx="66" cy="105" rx="8" ry="5" fill="#FFFFFF" stroke="#0D1B2A" stroke-width="2.5">
      <animateTransform attributeName="transform" type="translate" values="0 0;0 -4;0 0" dur="0.5s" repeatCount="indefinite"/>
    </ellipse>
    <ellipse cx="102" cy="105" rx="8" ry="5" fill="#FFFFFF" stroke="#0D1B2A" stroke-width="2.5">
      <animateTransform attributeName="transform" type="translate" values="0 -4;0 0;0 -4" dur="0.5s" repeatCount="indefinite"/>
    </ellipse>
    <circle cx="128" cy="30" r="3.5" fill="#2E7D32"><animate attributeName="opacity" values="0.2;1;0.2" dur="1.2s" repeatCount="indefinite"/></circle>
    <circle cx="137" cy="23" r="4" fill="#2E7D32"><animate attributeName="opacity" values="0.2;1;0.2" dur="1.2s" begin="0.2s" repeatCount="indefinite"/></circle>
    <circle cx="147" cy="15" r="4.5" fill="#2E7D32"><animate attributeName="opacity" values="0.2;1;0.2" dur="1.2s" begin="0.4s" repeatCount="indefinite"/></circle>
  </svg>`;

// Capa de carga global: modal de Bootstrap con el robot, visible sobre la cabecera y la barra lateral.
function crearTransicionGlobal() {
  let capa = document.querySelector(".fimacor-page-transition");

  if (!capa) {
    capa = document.createElement("div");
    capa.className = "fimacor-page-transition modal d-block d-none bg-dark bg-opacity-50";
    capa.setAttribute("role", "status");
    capa.setAttribute("aria-live", "polite");
    capa.innerHTML = `
      <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content border-0 rounded-4 shadow-lg text-center px-4 pt-4 pb-3">
          ${ROBOT_CARGA_SVG}
          <span class="small text-uppercase fw-semibold text-success mt-2">FIMACOR</span>
          <h3 class="h6 fw-bold mt-1 mb-1">Cargando modulo</h3>
          <p class="small text-secondary mb-3">Preparando la experiencia FIMACOR...</p>
          <div class="progress" role="progressbar" aria-label="Cargando">
            <div class="progress-bar progress-bar-striped progress-bar-animated bg-success w-100"></div>
          </div>
        </div>
      </div>
    `;
    document.body.appendChild(capa);
  }

  const api = {
    show: function (titulo, texto) {
      const h3 = capa.querySelector("h3");
      const p = capa.querySelector("p");
      if (h3 && titulo) h3.textContent = decodificarHtmlFimacor(titulo);
      if (p && texto) p.textContent = decodificarHtmlFimacor(texto);
      capa.classList.remove("d-none");
      document.body.classList.add("overflow-hidden");
    },
    hide: function () {
      capa.classList.add("d-none");
      document.body.classList.remove("overflow-hidden");
    }
  };

  window.FimacorCargando = api;
  window.mostrarCargaFimacor = function (titulo, texto) {
    api.show(titulo, texto);
  };

  return api;
}

function activarTransicionesDeModulo(transition) {
  document.addEventListener("click", function (evento) {
    const enlace = evento.target.closest("a[href]");
    if (!enlace || evento.defaultPrevented) return;

    const href = enlace.getAttribute("href") || "";
    const destinoExterno = enlace.target === "_blank" || href.startsWith("#") || href.startsWith("javascript:") || href.startsWith("mailto:") || href.startsWith("tel:");
    const descarga = enlace.hasAttribute("download") || enlace.classList.contains("modalmanual-link");

    if (destinoExterno || descarga || evento.ctrlKey || evento.metaKey || evento.shiftKey || evento.altKey) {
      return;
    }

    const url = new URL(enlace.href, window.location.href);
    if (url.origin !== window.location.origin) {
      return;
    }

    evento.preventDefault();

    const nombre = (enlace.querySelector("span") || enlace.querySelector("h3") || enlace).textContent.trim() || "modulo";
    const descripcion = obtenerMensajeModuloPorTexto(nombre + " " + url.pathname);
    const nombreVisible = /^modulo$/i.test(nombre.normalize("NFD").replace(/[\u0300-\u036f]/g, "")) ? descripcion.titulo : nombre;
    transition.show("Abriendo " + nombreVisible, descripcion.carga || "Cargando interfaz, permisos y datos del sistema...");

    setTimeout(function () {
      window.location.href = enlace.href;
    }, 400);
  });
}

function obtenerTextoNormalizado(valor) {
  return (valor || "")
    .toString()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function obtenerMensajeModuloPorTexto(nombre) {
  const clave = obtenerTextoNormalizado(nombre);
  const modulos = [
    { prueba: /menu|inicio|principal/, titulo: "Menu principal", carga: "Preparando el panel principal con accesos, indicadores y notificaciones." },
    { prueba: /admin.*maquina|administracion.*maquina/, titulo: "Administracion de maquinas", carga: "Cargando herramientas administrativas de maquinas." },
    { prueba: /(^|\s)(mis\s)?maquinas?$|maquinas? registradas?|inventario|asignar maquinas?/, titulo: "Maquinas", carga: "Cargando inventario, filtros y estado de maquinas." },
    { prueba: /admin.*mant|administracion.*mant/, titulo: "Administracion de mantenimiento", carga: "Preparando control administrativo de mantenimientos." },
    { prueba: /mant/, titulo: "Mantenimiento", carga: "Cargando programacion e historial de mantenimiento." },
    { prueba: /reubicacion|ubicacion|ambiente|sede|area/, titulo: "Reubicacion", carga: "Validando ubicaciones, ambientes y movimientos disponibles." },
    { prueba: /novedad|falla/, titulo: "Novedades", carga: "Preparando novedades, prioridades y evidencias." },
    { prueba: /usuario|rol|permiso/, titulo: "Usuarios y permisos", carga: "Cargando seguridad, roles y permisos del sistema." },
    { prueba: /manual|documentacion/, titulo: "Manuales", carga: "Cargando biblioteca tecnica y documentos disponibles." },
    { prueba: /configuracion|tipo|estado|marca|modelo|codigo/, titulo: "Configuracion", carga: "Cargando catalogos y opciones configurables." }
  ];

  for (let i = 0; i < modulos.length; i += 1) {
    if (modulos[i].prueba.test(clave)) return modulos[i];
  }

  return { titulo: "Modulo FIMACOR", carga: "Preparando datos y controles del modulo." };
}

function activarMejorasProfesionalesFimacor() {
  activarConfirmacionesProfesionales();
  activarValidacionViva();
  activarFormatoMonedaFimacor();
  activarVistaPreviaEvidenciaMantenimiento();
  activarCalendarioInformativoMantenimientos();
  activarHerramientasProfesionalesMaquinas();
  activarSemaforoEstadosMaquina();
}

function activarCalendarioInformativoMantenimientos() {
  const filas = Array.from(document.querySelectorAll(".mantenimiento-row-info"));
  if (!filas.length || document.querySelector(".mantenimiento-info-modal")) return;

  const modal = document.createElement("div");
  modal.className = "mantenimiento-info-modal modal d-block d-none bg-dark bg-opacity-75";
  modal.innerHTML = `
    <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-lg">
      <div class="mantenimiento-info-card modal-content rounded-4 border-0 shadow-lg" role="dialog" aria-modal="true" aria-label="Detalle del mantenimiento">
        <div class="mantenimiento-info-head modal-header bg-success text-white">
          <div class="d-flex flex-column">
            <strong class="fs-5"></strong>
            <span class="small opacity-75"></span>
          </div>
          <button type="button" class="mantenimiento-info-cerrar btn-close btn-close-white" aria-label="Cerrar detalle"></button>
        </div>
        <div class="modal-body">
          <div class="flujo-mantenimiento flujo-mantenimiento-info d-flex flex-wrap gap-2 mb-3" aria-label="Estado del mantenimiento">
            <span class="badge rounded-pill text-bg-light border" data-paso="1">1. Reportado</span>
            <span class="badge rounded-pill text-bg-light border" data-paso="2">2. Asignado</span>
            <span class="badge rounded-pill text-bg-light border" data-paso="3">3. En mantenimiento</span>
            <span class="badge rounded-pill text-bg-light border" data-paso="4">4. Finalizado</span>
            <span class="badge rounded-pill text-bg-light border" data-paso="5">5. Cerrado</span>
          </div>
          <div class="mantenimiento-info-grid mantenimiento-info-grid-horizontal row row-cols-2 row-cols-md-4 g-2 mb-3">
            <div class="col"><div class="h-100 border rounded-3 p-2 bg-body-tertiary"><b class="d-block small text-secondary">Tipo</b><span data-info="tipo"></span></div></div>
            <div class="col"><div class="h-100 border rounded-3 p-2 bg-body-tertiary"><b class="d-block small text-secondary">Fecha programada</b><span data-info="fecha"></span></div></div>
            <div class="col"><div class="h-100 border rounded-3 p-2 bg-body-tertiary"><b class="d-block small text-secondary">Hora</b><span data-info="hora"></span></div></div>
            <div class="col"><div class="h-100 border rounded-3 p-2 bg-body-tertiary"><b class="d-block small text-secondary">Pr&oacute;ximo mantenimiento</b><span data-info="proximo"></span></div></div>
            <div class="col"><div class="h-100 border rounded-3 p-2 bg-body-tertiary"><b class="d-block small text-secondary">D&iacute;as restantes</b><span data-info="dias"></span></div></div>
            <div class="col"><div class="h-100 border rounded-3 p-2 bg-body-tertiary"><b class="d-block small text-secondary">Estado</b><span data-info="estado"></span></div></div>
            <div class="col"><div class="h-100 border rounded-3 p-2 bg-body-tertiary"><b class="d-block small text-secondary">Evidencia</b><span data-info="evidencia"></span></div></div>
            <div class="col-12 col-md-6"><div class="h-100 border rounded-3 p-2 bg-body-tertiary"><b class="d-block small text-secondary">Programado por</b><span data-info="programador"></span></div></div>
            <div class="col-12 col-md-6"><div class="h-100 border rounded-3 p-2 bg-body-tertiary"><b class="d-block small text-secondary">Responsable asignado</b><span data-info="responsable-datos"></span></div></div>
          </div>
          <div class="mantenimiento-info-acciones d-flex flex-column mb-3">
            <span class="fw-bold text-success">Registro completo del mantenimiento</span>
            <small class="text-secondary">Programa, responsable, evidencia y calendario en una sola vista.</small>
          </div>
          <div class="mantenimiento-info-calendario mb-3"></div>
          <p class="mantenimiento-info-detalle mb-3"></p>
          <a class="mantenimiento-info-reparar btn btn-success" data-reparar hidden>Iniciar / reparar mantenimiento</a>
        </div>
      </div>
    </div>
  `;
  document.body.appendChild(modal);

  const card = modal.querySelector(".mantenimiento-info-card");
  const cerrar = modal.querySelector(".mantenimiento-info-cerrar");
  const titulo = modal.querySelector(".mantenimiento-info-head strong");
  const subtitulo = modal.querySelector(".mantenimiento-info-head span");
  const detalle = modal.querySelector(".mantenimiento-info-detalle");
  const calendario = modal.querySelector(".mantenimiento-info-calendario");
  const reparar = modal.querySelector("[data-reparar]");

  function marcarFlujo(estado) {
    const texto = (estado || "").toLowerCase();
    let paso = 1;
    if (texto.includes("asign")) paso = 2;
    if (texto.includes("mantenimiento") || texto.includes("proceso")) paso = 3;
    if (texto.includes("finaliz") || texto.includes("reparad")) paso = 4;
    if (texto.includes("cerrad")) paso = 5;
    modal.querySelectorAll("[data-paso]").forEach(function (item) {
      const numero = Number(item.dataset.paso);
      item.classList.toggle("activo", numero === paso);
      item.classList.toggle("completado", numero < paso);
      item.classList.toggle("text-bg-success", numero === paso);
      item.classList.toggle("text-bg-success-subtle", false);
      item.classList.toggle("bg-success-subtle", numero < paso);
      item.classList.toggle("text-success-emphasis", numero < paso);
      item.classList.toggle("text-bg-light", numero > paso);
    });
  }

  function abrir(fila) {
    titulo.textContent = fila.dataset.maquina || "Mantenimiento";
    subtitulo.textContent = (fila.dataset.tipo || "Tipo") + " | " + (fila.dataset.estado || "Estado");
    marcarFlujo(fila.dataset.estado || "");
    modal.querySelector("[data-info='tipo']").textContent = fila.dataset.tipo || "Sin tipo";
    modal.querySelector("[data-info='fecha']").textContent = fila.dataset.fecha || "Sin fecha";
    modal.querySelector("[data-info='hora']").textContent = fila.dataset.hora || "Sin hora";
    modal.querySelector("[data-info='proximo']").textContent = fila.dataset.proximo || "Sin fecha";
    modal.querySelector("[data-info='dias']").textContent = fila.dataset.dias || "Sin dato";
    modal.querySelector("[data-info='estado']").textContent = fila.dataset.estado || "Sin estado";
    modal.querySelector("[data-info='evidencia']").textContent = fila.dataset.evidencia || "Sin dato";
    modal.querySelector("[data-info='programador']").textContent = fila.dataset.programador || "No registrado";
    modal.querySelector("[data-info='responsable-datos']").textContent = fila.dataset.responsableDatos || fila.dataset.responsable || "Sin responsable";
    detalle.textContent = fila.dataset.detalle || "Sin detalle registrado.";
    calendario.innerHTML = crearCalendarioMantenimiento(fila.dataset.proximo || fila.dataset.fecha);
    const historial = Number(fila.dataset.historial || 0);
    if (historial > 0 && document.body.classList.contains("mantenimientospreventivos-form")) {
      reparar.href = (document.body.dataset.context || "") + "/mantenimiento/historial?historial=" + historial + "&accion=confirmar";
      reparar.hidden = false;
    } else reparar.hidden = true;
    modal.classList.remove("d-none");
  }

  filas.forEach(function (fila) {
    fila.setAttribute("role", "button");
    fila.addEventListener("click", function (evento) {
      if (evento.target.closest("a")) return;
      abrir(fila);
    });
  });

  cerrar.addEventListener("click", function () {
    modal.classList.add("d-none");
  });
  modal.addEventListener("click", function (evento) {
    if (!card.contains(evento.target)) modal.classList.add("d-none");
  });
  document.addEventListener("keydown", function (evento) {
    if (evento.key === "Escape") modal.classList.add("d-none");
  });
}

function crearCalendarioMantenimiento(fechaTexto) {
  const fecha = new Date((fechaTexto || "").toString() + "T00:00:00");
  if (Number.isNaN(fecha.getTime())) {
    return "<div class='mantenimiento-calendario-vacio alert alert-light border small mb-0'>Sin fecha para calendario</div>";
  }
  const meses = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];
  const primerDia = new Date(fecha.getFullYear(), fecha.getMonth(), 1);
  const ultimoDia = new Date(fecha.getFullYear(), fecha.getMonth() + 1, 0);
  const inicio = primerDia.getDay();
  let html = "<strong class='d-block text-capitalize mb-2'>" + meses[fecha.getMonth()] + " de " + fecha.getFullYear() + "</strong>";
  html += "<table class='mantenimiento-calendario-grid table table-sm table-borderless text-center small mb-0'><thead><tr>";
  ["DO", "LU", "MA", "MI", "JU", "VI", "SA"].forEach(function (dia) {
    html += "<th class='text-secondary'>" + dia + "</th>";
  });
  html += "</tr></thead><tbody><tr>";
  for (let i = 0; i < inicio; i += 1) html += "<td></td>";
  for (let dia = 1; dia <= ultimoDia.getDate(); dia += 1) {
    if ((inicio + dia - 1) % 7 === 0 && dia > 1) html += "</tr><tr>";
    const activo = dia === fecha.getDate();
    html += "<td><span class='d-inline-block rounded-circle px-2 py-1" + (activo ? " activo text-bg-success fw-bold" : "") + "'>" + dia + "</span></td>";
  }
  html += "</tr></tbody></table>";
  return html;
}

// El atajo administrativo debe estar disponible tanto en Login como en Registro.
// Se instala en el archivo comun para que no dependa de que una pagina haya sido
// visitada previamente ni de la cache del navegador.
(function activarAtajoRegistroAdministrativo() {
  if (window.FIMACOR_ATAJO_REGISTRO_ADMIN_ACTIVO) return;
  window.FIMACOR_ATAJO_REGISTRO_ADMIN_ACTIVO = true;

  document.addEventListener("keydown", function (event) {
    const tecla = (event.key || "").toLowerCase();
    const esAtajo = event.ctrlKey && event.altKey
      && (tecla === "f" || tecla === "r" || tecla === "m");
    if (!esAtajo || event.repeat) return;

    const esLoginORegistro = document.getElementById("formLogin")
      || document.getElementById("formRegistro");
    if (!esLoginORegistro) return;

    event.preventDefault();
    event.stopPropagation();

    if (typeof window.abrirSelectorRolAdminFimacor === "function") {
      window.abrirSelectorRolAdminFimacor();
      return;
    }

    const contexto = document.body && document.body.dataset
      ? (document.body.dataset.context || "") : "";
    window.location.assign(contexto + "/registro");
  }, true);
})();

function activarConfirmacionesProfesionales() {
  const formularios = Array.from(document.querySelectorAll("form[onsubmit]"));
  formularios.forEach(function (formulario) {
    const onsubmit = formulario.getAttribute("onsubmit") || "";
    if (!/confirm\s*\(/i.test(onsubmit) || formulario.dataset.confirmacionProfesional === "true") return;

    formulario.dataset.confirmacionProfesional = "true";
    formulario.addEventListener("submit", function (evento) {
      if (formulario.dataset.confirmadoFimacor === "true") return;

      evento.preventDefault();
      evento.stopImmediatePropagation();

      mostrarConfirmacionFimacor(
        extraerMensajeConfirmacion(onsubmit),
        function () {
          formulario.dataset.confirmadoFimacor = "true";
          formulario.removeAttribute("onsubmit");
          formulario.submit();
        }
      );
    }, true);
  });
}

function extraerMensajeConfirmacion(texto) {
  const coincidencia = texto.match(/confirm\s*\(\s*(['"])(.*?)\1\s*\)/i);
  const mensaje = coincidencia ? coincidencia[2] : "Confirma la operacion antes de continuar.";
  const caja = document.createElement("textarea");
  caja.innerHTML = mensaje;
  return caja.value
    .replace(/\\'/g, "'")
    .replace(/\\"/g, "\"")
    .trim();
}

function mostrarConfirmacionFimacor(mensaje, alConfirmar) {
  let modal = document.querySelector(".fimacor-confirmacion-pro");
  if (!modal) {
    modal = document.createElement("div");
    modal.className = "fimacor-confirmacion-pro modal d-block d-none bg-dark bg-opacity-75";
    modal.innerHTML = `
      <div class="modal-dialog modal-dialog-centered">
        <section class="fimacor-confirmacion-pro-card modal-content rounded-4 border-0 shadow-lg text-center p-4" role="dialog" aria-modal="true" aria-labelledby="fimacorConfirmacionTitulo">
          <div class="fimacor-confirmacion-pro-icono d-flex align-items-center justify-content-center rounded-circle text-bg-warning fw-bold fs-3 mx-auto mb-3 px-3">!</div>
          <h3 id="fimacorConfirmacionTitulo" class="h5 fw-bold">Confirmar acci&oacute;n</h3>
          <p class="text-secondary"></p>
          <div class="fimacor-confirmacion-pro-acciones d-flex justify-content-center gap-2">
            <button type="button" class="fimacor-confirmacion-cancelar btn btn-outline-secondary">Cancelar</button>
            <button type="button" class="fimacor-confirmacion-aceptar btn btn-success">Confirmar</button>
          </div>
        </section>
      </div>
    `;
    document.body.appendChild(modal);
  }

  const texto = modal.querySelector("p");
  const cancelar = modal.querySelector(".fimacor-confirmacion-cancelar");
  const aceptar = modal.querySelector(".fimacor-confirmacion-aceptar");
  texto.textContent = decodificarHtmlFimacor(mensaje || "Confirma la operacion antes de continuar.");
  modal.classList.remove("d-none");
  document.body.classList.add("modalmaquina-abierto", "overflow-hidden");

  const cerrar = function () {
    modal.classList.add("d-none");
    document.body.classList.remove("modalmaquina-abierto", "overflow-hidden");
    cancelar.onclick = null;
    aceptar.onclick = null;
  };

  cancelar.onclick = cerrar;
  aceptar.onclick = function () {
    cerrar();
    if (typeof alConfirmar === "function") alConfirmar();
  };
}

function activarValidacionViva() {
  document.querySelectorAll("form").forEach(function (formulario) {
    if (formulario.dataset.validacionViva === "true") return;
    formulario.dataset.validacionViva = "true";

    formulario.querySelectorAll("input, select, textarea").forEach(function (campo) {
      campo.addEventListener("input", function () {
        evaluarCampoFimacor(campo, true);
      });
      campo.addEventListener("change", function () {
        evaluarCampoFimacor(campo, true);
      });
      campo.addEventListener("blur", function () {
        evaluarCampoFimacor(campo, true);
      });
    });

    formulario.addEventListener("submit", function () {
      formulario.querySelectorAll("input, select, textarea").forEach(function (campo) {
        evaluarCampoFimacor(campo, true);
      });
    });
  });
}

function evaluarCampoFimacor(campo, mostrarEstado) {
  if (!campo || campo.type === "hidden" || campo.disabled) return;
  if (campo.type === "checkbox" || campo.type === "radio" || campo.type === "file" || campo.type === "search") return;

  const valor = (campo.value || "").trim();
  let valido = true;

  if (campo.required && !valor) valido = false;
  if (valor && campo.type === "email") valido = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valor);
  if (valor && (campo.name || campo.id || "").toLowerCase().includes("telefono")) valido = /^[0-9+\s-]{7,20}$/.test(valor);

  if (mostrarEstado) {
    campo.classList.toggle("is-valid", valido && valor.length > 0);
    campo.classList.toggle("is-invalid", !valido);
  }
}

function activarFormatoMonedaFimacor() {
  const selectores = [
    "input[name*='valor' i]",
    "input[id*='valor' i]",
    "input[name*='precio' i]",
    "input[id*='precio' i]"
  ].join(",");

  document.querySelectorAll(selectores).forEach(function (campo) {
    if (campo.dataset.monedaFimacor === "true") return;
    campo.dataset.monedaFimacor = "true";
    campo.setAttribute("inputmode", "numeric");
    campo.addEventListener("input", function () {
      const cursorFinal = campo.selectionStart === campo.value.length;
      const digitos = campo.value.replace(/\D/g, "");
      campo.value = digitos ? digitos.replace(/\B(?=(\d{3})+(?!\d))/g, ".") : "";
      if (cursorFinal) {
        campo.setSelectionRange(campo.value.length, campo.value.length);
      }
    });
  });
}

function decodificarHtmlFimacor(valor) {
  if (valor == null) return "";
  const area = document.createElement("textarea");
  area.innerHTML = String(valor);
  return area.value;
}

function activarVistaPreviaEvidenciaMantenimiento() {
  const input = document.querySelector("#evidenciaMantenimiento");
  if (!input || input.dataset.previewFimacor === "true") return;
  input.dataset.previewFimacor = "true";

  const preview = document.createElement("div");
  preview.className = "mantenimiento-evidencia-preview mt-2";
  preview.innerHTML = `
    <div class="mantenimiento-evidencia-preview-marco d-none border rounded-3 p-2 mb-2">
      <span class="small fw-bold text-success d-block mb-2">Vista previa</span>
      <div class="mantenimiento-evidencia-miniaturas d-flex flex-wrap gap-2"></div>
    </div>
    <p class="small text-secondary mb-0">Selecciona una o varias imagenes para confirmar que la evidencia se ve clara antes de programar el mantenimiento.</p>
  `;
  input.insertAdjacentElement("afterend", preview);

  const marco = preview.querySelector(".mantenimiento-evidencia-preview-marco");
  const miniaturas = preview.querySelector(".mantenimiento-evidencia-miniaturas");
  const texto = preview.querySelector("p");
  input.addEventListener("change", function () {
    const archivos = Array.from(input.files || []);
    miniaturas.innerHTML = "";
    if (!archivos.length) {
      marco.classList.add("d-none");
      texto.textContent = "Selecciona una o varias imagenes para confirmar que la evidencia se ve clara antes de programar el mantenimiento.";
      return;
    }

    const invalidos = archivos.filter(function (archivo) {
      return !/^image\/(png|jpe?g|webp)$/i.test(archivo.type);
    });
    if (invalidos.length) {
      marco.classList.add("d-none");
      miniaturas.innerHTML = "";
      texto.textContent = "Todos los archivos deben ser imagenes PNG, JPG, JPEG o WEBP.";
      input.value = "";
      return;
    }

    archivos.slice(0, 6).forEach(function (archivo) {
      const lector = new FileReader();
      lector.onload = function (evento) {
        const img = document.createElement("img");
        img.src = evento.target.result;
        img.alt = "Vista previa de evidencia";
        img.width = 96;
        img.height = 96;
        img.className = "img-thumbnail object-fit-cover";
        miniaturas.appendChild(img);
      };
      lector.readAsDataURL(archivo);
    });
    marco.classList.remove("d-none");
    texto.textContent = archivos.length + " foto(s) seleccionada(s). Revisa que la evidencia sea legible.";
  });
}

function activarHerramientasProfesionalesMaquinas() {
  const ruta = window.location.pathname.toLowerCase();
  const enListaMaquinas = ruta === "/maquinas" || ruta.includes("/maquinas/administracion");
  if (!enListaMaquinas && !ruta.includes("mantenimiento")) return;
  activarExportacionTablasFimacor();
  if (enListaMaquinas) {
    activarQrFichasTecnicasFimacor();
    activarAlertasGarantiaFimacor();
    activarLineaTiempoMaquinasFimacor();
  }
}

function activarExportacionTablasFimacor() {
  if (document.body && document.body.dataset.fimacorSinExport === "true") return;
  const tabla = document.querySelector(".tablemaquinasregistradas, table");
  let barra = document.querySelector(".accionesmaquinasregistradas, .accioneshistorialmantenimiento, .acciones-admin-maquinas, .accionesmaquinas, .admin-actions");
  if (!tabla) return;
  if (!barra) {
    barra = document.createElement("div");
    barra.className = "fimacor-export-barra-generada d-flex justify-content-end mb-2 d-print-none";
    tabla.parentElement.insertAdjacentElement("beforebegin", barra);
  }
  if (barra.dataset.exportFimacor === "true") return;
  barra.dataset.exportFimacor = "true";
  const ruta = window.location.pathname.toLowerCase();
  const esMantenimiento = /mantenimiento/.test(ruta);
  const encabezadoPagina = document.querySelector("main h2, main h1");
  const titulo = encabezadoPagina ? encabezadoPagina.textContent.replace(/\s+/g, " ").trim()
    : (esMantenimiento ? "Listado de mantenimientos" : "Listado de máquinas");
  const base = (esMantenimiento ? "mantenimientos" : "maquinas") + "_fimacor_" + new Date().toISOString().slice(0, 10);

  const grupo = document.createElement("div");
  grupo.className = "fimacor-export-actions d-flex flex-wrap gap-2 d-print-none";
  grupo.innerHTML = `
    <button type="button" class="btn btn-light border btn-sm fw-semibold" data-fimacor-export="excel"><i class="bi bi-file-earmark-excel text-success me-1" aria-hidden="true"></i>Exportar Excel</button>
    <button type="button" class="btn btn-light border btn-sm fw-semibold" data-fimacor-export="pdf"><i class="bi bi-file-earmark-pdf text-danger me-1" aria-hidden="true"></i>Exportar PDF</button>
  `;
  barra.appendChild(grupo);

  const conCarga = function (boton, tarea) {
    if (boton.disabled) return;
    const original = boton.innerHTML;
    boton.disabled = true;
    boton.innerHTML = '<span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Generando...';
    Promise.resolve().then(tarea).finally(function () {
      boton.disabled = false;
      boton.innerHTML = original;
    });
  };
  grupo.querySelector("[data-fimacor-export='excel']").addEventListener("click", function () {
    conCarga(this, function () {
      return exportarTablaExcelFimacor(tabla, titulo, base + ".xlsx").catch(function () {
        exportarTablaCsvFimacor(tabla, base + ".csv");
      });
    });
  });
  grupo.querySelector("[data-fimacor-export='pdf']").addEventListener("click", function () {
    conCarga(this, function () {
      return exportarTablaPdfFimacor(tabla, titulo, base + ".pdf").catch(function () {
        exportarTablaImpresionFimacor(tabla, titulo);
      });
    });
  });
}

// Librerias de exportacion: se cargan desde cdnjs solo al primer clic (con verificacion de integridad).
const LIBRERIAS_EXPORT_FIMACOR = {
  exceljs: ["https://cdnjs.cloudflare.com/ajax/libs/exceljs/4.4.0/exceljs.min.js",
    "sha384-Pqp51FUN2/qzfxZxBCtF0stpc9ONI6MYZpVqmo8m20SoaQCzf+arZvACkLkirlPz"],
  jspdf: ["https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js",
    "sha384-JcnsjUPPylna1s1fvi1u12X5qjY5OL56iySh75FdtrwhO/SWXgMjoVqcKyIIWOLk"],
  autotable: ["https://cdnjs.cloudflare.com/ajax/libs/jspdf-autotable/3.8.2/jspdf.plugin.autotable.min.js",
    "sha384-fCAW/rDWORTbQXSiB7mOg0QtQ5c+r0f544y6XoKjuVva0nMBlCpNUjiFeG5iMdS3"]
};
const cargasExportFimacor = {};

function cargarLibreriaFimacor(clave) {
  if (!cargasExportFimacor[clave]) {
    cargasExportFimacor[clave] = new Promise(function (resolver, rechazar) {
      const script = document.createElement("script");
      script.src = LIBRERIAS_EXPORT_FIMACOR[clave][0];
      script.integrity = LIBRERIAS_EXPORT_FIMACOR[clave][1];
      script.crossOrigin = "anonymous";
      script.referrerPolicy = "no-referrer";
      script.onload = resolver;
      script.onerror = function () { delete cargasExportFimacor[clave]; rechazar(new Error("No se pudo cargar " + clave)); };
      document.head.appendChild(script);
    });
  }
  return cargasExportFimacor[clave];
}

// Lee la tabla (todas las filas, tambien las de otras paginas) sin columnas de seleccion ni acciones.
function datosTablaFimacor(tabla) {
  const limpiar = function (texto) { return (texto || "").replace(/\s+/g, " ").trim(); };
  const filas = Array.from(tabla.querySelectorAll("tr"));
  const encabezado = filas.length ? Array.from(filas[0].querySelectorAll("th, td")) : [];
  const omitir = new Set();
  encabezado.forEach(function (celda, i) {
    const texto = limpiar(celda.textContent).toLowerCase();
    if (celda.classList.contains("admin-col-check") || texto === "" || /^(acciones|opciones|seleccionar)$/.test(texto)) omitir.add(i);
  });
  const tomar = function (celdas) {
    return celdas.filter(function (c, i) { return !omitir.has(i); }).map(function (c) { return limpiar(c.textContent); });
  };
  const encabezados = tomar(encabezado);
  const cuerpo = filas.slice(1)
    .map(function (fila) { return tomar(Array.from(fila.querySelectorAll("th, td"))); })
    .filter(function (celdas) { return celdas.length === encabezados.length && celdas.some(function (t) { return t !== ""; }); });
  const columnasEstado = encabezados
    .map(function (t, i) { return /estado/i.test(t) ? i : -1; })
    .filter(function (i) { return i >= 0; });
  const columnaNumero = encabezados.findIndex(function (t) { return /^(n\.?\s*[°º]?|#)$/i.test(t); });
  return { encabezados: encabezados, filas: cuerpo, columnasEstado: columnasEstado, columnaNumero: columnaNumero };
}

// Colores de estado (fondo suave + texto oscuro), iguales a los de la interfaz.
function colorEstadoFimacor(texto) {
  const t = (texto || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
  if (/fuera|dan|aver|falla|vencid|rechaz|cancel/.test(t)) return { fondo: "F8D7DA", texto: "58151C" };
  if (/mantenimiento|revision|pendiente|proxim|reparacion|asignad/.test(t)) return { fondo: "FFF3CD", texto: "664D03" };
  if (/operativ|reparad|finaliz|cerrad|aprob|complet|activ/.test(t)) return { fondo: "E8F5E9", texto: "1B5E20" };
  return { fondo: "E9ECEF", texto: "41464B" };
}

function hexARgbFimacor(hex) {
  return [parseInt(hex.slice(0, 2), 16), parseInt(hex.slice(2, 4), 16), parseInt(hex.slice(4, 6), 16)];
}

function descargarBlobFimacor(blob, nombre) {
  const enlace = document.createElement("a");
  enlace.href = URL.createObjectURL(blob);
  enlace.download = nombre;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  setTimeout(function () { URL.revokeObjectURL(enlace.href); }, 1000);
}

async function exportarTablaExcelFimacor(tabla, titulo, nombreArchivo) {
  await cargarLibreriaFimacor("exceljs");
  const datos = datosTablaFimacor(tabla);
  const columnas = Math.max(datos.encabezados.length, 1);
  const ahora = new Date().toLocaleString("es-CO");
  const libro = new window.ExcelJS.Workbook();
  libro.creator = "FIMACOR";
  libro.created = new Date();
  const hoja = libro.addWorksheet(titulo.slice(0, 31).replace(/[\\/?*[\]:]/g, " "), {
    views: [{ state: "frozen", ySplit: 5, showGridLines: false }],
    pageSetup: { orientation: "landscape", fitToPage: true, fitToWidth: 1, fitToHeight: 0, paperSize: 9 },
    headerFooter: { oddFooter: "&LFIMACOR · " + titulo + "&RPágina &P de &N" }
  });
  const oscuro = { type: "pattern", pattern: "solid", fgColor: { argb: "FF0D1B2A" } };
  const borde = { style: "thin", color: { argb: "FFD9DEE3" } };
  const bordes = { top: borde, left: borde, bottom: borde, right: borde };

  const banda = function (fila, texto, fuente, alto) {
    hoja.mergeCells(fila, 1, fila, columnas);
    const celda = hoja.getCell(fila, 1);
    celda.value = texto;
    celda.font = fuente;
    celda.fill = oscuro;
    celda.alignment = { vertical: "middle", indent: 1 };
    hoja.getRow(fila).height = alto;
  };
  banda(1, "FIMACOR", { name: "Calibri", size: 18, bold: true, color: { argb: "FFFFFFFF" } }, 32);
  banda(2, titulo + "  ·  Sistema de Control de Máquinas Textiles · CMTC – SENA", { name: "Calibri", size: 11, color: { argb: "FFA5D6A7" } }, 20);
  banda(3, "Generado el " + ahora + "  ·  " + datos.filas.length + " registros", { name: "Calibri", size: 10, italic: true, color: { argb: "FFCED4DA" } }, 20);
  hoja.getRow(4).height = 8;

  const cabecera = hoja.getRow(5);
  datos.encabezados.forEach(function (texto, i) {
    const celda = cabecera.getCell(i + 1);
    celda.value = texto;
    celda.font = { name: "Calibri", size: 11, bold: true, color: { argb: "FFFFFFFF" } };
    celda.fill = { type: "pattern", pattern: "solid", fgColor: { argb: "FF2E7D32" } };
    celda.alignment = { vertical: "middle", horizontal: "center", wrapText: true };
    celda.border = bordes;
  });
  cabecera.height = 24;

  datos.filas.forEach(function (valores, indice) {
    const fila = hoja.getRow(6 + indice);
    valores.forEach(function (texto, i) {
      const celda = fila.getCell(i + 1);
      celda.value = i === datos.columnaNumero && /^\d{1,6}$/.test(texto) ? Number(texto) : texto;
      celda.font = { name: "Calibri", size: 11, color: { argb: "FF212529" } };
      celda.border = bordes;
      celda.alignment = { vertical: "middle", horizontal: i === datos.columnaNumero ? "center" : "left", wrapText: true };
      if (indice % 2 === 1) celda.fill = { type: "pattern", pattern: "solid", fgColor: { argb: "FFF6F8F6" } };
      if (datos.columnasEstado.indexOf(i) >= 0 && texto) {
        const color = colorEstadoFimacor(texto);
        celda.fill = { type: "pattern", pattern: "solid", fgColor: { argb: "FF" + color.fondo } };
        celda.font = { name: "Calibri", size: 11, bold: true, color: { argb: "FF" + color.texto } };
        celda.alignment = { vertical: "middle", horizontal: "center" };
      }
    });
    fila.height = 20;
  });

  const ultima = 5 + datos.filas.length;
  hoja.autoFilter = { from: { row: 5, column: 1 }, to: { row: Math.max(ultima, 5), column: columnas } };
  datos.encabezados.forEach(function (texto, i) {
    const largo = datos.filas.reduce(function (max, fila) { return Math.max(max, (fila[i] || "").length); }, texto.length);
    hoja.getColumn(i + 1).width = Math.min(45, Math.max(10, largo + 4));
  });

  const resumen = hoja.getRow(ultima + 2);
  resumen.getCell(1).value = "Total de registros: " + datos.filas.length;
  resumen.getCell(1).font = { name: "Calibri", size: 11, bold: true, color: { argb: "FF1B5E20" } };

  const buffer = await libro.xlsx.writeBuffer();
  descargarBlobFimacor(new Blob([buffer], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }), nombreArchivo);
}

// Logo reducido a 96 px para que el PDF no pese de mas.
function imagenDataUrlFimacor(ruta) {
  return new Promise(function (resolver) {
    const imagen = new Image();
    imagen.onload = function () {
      const lienzo = document.createElement("canvas");
      lienzo.width = 96;
      lienzo.height = 96;
      const escala = Math.min(96 / imagen.naturalWidth, 96 / imagen.naturalHeight);
      const w = imagen.naturalWidth * escala, h = imagen.naturalHeight * escala;
      lienzo.getContext("2d").drawImage(imagen, (96 - w) / 2, (96 - h) / 2, w, h);
      resolver(lienzo.toDataURL("image/png"));
    };
    imagen.onerror = function () { resolver(null); };
    imagen.src = ruta;
  });
}

async function exportarTablaPdfFimacor(tabla, titulo, nombreArchivo) {
  await cargarLibreriaFimacor("jspdf");
  await cargarLibreriaFimacor("autotable");
  const datos = datosTablaFimacor(tabla);
  const contexto = (document.body && document.body.dataset.context) || "";
  const logo = await imagenDataUrlFimacor(contexto + "/Img/Sena.png");
  const doc = new window.jspdf.jsPDF({ orientation: "landscape", unit: "pt", format: "a4" });
  const ancho = doc.internal.pageSize.getWidth();
  const alto = doc.internal.pageSize.getHeight();
  const margen = 36;
  const ahora = new Date().toLocaleString("es-CO");
  const totalPaginas = "{total_paginas}";

  // Encabezado: banda oscura de marca con logo, nombre y titulo del listado.
  doc.setFillColor(13, 27, 42);
  doc.rect(0, 0, ancho, 78, "F");
  let x = margen;
  if (logo) {
    doc.setFillColor(255, 255, 255);
    doc.roundedRect(margen, 20, 38, 38, 8, 8, "F");
    try { doc.addImage(logo, "PNG", margen + 5, 25, 28, 28); } catch (e) { /* sin logo */ }
    x = margen + 50;
  }
  doc.setTextColor(255, 255, 255);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(18);
  doc.text("FIMACOR", x, 40);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(9);
  doc.setTextColor(165, 214, 167);
  doc.text("Sistema de Control de Máquinas Textiles · CMTC – SENA", x, 55);
  doc.setTextColor(255, 255, 255);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(13);
  doc.text(titulo, ancho - margen, 40, { align: "right" });
  doc.setFont("helvetica", "normal");
  doc.setFontSize(9);
  doc.setTextColor(206, 212, 218);
  doc.text("Generado el " + ahora, ancho - margen, 55, { align: "right" });

  // Resumen: total de registros y conteo por estado en etiquetas de color.
  let y = 100;
  const etiqueta = function (texto, fondo, color) {
    doc.setFont("helvetica", "bold");
    doc.setFontSize(8.5);
    const w = doc.getTextWidth(texto) + 18;
    if (x + w > ancho - margen) { x = margen; y += 22; }
    doc.setFillColor.apply(doc, fondo);
    doc.roundedRect(x, y - 11, w, 17, 8.5, 8.5, "F");
    doc.setTextColor.apply(doc, color);
    doc.text(texto, x + 9, y);
    x += w + 6;
  };
  x = margen;
  etiqueta(datos.filas.length + " registros", [233, 236, 239], [33, 37, 41]);
  if (datos.columnasEstado.length) {
    const conteo = {};
    datos.filas.forEach(function (fila) {
      const estado = fila[datos.columnasEstado[0]] || "Sin estado";
      conteo[estado] = (conteo[estado] || 0) + 1;
    });
    Object.keys(conteo).forEach(function (estado) {
      const color = colorEstadoFimacor(estado);
      etiqueta(estado + ": " + conteo[estado], hexARgbFimacor(color.fondo), hexARgbFimacor(color.texto));
    });
  }

  doc.autoTable({
    head: [datos.encabezados],
    body: datos.filas,
    startY: y + 16,
    margin: { left: margen, right: margen, top: 40, bottom: 44 },
    theme: "grid",
    styles: { font: "helvetica", fontSize: 8.5, cellPadding: 5, textColor: [33, 37, 41], lineColor: [222, 226, 230], lineWidth: 0.5, valign: "middle" },
    headStyles: { fillColor: [46, 125, 50], textColor: 255, fontStyle: "bold", halign: "center", fontSize: 8.5 },
    alternateRowStyles: { fillColor: [246, 248, 246] },
    columnStyles: datos.columnaNumero >= 0 ? { [datos.columnaNumero]: { halign: "center", cellWidth: 32 } } : {},
    didParseCell: function (dato) {
      if (dato.section === "body" && datos.columnasEstado.indexOf(dato.column.index) >= 0 && dato.cell.raw) {
        const color = colorEstadoFimacor(String(dato.cell.raw));
        dato.cell.styles.fillColor = hexARgbFimacor(color.fondo);
        dato.cell.styles.textColor = hexARgbFimacor(color.texto);
        dato.cell.styles.fontStyle = "bold";
        dato.cell.styles.halign = "center";
      }
    },
    didDrawPage: function () {
      doc.setDrawColor(222, 226, 230);
      doc.setLineWidth(0.5);
      doc.line(margen, alto - 30, ancho - margen, alto - 30);
      doc.setFont("helvetica", "normal");
      doc.setFontSize(8);
      doc.setTextColor(108, 117, 125);
      doc.text("FIMACOR · " + titulo + " · Generado el " + ahora, margen, alto - 16);
      // Alineado desde un ancho fijo: el total de paginas se rellena al final y cambia el largo del texto.
      doc.text("Página " + doc.internal.getNumberOfPages() + " de " + totalPaginas, ancho - margen - doc.getTextWidth("Página 999 de 999"), alto - 16);
    }
  });
  if (typeof doc.putTotalPages === "function") doc.putTotalPages(totalPaginas);
  doc.setProperties({ title: "FIMACOR - " + titulo, creator: "FIMACOR" });
  descargarBlobFimacor(doc.output("blob"), nombreArchivo);
}

// Respaldo si no cargan las librerias: hoja limpia para imprimir o guardar como PDF.
function exportarTablaImpresionFimacor(tabla, titulo) {
  const escapar = function (texto) {
    return texto.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  };
  const datos = datosTablaFimacor(tabla);
  if (!datos.encabezados.length) return;
  const fila = function (celdas, etiqueta) {
    return "<tr>" + celdas.map(function (t) { return "<" + etiqueta + ">" + escapar(t) + "</" + etiqueta + ">"; }).join("") + "</tr>";
  };
  const ahora = new Date().toLocaleString("es-CO");
  const html = "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><title>" + escapar(titulo) + "</title>"
    + "<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" integrity=\"sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH\" crossorigin=\"anonymous\">"
    + "</head><body class=\"p-3\"><h1 class=\"h5 fw-bold mb-1\">FIMACOR - " + escapar(titulo) + "</h1>"
    + "<p class=\"small text-secondary mb-2\">Generado el " + escapar(ahora) + " | " + datos.filas.length + " registros</p>"
    + "<table class=\"table table-bordered table-sm small\"><thead class=\"table-success\">" + fila(datos.encabezados, "th") + "</thead><tbody>"
    + datos.filas.map(function (celdas) { return fila(celdas, "td"); }).join("")
    + "</tbody></table></body></html>";
  const ventana = window.open("", "_blank");
  if (!ventana) {
    alert("El navegador bloqueo la ventana del PDF. Permite las ventanas emergentes para este sitio e intentalo de nuevo.");
    return;
  }
  ventana.document.open();
  ventana.document.write(html);
  ventana.document.close();
  ventana.focus();
  setTimeout(function () { ventana.print(); }, 700);
}

// Respaldo si no carga ExcelJS: CSV con separador ";" (Excel en espanol lo abre en columnas).
function exportarTablaCsvFimacor(tabla, nombreArchivo) {
  const datos = datosTablaFimacor(tabla);
  const csv = [datos.encabezados].concat(datos.filas).map(function (fila) {
    return fila.map(function (t) { return '"' + t.replace(/"/g, '""') + '"'; }).join(";");
  }).join("\n");
  descargarBlobFimacor(new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8;" }), nombreArchivo);
}

// Bloque de QR (ficha de maquina o acta) con clases de Bootstrap.
function plantillaQrFimacor(qrUrl, altImagen, titulo, descripcion, acciones, nota, conFeedback) {
  return `
    <div class="card border-success-subtle shadow-sm">
      <div class="card-body d-flex flex-column flex-sm-row gap-3 align-items-start">
        <div class="fimacor-qr-dibujo flex-shrink-0 text-center">
          <img src="${qrUrl}" alt="${altImagen}" width="140" height="140" class="img-thumbnail" />
          <span class="qr-sin-imagen-texto d-none small text-secondary">QR no disponible sin conexion.</span>
        </div>
        <div class="d-flex flex-column gap-2">
          <strong class="text-success">${titulo}</strong>
          <span class="small text-secondary">${descripcion}</span>
          <div class="fimacor-qr-acciones d-flex flex-wrap gap-2">${acciones}</div>
          <small class="text-secondary">${nota}</small>
          ${conFeedback ? '<div class="fimacor-qr-feedback d-none alert alert-success py-1 px-2 small mb-0" role="status" aria-live="polite">Enlace copiado al portapapeles.</div>' : ''}
        </div>
      </div>
    </div>`;
}

function conectarQrFimacor(contenedor, url) {
  const imagen = contenedor.querySelector("img");
  imagen.addEventListener("error", function () {
    contenedor.classList.add("qr-sin-imagen");
    imagen.classList.add("d-none");
    contenedor.querySelector(".qr-sin-imagen-texto").classList.remove("d-none");
  });
  const copiar = contenedor.querySelector("[data-qr-copy]");
  if (copiar) copiar.addEventListener("click", function () {
    copiarTextoFimacor(url).then(function (copiado) {
      if (copiado) {
        const feedback = contenedor.querySelector(".fimacor-qr-feedback");
        const textoBoton = copiar.querySelector(".qr-boton-texto");
        const checkBoton = copiar.querySelector(".qr-boton-check");
        contenedor.classList.add("copiado");
        if (feedback) feedback.classList.remove("d-none");
        if (textoBoton && checkBoton) { textoBoton.classList.add("d-none"); checkBoton.classList.remove("d-none"); }
        setTimeout(function () {
          contenedor.classList.remove("copiado");
          if (feedback) feedback.classList.add("d-none");
          if (textoBoton && checkBoton) { textoBoton.classList.remove("d-none"); checkBoton.classList.add("d-none"); }
        }, 1800);
      } else {
        mostrarLinkManualQrFimacor(contenedor, url);
      }
    });
  });
  const configurarBoton = contenedor.querySelector("[data-qr-config]");
  if (configurarBoton) configurarBoton.addEventListener("click", configurarQrCelularFimacor);
}

function activarQrFichasTecnicasFimacor() {
  document.querySelectorAll(".fimacor-qr-ficha[data-maquina-id]").forEach(function (contenedor) {
    if (contenedor.dataset.qrListo === "true") return;
    const id = contenedor.dataset.maquinaId;
    if (!id) return;
    contenedor.dataset.qrListo = "true";

    const baseQr = obtenerBaseQrFimacor();
    const enlaceFicha = baseQr + "/MaquinaQrServlet?id=" + encodeURIComponent(id);
    const qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=260x260&margin=10&data=" + encodeURIComponent(enlaceFicha);
    const necesitaConfigCelular = qrUsaLocalhostFimacor();

    contenedor.classList.add("fimacor-qr-maquina");
    contenedor.innerHTML = plantillaQrFimacor(
      qrUrl,
      "QR de esta maquina",
      "QR de la maquina",
      "Quien lo escanee ve la informacion de la maquina. Un Instructor puede reportar una novedad con su codigo.",
      `<a href="${enlaceFicha}" target="_blank" rel="noopener noreferrer" class="btn btn-success btn-sm">Abrir pagina de la maquina</a>
       <button type="button" class="btn btn-outline-success btn-sm" data-qr-copy><span class="qr-boton-texto">Copiar enlace</span><span class="qr-boton-check d-none">Enlace copiado</span></button>
       ${necesitaConfigCelular ? '<button type="button" class="btn btn-outline-secondary btn-sm" data-qr-config>Configurar red actual</button>' : ''}`,
      necesitaConfigCelular ? 'Estas en localhost: configura la IP de este PC para abrir el QR desde el celular.' : 'El enlace y el QR abren la pagina publica de la maquina.',
      true
    );
    conectarQrFimacor(contenedor, enlaceFicha);
  });
}

function activarQrActasReubicacionFimacor() {
  document.querySelectorAll(".fimacor-qr-acta[data-acta-id]").forEach(function (contenedor) {
    if (contenedor.dataset.qrListo === "true") return;
    const id = contenedor.dataset.actaId;
    if (!id) return;
    contenedor.dataset.qrListo = "true";
    const baseRuta = window.location.pathname.replace(/[^/]*$/, "");
    const url = obtenerBaseQrFimacor() + baseRuta + "ActaReubicacionPdfServlet?id=" + encodeURIComponent(id);
    const qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=260x260&margin=10&data=" + encodeURIComponent(url);
    const configurar = qrUsaLocalhostFimacor();
    contenedor.classList.add("fimacor-qr-maquina");
    contenedor.innerHTML = plantillaQrFimacor(
      qrUrl,
      "QR del acta de traslado",
      "QR del acta de traslado",
      "Escanéalo desde el celular para abrir el PDF completo, con la evidencia y la firma digital.",
      `<button type="button" class="btn btn-outline-success btn-sm" data-qr-copy>Copiar enlace del acta</button>${configurar ? '<button type="button" class="btn btn-outline-secondary btn-sm" data-qr-config>Configurar red actual</button>' : ''}`,
      configurar ? 'Estás en localhost: configura la IP de este PC para abrir el acta desde el celular.' : 'El QR abre el acta PDF de este traslado.',
      true
    );
    conectarQrFimacor(contenedor, url);
  });
}

function obtenerBaseQrFimacor() {
  const baseServidor = (window.FIMACOR_QR_BASE_ORIGIN || "").trim();
  if (/^https?:\/\/[^/]+$/i.test(baseServidor) && !/\.railway\.internal(?::|$)/i.test(baseServidor)) {
    return baseServidor;
  }
  const baseGuardada = (localStorage.getItem("fimacorQrBaseOrigin") || "").trim();
  if (/^https?:\/\/[^/]+$/i.test(baseGuardada)) {
    return baseGuardada.replace(/\/+$/, "");
  }
  return window.location.origin;
}

function qrUsaLocalhostFimacor() {
  // La pagina puede estar abierta en localhost, pero el QR ya puede contener
  // la IP real de la red o el dominio publico de Railway. Se valida el enlace
  // que se va a escanear, no la direccion que muestra el navegador.
  try {
    const hostQr = new URL(obtenerBaseQrFimacor()).hostname;
    return /^(localhost|127\.0\.0\.1|\[?::1\]?)$/i.test(hostQr || "");
  } catch (error) {
    return true;
  }
}

function configurarQrCelularFimacor() {
  const actual = localStorage.getItem("fimacorQrBaseOrigin") || "";
  const pista = obtenerPistaPuertoQrFimacor();
  const valor = window.prompt(
    "Escribe la direccion del PC en esta red para que el QR abra en tu celular. Ejemplo: http://192.168.1.18" + pista + "\\n\\nSi estas en el SENA y cambia la red, vuelve a configurar este dato.",
    actual || "http://IP-DE-TU-PC" + pista
  );
  if (valor === null) return;
  const limpio = valor.trim().replace(/\/+$/, "");
  if (!/^https?:\/\/[^/]+$/i.test(limpio) || /localhost|127\.0\.0\.1/i.test(limpio)) {
    window.alert("Usa la IP de red del PC, no localhost. Ejemplo: http://192.168.1.18" + pista);
    return;
  }
  localStorage.setItem("fimacorQrBaseOrigin", limpio);
  window.alert("Listo. El QR ahora usara esa direccion para abrir desde el celular.");
  window.location.reload();
}

function obtenerPistaPuertoQrFimacor() {
  const puerto = window.location.port ? ":" + window.location.port : "";
  return puerto || ":8080";
}

function copiarTextoFimacor(texto) {
  if (navigator.clipboard && navigator.clipboard.writeText && window.isSecureContext) {
    return navigator.clipboard.writeText(texto).then(function () {
      return true;
    }).catch(function () {
      return copiarTextoFallbackFimacor(texto);
    });
  }
  return Promise.resolve(copiarTextoFallbackFimacor(texto));
}

function copiarTextoFallbackFimacor(texto) {
  const area = document.createElement("textarea");
  area.value = texto;
  area.setAttribute("readonly", "readonly");
  area.className = "position-fixed top-0 start-0 opacity-0";
  document.body.appendChild(area);
  area.focus();
  area.select();
  area.setSelectionRange(0, area.value.length);
  let copiado = false;
  try {
    copiado = document.execCommand("copy");
  } catch (error) {
    copiado = false;
  }
  document.body.removeChild(area);
  return copiado;
}

function mostrarLinkManualQrFimacor(qr, url) {
  let panel = qr.querySelector(".fimacor-qr-link-manual");
  if (!panel) {
    panel = document.createElement("div");
    panel.className = "fimacor-qr-link-manual alert alert-warning small mt-2 mb-0";
    panel.innerHTML = '<strong class="d-block">No se pudo copiar automatico</strong><input type="text" class="form-control form-control-sm my-1" readonly><small>Selecciona el enlace y copialo manualmente.</small>';
    qr.appendChild(panel);
  }
  const input = panel.querySelector("input");
  input.value = url;
  input.focus();
  input.select();
}

function activarLineaTiempoMaquinasFimacor() {
  const puedeReportarNovedad = document.body.dataset.puedeReportarNovedad === "true";
  document.querySelectorAll(".modalmaquina").forEach(function (modal) {
    if (modal.dataset.timelineFimacor === "true") return;
    const grid = modal.querySelector(".modalmaquina-grid");
    if (!grid) return;
    modal.dataset.timelineFimacor = "true";

    const texto = modal.textContent || "";
    const compra = extraerDatoTextoFimacor(texto, "Fecha compra") || "Sin fecha";
    const ubicacion = extraerDatoTextoFimacor(texto, "Fecha ubicacion") || "Sin fecha";
    const garantia = extraerDatoTextoFimacor(texto, "Fecha garantia") || "Sin fecha";
    const filaMaquina = Array.prototype.find.call(document.querySelectorAll(".fila-maquina[data-machine-id]"), function (fila) {
      return fila.dataset.modal === modal.id;
    });
    const idMaquina = filaMaquina ? filaMaquina.dataset.machineId : "";
    const contexto = document.body.dataset.context || "";
    const idCodificado = encodeURIComponent(idMaquina);
    // href: navega a otra pantalla. seccion: se queda en la ficha y resalta ese bloque.
    const boton = function (destino, titulo, detalle, icono) {
      const atributo = destino.seccion ? `data-seccion="${destino.seccion}"` : `data-href="${contexto}${destino.href}"`;
      return `<button type="button" class="btn btn-light border text-start d-flex align-items-start gap-2 px-3 py-2" ${atributo}>`
        + `<i class="bi ${icono} text-success mt-1" aria-hidden="true"></i>`
        + `<span class="d-flex flex-column lh-sm"><b class="small text-body">${titulo}</b><small class="text-secondary">${detalle}</small></span></button>`;
    };

    const linea = document.createElement("div");
    linea.className = "fimacor-linea-tiempo-maquina border-top pt-3 mt-3";
    linea.innerHTML = `
      <h4 class="h6 fw-bold mb-2">Línea de tiempo de la máquina</h4>
      <div class="fimacor-timeline-items d-flex flex-wrap gap-2">
        ${boton({ seccion: "Fecha compra" }, "Compra", compra, "bi-bag-check")}
        ${boton({ seccion: "Fecha ubicacion" }, "Ubicación", ubicacion, "bi-geo-alt")}
        ${boton({ href: "/mantenimiento/historial" }, "Mantenimiento", "Ver historial", "bi-tools")}
        ${puedeReportarNovedad ? boton({ href: "/novedades?maquina=" + idCodificado }, "Novedad", "Reportes asociados", "bi-exclamation-triangle") : ''}
        ${boton({ href: "/reubicacion" }, "Reubicación", "Movimientos", "bi-arrow-left-right")}
        ${boton({ seccion: "Fecha garantia" }, "Garantía", garantia, "bi-shield-check")}
      </div>
    `;
    linea.querySelectorAll("button[data-seccion]").forEach(function (botonLinea) {
      botonLinea.addEventListener("click", function () {
        const etiqueta = botonLinea.dataset.seccion.toLowerCase();
        const sinTildes = function (t) { return t.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, ""); };
        const bloque = Array.prototype.find.call(modal.querySelectorAll(".modalmaquina-bloque, .modalmaquina-grid p, .modalmaquina-grid li"), function (el) {
          return sinTildes(el.textContent || "").includes(etiqueta);
        });
        if (!bloque) return;
        bloque.scrollIntoView({ behavior: "smooth", block: "center" });
        bloque.classList.add("bg-success-subtle", "rounded-3");
        setTimeout(function () { bloque.classList.remove("bg-success-subtle", "rounded-3"); }, 2000);
      });
    });
    linea.querySelectorAll("button[data-href]").forEach(function (botonLinea) {
      botonLinea.addEventListener("click", function () {
        const titulo = botonLinea.querySelector("b") ? botonLinea.querySelector("b").textContent : "Modulo";
        if (window.FimacorCargando) {
          window.FimacorCargando.show("Abriendo " + titulo, "Cargando la pantalla relacionada con esta maquina...");
        }
        setTimeout(function () {
          window.location.href = botonLinea.dataset.href;
        }, 400);
      });
    });
    grid.insertAdjacentElement("afterend", linea);
  });
}

function extraerDatoTextoFimacor(texto, etiqueta) {
  const patron = new RegExp(etiqueta + "\\s*:\\s*([^\\n\\r]+)", "i");
  const coincidencia = texto.match(patron);
  if (!coincidencia) return "";
  return coincidencia[1].replace(/\s+/g, " ").trim().split(" ")[0] || "";
}

function activarAlertasGarantiaFimacor() {
  const hoy = new Date();
  document.querySelectorAll(".modalmaquina-bloque").forEach(function (bloque) {
    if (bloque.dataset.garantiaFimacor === "true") return;
    const texto = bloque.textContent || "";
    const coincidencia = texto.match(/Fecha\s+garant[iÃ­]a:\s*([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}\/[0-9]{2}\/[0-9]{4})/i);
    if (!coincidencia) return;

    const fecha = normalizarFechaFimacor(coincidencia[1]);
    if (!fecha) return;
    bloque.dataset.garantiaFimacor = "true";
    const dias = Math.ceil((fecha.getTime() - hoy.getTime()) / 86400000);
    const alerta = document.createElement("div");
    alerta.className = "fimacor-garantia-alerta alert small fw-semibold py-2 px-3 mt-2 mb-0";

    if (dias < 0) {
      alerta.classList.add("vencida", "alert-danger");
      alerta.textContent = "Garantia vencida: revisa soporte o renovacion.";
    } else if (dias <= 30) {
      alerta.classList.add("proxima", "alert-warning");
      alerta.textContent = "Garantia proxima a vencer: faltan " + dias + " dias.";
    } else {
      alerta.classList.add("alert-success");
      alerta.textContent = "Garantia vigente: faltan " + dias + " dias.";
    }
    bloque.appendChild(alerta);
  });
}

function normalizarFechaFimacor(valor) {
  if (!valor) return null;
  if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) return new Date(valor + "T00:00:00");
  const partes = valor.split("/");
  if (partes.length === 3) return new Date(partes[2] + "-" + partes[1] + "-" + partes[0] + "T00:00:00");
  return null;
}

function activarSemaforoEstadosMaquina() {
  document.querySelectorAll(".estadomaquinasregistradas, .modalmaquina-bloque p").forEach(function (elemento) {
    if (elemento.dataset.semaforoFimacor === "true") return;
    const texto = (elemento.textContent || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
    const esEstado = elemento.classList.contains("estadomaquinasregistradas") || texto.includes("estado:");
    if (!esEstado) return;
    elemento.dataset.semaforoFimacor = "true";
    elemento.classList.add("fimacor-semaforo-estado", "fw-semibold");
    let semaforo = "semaforo-verde", color = "success";
    if (texto.includes("fuera") || texto.includes("dan") || texto.includes("aver") || texto.includes("falla")) {
      semaforo = "semaforo-rojo"; color = "danger";
    } else if (texto.includes("mantenimiento") || texto.includes("revision") || texto.includes("pendiente")) {
      semaforo = "semaforo-amarillo"; color = "warning";
    }
    elemento.classList.add(semaforo);
    if (elemento.classList.contains("badge")) {
      Array.from(elemento.classList).forEach(function (clase) {
        if (/^(text-bg-|bg-|text-|border-)(success|danger|warning|secondary|primary|info)/.test(clase)) elemento.classList.remove(clase);
      });
      elemento.classList.add("bg-" + color + "-subtle", "text-" + color + "-emphasis", "border", "border-" + color + "-subtle");
    } else {
      elemento.classList.add("text-" + color + "-emphasis");
    }
  });
}
