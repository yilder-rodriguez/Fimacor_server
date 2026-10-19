package com.fimacor.maquinas.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.manuales.entity.Manual;
import com.fimacor.manuales.service.ManualesService;
import com.fimacor.mantenimiento.entity.HistorialMantenimiento;
import com.fimacor.mantenimiento.repository.HistorialMantenimientoRepository;
import com.fimacor.mantenimiento.entity.Mantenimiento;
import com.fimacor.mantenimiento.repository.MantenimientoRepository;
import com.fimacor.mantenimiento.util.MantenimientoTexto;
import com.fimacor.mantenimiento.entity.TipoMantenimiento;
import com.fimacor.mantenimiento.repository.TipoMantenimientoRepository;
import com.fimacor.novedades.entity.Novedad;
import com.fimacor.novedades.repository.NovedadRepository;
import com.fimacor.reubicacion.entity.ReubicacionMaquina;
import com.fimacor.reubicacion.repository.ReubicacionMaquinaRepository;
import com.fimacor.usuarios.entity.Usuario;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.service.MaquinasService;

/**
 * Listado principal de maquinas, con manuales, historial de reubicacion y
 * reparacion inmediata embebidos en el modal de cada fila, y una vista
 * filtrada para tecnicos que solo muestra sus maquinas pendientes de reparar.
 */
@Controller
public class MaquinaListadoController {

    private final MaquinasService service;
    private final PermisoService permisoService;
    private final ManualesService manualesService;
    private final ReubicacionMaquinaRepository reubicacionRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final HistorialMantenimientoRepository historialRepository;
    private final NovedadRepository novedadRepository;
    private final TipoMantenimientoRepository tipoMantenimientoRepository;

    public MaquinaListadoController(MaquinasService service, PermisoService permisoService,
            ManualesService manualesService, ReubicacionMaquinaRepository reubicacionRepository,
            MantenimientoRepository mantenimientoRepository, HistorialMantenimientoRepository historialRepository,
            NovedadRepository novedadRepository, TipoMantenimientoRepository tipoMantenimientoRepository) {
        this.service = service;
        this.permisoService = permisoService;
        this.manualesService = manualesService;
        this.reubicacionRepository = reubicacionRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.historialRepository = historialRepository;
        this.novedadRepository = novedadRepository;
        this.tipoMantenimientoRepository = tipoMantenimientoRepository;
    }

