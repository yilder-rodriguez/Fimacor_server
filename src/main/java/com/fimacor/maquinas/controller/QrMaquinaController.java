package com.fimacor.maquinas.controller;

import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.service.FichaTecnicaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Pagina publica que abre el QR pegado en cada maquina. Cualquiera que lo
 * escanee ve los datos de la maquina; reportar una novedad desde ahi exige el
 * codigo de validacion de un Instructor (ver ReporteQrController).
 */
@Controller
public class QrMaquinaController {

    private final FichaTecnicaService fichaService;

    public QrMaquinaController(FichaTecnicaService fichaService) {
        this.fichaService = fichaService;
    }

    @GetMapping("/MaquinaQrServlet")
    public String ver(@RequestParam int id, Model model) {
        FichaMaquina maquina = fichaService.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina no encontrada"));
        model.addAttribute("maquina", maquina);
        return "maquina-qr";
    }
}
