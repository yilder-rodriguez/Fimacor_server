package com.fimacor.importacion.controller;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.comun.seguridad.UsuarioAutenticado;
import com.fimacor.importacion.service.ImportacionDatosService;
import com.fimacor.importacion.service.ImportacionService;
import com.fimacor.importacion.service.PlantillasService;
import com.fimacor.importacion.service.TipoImportacion;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Importacion desde Excel o CSV de usuarios, maquinas, novedades y mantenimientos: se descarga la plantilla, se sube el
 * archivo lleno, se revisa lo que pasaria y, si todo esta bien, se confirma. Solo el Administrador.
 */
@Controller
public class ImportacionController {

    private static final String SESION_ANALISIS = "importacionAnalisis";
    private static final String SESION_ANALISIS_DATOS = "importacionAnalisisDatos";
    private static final String SESION_RESULTADO = "importacionResultado";
    private static final String SESION_RESULTADO_DATOS = "importacionResultadoDatos";
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final MediaType EXCEL = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ImportacionService service;
    private final ImportacionDatosService datos;
    private final PlantillasService plantillas;
    private final PermisoService permisoService;
    private final JdbcTemplate jdbc;

    public ImportacionController(ImportacionService service, ImportacionDatosService datos, PlantillasService plantillas,
            PermisoService permisoService, JdbcTemplate jdbc) {
        this.service = service;
        this.datos = datos;
        this.plantillas = plantillas;
        this.permisoService = permisoService;
        this.jdbc = jdbc;
    }

    @GetMapping("/importacion")
    public String ver(@RequestParam(required = false) String tipo, @AuthenticationPrincipal UsuarioAutenticado usuario, HttpSession sesion, Model model) {
        if (!puede(usuario)) return "redirect:/menu";
        limpiarSesion(sesion);
        return pantalla(model, TipoImportacion.de(tipo), null, null, null, null, null);
    }

    @PostMapping("/importacion/revisar")
    public String revisar(@RequestParam("archivo") MultipartFile archivo, @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Integer sedePorDefecto, @AuthenticationPrincipal UsuarioAutenticado usuario, HttpSession sesion, Model model) {
        if (!puede(usuario)) return "redirect:/menu";
        TipoImportacion t = TipoImportacion.de(tipo);
        sesion.removeAttribute(SESION_ANALISIS);
        sesion.removeAttribute(SESION_ANALISIS_DATOS);
        if (archivo == null || archivo.isEmpty()) return pantalla(model, t, null, null, null, null, "Elige un archivo para revisar.");
        if (archivo.getSize() > MAX_BYTES) return pantalla(model, t, null, null, null, null, "El archivo pesa más de 10 MB. Divídelo en partes más pequeñas.");
        try {
            if (t == TipoImportacion.MAQUINAS) {
                ImportacionService.Analisis analisis = service.analizar(archivo.getOriginalFilename(), archivo.getBytes(), sedePorDefecto);
                sesion.setAttribute(SESION_ANALISIS, analisis);
                return pantalla(model, t, analisis, null, null, null, null);
            }
            ImportacionDatosService.AnalisisDatos analisis = datos.analizar(t, archivo.getOriginalFilename(), archivo.getBytes());
            sesion.setAttribute(SESION_ANALISIS_DATOS, analisis);
            return pantalla(model, t, null, null, analisis, null, null);
        } catch (IOException e) {
            return pantalla(model, t, null, null, null, null, e.getMessage());
        } catch (RuntimeException e) {
            return pantalla(model, t, null, null, null, null, "No se pudo leer el archivo: " + (e.getMessage() == null ? "formato no válido." : e.getMessage()));
        }
    }

    @PostMapping("/importacion/confirmar")
    public String confirmar(@RequestParam String token, @RequestParam(defaultValue = "false") boolean actualizar,
            @AuthenticationPrincipal UsuarioAutenticado usuario, HttpSession sesion, Model model) {
        if (!puede(usuario)) return "redirect:/menu";
        Object guardado = sesion.getAttribute(SESION_ANALISIS);
        Object guardadoDatos = sesion.getAttribute(SESION_ANALISIS_DATOS);

        if (guardadoDatos instanceof ImportacionDatosService.AnalisisDatos analisisDatos && analisisDatos.token.equals(token)) {
            TipoImportacion t = TipoImportacion.de(analisisDatos.tipo);
            try {
                ImportacionDatosService.ResultadoDatos resultado = datos.importar(analisisDatos);
                sesion.removeAttribute(SESION_ANALISIS_DATOS);
                sesion.setAttribute(SESION_RESULTADO_DATOS, resultado);
                return "redirect:/importacion/resultado";
            } catch (RuntimeException e) {
                // Todo se hace en una transaccion: si falla, no queda nada a medias.
                return pantalla(model, t, null, null, analisisDatos, null, "La importación falló y no se guardó nada: " + (e.getMessage() == null ? "error inesperado." : e.getMessage()));
            }
        }
        if (!(guardado instanceof ImportacionService.Analisis analisis) || !analisis.token.equals(token)) {
            return pantalla(model, TipoImportacion.MAQUINAS, null, null, null, null, "La revisión venció o ya se importó. Sube el archivo de nuevo.");
        }
        try {
            ImportacionService.Resultado resultado = service.importar(analisis, actualizar);
            sesion.removeAttribute(SESION_ANALISIS);
            sesion.setAttribute(SESION_RESULTADO, resultado);
            return "redirect:/importacion/resultado";
        } catch (RuntimeException e) {
            return pantalla(model, TipoImportacion.MAQUINAS, analisis, null, null, null, "La importación falló y no se guardó nada: " + (e.getMessage() == null ? "error inesperado." : e.getMessage()));
        }
    }

