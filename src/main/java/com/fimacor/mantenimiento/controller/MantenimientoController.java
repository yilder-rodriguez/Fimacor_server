package com.fimacor.mantenimiento.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.usuarios.entity.Usuario;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.fimacor.mantenimiento.entity.Mantenimiento;
import com.fimacor.mantenimiento.service.MantenimientoService;
import com.fimacor.mantenimiento.util.MantenimientoTexto;

/** Pantallas de mantenimiento: programar, ver historial y administrar. */
@Controller
public class MantenimientoController {

    private final MantenimientoService service;
    private final PermisoService permisoService;

    public MantenimientoController(MantenimientoService service, PermisoService permisoService) {
        this.service = service;
        this.permisoService = permisoService;
    }

    @GetMapping("/mantenimiento/programar")
    public String verProgramar(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        int idRol = idRol(usuario);
        boolean esTecnico = usuario != null && usuario.getUsuario().getRol() != null
                && normalizar(usuario.getUsuario().getRol().getDescripcionRol()).contains("tecnico");
        Usuario usuarioActual = usuario != null ? usuario.getUsuario() : null;
        boolean puedeGestionar = esTecnico || puedeRegistrar(usuario) || permisoService.actuaComoTecnico(usuarioActual);
        if (!puedeGestionar) return "redirect:/menu";

        boolean verSoloAsignados = usuarioActual != null
                && (esTecnico || service.puedeSerResponsable(usuarioActual))
                && !permisoService.esRolTotal(idRol);

        List<Mantenimiento> mantenimientos = verSoloAsignados
                ? service.listarPorResponsable(usuarioActual.getIdUsuario())
                : service.listarProgramados();
        LocalDate hoy = LocalDate.now();
        long totalPendientes = mantenimientos.stream().filter(m -> MantenimientoTexto.pendiente(m, hoy)).count();

        model.addAttribute("maquinas", service.listarMaquinas());
        model.addAttribute("tipos", service.listarTipos());
        model.addAttribute("tecnicos", service.listarTecnicosDisponibles());
        model.addAttribute("mantenimientos", mantenimientos);
        model.addAttribute("verSoloAsignados", verSoloAsignados);
        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("hoy", hoy);
        model.addAttribute("totalPendientes", totalPendientes);
        return "mantenimiento-programar";
    }

    @PostMapping("/mantenimiento")
    public String registrar(@RequestParam("maquina") int idMaquina, @RequestParam String fecha,
            @RequestParam(required = false) String hora, @RequestParam String frecuencia,
            @RequestParam("tipo_mantenimiento") int idTipo, @RequestParam("responsable") int idTecnico,
            @RequestParam String tareas, @RequestParam(required = false) List<MultipartFile> evidenciaMantenimiento,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeRegistrar(usuario)) return "redirect:/menu";

        List<MantenimientoService.EvidenciaArchivo> evidencias = new ArrayList<>();
        if (evidenciaMantenimiento != null) {
            for (MultipartFile archivo : evidenciaMantenimiento) {
                if (archivo == null || archivo.isEmpty()) continue;
                try {
                    evidencias.add(new MantenimientoService.EvidenciaArchivo(archivo.getOriginalFilename(), archivo.getInputStream()));
                } catch (IOException ignorado) {
                    // Se omite este archivo si no se puede leer.
                }
            }
        }

