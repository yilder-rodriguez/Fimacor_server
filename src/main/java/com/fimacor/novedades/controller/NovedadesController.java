package com.fimacor.novedades.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import java.text.Normalizer;
import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fimacor.novedades.service.NovedadesService;

/** Pantalla de novedades: reportar una falla y gestionar su atencion. */
@Controller
public class NovedadesController {

    private final NovedadesService service;
    private final PermisoService permisoService;

    public NovedadesController(NovedadesService service, PermisoService permisoService) {
        this.service = service;
        this.permisoService = permisoService;
    }

    @GetMapping("/novedades")
    public String ver(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model,
            @RequestParam(required = false) String maquina, @RequestParam(defaultValue = "false") boolean reparadas) {
        int idRol = idRol(usuario);
        String nombreRol = usuario != null && usuario.getUsuario().getRol() != null
                ? usuario.getUsuario().getRol().getDescripcionRol() : "";
        String rolNormalizado = normalizar(nombreRol);
        boolean esAdministrador = permisoService.esRolTotal(idRol) || rolNormalizado.contains("administrador");
        boolean esLogistica = rolNormalizado.contains("logistica");
        boolean esTecnico = rolNormalizado.contains("tecnico");

        boolean puedeReportar = puedeReportar(usuario);
        boolean puedeGestionar = esAdministrador || esLogistica || service.puedeGestionarNovedad(idRol);
        boolean esRolTotal = permisoService.esRolTotal(idRol);
        if (!puedeReportar && !puedeGestionar && !esTecnico) return "redirect:/menu";

        model.addAttribute("novedades", service.listarParaVista(usuario != null ? usuario.getUsuario() : null,
                esTecnico, puedeGestionar, reparadas));
        // Quien no puede reportar (por ejemplo el Tecnico) no recibe nada del formulario de registro:
        // ni la lista de maquinas ni el resumen de novedades de otras personas por maquina.
        model.addAttribute("maquinas", puedeReportar ? service.listarMaquinas() : java.util.List.of());
        model.addAttribute("pendientesPorMaquina", puedeReportar ? service.pendientesPorMaquina() : java.util.Map.of());
        model.addAttribute("sedes", puedeReportar ? service.listarSedes() : java.util.List.of());
        model.addAttribute("ambientes", puedeReportar ? service.listarAmbientes() : java.util.List.of());
        model.addAttribute("areas", puedeReportar ? service.listarAreas() : java.util.List.of());
        model.addAttribute("reportadores", puedeReportar ? service.listarReportadores() : java.util.List.of());
        model.addAttribute("tecnicos", service.listarTecnicosDisponibles());
        model.addAttribute("puedeReportar", puedeReportar);
        model.addAttribute("puedeGestionar", puedeGestionar);
        model.addAttribute("esTecnico", esTecnico);
        model.addAttribute("esRolTotal", esRolTotal);
        model.addAttribute("puedeSolicitarBaja", esCuentadante(usuario));
        model.addAttribute("soloReparadas", reparadas);
        model.addAttribute("maquinaSeleccionada", maquina == null ? "" : maquina);
        model.addAttribute("fechaHoy", LocalDate.now().toString());
        model.addAttribute("usuario", usuario != null ? usuario.getUsuario() : null);
        return "novedades";
    }

    @PostMapping("/novedades")
    public String reportar(@RequestParam int maquina, @RequestParam(required = false) Integer reportado_por,
            @RequestParam int sede, @RequestParam int ambiente, @RequestParam("fecha_reporte") String fecha,
            @RequestParam("tipo_falla") String tipoFalla, @RequestParam String area, @RequestParam String descripcion,
            @RequestParam(defaultValue = "false") boolean solicitarBaja,
            @RequestParam(defaultValue = "false") boolean confirmarOtraFalla, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeReportar(usuario)) return "redirect:/menu";