    @GetMapping("/maquinas")
    public String ver(@AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado, Model model,
            @RequestParam(required = false) String maquina) {
        int idRol = idRol(usuarioAutenticado);
        Usuario usuarioSesion = usuarioAutenticado != null ? usuarioAutenticado.getUsuario() : null;
        boolean esTecnico = usuarioSesion != null && usuarioSesion.getRol() != null
                && normalizar(usuarioSesion.getRol().getDescripcionRol()).contains("tecnico");
        boolean esLogistica = usuarioSesion != null && usuarioSesion.getRol() != null
                && normalizar(usuarioSesion.getRol().getDescripcionRol()).contains("logistica");

        boolean puedeEditar = !esTecnico && !esLogistica && (permisoService.tienePermisoConTodas(idRol, "maquinas", "editar")
                || permisoService.tienePermisoConTodas(idRol, "maquina", "editar"));
        boolean puedeDarDeBaja = permisoService.esRolTotal(idRol);
        boolean puedeModificar = puedeEditar || puedeDarDeBaja;
        boolean puedeEliminarManuales = !esTecnico && !esLogistica && (permisoService.tienePermisoConTodas(idRol, "manuales", "eliminar")
                || permisoService.tienePermisoConTodas(idRol, "manual", "eliminar") || permisoService.esRolTotal(idRol));
        boolean puedeRegistrar = !esTecnico && !esLogistica && (permisoService.tienePermisoConTodas(idRol, "maquinas", "crear")
                || permisoService.tienePermisoConTodas(idRol, "maquinas", "registrar")
                || permisoService.tienePermisoConTodas(idRol, "maquina", "crear")
                || permisoService.tienePermisoConTodas(idRol, "maquina", "registrar"));
        boolean puedeVerHistorial = !esTecnico && (permisoService.tienePermisoConTodas(idRol, "mantenimiento", "historial")
                || permisoService.tienePermiso(idRol, "historial", "mantenimiento"));
        boolean tecnicoPuedeReparar = permisoService.tienePermisoConTodas(idRol, "reparacion", "reparar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "confirmar");
        boolean puedeReportarNovedad = permisoService.tienePermisoConTodas(idRol, "novedades", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "novedad", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "reporte", "falla")
                || permisoService.tienePermisoConTodas(idRol, "reportes", "falla");

        List<FichaMaquina> maquinas = service.listar();
        if (esTecnico) {
            maquinas = filtrarParaTecnico(maquinas, usuarioSesion, maquina);
        }

        Map<Integer, List<Manual>> manualesPorMaquina = new HashMap<>();
        Map<Integer, ReubicacionMaquina> reubicacionPorMaquina = new HashMap<>();
        for (FichaMaquina m : maquinas) {
            manualesPorMaquina.put(m.getIdMaquinas(), manualesService.listarPorMaquina(m.getIdMaquinas()));
            if (m.getReubicacionId() != null && m.getReubicacionId() > 0) {
                reubicacionRepository.findById(m.getReubicacionId()).ifPresent(r -> reubicacionPorMaquina.put(m.getIdMaquinas(), r));
            }
        }

        model.addAttribute("maquinas", maquinas);
        model.addAttribute("tipos", service.listarTipos());
        model.addAttribute("marcas", service.listarMarcas());
        model.addAttribute("modelos", service.listarModelos());
        model.addAttribute("ambientes", service.listarAmbientes());
        model.addAttribute("estados", service.listarEstados());
        model.addAttribute("sedes", service.listarSedes());
        model.addAttribute("areas", service.listarAreas());
        model.addAttribute("manualesPorMaquina", manualesPorMaquina);
        model.addAttribute("reubicacionPorMaquina", reubicacionPorMaquina);
        model.addAttribute("puedeEditar", puedeEditar);
        model.addAttribute("puedeDarDeBaja", puedeDarDeBaja);
        model.addAttribute("puedeModificar", puedeModificar);
        model.addAttribute("puedeEliminarManuales", puedeEliminarManuales);
        model.addAttribute("puedeRegistrar", puedeRegistrar);
        model.addAttribute("puedeVerHistorial", puedeVerHistorial);
        model.addAttribute("tecnicoPuedeReparar", tecnicoPuedeReparar);
        model.addAttribute("puedeReportarNovedad", puedeReportarNovedad);
        model.addAttribute("sinExport", esTecnico || esLogistica);
        model.addAttribute("esTecnico", esTecnico);
        return "maquinas";
    }

    /** Filtra las maquinas para un tecnico: solo las pendientes de reparar por
     * el, mas la que llega por el enlace "Ver maquina" desde su listado de reparadas. */
    private List<FichaMaquina> filtrarParaTecnico(List<FichaMaquina> maquinas, Usuario tecnico, String maquinaParam) {
        Set<Integer> pendientes = new HashSet<>();
        Set<Integer> reparadas = new HashSet<>();
        int maquinaSolicitada = parseInt(maquinaParam);
        List<TipoMantenimiento> tipos = tipoMantenimientoRepository.findAll();

        if (tecnico != null) {
            for (Mantenimiento m : mantenimientoRepository.findAll()) {
                if (m.getTecnico() == null || !tecnico.getIdUsuario().equals(m.getTecnico().getIdUsuario())) continue;
                if (esMantenimientoCorrectivo(m, tipos)) pendientes.add(m.getMaquina().getIdMaquinas());
            }
        }
        for (HistorialMantenimiento h : historialRepository.findAll()) {
            if (MantenimientoTexto.estaAsignadoHistorial(h) && MantenimientoTexto.asignadoAlUsuario(h, tecnico) && h.getMaquina() != null) {
                pendientes.add(h.getMaquina().getIdMaquinas());
            }
        }
        if (tecnico != null) {
            for (Novedad n : novedadRepository.findAll()) {
                if (n.getTecnicoAsignado() == null || !tecnico.getIdUsuario().equals(n.getTecnicoAsignado().getIdUsuario())) continue;
                String estado = n.getEstadoFlujo() == null ? "" : n.getEstadoFlujo().trim();
                if (("FINALIZADA_TECNICO".equalsIgnoreCase(estado) || "CERRADA".equalsIgnoreCase(estado)) && n.getMaquina() != null) {
                    reparadas.add(n.getMaquina().getIdMaquinas());
                }
            }
        }
        if (maquinaSolicitada > 0 && reparadas.contains(maquinaSolicitada)) {
            pendientes.add(maquinaSolicitada);
        }

        return maquinas.stream().filter(m -> pendientes.contains(m.getIdMaquinas())).toList();
    }

    private boolean esMantenimientoCorrectivo(Mantenimiento m, List<TipoMantenimiento> tipos) {
        if (m.getTipoMantenimiento() != null) {
            String d = normalizar(m.getTipoMantenimiento().getDescripcionMantenimiento());
            return d.contains("correctivo") || d.contains("repar");
        }
        String d = normalizar(m.getDescripcionMantenimiento());
        return d.contains("correctivo") || d.contains("repar");
    }

    private int parseInt(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (Exception e) {
            return 0;
        }
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private int idRol(UsuarioAutenticado usuario) {
        return usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
    }
}
