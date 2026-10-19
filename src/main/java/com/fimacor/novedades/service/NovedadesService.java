package com.fimacor.novedades.service;

import com.fimacor.catalogos.repository.AmbienteAprendizajeRepository;
import com.fimacor.catalogos.entity.Area;
import com.fimacor.catalogos.repository.AreaRepository;
import com.fimacor.catalogos.entity.EstadoMaquina;
import com.fimacor.catalogos.repository.EstadoMaquinaRepository;
import com.fimacor.catalogos.repository.SedeRepository;
import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.mantenimiento.entity.HistorialMantenimiento;
import com.fimacor.mantenimiento.repository.HistorialMantenimientoRepository;
import com.fimacor.mantenimiento.util.MantenimientoTexto;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.repository.FichaMaquinaRepository;
import com.fimacor.maquinas.service.BajaMaquinaService;
import com.fimacor.usuarios.entity.NotificacionUsuario;
import com.fimacor.usuarios.repository.NotificacionUsuarioRepository;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.UsuarioRepository;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fimacor.catalogos.entity.AmbienteAprendizaje;
import com.fimacor.catalogos.entity.Sede;
import com.fimacor.novedades.entity.Novedad;
import com.fimacor.novedades.repository.NovedadRepository;
import com.fimacor.novedades.util.NovedadTexto;

/**
 * Logica de novedades: reportar una falla y gestionar su atencion. Los
 * avisos por WhatsApp y correo de cada cambio de estado todavia no estan
 * conectados; la notificacion interna en la campanita si funciona.
 */
@Service
public class NovedadesService {

    private final NovedadRepository novedadRepository;
    private final FichaMaquinaRepository fichaMaquinaRepository;
    private final SedeRepository sedeRepository;
    private final AmbienteAprendizajeRepository ambienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoMaquinaRepository estadoMaquinaRepository;
    private final NotificacionUsuarioRepository notificacionRepository;
    private final PermisoService permisoService;
    private final AreaRepository areaRepository;
    private final BajaMaquinaService bajaService;
    private final HistorialMantenimientoRepository historialRepository;

    public NovedadesService(NovedadRepository novedadRepository, FichaMaquinaRepository fichaMaquinaRepository,
            SedeRepository sedeRepository, AmbienteAprendizajeRepository ambienteRepository,
            UsuarioRepository usuarioRepository, EstadoMaquinaRepository estadoMaquinaRepository,
            NotificacionUsuarioRepository notificacionRepository, PermisoService permisoService,
            AreaRepository areaRepository, BajaMaquinaService bajaService,
            HistorialMantenimientoRepository historialRepository) {
        this.novedadRepository = novedadRepository;
        this.fichaMaquinaRepository = fichaMaquinaRepository;
        this.sedeRepository = sedeRepository;
        this.ambienteRepository = ambienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.estadoMaquinaRepository = estadoMaquinaRepository;
        this.notificacionRepository = notificacionRepository;
        this.permisoService = permisoService;
        this.areaRepository = areaRepository;
        this.bajaService = bajaService;
        this.historialRepository = historialRepository;
    }

    public List<FichaMaquina> listarMaquinas() {
        return fichaMaquinaRepository.findByBajaFalseOrderByCodigoSenaAsc();
    }

    public FichaMaquina buscarMaquina(int idMaquina) {
        return fichaMaquinaRepository.findById(idMaquina).orElse(null);
    }

    public List<com.fimacor.catalogos.entity.Sede> listarSedes() {
        return sedeRepository.findAllByOrderByNombreSedeAsc();
    }

    public List<com.fimacor.catalogos.entity.AmbienteAprendizaje> listarAmbientes() {
        return ambienteRepository.findAllByOrderByIdAmbientesDeAprendizajeDesc();
    }

