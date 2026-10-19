package com.fimacor.ayuda.controller;

import com.fimacor.ayuda.service.AyudaService;
import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.usuarios.repository.RolRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
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

/**
 * Centro de ayuda: manuales de usuario del sistema y videos. Lo ve cualquier persona con sesion
 * (segun el rol al que va dirigido cada recurso); solo el Administrador agrega o quita.
 */
@Controller
public class AyudaController {

    private final AyudaService service;
    private final RolRepository rolRepository;
    private final PermisoService permisoService;

    public AyudaController(AyudaService service, RolRepository rolRepository, PermisoService permisoService) {
        this.service = service;
        this.rolRepository = rolRepository;
        this.permisoService = permisoService;
    }

    @GetMapping("/ayuda")
    public String ver(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        int idRol = idRol(usuario);
        boolean gestiona = puedeGestionar(usuario);
        model.addAttribute("recursos", service.listar(idRol, gestiona));
        model.addAttribute("puedeGestionar", gestiona);
        if (gestiona) model.addAttribute("roles", rolRepository.findAllByOrderByIdRolAsc());
        return "ayuda";
    }

    @PostMapping("/ayuda")
    public String crear(@RequestParam String titulo, @RequestParam(required = false) String descripcion,
            @RequestParam String tipo, @RequestParam String fuente,
            @RequestParam(required = false) String enlace, @RequestParam(required = false) Integer rol,
            @RequestParam(required = false) MultipartFile archivo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/ayuda";
        byte[] contenido = null;
        String nombre = null;
        try {
            if (archivo != null && !archivo.isEmpty()) {
                nombre = archivo.getOriginalFilename();
                contenido = archivo.getBytes();
            }
        } catch (IOException e) {
            return "redirect:/ayuda?error=archivo";
        }
        AyudaService.Resultado resultado = service.crear(titulo, descripcion, tipo, fuente, enlace, rol, nombre, contenido,
                usuario.getUsuario().getCorreo());
        return resultado.ok() ? "redirect:/ayuda?ok=guardado" : "redirect:/ayuda?error=" + resultado.error();
    }

    @PostMapping("/ayuda/{id}/eliminar")
    public String eliminar(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/ayuda";
        return "redirect:/ayuda?" + (service.eliminar(id) ? "ok=eliminado" : "error=eliminar");
    }

    @GetMapping("/ayuda/{id}/archivo")
    public ResponseEntity<byte[]> archivo(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        AyudaService.Archivo pdf = service.archivo(id, idRol(usuario), puedeGestionar(usuario));
        if (pdf == null || pdf.contenido() == null) return ResponseEntity.notFound().build();
        String nombre = pdf.nombre() == null || pdf.nombre().isBlank() ? "manual.pdf" : pdf.nombre();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(nombre, StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(pdf.contenido());
    }

    private boolean puedeGestionar(UsuarioAutenticado usuario) {
        return usuario != null && permisoService.esRolTotal(idRol(usuario));
    }

    private int idRol(UsuarioAutenticado usuario) {
        return usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
    }
}
