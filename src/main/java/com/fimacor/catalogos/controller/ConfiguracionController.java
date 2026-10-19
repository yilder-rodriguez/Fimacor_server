package com.fimacor.catalogos.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fimacor.catalogos.service.ConfiguracionService;

/**
 * Administra los catalogos: Tipo_Maquina, Estado_de_Maquina, Marca, Modelo,
 * Sede, Ambientes_Aprendizaje, Area, Tipo_Manteniento y Tipos_Manuales.
 * Ubicaciones se muestra de solo lectura (bitacora que ya se llena sola desde
 * reubicacion). Todavia falta por conectar: el aviso por WhatsApp de cada
 * cambio, y la notificacion interna en la campanita del admin.
 */
@Controller
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;
    private final PermisoService permisoService;

    public ConfiguracionController(ConfiguracionService configuracionService, PermisoService permisoService) {
        this.configuracionService = configuracionService;
        this.permisoService = permisoService;
    }

    @GetMapping("/configuracion")
    public String ver(@RequestParam(defaultValue = "tipo-maquina") String catalogo,
            @RequestParam(required = false) String estado,
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            Model model) {
        if (!puedeGestionar(usuarioAutenticado)) {
            return "redirect:/menu";
        }
        model.addAttribute("catalogoActual", catalogo);
        model.addAttribute("estado", estado);
        model.addAttribute("tiposMaquina", configuracionService.listarTiposMaquina());
        model.addAttribute("estadosMaquina", configuracionService.listarEstadosMaquina());
        model.addAttribute("marcas", configuracionService.listarMarcas());
        model.addAttribute("marcasOrdenadas", configuracionService.listarMarcasOrdenAlfabetico());
        model.addAttribute("modelos", configuracionService.listarModelos());
        model.addAttribute("sedes", configuracionService.listarSedes());
        model.addAttribute("sedesOrdenadas", configuracionService.listarSedesOrdenAlfabetico());
        model.addAttribute("ambientes", configuracionService.listarAmbientes());
        model.addAttribute("areas", configuracionService.listarAreas());
        model.addAttribute("tiposMantenimiento", configuracionService.listarTiposMantenimiento());
        model.addAttribute("tiposManual", configuracionService.listarTiposManual());
        model.addAttribute("ubicaciones", configuracionService.listarUbicaciones());
        return "configuracion";
    }

    // ---- Tipo de maquina ----

    @PostMapping("/configuracion/tipo-maquina")
    public String crearTipoMaquina(@RequestParam String descripcion_maquina, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.crearTipoMaquina(descripcion_maquina);
        return redirigir("tipo-maquina", ok);
    }

    @PostMapping("/configuracion/tipo-maquina/{id}")
    public String actualizarTipoMaquina(@PathVariable int id, @RequestParam String descripcion_maquina,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.actualizarTipoMaquina(id, descripcion_maquina);
        return redirigir("tipo-maquina", ok);
    }

    @PostMapping("/configuracion/tipo-maquina/{id}/eliminar")
    public String eliminarTipoMaquina(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.eliminarTipoMaquina(id);
        return redirigir("tipo-maquina", ok);
    }

    // ---- Estado de maquina ----

    @PostMapping("/configuracion/estado-maquina")
    public String crearEstadoMaquina(@RequestParam String nombre_tipo, @RequestParam String descripcion_estado,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.crearEstadoMaquina(nombre_tipo, descripcion_estado);
        return redirigir("estado-maquina", ok);
    }

    @PostMapping("/configuracion/estado-maquina/{id}")
    public String actualizarEstadoMaquina(@PathVariable int id, @RequestParam String nombre_tipo,
            @RequestParam String descripcion_estado, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.actualizarEstadoMaquina(id, nombre_tipo, descripcion_estado);
        return redirigir("estado-maquina", ok);
    }

    @PostMapping("/configuracion/estado-maquina/{id}/eliminar")
    public String eliminarEstadoMaquina(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.eliminarEstadoMaquina(id);
        return redirigir("estado-maquina", ok);
    }

    // ---- Marca ----

    @PostMapping("/configuracion/marca")
    public String crearMarca(@RequestParam String descripcion_marca, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.crearMarca(descripcion_marca);
        return redirigir("marca", ok);
    }

    @PostMapping("/configuracion/marca/{id}")
    public String actualizarMarca(@PathVariable int id, @RequestParam String descripcion_marca,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.actualizarMarca(id, descripcion_marca);
        return redirigir("marca", ok);
    }

    @PostMapping("/configuracion/marca/{id}/eliminar")
    public String eliminarMarca(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.eliminarMarca(id);
        return redirigir("marca", ok);
    }

    // ---- Modelo ----

    @PostMapping("/configuracion/modelo")
    public String crearModelo(@RequestParam String descripcion_modelo, @RequestParam("Marca_idMarca") int idMarca,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.crearModelo(descripcion_modelo, idMarca);
        return redirigir("modelo", ok);
    }

    @PostMapping("/configuracion/modelo/{id}")
    public String actualizarModelo(@PathVariable int id, @RequestParam String descripcion_modelo,
            @RequestParam("Marca_idMarca") int idMarca, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.actualizarModelo(id, descripcion_modelo, idMarca);
        return redirigir("modelo", ok);
    }

    @PostMapping("/configuracion/modelo/{id}/eliminar")
    public String eliminarModelo(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.eliminarModelo(id);
        return redirigir("modelo", ok);
    }

    // ---- Sede ----

    @PostMapping("/configuracion/sede")
    public String crearSede(@RequestParam String nombre_sede, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.crearSede(nombre_sede);
        return redirigir("sedes", ok);
    }

    @PostMapping("/configuracion/sede/{id}")
    public String actualizarSede(@PathVariable int id, @RequestParam String nombre_sede,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.actualizarSede(id, nombre_sede);
        return redirigir("sedes", ok);
    }

    @PostMapping("/configuracion/sede/{id}/eliminar")
    public String eliminarSede(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.eliminarSede(id);
        return redirigir("sedes", ok);
    }

    // ---- Ambiente de aprendizaje ----

    @PostMapping("/configuracion/ambiente")
    public String crearAmbiente(@RequestParam String descripcion_ambiente, @RequestParam("Sede_idSede") int idSede,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.crearAmbiente(descripcion_ambiente, idSede);
        return redirigir("ambientes", ok);
    }

    @PostMapping("/configuracion/ambiente/{id}")
    public String actualizarAmbiente(@PathVariable int id, @RequestParam String descripcion_ambiente,
            @RequestParam("Sede_idSede") int idSede, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.actualizarAmbiente(id, descripcion_ambiente, idSede);
        return redirigir("ambientes", ok);
    }

    @PostMapping("/configuracion/ambiente/{id}/eliminar")
    public String eliminarAmbiente(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.eliminarAmbiente(id);
        return redirigir("ambientes", ok);
    }

    // ---- Area ----

    @PostMapping("/configuracion/area")
    public String crearArea(@RequestParam String nombre_area, @RequestParam("Sede_idSede") int idSede,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.crearArea(nombre_area, idSede);
        return redirigir("areas", ok);
    }

    @PostMapping("/configuracion/area/{id}")
    public String actualizarArea(@PathVariable int id, @RequestParam String nombre_area,
            @RequestParam("Sede_idSede") int idSede, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.actualizarArea(id, nombre_area, idSede);
        return redirigir("areas", ok);
    }

    @PostMapping("/configuracion/area/{id}/eliminar")
    public String eliminarArea(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.eliminarArea(id);
        return redirigir("areas", ok);
    }

    // ---- Tipo de mantenimiento ----

    @PostMapping("/configuracion/tipo-mantenimiento")
    public String crearTipoMantenimiento(@RequestParam String descripcion_matenimiento, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.crearTipoMantenimiento(descripcion_matenimiento);
        return redirigir("tipo-mantenimiento", ok);
    }

    @PostMapping("/configuracion/tipo-mantenimiento/{id}")
    public String actualizarTipoMantenimiento(@PathVariable int id, @RequestParam String descripcion_matenimiento,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.actualizarTipoMantenimiento(id, descripcion_matenimiento);
        return redirigir("tipo-mantenimiento", ok);
    }

    @PostMapping("/configuracion/tipo-mantenimiento/{id}/eliminar")
    public String eliminarTipoMantenimiento(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.eliminarTipoMantenimiento(id);
        return redirigir("tipo-mantenimiento", ok);
    }

    // ---- Tipos de manuales ----

    @PostMapping("/configuracion/tipos-manuales")
    public String crearTipoManual(@RequestParam String descripcion_manual, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.crearTipoManual(descripcion_manual);
        return redirigir("tipos-manuales", ok);
    }

    @PostMapping("/configuracion/tipos-manuales/{id}")
    public String actualizarTipoManual(@PathVariable int id, @RequestParam String descripcion_manual,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.actualizarTipoManual(id, descripcion_manual);
        return redirigir("tipos-manuales", ok);
    }

    @PostMapping("/configuracion/tipos-manuales/{id}/eliminar")
    public String eliminarTipoManual(@PathVariable int id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeGestionar(usuario)) return "redirect:/menu";
        boolean ok = configuracionService.eliminarTipoManual(id);
        return redirigir("tipos-manuales", ok);
    }

    // ---- utilidades ----

    private boolean puedeGestionar(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.getUsuario().getRol() == null) return false;
        return permisoService.tienePermisoConTodas(usuario.getUsuario().getRol().getIdRol(), "configuracion", "gestionar");
    }

    private String redirigir(String catalogo, boolean ok) {
        return "redirect:/configuracion?catalogo=" + catalogo + "&estado=" + (ok ? "ok" : "error");
    }
}
