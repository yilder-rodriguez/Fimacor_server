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

function limpiarCargaRegistroAlVolver() {
  document.querySelectorAll('.modal-login').forEach(modal => {
    modal.classList.add('d-none');
  });
  if (window.FimacorCargando) window.FimacorCargando.hide();
}

window.addEventListener('pageshow', limpiarCargaRegistroAlVolver);
window.addEventListener('popstate', limpiarCargaRegistroAlVolver);

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.toggle-pass').forEach(icono => {
    icono.innerHTML = SVG_OJO_CERRADO;
  });

  activarVistaUltimoCaracterPassword();
  blindarCampoDocumento();

  cargarDatosRegistro();

  const btnVolverLoginRegistro = document.getElementById('btnVolverLoginRegistro');
  const modalVolverLoginRegistro = document.getElementById('modalVolverLoginRegistro');

  if (btnVolverLoginRegistro && modalVolverLoginRegistro) {
    btnVolverLoginRegistro.addEventListener('click', function (event) {
      event.preventDefault();
      modalVolverLoginRegistro.classList.remove('d-none');

      window.location.assign(btnVolverLoginRegistro.href);
    });
  }
});

if (document.getElementById('formRegistro')) {
  const params   = new URLSearchParams(window.location.search);
  const mensajes = document.getElementById('mensajes');

  if (mensajes) {
    if (params.get('error') === '1')         mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">Error al registrar. Intente de nuevo.</p>';
    if (params.get('error') === 'correo')    mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">Este correo electrónico ya está registrado.</p>';
    if (params.get('error') === 'documento') mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">Este número de documento ya está registrado.</p>';
    if (params.get('error') === 'telefono')  mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">Este número de teléfono ya está registrado.</p>';
    if (params.get('error') === 'rol')       mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">El rol seleccionado no está permitido para este registro.</p>';
    if (params.get('error') === 'sesion')    mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">La sesión de verificación venció. Solicita un código nuevo.</p>';
    if (params.get('error') === 'documento_bd') mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">No se pudo guardar el documento. Revisa el tipo y número de documento.</p>';
    if (params.get('error') === 'insertar')  mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-2">No se pudo crear el usuario. Revisa que correo, documento y teléfono no estén repetidos.</p>';
  }
}

function guardarDatosRegistro() {
  const datos = {
    nombre: document.getElementById('nombre').value,
    apellido: document.getElementById('apellido').value,
    correo: document.getElementById('correo').value,
    telefono: document.getElementById('telefono').value,
    rol: document.getElementById('rol').value,
    activo: document.getElementById('activo') ? document.getElementById('activo').value : '1',
    tipoDoc: document.getElementById('tipoDoc').value,
    documento: document.getElementById('documento').value
  };

  localStorage.setItem('datosRegistroFimacor', JSON.stringify(datos));
}

function cargarDatosRegistro() {
  const datosGuardados = localStorage.getItem('datosRegistroFimacor');

  if (!datosGuardados) return;

  const datos = JSON.parse(datosGuardados);

  if (document.getElementById('nombre')) document.getElementById('nombre').value = datos.nombre || '';
  if (document.getElementById('apellido')) document.getElementById('apellido').value = datos.apellido || '';
  if (document.getElementById('correo')) document.getElementById('correo').value = datos.correo || '';
  if (document.getElementById('telefono')) document.getElementById('telefono').value = datos.telefono || '';
  if (document.getElementById('rol')) document.getElementById('rol').value = datos.rol || '';
  if (document.getElementById('activo')) document.getElementById('activo').value = datos.activo || '1';
  if (document.getElementById('tipoDoc')) document.getElementById('tipoDoc').value = datos.tipoDoc || '';
  if (document.getElementById('documento')) document.getElementById('documento').value = datos.documento || '';
}

function limpiarDatosRegistro() {
  localStorage.removeItem('datosRegistroFimacor');
}

function validarCorreoEnVivo() {
  const correo = document.getElementById('correo');
  const status = document.getElementById('correoStatus');
  if (!correo || !status) return;

  const valor = correo.value.trim();
  if (valor === '') {
    status.innerHTML = '';
    return;
  }

  if (/^[a-zA-Z0-9._%+-]+@gmail\.com$/.test(valor)) {
    status.innerHTML = '<span class="text-success fw-semibold">Correo válido</span>';
  } else {
    status.innerHTML = '<span class="text-danger fw-semibold">Debe ser una cuenta @gmail.com</span>';
  }
}

