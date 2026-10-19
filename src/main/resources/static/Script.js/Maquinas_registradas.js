document.addEventListener("DOMContentLoaded", function () {
    const filas = document.querySelectorAll(".fila-maquina, .fila-usuario");
    const botonesCerrar = document.querySelectorAll(".modalmaquina-cerrar");
    const modales = document.querySelectorAll(".modalmaquina");
    const buscador = document.getElementById("buscarMaquina");
    const buscadorUsuario = document.getElementById("buscarUsuario");
    const filasAdmin = document.querySelectorAll("[data-admin-row]");
    const selectorTodosAdmin = document.querySelector("[data-admin-select-all]");
    const botonActivarEliminar = document.querySelector("[data-admin-start-delete]");
    const botonCancelarEliminar = document.querySelector("[data-admin-cancel-delete]");
    const botonEliminarSeleccionados = document.querySelector("[data-admin-delete-selected]");
    const botonActivarEditar = document.querySelector("[data-admin-start-edit]");
    const botonCancelarEditar = document.querySelector("[data-admin-cancel-edit]");
    const botonConfirmarEditar = document.querySelector("[data-admin-confirm-edit]");

    // Visibilidad con utilidades de Bootstrap: los modales llevan "modal d-block d-none".
    function mostrarModalMaquina(modal) {
        modal.classList.add("mostrar");
        modal.classList.remove("d-none");
    }

    function ocultarModalMaquina(modal) {
        modal.classList.remove("mostrar", "modal-usuario-editar");
        modal.classList.add("d-none");
        sincronizarTitulosUsuario(modal);
    }

    function sincronizarTitulosUsuario(modal) {
        const editar = modal.classList.contains("modal-usuario-editar");
        modal.querySelectorAll(".titulo-consulta-usuario").forEach(function (titulo) { titulo.classList.toggle("d-none", editar); });
        modal.querySelectorAll(".titulo-editar-usuario").forEach(function (titulo) { titulo.classList.toggle("d-none", !editar); });
    }

    // Reemplaza las reglas de body.admin-seleccion-activa / body.admin-edicion-activa.
    function sincronizarControlesAdmin() {
        const algunModo = modoEliminarAdmin || modoEditarAdmin;
        document.querySelectorAll(".admin-delete-mode-control").forEach(function (control) { control.classList.toggle("d-none", !modoEliminarAdmin); });
        document.querySelectorAll(".admin-edit-mode-control").forEach(function (control) { control.classList.toggle("d-none", !modoEditarAdmin); });
        document.querySelectorAll("[data-admin-start-edit], [data-admin-start-delete]").forEach(function (boton) { boton.classList.toggle("d-none", algunModo); });
        document.querySelectorAll(".tabla-admin-masiva .admin-col-check").forEach(function (celda) { celda.classList.toggle("d-none", !algunModo); });
    }

    // Reemplaza las reglas de .form-confirmar-reparacion(.reparacion-abierta).
    function sincronizarFormReparacion(formulario) {
        if (!formulario || !formulario.classList.contains("form-confirmar-reparacion")) return;
        const abierto = formulario.classList.contains("reparacion-abierta");
        formulario.querySelectorAll("textarea, .btnmantenimientospreventivos").forEach(function (elemento) {
            elemento.classList.toggle("d-none", !abierto);
        });
    }

    function configurarFiltroDeModelos(origenMarca, destinoModelo) {
        if (!origenMarca || !destinoModelo) return;

        const opciones = Array.from(destinoModelo.options).map(function (opcion) {
            return {
                opcion: opcion,
                marca: opcion.getAttribute("data-marca")
            };
        });

        function filtrarModelos() {
            const marcaSeleccionada = origenMarca.value;
            const modeloSeleccionado = destinoModelo.value;
            let modeloSeleccionadoDisponible = false;

            opciones.forEach(function (item) {
                if (!item.opcion.value) return;

                const disponible = marcaSeleccionada !== "" && item.marca === marcaSeleccionada;
                item.opcion.hidden = !disponible;
                item.opcion.disabled = !disponible;

                if (disponible && item.opcion.value === modeloSeleccionado) {
                    modeloSeleccionadoDisponible = true;
                }
            });

            if (!modeloSeleccionadoDisponible) destinoModelo.value = "";
            destinoModelo.disabled = marcaSeleccionada === "";
        }

        origenMarca.addEventListener("change", filtrarModelos);
        filtrarModelos();
    }

    document.querySelectorAll("[data-model-brand-source]").forEach(function (origenMarca) {
        const contenedor = origenMarca.closest("form, .modalmaquina");
        const destinoModelo = contenedor
            ? contenedor.querySelector("[data-model-brand-target]")
            : document.querySelector("[data-model-brand-target]");
        configurarFiltroDeModelos(origenMarca, destinoModelo);
    });

    // Cascada de ubicación para formularios de máquina: sólo muestra los
    // catálogos cuya sede coincide con la sede elegida.
    document.querySelectorAll('form').forEach(function (formulario) {
        const sede = formulario.querySelector('select[name="sede"]');
        const area = formulario.querySelector('select[name="area"]');
        const ambiente = formulario.querySelector('select[name="ambiente"]');
        if (!sede || !area || !ambiente || sede.hasAttribute('data-novedad-sede')) return;
        const filtrarUbicacion = function (select, sedeSeleccionada) {
            let disponibles = 0;
            Array.from(select.options).forEach(function (opcion, indice) {
                if (indice === 0 && !opcion.value) return;
                const visible = sedeSeleccionada !== '' && opcion.dataset.sede === sedeSeleccionada;
                opcion.hidden = !visible;
                opcion.disabled = !visible;
                if (visible) disponibles += 1;
            });
            if (select.selectedOptions.length && select.selectedOptions[0].disabled) select.value = '';
            select.disabled = sedeSeleccionada === '' || disponibles === 0;
        };
        const actualizarUbicacion = function () {
            const seleccion = sede.value;
            filtrarUbicacion(area, seleccion);
            filtrarUbicacion(ambiente, seleccion);
        };
        sede.addEventListener('change', actualizarUbicacion);
        actualizarUbicacion();
    });
    const contadorVisibleAdmin = document.querySelector("[data-admin-visible]");
    const contadorSeleccionadosAdmin = document.querySelector("[data-admin-selected]");
    const tamanoPaginaAdmin = 15;
    let paginaActualAdmin = 1;
    let paginadorAdmin = null;
    let modoEliminarAdmin = false;
    let modoEditarAdmin = false;
    function prepararModalMaquinaSeguro(modal, modo) {
        if (!modal) return;
        modal.classList.toggle("modal-modo-editar", modo === "editar");
        modal.classList.toggle("modal-modo-consulta", modo !== "editar");
        modal.classList.toggle("modal-usuario-editar", modo === "editar");
        const titulo = modal.querySelector("[data-modal-machine-title]");
        if (titulo) titulo.textContent = modo === "editar" ? "Editar máquina" : "Información de máquina";
        // Roles y permisos abren directo con su formulario (no tienen modo "solo consulta"):
        // ocultarlo dejaba esas pantallas sin forma de editar ni de eliminar.
        const ocultaFormulario = modal.matches(".modal-maquina-edicion, [id^='modalUsuario']");
        modal.querySelectorAll(".rolespermisos-modal-form").forEach(function (formulario) {
            formulario.hidden = ocultaFormulario && modo !== "editar";
        });
        sincronizarTitulosUsuario(modal);
    }

    // El modal de Bootstrap (position-fixed + modal-dialog-centered) ya queda centrado en la
    // ventana; solo se mueve al body para que ningun contenedor con overflow lo recorte.
    function forzarModalCentrado(modal) {
        if (!modal) return;
        if (modal.parentElement !== document.body) document.body.appendChild(modal);
    }

    function abrirModalMaquinaSeguro(idModal, modo) {
        const modal = idModal ? document.getElementById(idModal) : null;
        if (!modal) return false;
        prepararModalMaquinaSeguro(modal, modo || "consulta");
        forzarModalCentrado(modal);
        mostrarModalMaquina(modal);
        document.documentElement.classList.add("modalmaquina-abierto");
        document.body.classList.add("modalmaquina-abierto", "overflow-hidden");
        const contenido = modal.querySelector(".modalmaquina-contenido");
        if (contenido) {
            contenido.scrollTop = 0;
        }
        return false;
    }

    function abrirModalMaquina(modal, modo) {
        if (modal) abrirModalMaquinaSeguro(modal.id, modo);
    }

    window.fimacorAbrirModalMaquina = function (idModal, modo) {
        const fila = idModal ? document.querySelector('[data-modal="' + idModal + '"]') : null;
        if ((modoEliminarAdmin || modoEditarAdmin) && typeof seleccionarFilaAdminDesdeClick === "function") {
            seleccionarFilaAdminDesdeClick(fila);
            return false;
        }
        return abrirModalMaquinaSeguro(idModal, modo || "consulta");
    };

    document.addEventListener("click", function (evento) {
        const botonCerrar = evento.target.closest(".modalmaquina-cerrar");
        if (botonCerrar) {
            const modal = botonCerrar.closest(".modalmaquina");
            if (modal) {
                ocultarModalMaquina(modal);
                modal.classList.remove("modal-modo-editar");
                modal.classList.add("modal-modo-consulta");
                document.documentElement.classList.remove("modalmaquina-abierto");
                document.body.classList.remove("modalmaquina-abierto", "overflow-hidden");
            }
            evento.preventDefault();
            evento.stopPropagation();
            return;
        }

        if (evento.target.closest("[data-admin-start-edit]")) {
            cambiarModoEditarAdmin(true);
            evento.stopPropagation();
            return;
        }
        if (evento.target.closest("[data-admin-cancel-edit]")) {
            cambiarModoEditarAdmin(false);
            evento.stopPropagation();
            return;
        }
        if (evento.target.closest("[data-admin-start-delete]")) {
            cambiarModoEliminarAdmin(true);
            evento.stopPropagation();
            return;
        }
        if (evento.target.closest("[data-admin-cancel-delete]")) {
            cambiarModoEliminarAdmin(false);
            evento.stopPropagation();
            return;
        }

        const fila = evento.target.closest(".fila-maquina, .fila-usuario");
        if (!fila || evento.target.closest(".no-abrir-modal") || evento.target.closest(".modalmaquina")) return;
        const idModal = fila.getAttribute("data-modal");
        if (!idModal) return;
        if ((modoEliminarAdmin || modoEditarAdmin) && typeof seleccionarFilaAdminDesdeClick === "function") {
            seleccionarFilaAdminDesdeClick(fila);
            evento.preventDefault();
            evento.stopPropagation();
            return;
        }
        abrirModalMaquinaSeguro(idModal, "consulta");
        evento.preventDefault();
        evento.stopPropagation();
    }, true);

    document.addEventListener("change", function (evento) {
        const check = evento.target.closest(".admin-row-check");
        if (!check) return;
        if (!modoEliminarAdmin && !modoEditarAdmin) {
            check.checked = false;
            actualizarResumenAdmin();
            return;
        }
        if (modoEditarAdmin && check.checked) {
            document.querySelectorAll(".admin-row-check").forEach(function (otroCheck) {
                if (otroCheck !== check) otroCheck.checked = false;
            });
        }
        actualizarResumenAdmin();
    }, true);
    function mostrarConfirmacionFimacor(opciones) {
        return new Promise(function (resolve) {
            const existente = document.querySelector(".fimacor-confirmacion");
            if (existente) {
                existente.remove();
            }

            const capa = document.createElement("div");
            capa.className = "fimacor-confirmacion modal d-block bg-dark bg-opacity-75";
            capa.innerHTML = `
                <div class="modal-dialog modal-dialog-centered">
                    <div class="fimacor-confirmacion-card modal-content border-0 rounded-4 shadow-lg text-center p-4" role="dialog" aria-modal="true">
                        <div class="fimacor-confirmacion-icono d-flex align-items-center justify-content-center rounded-circle text-bg-warning fw-bold fs-3 mx-auto mb-3 px-3">${opciones.icono || "!"}</div>
                        <h3 class="h5 fw-bold">${opciones.titulo || "Confirmar acción"}</h3>
                        <p class="text-secondary">${opciones.mensaje || ""}</p>
                        <div class="fimacor-confirmacion-botones d-flex justify-content-center gap-2">
                            <button type="button" class="fimacor-confirmacion-cancelar btn btn-outline-secondary">${opciones.cancelar || "Cancelar"}</button>
                            <button type="button" class="fimacor-confirmacion-aceptar btn btn-success">${opciones.aceptar || "Aceptar"}</button>
                        </div>
                    </div>
                </div>
            `;

            document.body.appendChild(capa);
            const aceptar = capa.querySelector(".fimacor-confirmacion-aceptar");
            const cancelar = capa.querySelector(".fimacor-confirmacion-cancelar");

            function cerrar(valor) {
                capa.classList.add("cerrando");
                setTimeout(function () {
                    capa.remove();
                    resolve(valor);
                }, 140);
            }

            aceptar.addEventListener("click", function () { cerrar(true); });
            cancelar.addEventListener("click", function () { cerrar(false); });
            capa.addEventListener("click", function (evento) {
                if (!capa.querySelector(".modal-content").contains(evento.target)) {
                    cerrar(false);
                }
            });

            setTimeout(function () {
                aceptar.focus();
            }, 80);
        });
    }

    document.querySelectorAll("form[onsubmit*='confirm']").forEach(function (formulario) {
        const textoSubmit = formulario.getAttribute("onsubmit") || "";
        const coincidencia = textoSubmit.match(/confirm\(['"](.+?)['"]\)/);
        const mensaje = coincidencia ? coincidencia[1] : "Confirma si deseas continuar con esta acción.";
        formulario.removeAttribute("onsubmit");

        formulario.addEventListener("submit", async function (evento) {
            if (formulario.dataset.fimacorConfirmado === "true") {
                return;
            }

            evento.preventDefault();
            const confirmado = await mostrarConfirmacionFimacor({
                icono: "!",
                titulo: "Confirmar operación",
                mensaje: mensaje,
                aceptar: "Continuar",
                cancelar: "Cancelar"
            });

            if (confirmado) {
                formulario.dataset.fimacorConfirmado = "true";
                formulario.submit();
            }
        });
    });

    filas.forEach(function (fila) {
        fila.addEventListener("click", function (evento) {
            if (evento.target.closest(".no-abrir-modal")) {
                return;
            }

            if (seleccionarFilaAdminDesdeClick(fila)) {
                evento.preventDefault();
                evento.stopPropagation();
                return;
            }

            const idModal = fila.getAttribute("data-modal");
            const modal = document.getElementById(idModal);
            abrirModalMaquina(modal, "consulta");
            evento.stopPropagation();
        });
    });

    document.addEventListener("click", function (evento) {
        const fila = evento.target.closest(".fila-maquina, .fila-usuario");
        if (!fila || evento.target.closest(".no-abrir-modal") || evento.target.closest(".modalmaquina")) {
            return;
        }
        if (seleccionarFilaAdminDesdeClick(fila)) {
            evento.preventDefault();
            return;
        }
        const idModal = fila.getAttribute("data-modal");
        const modal = idModal ? document.getElementById(idModal) : null;
        if (modal && modal.classList.contains("d-none")) {
            abrirModalMaquina(modal, "consulta");
        }
    });

    botonesCerrar.forEach(function (boton) {
        boton.addEventListener("click", function () {
            const modal = boton.closest(".modalmaquina");

            if (modal) {
                ocultarModalMaquina(modal);
                document.documentElement.classList.remove("modalmaquina-abierto");
                document.body.classList.remove("modalmaquina-abierto", "overflow-hidden");
            }
        });
    });

    modales.forEach(function (modal) {
        modal.addEventListener("click", function (evento) {
            const contenido = modal.querySelector(".modal-content");
            if (contenido && !contenido.contains(evento.target)) {
                ocultarModalMaquina(modal);
                document.documentElement.classList.remove("modalmaquina-abierto");
                document.body.classList.remove("modalmaquina-abierto", "overflow-hidden");
            }
        });
    });

    document.addEventListener("keydown", function (evento) {
        if (evento.key === "Escape") {
            modales.forEach(ocultarModalMaquina);
            document.documentElement.classList.remove("modalmaquina-abierto");
            document.body.classList.remove("modalmaquina-abierto", "overflow-hidden");
        }
    });

    document.querySelectorAll('[data-repair-toggle="true"]').forEach(function (boton) {
        const formInicial = boton.closest('[data-repair-form="true"]');
        if (formInicial) {
            boton.textContent = "Hacer reparación inmediata";
            const etiqueta = formInicial.querySelector("label");
            const panel = formInicial.querySelector(".reparacion-formulario-inmediato");
            const observacion = formInicial.querySelector("textarea");
            const confirmar = formInicial.querySelector(".btnmantenimientospreventivos");

            if (etiqueta) {
                etiqueta.remove();
            }

            if (observacion) {
                observacion.placeholder = "Describe qué reparaste, qué prueba realizaste y confirma que queda en servicio";
            }

            if (confirmar) {
                confirmar.textContent = "Poner en servicio";
            }

            if (panel) {
                panel.hidden = true;
            }
            sincronizarFormReparacion(formInicial);
        }

        if (boton.hasAttribute("onclick")) {
            return;
        }

        boton.addEventListener("click", function () {
            const formReparacion = boton.closest('[data-repair-form="true"]');
            if (!formReparacion) {
                return;
            }

            const abierto = formReparacion.classList.toggle("reparacion-abierta");
            boton.setAttribute("aria-expanded", abierto ? "true" : "false");
            sincronizarFormReparacion(formReparacion);

            const observacion = formReparacion.querySelector("textarea");
            const confirmar = formReparacion.querySelector(".btnmantenimientospreventivos");
            const panel = formReparacion.querySelector(".reparacion-formulario-inmediato");

            if (panel) {
                panel.hidden = !abierto;
            }

            if (abierto) {
                if (observacion) {
                    setTimeout(function () {
                        observacion.focus();
                    }, 120);
                }
            }
        });
    });

    document.querySelectorAll('[data-repair-form="true"] .btnmantenimientospreventivos').forEach(function (boton) {
        boton.textContent = "Poner en servicio";
    });

    function filasFiltrables() {
        return filasAdmin.length ? filasAdmin : filas;
    }

    function estaVisible(fila) {
        return !fila.classList.contains("d-none");
    }

    function pasaFiltro(fila) {
        return fila.dataset.adminFilterHidden !== "true";
    }

    function filasFiltradasAdmin() {
        return Array.from(filasAdmin).filter(pasaFiltro);
    }

    function asegurarPaginadorAdmin() {
        if (!filasAdmin.length) {
            return null;
        }

        if (paginadorAdmin) {
            return paginadorAdmin;
        }

        const tabla = filasAdmin[0].closest("table");
        if (!tabla) {
            return null;
        }

        paginadorAdmin = document.createElement("div");
        paginadorAdmin.className = "fimacor-paginador-tabla d-flex justify-content-center align-items-center gap-3 my-3";
        paginadorAdmin.innerHTML = `
            <button type="button" class="btnmaquinasregistradas btn btn-outline-success btn-sm" data-page-prev>Anterior</button>
            <span class="small text-secondary" data-page-info></span>
            <button type="button" class="btnmaquinasregistradas btn btn-outline-success btn-sm" data-page-next>Siguiente</button>
        `;

        const contenedorTabla = tabla.closest(".tabla-contenedormaquinasregistradas, .table-responsivehistorialmantenimiento") || tabla.parentElement;
        contenedorTabla.insertAdjacentElement("afterend", paginadorAdmin);

        paginadorAdmin.querySelector("[data-page-prev]").addEventListener("click", function () {
            paginaActualAdmin = Math.max(1, paginaActualAdmin - 1);
            aplicarPaginacionAdmin();
        });

        paginadorAdmin.querySelector("[data-page-next]").addEventListener("click", function () {
            const totalPaginas = Math.max(1, Math.ceil(filasFiltradasAdmin().length / tamanoPaginaAdmin));
            paginaActualAdmin = Math.min(totalPaginas, paginaActualAdmin + 1);
            aplicarPaginacionAdmin();
        });

        return paginadorAdmin;
    }

    function aplicarPaginacionAdmin() {
        if (!filasAdmin.length) {
            return;
        }

        const filtradas = filasFiltradasAdmin();
        const totalPaginas = Math.max(1, Math.ceil(filtradas.length / tamanoPaginaAdmin));
        paginaActualAdmin = Math.min(Math.max(1, paginaActualAdmin), totalPaginas);
        const inicio = (paginaActualAdmin - 1) * tamanoPaginaAdmin;
        const fin = inicio + tamanoPaginaAdmin;

        filasAdmin.forEach(function (fila) {
            const indiceFiltrado = filtradas.indexOf(fila);
            const visible = indiceFiltrado >= inicio && indiceFiltrado < fin;
            fila.classList.toggle("d-none", !visible);
            if (!visible) {
                const check = fila.querySelector(".admin-row-check");
                if (check) {
                    check.checked = false;
                }
            }
        });

        const paginador = asegurarPaginadorAdmin();
        if (paginador) {
            const info = paginador.querySelector("[data-page-info]");
            const anterior = paginador.querySelector("[data-page-prev]");
            const siguiente = paginador.querySelector("[data-page-next]");
            if (info) {
                info.textContent = `Página ${paginaActualAdmin} de ${totalPaginas} - ${filtradas.length} registros`;
            }
            if (anterior) {
                anterior.disabled = paginaActualAdmin <= 1;
            }
            if (siguiente) {
                siguiente.disabled = paginaActualAdmin >= totalPaginas;
            }
            paginador.hidden = filtradas.length <= tamanoPaginaAdmin;
            paginador.classList.toggle("d-none", filtradas.length <= tamanoPaginaAdmin);
        }
    }

    function limpiarSeleccionAdmin() {
        filasAdmin.forEach(function (fila) {
            const check = fila.querySelector(".admin-row-check");
            if (check) {
                check.checked = false;
            }
        });

        if (selectorTodosAdmin) {
            selectorTodosAdmin.checked = false;
            selectorTodosAdmin.indeterminate = false;
        }
    }

    function cambiarModoEliminarAdmin(activo) {
        if (!filasAdmin.length) {
            return;
        }

        modoEliminarAdmin = activo;
        if (activo) {
            modoEditarAdmin = false;
            document.body.classList.remove("admin-edicion-activa");
            limpiarSeleccionAdmin();
        }

        document.body.classList.toggle("admin-seleccion-activa", activo);

        if (!activo) {
            limpiarSeleccionAdmin();
        }

        sincronizarControlesAdmin();
        actualizarResumenAdmin();
    }

    function cambiarModoEditarAdmin(activo) {
        if (!filasAdmin.length) {
            return;
        }

        modoEditarAdmin = activo;
        if (activo) {
            modoEliminarAdmin = false;
            document.body.classList.remove("admin-seleccion-activa");
        }

        document.body.classList.toggle("admin-edicion-activa", activo);
        limpiarSeleccionAdmin();
        sincronizarControlesAdmin();
        actualizarResumenAdmin();
    }

    function seleccionarFilaAdminDesdeClick(fila) {
        const check = fila ? fila.querySelector(".admin-row-check") : null;
        if (!check || (!modoEliminarAdmin && !modoEditarAdmin)) {
            return false;
        }

        if (modoEditarAdmin) {
            document.querySelectorAll(".admin-row-check").forEach(function (otroCheck) {
                otroCheck.checked = otroCheck === check;
            });
        } else {
            check.checked = !check.checked;
        }

        actualizarResumenAdmin();
        return true;
    }
    function actualizarResumenAdmin() {
        if (!filasAdmin.length) {
            return;
        }

        let visibles = 0;
        let seleccionadas = 0;
        let numeroVisible = 1;

        filasAdmin.forEach(function (fila) {
            const visible = estaVisible(fila);
            const check = fila.querySelector(".admin-row-check");
            const numero = fila.querySelector("[data-admin-number]");

            if (visible) {
                visibles += 1;
                if (numero) {
                    numero.textContent = numeroVisible;
                    numeroVisible += 1;
                }
            } else if (check) {
                check.checked = false;
            }

            if (check && check.checked) {
                seleccionadas += 1;
            }
        });

        if (contadorVisibleAdmin) {
            contadorVisibleAdmin.textContent = visibles;
        }

        if (contadorSeleccionadosAdmin) {
            contadorSeleccionadosAdmin.textContent = seleccionadas;
        }

        if (botonEliminarSeleccionados) {
            botonEliminarSeleccionados.disabled = seleccionadas === 0;
        }

        if (botonConfirmarEditar) {
            botonConfirmarEditar.disabled = seleccionadas !== 1;
        }

        if (selectorTodosAdmin) {
            const visiblesChecks = Array.from(filasAdmin)
                .filter(estaVisible)
                .map(function (fila) {
                    return fila.querySelector(".admin-row-check");
                })
                .filter(Boolean);
            const todosSeleccionados = visiblesChecks.length > 0
                    && visiblesChecks.every(function (check) { return check.checked; });

            selectorTodosAdmin.checked = todosSeleccionados;
            selectorTodosAdmin.indeterminate = seleccionadas > 0 && !todosSeleccionados;
        }
    }

    function filtrarFilas(input) {
        const texto = input.value.toLowerCase();

        filasFiltrables().forEach(function (fila) {
            const contenido = fila.textContent.toLowerCase();
            fila.dataset.adminFilterHidden = contenido.includes(texto) ? "false" : "true";
        });

        paginaActualAdmin = 1;
        aplicarPaginacionAdmin();
        actualizarResumenAdmin();
    }

    if (buscador) {
        buscador.addEventListener("input", function () {
            filtrarFilas(buscador);
        });
    }

    if (buscadorUsuario) {
        buscadorUsuario.addEventListener("input", function () {
            const texto = buscadorUsuario.value.toLowerCase();

            filas.forEach(function (fila) {
                const contenido = fila.textContent.toLowerCase();
                fila.classList.toggle("d-none", !contenido.includes(texto));
            });
        });
    }

    if (botonActivarEliminar) {
        botonActivarEliminar.addEventListener("click", function () {
            cambiarModoEliminarAdmin(true);
        });
    }

    if (botonCancelarEliminar) {
        botonCancelarEliminar.addEventListener("click", function () {
            cambiarModoEliminarAdmin(false);
        });
    }

    if (botonActivarEditar) {
        botonActivarEditar.addEventListener("click", function () {
            cambiarModoEditarAdmin(true);
        });
    }

    if (botonCancelarEditar) {
        botonCancelarEditar.addEventListener("click", function () {
            cambiarModoEditarAdmin(false);
        });
    }

    if (botonConfirmarEditar) {
        botonConfirmarEditar.addEventListener("click", async function () {
            const checkSeleccionado = document.querySelector(".admin-row-check:checked");
            if (!modoEditarAdmin || !checkSeleccionado) {
                await mostrarConfirmacionFimacor({
                    icono: "i",
                    titulo: "Seleccion pendiente",
                    mensaje: "Para editar, primero selecciona una máquina de la tabla.",
                    aceptar: "Entendido",
                    cancelar: "Cerrar"
                });
                return;
            }

            const filaSeleccionada = checkSeleccionado.closest("[data-admin-row]");
            const idModal = filaSeleccionada ? filaSeleccionada.getAttribute("data-modal") : "";

            cambiarModoEditarAdmin(false);
            abrirModalMaquinaSeguro(idModal, "editar");
        });
    }

    if (selectorTodosAdmin) {
        selectorTodosAdmin.addEventListener("change", function () {
            if (!modoEliminarAdmin) {
                selectorTodosAdmin.checked = false;
                return;
            }

            filasAdmin.forEach(function (fila) {
                const check = fila.querySelector(".admin-row-check");
                if (check && estaVisible(fila)) {
                    check.checked = selectorTodosAdmin.checked;
                }
            });
            actualizarResumenAdmin();
        });
    }

    document.querySelectorAll(".admin-row-check").forEach(function (check) {
        check.addEventListener("click", function (evento) {
            evento.stopPropagation();
        });
        check.addEventListener("change", function () {
            if (!modoEliminarAdmin && !modoEditarAdmin) {
                check.checked = false;
                return;
            }

            if (modoEditarAdmin && check.checked) {
                document.querySelectorAll(".admin-row-check").forEach(function (otroCheck) {
                    if (otroCheck !== check) {
                        otroCheck.checked = false;
                    }
                });
            }

            actualizarResumenAdmin();
        });
    });

    document.querySelectorAll(".admin-bulk-form").forEach(function (formulario) {
        formulario.addEventListener("submit", function (evento) {
            if (!modoEliminarAdmin) {
                evento.preventDefault();
                cambiarModoEliminarAdmin(true);
                return;
            }

            const seleccionadas = document.querySelectorAll(`.admin-row-check[form="${formulario.id}"]:checked`).length;
            if (seleccionadas === 0) {
                evento.preventDefault();
                alert("Selecciona al menos un registro para eliminar.");
            }
        });
    });

    aplicarPaginacionAdmin();
    cambiarModoEliminarAdmin(false);
    cambiarModoEditarAdmin(false);
    sincronizarControlesAdmin();

    const parametros = new URLSearchParams(window.location.search);
    const maquinaObjetivo = parametros.get("maquina");
    const manualObjetivo = parametros.get("manual");
    const accionObjetivo = parametros.get("accion");
    const destelloObjetivo = parametros.get("destello");

    if (maquinaObjetivo) {
        const filaObjetivo = document.querySelector(`.fila-maquina[data-machine-id="${maquinaObjetivo}"]`);

        if (filaObjetivo) {
            filaObjetivo.classList.add("maquina-resaltada", "table-warning");
            if (destelloObjetivo === "verde") {
                filaObjetivo.classList.add("maquina-destello-verde", "table-success");
            }
            filaObjetivo.scrollIntoView({ behavior: "smooth", block: "center" });

            setTimeout(function () {
                filaObjetivo.classList.remove("maquina-resaltada", "table-warning");
                filaObjetivo.classList.remove("maquina-destello-verde", "table-success");
            }, destelloObjetivo === "verde" ? 4200 : 3000);

            if (manualObjetivo || accionObjetivo === "reparar") {
                const idModal = filaObjetivo.getAttribute("data-modal");
                const modal = document.getElementById(idModal);

                if (modal) {
                    abrirModalMaquina(modal, "consulta");

                    setTimeout(function () {
                        const manual = modal.querySelector(`[data-manual-id="${manualObjetivo}"]`);

                        if (manual) {
                            manual.scrollIntoView({ behavior: "smooth", block: "center" });
                            manual.classList.add("manual-resaltado", "bg-success-subtle");

                            setTimeout(function () {
                                manual.classList.remove("manual-resaltado", "bg-success-subtle");
                            }, 4200);
                        }
                    }, 450);

                    if (accionObjetivo === "reparar") {
                        const contenidoModal = modal.querySelector(".modalmaquina-contenido");
                        if (contenidoModal) {
                            contenidoModal.classList.add("modal-reparacion-alerta", "border", "border-3", "border-warning");

                            setTimeout(function () {
                                contenidoModal.classList.remove("modal-reparacion-alerta", "border", "border-3", "border-warning");
                            }, 4600);
                        }

                        setTimeout(function () {
                            const formReparacion = modal.querySelector('[data-repair-form="true"]');

                            if (formReparacion) {
                                const botonDesplegar = formReparacion.querySelector('[data-repair-toggle="true"]');
                                const panelAuto = formReparacion.querySelector(".reparacion-formulario-inmediato");

                                if (botonDesplegar) {
                                    formReparacion.classList.remove("reparacion-abierta");
                                    sincronizarFormReparacion(formReparacion);
                                    botonDesplegar.setAttribute("aria-expanded", "false");
                                    botonDesplegar.focus({ preventScroll: true });
                                    if (panelAuto) {
                                        panelAuto.hidden = true;
                                    }
                                } else {
                                    formReparacion.classList.add("reparacion-abierta");
                                    sincronizarFormReparacion(formReparacion);
                                    if (panelAuto) {
                                        panelAuto.hidden = false;
                                    }
                                }

                                formReparacion.scrollIntoView({ behavior: "smooth", block: "center" });
                                formReparacion.classList.add("reparacion-resaltada", "border", "border-2", "border-warning");

                                setTimeout(function () {
                                    formReparacion.classList.remove("reparacion-resaltada", "border", "border-2", "border-warning");
                                }, 4200);
                            }
                        }, 500);
                    }
                }
            }
        }
    }
});














