// FIMACOR · Foto de perfil (modal de perfil del Inicio).
// Al elegir una imagen se recorta al centro en cuadrado y se reduce a 256 px en el propio celular o computador,
// asi se sube rapido aunque la foto original pese varios MB. El servidor vuelve a validarla antes de guardarla.
(function () {
  const LADO = 256;
  const boton = document.getElementById("btnCambiarFoto");
  const entrada = document.getElementById("inputFotoPerfil");
  const formulario = document.getElementById("formFotoPerfil");
  if (!boton || !entrada || !formulario) return;

  // Tras guardar o quitar la foto el servidor recarga el Inicio: se vuelve a abrir el perfil para ver el resultado.
  const parametros = new URLSearchParams(location.search);
  if (parametros.has("fotoPerfil") || parametros.has("fotoPerfilError")) {
    const modal = document.getElementById("perfilModal");
    if (modal) modal.classList.remove("d-none");
  }

  boton.addEventListener("click", function () { entrada.click(); });

  entrada.addEventListener("change", function () {
    const archivo = entrada.files && entrada.files[0];
    if (!archivo) return;
    if (archivo.type && archivo.type.indexOf("image/") !== 0) {
      alert("Elige una imagen (JPG, PNG o WebP).");
      entrada.value = "";
      return;
    }
    reducir(archivo).then(function (blob) {
      const envio = new DataTransfer();
      envio.items.add(new File([blob], "foto.jpg", { type: "image/jpeg" }));
      entrada.files = envio.files;
      formulario.submit();
    }).catch(function () {
      alert("No se pudo leer esa imagen. Prueba con otra (JPG, PNG o WebP).");
      entrada.value = "";
    });
  });

  function reducir(archivo) {
    return new Promise(function (resolver, rechazar) {
      const url = URL.createObjectURL(archivo);
      const imagen = new Image();
      imagen.onload = function () {
        const lado = Math.min(imagen.naturalWidth, imagen.naturalHeight);
        const destino = Math.min(LADO, lado);
        const lienzo = document.createElement("canvas");
        lienzo.width = destino;
        lienzo.height = destino;
        const ctx = lienzo.getContext("2d");
        ctx.fillStyle = "#fff";
        ctx.fillRect(0, 0, destino, destino);
        ctx.imageSmoothingQuality = "high";
        ctx.drawImage(imagen, (imagen.naturalWidth - lado) / 2, (imagen.naturalHeight - lado) / 2, lado, lado, 0, 0, destino, destino);
        URL.revokeObjectURL(url);
        lienzo.toBlob(function (blob) { blob ? resolver(blob) : rechazar(new Error("sin imagen")); }, "image/jpeg", 0.85);
      };
      imagen.onerror = function () { URL.revokeObjectURL(url); rechazar(new Error("no se pudo leer")); };
      imagen.src = url;
    });
  }
})();
