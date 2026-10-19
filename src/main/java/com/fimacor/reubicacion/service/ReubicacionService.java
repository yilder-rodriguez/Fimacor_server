package com.fimacor.reubicacion.service;

import com.fimacor.catalogos.entity.AmbienteAprendizaje;
import com.fimacor.catalogos.repository.AmbienteAprendizajeRepository;
import com.fimacor.catalogos.repository.AreaRepository;
import com.fimacor.catalogos.entity.Sede;
import com.fimacor.catalogos.repository.SedeRepository;
import com.fimacor.comun.archivos.FileStorageService;
import com.fimacor.comun.correo.EmailService;
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
import java.util.Base64;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fimacor.catalogos.entity.Area;
import com.fimacor.reubicacion.entity.ReubicacionMaquina;
import com.fimacor.reubicacion.entity.UbicacionMaquina;
import com.fimacor.reubicacion.repository.ReubicacionMaquinaRepository;
import com.fimacor.reubicacion.repository.UbicacionMaquinaRepository;

/**
 * Logica de reubicacion: registrar la solicitud, responder (aprobar o
 * rechazar), el correo de solicitud/aprobacion con el PDF adjunto, el
 * acta descargable y la firma digital servida como imagen.
 */
@Service
public class ReubicacionService {

    private static final String CARPETA_EVIDENCIAS = "evidencias-reubicacion";

    private final ReubicacionMaquinaRepository reubicacionRepository;
    private final UbicacionMaquinaRepository ubicacionRepository;
    private final FichaMaquinaRepository fichaMaquinaRepository;
    private final AmbienteAprendizajeRepository ambienteRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionUsuarioRepository notificacionRepository;
    private final PermisoService permisoService;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final ReubicacionPdfBuilder pdfBuilder;
    private final AreaRepository areaRepository;

    public ReubicacionService(ReubicacionMaquinaRepository reubicacionRepository, UbicacionMaquinaRepository ubicacionRepository,
            FichaMaquinaRepository fichaMaquinaRepository, AmbienteAprendizajeRepository ambienteRepository,
            SedeRepository sedeRepository, UsuarioRepository usuarioRepository,
            NotificacionUsuarioRepository notificacionRepository, PermisoService permisoService,
            FileStorageService fileStorageService, EmailService emailService, ReubicacionPdfBuilder pdfBuilder,
            AreaRepository areaRepository) {
        this.reubicacionRepository = reubicacionRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.fichaMaquinaRepository = fichaMaquinaRepository;
        this.ambienteRepository = ambienteRepository;
        this.sedeRepository = sedeRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
        this.permisoService = permisoService;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
        this.pdfBuilder = pdfBuilder;
        this.areaRepository = areaRepository;
    }

    public List<FichaMaquina> listarMaquinas() {
        return fichaMaquinaRepository.findByBajaFalseOrderByCodigoSenaAsc();
    }

    public List<AmbienteAprendizaje> listarAmbientes() {
        return ambienteRepository.findAllByOrderByIdAmbientesDeAprendizajeDesc();
    }

    public List<Sede> listarSedes() {
        return sedeRepository.findAllByOrderByNombreSedeAsc();
    }

    public List<ReubicacionMaquina> listar() {
        return reubicacionRepository.findAllByOrderByIdRehubicacionMaquinaDesc();
    }

    public List<com.fimacor.catalogos.entity.Area> listarAreas() {
        return areaRepository.findAllByOrderByNombreAreaAsc();
    }

    public record Resultado(boolean ok, String error, boolean correoEnviado) {
        static Resultado exito() {
            return new Resultado(true, null, true);
        }
        static Resultado exito(boolean correoEnviado) {
            return new Resultado(true, null, correoEnviado);
        }
        static Resultado fallo(String motivo) {
            return new Resultado(false, motivo, false);
        }
    }

