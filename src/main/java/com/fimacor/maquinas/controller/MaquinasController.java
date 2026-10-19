package com.fimacor.maquinas.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fimacor.maquinas.service.AsignacionMaquinaService;
import com.fimacor.maquinas.service.BajaMaquinaService;
import com.fimacor.maquinas.service.MaquinasService;

/** Registro de maquinas y su pantalla de administracion. */
@Controller
public class MaquinasController {

    private final MaquinasService service;
    private final PermisoService permisoService;
    private final BajaMaquinaService bajaService;
    private final AsignacionMaquinaService asignacionService;

    public MaquinasController(MaquinasService service, PermisoService permisoService, BajaMaquinaService bajaService,
            AsignacionMaquinaService asignacionService) {
        this.service = service;
        this.permisoService = permisoService;
        this.bajaService = bajaService;
        this.asignacionService = asignacionService;
    }

    @GetMapping("/maquinas/registrar")
    public String verRegistro(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        if (!puedeRegistrar(usuario)) return "redirect:/menu";
        cargarCatalogos(model);
        model.addAttribute("areas", service.listarAreas());
        model.addAttribute("fechaHoy", LocalDate.now().toString());
        // Quien puede asignar maquinas puede dejar al cuentadante responsable desde el mismo registro.
        boolean puedeAsignar = puedeAsignar(usuario);
        model.addAttribute("puedeAsignar", puedeAsignar);
        if (puedeAsignar) model.addAttribute("cuentadantes", asignacionService.listarCuentadantes());
        return "maquina-registro";
    }

    @PostMapping("/maquinas")
    public String registrar(@RequestParam String codigoSena, @RequestParam String descripcion,
            @RequestParam int tipo, @RequestParam int marca, @RequestParam int modelo, @RequestParam int estado,
            @RequestParam int ambiente, @RequestParam int sede, @RequestParam String area,
            @RequestParam(required = false) String fechaCompra, @RequestParam(required = false) String valorMaquina,
            @RequestParam(defaultValue = "0") int tieneGarantia, @RequestParam(required = false) String fechaGarantia,
            @RequestParam(required = false) Integer cuentadante,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeRegistrar(usuario)) return "redirect:/menu";

        MaquinasService.Resultado resultado = service.registrar(codigoSena, descripcion, tipo, marca, modelo, estado,
                ambiente, sede, area, fecha(fechaCompra), valorMaquina, tieneGarantia == 1, fecha(fechaGarantia));
        if (!resultado.ok()) return "redirect:/maquinas/registrar?error=" + resultado.error();

