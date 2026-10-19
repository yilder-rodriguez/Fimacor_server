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
    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8
             a18.5 18.5 0 0 1-2.16 3.19"/>
    <line x1="1" y1="1" x2="23" y2="23"/>
  </svg>`;

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.toggle-pass').forEach(icono => {
    icono.innerHTML = SVG_OJO_CERRADO;
  });

  activarVistaUltimoCaracterPassword();

  const parametros = new URLSearchParams(window.location.search);

  if (parametros.get("error") === "1") {
    mostrarError("No se pudo cambiar la contraseña. Intenta nuevamente.");
  }

  if (parametros.get("error") === "2") {
    mostrarError("El código de verificación es incorrecto.");
  }

  if (parametros.get("error") === "3") {
    mostrarError("La nueva contraseña no puede ser igual a la contraseña anterior.");
  }

  if (parametros.get("reenviado") === "1") {
    mostrarMensaje("Código reenviado correctamente.");
  }

  const btnCambiarContrasena = document.getElementById("btnCambiarContrasena");
  const formCambiarContrasena = document.getElementById("formCambiarContrasena");
  const modalCambiarContrasena = document.getElementById("modalCambiarContrasena");

  if (btnCambiarContrasena && formCambiarContrasena && modalCambiarContrasena) {
    btnCambiarContrasena.addEventListener("click", function () {
      if (!formCambiarContrasena.checkValidity()) {
        formCambiarContrasena.reportValidity();
        return;
      }

      if (!validar()) {
        return;
      }

      mostrarCargaCambioContrasena(
        modalCambiarContrasena,
        "Cambiando contraseña",
        "Validando código y actualizando credenciales de forma segura..."
      );

      setTimeout(function () {
        formCambiarContrasena.submit();
      }, 2000);
    });
  }
});

function mostrarCargaCambioContrasena(modal, titulo, mensaje) {
  if (typeof window.mostrarCargaFimacor === "function") {
    if (modal) {
      modal.classList.add("d-none");
    }
    window.mostrarCargaFimacor(titulo, mensaje);
    return;
  }

  if (modal) {
    modal.classList.remove("d-none");
  }
}

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

function mostrarError(mensaje) {
  const errorPass = document.getElementById("errorPass");

  if (errorPass) {
    errorPass.classList.remove("d-none", "text-success");
    errorPass.classList.add("d-block", "text-danger");
    errorPass.innerHTML = mensaje;
  }
}

function mostrarMensaje(mensaje) {
  const errorPass = document.getElementById("errorPass");

  if (errorPass) {
    errorPass.classList.remove("d-none", "text-danger");
    errorPass.classList.add("d-block", "text-success");
    errorPass.innerHTML = mensaje;
  }
}

function validar() {
  const nuevaPassword = document.getElementById("nuevaPassword").value;
  const confirmar = document.getElementById("confirmar").value;

  ocultarMensaje();

  if (nuevaPassword.length < 8) {
    mostrarError("La contraseña debe tener mínimo 8 caracteres.");
    return false;
  }

  if (confirmar.length < 8) {
    mostrarError("La confirmación debe tener mínimo 8 caracteres.");
    return false;
  }

  if (!/[A-Z]/.test(nuevaPassword)) {
    mostrarError("La contraseña debe tener al menos una letra mayúscula.");
    return false;
  }

  if (!/[a-z]/.test(nuevaPassword)) {
    mostrarError("La contraseña debe tener al menos una letra minúscula.");
    return false;
  }

  if (!/[0-9]/.test(nuevaPassword)) {
    mostrarError("La contraseña debe tener al menos un número.");
    return false;
  }

  if (!/[.@#$%^&*!_\-+=/]/.test(nuevaPassword)) {
    mostrarError("La contraseña debe tener al menos un carácter especial.");
    return false;
  }

  if (nuevaPassword !== confirmar) {
    mostrarError("Las contraseñas no coinciden.");
    return false;
  }

  return true;
}

function ocultarMensaje() {
  const errorPass = document.getElementById("errorPass");

  if (errorPass) {
    errorPass.classList.add("d-none");
    errorPass.innerHTML = "";
  }
}