    @Transactional
    public Resultado registrar(int idMaquina, String tipoTrasladoParam, String origen, int idAmbienteDestino,
            int idSedeDestinoParam, String areaDestino, String destino, String observacion, String descripcion,
            String nombreArchivoEvidencia, InputStream evidenciaContenido, Usuario solicitante) {
        String tipoTraslado = "Externo".equalsIgnoreCase(tipoTrasladoParam) ? "Externo" : "Interno";
        boolean externo = "Externo".equals(tipoTraslado);

        int idSedeDestino = idSedeDestinoParam;
        if (!externo) {
            Sede complejoSur = idSedeComplejoSur();
            if (complejoSur == null) return Resultado.fallo("sede_interna");
            idSedeDestino = complejoSur.getIdSede();
        }

        FichaMaquina maquina = fichaMaquinaRepository.findById(idMaquina).orElse(null);
        AmbienteAprendizaje ambienteDestino = ambienteRepository.findById(idAmbienteDestino).orElse(null);
        Sede sedeDestino = sedeRepository.findById(idSedeDestino).orElse(null);
        if (maquina == null || maquina.isBaja() || ambienteDestino == null || sedeDestino == null || origen == null || origen.isBlank()
                || destino == null || destino.isBlank() || areaDestino == null || areaDestino.isBlank()
                || descripcion == null || descripcion.isBlank()) {
            return Resultado.fallo("datos");
        }

        String evidencia = guardarEvidencia(nombreArchivoEvidencia, evidenciaContenido);
        if (evidencia == null || evidencia.isBlank()) return Resultado.fallo("evidencia");

        ReubicacionMaquina r = new ReubicacionMaquina();
        r.setMaquina(maquina);
        r.setFecha(LocalDate.now().toString());
        r.setTipoTraslado(tipoTraslado);
        r.setOrigenTexto(limitar(origen, 45));
        r.setDestinoTexto(limitar(destino, 45));
        r.setAutorizadoPor("Subdireccion");
        r.setAutorizacion("Pendiente de firma electronica de Subdireccion");
        r.setObservacion(limitar(observacion, 500));
        r.setEvidencia(evidencia);
        r.setDescripcionReubicacion(limitar(descripcion, 500));
        r.setEstadoAutorizacion("Pendiente");
        r.setAmbienteDestino(ambienteDestino);
        r.setSedeDestino(sedeDestino);
        r.setAreaDestino(limitar(areaDestino, 45));
        reubicacionRepository.save(r);

        notificarResponsablesAutorizacion(r, tipoTraslado, origen, destino, descripcion);
        boolean correoEnviado = enviarCorreosSolicitud(r, solicitante);
        return Resultado.exito(correoEnviado);
    }

    @Transactional
    public Resultado responder(int idReubicacion, boolean aprobar, String firmaBase64, String responsableNombre,
            String observacionDecision, int idRol) {
        ReubicacionMaquina r = reubicacionRepository.findById(idReubicacion).orElse(null);
        if (r == null || !"Pendiente".equalsIgnoreCase(r.getEstadoAutorizacion())) return Resultado.fallo("estado");

        boolean externo = "Externo".equalsIgnoreCase(r.getTipoTraslado());
        String rolActual = normalizar(rolTexto(idRol));
        boolean puedeAdministrador = permisoService.esRolTotal(idRol);
        boolean puedeSubdireccionExterno = externo && rolActual.contains("subdireccion");
        boolean puedeCuentadanteInterna = !externo && rolActual.contains("cuentadante");
        if (!puedeAdministrador && !puedeSubdireccionExterno && !puedeCuentadanteInterna) return Resultado.fallo("sin_permiso");

        if (aprobar && (r.getEvidencia() == null || r.getEvidencia().isBlank())) return Resultado.fallo("firma_requerida");

        // La observacion es obligatoria tanto al aprobar como al rechazar: deja constancia de la decision.
        if (observacionDecision == null || observacionDecision.isBlank()) return Resultado.fallo("observacion");

        byte[] firmaImagen = decodificarFirma(firmaBase64);
        if (firmaImagen == null) return Resultado.fallo("firma_error");

        String estado = aprobar ? "Aprobado" : "Rechazado";
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String respuesta = "Decision: " + estado
                + " | Firma electronica declarada: " + responsableNombre
                + " | Usuario FIMACOR: " + responsableNombre
                + " | Fecha y hora: " + fechaHora
                + (observacionDecision != null && !observacionDecision.isBlank() ? " | Observacion: " + observacionDecision : "")
                + " | Firma manuscrita: registrada en el acta.";

        r.setFirmaDigital(firmaImagen);

        if (aprobar) {
            if (!aplicarMovimientoAprobado(r)) return Resultado.fallo("aplicar");
        }

        r.setEstadoAutorizacion(estado);
        r.setRespuestaAutorizacion(respuesta);
        r.setFechaRespuestaAutorizacion(LocalDateTime.now());
        r.setAutorizacion(estado + " con firma electronica de Subdireccion: " + responsableNombre);
        reubicacionRepository.save(r);

        if (aprobar) {
            notificarResponsablesAprobacion(r, responsableNombre);
            enviarCorreosActaFinalizada(r, responsableNombre, firmaImagen);
        }
        return Resultado.exito();
    }

