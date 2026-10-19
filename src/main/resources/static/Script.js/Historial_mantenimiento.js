document.addEventListener("DOMContentLoaded", function () {
    const botonesVer = document.querySelectorAll(".btn-ver-historial");
    const filas = document.querySelectorAll(".fila-historial");
    const botonesCerrar = document.querySelectorAll(".modalmaquina-cerrar");
    const modales = document.querySelectorAll(".modalmaquina");

    function abrirModal(idModal) {
        const modal = document.getElementById(idModal);

        if (modal) {
            if (modal.parentElement !== document.body) document.body.appendChild(modal);
            modal.classList.add("mostrar");
            modal.classList.remove("d-none");
            const fila = document.querySelector('[data-modal="' + idModal + '"]');
            if (fila) {
                fila.scrollIntoView({ block: "center", behavior: "smooth" });
            }
        }
    }

    botonesVer.forEach(function (boton) {
        boton.addEventListener("click", function (evento) {
            evento.stopPropagation();
            abrirModal(boton.getAttribute("data-modal"));
        });
    });

    filas.forEach(function (fila) {
        fila.addEventListener("click", function () {
            abrirModal(fila.getAttribute("data-modal"));
        });
    });

    botonesCerrar.forEach(function (boton) {
        boton.addEventListener("click", function () {
            const modal = boton.closest(".modalmaquina");

            if (modal) {
                modal.classList.remove("mostrar");
                modal.classList.add("d-none");
            }
        });
    });

    modales.forEach(function (modal) {
        modal.addEventListener("click", function (evento) {
            const contenido = modal.querySelector(".modal-content");
            if (contenido && !contenido.contains(evento.target)) {
                modal.classList.remove("mostrar");
                modal.classList.add("d-none");
            }
        });
    });

    document.addEventListener("keydown", function (evento) {
        if (evento.key === "Escape") {
            modales.forEach(function (modal) {
                modal.classList.remove("mostrar");
                modal.classList.add("d-none");
            });
        }
    });
    const buscadorHistorial = document.getElementById("buscarHistorial");
    if (buscadorHistorial) {
        buscadorHistorial.addEventListener("input", function () {
            const termino = buscadorHistorial.value.toLowerCase().trim();
            document.querySelectorAll("#tablaHistorial tbody tr").forEach(function (fila) {
                fila.classList.toggle("d-none", !fila.textContent.toLowerCase().includes(termino));
            });
        });
    }

    const parametros = new URLSearchParams(window.location.search);
    const historialId = parametros.get("historial");
    if (historialId) {
        const idModal = "modalHistorial" + historialId;
        abrirModal(idModal);

        if (parametros.get("accion") === "confirmar") {
            const formulario = document.querySelector("#" + idModal + " .form-confirmar-reparacion");
            if (formulario) {
                setTimeout(function () {
                    formulario.scrollIntoView({ block: "center", behavior: "smooth" });
                    const observacion = formulario.querySelector("textarea[name='observacionReparacion']");
                    if (observacion) {
                        observacion.focus();
                    }
                }, 250);
            }
        }
    }
});