        MantenimientoService.Resultado resultado = service.registrarProgramado(idMaquina, fecha(fecha), hora,
                frecuencia, idTipo, idTecnico, tareas, usuario.getUsuario(), evidencias);
        return resultado.ok()
                ? "redirect:/mantenimiento/programar?estado=ok"
                : "redirect:/mantenimiento/programar?error=" + resultado.error();
    }

    @GetMapping("/mantenimiento/evidencia/{archivo}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> evidencia(@PathVariable String archivo) {
        var recurso = service.cargarEvidencia(archivo);
        if (recurso == null) return org.springframework.http.ResponseEntity.notFound().build();
        return org.springframework.http.ResponseEntity.ok()
                .contentType(com.fimacor.comun.archivos.FileStorageService.tipoContenido(archivo))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(recurso);
    }

    @GetMapping("/mantenimiento/historial")
    public String verHistorial(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        int idRol = idRol(usuario);
        boolean esTecnico = usuario != null && usuario.getUsuario().getRol() != null
                && normalizar(usuario.getUsuario().getRol().getDescripcionRol()).contains("tecnico");
        boolean permisoVerHistorial = permisoService.tienePermiso(idRol, "historial", "mantenimiento");
        boolean permisoConfirmarReparacion = permisoService.tienePermisoConTodas(idRol, "reparacion", "reparar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "confirmar")
                || permisoService.actuaComoTecnico(usuario != null ? usuario.getUsuario() : null);
        if (!permisoVerHistorial && !permisoConfirmarReparacion) return "redirect:/menu";

        boolean esLogistica = normalizar(usuario != null && usuario.getUsuario().getRol() != null
                ? usuario.getUsuario().getRol().getDescripcionRol() : "").contains("logistica") || permisoService.esRolTotal(idRol);

        model.addAttribute("historial", service.listarHistorial());
        model.addAttribute("usuarioActual", usuario != null ? usuario.getUsuario() : null);
        model.addAttribute("puedeRegistrarMantenimiento", puedeRegistrar(usuario));
        model.addAttribute("puedeConfirmarReparacionHistorial", permisoConfirmarReparacion || esTecnico);
        model.addAttribute("puedeConfirmarCualquierAsignacion",
                permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar")
                        || permisoService.tienePermisoConTodas(idRol, "reparacion", "gestionar"));
        model.addAttribute("esLogistica", esLogistica);
        model.addAttribute("puedeEliminarHistorial", puedeEliminarHistorial(usuario));
        return "mantenimiento-historial";
    }

    @PostMapping("/mantenimiento/historial/{id}/confirmar")
    public String confirmar(@PathVariable int id, @RequestParam(defaultValue = "finalizar") String accion,
            @RequestParam(required = false) String observacionReparacion,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        MantenimientoService.Resultado resultado = service.confirmarAccion(id, accion, observacionReparacion, usuario.getUsuario());
        return "redirect:/mantenimiento/historial?historial=" + id + "&mantenimiento=" + (resultado.ok() ? "ok" : "error");
    }

    @PostMapping("/mantenimiento/historial/{id}/eliminar")
    public String eliminarHistorial(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeEliminarHistorial(usuario)) return "redirect:/mantenimiento/historial?error=eliminar";
        boolean ok = service.eliminarHistorial(id);
        return "redirect:/mantenimiento/historial?" + (ok ? "ok=eliminado" : "error=eliminar");
    }

    /** Logistica, Tecnico e Instructor no pueden borrar historial, aunque tengan algun permiso suelto. */
    private boolean puedeEliminarHistorial(UsuarioAutenticado usuario) {
        String rolNormalizado = normalizar(usuario != null && usuario.getUsuario().getRol() != null
                ? usuario.getUsuario().getRol().getDescripcionRol() : "");
        if (rolNormalizado.contains("logistica") || rolNormalizado.contains("tecnico") || rolNormalizado.contains("instructor")) {
            return false;
        }
        int idRol = idRol(usuario);
        return permisoService.tienePermisoConTodas(idRol, "mantenimiento", "administrar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "eliminar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "eliminar")
                || permisoService.tienePermisoConTodas(idRol, "historial", "eliminar");
    }

    @PostMapping("/mantenimiento/reparar-rapido")
    public String repararRapido(@RequestParam int idMaquina, @RequestParam(required = false) String observacionReparacion,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        int idRol = idRol(usuario);
        boolean puedeReparar = permisoService.tienePermisoConTodas(idRol, "reparacion", "reparar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "confirmar");
        if (!puedeReparar) return "redirect:/menu";
        // Un tecnico solo puede marcar como reparadas las maquinas que le asignaron.
        if (!permisoService.esRolTotal(idRol) && !service.tieneAsignada(idMaquina, usuario.getUsuario())) {
            return "redirect:/maquinas?reparacion=error";
        }

        MantenimientoService.Resultado resultado = service.repararRapido(idMaquina, observacionReparacion, usuario.getUsuario());
        return resultado.ok()
                ? "redirect:/maquinas?maquina=" + idMaquina + "&reparacion=ok"
                : "redirect:/maquinas?reparacion=error";
    }

    @GetMapping("/mantenimiento/administracion")
    public String verAdministracion(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        boolean esTecnico = usuario != null && usuario.getUsuario().getRol() != null
                && normalizar(usuario.getUsuario().getRol().getDescripcionRol()).contains("tecnico");
        if (esTecnico || !puedeAdministrar(usuario, "ver")) return "redirect:/menu";
        model.addAttribute("mantenimientos", service.listarProgramados());
        model.addAttribute("tecnicos", service.listarTecnicosDisponibles());
        return "mantenimiento-administracion";
    }

    @PostMapping("/mantenimiento/{id}/tecnico")
    public String cambiarTecnico(@PathVariable int id, @RequestParam int responsable,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeAdministrar(usuario, "editar")) return "redirect:/menu";
        boolean ok = service.cambiarTecnico(id, responsable);
        return "redirect:/mantenimiento/administracion?" + (ok ? "ok=tecnico" : "error=actualizar");
    }

    @PostMapping("/mantenimiento/{id}/reprogramar")
    public String reprogramar(@PathVariable int id, @RequestParam String fecha, @RequestParam("fecha_proxima") String fechaProxima,
            @RequestParam String frecuencia, @RequestParam String tareas,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeAdministrar(usuario, "editar")) return "redirect:/menu";
        boolean ok = service.reprogramar(id, fecha(fecha), fecha(fechaProxima), frecuencia, tareas);
        return "redirect:/mantenimiento/administracion?" + (ok ? "ok=reprogramado" : "error=actualizar");
    }

    @PostMapping("/mantenimiento/cancelar")
    public String cancelar(@RequestParam("idMantenimiento") List<Integer> ids,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeAdministrar(usuario, "eliminar")) return "redirect:/menu";
        boolean ok = service.cancelar(ids);
        return "redirect:/mantenimiento/administracion?" + (ok ? "ok=cancelado" : "error=eliminar");
    }

    private LocalDate fecha(String valor) {
        try {
            return LocalDate.parse(valor);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean puedeRegistrar(UsuarioAutenticado usuario) {
        int idRol = idRol(usuario);
        return permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "programar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "programar");
    }

    /** Igual al filtro de seguridad del sistema original para Administracion_mantenimiento.jsp /
     * GestionMantenimientoServlet: solo administrar o gestionar (singular). El Instructor tiene
     * mantenimiento.programar y el permiso suelto mantenimientos.gestionar (plural, nunca revisado
     * por el filtro original) en la base de datos, y sin esta restriccion terminaba pudiendo
     * cambiar el tecnico responsable o cancelar mantenimientos desde "Consultar", cuando su unico
     * permiso real es programar uno nuevo. */
    private boolean puedeAdministrar(UsuarioAutenticado usuario, String accion) {
        int idRol = idRol(usuario);
        if (permisoService.tienePermisoConTodas(idRol, "mantenimiento", "administrar")) return true;
        if ("eliminar".equals(accion)) {
            return permisoService.tienePermisoConTodas(idRol, "mantenimiento", "eliminar")
                    || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "eliminar")
                    || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar");
        }
        return permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar");
    }

    private int idRol(UsuarioAutenticado usuario) {
        return usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
