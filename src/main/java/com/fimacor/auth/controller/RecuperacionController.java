package com.fimacor.auth.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fimacor.auth.service.RecuperacionService;

/** Flujo publico de recuperacion de contrasena, sin necesidad de sesion. */
@Controller
public class RecuperacionController {

    private final RecuperacionService service;

    public RecuperacionController(RecuperacionService service) {
        this.service = service;
    }

    @GetMapping("/recuperar")
    public String verFormulario() {
        return "recuperar";
    }

    @PostMapping("/recuperar")
    public String solicitar(HttpSession session, @RequestParam String correo) {
        RecuperacionService.Resultado resultado = service.solicitar(session, correo);
        return resultado.ok() ? "redirect:/recuperar/verificar" : "redirect:/recuperar?error=" + resultado.error();
    }

    @GetMapping("/recuperar/verificar")
    public String verVerificacion() {
        return "recuperar-verificar";
    }

    @PostMapping("/recuperar/verificar")
    public String cambiar(HttpSession session, @RequestParam String codigo, @RequestParam String nuevaPassword) {
        RecuperacionService.Resultado resultado = service.cambiar(session, codigo, nuevaPassword);
        return resultado.ok() ? "redirect:/login?recuperado=1" : "redirect:/recuperar/verificar?error=" + resultado.error();
    }

    @PostMapping("/recuperar/reenviar")
    public String reenviar(HttpSession session) {
        RecuperacionService.Resultado resultado = service.reenviar(session);
        return "redirect:/recuperar/verificar?" + (resultado.ok() ? "reenviado=1" : "error=" + resultado.error());
    }
}
