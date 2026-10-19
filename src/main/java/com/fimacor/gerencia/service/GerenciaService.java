package com.fimacor.gerencia.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Datos del tablero de gerencia: una fila por maquina activa (no dada de baja) con todo lo que el tablero
 * necesita. Los graficos y filtros se calculan en el navegador sobre estas filas. Se usa JdbcTemplate y valores
 * simples (texto, numeros, booleanos) para poder entregarlos tal cual como JSON a la pagina.
 */
@Service
public class GerenciaService {

    private final JdbcTemplate jdbc;

    public GerenciaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> maquinas() {
        String sql = "SELECT f.idMaquinas AS id, f.codigo_sena AS codigo, f.descripcion, "
                + "t.descripcion_maquina AS tipo, ma.descripcion_marca AS marca, mo.descripcion_modelo AS modelo, "
                + "e.nombre_tipo AS estado, a.descripcion_ambiente AS ambiente, f.area, s.nombre_sede AS sede, "
                + "f.fecha_compra AS fechaCompra, f.valor_maquina AS valor, f.tiene_garantia AS garantia, f.fecha_garantia AS fechaGarantia, "
                + "u.nombre AS cNombre, u.apellido AS cApellido, u.documento_identidad AS cDocumento "
                + "FROM ficha_maquina f "
                + "JOIN tipo_maquina t ON t.idTipo_Maquina = f.Tipo_Maquina_idTipo_Maquina "
                + "JOIN estado_de_maquina e ON e.idEstado_de_Maquina = f.Estado_de_Maquina_idEstado_de_Maquina "
                + "JOIN ambientes_aprendizaje a ON a.idAmbientes_de_aprendizaje = f.Ambientes_Aprendizaje_idAmbientes_de_aprendizaje "
                + "JOIN sede s ON s.idSede = f.Sede_idSede "
                + "LEFT JOIN marca ma ON ma.idMarca = f.Marca_idMarca "
                + "LEFT JOIN modelo mo ON mo.idModelo = f.Modelo_idModelo "
                + "LEFT JOIN maquina_cuentadante mc ON mc.Ficha_Maquina_idMaquinas = f.idMaquinas "
                + "LEFT JOIN usuario u ON u.idUsuario = mc.Usuario_idUsuario "
                + "WHERE f.baja = 0 ORDER BY f.codigo_sena";
        List<Map<String, Object>> filas = new ArrayList<>();
        jdbc.query(sql, rs -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.getInt("id"));
            m.put("codigo", texto(rs.getString("codigo")));
            m.put("descripcion", texto(rs.getString("descripcion")));
            m.put("tipo", texto(rs.getString("tipo")));
            m.put("marca", texto(rs.getString("marca")));
            m.put("modelo", texto(rs.getString("modelo")));
            m.put("estado", texto(rs.getString("estado")).toUpperCase());
            m.put("ambiente", texto(rs.getString("ambiente")));
            m.put("area", texto(rs.getString("area")));
            m.put("sede", texto(rs.getString("sede")));
            Date compra = rs.getDate("fechaCompra");
            m.put("fechaCompra", compra == null ? "" : compra.toString());
            m.put("anioCompra", compra == null ? null : (Object) compra.toLocalDate().getYear());
            BigDecimal valor = rs.getBigDecimal("valor");
            m.put("valor", valor == null ? null : (Object) valor.longValue());
            // Garantia: sin garantia, vigente, vencida, o "por revisar" cuando dice que tiene pero no hay fecha de fin.
            boolean garantia = rs.getBoolean("garantia");
            Date fin = rs.getDate("fechaGarantia");
            String estadoGarantia = !garantia ? "Sin garantía"
                    : fin == null ? "Garantía por revisar"
                    : fin.toLocalDate().isBefore(LocalDate.now()) ? "Garantía vencida" : "Garantía vigente";
            m.put("estadoGarantia", estadoGarantia);
            m.put("venceGarantia", garantia && fin != null ? fin.toString() : "");
            String nombre = (texto(rs.getString("cNombre")) + " " + texto(rs.getString("cApellido"))).trim();
            m.put("cuentadante", nombre);
            m.put("cuentadanteDocumento", texto(rs.getString("cDocumento")));
            filas.add(m);
        });
        return filas;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