        LocalDate fechaReporte = fecha(fecha);
        NovedadesService.Resultado resultado = service.reportar(maquina, reportado_por, usuario.getUsuario(), sede,
                ambiente, fechaReporte, tipoFalla, area, descripcion, solicitarBaja && esCuentadante(usuario), confirmarOtraFalla);
        if (resultado.ok()) return "redirect:/novedades?novedad=ok";
        return "pendientes".equals(resultado.error())
                ? "redirect:/novedades?error=pendientes&maquina=" + maquina
                : "redirect:/novedades?error=" + resultado.error();
    }

    @PostMapping("/novedades/{id}/asignar")
    public String asignar(@PathVariable int id, @RequestParam("tecnicoAsignado") int idTecnico,
            @RequestParam(required = false) String observacionGestion, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!service.puedeGestionarNovedad(idRol(usuario))) return "redirect:/menu";
        NovedadesService.Resultado r = service.asignar(id, idTecnico, usuario.getUsuario(), observacionGestion);
        return redirigir(id, r.ok());
    }

    /** Solo el Administrador decide sobre la solicitud de baja que hace la Cuentadante. */
    @PostMapping("/novedades/{id}/baja")
    public String aprobarBaja(@PathVariable int id, @RequestParam(required = false) String motivo,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!permisoService.esRolTotal(idRol(usuario))) return "redirect:/menu";
        NovedadesService.Resultado r = service.aprobarSolicitudBaja(id, motivo, usuario.getUsuario());
        return redirigir(id, r.ok());
    }

    @PostMapping("/novedades/{id}/desestimar-baja")
    public String desestimarBaja(@PathVariable int id, @RequestParam(required = false) String observacion,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!permisoService.esRolTotal(idRol(usuario))) return "redirect:/menu";
        NovedadesService.Resultado r = service.desestimarSolicitudBaja(id, observacion, usuario.getUsuario());
        return redirigir(id, r.ok());
    }

    @PostMapping("/novedades/{id}/iniciar")
    public String iniciar(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        int idRol = idRol(usuario);
        boolean esRolTotal = permisoService.esRolTotal(idRol);
        if (!service.puedeAtenderComoTecnico(usuario.getUsuario()) && !service.puedeGestionarNovedad(idRol) && !esRolTotal) return "redirect:/menu";
        NovedadesService.Resultado r = service.iniciar(id, usuario.getUsuario(), esRolTotal);
        return redirigir(id, r.ok());
    }

    @PostMapping("/novedades/{id}/finalizar")
    public String finalizar(@PathVariable int id, @RequestParam(required = false) String observacionReparacion,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        int idRol = idRol(usuario);
        boolean esRolTotal = permisoService.esRolTotal(idRol);
        if (!service.puedeAtenderComoTecnico(usuario.getUsuario()) && !service.puedeGestionarNovedad(idRol) && !esRolTotal) return "redirect:/menu";
        NovedadesService.Resultado r = service.finalizar(id, usuario.getUsuario(), esRolTotal, observacionReparacion);
        return redirigir(id, r.ok());
    }

    @PostMapping("/novedades/{id}/cerrar")
    public String cerrar(@PathVariable int id, @RequestParam(required = false) String observacionGestion,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!service.puedeGestionarNovedad(idRol(usuario))) return "redirect:/menu";
        NovedadesService.Resultado r = service.cerrar(id, observacionGestion, usuario.getUsuario(), esLogistica(usuario));
        return r.ok() ? "redirect:/maquinas/administracion?ok=novedadCerrada" : redirigir(id, false);
    }

    @PostMapping("/novedades/{id}/devolver")
    public String devolver(@PathVariable int id, @RequestParam(required = false) String observacionGestion,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!service.puedeGestionarNovedad(idRol(usuario))) return "redirect:/menu";
        NovedadesService.Resultado r = service.devolver(id, observacionGestion);
        return redirigir(id, r.ok());
    }

    @PostMapping("/novedades/{id}/cancelar")
    public String cancelar(@PathVariable int id, @RequestParam(required = false) String observacionGestion,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!service.puedeGestionarNovedad(idRol(usuario)) || esLogistica(usuario)) return "redirect:/menu";
        NovedadesService.Resultado r = service.cancelar(id, observacionGestion);
        return redirigir(id, r.ok());
    }

    @PostMapping("/novedades/{id}/eliminar")
    public String eliminar(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!service.puedeGestionarNovedad(idRol(usuario)) || esLogistica(usuario)) return "redirect:/menu";
        boolean ok = service.eliminar(id);
        return "redirect:/novedades?" + (ok ? "gestion=ok" : "error=gestion");
    }

    private boolean esCuentadante(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.getUsuario().getRol() == null) return false;
        return normalizar(usuario.getUsuario().getRol().getDescripcionRol()).contains("cuentadante");
    }

    /** Logistica puede aprobar/cerrar novedades, pero ya no cancelarlas ni eliminarlas. */
    private boolean esLogistica(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.getUsuario().getRol() == null) return false;
        return normalizar(usuario.getUsuario().getRol().getDescripcionRol()).contains("logistica");
    }

    private String redirigir(int id, boolean ok) {
        return "redirect:/novedades?" + (ok ? "gestion=ok&novedad=" + id : "error=gestion&novedad=" + id);
    }

    private LocalDate fecha(String valor) {
        try {
            return LocalDate.parse(valor);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean puedeReportar(UsuarioAutenticado usuario) {
        int idRol = idRol(usuario);
        return permisoService.tienePermisoConTodas(idRol, "novedades", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "novedad", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "reporte", "falla")
                || permisoService.tienePermisoConTodas(idRol, "reportes", "falla");
    }

    private int idRol(UsuarioAutenticado usuario) {
        return usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
