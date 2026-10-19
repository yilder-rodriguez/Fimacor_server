package com.fimacor.ayuda.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Centro de ayuda: manuales de usuario del sistema (PDF) y enlaces a videos. No son los manuales de las maquinas.
 * Los PDF viven en la base (mediumblob) para no depender de carpetas del servidor. Se usa JdbcTemplate para no
 * cargar el archivo cuando solo se lista.
 */
@Service
public class AyudaService {

    public static final int MAX_BYTES_PDF = 15 * 1024 * 1024;

    private final JdbcTemplate jdbc;

    public AyudaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Resultado(boolean ok, String error) {
        static Resultado exito() {
            return new Resultado(true, null);
        }
        static Resultado fallo(String motivo) {
            return new Resultado(false, motivo);
        }
    }

    public record Archivo(String nombre, byte[] contenido) {
    }

    /** Recursos que puede ver una persona: los de todos y los dirigidos a su rol (el Administrador ve todos). */
    public List<Map<String, Object>> listar(int idRol, boolean verTodos) {
        String sql = "SELECT a.idAyuda AS id, a.titulo, a.descripcion, a.tipo, a.fuente, a.enlace, a.nombre_archivo AS nombreArchivo, "
                + "a.idRol, r.descripcion_rol AS rolNombre, a.creado_en AS creadoEn "
                + "FROM ayuda_recurso a LEFT JOIN rol r ON r.idRol = a.idRol ";
        List<Object> parametros = new ArrayList<>();
        if (!verTodos) {
            sql += "WHERE a.idRol IS NULL OR a.idRol = ? ";
            parametros.add(idRol);
        }
        sql += "ORDER BY a.creado_en DESC, a.idAyuda DESC";
        return jdbc.queryForList(sql, parametros.toArray());
    }

    public Resultado crear(String titulo, String descripcion, String tipo, String fuente, String enlace, Integer idRol,
            String nombreArchivo, byte[] contenido, String creadoPor) {
        String tituloLimpio = limitar(titulo, 120);
        if (tituloLimpio.isEmpty()) return Resultado.fallo("titulo");
        if (!"manual".equals(tipo) && !"video".equals(tipo)) return Resultado.fallo("datos");

        if ("link".equals(fuente)) {
            String url = normalizarUrl(enlace);
            if (url.isEmpty()) return Resultado.fallo("link");
            jdbc.update("INSERT INTO ayuda_recurso (titulo, descripcion, tipo, fuente, enlace, idRol, creado_por, creado_en) "
                    + "VALUES (?, ?, ?, 'link', ?, ?, ?, ?)",
                    tituloLimpio, limitar(descripcion, 300), tipo, url, idRol, limitar(creadoPor, 150), Timestamp.valueOf(LocalDateTime.now()));
            return Resultado.exito();
        }

        // Los videos van siempre como enlace: solo los manuales se suben como PDF.
        if ("video".equals(tipo)) return Resultado.fallo("video");
        if (contenido == null || contenido.length == 0 || nombreArchivo == null || nombreArchivo.isBlank()) return Resultado.fallo("archivo");
        if (contenido.length > MAX_BYTES_PDF) return Resultado.fallo("archivo_grande");
        boolean esPdf = nombreArchivo.toLowerCase().endsWith(".pdf") && contenido.length > 4
                && contenido[0] == '%' && contenido[1] == 'P' && contenido[2] == 'D' && contenido[3] == 'F';
        if (!esPdf) return Resultado.fallo("pdf");
        jdbc.update("INSERT INTO ayuda_recurso (titulo, descripcion, tipo, fuente, archivo, nombre_archivo, idRol, creado_por, creado_en) "
                + "VALUES (?, ?, ?, 'archivo', ?, ?, ?, ?, ?)",
                tituloLimpio, limitar(descripcion, 300), tipo, contenido, limitar(nombreArchivo, 150), idRol,
                limitar(creadoPor, 150), Timestamp.valueOf(LocalDateTime.now()));
        return Resultado.exito();
    }

    public boolean eliminar(int id) {
        return jdbc.update("DELETE FROM ayuda_recurso WHERE idAyuda = ?", id) > 0;
    }

    /** El PDF de un recurso, solo si esa persona puede verlo. */
    public Archivo archivo(int id, int idRol, boolean verTodos) {
        List<Archivo> filas = jdbc.query(
                "SELECT nombre_archivo, archivo FROM ayuda_recurso WHERE idAyuda = ? AND archivo IS NOT NULL"
                        + (verTodos ? "" : " AND (idRol IS NULL OR idRol = ?)"),
                (rs, i) -> new Archivo(rs.getString(1), rs.getBytes(2)),
                verTodos ? new Object[] {id} : new Object[] {id, idRol});
        return filas.isEmpty() ? null : filas.get(0);
    }

    private String normalizarUrl(String url) {
        String limpio = url == null ? "" : url.trim();
        if (limpio.isEmpty() || limpio.length() > 500 || limpio.matches(".*\\s.*")) return "";
        // Solo http o https (nada de javascript: ni otros esquemas).
        if (limpio.matches("(?i)^[a-z][a-z0-9+.-]*:.*") && !limpio.matches("(?i)^https?://.+")) return "";
        if (!limpio.matches("(?i)^https?://.+")) limpio = "https://" + limpio;
        return limpio;
    }

    private String limitar(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
