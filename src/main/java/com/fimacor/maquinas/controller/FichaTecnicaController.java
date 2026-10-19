package com.fimacor.maquinas.controller;

import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.service.FichaTecnicaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ficha tecnica publica de una maquina: pensada para el QR pegado en la
 * maquina, por eso no exige sesion (ver SecurityConfig). Muestra una pantalla
 * de transicion y luego el PDF, igual que el flujo legacy
 * CargarFichaPdf.jsp -> FichaTecnicaPdfServlet.
 */
@Controller
public class FichaTecnicaController {

    private final FichaTecnicaService service;

    public FichaTecnicaController(FichaTecnicaService service) {
        this.service = service;
    }

    @GetMapping("/FichaTecnicaPublicaServlet")
    public String cargando(@RequestParam int id, Model model) {
        if (!service.buscar(id).isPresent()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Maquina no encontrada");
        }
        model.addAttribute("id", id);
        return "ficha-tecnica-cargando";
    }

    @GetMapping("/FichaTecnicaPublicaPdfServlet")
    public ResponseEntity<byte[]> pdf(@RequestParam int id) {
        FichaMaquina maquina = service.buscar(id).orElse(null);
        if (maquina == null) return ResponseEntity.notFound().build();
        byte[] pdf = service.generarPdf(maquina);
        String codigo = (maquina.getCodigoSena() == null ? "maquina_" + id : maquina.getCodigoSena())
                .replaceAll("[^A-Za-z0-9_-]", "_");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Ficha_Tecnica_" + codigo + ".pdf\"")
                .header("X-Robots-Tag", "noindex, nofollow, noarchive")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0")
                .body(pdf);
    }
}
