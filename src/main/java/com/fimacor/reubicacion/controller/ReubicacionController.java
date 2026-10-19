package com.fimacor.reubicacion.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
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
import com.fimacor.reubicacion.entity.ReubicacionMaquina;
import com.fimacor.reubicacion.service.ReubicacionService;
import com.fimacor.reubicacion.util.ReubicacionTexto;

/**
 * Pantalla de reubicacion de maquinas: registrar solicitud, responder
 * (aprobar/rechazar), descargar el acta en PDF, ver la firma digital y
 * eliminar evidencia.
 */
@Controller
public class ReubicacionController {

    private final ReubicacionService service;
    private final PermisoService permisoService;

    public ReubicacionController(ReubicacionService service, PermisoService permisoService) {
        this.service = service;
        this.permisoService = permisoService;
    }

    @GetMapping("/reubicacion")
    public String ver(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        boolean puedeRegistrar = puedeRegistrar(usuario);
        boolean esSubdireccion = esRol(usuario, "subdireccion");
        if (!puedeRegistrar && !esSubdireccion) return "redirect:/menu";

        List<ReubicacionMaquina> reubicaciones = service.listar();
        model.addAttribute("reubicaciones", reubicaciones);
        model.addAttribute("maquinas", service.listarMaquinas());
        model.addAttribute("ambientes", service.listarAmbientes());
        model.addAttribute("sedes", service.listarSedes());
        model.addAttribute("areas", service.listarAreas());
        model.addAttribute("puedeRegistrar", puedeRegistrar);
        int idRol = idRol(usuario);
        model.addAttribute("puedeAutorizar", permisoService.esRolTotal(idRol));
        model.addAttribute("puedeAutorizarExterno", esSubdireccion);
        model.addAttribute("puedeAutorizarInterna", esRol(usuario, "cuentadante"));
        model.addAttribute("puedeEliminarEvidencia", esRol(usuario, "administrador"));
        model.addAttribute("hayEvidenciasReubicacion",
                reubicaciones.stream().anyMatch(r -> ReubicacionTexto.tieneActa(r.getEstadoAutorizacion())));
        return "reubicacion";
    }

    @PostMapping("/reubicacion")
    public String registrar(@RequestParam int maquina, @RequestParam(name = "tipoTraslado", defaultValue = "Interno") String tipoTraslado,
            @RequestParam String origen, @RequestParam("ambienteDestino") int idAmbienteDestino,
            @RequestParam(name = "sedeDestino", defaultValue = "0") int idSedeDestino,
            @RequestParam("areaDestino") String areaDestino, @RequestParam String destino,
            @RequestParam(required = false) String observacion, @RequestParam String descripcion,
            @RequestParam(required = false) MultipartFile evidencia,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeRegistrar(usuario)) return "redirect:/menu";

        try {
            ReubicacionService.Resultado resultado = service.registrar(maquina, tipoTraslado, origen, idAmbienteDestino,
                    idSedeDestino, areaDestino, destino, observacion, descripcion,
                    evidencia != null && !evidencia.isEmpty() ? evidencia.getOriginalFilename() : null,
                    evidencia != null && !evidencia.isEmpty() ? evidencia.getInputStream() : null,
                    usuario.getUsuario());
            if (!resultado.ok()) return "redirect:/reubicacion?error=" + resultado.error();
            return "redirect:/reubicacion?registro=ok&correo=" + (resultado.correoEnviado() ? "ok" : "error");
        } catch (IOException e) {
            return "redirect:/reubicacion?error=1";
        }
    }

    @PostMapping("/reubicacion/{id}/responder")
    public String responder(@PathVariable int id, @RequestParam String accion,
            @RequestParam("firmaDigital") String firmaDigital,
            @RequestParam(required = false) String observacionDecision,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        boolean aprobar = "aceptar".equalsIgnoreCase(accion) || "aprobar".equalsIgnoreCase(accion);
        boolean rechazar = "rechazar".equalsIgnoreCase(accion);
        if (!aprobar && !rechazar) return "redirect:/reubicacion?autorizacion=decision_invalida";

        String responsable = usuario.getUsuario().getNombre() + " " + usuario.getUsuario().getApellido();
        ReubicacionService.Resultado resultado = service.responder(id, aprobar, firmaDigital, responsable,
                observacionDecision, idRol(usuario));
        return "redirect:/reubicacion?autorizacion=" + (resultado.ok() ? "ok" : resultado.error()) + "&solicitudReubicacion=" + id;
    }

    @GetMapping("/ActaReubicacionPdfServlet")
    public ResponseEntity<byte[]> actaPdf(@RequestParam int id) {
        byte[] pdf = service.generarActaPdf(id);
        if (pdf == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Acta_Traslado_" + id + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/FirmaReubicacionServlet")
    public ResponseEntity<byte[]> firma(@RequestParam int id) {
        byte[] firma = service.obtenerFirma(id);
        if (firma == null || firma.length == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(firma);
    }

    @GetMapping("/reubicacion/{id}/evidencia")
    public ResponseEntity<?> evidencia(@PathVariable int id) {
        Resource recurso = service.cargarEvidenciaPublica(id);
        if (recurso == null || !recurso.exists()) {
            // Con cuerpo, el navegador y la app movil muestran este aviso en vez de su pagina de error generica.
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML)
                    .body("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Evidencia no disponible</title></head>"
                            + "<body style=\"font-family:system-ui,sans-serif;max-width:32rem;margin:3rem auto;padding:0 1rem;text-align:center\">"
                            + "<h1 style=\"font-size:1.4rem\">Evidencia no disponible</h1>"
                            + "<p>El archivo de esta evidencia ya no existe o no se pudo encontrar.</p>"
                            + "<p><a href=\"javascript:history.back()\">Volver</a></p></body></html>");
        }
        return ResponseEntity.ok()
                .contentType(com.fimacor.comun.archivos.FileStorageService.tipoContenido(recurso.getFilename()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(recurso);
    }

    @PostMapping("/EliminarEvidenciaReubicacionServlet")
    public String eliminarEvidencia(@RequestParam(name = "idReubicacion") List<Integer> ids,
            @RequestParam(required = false) String accion, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!esRol(usuario, "administrador")) return "redirect:/menu?evidencia=sin_permiso";
        boolean eliminarRegistro = "registro".equalsIgnoreCase(accion);
        boolean ok = service.eliminarEvidencias(ids, eliminarRegistro);
        String parametro = eliminarRegistro ? "registro" : "evidencia";
        return "redirect:/reubicacion?" + parametro + "=" + (ok ? (eliminarRegistro ? "eliminado" : "eliminada") : "error");
    }

    private boolean puedeRegistrar(UsuarioAutenticado usuario) {
        int idRol = idRol(usuario);
        return esRol(usuario, "cuentadante")
                || permisoService.tienePermisoConTodas(idRol, "reubicacion", "registrar")
                || permisoService.tienePermisoConTodas(idRol, "reubicacion", "reubicar")
                || permisoService.tienePermisoConTodas(idRol, "maquinas", "reubicar")
                || permisoService.tienePermisoConTodas(idRol, "maquina", "reubicar");
    }

    private boolean esRol(UsuarioAutenticado usuario, String fragmento) {
        if (usuario == null || usuario.getUsuario().getRol() == null) return false;
        String normalizado = Normalizer.normalize(usuario.getUsuario().getRol().getDescripcionRol(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        return normalizado.contains(fragmento);
    }

    private int idRol(UsuarioAutenticado usuario) {
        return usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
    }
}
