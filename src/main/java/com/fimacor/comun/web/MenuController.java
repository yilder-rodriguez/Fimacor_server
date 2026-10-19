package com.fimacor.comun.web;

import com.fimacor.comun.menu.MenuService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import jakarta.servlet.http.HttpSession;
import java.util.Random;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MenuController {

    private static final String[] TIPS_DIA = {
        "Revisa el estado de las máquinas antes de iniciar la jornada.",
        "Registra las fallas apenas se detecten para evitar daños mayores.",
        "Mantener los manuales actualizados facilita el mantenimiento técnico.",
        "Limpia el área de trabajo antes y después de usar cada máquina.",
        "Programa revisiones preventivas cuando una máquina empiece a presentar ruido o vibración."
    };

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menu")
    public String menu(@AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado, Model model, HttpSession session,
            @RequestParam(required = false) String solicitudReubicacion) {
        model.addAttribute("usuario", usuarioAutenticado.getUsuario());
        model.addAttribute("datos", menuService.calcular(usuarioAutenticado.getUsuario(), solicitudReubicacion));
        model.addAttribute("solicitudReubicacionInicial", solicitudReubicacion != null ? solicitudReubicacion : "");

        String tipSesion = (String) session.getAttribute("tipDia");
        if (tipSesion == null || tipSesion.trim().isEmpty()) {
            tipSesion = TIPS_DIA[new Random().nextInt(TIPS_DIA.length)];
            session.setAttribute("tipDia", tipSesion);
        }
        model.addAttribute("tipSesion", tipSesion);
        return "menu";
    }

    /** El navegador con la sesion invalidada por login-en-otro-lugar (maximumSessions) solo se
     * entera cuando hace su siguiente peticion al servidor; sin esto, se queda usando la pagina
     * con normalidad hasta que navega a otro lado. El front hace ping aqui cada cierto tiempo:
     * si la sesion sigue viva responde 200, si ya la invalidaron el filtro de Spring Security
     * intercepta esta misma peticion y la redirige a /login?expirado=1 antes de llegar aqui. */
    @GetMapping("/sesion/estado")
    @ResponseBody
    public ResponseEntity<String> estadoSesion() {
        return ResponseEntity.ok("activa");
    }
}
