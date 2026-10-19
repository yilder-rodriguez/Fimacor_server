package com.fimacor.auth.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.usuarios.entity.Rol;
import com.fimacor.usuarios.entity.Usuario;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fimacor.auth.service.RegistroService;

/** Flujo de registro publico: formulario y verificacion por codigo. No requiere sesion. */
@Controller
public class RegistroController {

    private final RegistroService service;
    private final PermisoService permisoService;

    public RegistroController(RegistroService service, PermisoService permisoService) {
        this.service = service;
        this.permisoService = permisoService;
    }

    @GetMapping("/registro")
    public String verFormulario(HttpSession session, Model model, @RequestParam(required = false) String origen,
            @RequestParam(required = false) String rolAdmin) {
        boolean atajoRolVigente = atajoRolVigente(session);
        boolean registroAdministrativo = esRegistroAdministrativo(session, origen);

        List<Rol> roles = service.listarRoles(registroAdministrativo);

        model.addAttribute("roles", roles);
        model.addAttribute("registroAdministrativo", registroAdministrativo);
        model.addAttribute("atajoRolVigente", atajoRolVigente);
        model.addAttribute("mostrarEstadoRegistro", registroAdministrativo);
        model.addAttribute("rolAdminPreseleccionado", rolAdmin == null ? "" : rolAdmin);
        return "registro";
    }

    @PostMapping("/registro")
    public String iniciar(HttpSession session, @RequestParam String nombre, @RequestParam String apellido,
            @RequestParam String documento, @RequestParam String telefono, @RequestParam String correo,
            @RequestParam String contrasena, @RequestParam int rol, @RequestParam(defaultValue = "CC") String tipoDoc,
            @RequestParam(defaultValue = "publico") String origen, @RequestParam(defaultValue = "1") String activo) {
        boolean registroAdministrativo = esRegistroAdministrativo(session, origen);

        RegistroService.Resultado resultado = service.iniciar(session, nombre, apellido, documento, telefono, correo,
                contrasena, rol, tipoDoc, registroAdministrativo, !"0".equals(activo));
        if (!resultado.ok()) return "redirect:/registro?error=" + resultado.error();
        String correoCodificado = URLEncoder.encode(correo, StandardCharsets.UTF_8);
        return "redirect:/registro/verificar?correo=" + correoCodificado + (resultado.correoEnviado() ? "" : "&envio=error");
    }

    @GetMapping("/registro/verificar")
    public String verVerificacion() {
        return "registro-verificar";
    }

    @PostMapping("/registro/verificar")
    public String confirmar(HttpSession session, @RequestParam String codigo) {
        RegistroService.Resultado resultado = service.confirmar(session, codigo);
        if (resultado.ok()) {
            if (resultado.administrativo()) return "redirect:/usuarios?ok=1";
            session.invalidate();
            return "redirect:/login?registro=ok";
        }
        return switch (resultado.error()) {
            case "sesion" -> "redirect:/registro?error=sesion";
            case "vencido", "codigo" -> "redirect:/registro/verificar?error=2";
            default -> "redirect:/registro?error=" + resultado.error();
        };
    }

    @PostMapping("/registro/reenviar")
    public String reenviar(HttpSession session) {
        Object correoPrevio = session.getAttribute("reg_correo");
        RegistroService.Resultado resultado = service.reenviarCodigo(session);
        if (resultado.ok()) {
            String correoCodificado = URLEncoder.encode((String) correoPrevio, StandardCharsets.UTF_8);
            return "redirect:/registro/verificar?correo=" + correoCodificado + "&reenvio=1";
        }
        return "sesion".equals(resultado.error())
                ? "redirect:/registro?error=1"
                : "redirect:/registro/verificar?error=1";
    }

    private int idRolSesionActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado ua)) return 0;
        Usuario usuario = ua.getUsuario();
        return usuario.getRol() != null ? usuario.getRol().getIdRol() : 0;
    }

    private boolean atajoRolVigente(HttpSession session) {
        Object marca = session.getAttribute("admin_shortcut_registro_tiempo");
        long tiempo = marca instanceof Long ? (Long) marca : 0L;
        return Boolean.TRUE.equals(session.getAttribute("admin_shortcut_registro_autorizado"))
                && tiempo > 0L && (System.currentTimeMillis() - tiempo) <= 5L * 60L * 1000L;
    }

    /** Registro administrativo: admin autenticado con permiso, o visitante que
     * acaba de pasar el acceso rapido con codigo. */
    private boolean esRegistroAdministrativo(HttpSession session, String origen) {
        if (!"admin".equalsIgnoreCase(origen)) return false;
        if (permisoService.tienePermisoConTodas(idRolSesionActual(), "usuarios", "crear")) return true;
        return atajoRolVigente(session);
    }
}
