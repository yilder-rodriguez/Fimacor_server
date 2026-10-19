package com.fimacor.comun.menu;

import com.fimacor.catalogos.entity.EstadoMaquina;
import com.fimacor.catalogos.repository.EstadoMaquinaRepository;
import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.maquinas.service.AsignacionMaquinaService;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.repository.FichaMaquinaRepository;
import com.fimacor.mantenimiento.entity.HistorialMantenimiento;
import com.fimacor.mantenimiento.repository.HistorialMantenimientoRepository;
import com.fimacor.mantenimiento.entity.Mantenimiento;
import com.fimacor.mantenimiento.repository.MantenimientoRepository;
import com.fimacor.mantenimiento.util.MantenimientoTexto;
import com.fimacor.manuales.entity.Manual;
import com.fimacor.manuales.repository.ManualRepository;
import com.fimacor.novedades.entity.Novedad;
import com.fimacor.novedades.repository.NovedadRepository;
import com.fimacor.reubicacion.entity.ReubicacionMaquina;
import com.fimacor.reubicacion.repository.ReubicacionMaquinaRepository;
import com.fimacor.reubicacion.util.ReubicacionTexto;
import com.fimacor.usuarios.entity.NotificacionConfiguracion;
import com.fimacor.usuarios.repository.NotificacionConfiguracionRepository;
import com.fimacor.usuarios.entity.NotificacionUsuario;
import com.fimacor.usuarios.repository.NotificacionUsuarioRepository;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Calcula los permisos, estadisticas, modulos y notificaciones del panel
 * principal: variantes de titulo/descripcion para tecnicos, la campanita
 * de notificaciones (varias fuentes) y la bandeja de reubicaciones
 * pendientes de Subdireccion/Cuentadante.
 */
@Service
public class MenuService {

    private final PermisoService permisoService;
    private final FichaMaquinaRepository fichaMaquinaRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final NovedadRepository novedadRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionUsuarioRepository notificacionUsuarioRepository;
    private final NotificacionConfiguracionRepository notificacionConfiguracionRepository;
    private final ManualRepository manualRepository;
    private final ReubicacionMaquinaRepository reubicacionRepository;
    private final HistorialMantenimientoRepository historialRepository;
    private final EstadoMaquinaRepository estadoMaquinaRepository;
    private final AsignacionMaquinaService asignacionMaquinaService;

    public MenuService(PermisoService permisoService, FichaMaquinaRepository fichaMaquinaRepository,
            MantenimientoRepository mantenimientoRepository, NovedadRepository novedadRepository,
            UsuarioRepository usuarioRepository, NotificacionUsuarioRepository notificacionUsuarioRepository,
            NotificacionConfiguracionRepository notificacionConfiguracionRepository, ManualRepository manualRepository,
            ReubicacionMaquinaRepository reubicacionRepository, HistorialMantenimientoRepository historialRepository,
            EstadoMaquinaRepository estadoMaquinaRepository, AsignacionMaquinaService asignacionMaquinaService) {
        this.permisoService = permisoService;
        this.fichaMaquinaRepository = fichaMaquinaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.novedadRepository = novedadRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionUsuarioRepository = notificacionUsuarioRepository;
        this.notificacionConfiguracionRepository = notificacionConfiguracionRepository;
        this.manualRepository = manualRepository;
        this.reubicacionRepository = reubicacionRepository;
        this.historialRepository = historialRepository;
        this.estadoMaquinaRepository = estadoMaquinaRepository;
        this.asignacionMaquinaService = asignacionMaquinaService;
    }

    public record ModuloCard(String titulo, String descripcion, String imagen, String enlace, String dataName) {
    }

