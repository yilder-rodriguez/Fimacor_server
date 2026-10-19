package com.fimacor.comun.menu;

import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.usuarios.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Expone el atributo de modelo "navBar" en todas las vistas para el fragmento fragments/nav.html. */
@ControllerAdvice(annotations = Controller.class)
public class GlobalNavBarAdvice {

    private final NavBarService navBarService;

    public GlobalNavBarAdvice(NavBarService navBarService) {
        this.navBarService = navBarService;
    }

    @ModelAttribute("navBar")
    public NavBarService.NavBar navBar(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = auth != null && auth.getPrincipal() instanceof UsuarioAutenticado ua ? ua.getUsuario() : null;
        return navBarService.construir(usuario, request);
    }
}
