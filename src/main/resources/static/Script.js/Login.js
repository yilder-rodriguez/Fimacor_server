const SVG_OJO_ABIERTO = `
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20"
       viewBox="0 0 24 24" fill="none" stroke="currentColor"
       stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
    <circle cx="12" cy="12" r="3"/>
  </svg>`;

const SVG_OJO_CERRADO = `
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20"
       viewBox="0 0 24 24" fill="none" stroke="currentColor"
       stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8
             a18.45 18.45 0 0 1 5.06-5.94"/>
    <path d="M9.9 4.24A9.12 9 0 0 1 12 4c7 0 11 8 11 8
             a18.5 18.5 0 0 1-2.16 3.19"/>
    <line x1="1" y1="1" x2="23" y2="23"/>
  </svg>`;

function limpiarCargaLoginAlVolver() {
  const modal = document.getElementById("modalLogin");
  if (modal) modal.classList.add("d-none");
  if (window.FimacorCargando) window.FimacorCargando.hide();
}

window.addEventListener("pageshow", function (e) {
  limpiarCargaLoginAlVolver();
  // Al volver con "atras" el navegador puede mostrar esta pantalla guardada, con un token de seguridad ya vencido:
  // se recarga para pedir uno nuevo y que el ingreso no falle.
  if (e.persisted) window.location.reload();
});

// Si la pestana estuvo abierta mucho rato (la sesion del servidor ya pudo caducar) y no se ha escrito nada, se renueva sola.
(function () {
  const abierta = Date.now();
  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState !== "visible" || Date.now() - abierta < 20 * 60 * 1000) return;
    const escrito = Array.from(document.querySelectorAll("#formLogin input:not([type=hidden])")).some(i => i.value);
    if (!escrito) window.location.reload();
  });
})();
window.addEventListener("popstate", limpiarCargaLoginAlVolver);

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.toggle-pass').forEach(icono => {
    icono.innerHTML = SVG_OJO_CERRADO;
  });

  activarVistaUltimoCaracterPassword();

  const parametros = new URLSearchParams(window.location.search);
  const mensajes = document.getElementById("mensajes");

  if (mensajes) {
    if (parametros.get("recuperado") === "1") {
      mensajes.innerHTML = '<div class="success-msg alert alert-success small text-center py-2 px-2 mb-2">Contrase&ntilde;a actualizada correctamente. Ahora puedes iniciar sesi&oacute;n.</div>';
      const modalContrasenaActualizada = document.getElementById("modalContrasenaActualizada");
      if (modalContrasenaActualizada) modalContrasenaActualizada.classList.remove("d-none");
    }

    if (parametros.get("error") === "1") {
      mensajes.innerHTML = '<div class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">Correo o contrase&ntilde;a incorrectos.</div>';
    }

    if (parametros.get("error") === "inactivo") {
      mensajes.innerHTML = '<div class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">Usuario inactivo. Contacte a un administrador para activar su acceso.</div>';
    }
  }

  if (parametros.get("registro") === "ok") {
    const modalRegistroOk = document.getElementById("modalRegistroOk");

    if (modalRegistroOk) {
      modalRegistroOk.classList.remove("d-none");
    }
  }

  const btnCerrarContrasenaActualizada = document.getElementById("btnCerrarContrasenaActualizada");
  if (btnCerrarContrasenaActualizada) {
    btnCerrarContrasenaActualizada.addEventListener("click", function () {
      const modal = document.getElementById("modalContrasenaActualizada");
      if (modal) modal.classList.add("d-none");
      window.history.replaceState({}, document.title, window.location.pathname);
    });
  }

  const btnCerrarRegistroOk = document.getElementById("btnCerrarRegistroOk");

  if (btnCerrarRegistroOk) {
    btnCerrarRegistroOk.addEventListener("click", function () {
      const modalRegistroOk = document.getElementById("modalRegistroOk");

      if (modalRegistroOk) {
        modalRegistroOk.classList.add("d-none");
      }

      window.history.replaceState({}, document.title, window.location.pathname);
    });
  }

  const formLogin = document.getElementById("formLogin");

  if (formLogin) {
    formLogin.addEventListener("submit", function (e) {
      const correoInput = document.getElementById("correo");
      const contrasenaInput = document.getElementById("contrasena");
      const correo = correoInput ? correoInput.value.trim() : "";
      const contrasena = contrasenaInput ? contrasenaInput.value : "";
      const mensajes = document.getElementById("mensajes");

      if (mensajes) mensajes.innerHTML = "";

      if (!correo || !contrasena) {
        e.preventDefault();
        if (mensajes) mensajes.innerHTML = '<div class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">Por favor completa todos los campos.</div>';
        return;
      }

      const modalTitulo = document.getElementById("modalTitulo");
      const modalTexto = document.getElementById("modalTexto");
      const modalLogin = document.getElementById("modalLogin");
      if (modalTitulo) modalTitulo.innerHTML = "Iniciando sesi&oacute;n";
      if (modalTexto) modalTexto.innerHTML = "Validando tus credenciales...";
      if (modalLogin) modalLogin.classList.remove("d-none");
    });
  }