        // Cuentadante opcional: solo si quien registra tiene permiso de asignar. La maquina ya quedo registrada;
        // si la asignacion falla se avisa y se puede asignar despues desde "Asignar maquinas".
        if (cuentadante != null && puedeAsignar(usuario) && resultado.id() != null) {
            AsignacionMaquinaService.Resultado asignacion = asignacionService.asignar(resultado.id(), cuentadante);
            return "redirect:/maquinas?registro=ok&" + (asignacion.ok() ? "asignada=1" : "asignacion=error");
        }
        return "redirect:/maquinas?registro=ok";
    }

    @GetMapping("/maquinas/administracion")
    public String verAdministracion(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        int idRol = idRol(usuario);
        if (!permisoService.tienePermisoConTodas(idRol, "maquinas", "ver")
                && !permisoService.tienePermisoConTodas(idRol, "maquinas", "administrar")
                && !permisoService.esRolTotal(idRol)) {
            return "redirect:/menu";
        }
        model.addAttribute("maquinas", service.listar());
        cargarCatalogos(model);
        model.addAttribute("areas", service.listarAreas());
        model.addAttribute("puedeEditar", puedeEditar(usuario));
        model.addAttribute("puedeDarDeBaja", puedeDarDeBaja(usuario));
        model.addAttribute("puedeRegistrar", puedeRegistrar(usuario));
        model.addAttribute("puedeReportarNovedad", puedeReportarNovedad(usuario));
        model.addAttribute("fechaHoy", LocalDate.now().toString());
        return "maquina-administracion";
    }

    @PostMapping("/maquinas/{id}")
    public String actualizar(@PathVariable int id, @RequestParam String codigoSena, @RequestParam String descripcion,
            @RequestParam int tipo, @RequestParam int marca, @RequestParam int modelo, @RequestParam int estado,
            @RequestParam int ambiente, @RequestParam int sede, @RequestParam String area,
            @RequestParam(required = false) String fechaCompra, @RequestParam(required = false) String valorMaquina,
            @RequestParam(defaultValue = "0") int tieneGarantia, @RequestParam(required = false) String fechaGarantia,
            @RequestParam(required = false) String retorno,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeEditar(usuario)) return "redirect:/menu";

        MaquinasService.Resultado resultado = service.actualizar(id, codigoSena, descripcion, tipo, marca, modelo,
                estado, ambiente, sede, area, fecha(fechaCompra), valorMaquina, tieneGarantia == 1, fecha(fechaGarantia));
        return "redirect:" + destino(retorno) + "?" + (resultado.ok() ? "ok=actualizado" : "error=actualizar");
    }

    /** Las maquinas no se eliminan (trazabilidad): se dan de baja y conservan su historial. */
    @PostMapping("/maquinas/baja")
    public String darDeBaja(@RequestParam(name = "idMaquina") List<Integer> ids,
            @RequestParam(required = false) String motivo, @RequestParam(required = false) String retorno,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeDarDeBaja(usuario)) return "redirect:/menu";
        int hechas = 0;
        String primerError = null;
        for (Integer id : ids) {
            BajaMaquinaService.Resultado r = bajaService.darDeBaja(id, motivo, usuario.getUsuario(), null);
            if (r.ok()) hechas++;
            else if (primerError == null) primerError = r.error();
        }
        String query = hechas > 0 ? "ok=baja" + (primerError != null ? "&error=baja_" + primerError : "") : "error=baja_" + primerError;
        return "redirect:" + destino(retorno) + "?" + query;
    }

    @GetMapping("/maquinas/bajas")
    public String verBajas(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        int idRol = idRol(usuario);
        if (!permisoService.tienePermisoConTodas(idRol, "maquinas", "ver") && !permisoService.esRolTotal(idRol)) {
            return "redirect:/menu";
        }
        model.addAttribute("bajas", bajaService.listarDadasDeBaja());
        model.addAttribute("historial", bajaService.listarHistorial());
        model.addAttribute("puedeReactivar", puedeDarDeBaja(usuario));
        return "maquinas-bajas";
    }

    /** Hoja con el QR de cada maquina en servicio, para que inventarios la imprima. */
    @GetMapping("/maquinas/qr")
    public String verEtiquetasQr(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        int idRol = idRol(usuario);
        if (!permisoService.tienePermisoConTodas(idRol, "maquinas", "ver") && !permisoService.esRolTotal(idRol)) {
            return "redirect:/menu";
        }
        model.addAttribute("maquinas", service.listar());
        return "maquinas-qr";
    }

    @PostMapping("/maquinas/{id}/reactivar")
    public String reactivar(@PathVariable int id, @RequestParam(required = false) String motivo,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (!puedeDarDeBaja(usuario)) return "redirect:/menu";
        BajaMaquinaService.Resultado r = bajaService.reactivar(id, motivo, usuario.getUsuario());
        return "redirect:/maquinas/bajas?" + (r.ok() ? "ok=reactivada" : "error=" + r.error());
    }

    /** Por defecto vuelve al listado principal; la pagina de administracion
     * pasa retorno=administracion. */
    private String destino(String retorno) {
        return "administracion".equals(retorno) ? "/maquinas/administracion" : "/maquinas";
    }

    private void cargarCatalogos(Model model) {
        model.addAttribute("tipos", service.listarTipos());
        model.addAttribute("marcas", service.listarMarcas());
        model.addAttribute("modelos", service.listarModelos());
        model.addAttribute("estados", service.listarEstados());
        model.addAttribute("ambientes", service.listarAmbientes());
        model.addAttribute("sedes", service.listarSedes());
    }

    private LocalDate fecha(String valor) {
        try {
            return (valor == null || valor.isBlank()) ? null : LocalDate.parse(valor);
        } catch (Exception e) {
            return null;
        }
    }

    /** Logistica tiene los permisos sueltos de maquinas.crear/editar/eliminar en la base de
     * datos (heredados del sistema original), pero ese rol nunca deberia poder modificar el
     * registro de maquinas -- solo consultarlo. */
    private boolean puedeRegistrar(UsuarioAutenticado usuario) {
        if (esLogistica(usuario)) return false;
        int idRol = idRol(usuario);
        return permisoService.tienePermisoConTodas(idRol, "maquinas", "crear")
                || permisoService.tienePermisoConTodas(idRol, "maquinas", "registrar")
                || permisoService.tienePermisoConTodas(idRol, "maquina", "crear")
                || permisoService.tienePermisoConTodas(idRol, "maquina", "registrar");
    }

    private boolean puedeEditar(UsuarioAutenticado usuario) {
        if (esLogistica(usuario)) return false;
        int idRol = idRol(usuario);
        return permisoService.tienePermisoConTodas(idRol, "maquinas", "editar")
                || permisoService.tienePermisoConTodas(idRol, "maquina", "editar");
    }

    /** Solo el Administrador da de baja o reactiva maquinas. */
    private boolean puedeDarDeBaja(UsuarioAutenticado usuario) {
        return permisoService.esRolTotal(idRol(usuario));
    }

    /** Mismo criterio que la pantalla "Asignar maquinas": Administrador o permiso maquinas.asignar. */
    private boolean puedeAsignar(UsuarioAutenticado usuario) {
        if (usuario == null) return false;
        String rol = usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getDescripcionRol() : "";
        boolean esAdministrador = java.text.Normalizer.normalize(rol == null ? "" : rol, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().contains("admin");
        return esAdministrador || permisoService.tienePermisoConTodas(idRol(usuario), "maquinas", "asignar");
    }

    private boolean esLogistica(UsuarioAutenticado usuario) {
        String rol = usuario != null && usuario.getUsuario().getRol() != null
                ? usuario.getUsuario().getRol().getDescripcionRol() : "";
        return java.text.Normalizer.normalize(rol, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().contains("logistica");
    }

    private boolean puedeReportarNovedad(UsuarioAutenticado usuario) {
        int idRol = idRol(usuario);
        return permisoService.tienePermisoConTodas(idRol, "novedades", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "novedad", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "reporte", "falla")
                || permisoService.tienePermisoConTodas(idRol, "reportes", "falla");
    }

    private int idRol(UsuarioAutenticado usuario) {
        return usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
    }
}