    public record NotificacionItem(String tipo, String enlace, String titulo, String mensaje, String meta,
            String icono, int prioridad, String grupo, String id, String reubicacionId) {
    }

    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public MenuDatos calcular(Usuario usuario, String solicitudReubicacionParam) {
        int idRol = usuario.getRol() != null ? usuario.getRol().getIdRol() : 0;
        String nombreRol = usuario.getRol() != null ? usuario.getRol().getDescripcionRol() : "Usuario";
        String rolNorm = MenuTexto.normalizar(nombreRol);

        boolean esAdministrador = rolNorm.contains("admin");
        boolean esCuentadante = rolNorm.contains("cuentadante");
        boolean esTecnico = rolNorm.contains("tecnico");
        boolean esSubdireccion = rolNorm.contains("subdireccion");
        boolean esLogistica = rolNorm.contains("logistica");
        boolean esInstructor = rolNorm.contains("instructor");
        // El Instructor y Logistica no deben recibir la campanita "global" (maquina registrada,
        // fallas criticas de cualquier maquina, mantenimientos vencidos, garantias...): son
        // demasiado ruidosas para su rol. Logistica ya tiene su propio aviso de reparaciones por
        // revisar (mas abajo) y el Instructor el de sus propias novedades cerradas.
        boolean puedeVerNotificacionesGlobales = !esCuentadante && !esTecnico && !esLogistica && !esInstructor;

        boolean puedeGestionarSistema = (permisoService.tienePermisoConTodas(idRol, "roles", "gestionar")
                && permisoService.tienePermisoConTodas(idRol, "configuracion", "gestionar")) || esAdministrador;
        // Instructor al que el Administrador le habilito la casilla "actua como tecnico": conserva
        // su menu de Instructor y ademas ve (solo) lo que le asignaron como tecnico.
        boolean instructorTecnico = permisoService.actuaComoTecnico(usuario);
        boolean puedeAtenderReparaciones = permisoService.tienePermisoConTodas(idRol, "reparacion", "reparar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "confirmar")
                || instructorTecnico;
        boolean puedeGestionarOperaciones = permisoService.tienePermisoConTodas(idRol, "reubicacion", "registrar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar");
        boolean puedeVerUsuarios = permisoService.tienePermisoConTodas(idRol, "usuarios", "ver");
        boolean puedeVerMaquinas = permisoService.tienePermisoConTodas(idRol, "maquinas", "ver");
        boolean puedeAdministrarMaquinas = !esTecnico && !esLogistica
                && (permisoService.tienePermisoConTodas(idRol, "maquinas", "administrar")
                    || (permisoService.tienePermisoConTodas(idRol, "maquinas", "editar") && permisoService.tienePermisoConTodas(idRol, "maquinas", "eliminar"))
                    || esAdministrador);
        // El Instructor tiene permisos de mantenimiento.programar en la base de datos (heredados
        // del sistema original), pero ese modulo nunca deberia aparecerle: su rol es reportar
        // novedades, no programar mantenimientos de las maquinas.
        boolean puedeVerMantenimiento = !esInstructor
                && (permisoService.tienePermisoConTodas(idRol, "mantenimiento", "ver")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "programar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "programar"));
        boolean puedeAdministrarMantenimiento = !esTecnico && !esLogistica && !esInstructor
                && (permisoService.tienePermisoConTodas(idRol, "mantenimiento", "administrar")
                    || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar")
                    || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "gestionar")
                    || esAdministrador);
        boolean puedeVerReportes = permisoService.tienePermisoConTodas(idRol, "novedades", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "novedad", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "reporte", "falla")
                || permisoService.tienePermisoConTodas(idRol, "reportes", "falla");
        boolean puedeVerFallasCriticas = puedeVerReportes || puedeAtenderReparaciones;
        boolean puedeReubicarMaquinas = !esLogistica && (permisoService.tienePermisoConTodas(idRol, "reubicacion", "registrar")
                || permisoService.tienePermisoConTodas(idRol, "reubicacion", "reubicar")
                || permisoService.tienePermisoConTodas(idRol, "maquinas", "reubicar")
                || permisoService.tienePermisoConTodas(idRol, "maquina", "reubicar")
                || esCuentadante || esSubdireccion);
        boolean puedeAutorizarBandejaSubdireccion = esAdministrador
                || (nombreRol != null && nombreRol.toLowerCase().contains("subdirecci"));
        boolean puedeVerManuales = !esTecnico && !esLogistica && !esInstructor && (permisoService.tienePermisoConTodas(idRol, "manuales", "ver")
                || permisoService.tienePermiso(idRol, "manual", "manuales"));
        boolean puedeVerRolesPermisos = permisoService.tienePermisoConTodas(idRol, "roles", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "permisos", "gestionar") || esAdministrador;
        boolean puedeAsignarMaquinas = esAdministrador || permisoService.tienePermisoConTodas(idRol, "maquinas", "asignar");
        boolean puedeVerMaquinasAsignadas = esCuentadante && permisoService.tienePermisoConTodas(idRol, "maquinas", "asignadas");

        MenuDatos datos = new MenuDatos();
        datos.esTecnico = esTecnico;
        datos.esCuentadante = esCuentadante;
        datos.puedeVerUsuarios = puedeVerUsuarios;
        datos.puedeVerMaquinas = puedeVerMaquinas;
        datos.puedeVerMantenimiento = puedeVerMantenimiento;
        datos.puedeVerReportes = puedeVerReportes;
        datos.puedeReubicarMaquinas = puedeReubicarMaquinas;
        datos.puedeVerManuales = puedeVerManuales;
        datos.puedeVerRolesPermisos = puedeVerRolesPermisos;
        datos.puedeGestionarSistema = puedeGestionarSistema;
        datos.puedeAdministrarMaquinas = puedeAdministrarMaquinas;
        datos.puedeAdministrarMantenimiento = puedeAdministrarMantenimiento;
        datos.puedeAsignarMaquinas = puedeAsignarMaquinas;
        datos.puedeVerMaquinasAsignadas = puedeVerMaquinasAsignadas;
        datos.puedeAutorizarBandejaSubdireccion = puedeAutorizarBandejaSubdireccion;
        datos.puedeAutorizarBandejaCuentadante = esCuentadante;
        datos.puedeAutorizarTodosTipos = esAdministrador;
        datos.puedeAutorizarExternos = esSubdireccion;
        datos.puedeEliminarEvidenciaReubicacion = esAdministrador;

        // ---- Datos base (con filtrado especifico de tecnico) ----
        List<Novedad> listaNovedades = novedadRepository.findAllByOrderByIdNovedadesDesc();
        List<FichaMaquina> listaMaquinas = fichaMaquinaRepository.findByBajaFalseOrderByCodigoSenaAsc();
        List<Mantenimiento> listaMantenimientos = mantenimientoRepository.findAllByOrderByIdMantenimientoDesc();
        List<HistorialMantenimiento> listaHistorial = historialRepository.findAllByOrderByIdHistorialMantenimientoDesc();
        List<Manual> listaManuales = manualRepository.findAll();
        List<ReubicacionMaquina> listaReubicaciones = reubicacionRepository.findAllByOrderByIdRehubicacionMaquinaDesc();
        List<Usuario> listaUsuarios = usuarioRepository.findAll();

        int totalMaquinasAsignadasTecnico = 0;
        int totalMantenimientosPendientesTecnico = 0;
        int primeraMaquinaPendienteTecnico = 0;
        int totalNovedadesPendientesTecnico = 0;
        int primeraNovedadPendienteTecnico = 0;
        if (esTecnico) {
            Set<Integer> idsMaquinasTecnico = new HashSet<>();
            List<Mantenimiento> mantenimientosTecnico = mantenimientoRepository
                    .findByTecnico_IdUsuarioOrderByIdMantenimientoDesc(usuario.getIdUsuario());
            for (Mantenimiento m : mantenimientosTecnico) {
                idsMaquinasTecnico.add(m.getMaquina().getIdMaquinas());
                if (primeraMaquinaPendienteTecnico == 0) primeraMaquinaPendienteTecnico = m.getMaquina().getIdMaquinas();
            }
            listaMantenimientos = mantenimientosTecnico;

            for (HistorialMantenimiento h : listaHistorial) {
                if (MantenimientoTexto.asignadoAlUsuario(h, usuario) && MantenimientoTexto.estaAsignadoHistorial(h) && h.getMaquina() != null) {
                    idsMaquinasTecnico.add(h.getMaquina().getIdMaquinas());
                    if (primeraMaquinaPendienteTecnico == 0) primeraMaquinaPendienteTecnico = h.getMaquina().getIdMaquinas();
                }
            }

            for (Novedad n : listaNovedades) {
                String estado = n.getEstadoFlujo() == null ? "REPORTADA" : n.getEstadoFlujo().trim();
                boolean pendiente = n.getTecnicoAsignado() != null && usuario.getIdUsuario().equals(n.getTecnicoAsignado().getIdUsuario())
                        && !"CERRADA".equalsIgnoreCase(estado) && !"CANCELADA".equalsIgnoreCase(estado)
                        && !"FINALIZADA_TECNICO".equalsIgnoreCase(estado);
                if (pendiente) {
                    totalNovedadesPendientesTecnico++;
                    if (n.getMaquina() != null) {
                        idsMaquinasTecnico.add(n.getMaquina().getIdMaquinas());
                        if (primeraMaquinaPendienteTecnico == 0) primeraMaquinaPendienteTecnico = n.getMaquina().getIdMaquinas();
                    }
                    if (primeraNovedadPendienteTecnico == 0) primeraNovedadPendienteTecnico = n.getIdNovedades();
                }
            }

            listaMaquinas = listaMaquinas.stream().filter(m -> idsMaquinasTecnico.contains(m.getIdMaquinas())).toList();
            totalMaquinasAsignadasTecnico = listaMaquinas.size();
        }

        Set<Integer> estadosActivos = new HashSet<>();
        Set<Integer> estadosCriticos = new HashSet<>();
        Map<Integer, EstadoMaquina> estadosPorId = new HashMap<>();
        for (EstadoMaquina estado : estadoMaquinaRepository.findAllByOrderByIdEstadoDeMaquinaAsc()) {
            estadosPorId.put(estado.getIdEstadoDeMaquina(), estado);
            if (MenuTexto.esEstadoActivo(estado.getNombreTipo(), estado.getDescripcionEstado())) estadosActivos.add(estado.getIdEstadoDeMaquina());
            if (MenuTexto.esEstadoCritico(estado.getNombreTipo(), estado.getDescripcionEstado())) estadosCriticos.add(estado.getIdEstadoDeMaquina());
        }

        int totalMaquinasActivas;
        if (estadosActivos.isEmpty()) {
            totalMaquinasActivas = listaMaquinas.size();
        } else {
            totalMaquinasActivas = (int) listaMaquinas.stream()
                    .filter(m -> m.getEstado() != null && estadosActivos.contains(m.getEstado().getIdEstadoDeMaquina())).count();
        }

        LocalDate hoy = LocalDate.now();
        int totalMantenimientos = listaMantenimientos.size();
        if (esTecnico) {
            totalMaquinasActivas = totalMaquinasAsignadasTecnico;
            totalMantenimientosPendientesTecnico = listaMantenimientos.size() + totalNovedadesPendientesTecnico;
            totalMantenimientos = totalMantenimientosPendientesTecnico;
        }

        int totalUsuarios = listaUsuarios.size();
        int totalMaquinasAsignadasCuentadante = (int) asignacionMaquinaService.contarPorUsuario(usuario.getIdUsuario());

        datos.totalMaquinasActivas = totalMaquinasActivas;
        datos.totalMantenimientos = totalMantenimientos;
        datos.totalUsuarios = totalUsuarios;
        datos.totalMaquinasAsignadasCuentadante = totalMaquinasAsignadasCuentadante;
        datos.primeraNovedadPendienteTecnico = primeraNovedadPendienteTecnico;
        datos.primeraMaquinaPendienteTecnico = primeraMaquinaPendienteTecnico;
        datos.totalMantenimientosPendientesTecnico = totalMantenimientosPendientesTecnico;

        if (puedeVerFallasCriticas && !esTecnico) {
            datos.totalFallasCriticas = (int) listaNovedades.stream()
                    .filter(n -> !"CERRADA".equalsIgnoreCase(n.getEstadoFlujo()) && !"CANCELADA".equalsIgnoreCase(n.getEstadoFlujo()))
                    .count();
        }

        // ---- Notificaciones (campanita) ----
        List<NotificacionItem> notificaciones = new ArrayList<>();
        if (!esInstructor) {
            for (NotificacionUsuario n : notificacionUsuarioRepository
                    .findFirst20ByUsuario_IdUsuarioOrderByIdNotificacionUsuarioDesc(usuario.getIdUsuario())) {
                String enlace = (n.getEnlace() == null || n.getEnlace().isBlank())
                        ? "/maquinas/mis-asignadas" : MenuTexto.normalizarEnlace(n.getEnlace());
                agregar(notificaciones, n.getTipo(), enlace, n.getTitulo(), n.getMensaje(),
                        "Fecha: " + n.getFechaCreacion(), n.getIcono());
            }
        }

        for (FichaMaquina maquina : listaMaquinas) {
            if (puedeVerMaquinas && puedeVerNotificacionesGlobales) {
                agregar(notificaciones, "ok", "/maquinas?maquina=" + maquina.getIdMaquinas(), "Máquina registrada",
                        MenuTexto.referenciaMaquina(maquina), "Disponible para consulta", "M");
            }
            if (puedeVerFallasCriticas && puedeVerNotificacionesGlobales && maquina.getEstado() != null
                    && estadosCriticos.contains(maquina.getEstado().getIdEstadoDeMaquina())) {
                String enlaceFalla = (puedeVerReportes && !puedeAtenderReparaciones)
                        ? "/novedades?maquina=" + maquina.getIdMaquinas()
                        : "/maquinas?maquina=" + maquina.getIdMaquinas() + "&accion=reparar";
                agregar(notificaciones, "red", enlaceFalla, "Falla crítica de máquina",
                        "La máquina " + codigoSena(maquina) + " requiere revisión: " + maquina.getEstado().getNombreTipo(),
                        (puedeVerReportes && !puedeAtenderReparaciones) ? "Haz clic para reportar la novedad" : "Haz clic para reparar", "!");
            }
        }

        if (puedeVerManuales && puedeVerNotificacionesGlobales) {
            for (Manual manual : listaManuales) {
                agregar(notificaciones, "ok",
                        "/maquinas?maquina=" + (manual.getMaquina() != null ? manual.getMaquina().getIdMaquinas() : 0) + "&manual=" + manual.getIdManuales(),
                        "Nuevo manual disponible", manual.getNombreDelManual() + " - " + MenuTexto.referenciaMaquina(manual.getMaquina()),
                        manual.getDescripcionManual() != null ? manual.getDescripcionManual() : "Documento técnico registrado", "D");
            }
        }

        if (puedeVerNotificacionesGlobales && (puedeGestionarSistema || puedeGestionarOperaciones)) {
            for (ReubicacionMaquina r : listaReubicaciones) {
                String maquinaTexto = MenuTexto.referenciaMaquina(r.getMaquina());
                agregar(notificaciones, "warn", "/reubicacion?detalle=" + r.getIdRehubicacionMaquina(), "Solicitud de reubicación",
                        maquinaTexto + " solicitada de " + MenuTexto.valorTexto(r.getOrigenTexto(), "Sin origen") + " a " + MenuTexto.valorTexto(r.getDestinoTexto(), "Sin destino"),
                        "Estado: " + ReubicacionTexto.estadoTexto(r.getEstadoAutorizacion()) + " | Fecha: " + r.getFecha() + " | Autoriza: " + MenuTexto.valorTexto(r.getAutorizadoPor(), "Sin responsable"),
                        "R");
            }
        }
        if (esSubdireccion) {
            for (ReubicacionMaquina r : listaReubicaciones) {
                if (!"Externo".equalsIgnoreCase(r.getTipoTraslado())) continue;
                if (!"Pendiente".equalsIgnoreCase(MenuTexto.valorTexto(r.getEstadoAutorizacion(), "Pendiente"))) continue;
                agregar(notificaciones, "warn", "/reubicacion?detalle=" + r.getIdRehubicacionMaquina(), "Traslado externo por aprobar",
                        MenuTexto.referenciaMaquina(r.getMaquina()) + " solicitada de " + MenuTexto.valorTexto(r.getOrigenTexto(), "Sin origen") + " a " + MenuTexto.valorTexto(r.getDestinoTexto(), "Sin destino"),
                        "Estado: " + ReubicacionTexto.estadoTexto(r.getEstadoAutorizacion()) + " | Fecha: " + r.getFecha(),
                        "R");
            }
        }

        if (esTecnico || instructorTecnico) {
            for (Novedad n : listaNovedades) {
                String estado = n.getEstadoFlujo() == null ? "REPORTADA" : n.getEstadoFlujo().trim();
                if (n.getTecnicoAsignado() != null && usuario.getIdUsuario().equals(n.getTecnicoAsignado().getIdUsuario())
                        && !"CERRADA".equalsIgnoreCase(estado) && !"CANCELADA".equalsIgnoreCase(estado) && !"FINALIZADA_TECNICO".equalsIgnoreCase(estado)) {
                    agregar(notificaciones, "warn", "/novedades?novedad=" + n.getIdNovedades(), "Reparación asignada",
                            "Tienes una reparación pendiente en " + MenuTexto.referenciaMaquina(n.getMaquina()) + ".", "Estado: " + estado, "R");
                }
            }
            for (Mantenimiento m : listaMantenimientos) {
                if (m.getTecnico() == null || !usuario.getIdUsuario().equals(m.getTecnico().getIdUsuario())) continue;
                agregar(notificaciones, "warn", "/mantenimiento/programar", "Mantenimiento asignado",
                        "Tienes asignada la máquina " + MenuTexto.referenciaMaquina(m.getMaquina()) + ".",
                        "Fecha programada: " + m.getFechaMantenimiento() + " | Próximo: " + m.getFechaProximoMantenimiento(), "T");
            }
        }

        if (puedeVerMantenimiento && puedeVerNotificacionesGlobales) {
            for (Mantenimiento m : listaMantenimientos) {
                if (MantenimientoTexto.vencido(m, hoy)) {
                    agregar(notificaciones, "red", "/mantenimiento/programar", "Mantenimiento vencido",
                            "La máquina " + MenuTexto.referenciaMaquina(m.getMaquina()) + " tiene mantenimiento atrasado.",
                            "Fecha vencida: " + m.getFechaProximoMantenimiento(), "!");
                } else if (MantenimientoTexto.proximo(m, hoy)) {
                    agregar(notificaciones, "warn", "/maquinas?maquina=" + m.getMaquina().getIdMaquinas(), "Mantenimiento pendiente",
                            "La máquina " + MenuTexto.referenciaMaquina(m.getMaquina()) + " necesita mantenimiento pronto.",
                            "Fecha límite: " + m.getFechaProximoMantenimiento(), "!");
                }
            }
        }

        if (puedeVerMaquinas && puedeVerNotificacionesGlobales) {
            for (FichaMaquina maquina : listaMaquinas) {
                LocalDate fechaGarantia = maquina.isTieneGarantia() ? maquina.getFechaGarantia() : null;
                if (fechaGarantia == null) continue;
                long dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaGarantia);
                if (fechaGarantia.isBefore(hoy)) {
                    agregar(notificaciones, "red", "/maquinas?maquina=" + maquina.getIdMaquinas(), "Garantía vencida",
                            MenuTexto.referenciaMaquina(maquina) + " ya superó la fecha de garantía.",
                            "Fecha garantía: " + fechaGarantia.format(FECHA_CORTA), "G");
                } else if (dias >= 0 && dias <= 30) {
                    agregar(notificaciones, "warn", "/maquinas?maquina=" + maquina.getIdMaquinas(), "Garantía próxima a vencer",
                            MenuTexto.referenciaMaquina(maquina) + " requiere revisión de soporte.",
                            "Fecha garantía: " + fechaGarantia.format(FECHA_CORTA), "G");
                }
            }
        }

        for (HistorialMantenimiento h : listaHistorial) {
            boolean asignadoParaMi = puedeAtenderReparaciones && MantenimientoTexto.asignadoAlUsuario(h, usuario);
            // puedeGestionarOperaciones da falso positivo para Instructor: su fila de permisos
            // trae "mantenimientos.gestionar" (plural, heredado del sistema original) y
            // PermisoService hace matching flexible por texto, asi que "mantenimiento"+"gestionar"
            // le hace match igual. Sin el chequeo de puedeVerNotificacionesGlobales, el Instructor
            // terminaba viendo el aviso "Responsable asignado" de CUALQUIER mantenimiento del
            // sistema, no solo el ruido que esa bandera ya esta pensada para filtrarle.
            if (MantenimientoTexto.estaAsignadoHistorial(h) && (esTecnico ? asignadoParaMi
                    : ((puedeVerNotificacionesGlobales && (puedeGestionarSistema || puedeGestionarOperaciones)) || asignadoParaMi))) {
                agregar(notificaciones, "warn", "/mantenimiento/historial?historial=" + h.getIdHistorialMantenimiento() + "&accion=confirmar",
                        asignadoParaMi ? "Te asignaron esta máquina para mantenimiento" : "Responsable asignado",
                        asignadoParaMi
                                ? MenuTexto.referenciaMaquina(h.getMaquina()) + " fue asignada a ti para mantenimiento. Haz clic para registrar la reparación."
                                : MenuTexto.referenciaMaquina(h.getMaquina()) + " asignada a " + h.getTecnicoEncargado(),
                        asignadoParaMi ? "Abre el formulario de reparación" : MenuTexto.resumenHistorial(h.getDescripcionAccion()), "T");
            }
            if (puedeVerNotificacionesGlobales
                    && (esTecnico ? MantenimientoTexto.asignadoAlUsuario(h, usuario) : (puedeGestionarSistema || puedeGestionarOperaciones || puedeVerMantenimiento))
                    && MantenimientoTexto.estaReparado(h)) {
                String reparadoPor = h.getTecnicoEncargado() != null && !h.getTecnicoEncargado().trim().isEmpty() ? h.getTecnicoEncargado() : "Sin responsable registrado";
                agregar(notificaciones, "ok", "/mantenimiento/historial?historial=" + h.getIdHistorialMantenimiento(),
                        "Máquina reparada por " + reparadoPor, MenuTexto.referenciaMaquina(h.getMaquina()) + " quedó en servicio",
                        MenuTexto.resumenHistorial(h.getDescripcionAccion()), "R");
            }
        }

        if (esInstructor) {
            for (Novedad n : listaNovedades) {
                if (n.getReportadoPor() == null || !n.getReportadoPor().getIdUsuario().equals(usuario.getIdUsuario())) continue;
                if (!"CERRADA".equalsIgnoreCase(n.getEstadoFlujo())) continue;
                agregar(notificaciones, "ok", "/maquinas?maquina=" + n.getMaquina().getIdMaquinas(), "Máquina reparada y puesta en servicio",
                        MenuTexto.referenciaMaquina(n.getMaquina()) + " fue reparada y aprobada.",
                        "La novedad que reportaste fue resuelta.", "R");
            }
        }

        if (esLogistica) {
            for (Novedad n : listaNovedades) {
                if (!"FINALIZADA_TECNICO".equalsIgnoreCase(n.getEstadoFlujo())) continue;
                agregar(notificaciones, "warn", "/novedades?novedad=" + n.getIdNovedades(), "Reparación finalizada pendiente de aprobación",
                        MenuTexto.referenciaMaquina(n.getMaquina()) + " fue finalizada por el técnico.",
                        "Revisa la reparación y decide si se pone en servicio o se devuelve.", "R");
            }
        }

        // El original (Menu.jsp linea 298) excluye Tecnico e Instructor de este aviso especifico
        // -- puedeVerReportes por si solo no los excluye aqui porque tambien controla el enlace
        // "Novedad" del menu/sidebar, que si les debe seguir apareciendo (para que reporten SU
        // propia novedad). Sin este chequeo aparte, terminaban viendo en la campanita el aviso
        // "Novedad registrada" de CUALQUIER maquina critica del sistema, no solo la suya.
        if (puedeVerReportes && !esTecnico && !esInstructor) {
            for (Novedad n : listaNovedades) {
                if (n.getMaquina() == null || n.getMaquina().getEstado() == null || !estadosCriticos.contains(n.getMaquina().getEstado().getIdEstadoDeMaquina())) continue;
                agregar(notificaciones, "red", "/novedades?novedad=" + n.getIdNovedades(), "Novedad registrada",
                        MenuTexto.referenciaMaquina(n.getMaquina()) + ": " + n.getDescripcionNovedad(),
                        n.getFechaNovedad() + " " + n.getHoraNovedad(), "!");
            }
        }

        if (puedeGestionarSistema) {
            for (Usuario u : listaUsuarios) {
                String nombreRolUsuario = u.getRol() != null ? u.getRol().getDescripcionRol() : "Sin rol";
                agregar(notificaciones, "admin", "/usuarios", "Usuario registrado",
                        u.getNombre() + " " + u.getApellido() + " - " + nombreRolUsuario,
                        "Documento: " + u.getDocumentoIdentidad() + " | Teléfono: " + u.getNumeroTelefono() + " | Correo: " + u.getCorreo(),
                        MenuTexto.iniciales(u.getNombre(), u.getApellido()));
            }
            for (NotificacionConfiguracion c : notificacionConfiguracionRepository.findFirst12ByOrderByIdNotificacionDesc()) {
                agregar(notificaciones, c.getTipo(), MenuTexto.normalizarEnlace(c.getEnlace()), c.getTitulo(), c.getMensaje(),
                        "Por: " + c.getCreadoPor() + " | " + c.getFechaCreacion(), c.getIcono());
            }
        }

        notificaciones.sort(Comparator.comparingInt(NotificacionItem::prioridad).thenComparing(n -> n.titulo().toLowerCase()));
        datos.notificaciones = notificaciones;
        datos.claveLecturaNotificaciones = "fimacor_notifs_" + usuario.getIdUsuario();

        // ---- Perfil ----
        boolean perfilIncompleto = usuario.getDocumentoIdentidad() == null || usuario.getDocumentoIdentidad().trim().isEmpty()
                || usuario.getNumeroTelefono() == null || usuario.getNumeroTelefono().trim().isEmpty();
        datos.perfilIncompleto = perfilIncompleto;
        datos.fotoCarnetPerfil = usuario.getFotoCarnet() != null ? usuario.getFotoCarnet().trim() : "";
        datos.iniciales = MenuTexto.iniciales(usuario.getNombre(), usuario.getApellido());

        // ---- Bandeja de reubicaciones pendientes ----
        if (puedeAutorizarBandejaSubdireccion) {
            datos.reubicacionesPendientesSubdireccion = listaReubicaciones.stream()
                    .filter(r -> esAdministrador || "Externo".equalsIgnoreCase(r.getTipoTraslado()))
                    .filter(r -> "Pendiente".equalsIgnoreCase(MenuTexto.valorTexto(r.getEstadoAutorizacion(), "Pendiente")))
                    .toList();
        }
        if (esCuentadante) {
            datos.reubicacionesPendientesCuentadante = listaReubicaciones.stream()
                    .filter(r -> "Interno".equalsIgnoreCase(MenuTexto.valorTexto(r.getTipoTraslado(), "Interno")))
                    .filter(r -> "Pendiente".equalsIgnoreCase(MenuTexto.valorTexto(r.getEstadoAutorizacion(), "Pendiente")))
                    .toList();
        }
        datos.reubicaciones = listaReubicaciones;
        datos.puedeDecidirReubicacion = puedeAutorizarBandejaSubdireccion || esCuentadante;

        // ---- Modulos ----
        boolean hayMetricas = puedeVerMaquinas || puedeVerMantenimiento || puedeVerFallasCriticas || puedeVerUsuarios || puedeVerMaquinasAsignadas;
        List<ModuloCard> modulos = new ArrayList<>();
        if (permisoService.tienePermisoConTodas(idRol, "tablero", "ver")) {
            modulos.add(new ModuloCard("Tablero de gerencia", "Indicadores del inventario: estado de las máquinas, valor, calidad de los datos y detalle.",
                    "/Img/Maquinas.png", "/gerencia", "tablero gerencia indicadores inventario"));
        }
        if (puedeVerMaquinas) {
            modulos.add(new ModuloCard(esTecnico ? "Máquinas pendientes" : "Máquinas",
                    esTecnico ? "Consulta solo las máquinas pendientes por reparar que tienes asignadas."
                            : "Consulta las máquinas textiles registradas y sus características.",
                    "/Img/Maquinas.png", "/maquinas", "maquinas"));
        }
        if (esTecnico) {
            modulos.add(new ModuloCard("Máquinas reparadas", "Consulta las novedades de reparación que ya finalizaste.",
                    "/Img/Mantenimiento.png", "/novedades?reparadas=true", "maquinas reparadas reparacion finalizada"));
        }
        if (puedeVerMantenimiento) {
            modulos.add(new ModuloCard(esTecnico ? "Mis mantenimientos" : "Mantenimientos",
                    esTecnico ? "Consulta solo los mantenimientos que tienes asignados." : "Registra mantenimientos preventivos y correctivos.",
                    "/Img/Mantenimiento.png", "/mantenimiento/programar", "mantenimientos"));
        }
        if (esTecnico) {
            modulos.add(new ModuloCard("Historial de reparaciones", "Consulta el seguimiento y los cierres de tus mantenimientos.",
                    "/Img/Mantenimiento.png", "/mantenimiento/historial", "historial reparaciones mantenimiento"));
        }
        if (puedeAdministrarMaquinas) {
            modulos.add(new ModuloCard("Admin Máquinas", "Busca, consulta, modifica y elimina máquinas desde un apartado administrativo.",
                    "/Img/Maquinas.png", "/maquinas/administracion", "administracion maquinas consultar editar eliminar"));
        }
        if (puedeAdministrarMantenimiento) {
            modulos.add(new ModuloCard("Admin Mant.", "Gestiona todos los mantenimientos registrados sin cambiar la pantalla operativa.",
                    "/Img/Mantenimiento.png", "/mantenimiento/administracion", "administracion mantenimiento consultar editar eliminar"));
        }
        if (puedeGestionarSistema) {
            modulos.add(new ModuloCard("Importar inventario", "Carga máquinas desde una hoja de Excel o CSV y crea lo que falte en los catálogos.",
                    "/Img/Maquinas.png", "/importacion", "importar inventario excel csv carga masiva"));
        }
        if (puedeGestionarSistema && puedeAsignarMaquinas) {
            modulos.add(new ModuloCard("Asignar máquinas", "Asigna máquinas registradas únicamente a usuarios Cuentadante.",
                    "/Img/Maquinas.png", "/maquinas/asignar", "asignar maquinas cuentadante"));
        }
        if (puedeVerMaquinasAsignadas) {
            modulos.add(new ModuloCard("Mis máquinas asignadas", "Consulta cuántas máquinas tienes, dónde están y sus características.",
                    "/Img/Maquinas.png", "/maquinas/mis-asignadas", "mis maquinas asignadas cuentadante"));
        }
        if (puedeReubicarMaquinas) {
            modulos.add(new ModuloCard("Reubicación", "Registra movimientos autorizados y evidencias de traslado.",
                    "/Img/Maquinas.png", "/reubicacion", "reubicacion"));
        }
        if (instructorTecnico) {
            modulos.add(new ModuloCard("Mis mantenimientos", "Consulta solo los mantenimientos que tienes asignados como técnico.",
                    "/Img/Mantenimiento.png", "/mantenimiento/programar", "mantenimientos asignados tecnico"));
            modulos.add(new ModuloCard("Historial de reparaciones", "Consulta el seguimiento y los cierres de tus mantenimientos.",
                    "/Img/Mantenimiento.png", "/mantenimiento/historial", "historial reparaciones mantenimiento"));
        }
        if (puedeVerReportes && !esTecnico) {
            modulos.add(new ModuloCard("Novedad", "Registra incidencias o novedades de las máquinas según tu rol.",
                    "/Img/Fallas.png", "/novedades", "novedad incidencia falla"));
        }
        if (puedeVerUsuarios) {
            modulos.add(new ModuloCard("Usuarios", "Administra usuarios y roles del sistema.", "/Img/Usuario.png", "/usuarios", "usuarios"));
        }
        if (puedeVerManuales) {
            modulos.add(new ModuloCard("Guía / Manuales", "Consulta guías y manuales técnicos de las máquinas.",
                    "/Img/Manuales.png", "/manuales", "guia manuales"));
        }
        datos.modulos = modulos;
        datos.hayMetricas = hayMetricas;
        datos.hayModulos = !modulos.isEmpty();
        return datos;
    }

