package com.fimacor.auth.controller;

import com.fimacor.usuarios.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fimacor.auth.service.AdminShortcutAuthService;

/**
 * Endpoints JSON que consume el formulario de registro por fetch(): validar
 * datos unicos en vivo y el acceso rapido administrativo.
 */
@RestController
public class RegistroApiController {

    private final UsuarioRepository usuarioRepository;
    private final AdminShortcutAuthService adminShortcutAuthService;

    public RegistroApiController(UsuarioRepository usuarioRepository,
            AdminShortcutAuthService adminShortcutAuthService) {
        this.usuarioRepository = usuarioRepository;
        this.adminShortcutAuthService = adminShortcutAuthService;
    }

    @GetMapping(value = "/ValidarDatoRegistroServlet", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> validarDato(@RequestParam(required = false) String tipo,
            @RequestParam(required = false) String valor) {
        String tipoLimpio = tipo == null ? "" : tipo.trim();
        String valorLimpio = valor == null ? "" : valor.trim();
        boolean existe;
        if ("correo".equalsIgnoreCase(tipoLimpio) && !valorLimpio.isEmpty()) {
            existe = usuarioRepository.existsByCorreo(valorLimpio);
        } else if ("documento".equalsIgnoreCase(tipoLimpio) && !valorLimpio.isEmpty()) {
            existe = usuarioRepository.existsByDocumentoIdentidad(valorLimpio);
        } else if ("telefono".equalsIgnoreCase(tipoLimpio) && !valorLimpio.isEmpty()) {
            existe = usuarioRepository.existsByNumeroTelefono(valorLimpio);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"ok\":false,\"mensaje\":\"Dato no valido.\"}");
        }
        return ResponseEntity.ok("{\"ok\":true,\"existe\":" + existe + "}");
    }

    @PostMapping(value = "/AdminShortcutAuthServlet", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> adminShortcutAuth(HttpSession session,
            @RequestParam(required = false) String codigo, @RequestParam(required = false) String correo,
            @RequestParam(required = false) String contrasena) {
        AdminShortcutAuthService.Resultado resultado = (codigo != null && !codigo.trim().isEmpty())
                ? adminShortcutAuthService.verificarCodigo(session, codigo)
                : adminShortcutAuthService.solicitarCodigo(session, correo, contrasena);

        String json = resultado.ok()
                ? "{\"ok\":true" + (resultado.requiereCodigo() ? ",\"requiereCodigo\":true" : "")
                        + (resultado.autorizado() ? ",\"autorizado\":true" : "")
                        + ",\"mensaje\":\"" + escaparJson(resultado.mensaje()) + "\"}"
                : "{\"ok\":false,\"mensaje\":\"" + escaparJson(resultado.mensaje()) + "\"}";
        return ResponseEntity.status(resultado.status()).body(json);
    }

    private String escaparJson(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