    private boolean aplicarMovimientoAprobado(ReubicacionMaquina r) {
        if (r.getMaquina() == null || r.getAmbienteDestino() == null || r.getSedeDestino() == null
                || r.getAreaDestino() == null || r.getAreaDestino().isBlank()) {
            return false;
        }
        UbicacionMaquina ubicacion = new UbicacionMaquina();
        ubicacion.setFecha(LocalDate.now().toString());
        ubicacion.setAmbiente(r.getDestinoTexto());
        ubicacionRepository.save(ubicacion);

        FichaMaquina maquina = r.getMaquina();
        maquina.setReubicacionId(r.getIdRehubicacionMaquina());
        maquina.setAmbiente(r.getAmbienteDestino());
        maquina.setSede(r.getSedeDestino());
        maquina.setArea(r.getAreaDestino());
        maquina.setFechaUbicacionAmbiente(LocalDate.now());
        fichaMaquinaRepository.save(maquina);

        notificarCuentadanteMovimiento(r);
        return true;
    }

    private void notificarResponsablesAutorizacion(ReubicacionMaquina r, String tipoTraslado, String origen, String destino, String descripcion) {
        String enlace = "/reubicacion?detalle=" + r.getIdRehubicacionMaquina();
        String titulo = "Solicitud de autorizacion de traslado " + tipoTraslado.toLowerCase();
        String codigo = r.getMaquina().getCodigoSena();
        String mensaje = codigo + " - " + r.getMaquina().getDescripcion() + " de " + origen + " a " + destino + ". " + descripcion;
        for (Usuario responsable : usuarioRepository.listarResponsablesAutorizacionReubicacion()) {
            crearNotificacion(responsable, titulo, mensaje, enlace, "admin", "A");
        }
    }

    private void notificarCuentadanteMovimiento(ReubicacionMaquina r) {
        for (Usuario u : usuarioRepository.findAll()) {
            if (u.getRol() != null && normalizar(u.getRol().getDescripcionRol()).contains("cuentadante")) {
                crearNotificacion(u, "Maquina reubicada",
                        "Traslado " + r.getTipoTraslado().toLowerCase() + ": movieron la maquina " + r.getMaquina().getCodigoSena()
                                + " de " + r.getOrigenTexto() + " a " + r.getDestinoTexto() + ".",
                        "/mis-maquinas", "warn", "R");
            }
        }
    }

    private void notificarResponsablesAprobacion(ReubicacionMaquina r, String responsableNombre) {
        String enlace = "/reubicacion?detalle=" + r.getIdRehubicacionMaquina();
        String mensaje = "La reubicacion " + r.getIdRehubicacionMaquina() + " de " + r.getMaquina().getCodigoSena()
                + " fue aprobada por " + responsableNombre + ". El acta y la evidencia estan disponibles.";
        for (Usuario responsable : usuarioRepository.listarResponsablesAutorizacionReubicacion()) {
            crearNotificacion(responsable, "Reubicacion aprobada", mensaje, enlace, "success", "R");
        }
    }

