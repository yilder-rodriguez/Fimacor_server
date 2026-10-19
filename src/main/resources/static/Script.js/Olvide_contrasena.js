/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/JavaScript.js to edit this template
 */


document.addEventListener("DOMContentLoaded", () => {
    const btnRecuperar = document.getElementById("btnRecuperar");
    const formRecuperar = document.getElementById("formRecuperar");
    const modalRecuperar = document.getElementById("modalRecuperar");
    const btnVolverLoginRecuperar = document.getElementById("btnVolverLoginRecuperar");
    const modalVolverLoginRecuperar = document.getElementById("modalVolverLoginRecuperar");

    if (btnRecuperar && formRecuperar && modalRecuperar) {
        btnRecuperar.addEventListener("click", function () {
            if (!formRecuperar.checkValidity()) {
                formRecuperar.reportValidity();
                return;
            }

            mostrarCargaRecuperacion(
                modalRecuperar,
                "Enviando código",
                "Validando correo y preparando la verificacion segura..."
            );

            setTimeout(function () {
                formRecuperar.submit();
            }, 2000);
        });
    }

    if (btnVolverLoginRecuperar && modalVolverLoginRecuperar) {
        btnVolverLoginRecuperar.addEventListener("click", function (event) {
            event.preventDefault();
            mostrarCargaRecuperacion(
                modalVolverLoginRecuperar,
                "Volviendo al inicio de sesión",
                "Preparando el acceso seguro..."
            );

            setTimeout(function () {
                window.location.href = btnVolverLoginRecuperar.href;
            }, 1200);
        });
    }
});

function mostrarCargaRecuperacion(modal, titulo, mensaje) {
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