    public List<Usuario> listarTecnicosDisponibles() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.isActivo() && puedeAtenderComoTecnico(u))
                .toList();
    }

    public List<Novedad> listar() {
        return novedadRepository.findAllByOrderByIdNovedadesDesc();
    }

    public List<Area> listarAreas() {
        return areaRepository.findAllByOrderByNombreAreaAsc();
    }

    public List<Usuario> listarReportadores() {
        return usuarioRepository.findAll().stream()
                .filter(u -> {
                    String rol = u.getRol() != null ? normalizar(u.getRol().getDescripcionRol()) : "";
                    return rol.contains("administrador") || rol.contains("cuentadante")
                            || rol.contains("instructor") || rol.contains("logistica");
                })
                .sorted(java.util.Comparator
                        .comparing((Usuario u) -> u.getRol() != null ? u.getRol().getDescripcionRol() : "")
                        .thenComparing(Usuario::getNombre).thenComparing(Usuario::getApellido))
                .toList();
    }

    /** Filtra las novedades: un tecnico sin permiso de gestion solo ve sus propias
     * novedades asignadas, y con ?reparadas=1 solo las que ya finalizo o se cerraron. */
    public List<Novedad> listarParaVista(Usuario usuarioActual, boolean esTecnico, boolean puedeGestionar, boolean soloReparadas) {
        List<Novedad> todas = novedadRepository.findAllByOrderByIdNovedadesDesc();
        if (!esTecnico || puedeGestionar || usuarioActual == null) return todas;
        return todas.stream()
                .filter(n -> n.getTecnicoAsignado() != null && n.getTecnicoAsignado().getIdUsuario().equals(usuarioActual.getIdUsuario()))
                .filter(n -> !soloReparadas || esReparada(n.getEstadoFlujo()))
                .toList();
    }

    private boolean esReparada(String estado) {
        String limpio = estado == null ? "" : estado.trim();
        return "FINALIZADA_TECNICO".equals(limpio) || "CERRADA".equals(limpio);
    }

    public record Resultado(boolean ok, String error) {
        static Resultado exito() {
            return new Resultado(true, null);
        }
        static Resultado fallo(String motivo) {
            return new Resultado(false, motivo);
        }
    }

    @Transactional
    public Resultado reportar(int idMaquina, Integer idReportadoPor, Usuario usuarioSesion, int idSede, int idAmbiente,
            LocalDate fecha, String tipoFalla, String area, String descripcion, boolean solicitarBaja,
            boolean confirmarOtraFalla) {
        Usuario reportante = idReportadoPor != null && idReportadoPor > 0
                ? usuarioRepository.findById(idReportadoPor).orElse(usuarioSesion) : usuarioSesion;
        if (reportante == null) return Resultado.fallo("datos");
        FichaMaquina maquina = fichaMaquinaRepository.findById(idMaquina).orElse(null);
        var sede = sedeRepository.findById(idSede).orElse(null);
        var ambiente = ambienteRepository.findById(idAmbiente).orElse(null);
        if (maquina == null || maquina.isBaja() || sede == null || ambiente == null || fecha == null
                || area == null || area.isBlank() || tipoFalla == null || tipoFalla.isBlank()
                || descripcion == null || descripcion.isBlank()) {
            return Resultado.fallo("datos");
        }
        // Evita que la misma falla se reporte varias veces: si la maquina ya tiene novedades o
        // mantenimientos en curso, quien reporta debe confirmar que su falla es distinta.
        if (!confirmarOtraFalla && !pendientesDeMaquina(idMaquina).isEmpty()) return Resultado.fallo("pendientes");

        Novedad n = new Novedad();
        n.setFechaNovedad(fecha);
        n.setHoraNovedad(LocalTime.now());
        n.setDescripcionNovedad(limitar(descripcion, 500));
        n.setTipoFalla(limitar(tipoFalla, 80));
        n.setSede(sede);
        n.setArea(limitar(area, 100));
        n.setAmbiente(ambiente);
        n.setReportadoPor(reportante);
        n.setMaquina(maquina);
        n.setEstadoFlujo("REPORTADA");
        if (solicitarBaja) n.setSolicitudBaja("PENDIENTE");
        novedadRepository.save(n);

        ponerEstadoMantenimiento(maquina);
        if (solicitarBaja) notificarSolicitudBaja(n, usuarioSesion);
        return Resultado.exito();
    }

    /** Novedades sin cerrar y mantenimientos en curso de una maquina (una linea de texto por cada uno). */
    public List<String> pendientesDeMaquina(int idMaquina) {
        List<String> lineas = new ArrayList<>();
        for (Novedad n : novedadRepository.findByMaquina_IdMaquinasOrderByIdNovedadesDesc(idMaquina)) {
            if (novedadAbierta(n)) lineas.add(lineaNovedad(n));
        }
        for (HistorialMantenimiento h : historialRepository.findByMaquina_IdMaquinasOrderByIdHistorialMantenimientoDesc(idMaquina)) {
            if (MantenimientoTexto.enCurso(h)) lineas.add(lineaMantenimiento(h));
        }
        return lineas;
    }

    /** Lo mismo que pendientesDeMaquina, para todas las maquinas a la vez (solo las que tienen algo pendiente). */
    public Map<Integer, List<String>> pendientesPorMaquina() {
        Map<Integer, List<String>> resultado = new HashMap<>();
        for (Novedad n : novedadRepository.findAllByOrderByIdNovedadesDesc()) {
            if (n.getMaquina() != null && novedadAbierta(n)) {
                resultado.computeIfAbsent(n.getMaquina().getIdMaquinas(), k -> new ArrayList<>()).add(lineaNovedad(n));
            }
        }
        for (HistorialMantenimiento h : historialRepository.findAllByOrderByIdHistorialMantenimientoDesc()) {
            if (h.getMaquina() != null && MantenimientoTexto.enCurso(h)) {
                resultado.computeIfAbsent(h.getMaquina().getIdMaquinas(), k -> new ArrayList<>()).add(lineaMantenimiento(h));
            }
        }
        return resultado;
    }

    private boolean novedadAbierta(Novedad n) {
        String estado = n.getEstadoFlujo() == null ? "REPORTADA" : n.getEstadoFlujo().trim();
        return !"CERRADA".equals(estado) && !"CANCELADA".equals(estado);
    }

    private String lineaNovedad(Novedad n) {
        return "Novedad del " + n.getFechaNovedad() + " (" + NovedadTexto.estadoTexto(n.getEstadoFlujo()).toLowerCase() + "): "
                + NovedadTexto.textoCorto(n.getTipoFalla(), 30) + " - " + NovedadTexto.textoCorto(NovedadTexto.detalle(n.getDescripcionNovedad()), 90);
    }

    private String lineaMantenimiento(HistorialMantenimiento h) {
        String tecnico = h.getTecnicoEncargado() != null && !h.getTecnicoEncargado().isBlank() ? " con " + h.getTecnicoEncargado() : "";
        return "Mantenimiento en curso (" + MantenimientoTexto.estadoHistorialTexto(h).toLowerCase() + ")" + tecnico;
    }

    /** Avisa a los administradores que se pidio dar de baja una maquina. */
    private void notificarSolicitudBaja(Novedad n, Usuario solicitante) {
        for (Usuario admin : usuarioRepository.findByActivoTrueAndRol_DescripcionRolContainingIgnoreCase("administrador")) {
            notificar(admin, "Solicitud de baja de maquina",
                    nombreUsuario(solicitante) + " solicita dar de baja " + nombreMaquina(n.getMaquina()) + ".",
                    n.getIdNovedades(), "warn", "B");
        }
    }

    /** El Administrador acepta la solicitud: la maquina queda dada de baja y la novedad se cierra. */
    @Transactional
    public Resultado aprobarSolicitudBaja(int idNovedad, String motivo, Usuario administrador) {
        Novedad n = novedadRepository.findById(idNovedad).orElse(null);
        if (n == null || n.getMaquina() == null) return Resultado.fallo("datos");
        if (!"PENDIENTE".equals(n.getSolicitudBaja())) return Resultado.fallo("estado");
        BajaMaquinaService.Resultado baja = bajaService.darDeBaja(n.getMaquina().getIdMaquinas(), motivo, administrador, idNovedad);
        if (!baja.ok()) return Resultado.fallo(baja.error());

        n.setSolicitudBaja("APROBADA");
        n.setEstadoFlujo("CERRADA");
        n.setObservacionGestion(limitar("Baja aprobada: " + motivo, 500));
        novedadRepository.save(n);
        if (n.getReportadoPor() != null) {
            notificar(n.getReportadoPor(), "Baja de maquina aprobada",
                    nombreMaquina(n.getMaquina()) + " fue dada de baja por " + nombreUsuario(administrador) + ".",
                    idNovedad, "ok", "B");
        }
        return Resultado.exito();
    }

    /** El Administrador no acepta la solicitud: la novedad sigue su flujo normal de reparacion. */
    @Transactional
    public Resultado desestimarSolicitudBaja(int idNovedad, String observacion, Usuario administrador) {
        Novedad n = novedadRepository.findById(idNovedad).orElse(null);
        if (n == null) return Resultado.fallo("datos");
        if (!"PENDIENTE".equals(n.getSolicitudBaja())) return Resultado.fallo("estado");
        if (observacion == null || observacion.isBlank()) return Resultado.fallo("motivo");

        n.setSolicitudBaja("DESESTIMADA");
        n.setObservacionGestion(limitar("Solicitud de baja desestimada: " + observacion, 500));
        novedadRepository.save(n);
        if (n.getReportadoPor() != null) {
            notificar(n.getReportadoPor(), "Solicitud de baja desestimada",
                    "No se dio de baja " + nombreMaquina(n.getMaquina()) + ". Motivo: " + limitar(observacion, 120),
                    idNovedad, "warn", "B");
        }
        return Resultado.exito();
    }

    private static final List<String> ESTADOS_CERRADOS = List.of("FINALIZADA_TECNICO", "CERRADA", "CANCELADA");

    @Transactional
    public Resultado asignar(int idNovedad, int idTecnico, Usuario asignador, String observacion) {
        Novedad n = novedadRepository.findById(idNovedad).orElse(null);
        Usuario tecnico = usuarioRepository.findById(idTecnico).orElse(null);
        if (n == null || tecnico == null) return Resultado.fallo("datos");
        if (ESTADOS_CERRADOS.contains(n.getEstadoFlujo())) return Resultado.fallo("estado");

        n.setEstadoFlujo("ASIGNADA");
        n.setTecnicoAsignado(tecnico);
        n.setAsignadoPor(asignador);
        n.setFechaAsignacion(LocalDateTime.now());
        n.setObservacionGestion(limitar(observacion, 500));
        novedadRepository.save(n);
        ponerEstadoMantenimiento(n.getMaquina());

        notificar(tecnico, "Reparacion asignada",
                "Tienes asignada la reparacion de " + nombreMaquina(n.getMaquina()) + ". Asignado por: " + nombreUsuario(asignador) + ".",
                idNovedad, "warn", "R");
        notificarAsignacionAdministradores(n, tecnico);
        return Resultado.exito();
    }

    /** Avisa a los administradores que un tecnico quedo asignado a una novedad. */
    private void notificarAsignacionAdministradores(Novedad n, Usuario tecnico) {
        for (Usuario admin : usuarioRepository.findByActivoTrueAndRol_DescripcionRolContainingIgnoreCase("administrador")) {
            notificar(admin, "Tecnico asignado a novedad",
                    nombreUsuario(tecnico) + " fue asignado para reparar " + nombreMaquina(n.getMaquina()) + ".",
                    n.getIdNovedades(), "warn", "R");
        }
    }

    /** Avisa que una maquina volvio a servicio: al tecnico, a los administradores, y a quien la aprobo si fue Logistica. */
    private void notificarMaquinaPuestaEnServicio(Novedad n, Usuario usuarioOperacion, boolean operacionEsLogistica) {
        String maquinaTexto = nombreMaquina(n.getMaquina());
        if (n.getTecnicoAsignado() != null) {
            notificar(n.getTecnicoAsignado(), "Maquina aprobada y puesta en servicio",
                    maquinaTexto + " fue aprobada por logistica y ya esta en servicio.", n.getIdNovedades(), "ok", "R");
        }
        for (Usuario admin : usuarioRepository.findByActivoTrueAndRol_DescripcionRolContainingIgnoreCase("administrador")) {
            notificar(admin, "Maquina puesta en servicio", maquinaTexto + " fue aprobada y puesta en servicio.",
                    n.getIdNovedades(), "ok", "M");
        }
        if (usuarioOperacion != null && operacionEsLogistica) {
            notificar(usuarioOperacion, "Maquina puesta en servicio", "Pusiste " + maquinaTexto + " en servicio.",
                    n.getIdNovedades(), "ok", "M");
        }
    }

    private void notificar(Usuario destinatario, String titulo, String mensaje, int idNovedad, String tipo, String icono) {
        NotificacionUsuario notif = new NotificacionUsuario();
        notif.setUsuario(destinatario);
        notif.setTitulo(titulo);
        notif.setMensaje(mensaje);
        notif.setEnlace("/novedades?novedad=" + idNovedad);
        notif.setTipo(tipo);
        notif.setIcono(icono);
        notificacionRepository.save(notif);
    }

    @Transactional
    public Resultado iniciar(int idNovedad, Usuario usuarioActual, boolean esRolTotal) {
        Novedad n = novedadRepository.findById(idNovedad).orElse(null);
        if (n == null) return Resultado.fallo("datos");
        boolean asignadoAlUsuario = esRolTotal || (n.getTecnicoAsignado() != null && n.getTecnicoAsignado().getIdUsuario().equals(usuarioActual.getIdUsuario()));
        boolean estadoValido = List.of("ASIGNADA", "DEVUELTA", "REPORTADA").contains(n.getEstadoFlujo());
        if (!asignadoAlUsuario || !estadoValido) return Resultado.fallo("estado");

        n.setEstadoFlujo("EN_REPARACION");
        n.setFechaInicioReparacion(LocalDateTime.now());
        novedadRepository.save(n);
        return Resultado.exito();
    }

    @Transactional
    public Resultado finalizar(int idNovedad, Usuario usuarioActual, boolean esRolTotal, String observacion) {
        Novedad n = novedadRepository.findById(idNovedad).orElse(null);
        if (n == null || observacion == null || observacion.isBlank()) return Resultado.fallo("datos");
        boolean asignadoAlUsuario = esRolTotal || (n.getTecnicoAsignado() != null && n.getTecnicoAsignado().getIdUsuario().equals(usuarioActual.getIdUsuario()));
        if (!asignadoAlUsuario || !"EN_REPARACION".equals(n.getEstadoFlujo())) return Resultado.fallo("estado");

        n.setEstadoFlujo("FINALIZADA_TECNICO");
        n.setFechaFinReparacion(LocalDateTime.now());
        n.setObservacionReparacion(limitar(observacion, 500));
        novedadRepository.save(n);
        return Resultado.exito();
    }

    @Transactional
    public Resultado cerrar(int idNovedad, String observacion, Usuario usuarioOperacion, boolean operacionEsLogistica) {
        Novedad n = novedadRepository.findById(idNovedad).orElse(null);
        if (n == null) return Resultado.fallo("datos");
        n.setEstadoFlujo("CERRADA");
        if ("PENDIENTE".equals(n.getSolicitudBaja())) n.setSolicitudBaja("DESESTIMADA");
        n.setObservacionGestion(limitar(observacion, 500));
        novedadRepository.save(n);
        ponerEstadoOperativo(n.getMaquina());
        notificarMaquinaPuestaEnServicio(n, usuarioOperacion, operacionEsLogistica);
        return Resultado.exito();
    }

    @Transactional
    public Resultado devolver(int idNovedad, String observacion) {
        Novedad n = novedadRepository.findById(idNovedad).orElse(null);
        if (n == null) return Resultado.fallo("datos");
        n.setEstadoFlujo("DEVUELTA");
        n.setObservacionGestion(limitar(observacion, 500));
        novedadRepository.save(n);
        ponerEstadoMantenimiento(n.getMaquina());
        return Resultado.exito();
    }

    @Transactional
    public Resultado cancelar(int idNovedad, String observacion) {
        Novedad n = novedadRepository.findById(idNovedad).orElse(null);
        if (n == null) return Resultado.fallo("datos");
        n.setEstadoFlujo("CANCELADA");
        if ("PENDIENTE".equals(n.getSolicitudBaja())) n.setSolicitudBaja("DESESTIMADA");
        n.setObservacionGestion(limitar(observacion, 500));
        novedadRepository.save(n);
        ponerEstadoOperativo(n.getMaquina());
        return Resultado.exito();
    }

    public boolean eliminar(int idNovedad) {
        if (!novedadRepository.existsById(idNovedad)) return false;
        novedadRepository.deleteById(idNovedad);
        return true;
    }

    public Novedad buscar(int idNovedad) {
        return novedadRepository.findById(idNovedad).orElse(null);
    }

    public boolean puedeGestionarNovedad(int idRol) {
        return permisoService.esRolTotal(idRol)
                || permisoService.tienePermisoConTodas(idRol, "novedades", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "novedad", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "novedades", "asignar")
                || permisoService.tienePermisoConTodas(idRol, "novedad", "asignar");
    }

    public boolean puedeAtenderComoTecnico(Usuario usuario) {
        return permisoService.puedeAtenderComoTecnico(usuario);
    }

    private void ponerEstadoMantenimiento(FichaMaquina maquina) {
        cambiarEstado(maquina, texto -> texto.contains("dan") || texto.contains("aver") || texto.contains("fall")
                || texto.contains("mantenimiento") || texto.contains("fuera"));
    }

    private void ponerEstadoOperativo(FichaMaquina maquina) {
        cambiarEstado(maquina, texto -> texto.contains("activo") || texto.contains("operativ") || texto.contains("disponible")
                || texto.contains("funcional"));
    }

    private void cambiarEstado(FichaMaquina maquina, java.util.function.Predicate<String> coincide) {
        for (EstadoMaquina e : estadoMaquinaRepository.findAll()) {
            String texto = normalizar(e.getNombreTipo() + " " + e.getDescripcionEstado());
            if (coincide.test(texto)) {
                maquina.setEstado(e);
                fichaMaquinaRepository.save(maquina);
                return;
            }
        }
    }

    private String nombreMaquina(FichaMaquina maquina) {
        return maquina == null ? "Maquina no registrada" : maquina.getCodigoSena() + " - " + maquina.getDescripcion();
    }

    private String nombreUsuario(Usuario usuario) {
        return usuario == null ? "No registrado" : usuario.getNombre() + " " + usuario.getApellido();
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private String limitar(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
