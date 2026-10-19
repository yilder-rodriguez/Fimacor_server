package com.fimacor.mantenimiento.service;

import com.fimacor.catalogos.entity.EstadoMaquina;
import com.fimacor.catalogos.repository.EstadoMaquinaRepository;
import com.fimacor.comun.archivos.FileStorageService;
import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.repository.FichaMaquinaRepository;
import com.fimacor.usuarios.entity.NotificacionUsuario;
import com.fimacor.usuarios.repository.NotificacionUsuarioRepository;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.UsuarioRepository;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fimacor.mantenimiento.entity.HistorialMantenimiento;
import com.fimacor.mantenimiento.entity.Mantenimiento;
import com.fimacor.mantenimiento.entity.TipoMantenimiento;
import com.fimacor.mantenimiento.repository.HistorialMantenimientoRepository;
import com.fimacor.mantenimiento.repository.MantenimientoRepository;
import com.fimacor.mantenimiento.repository.TipoMantenimientoRepository;
import com.fimacor.mantenimiento.util.MantenimientoTexto;

/**
 * Programa mantenimientos, confirma reparaciones y registra reparaciones
 * rapidas sin mantenimiento previo. El estado de una reparacion queda
 * codificado como texto dentro de descripcionAccion (ver nota en
 * HistorialMantenimiento). El aviso por WhatsApp todavia no esta conectado.
 */
