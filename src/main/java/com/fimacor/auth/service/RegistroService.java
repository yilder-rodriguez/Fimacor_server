package com.fimacor.auth.service;

import com.fimacor.comun.correo.EmailService;
import com.fimacor.usuarios.entity.DocumentoIdentidad;
import com.fimacor.usuarios.repository.DocumentoIdentidadRepository;
import com.fimacor.usuarios.entity.Rol;
import com.fimacor.usuarios.repository.RolRepository;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro publico de un usuario nuevo, con verificacion por correo.
 */
@Service
public class RegistroService {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VENCIMIENTO_MS = 15 * 60 * 1000;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final DocumentoIdentidadRepository documentoIdentidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public RegistroService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            DocumentoIdentidadRepository documentoIdentidadRepository, PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.documentoIdentidadRepository = documentoIdentidadRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public List<Rol> listarRoles(boolean incluirTodos) {
        List<Rol> todos = rolRepository.findAllByOrderByIdRolAsc();
        if (incluirTodos) return todos;
        return todos.stream().filter(r -> esRolPermitidoPublico(r.getDescripcionRol())).toList();
    }

    public record Resultado(boolean ok, String error, boolean correoEnviado, boolean administrativo) {
        static Resultado exito() {
            return new Resultado(true, null, true, false);
        }
        static Resultado exitoSinCorreo() {
            return new Resultado(true, null, false, false);
        }
        static Resultado exitoRegistro(boolean administrativo) {
            return new Resultado(true, null, true, administrativo);
        }
        static Resultado fallo(String motivo) {
            return new Resultado(false, motivo, false, false);
        }
    }

    public Resultado iniciar(HttpSession session, String nombre, String apellido, String documento, String telefono,
            String correo, String contrasena, int idRol, String tipoDoc, boolean registroAdministrativo, boolean activo) {
        if (usuarioRepository.buscarPorCorreo(correo).isPresent()) return Resultado.fallo("correo");
        if (existeDocumento(documento)) return Resultado.fallo("documento");
        if (existeTelefono(telefono)) return Resultado.fallo("telefono");

        Rol rol = rolRepository.findById(idRol).orElse(null);
        if (rol == null) return Resultado.fallo("rol");
        if (!registroAdministrativo && !esRolPermitidoPublico(rol.getDescripcionRol())) return Resultado.fallo("rol");

        String codigo = String.valueOf(100000 + RANDOM.nextInt(900000));
        session.setAttribute("reg_nombre", nombre);
        session.setAttribute("reg_apellido", apellido);
        session.setAttribute("reg_documento", documento);
        session.setAttribute("reg_telefono", telefono);
        session.setAttribute("reg_correo", correo);
        session.setAttribute("reg_contrasena", contrasena);
        session.setAttribute("reg_rol_id", idRol);
        session.setAttribute("reg_tipoDoc", tipoDoc);
        session.setAttribute("reg_activo", activo);
        session.setAttribute("reg_admin", registroAdministrativo);
        session.setAttribute("reg_codigo", codigo);
        session.setAttribute("reg_tiempo", System.currentTimeMillis());

        try {
            emailService.enviarCodigoRegistro(correo, nombre, codigo);
        } catch (Exception e) {
            LOG.error("No se pudo enviar el codigo de registro a {}", correo, e);
            return Resultado.exitoSinCorreo();
        }
        return Resultado.exito();
    }

    public Resultado reenviarCodigo(HttpSession session) {
        String correo = (String) session.getAttribute("reg_correo");
        String nombre = (String) session.getAttribute("reg_nombre");
        if (correo == null) return Resultado.fallo("sesion");

        String codigo = String.valueOf(100000 + RANDOM.nextInt(900000));
        session.setAttribute("reg_codigo", codigo);
        session.setAttribute("reg_tiempo", System.currentTimeMillis());
        try {
            emailService.enviarCodigoRegistro(correo, nombre, codigo);
        } catch (Exception e) {
            LOG.error("No se pudo reenviar el codigo de registro a {}", correo, e);
            return Resultado.fallo("correo_envio");
        }
        return Resultado.exito();
    }

