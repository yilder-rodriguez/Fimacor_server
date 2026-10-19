package com.fimacor.auth.service;

import com.fimacor.comun.correo.EmailService;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Flujo de recuperacion de contrasena: enviar codigo, reenviar y cambiar contrasena. */
@Service
public class RecuperacionService {

    private static final Logger LOG = LoggerFactory.getLogger(RecuperacionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VENCIMIENTO_MS = 15 * 60 * 1000;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public RecuperacionService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public record Resultado(boolean ok, String error) {
        static Resultado exito() {
            return new Resultado(true, null);
        }
        static Resultado fallo(String motivo) {
            return new Resultado(false, motivo);
        }
    }

    public Resultado solicitar(HttpSession session, String correo) {
        Usuario usuario = usuarioRepository.buscarPorCorreo(correo).orElse(null);
        if (usuario == null) return Resultado.fallo("1");

        String codigo = String.valueOf(100000 + RANDOM.nextInt(900000));
        session.setAttribute("codigoRecuperacion", codigo);
        session.setAttribute("correoRecuperacion", correo);
        session.setAttribute("recuperacionTiempo", System.currentTimeMillis());
        session.setMaxInactiveInterval(600);

        try {
            emailService.enviarCodigoRecuperacion(correo, usuario.getNombre(), codigo);
        } catch (Exception e) {
            LOG.error("No se pudo enviar el codigo de recuperacion a {}", correo, e);
            return Resultado.fallo("2");
        }
        return Resultado.exito();
    }

    public Resultado reenviar(HttpSession session) {
        String correo = (String) session.getAttribute("correoRecuperacion");
        if (correo == null) return Resultado.fallo("1");
        return solicitar(session, correo);
    }

    public Resultado cambiar(HttpSession session, String codigoIngresado, String nuevaContrasena) {
        String codigoReal = (String) session.getAttribute("codigoRecuperacion");
        String correo = (String) session.getAttribute("correoRecuperacion");
        Long tiempo = (Long) session.getAttribute("recuperacionTiempo");
        if (codigoReal == null || correo == null || !codigoReal.equals(codigoIngresado)) return Resultado.fallo("2");
        if (tiempo != null && System.currentTimeMillis() - tiempo > VENCIMIENTO_MS) return Resultado.fallo("2");

        Usuario usuario = usuarioRepository.buscarPorCorreo(correo).orElse(null);
        if (usuario == null) return Resultado.fallo("1");
        if (passwordEncoder.matches(nuevaContrasena, usuario.getContrasenaUsuario())) return Resultado.fallo("3");

        usuario.setContrasenaUsuario(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);

        session.removeAttribute("codigoRecuperacion");
        session.removeAttribute("correoRecuperacion");
        session.removeAttribute("recuperacionTiempo");
        return Resultado.exito();
    }
}
