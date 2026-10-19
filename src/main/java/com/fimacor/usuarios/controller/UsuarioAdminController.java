package com.fimacor.usuarios.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.service.UsuarioAdminService;

/** Pantalla de administracion de usuarios: listar, editar, cambiar estado y eliminar. */
@Controller
public class UsuarioAdminController {

    private final UsuarioAdminService service;
    private final PermisoService permisoService;

    public UsuarioAdminController(UsuarioAdminService service, PermisoService permisoService) {
        this.service = service;
        this.permisoService = permisoService;
    }

    @GetMapping("/usuarios")
    public String listar(@AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado, Model model) {
        int idRol = idRol(usuarioAutenticado);
        if (!permisoService.tienePermisoConTodas(idRol, "usuarios", "ver")) return "redirect:/menu";

        List<Usuario> usuarios = service.listarUsuarios();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("roles", service.listarRoles());
        model.addAttribute("documentos", service.listarDocumentos());
        model.addAttribute("tipoDocumentoPorUsuario", usuarios.stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, service::codigoTipoDocumento)));
        model.addAttribute("idUsuarioSesion", usuarioAutenticado.getUsuario().getIdUsuario());
        model.addAttribute("puedeEditar", permisoService.tienePermisoConTodas(idRol, "usuarios", "editar"));
        model.addAttribute("puedeEliminar", permisoService.tienePermisoConTodas(idRol, "usuarios", "eliminar"));
        model.addAttribute("puedeCambiarEstado", permisoService.esRolTotal(idRol)
                || permisoService.tienePermisoConTodas(idRol, "usuarios", "estado"));
        model.addAttribute("puedeEditarCodigoInstructor", permisoService.esRolTotal(idRol));
        return "usuarios";
    }

    @PostMapping("/usuarios/{id}")
    public String actualizar(@PathVariable int id, @RequestParam Map<String, String> form,
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado) {
        if (!permisoService.tienePermisoConTodas(idRol(usuarioAutenticado), "usuarios", "editar")) return "redirect:/menu";

        LocalDateTime fechaExpiracion = null;
        try {
            fechaExpiracion = LocalDate.parse(form.get("fechaExpiracion")).atStartOfDay();
        } catch (Exception ignorado) {
            // Se conserva la fecha actual si el campo llega vacio o invalido.
        }
        // Solo el Administrador decide la casilla; para los demas editores se conserva lo que ya tenia.
        Boolean actuaComoTecnico = permisoService.esRolTotal(idRol(usuarioAutenticado))
                ? form.containsKey("actuaComoTecnico") : null;
        boolean ok = service.actualizar(id, form.get("nombre"), form.get("apellido"), form.get("documento"),
                form.get("telefono"), form.get("correo"), form.get("contrasena"), fechaExpiracion,
                form.get("tipoDocumento"), enteroSeguro(form.get("rolUsuario")), form.get("codigoInstructor"),
                actuaComoTecnico);
        return "redirect:/usuarios?" + (ok ? "ok=actualizado" : "error=actualizar");
    }

    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstado(@PathVariable int id, @RequestParam int activo,
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado) {
        int idRol = idRol(usuarioAutenticado);
        boolean puedeCambiarEstado = permisoService.esRolTotal(idRol)
                || permisoService.tienePermisoConTodas(idRol, "usuarios", "estado");
        if (!puedeCambiarEstado) return "redirect:/menu";

        boolean esUsuarioSesion = usuarioAutenticado.getUsuario().getIdUsuario() == id;
        if (esUsuarioSesion && activo == 0) return "redirect:/usuarios?error=estado";

        boolean ok = service.cambiarEstado(id, activo == 1);
        return "redirect:/usuarios?" + (ok ? "ok=estado" : "error=estado");
    }

    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminar(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado) {
        if (!permisoService.tienePermisoConTodas(idRol(usuarioAutenticado), "usuarios", "eliminar")) return "redirect:/menu";
        if (usuarioAutenticado.getUsuario().getIdUsuario() == id) return "redirect:/usuarios?error=eliminar";

        boolean ok = service.eliminarConDependencias(id);
        return "redirect:/usuarios?" + (ok ? "ok=eliminado" : "error=eliminar");
    }

    private int idRol(UsuarioAutenticado usuario) {
        return usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
    }

    private int enteroSeguro(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
