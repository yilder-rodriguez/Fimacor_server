package com.fimacor.auth.service;

import com.fimacor.comun.correo.EmailService;
import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Permite a un administrador autorizar, desde la propia
 * pagina publica de registro, la creacion de un usuario con cualquier rol
 * sin pasar por la validacion de carnet. Verifica la contrasena actual del
 * administrador y luego un codigo de 6 digitos enviado por correo. Si el
 * correo falla, se reporta el error (no hay respaldo por WhatsApp todavia).
 */
@Service
public class AdminShortcutAuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminShortcutAuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long CODIGO_VIGENCIA_MS = 5L * 60L * 1000L;

    private final UsuarioRepository usuarioRepository;
    private final PermisoService permisoService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AdminShortcutAuthService(UsuarioRepository usuarioRepository, PermisoService permisoService,
            PasswordEncoder passwordEncoder, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.permisoService = permisoService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public record Resultado(boolean ok, int status, String mensaje, boolean requiereCodigo, boolean autorizado) {
        static Resultado error(int status, String mensaje) {
            return new Resultado(false, status, mensaje, false, false);
        }

        static Resultado codigoEnviado(String mensaje) {
            return new Resultado(true, 200, mensaje, true, false);
        }

        static Resultado autorizado(String mensaje) {
            return new Resultado(true, 200, mensaje, false, true);
        }
    }

    public Resultado solicitarCodigo(HttpSession session, String correoParam, String contrasena) {
        Usuario usuarioSesion = usuarioSesionActual();
        int rolSesion = usuarioSesion != null && usuarioSesion.getRol() != null ? usuarioSesion.getRol().getIdRol() : 0;

        String correo = limpiar(correoParam);
        if (correo.isEmpty() && usuarioSesion != null) correo = limpiar(usuarioSesion.getCorreo());
        if (correo.isEmpty()) return Resultado.error(400, "Escribe el correo del administrador.");
        if (contrasena == null || contrasena.trim().isEmpty()) return Resultado.error(400, "Escribe la contrasena actual.");

        Usuario validado = usuarioRepository.buscarPorCorreo(correo).orElse(null);
        if (validado == null || !validado.isActivo() || !passwordEncoder.matches(contrasena, validado.getContrasenaUsuario())) {
            return Resultado.error(401, "Correo o contrasena administrativa incorrecta.");
        }
        int idRolValidado = validado.getRol() != null ? validado.getRol().getIdRol() : 0;
        if (!puedeUsarAccesoRapido(idRolValidado)) {
            return Resultado.error(403, "El correo indicado no pertenece a un administrador.");
        }
        if (usuarioSesion != null && !usuarioSesion.getIdUsuario().equals(validado.getIdUsuario())
                && !permisoService.esRolTotal(rolSesion)) {
            return Resultado.error(403, "Solo un administrador total puede autorizar con otra cuenta administrativa.");
        }

        String codigo = generarCodigo();
        session.setAttribute("admin_shortcut_usuario_id", validado.getIdUsuario());
        session.setAttribute("admin_shortcut_codigo", codigo);
        session.setAttribute("admin_shortcut_tiempo", System.currentTimeMillis());

        try {
            String nombre = limpiar(validado.getNombre() + " " + validado.getApellido());
            emailService.enviarCodigoAccesoAdministrativo(validado.getCorreo(), nombre, codigo);
        } catch (Exception e) {
            LOG.error("No se pudo enviar el codigo de acceso administrativo a {}", validado.getCorreo(), e);
            limpiarCodigo(session);
            return Resultado.error(503,
                    "No fue posible enviar el codigo al correo. Verifica la conexion del servidor e intenta nuevamente.");
        }
        return Resultado.codigoEnviado("Codigo de 6 digitos enviado al correo del responsable.");
    }

    public Resultado verificarCodigo(HttpSession session, String codigoIngresado) {
        String codigo = limpiar(codigoIngresado);
        String codigoGuardado = limpiar((String) session.getAttribute("admin_shortcut_codigo"));
        Object tiempoObj = session.getAttribute("admin_shortcut_tiempo");
        Integer idUsuarioValidado = (Integer) session.getAttribute("admin_shortcut_usuario_id");
        long tiempo = tiempoObj instanceof Long ? (Long) tiempoObj : 0L;

        if (idUsuarioValidado == null || codigoGuardado.isEmpty() || tiempo == 0L) {
            return Resultado.error(401, "Primero solicita un codigo de acceso.");
        }
        if ((System.currentTimeMillis() - tiempo) > CODIGO_VIGENCIA_MS) {
            limpiarCodigo(session);
            return Resultado.error(401, "El codigo vencio. Solicita uno nuevo.");
        }
        if (!codigoGuardado.equals(codigo)) {
            return Resultado.error(401, "Codigo incorrecto.");
        }

        limpiarCodigo(session);
        session.setAttribute("admin_shortcut_registro_autorizado", Boolean.TRUE);
        session.setAttribute("admin_shortcut_registro_tiempo", System.currentTimeMillis());
        session.setAttribute("admin_shortcut_registro_autorizador", idUsuarioValidado);
        return Resultado.autorizado("Acceso autorizado temporalmente para crear el usuario.");
    }

    private void limpiarCodigo(HttpSession session) {
        session.removeAttribute("admin_shortcut_usuario_id");
        session.removeAttribute("admin_shortcut_codigo");
        session.removeAttribute("admin_shortcut_tiempo");
    }

    private boolean puedeUsarAccesoRapido(int idRol) {
        return permisoService.esRolTotal(idRol) || permisoService.tienePermisoConTodas(idRol, "usuarios", "crear");
    }

    private Usuario usuarioSesionActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado ua)) return null;
        return ua.getUsuario();
    }

    private String generarCodigo() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