async function validarFormulario() {
  const esRegistroAdministrativo = window.FIMACOR_REGISTRO_ADMIN_ACTIVO === true
    || document.querySelector('#formRegistro input[name="origen"]')?.value === 'admin';
  const nombre     = document.getElementById('nombre').value.trim();
  const apellido   = document.getElementById('apellido').value.trim();
  const tipoDoc    = document.getElementById('tipoDoc').value;
  const documento  = document.getElementById('documento').value.trim();
  const telefono   = document.getElementById('telefono').value.trim();
  const correo     = document.getElementById('correo').value.trim();
  const rol        = document.getElementById('rol').value;
  const activo     = document.getElementById('activo') ? document.getElementById('activo').value : '1';
  const contrasena = document.getElementById('contrasena').value;
  const confirmar  = document.getElementById('confirmar').value;
  const aceptaPolitica = document.getElementById('aceptaPolitica').checked;
  const mensajes   = document.getElementById('mensajes');

  const mostrarError = (msg) => {
    mensajes.innerHTML = `<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-2"> ${msg}</p>`;
    window.scrollTo(0, 0);
  };

  if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(nombre)) return mostrarError('El nombre solo puede contener letras.');
  if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(apellido)) return mostrarError('El apellido solo puede contener letras.');
  if (tipoDoc === '') return mostrarError('Seleccione un tipo de documento.');
  if (!/^\d{6,12}$/.test(documento)) return mostrarError('El número de documento debe tener entre 6 y 12 dígitos.');
  if (!/^\d{10}$/.test(telefono)) return mostrarError('El teléfono debe tener exactamente 10 dígitos.');
  if (!/^[a-zA-Z0-9._%+-]+@gmail\.com$/.test(correo)) return mostrarError('El correo debe ser una cuenta @gmail.com válida.');
  if (rol === '') return mostrarError('Seleccione un rol.');
  if (activo !== '1' && activo !== '0') return mostrarError('Seleccione un estado.');
  if (contrasena.length < 8) return mostrarError('La contraseña debe tener mínimo 8 caracteres.');
  if (confirmar.length < 8) return mostrarError('La confirmación debe tener mínimo 8 caracteres.');
  if (!/[A-Z]/.test(contrasena)) return mostrarError('La contraseña debe tener al menos una letra mayúscula.');
  if (!/[a-z]/.test(contrasena)) return mostrarError('La contraseña debe tener al menos una letra minúscula.');
  if (!/[0-9]/.test(contrasena)) return mostrarError('La contraseña debe tener al menos un número.');
  if (!/[.@#$%^&*!_\-+=/]/.test(contrasena)) return mostrarError('La contraseña debe tener al menos un carácter especial.');
  if (contrasena !== confirmar) return mostrarError('Las contraseñas no coinciden.');
  if (!aceptaPolitica) return mostrarError('Debes aceptar la política de tratamiento de datos para registrar el usuario.');

  const repetido = await validarDatosUnicosRegistro(correo, documento, telefono);
  if (repetido) return mostrarError(repetido);

  mensajes.innerHTML = '';
  guardarDatosRegistro();

  const modalRegistro = document.getElementById('modalRegistro');

  if (modalRegistro && !esRegistroAdministrativo) {
    modalRegistro.classList.remove('d-none');

    setTimeout(function () {
      document.getElementById('formRegistro').submit();
    }, 2000);
  } else {
    document.getElementById('formRegistro').submit();
  }
}

async function validarDatosUnicosRegistro(correo, documento, telefono) {
  const validaciones = [
    { tipo: 'correo', valor: correo, mensaje: 'Ese correo electrónico ya está registrado. Usa otro correo o inicia sesión.' },
    { tipo: 'documento', valor: documento, mensaje: 'Ese número de documento ya está registrado.' },
    { tipo: 'telefono', valor: telefono, mensaje: 'Ese número de teléfono ya está registrado.' }
  ];

  for (const item of validaciones) {
    const existe = await existeDatoRegistro(item.tipo, item.valor);
    if (existe) return item.mensaje;
  }
  return '';
}

async function existeDatoRegistro(tipo, valor) {
  try {
    const respuesta = await fetch(
      CONTEXTO + '/ValidarDatoRegistroServlet?tipo=' + encodeURIComponent(tipo) + '&valor=' + encodeURIComponent(valor),
      { headers: { 'Accept': 'application/json' } }
    );
    const datos = await respuesta.json();
    return respuesta.ok && datos.ok && datos.existe === true;
  } catch (error) {
    return true;
  }
}

function blindarCampoDocumento() {
  const documento = document.getElementById('documento');
  if (!documento) return;
  documento.setAttribute('autocomplete', 'off');
  documento.addEventListener('input', () => {
    const limpio = documento.value.replace(/\D/g, '').slice(0, 12);
    if (documento.value !== limpio) documento.value = limpio;
  });
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
