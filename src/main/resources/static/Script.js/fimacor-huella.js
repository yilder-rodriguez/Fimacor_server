// FIMACOR · Ingreso con huella (solo dentro de la app Android).
// Guarda el correo y la contrasena en el almacen seguro del celular (Android Keystore), protegidos por la huella:
// para leerlos hace falta una verificacion biometrica en vivo. En el navegador normal este archivo no hace nada.
// Se carga en el login (boton y activacion) y en el menu (para terminar de guardar tras un ingreso correcto).
(function () {
  const cap = window.Capacitor;
  if (!cap || typeof cap.nativePromise !== "function" || !(cap.isNativePlatform && cap.isNativePlatform())) return;

  const SERVIDOR = "fimacor:" + location.host;
  const CLAVE_ACTIVA = "fimacorHuellaCorreo";      // correo con huella activada en este celular
  const CLAVE_RECHAZO = "fimacorHuellaRechazada";   // la persona dijo "no" a activarla
  const CLAVE_PENDIENTE = "fimacorHuellaPendiente"; // credenciales a guardar cuando el ingreso salga bien
  const CLAVE_INTENTO = "fimacorHuellaIntento";     // el ingreso en curso salio de la huella
  const CONTROL_BIOMETRIA_ACTUAL = 1;                // se invalida si cambian las huellas registradas en el celular

  function leer(almacen, clave) { try { return almacen.getItem(clave); } catch (e) { return null; } }
  function guardar(almacen, clave, valor) { try { almacen.setItem(clave, valor); } catch (e) { /* sin almacenamiento */ } }
  function borrar(almacen, clave) { try { almacen.removeItem(clave); } catch (e) { /* sin almacenamiento */ } }

  function bio(metodo, opciones) { return cap.nativePromise("NativeBiometric", metodo, opciones || {}); }

  function disponible() {
    return bio("isAvailable", {}).then(function (r) { return !!(r && r.isAvailable); }).catch(function () { return false; });
  }

  function olvidarHuella() {
    borrar(window.localStorage, CLAVE_ACTIVA);
    return bio("deleteCredentials", { server: SERVIDOR }).catch(function () { /* ya no estaban */ });
  }

  function aviso(texto) {
    const mensajes = document.getElementById("mensajes");
    if (!mensajes) return;
    const div = document.createElement("div");
    div.className = "error-msg alert alert-danger small text-center py-2 px-2 mb-2";
    div.textContent = texto;
    mensajes.innerHTML = "";
    mensajes.appendChild(div);
  }

  // ---------- Menu: guardar la huella despues de un ingreso correcto ----------
  function terminarActivacion() {
    const crudo = leer(window.sessionStorage, CLAVE_PENDIENTE);
    if (!crudo) return;
    borrar(window.sessionStorage, CLAVE_PENDIENTE); // la contrasena no se queda en la pagina
    let datos;
    try { datos = JSON.parse(crudo); } catch (e) { return; }
    if (!datos || !datos.u || !datos.p) return;
    disponible().then(function (ok) {
      if (!ok) return;
      return bio("setCredentials", {
        username: datos.u, password: datos.p, server: SERVIDOR,
        accessControl: CONTROL_BIOMETRIA_ACTUAL,
        title: "Activar ingreso con huella", negativeButtonText: "Cancelar"
      }).then(function () {
        guardar(window.localStorage, CLAVE_ACTIVA, datos.u);
        alert("Listo: la próxima vez puedes entrar con tu huella.");
      }).catch(function () {
        alert("No se activó la huella. Puedes intentarlo de nuevo en el próximo ingreso.");
      });
    });
  }

  // ---------- Login ----------
  function iniciarLogin(formulario) {
    const parametros = new URLSearchParams(location.search);
    const fallo = parametros.has("error");

    // Un ingreso con huella que falla (contrasena cambiada, cuenta inactiva): se borra lo guardado.
    if (fallo && leer(window.sessionStorage, CLAVE_INTENTO)) olvidarHuella();
    borrar(window.sessionStorage, CLAVE_INTENTO);
    borrar(window.sessionStorage, CLAVE_PENDIENTE);

    const correoInput = document.getElementById("correo");
    const contrasenaInput = document.getElementById("contrasena");
    const botonLogin = document.getElementById("btnLogin");
    if (!correoInput || !contrasenaInput || !botonLogin) return;

    // Al enviar el formulario: ofrecer activar la huella si todavia no esta activa para ese correo.
    formulario.addEventListener("submit", function (evento) {
      if (evento.defaultPrevented) return;
      const correo = correoInput.value.trim();
      const contrasena = contrasenaInput.value;
      if (!correo || !contrasena || leer(window.sessionStorage, CLAVE_INTENTO)) return;
      if (leer(window.localStorage, CLAVE_ACTIVA) === correo) return;
      if (leer(window.localStorage, CLAVE_RECHAZO) === "1") return;
      if (!formulario.dataset.huellaDisponible) return;
      if (confirm("¿Quieres entrar con tu huella la próxima vez en este celular?")) {
        guardar(window.sessionStorage, CLAVE_PENDIENTE, JSON.stringify({ u: correo, p: contrasena }));
      } else {
        guardar(window.localStorage, CLAVE_RECHAZO, "1");
      }
    });

    disponible().then(function (ok) {
      if (!ok) return;
      formulario.dataset.huellaDisponible = "1";

      const activaPara = leer(window.localStorage, CLAVE_ACTIVA);
      const bloque = document.createElement("div");
      bloque.className = "mt-2 text-center";
      botonLogin.insertAdjacentElement("afterend", bloque);

      if (activaPara) {
        bloque.innerHTML =
          '<button type="button" id="btnHuella" class="btn btn-outline-success w-100 fw-bold py-2 d-flex align-items-center justify-content-center gap-2">' +
          '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
          '<path d="M12 11a4 4 0 0 0-4 4c0 2.2-.6 4.2-1.6 6"/><path d="M12 7a8 8 0 0 0-8 8c0 1.7-.2 3.4-.6 5"/>' +
          '<path d="M12 15c0 2.6-.5 4.8-1.4 6.7"/><path d="M16 15.5c0 1.8-.3 3.6-.9 5.3"/><path d="M20 15a8 8 0 0 0-3.5-6.6"/><path d="M5.5 9A8 8 0 0 1 12 4a8 8 0 0 1 3 .6"/></svg>' +
          'Entrar con huella</button>' +
          '<a href="#" id="quitarHuella" class="d-block small text-secondary mt-1">Quitar la huella de este celular</a>';
        document.getElementById("btnHuella").addEventListener("click", entrarConHuella);
        document.getElementById("quitarHuella").addEventListener("click", function (e) {
          e.preventDefault();
          olvidarHuella().then(function () { bloque.remove(); });
        });
        // Al abrir la app (no tras un error ni al cerrar sesion) se pide la huella sola.
        if (!parametros.has("error") && !parametros.has("salida") && !parametros.has("expirado")) entrarConHuella();
      } else if (leer(window.localStorage, CLAVE_RECHAZO) === "1") {
        bloque.innerHTML = '<a href="#" id="activarHuella" class="d-block small text-secondary">Activar ingreso con huella</a>';
        document.getElementById("activarHuella").addEventListener("click", function (e) {
          e.preventDefault();
          borrar(window.localStorage, CLAVE_RECHAZO);
          bloque.innerHTML = '<span class="small text-success">Listo: al iniciar sesión te preguntaremos de nuevo.</span>';
        });
      }
    });

    function entrarConHuella() {
      bio("getSecureCredentials", {
        server: SERVIDOR, title: "Ingresar a FIMACOR", subtitle: "Verifica tu identidad", negativeButtonText: "Cancelar"
      }).then(function (credenciales) {
        correoInput.value = credenciales.username;
        contrasenaInput.value = credenciales.password;
        guardar(window.sessionStorage, CLAVE_INTENTO, "1");
        formulario.requestSubmit ? formulario.requestSubmit() : formulario.submit();
      }).catch(function (error) {
        const codigo = error && (error.code || error.errorCode);
        const texto = String((error && error.message) || "").toLowerCase();
        if (String(codigo) === "21" || texto.indexOf("no protected credentials") >= 0) {
          // Cambiaron las huellas del celular o se borraron los datos: toca activarla otra vez.
          olvidarHuella().then(function () {
            const boton = document.getElementById("btnHuella");
            if (boton && boton.parentElement) boton.parentElement.remove();
            aviso("La huella ya no es válida en este celular. Inicia sesión con tu contraseña para activarla de nuevo.");
          });
          return;
        }
        // Cancelar o no reconocer la huella no es un error: se puede usar la contrasena.
      });
    }
  }

  function arrancar() {
    const formulario = document.getElementById("formLogin");
    if (formulario) iniciarLogin(formulario);
    else terminarActivacion();
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", arrancar);
  else arrancar();
})();