    /** Notifica por correo a los responsables de autorizar, o al cuentadante segun el caso. */
    private boolean enviarCorreosSolicitud(ReubicacionMaquina r, Usuario solicitante) {
        String solicitanteTexto = solicitante != null
                ? limitar(solicitante.getNombre() + " " + solicitante.getApellido(), 200) + " - " + limitar(solicitante.getCorreo(), 200)
                : "Usuario del sistema";
        String asunto = "Solicitud de autorizacion de traslado " + r.getTipoTraslado().toLowerCase();
        String parrafo = "Se solicita revisar y autorizar un movimiento de maquina registrado en FIMACOR.";
        List<String[]> filas = filasSolicitud(r, solicitanteTexto);
        byte[] pdf = pdfSolicitud(r, solicitanteTexto);
        List<EmailService.Adjunto> adjuntos = List.of(
                EmailService.Adjunto.pdf("Solicitud_Reubicacion_" + r.getIdRehubicacionMaquina() + ".pdf", pdf));

        boolean enviado = false;
        for (Usuario responsable : usuarioRepository.listarResponsablesAutorizacionReubicacion()) {
            enviado |= enviarSiTieneCorreo(responsable, asunto, parrafo, filas, adjuntos);
        }
        // Sin el modulo de asignacion de maquinas a cuentadantes migrado todavia
        // (ver Asignar_maquinas.jsp/Mis_maquinas_asignadas.jsp pendientes), se
        // notifica a todos los cuentadantes en vez de solo al asignado a la maquina.
        for (Usuario u : usuarioRepository.findAll()) {
            if (u.getRol() != null && normalizar(u.getRol().getDescripcionRol()).contains("cuentadante")) {
                enviado |= enviarSiTieneCorreo(u, asunto, parrafo, filas, adjuntos);
            }
        }
        return enviado;
    }

    private void enviarCorreosActaFinalizada(ReubicacionMaquina r, String responsableNombre, byte[] firmaImagen) {
        String asunto = "Acta final de reubicacion TR-" + r.getIdRehubicacionMaquina();
        String parrafo = "La reubicacion fue aprobada, firmada y aplicada correctamente.";
        List<String[]> filas = List.of(
                new String[]{"Folio", "TR-" + r.getIdRehubicacionMaquina()},
                new String[]{"Estado", "Aprobado y ejecutado"},
                new String[]{"Maquina", r.getMaquina().getCodigoSena() + " - " + r.getMaquina().getDescripcion()},
                new String[]{"Tipo de traslado", r.getTipoTraslado()},
                new String[]{"Origen", r.getOrigenTexto()},
                new String[]{"Destino", r.getDestinoTexto()},
                new String[]{"Fecha", r.getFecha()},
                new String[]{"Aprobado por", responsableNombre},
                new String[]{"Descripcion", r.getDescripcionReubicacion()},
                new String[]{"Observacion", r.getObservacion()});
        byte[] pdf = pdfSolicitud(r, "Aprobado por " + responsableNombre);

        List<EmailService.Adjunto> adjuntos = new ArrayList<>();
        adjuntos.add(EmailService.Adjunto.pdf("Acta_Reubicacion_TR-" + r.getIdRehubicacionMaquina() + ".pdf", pdf));
        if (firmaImagen != null && firmaImagen.length > 0) {
            adjuntos.add(EmailService.Adjunto.png("Firma_Subdireccion_TR.png", firmaImagen));
        }
        Resource evidenciaRecurso = cargarEvidencia(r.getEvidencia());
        if (evidenciaRecurso != null) {
            adjuntos.add(new EmailService.Adjunto(r.getEvidencia(), evidenciaRecurso));
        }

        for (Usuario responsable : usuarioRepository.listarResponsablesAutorizacionReubicacion()) {
            enviarSiTieneCorreo(responsable, asunto, parrafo, filas, adjuntos);
        }
        for (Usuario u : usuarioRepository.findAll()) {
            if (u.getRol() != null && normalizar(u.getRol().getDescripcionRol()).contains("cuentadante")) {
                enviarSiTieneCorreo(u, asunto, parrafo, filas, adjuntos);
            }
        }
    }

