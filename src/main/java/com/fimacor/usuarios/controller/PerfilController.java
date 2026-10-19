package com.fimacor.usuarios.controller;

import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.service.PerfilService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

/** Autoedicion del perfil propio desde el modal de Inicio: datos de contacto, contrasena y foto de perfil. */
@Controller
public class PerfilController {

    private final PerfilService service;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    @PostMapping("/perfil")
    public String actualizar(@RequestParam String nombre, @RequestParam String apellido, @RequestParam String telefono,
            @AuthenticationPrincipal UsuarioAutenticado usuario, HttpServletRequest request, HttpServletResponse response) {
        PerfilService.Resultado resultado = service.actualizarDatos(usuario.getUsuario().getIdUsuario(), nombre, apellido, telefono);
        if (resultado.ok()) refrescarSesion(resultado.usuario(), request, response);
        return "redirect:/menu?" + (resultado.ok() ? "perfil=ok" : "perfilError=" + resultado.error());
    }

    /** Foto de la persona que tiene la sesion abierta. 404 si no tiene; el ETag evita volver a descargarla. */
    @GetMapping("/perfil/foto")
    public ResponseEntity<byte[]> foto(@AuthenticationPrincipal UsuarioAutenticado usuario, WebRequest request) {
        int id = usuario.getUsuario().getIdUsuario();
        Long version = service.versionFoto(id);
        if (version == null) return ResponseEntity.notFound().build();
        String etag = "\"" + version + "\"";
        if (request.checkNotModified(etag)) return null;
        byte[] bytes = service.foto(id);
        if (bytes == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noCache().cachePrivate()).eTag(etag).body(bytes);
    }

    @PostMapping("/perfil/foto")
    public String subirFoto(@RequestParam("foto") MultipartFile foto, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        try {
            PerfilService.Resultado resultado = service.guardarFoto(usuario.getUsuario().getIdUsuario(), foto.getBytes());
            return "redirect:/menu?" + (resultado.ok() ? "fotoPerfil=ok" : "fotoPerfilError=" + resultado.error());
        } catch (IOException e) {
            return "redirect:/menu?fotoPerfilError=foto";
        }
    }

    @PostMapping("/perfil/foto/eliminar")
    public String quitarFoto(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        service.quitarFoto(usuario.getUsuario().getIdUsuario());
        return "redirect:/menu?fotoPerfil=quitada";
    }

    @PostMapping("/perfil/contrasena")
    public String cambiarContrasena(@RequestParam String actual, @RequestParam String nueva, @RequestParam String confirmar,
            @AuthenticationPrincipal UsuarioAutenticado usuario, HttpServletRequest request, HttpServletResponse response) {
        PerfilService.Resultado resultado = service.cambiarContrasena(usuario.getUsuario().getIdUsuario(), actual, nueva, confirmar);
        if (resultado.ok()) refrescarSesion(resultado.usuario(), request, response);
        return "redirect:/menu?" + (resultado.ok() ? "clave=ok" : "claveError=" + resultado.error());
    }

    /** Refresca el principal autenticado para que el resto de la sesion (y esta misma respuesta)
     * vean los datos nuevos, sin tener que cerrar sesion y volver a entrar. */
    private void refrescarSesion(Usuario usuarioActualizado, HttpServletRequest request, HttpServletResponse response) {
        UsuarioAutenticado nuevoPrincipal = new UsuarioAutenticado(usuarioActualizado);
        Authentication anterior = SecurityContextHolder.getContext().getAuthentication();
        Authentication nueva = new UsernamePasswordAuthenticationToken(nuevoPrincipal, anterior.getCredentials(), nuevoPrincipal.getAuthorities());

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(nueva);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);
    }
}