    @GetMapping("/importacion/resultado")
    public String resultado(@AuthenticationPrincipal UsuarioAutenticado usuario, HttpSession sesion, Model model) {
        if (!puede(usuario)) return "redirect:/menu";
        Object datosRes = sesion.getAttribute(SESION_RESULTADO_DATOS);
        if (datosRes instanceof ImportacionDatosService.ResultadoDatos r) {
            return pantalla(model, TipoImportacion.de(r.tipo), null, null, null, r, null);
        }
        Object r = sesion.getAttribute(SESION_RESULTADO);
        if (!(r instanceof ImportacionService.Resultado resultado)) return "redirect:/importacion";
        return pantalla(model, TipoImportacion.MAQUINAS, null, resultado, null, null, null);
    }

    @GetMapping("/importacion/plantilla")
    public ResponseEntity<byte[]> plantilla(@RequestParam(required = false) String tipo, @AuthenticationPrincipal UsuarioAutenticado usuario) throws IOException {
        if (!puede(usuario)) return ResponseEntity.status(403).build();
        TipoImportacion t = TipoImportacion.de(tipo);
        return descarga(EXCEL, t.archivo, plantillas.generar(t));
    }

    @GetMapping("/importacion/plantillas.zip")
    public ResponseEntity<byte[]> todasLasPlantillas(@AuthenticationPrincipal UsuarioAutenticado usuario) throws IOException {
        if (!puede(usuario)) return ResponseEntity.status(403).build();
        return descarga(MediaType.parseMediaType("application/zip"), "plantillas_fimacor.zip", plantillas.generarTodasEnZip());
    }

    @GetMapping("/importacion/credenciales")
    public ResponseEntity<byte[]> credenciales(@AuthenticationPrincipal UsuarioAutenticado usuario, HttpSession sesion) throws IOException {
        if (!puede(usuario)) return ResponseEntity.status(403).build();
        Object r = sesion.getAttribute(SESION_RESULTADO_DATOS);
        if (!(r instanceof ImportacionDatosService.ResultadoDatos resultado) || resultado.credenciales.isEmpty()) return ResponseEntity.notFound().build();
        return descarga(EXCEL, "contrasenas_iniciales.xlsx", plantillas.credenciales(resultado.credenciales));
    }

    private ResponseEntity<byte[]> descarga(MediaType tipo, String nombre, byte[] cuerpo) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(cuerpo);
    }

    private void limpiarSesion(HttpSession sesion) {
        sesion.removeAttribute(SESION_ANALISIS);
        sesion.removeAttribute(SESION_ANALISIS_DATOS);
        sesion.removeAttribute(SESION_RESULTADO);
        sesion.removeAttribute(SESION_RESULTADO_DATOS);
    }

    private String pantalla(Model model, TipoImportacion tipo, ImportacionService.Analisis analisis, ImportacionService.Resultado resultado,
            ImportacionDatosService.AnalisisDatos analisisDatos, ImportacionDatosService.ResultadoDatos resultadoDatos, String error) {
        List<Map<String, Object>> sedes = jdbc.queryForList("SELECT idSede AS id, nombre_sede AS nombre FROM sede ORDER BY nombre_sede");
        model.addAttribute("sedes", sedes);
        model.addAttribute("tipo", tipo);
        model.addAttribute("tipos", TipoImportacion.values());
        model.addAttribute("analisis", analisis);
        if (analisis != null) {
            // Para no pintar miles de filas: una muestra, los errores y los avisos (con tope).
            model.addAttribute("muestra", analisis.filas.stream().limit(30).toList());
            model.addAttribute("filasConError", analisis.filas.stream().filter(f -> !f.errores.isEmpty()).limit(200).toList());
            model.addAttribute("filasConAviso", analisis.filas.stream().filter(f -> f.errores.isEmpty() && !f.avisos.isEmpty()).limit(100).toList());
            model.addAttribute("importables", analisis.nuevas + analisis.actualizan);
        }
        model.addAttribute("analisisDatos", analisisDatos);
        if (analisisDatos != null) {
            model.addAttribute("muestraDatos", analisisDatos.filas.stream().limit(30).toList());
            model.addAttribute("filasDatosConError", analisisDatos.filas.stream().filter(f -> !f.errores.isEmpty()).limit(200).toList());
            model.addAttribute("filasDatosConAviso", analisisDatos.filas.stream().filter(f -> f.errores.isEmpty() && !f.avisos.isEmpty()).limit(100).toList());
        }
        model.addAttribute("resultado", resultado);
        model.addAttribute("resultadoDatos", resultadoDatos);
        model.addAttribute("error", error);
        return "importacion";
    }

    private boolean puede(UsuarioAutenticado usuario) {
        int idRol = usuario != null && usuario.getUsuario().getRol() != null ? usuario.getUsuario().getRol().getIdRol() : 0;
        return permisoService.esRolTotal(idRol);
    }
}
