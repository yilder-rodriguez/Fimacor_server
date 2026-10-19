package com.fimacor.usuarios.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fimacor.usuarios.entity.Permiso;
import com.fimacor.usuarios.entity.Rol;
import com.fimacor.usuarios.util.PermisoModulos;
import com.fimacor.usuarios.service.RolesPermisosService;

/** Pantallas de administracion de roles y permisos. */
@Controller
public class RolesPermisosController {

    private final RolesPermisosService service;
    private final PermisoService permisoService;

    public RolesPermisosController(RolesPermisosService service, PermisoService permisoService) {
        this.service = service;
        this.permisoService = permisoService;
    }

    @GetMapping("/roles")
    public String verRoles(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        List<Rol> roles = service.listarRoles();
        List<Permiso> permisos = service.listarPermisos();
        List<PermisoModulos.Modulo> modulos = PermisoModulos.agrupar(permisos);
        var permisosPorRol = roles.stream()
                .collect(java.util.stream.Collectors.toMap(Rol::getIdRol, r -> service.permisosPorRol(r.getIdRol())));
        var usuariosPorRol = roles.stream()
                .collect(java.util.stream.Collectors.toMap(Rol::getIdRol, r -> service.usuariosPorRol(r.getIdRol())));
        model.addAttribute("roles", roles);
        model.addAttribute("permisos", permisos);
        model.addAttribute("modulosPermisos", modulos);
        model.addAttribute("rolesDelSistema", service.idsRolesDelSistema());
        model.addAttribute("permisosPorRol", permisosPorRol);
        model.addAttribute("usuariosPorRol", usuariosPorRol);
        model.addAttribute("resumenPorRol", roles.stream().collect(java.util.stream.Collectors.toMap(
                Rol::getIdRol, r -> PermisoModulos.resumen(modulos, permisosPorRol.get(r.getIdRol())))));
        model.addAttribute("totalUsuariosConRol", usuariosPorRol.values().stream().mapToInt(List::size).sum());
        return "roles";
    }

    @GetMapping("/permisos")
    public String verPermisos(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model,
            @RequestParam(required = false) Integer rol) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        List<Rol> roles = service.listarRoles();
        List<Permiso> permisos = service.listarPermisos();
        model.addAttribute("roles", roles);
        model.addAttribute("permisos", permisos);
        model.addAttribute("modulosPermisos", PermisoModulos.agrupar(permisos));
        model.addAttribute("rolActivo", rol != null ? rol : (roles.isEmpty() ? 0 : roles.get(0).getIdRol()));
        model.addAttribute("permisosPorRol", roles.stream()
                .collect(java.util.stream.Collectors.toMap(Rol::getIdRol, r -> service.permisosPorRol(r.getIdRol()))));
        return "permisos";
    }

    @PostMapping("/roles")
    public String crearRol(@RequestParam String descripcion,
            @RequestParam(name = "permisos", required = false) List<Integer> permisos,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = service.crearRol(descripcion, permisos);
        return "redirect:/roles?" + (ok ? "ok=creado" : "error=crear");
    }

    @PostMapping("/roles/{id}")
    public String actualizarRol(@PathVariable int id, @RequestParam String descripcion,
            @RequestParam(name = "permisos", required = false) List<Integer> permisos,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = service.actualizarRol(id, descripcion) && service.guardarPermisosRol(id, permisos);
        return "redirect:/roles?" + (ok ? "ok=actualizado" : "error=actualizar");
    }

    @PostMapping("/roles/{id}/eliminar")
    public String eliminarRol(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = service.eliminarRol(id);
        return "redirect:/roles?" + (ok ? "ok=eliminado" : "error=eliminar");
    }

    @PostMapping("/roles/{id}/permisos")
    public String guardarPermisosRol(@PathVariable int id,
            @RequestParam(name = "permisos", required = false) List<Integer> permisos,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = service.guardarPermisosRol(id, permisos);
        return "redirect:/permisos?rol=" + id + "&" + (ok ? "ok=guardado" : "error=guardar");
    }

    @PostMapping("/permisos")
    public String crearPermiso(@RequestParam String codigo, @RequestParam String descripcion,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = service.crearPermiso(codigo, descripcion);
        return "redirect:/permisos?" + (ok ? "ok=creado" : "error=crear");
    }

    @PostMapping("/permisos/{id}")
    public String actualizarPermiso(@PathVariable int id, @RequestParam String codigo, @RequestParam String descripcion,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = service.actualizarPermiso(id, codigo, descripcion);
        return "redirect:/permisos?" + (ok ? "ok=actualizado" : "error=actualizar");
    }

    @PostMapping("/permisos/{id}/eliminar")
    public String eliminarPermiso(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = service.eliminarPermiso(id);
        return "redirect:/permisos?" + (ok ? "ok=eliminado" : "error=eliminar");
    }

    private boolean puedeGestionar(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.getUsuario().getRol() == null) return false;
        int idRol = usuario.getUsuario().getRol().getIdRol();
        return permisoService.tienePermisoConTodas(idRol, "roles", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "permisos", "gestionar");
    }
}