    @Transactional
    public Resultado confirmar(HttpSession session, String codigoIngresado) {
        String codigoGuardado = (String) session.getAttribute("reg_codigo");
        Long tiempo = (Long) session.getAttribute("reg_tiempo");
        if (codigoGuardado == null || tiempo == null) return Resultado.fallo("sesion");
        if (System.currentTimeMillis() - tiempo > VENCIMIENTO_MS) return Resultado.fallo("vencido");
        if (codigoIngresado == null || !codigoIngresado.trim().equals(codigoGuardado.trim())) return Resultado.fallo("codigo");

        String nombre = (String) session.getAttribute("reg_nombre");
        String apellido = (String) session.getAttribute("reg_apellido");
        String documento = (String) session.getAttribute("reg_documento");
        String telefono = (String) session.getAttribute("reg_telefono");
        String correo = (String) session.getAttribute("reg_correo");
        String contrasena = (String) session.getAttribute("reg_contrasena");
        Integer idRol = (Integer) session.getAttribute("reg_rol_id");
        String tipoDoc = (String) session.getAttribute("reg_tipoDoc");
        boolean activo = !Boolean.FALSE.equals(session.getAttribute("reg_activo"));
        boolean administrativo = Boolean.TRUE.equals(session.getAttribute("reg_admin"));

        if (usuarioRepository.buscarPorCorreo(correo).isPresent()) return Resultado.fallo("correo");
        if (existeDocumento(documento)) return Resultado.fallo("documento");

        Rol rol = idRol != null ? rolRepository.findById(idRol).orElse(null) : null;
        if (rol == null) return Resultado.fallo("rol");

        DocumentoIdentidad doc = documentoIdentidadRepository.findAll().stream()
                .filter(d -> documento.equals(d.getNumeroDocumento()))
                .findFirst()
                .orElseGet(() -> {
                    DocumentoIdentidad nuevo = new DocumentoIdentidad();
                    nuevo.setDescripcionDocumento(tipoDoc == null || tipoDoc.isBlank() ? "CC" : tipoDoc);
                    nuevo.setNumeroDocumento(documento);
                    return documentoIdentidadRepository.save(nuevo);
                });

        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setDocumentoIdentidad(documento);
        u.setNumeroTelefono(telefono);
        u.setCorreo(correo);
        u.setContrasenaUsuario(passwordEncoder.encode(contrasena));
        u.setFechaExpiracion(LocalDate.now().plusYears(1).atStartOfDay());
        u.setRol(rol);
        u.setDocumentoIdentidadId(doc.getIdDocumentoIdentidad());
        u.setActivo(activo);
        usuarioRepository.save(u);

        limpiarSesion(session);
        return Resultado.exitoRegistro(administrativo);
    }

    private void limpiarSesion(HttpSession session) {
        for (String clave : new String[]{"reg_nombre", "reg_apellido", "reg_documento", "reg_telefono", "reg_correo",
                "reg_contrasena", "reg_rol_id", "reg_tipoDoc", "reg_activo", "reg_admin",
                "reg_codigo", "reg_tiempo"}) {
            session.removeAttribute(clave);
        }
    }

    private boolean existeDocumento(String documento) {
        return documentoIdentidadRepository.findAll().stream().anyMatch(d -> documento.equals(d.getNumeroDocumento()));
    }

    private boolean existeTelefono(String telefono) {
        return usuarioRepository.findAll().stream().anyMatch(u -> telefono.equals(u.getNumeroTelefono()));
    }

    private boolean esRolPermitidoPublico(String descripcionRol) {
        String rol = normalizar(descripcionRol);
        return rol.contains("instructor") || rol.contains("tecnico") || rol.contains("logistica");
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
