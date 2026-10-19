package com.fimacor.novedades.controller;

import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.novedades.service.NovedadesService;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.UsuarioRepository;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reporte de novedad desde el QR de una maquina, sin sesion: el codigo de
 * instructor (Usuario.codigoInstructor) hace las veces de identificacion.
 * Sede, ambiente y area quedan bloqueados a los de la maquina escaneada
 * (igual que el legacy ReporteNovedadQrServlet).
 */
@Controller
public class ReporteQrController {

    private final NovedadesService novedadesService;
    private final UsuarioRepository usuarioRepository;

    public ReporteQrController(NovedadesService novedadesService, UsuarioRepository usuarioRepository) {
        this.novedadesService = novedadesService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/AbrirReporteNovedadQrServlet")
    public String abrir(@RequestParam int id, Model model) {
        if (novedadesService.buscarMaquina(id) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maquina no valida.");
        }
        model.addAttribute("id", id);
        return "reportar-novedad-qr-cargando";
    }

    @GetMapping("/ReporteNovedadQrServlet")
    public String mostrar(@RequestParam int id, @AuthenticationPrincipal UsuarioAutenticado sesion, Model model) {
        FichaMaquina maquina = maquinaOFallar(id);
        model.addAttribute("maquina", maquina);
        if (maquina.isBaja()) return bloquearPorBaja(model);
        model.addAttribute("codigoSugerido", codigoSugerido(sesion));
        model.addAttribute("pendientes", novedadesService.pendientesDeMaquina(id));
        return "reportar-novedad-qr";
    }

    @PostMapping("/ReporteNovedadQrServlet")
    public String enviar(@RequestParam int id, @RequestParam(name = "codigo_instructor") String codigoInstructor,
            @RequestParam(name = "tipo_falla") String tipoFalla, @RequestParam String descripcion,
            @RequestParam(defaultValue = "false") boolean confirmarOtraFalla,
            @AuthenticationPrincipal UsuarioAutenticado sesion, Model model) {
        FichaMaquina maquina = maquinaOFallar(id);
        model.addAttribute("codigoSugerido", codigoSugerido(sesion));
        model.addAttribute("maquina", maquina);
        model.addAttribute("tipoFallaIngresado", tipoFalla);
        model.addAttribute("descripcionIngresada", descripcion);
        if (maquina.isBaja()) return bloquearPorBaja(model);
        model.addAttribute("pendientes", novedadesService.pendientesDeMaquina(id));

        Usuario instructor = usuarioRepository.buscarInstructorPorCodigo(codigoInstructor).orElse(null);
        if (instructor == null) {
            model.addAttribute("error", "El código no pertenece a un Instructor activo.");
            return "reportar-novedad-qr";
        }
        if (tipoFalla == null || tipoFalla.isBlank() || descripcion == null || descripcion.isBlank()) {
            model.addAttribute("error", "Completa el tipo y la descripción de la novedad.");
            return "reportar-novedad-qr";
        }

        int idSede = maquina.getSede() != null ? maquina.getSede().getIdSede() : 0;
        int idAmbiente = maquina.getAmbiente() != null ? maquina.getAmbiente().getIdAmbientesDeAprendizaje() : 0;
        var resultado = novedadesService.reportar(id, instructor.getIdUsuario(), instructor, idSede, idAmbiente,
                LocalDate.now(), tipoFalla, maquina.getArea(), descripcion, false, confirmarOtraFalla);
        if (!resultado.ok()) {
            model.addAttribute("error", "pendientes".equals(resultado.error())
                    ? "Esta máquina ya tiene novedades o mantenimientos en curso. Revísalos antes de crear otra novedad: "
                            + "si tu falla es distinta, marca la casilla de confirmación y envía de nuevo."
                    : "No se pudo guardar el reporte. Inténtalo nuevamente.");
            return "reportar-novedad-qr";
        }
        model.addAttribute("exito", "Instructor verificado: " + instructor.getNombre() + " " + instructor.getApellido()
                + ". Reporte enviado a Logística.");
        return "reportar-novedad-qr";
    }

    /** Si quien escanea ya inicio sesion como Instructor se le adelanta su codigo: igual se valida al enviar. */
    private String codigoSugerido(UsuarioAutenticado sesion) {
        if (sesion == null) return "";
        Usuario u = sesion.getUsuario();
        boolean esInstructor = u.getRol() != null && u.getRol().getDescripcionRol() != null
                && u.getRol().getDescripcionRol().toLowerCase().contains("instructor");
        return esInstructor && u.isActivo() && u.getCodigoInstructor() != null ? u.getCodigoInstructor() : "";
    }

    private String bloquearPorBaja(Model model) {
        model.addAttribute("error", "Esta máquina fue dada de baja y ya no recibe reportes de novedad.");
        model.addAttribute("bloqueada", true);
        return "reportar-novedad-qr";
    }

    private FichaMaquina maquinaOFallar(int id) {
        FichaMaquina maquina = novedadesService.buscarMaquina(id);
        if (maquina == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina no encontrada.");
        return maquina;
    }
}
