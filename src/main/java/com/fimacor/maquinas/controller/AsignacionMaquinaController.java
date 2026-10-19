package com.fimacor.maquinas.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.manuales.service.ManualesService;
import com.fimacor.usuarios.entity.Usuario;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fimacor.manuales.entity.Manual;
import com.fimacor.maquinas.entity.MaquinaCuentadante;
import com.fimacor.maquinas.service.AsignacionMaquinaService;
import com.fimacor.maquinas.service.MaquinasService;

/** Pantallas para asignar maquinas a cuentadantes y ver las maquinas asignadas. */
@Controller
public class AsignacionMaquinaController {

    private final AsignacionMaquinaService service;
    private final MaquinasService maquinasService;
    private final ManualesService manualesService;
    private final PermisoService permisoService;

    public AsignacionMaquinaController(AsignacionMaquinaService service, MaquinasService maquinasService,
            ManualesService manualesService, PermisoService permisoService) {
        this.service = service;
        this.maquinasService = maquinasService;
        this.manualesService = manualesService;
        this.permisoService = permisoService;
    }

    @GetMapping("/maquinas/asignar")
    public String verAsignar(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        if (!puedeAsignar(usuario)) return "redirect:/menu";

        List<MaquinaCuentadante> asignaciones = service.listarTodas();
        Map<Integer, Long> totalPorUsuario = new HashMap<>();
        for (Usuario cuentadante : service.listarCuentadantes()) {
            totalPorUsuario.put(cuentadante.getIdUsuario(), service.contarPorUsuario(cuentadante.getIdUsuario()));
        }

        model.addAttribute("maquinas", maquinasService.listar());
        model.addAttribute("cuentadantes", service.listarCuentadantes());
        model.addAttribute("asignaciones", asignaciones);
        model.addAttribute("totalPorUsuario", totalPorUsuario);
        return "maquina-asignar";
    }

    @PostMapping("/maquinas/asignar")
    public String asignar(@RequestParam int idMaquina, @RequestParam int idUsuario,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeAsignar(usuario)) return "redirect:/menu";
        AsignacionMaquinaService.Resultado resultado = service.asignar(idMaquina, idUsuario);
        return resultado.ok() ? "redirect:/maquinas/asignar?ok=asignado" : "redirect:/maquinas/asignar?error=" + resultado.error();
    }

    @PostMapping("/maquinas/asignar/{id}/eliminar")
    public String eliminar(@org.springframework.web.bind.annotation.PathVariable int id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeAsignar(usuario)) return "redirect:/menu";
        boolean ok = service.eliminar(id);
        return "redirect:/maquinas/asignar?" + (ok ? "ok=eliminado" : "error=eliminar");
    }

    @GetMapping("/maquinas/mis-asignadas")
    public String verMisAsignadas(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        int idRol = idRol(usuario);
        if (usuario == null || !permisoService.tienePermisoConTodas(idRol, "maquinas", "asignadas")) {
            return "redirect:/menu";
        }
        List<MaquinaCuentadante> misAsignaciones = service.listarPorUsuario(usuario.getUsuario().getIdUsuario());

        Map<Integer, java.util.List<com.fimacor.manuales.entity.Manual>> manualesPorMaquina = new HashMap<>();
        for (MaquinaCuentadante asignacion : misAsignaciones) {
            int idMaquina = asignacion.getMaquina().getIdMaquinas();
            manualesPorMaquina.put(idMaquina, manualesService.listarPorMaquina(idMaquina));
        }

        model.addAttribute("misAsignaciones", misAsignaciones);
        model.addAttribute("manualesPorMaquina", manualesPorMaquina);
        return "maquina-mis-asignadas";
    }

    private boolean puedeAsignar(UsuarioAutenticado usuario) {
        int idRol = idRol(usuario);
        boolean esAdministrador = usuario != null && usuario.getUsuario().getRol() != null
                && normalizar(usuario.getUsuario().getRol().getDescripcionRol()).contains("admin");
        return esAdministrador || permisoService.tienePermisoConTodas(idRol, "maquinas", "asignar");
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private int idRol(UsuarioAutenticado usuario) {
        return usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
    }
}
