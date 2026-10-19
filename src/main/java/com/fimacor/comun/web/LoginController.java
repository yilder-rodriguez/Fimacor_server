package com.fimacor.comun.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    /** El sistema original servia Login.jsp como welcome-file de "/". Aqui no hay welcome-file
     * equivalente, asi que sin este mapeo la raiz del dominio quedaba en 404 (Whitelabel Error
     * Page) en vez de mostrar el login. */
    @GetMapping("/")
    public String raiz() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
