package com.fimacor.gerencia.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.gerencia.service.GerenciaService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Tablero de gerencia: indicadores del inventario de maquinas. Solo quien tenga el permiso tablero.ver. */
@Controller
public class GerenciaController {

    private final GerenciaService service;
    private final PermisoService permisoService;

    public GerenciaController(GerenciaService service, PermisoService permisoService) {
        this.service = service;
        this.permisoService = permisoService;
    }

    @GetMapping("/gerencia")
    public String ver(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        int idRol = usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
        if (!permisoService.tienePermisoConTodas(idRol, "tablero", "ver")) return "redirect:/menu";
        model.addAttribute("maquinas", service.maquinas());
        model.addAttribute("fechaCorte", LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "CO"))));
        return "gerencia";
    }
}