    private void agregar(List<NotificacionItem> notificaciones, String tipo, String enlace, String titulo, String mensaje, String meta, String icono) {
        int prioridad = MenuTexto.prioridad(tipo, titulo);
        String grupo = MenuTexto.grupo(prioridad);
        String id = MenuTexto.idNotificacion(enlace, titulo, mensaje, meta);
        String reubicacionId = MenuTexto.reubicacionIdDesdeEnlace(enlace);
        notificaciones.add(new NotificacionItem(tipo, enlace, titulo, mensaje, meta, icono, prioridad, grupo, id, reubicacionId));
    }

    private String codigoSena(FichaMaquina maquina) {
        return maquina.getCodigoSena() != null && !maquina.getCodigoSena().trim().isEmpty() ? maquina.getCodigoSena() : "Sin código";
    }

    public static class MenuDatos {
        public boolean esTecnico;
        public boolean esCuentadante;
        public boolean puedeVerUsuarios;
        public boolean puedeVerMaquinas;
        public boolean puedeVerMantenimiento;
        public boolean puedeVerReportes;
        public boolean puedeReubicarMaquinas;
        public boolean puedeVerManuales;
        public boolean puedeVerRolesPermisos;
        public boolean puedeGestionarSistema;
        public boolean puedeAdministrarMaquinas;
        public boolean puedeAdministrarMantenimiento;
        public boolean puedeAsignarMaquinas;
        public boolean puedeVerMaquinasAsignadas;
        public boolean puedeAutorizarBandejaSubdireccion;
        public boolean puedeAutorizarBandejaCuentadante;
        public boolean puedeAutorizarTodosTipos;
        public boolean puedeAutorizarExternos;
        public boolean puedeEliminarEvidenciaReubicacion;
        public boolean puedeDecidirReubicacion;
        public int totalMaquinasActivas;
        public int totalMantenimientos;
        public int totalFallasCriticas;
        public int totalUsuarios;
        public int totalMaquinasAsignadasCuentadante;
        public int primeraNovedadPendienteTecnico;
        public int primeraMaquinaPendienteTecnico;
        public int totalMantenimientosPendientesTecnico;
        public boolean perfilIncompleto;
        public String fotoCarnetPerfil = "";
        public String iniciales = "U";
        public String claveLecturaNotificaciones = "";
        public List<NotificacionItem> notificaciones = new ArrayList<>();
        public List<ReubicacionMaquina> reubicaciones = new ArrayList<>();
        public List<ReubicacionMaquina> reubicacionesPendientesSubdireccion = new ArrayList<>();
        public List<ReubicacionMaquina> reubicacionesPendientesCuentadante = new ArrayList<>();
        public List<ModuloCard> modulos = new ArrayList<>();
        public boolean hayMetricas;
        public boolean hayModulos;
    }
}