    private boolean enviarSiTieneCorreo(Usuario destinatario, String asunto, String parrafo, List<String[]> filas,
            List<EmailService.Adjunto> adjuntos) {
        if (destinatario == null || destinatario.getCorreo() == null || destinatario.getCorreo().isBlank()) return false;
        try {
            emailService.enviarReubicacion(destinatario.getCorreo(),
                    limitar(destinatario.getNombre() + " " + destinatario.getApellido(), 200), asunto, parrafo, filas, adjuntos);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<String[]> filasSolicitud(ReubicacionMaquina r, String solicitanteTexto) {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"Tipo de traslado", r.getTipoTraslado()});
        filas.add(new String[]{"Maquina", r.getMaquina().getCodigoSena() + " - " + r.getMaquina().getDescripcion()});
        filas.add(new String[]{"Origen", r.getOrigenTexto()});
        filas.add(new String[]{"Destino", r.getDestinoTexto()});
        filas.add(new String[]{"Fecha", r.getFecha()});
        filas.add(new String[]{"Solicitante", solicitanteTexto});
        filas.add(new String[]{"Descripcion", r.getDescripcionReubicacion()});
        filas.add(new String[]{"Observacion", r.getObservacion()});
        filas.add(new String[]{"Evidencia", r.getEvidencia()});
        return filas;
    }

