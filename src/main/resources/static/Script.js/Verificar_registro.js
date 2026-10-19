const p = new URLSearchParams(window.location.search);

    // Campos del form de verificación
    document.getElementById('correo').value      = p.get('correo')     || '';
    document.getElementById('nombre').value      = p.get('nombre')     || '';
    document.getElementById('apellido').value    = p.get('apellido')   || '';
    document.getElementById('documento').value   = p.get('documento')  || '';
    document.getElementById('telefono').value    = p.get('telefono')   || '';
    document.getElementById('contrasena').value  = p.get('contrasena') || '';
    document.getElementById('rol').value         = p.get('rol')        || '';
    document.getElementById('tipoDoc').value     = p.get('tipoDoc')    || '';

  
    document.getElementById('correoReenviar').value     = p.get('correo')     || '';
    document.getElementById('nombreReenviar').value     = p.get('nombre')     || '';
    document.getElementById('apellidoReenviar').value   = p.get('apellido')   || '';
    document.getElementById('documentoReenviar').value  = p.get('documento')  || '';
    document.getElementById('telefonoReenviar').value   = p.get('telefono')   || '';
    document.getElementById('contrasenaReenviar').value = p.get('contrasena') || '';
    document.getElementById('rolReenviar').value        = p.get('rol')        || '';
    document.getElementById('tipoDocReenviar').value    = p.get('tipoDoc')    || '';

    // Mensajes
    const mensajes = document.getElementById('mensajes');
    if (p.get('error') === '1') { 
      mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-3"> X Error al registrar. Intente de nuevo.</p>';
    }
    if (p.get('error') === '2') {
      mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-3"> X Código incorrecto o expirado. Intente de nuevo.</p>';
    }
    if (p.get('error') === 'sesion') {
      mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-3"> X Sesion expirada. Por favor regístrese de nuevo.</p>';
    }
    if (p.get('envio') === 'error') {
      mensajes.innerHTML = '<p class="error-msg alert alert-danger small text-center py-2 px-2 mb-3">No fue posible enviar el codigo. Revisa la configuracion de correo e intenta reenviarlo.</p>';
    }
    if (p.get('reenvio') === '1') {
      mensajes.innerHTML = '<p class="success-msg alert alert-success small text-center py-2 px-2 mb-3"> Código reenviado a tu correo.</p>';
    }




