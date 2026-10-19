package com.fimacor.usuarios.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fimacor.usuarios.entity.DocumentoIdentidad;
import com.fimacor.usuarios.entity.Rol;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.DocumentoIdentidadRepository;
import com.fimacor.usuarios.repository.RolRepository;
import com.fimacor.usuarios.repository.UsuarioRepository;

/** Administra los usuarios ya existentes. Crear un usuario nuevo no se hace
 * desde aca: solo se puede crear mediante el registro publico con
 * verificacion por correo. */
@Service
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final DocumentoIdentidadRepository documentoIdentidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public UsuarioAdminService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            DocumentoIdentidadRepository documentoIdentidadRepository, PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.documentoIdentidadRepository = documentoIdentidadRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAllByOrderByIdRolAsc();
    }

    public List<DocumentoIdentidad> listarDocumentos() {
        return documentoIdentidadRepository.findAll();
    }

    /** Deriva CC/TI/CE a partir de la descripcion actual del documento. */
    public String codigoTipoDocumento(Usuario usuario) {
        if (usuario.getDocumentoIdentidadId() == null) return "CC";
        DocumentoIdentidad doc = documentoIdentidadRepository.findById(usuario.getDocumentoIdentidadId()).orElse(null);
        String descripcion = doc != null && doc.getDescripcionDocumento() != null
                ? doc.getDescripcionDocumento().trim().toUpperCase() : "";
        if (descripcion.contains("TI") || descripcion.contains("TARJETA")) return "TI";
        if (descripcion.contains("CE") || descripcion.contains("EXTRANJ")) return "CE";
        return "CC";
    }

    @Transactional
    public boolean actualizar(int idUsuario, String nombre, String apellido, String documento, String telefono,
            String correo, String contrasenaNueva, LocalDateTime fechaExpiracion, String tipoDocumentoCodigo, int idRol,
            String codigoInstructorSolicitado, Boolean actuaComoTecnico) {
        Usuario actual = usuarioRepository.findById(idUsuario).orElse(null);
        if (actual == null) return false;
        Rol rol = rolRepository.findById(idRol).orElse(null);
        if (rol == null) return false;
        if (limpiar(nombre).isEmpty() || limpiar(apellido).isEmpty() || limpiar(documento).isEmpty() || limpiar(correo).isEmpty()) {
            return false;
        }

        actual.setNombre(limitar(nombre, 45));
        actual.setApellido(limitar(apellido, 45));
        actual.setDocumentoIdentidad(limitar(documento, 45));
        actual.setNumeroTelefono(limitar(telefono, 45));
        actual.setCorreo(limitar(correo, 45));
        actual.setFechaExpiracion(fechaExpiracion != null ? fechaExpiracion : actual.getFechaExpiracion());
        actual.setRol(rol);
        actual.setCodigoInstructor(codigoParaGuardar(actual, rol, codigoInstructorSolicitado));
        // La casilla solo aplica a Instructores: con cualquier otro rol se limpia.
        boolean esInstructor = rol.getDescripcionRol() != null && rol.getDescripcionRol().toLowerCase().contains("instructor");
        actual.setActuaComoTecnico(esInstructor && (actuaComoTecnico != null ? actuaComoTecnico : actual.isActuaComoTecnico()));

        if (!limpiar(contrasenaNueva).isEmpty()) {
            actual.setContrasenaUsuario(passwordEncoder.encode(limpiar(contrasenaNueva)));
        }

        actualizarTipoDocumento(actual.getDocumentoIdentidadId(), tipoDocumentoCodigo, actual.getDocumentoIdentidad());

        usuarioRepository.save(actual);
        return true;
    }

    /**
     * Codigo corto, unico y administrable para validar reportes por QR de
     * instructores (legacy: UsuarioDAO.codigoParaGuardar). Solo aplica a
     * usuarios con rol Instructor; para cualquier otro rol se limpia.
     */
    private String codigoParaGuardar(Usuario actual, Rol rol, String codigoSolicitado) {
        if (rol.getDescripcionRol() == null || !rol.getDescripcionRol().toLowerCase().contains("instructor")) {
            return null;
        }
        String solicitado = normalizarCodigo(codigoSolicitado);
        if (!solicitado.isEmpty() && codigoDisponible(solicitado, actual.getIdUsuario())) {
            return solicitado;
        }
        if (actual.getCodigoInstructor() != null && !actual.getCodigoInstructor().isBlank()) {
            return actual.getCodigoInstructor();
        }
        return generarCodigoInstructor(actual.getNombre(), actual.getIdUsuario());
    }

    private String normalizarCodigo(String valor) {
        String limpio = valor == null ? "" : Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return limpio.length() > 8 ? limpio.substring(0, 8) : limpio;
    }

    private String generarCodigoInstructor(String nombre, Integer excluirId) {
        String letras = nombre == null ? "INS" : Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z]", "").toUpperCase();
        letras = (letras + "INS").substring(0, Math.min(4, (letras + "INS").length()));
        Random aleatorio = new Random();
        for (int intento = 0; intento < 200; intento++) {
            String codigo = letras + String.format("%04d", aleatorio.nextInt(10000));
            if (codigoDisponible(codigo, excluirId)) return codigo;
        }
        return "INS" + String.format("%05d", Math.max(0, excluirId == null ? 0 : excluirId));
    }

    private boolean codigoDisponible(String codigo, Integer excluirId) {
        if (codigo == null || codigo.isEmpty()) return false;
        return !usuarioRepository.existsByCodigoInstructorIgnoreCaseAndIdUsuarioNot(codigo, excluirId == null ? -1 : excluirId);
    }

    private void actualizarTipoDocumento(Integer idDocumento, String codigo, String numeroDocumento) {
        if (idDocumento == null) return;
        documentoIdentidadRepository.findById(idDocumento).ifPresent(doc -> {
            doc.setDescripcionDocumento(codigo == null || codigo.isBlank() ? "CC" : codigo);
            doc.setNumeroDocumento(numeroDocumento);
            documentoIdentidadRepository.save(doc);
        });
    }

    public boolean cambiarEstado(int idUsuario, boolean activo) {
        return usuarioRepository.findById(idUsuario).map(usuario -> {
            usuario.setActivo(activo);
            usuarioRepository.save(usuario);
            return true;
        }).orElse(false);
    }

    /** Limpia las tablas de otros modulos que todavia no tienen relacion JPA
     * antes de borrar el usuario. */
    @Transactional
    public boolean eliminarConDependencias(int idUsuario) {
        jdbcTemplate.update("DELETE FROM programacion_mantenimiento WHERE Usuario_idUsuario = ?", idUsuario);
        jdbcTemplate.update("DELETE FROM mantenimiento WHERE Usuario_idUsuario = ?", idUsuario);
        jdbcTemplate.update("DELETE FROM historial_mantenimiento WHERE Usuario_idUsuario = ?", idUsuario);
        jdbcTemplate.update("DELETE FROM novedades WHERE Usuario_idUsuario = ?", idUsuario);
        int filas = jdbcTemplate.update("DELETE FROM usuario WHERE idUsuario = ?", idUsuario);
        return filas > 0;
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String limitar(String valor, int maximo) {
        String limpio = limpiar(valor);
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