    private byte[] pdfSolicitud(ReubicacionMaquina r, String solicitanteTexto) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Folio: TR-" + r.getIdRehubicacionMaquina());
        lineas.add("Estado: " + r.getEstadoAutorizacion());
        lineas.add("");
        lineas.add("## MAQUINA");
        lineas.add("Codigo SENA: " + valor(r.getMaquina().getCodigoSena()));
        lineas.add("Descripcion: " + valor(r.getMaquina().getDescripcion()));
        lineas.add("Tipo de traslado: " + valor(r.getTipoTraslado()));
        lineas.add("");
        lineas.add("## MOVIMIENTO");
        lineas.add("Fecha: " + valor(r.getFecha()));
        lineas.add("Origen: " + valor(r.getOrigenTexto()));
        lineas.add("Destino: " + valor(r.getDestinoTexto()));
        lineas.add("Area destino: " + valor(r.getAreaDestino()));
        lineas.add("Motivo: " + valor(r.getDescripcionReubicacion()));
        lineas.add("Observacion: " + valor(r.getObservacion()));
        lineas.add("Evidencia: " + valor(r.getEvidencia()));
        lineas.add("Solicitante: " + valor(solicitanteTexto));
        lineas.add("");
        lineas.add("## APROBACION Y FIRMA ELECTRONICA");
        lineas.add("Autoridad: " + valor(r.getAutorizadoPor()));
        lineas.add("Fecha y hora de decision: " + (r.getFechaRespuestaAutorizacion() != null ? r.getFechaRespuestaAutorizacion().toString() : "Sin fecha"));
        lineas.add("Registro de firma: " + valor(r.getRespuestaAutorizacion()));
        return pdfBuilder.generar("FIMACOR - Acta de reubicacion", lineas);
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "Sin dato" : texto;
    }

    private Resource cargarEvidencia(String evidencia) {
        if (evidencia == null || evidencia.isBlank()) return null;
        try {
            Resource recurso = fileStorageService.cargar(CARPETA_EVIDENCIAS, evidencia);
            return recurso != null && recurso.exists() ? recurso : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean tieneActaDisponible(String estado) {
        String limpio = estado == null ? "" : estado.trim();
        return "Aprobado".equalsIgnoreCase(limpio) || "Aceptado".equalsIgnoreCase(limpio) || "Aceptada".equalsIgnoreCase(limpio);
    }

    /** Genera el PDF del acta; null si el traslado no tiene acta aprobada disponible. */
    public byte[] generarActaPdf(int idReubicacion) {
        ReubicacionMaquina r = reubicacionRepository.findById(idReubicacion).orElse(null);
        if (r == null || !tieneActaDisponible(r.getEstadoAutorizacion())) return null;
        return pdfSolicitud(r, r.getAutorizadoPor());
    }

    /** Devuelve la imagen de la firma; null si no hay firma registrada. */
    public byte[] obtenerFirma(int idReubicacion) {
        ReubicacionMaquina r = reubicacionRepository.findById(idReubicacion).orElse(null);
        return r != null ? r.getFirmaDigital() : null;
    }

    public Resource cargarEvidenciaPublica(int idReubicacion) {
        ReubicacionMaquina r = reubicacionRepository.findById(idReubicacion).orElse(null);
        return r != null ? cargarEvidencia(r.getEvidencia()) : null;
    }

    /** Elimina la evidencia adjunta de una reubicacion. */
    @Transactional
    public boolean eliminarEvidencias(List<Integer> ids, boolean eliminarRegistro) {
        boolean algunCambio = false;
        for (Integer id : ids) {
            if (id == null || id <= 0) continue;
            ReubicacionMaquina r = reubicacionRepository.findById(id).orElse(null);
            if (r == null) continue;
            if (r.getEvidencia() != null && !r.getEvidencia().isBlank()) {
                fileStorageService.eliminar(CARPETA_EVIDENCIAS, r.getEvidencia());
                algunCambio = true;
            }
            if (eliminarRegistro) {
                // ficha_maquina guarda cual fue la ultima reubicacion de cada maquina
                // (Reubicacion_Maquina_idRehubicacion_Maquina, ON DELETE NO ACTION); si no se
                // limpia primero, borrar el registro de reubicacion revienta con un 500 por
                // violacion de esa llave foranea en cuanto la maquina todavia apunta a el.
                for (FichaMaquina maquina : fichaMaquinaRepository.findByReubicacionId(id)) {
                    maquina.setReubicacionId(null);
                    fichaMaquinaRepository.save(maquina);
                }
                reubicacionRepository.delete(r);
            } else {
                r.setEvidencia("");
                reubicacionRepository.save(r);
            }
            algunCambio = true;
        }
        return algunCambio;
    }

    private void crearNotificacion(Usuario destinatario, String titulo, String mensaje, String enlace, String tipo, String icono) {
        NotificacionUsuario n = new NotificacionUsuario();
        n.setUsuario(destinatario);
        n.setTitulo(titulo);
        n.setMensaje(limitar(mensaje, 255));
        n.setEnlace(enlace);
        n.setTipo(tipo);
        n.setIcono(icono);
        notificacionRepository.save(n);
    }

    private String rolTexto(int idRol) {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() != null && u.getRol().getIdRol() == idRol)
                .map(u -> u.getRol().getDescripcionRol())
                .findFirst().orElse("");
    }

    private Sede idSedeComplejoSur() {
        return sedeRepository.findAll().stream()
                .filter(s -> "complejo sur".equals(normalizar(s.getNombreSede()).trim()))
                .findFirst().orElse(null);
    }

    private byte[] decodificarFirma(String firmaDigital) {
        try {
            if (firmaDigital == null || !firmaDigital.startsWith("data:image/png;base64,")) return null;
            int separador = firmaDigital.indexOf(',');
            byte[] imagen = Base64.getDecoder().decode(firmaDigital.substring(separador + 1));
            return imagen.length < 80 || imagen.length > 2_000_000 ? null : imagen;
        } catch (Exception e) {
            return null;
        }
    }

    private String guardarEvidencia(String nombreOriginal, InputStream contenido) {
        if (nombreOriginal == null || contenido == null) return "";
        String extension = extension(nombreOriginal);
        if (!extension.matches("\\.(png|jpg|jpeg|webp|pdf)")) return "";
        String nombreSeguro = "reubicacion_" + System.currentTimeMillis() + extension;
        try {
            fileStorageService.guardar(CARPETA_EVIDENCIAS, nombreSeguro, contenido);
            return nombreSeguro;
        } catch (IOException e) {
            return "";
        }
    }

    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto >= 0 ? nombre.substring(punto).toLowerCase() : "";
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