@Service
public class MantenimientoService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CARPETA_EVIDENCIAS = "evidencias-mantenimiento";

    private final MantenimientoRepository mantenimientoRepository;
    private final HistorialMantenimientoRepository historialRepository;
    private final TipoMantenimientoRepository tipoMantenimientoRepository;
    private final FichaMaquinaRepository fichaMaquinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoMaquinaRepository estadoMaquinaRepository;
    private final NotificacionUsuarioRepository notificacionRepository;
    private final PermisoService permisoService;
    private final FileStorageService fileStorageService;
    private final JdbcTemplate jdbcTemplate;

    public MantenimientoService(MantenimientoRepository mantenimientoRepository,
            HistorialMantenimientoRepository historialRepository, TipoMantenimientoRepository tipoMantenimientoRepository,
            FichaMaquinaRepository fichaMaquinaRepository, UsuarioRepository usuarioRepository,
            EstadoMaquinaRepository estadoMaquinaRepository, NotificacionUsuarioRepository notificacionRepository,
            PermisoService permisoService, FileStorageService fileStorageService, JdbcTemplate jdbcTemplate) {
        this.mantenimientoRepository = mantenimientoRepository;
        this.historialRepository = historialRepository;
        this.tipoMantenimientoRepository = tipoMantenimientoRepository;
        this.fichaMaquinaRepository = fichaMaquinaRepository;
        this.usuarioRepository = usuarioRepository;
        this.estadoMaquinaRepository = estadoMaquinaRepository;
        this.notificacionRepository = notificacionRepository;
        this.permisoService = permisoService;
        this.fileStorageService = fileStorageService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TipoMantenimiento> listarTipos() {
        return tipoMantenimientoRepository.findAllByOrderByIdTipoMantenientoAsc();
    }

    public List<FichaMaquina> listarMaquinas() {
        return fichaMaquinaRepository.findByBajaFalseOrderByCodigoSenaAsc();
    }

    public List<Mantenimiento> listarProgramados() {
        return mantenimientoRepository.findAllByOrderByIdMantenimientoDesc();
    }

    public List<Mantenimiento> listarPorResponsable(int idUsuario) {
        return mantenimientoRepository.findByTecnico_IdUsuarioOrderByIdMantenimientoDesc(idUsuario);
    }

    public boolean puedeSerResponsable(Usuario usuario) {
        return permisoService.puedeAtenderComoTecnico(usuario);
    }

    public org.springframework.core.io.Resource cargarEvidencia(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) return null;
        try {
            var recurso = fileStorageService.cargar(CARPETA_EVIDENCIAS, nombreArchivo);
            return recurso != null && recurso.exists() ? recurso : null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<HistorialMantenimiento> listarHistorial() {
        return historialRepository.findAllByOrderByIdHistorialMantenimientoDesc();
    }

    public HistorialMantenimiento buscarHistorial(int id) {
        return historialRepository.findById(id).orElse(null);
    }

    /** Usuarios que pueden ser responsables de una reparacion (permiso reparacion.reparar o mantenimiento.confirmar). */
    public List<Usuario> listarTecnicosDisponibles() {
        List<Usuario> resultado = new ArrayList<>();
        for (Usuario u : usuarioRepository.findAll()) {
            if (u.isActivo() && puedeSerResponsable(u)) resultado.add(u);
        }
        return resultado;
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
    public Resultado registrarProgramado(int idMaquina, LocalDate fecha, String hora, String frecuencia, int idTipo,
            int idTecnico, String tareas, Usuario programador, List<EvidenciaArchivo> evidencias) {
        FichaMaquina maquina = fichaMaquinaRepository.findById(idMaquina).orElse(null);
        TipoMantenimiento tipo = tipoMantenimientoRepository.findById(idTipo).orElse(null);
        Usuario tecnico = usuarioRepository.findById(idTecnico).orElse(null);
        if (maquina == null || maquina.isBaja() || tipo == null || tecnico == null || tareas == null || tareas.isBlank()) {
            return Resultado.fallo("datos");
        }
        if (!puedeSerResponsable(tecnico)) return Resultado.fallo("tecnico");

        LocalDate proxima = calcularProximaFecha(fecha, frecuencia);
        String evidenciaRutas = guardarEvidencias(evidencias);

        Mantenimiento m = new Mantenimiento();
        m.setFechaMantenimiento(fecha);
        m.setFechaProximoMantenimiento(proxima);
        m.setDescripcionMantenimiento(limitar(tipo.getDescripcionMantenimiento() + " | Asignado a "
                + tecnico.getNombre() + " " + tecnico.getApellido(), 90));
        m.setFrecuencia(frecuencia);
        m.setHoraProgramada(hora != null && hora.matches("\\d{2}:\\d{2}") ? hora : null);
        m.setTareas(limitar(tareas, 255));
        m.setEvidenciaMantenimiento(evidenciaRutas);
        m.setTipoMantenimiento(tipo);
        m.setMaquina(maquina);
        m.setTecnico(tecnico);
        m.setProgramadoPor(programador);
        mantenimientoRepository.save(m);

        String nombreAsignador = programador != null ? programador.getNombre() + " " + programador.getApellido() : "Usuario del sistema";
        String nombreTecnico = tecnico.getNombre() + " " + tecnico.getApellido();
        String ahora = LocalDateTime.now().format(FECHA_HORA);

        HistorialMantenimiento h = new HistorialMantenimiento();
        h.setIdHistorialMantenimiento(siguienteIdHistorial());
        h.setTipoMaquina(limitar(maquina.getDescripcion(), 45));
        h.setFechaInicio(ahora);
        h.setFechaFinal(proxima.toString());
        h.setTecnicoEncargado(limitar(nombreTecnico, 45));
        h.setDescripcionAccion(limitar(
                "ESTADO: ASIGNADO"
                + " | Tipo mantenimiento: " + tipo.getDescripcionMantenimiento()
                + " | Asignado por: " + nombreAsignador
                + " | Responsable asignado: " + nombreTecnico
                + " | Fecha asignacion: " + ahora
                + " | Fecha programada: " + fecha
                + (m.getHoraProgramada() != null ? " | Hora programada: " + m.getHoraProgramada() : "")
                + " | Proximo mantenimiento: " + proxima
                + " | Frecuencia: " + frecuencia
                + " | Tareas: " + tareas
                + (evidenciaRutas != null && !evidenciaRutas.isEmpty() ? " | Evidencia: " + evidenciaRutas : "")
                + " | Confirmacion tecnica: pendiente", 1200));
        h.setMaquina(maquina);
        h.setTecnico(tecnico);
        historialRepository.save(h);
        return Resultado.exito();
    }

    /** Transiciones de estado: "iniciar" (tecnico empieza), "finalizar" (tecnico termina), "cerrar" (logistica cierra). */
    @Transactional
    public Resultado confirmarAccion(int idHistorial, String accion, String observacionTexto, Usuario usuarioActual) {
        HistorialMantenimiento historial = historialRepository.findById(idHistorial).orElse(null);
        if (historial == null) return Resultado.fallo("historial");

        int idRol = usuarioActual.getRol() != null ? usuarioActual.getRol().getIdRol() : 0;
        boolean esTecnicoRol = normalizar(usuarioActual.getRol() != null ? usuarioActual.getRol().getDescripcionRol() : "").contains("tecnico");
        boolean permisoReparar = esTecnicoRol || puedeSerResponsable(usuarioActual);
        boolean puedeConfirmarCualquierAsignacion = permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "reparacion", "gestionar");
        boolean logistica = normalizar(usuarioActual.getRol() != null ? usuarioActual.getRol().getDescripcionRol() : "").contains("logistica")
                || permisoService.esRolTotal(idRol);
        if (!permisoReparar && !puedeConfirmarCualquierAsignacion && !logistica) return Resultado.fallo("permiso");
        boolean asignadoAlUsuario = historial.getTecnico() != null && historial.getTecnico().getIdUsuario().equals(usuarioActual.getIdUsuario());
        if (!logistica && !puedeConfirmarCualquierAsignacion && !asignadoAlUsuario) return Resultado.fallo("permiso");

        String nombreTecnico = usuarioActual.getNombre() + " " + usuarioActual.getApellido();
        String observacion = observacionTexto == null || observacionTexto.isBlank() ? "Reparacion confirmada por el tecnico." : observacionTexto.trim();
        String descripcionActual = historial.getDescripcionAccion() == null ? "" : historial.getDescripcionAccion();
        String momento = LocalDateTime.now().format(FECHA_HORA);
        boolean cerrar = false;

        if ("iniciar".equalsIgnoreCase(accion)) {
            if (!historial.estaAsignado()) return Resultado.fallo("error");
            historial.setDescripcionAccion(descripcionActual.replace("ESTADO: ASIGNADO", "ESTADO: EN MANTENIMIENTO") + " | Inicio tecnico: " + momento);
        } else if ("cerrar".equalsIgnoreCase(accion)) {
            if (!logistica || !descripcionActual.contains("ESTADO: FINALIZADO")) return Resultado.fallo("error");
            historial.setDescripcionAccion(descripcionActual.replace("ESTADO: FINALIZADO", "ESTADO: CERRADO") + " | Cerrado por Logistica: " + nombreTecnico + " | Observacion cierre: " + observacion);
            historial.setFechaFinal(momento);
            cerrar = true;
        } else {
            if (!descripcionActual.contains("ESTADO: EN MANTENIMIENTO")) return Resultado.fallo("error");
            historial.setDescripcionAccion(descripcionActual.replace("ESTADO: EN MANTENIMIENTO", "ESTADO: FINALIZADO")
                    + " | Reparado por: " + nombreTecnico + " | Fecha reparacion: " + momento + " | Observacion tecnica: " + observacion);
            historial.setFechaFinal(momento);
        }
        historialRepository.save(historial);

        if ("finalizar".equalsIgnoreCase(accion) || (!"iniciar".equalsIgnoreCase(accion) && !"cerrar".equalsIgnoreCase(accion))) {
            notificarLogistica(historial, nombreTecnico);
        }
        if (cerrar) {
            actualizarEstadoOperativo(historial.getMaquina().getIdMaquinas());
        }
        return Resultado.exito();
    }

    /** Reparacion rapida sin mantenimiento programado previo. */
    /** Una maquina es "suya" para un tecnico si tiene un mantenimiento o una reparacion sin terminar asignados a el. */
    public boolean tieneAsignada(int idMaquina, Usuario tecnico) {
        if (tecnico == null || tecnico.getIdUsuario() == null) return false;
        for (Mantenimiento m : mantenimientoRepository.findByMaquina_IdMaquinasOrderByIdMantenimientoDesc(idMaquina)) {
            if (m.getTecnico() != null && tecnico.getIdUsuario().equals(m.getTecnico().getIdUsuario())) return true;
        }
        for (HistorialMantenimiento h : historialRepository.findByMaquina_IdMaquinasOrderByIdHistorialMantenimientoDesc(idMaquina)) {
            if (MantenimientoTexto.estaAsignadoHistorial(h) && MantenimientoTexto.asignadoAlUsuario(h, tecnico)) return true;
        }
        Long novedades = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM novedades WHERE Ficha_Maquina_idMaquinas = ? "
                + "AND tecnico_asignado_id = ? AND estado_flujo IN ('ASIGNADA', 'EN_REPARACION', 'DEVUELTA')",
                Long.class, idMaquina, tecnico.getIdUsuario());
        return novedades != null && novedades > 0;
    }

    @Transactional
    public Resultado repararRapido(int idMaquina, String observacion, Usuario usuarioActual) {
        FichaMaquina maquina = fichaMaquinaRepository.findById(idMaquina).orElse(null);
        EstadoMaquina operativo = estadoOperativo();
        if (maquina == null || operativo == null) return Resultado.fallo("error");

        maquina.setEstado(operativo);
        fichaMaquinaRepository.save(maquina);

        String nombreTecnico = usuarioActual != null ? usuarioActual.getNombre() + " " + usuarioActual.getApellido() : "Responsable";
        String ahora = LocalDateTime.now().format(FECHA_HORA);
        String detalle = observacion != null && !observacion.isBlank() ? " | DATOS: " + limitar(observacion, 45) : "";

        HistorialMantenimiento h = new HistorialMantenimiento();
        h.setIdHistorialMantenimiento(siguienteIdHistorial());
        h.setTipoMaquina(limitar(maquina.getDescripcion(), 45));
        h.setFechaInicio(ahora);
        h.setFechaFinal(ahora);
        h.setTecnicoEncargado(limitar(nombreTecnico, 45));
        h.setDescripcionAccion(limitar("ESTADO: REPARADO | EN SERVICIO" + detalle, 1200));
        h.setMaquina(maquina);
        h.setTecnico(usuarioActual);
        historialRepository.save(h);
        return Resultado.exito();
    }

    public boolean cambiarTecnico(int idMantenimiento, int idResponsable) {
        Mantenimiento actual = mantenimientoRepository.findById(idMantenimiento).orElse(null);
        Usuario responsable = usuarioRepository.findById(idResponsable).orElse(null);
        if (actual == null || responsable == null) return false;
        String nombreTipo = actual.getTipoMantenimiento() != null ? actual.getTipoMantenimiento().getDescripcionMantenimiento() : "Mantenimiento";
        actual.setTecnico(responsable);
        actual.setDescripcionMantenimiento(limitar(nombreTipo + " | Asignado a " + responsable.getNombre() + " " + responsable.getApellido(), 90));
        mantenimientoRepository.save(actual);
        return true;
    }

    public boolean reprogramar(int idMantenimiento, LocalDate fecha, LocalDate proxima, String frecuencia, String tareas) {
        if (fecha == null || proxima == null || frecuencia == null || frecuencia.isBlank() || tareas == null || tareas.isBlank()) return false;
        Mantenimiento actual = mantenimientoRepository.findById(idMantenimiento).orElse(null);
        if (actual == null) return false;
        actual.setFechaMantenimiento(fecha);
        actual.setFechaProximoMantenimiento(proxima);
        actual.setFrecuencia(limitar(frecuencia, 45));
        actual.setTareas(limitar(tareas, 255));
        mantenimientoRepository.save(actual);
        return true;
    }

    /** Cancela mantenimientos programados, limpiando primero sus reportes asociados. */
    @Transactional
    public boolean cancelar(List<Integer> ids) {
        boolean eliminoAlMenosUno = false;
        for (Integer id : ids) {
            jdbcTemplate.update("DELETE FROM reporte_mantenimientos WHERE Mantenimiento_idMantenimiento = ?", id);
            int filas = jdbcTemplate.update("DELETE FROM mantenimiento WHERE idMantenimiento = ?", id);
            if (filas > 0) eliminoAlMenosUno = true;
        }
        return eliminoAlMenosUno;
    }

    public boolean eliminarHistorial(int idHistorial) {
        if (!historialRepository.existsById(idHistorial)) return false;
        historialRepository.deleteById(idHistorial);
        return true;
    }

    private void notificarLogistica(HistorialMantenimiento historial, String tecnico) {
        for (Usuario u : usuarioRepository.findAll()) {
            if (u.getRol() == null || !normalizar(u.getRol().getDescripcionRol()).contains("logistica")) continue;
            NotificacionUsuario n = new NotificacionUsuario();
            n.setUsuario(u);
            n.setTitulo("Reparacion pendiente de aprobacion");
            n.setMensaje("El tecnico " + tecnico + " finalizo una reparacion. Revisala y cierrala.");
            n.setEnlace("/mantenimiento/historial?historial=" + historial.getIdHistorialMantenimiento());
            n.setTipo("warn");
            n.setIcono("R");
            notificacionRepository.save(n);
        }
    }

    private void actualizarEstadoOperativo(int idMaquina) {
        EstadoMaquina operativo = estadoOperativo();
        if (operativo == null) return;
        fichaMaquinaRepository.findById(idMaquina).ifPresent(maquina -> {
            maquina.setEstado(operativo);
            fichaMaquinaRepository.save(maquina);
        });
    }

    /** Igual a Estado_MaquinaDAO.obtenerEstadoOperativo() del sistema original: sin "funcional"
     * ni "en servicio" nunca encontraba el estado real (el catalogo de produccion usa
     * "FUNCIONAL", no "activo"/"operativo"/"disponible"), asi que la maquina se quedaba
     * marcada en mantenimiento para siempre aunque la reparacion se confirmara. */
    private EstadoMaquina estadoOperativo() {
        for (EstadoMaquina e : estadoMaquinaRepository.findAll()) {
            String texto = normalizar(e.getNombreTipo() + " " + e.getDescripcionEstado());
            boolean fueraDeServicio = texto.contains("fuera") && texto.contains("servicio");
            if (!fueraDeServicio && (texto.contains("operativ") || texto.contains("activo")
                    || texto.contains("disponible") || texto.contains("funcional")
                    || texto.contains("en servicio"))) {
                return e;
            }
        }
        return null;
    }

    public record EvidenciaArchivo(String nombreOriginal, InputStream contenido) {
    }

    private String guardarEvidencias(List<EvidenciaArchivo> evidencias) {
        if (evidencias == null || evidencias.isEmpty()) return "";
        List<String> rutas = new ArrayList<>();
        for (EvidenciaArchivo evidencia : evidencias) {
            if (evidencia.contenido() == null || evidencia.nombreOriginal() == null) continue;
            String extension = extension(evidencia.nombreOriginal());
            if (!extension.matches("\\.(png|jpg|jpeg|webp)")) continue;
            String nombreSeguro = "mantenimiento_" + System.currentTimeMillis() + "_" + rutas.size() + extension;
            try {
                fileStorageService.guardar(CARPETA_EVIDENCIAS, nombreSeguro, evidencia.contenido());
                rutas.add(nombreSeguro);
            } catch (IOException ignorado) {
                // Se omite este archivo si falla el guardado; los demas continuan.
            }
        }
        return String.join(";", rutas);
    }

    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto >= 0 ? nombre.substring(punto).toLowerCase() : "";
    }

    private LocalDate calcularProximaFecha(LocalDate fecha, String frecuencia) {
        if ("trimestral".equals(frecuencia)) return fecha.plusMonths(3);
        if ("semestral".equals(frecuencia)) return fecha.plusMonths(6);
        if ("anual".equals(frecuencia)) return fecha.plusYears(1);
        return fecha.plusMonths(1);
    }

    private int siguienteIdHistorial() {
        return historialRepository.findAll().stream().mapToInt(HistorialMantenimiento::getIdHistorialMantenimiento).max().orElse(0) + 1;
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private String limitar(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
