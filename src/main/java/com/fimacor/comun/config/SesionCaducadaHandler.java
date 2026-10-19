package com.fimacor.comun.config;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Un formulario que llega con el token CSRF vencido (la pagina estuvo abierta mientras la sesion caducaba,
 * la aplicacion se reinicio, o se cerro y abrio sesion en otra pestana) ya no termina en la pagina blanca
 * "Whitelabel Error Page / 403": se vuelve a la pantalla de origen, recien cargada y con token nuevo, con un aviso.
 * Cualquier otro rechazo (permisos) sigue el camino normal y muestra la pantalla de error amigable.
 */
public class SesionCaducadaHandler implements AccessDeniedHandler {

    private final AccessDeniedHandler porDefecto = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException ex) throws IOException, jakarta.servlet.ServletException {
        if (ex instanceof CsrfException && !response.isCommitted()) {
            response.sendRedirect(request.getContextPath() + destino(request.getServletPath(), estaAutenticado()) + "?caducada=1");
            return;
        }
        porDefecto.handle(request, response, ex);
    }

    static String destino(String ruta, boolean autenticado) {
        if (ruta == null) ruta = "";
        if (ruta.equals("/registro") || ruta.startsWith("/registro/")) return "/registro";
        if (ruta.equals("/recuperar") || ruta.startsWith("/recuperar/")) return "/recuperar";
        if (ruta.equals("/login")) return "/login";
        return autenticado ? "/menu" : "/login";
    }

    private static boolean estaAutenticado() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken);
    }
}
