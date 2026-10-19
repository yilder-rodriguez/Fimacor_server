package com.fimacor.manuales.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.fimacor.manuales.entity.Manual;
import com.fimacor.manuales.service.ManualesService;

/** Pantalla de manuales: listar, subir, ver y administrar. */
@Controller
public class ManualesController {

    private final ManualesService service;
    private final PermisoService permisoService;

    public ManualesController(ManualesService service, PermisoService permisoService) {
        this.service = service;
        this.permisoService = permisoService;
    }

    @GetMapping("/manuales")
    public String ver(@AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado, Model model) {
        if (esTecnico(usuarioAutenticado) || !puedeVer(usuarioAutenticado)) return "redirect:/menu";
        model.addAttribute("puedeSubir", puedeSubir(usuarioAutenticado));
        model.addAttribute("maquinas", service.listarMaquinas());
        model.addAttribute("tiposManual", service.listarTiposManual());
        return "manuales";
    }

    @PostMapping("/manuales")
    public String registrar(@RequestParam("maquina") int idMaquina, @RequestParam("tipo_manual") int idTipoManual,
            @RequestParam(required = false) String descripcion, @RequestParam("fecha_subida") String fechaSubida,
            @RequestParam(name = "fuenteManual", defaultValue = "archivo") String fuenteManual,
            @RequestParam(required = false) String linkManual,
            @RequestParam(required = false) MultipartFile archivo,
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado) {
        if (!puedeSubir(usuarioAutenticado)) return "redirect:/menu";

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaSubida);
        } catch (Exception e) {
            return "redirect:/manuales?error=datos";
        }

        try {
            ManualesService.Resultado resultado = service.registrar(idMaquina, idTipoManual, descripcion, fecha,
                    fuenteManual, linkManual,
                    archivo != null ? archivo.getOriginalFilename() : null,
                    archivo != null && !archivo.isEmpty() ? archivo.getInputStream() : null);
            return resultado.ok() ? "redirect:/manuales?ok=1" : "redirect:/manuales?error=" + resultado.error();
        } catch (IOException e) {
            return "redirect:/manuales?error=servidor";
        }
    }

    @GetMapping("/manuales/{id}/archivo")
    public ResponseEntity<Resource> verArchivo(@PathVariable int id) {
        Manual manual = service.buscar(id);
        if (manual == null || manual.esEnlace()) return ResponseEntity.notFound().build();

        Resource recurso = service.cargarArchivo(manual.getRutaManual());
        if (recurso == null || !recurso.exists()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + manual.getRutaManual() + "\"")
                .body(recurso);
    }

    @PostMapping("/manuales/{id}/eliminar")
    public String eliminar(@PathVariable int id, @RequestParam(required = false) Integer idMaquina,
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado) {
        int idRol = idRol(usuarioAutenticado);
        boolean puedeEliminar = permisoService.tienePermisoConTodas(idRol, "manuales", "eliminar")
                || permisoService.tienePermisoConTodas(idRol, "manual", "eliminar")
                || permisoService.esRolTotal(idRol);
        if (!puedeEliminar) return "redirect:/menu";

        boolean ok = service.eliminar(id);
        return "redirect:/manuales?" + (ok ? "ok=eliminado" : "error=manual");
    }

    private boolean puedeSubir(UsuarioAutenticado usuario) {
        int idRol = idRol(usuario);
        return permisoService.tienePermisoConTodas(idRol, "manuales", "subir")
                || permisoService.tienePermisoConTodas(idRol, "manual", "subir")
                || permisoService.tienePermisoConTodas(idRol, "manuales", "crear")
                || permisoService.tienePermisoConTodas(idRol, "manual", "crear");
    }

    private boolean puedeVer(UsuarioAutenticado usuario) {
        int idRol = idRol(usuario);
        return puedeSubir(usuario)
                || permisoService.tienePermisoConTodas(idRol, "manuales", "ver")
                || permisoService.tienePermiso(idRol, "manual", "manuales");
    }

    private boolean esTecnico(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.getUsuario().getRol() == null) return false;
        String normalizado = Normalizer.normalize(usuario.getUsuario().getRol().getDescripcionRol(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        return normalizado.contains("tecnico");
    }

    private int idRol(UsuarioAutenticado usuario) {
        return usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
    }
}