const btnIrRegistro = document.getElementById("btnIrRegistro");

  if (btnIrRegistro) {
    btnIrRegistro.addEventListener("click", function (e) {
      e.preventDefault();

      document.getElementById("modalTitulo").innerHTML = "Redireccionando";
      document.getElementById("modalTexto").innerHTML = "Abriendo el formulario de registro...";
      document.getElementById("modalLogin").classList.remove("d-none");

      window.location.assign(btnIrRegistro.href);
    });
  }
});

function activarVistaUltimoCaracterPassword() {
  document.querySelectorAll('.pass-wrapper input[type="password"], .password-wrapper input[type="password"]').forEach(input => {
    if (input.dataset.ultimoCaracterListo === "1") return;

    input.dataset.ultimoCaracterListo = "1";
    input.dataset.longitudAnterior = String(input.value.length);

    const contenedor = input.closest('.pass-wrapper, .password-wrapper') || input.parentElement;
    const indicador = document.createElement('span');
    indicador.className = 'password-last-char position-absolute top-50 end-0 translate-middle-y me-5 pe-2 fw-bold text-success d-none';
    indicador.setAttribute('aria-hidden', 'true');
    contenedor.appendChild(indicador);

    let temporizador = null;

    input.addEventListener('input', () => {
      const longitudAnterior = Number(input.dataset.longitudAnterior || 0);
      const valorActual = input.value || "";
      const longitudActual = valorActual.length;

      clearTimeout(temporizador);
      indicador.textContent = "";
      indicador.classList.remove('is-visible'); indicador.classList.add('d-none');

      if (input.type === "password" && longitudActual > longitudAnterior && longitudActual > 0) {
        indicador.textContent = valorActual.charAt(longitudActual - 1);
        indicador.classList.add('is-visible'); indicador.classList.remove('d-none');

        temporizador = setTimeout(() => {
          indicador.classList.remove('is-visible'); indicador.classList.add('d-none');
          indicador.textContent = "";
        }, 700);
      }

      input.dataset.longitudAnterior = String(longitudActual);
    });
  });
}

function ocultarVistaUltimoCaracterPassword(input) {
  if (!input) return;

  const contenedor = input.closest('.pass-wrapper, .password-wrapper') || input.parentElement;
  const indicador = contenedor ? contenedor.querySelector('.password-last-char') : null;

  if (indicador) {
    indicador.classList.remove('is-visible'); indicador.classList.add('d-none');
    indicador.textContent = "";
  }

  input.dataset.longitudAnterior = String(input.value.length);
}
function toggleVer(idInput, icono) {
  const input = document.getElementById(idInput);

  if (!input) return;

  if (input.type === "password") {
    ocultarVistaUltimoCaracterPassword(input);
    input.type = "text";
    icono.innerHTML = SVG_OJO_ABIERTO;
  } else {
    input.type = "password";
    ocultarVistaUltimoCaracterPassword(input);
    icono.innerHTML = SVG_OJO_CERRADO;
  }
}




