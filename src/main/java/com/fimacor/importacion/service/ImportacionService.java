package com.fimacor.importacion.service;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Importacion del inventario de maquinas desde una hoja de calculo. Tiene dos tiempos: analizar (lee el archivo, lo
 * valida y dice que pasaria, sin escribir nada) e importar (escribe todo en una sola transaccion: si algo falla, no
 * queda nada a medias). Crea los catalogos que falten (tipos, marcas, modelos, sedes, areas y ambientes), crea o
 * actualiza las maquinas por codigo SENA y asigna el cuentadante cuando lo encuentra.
 */
@Service
public class ImportacionService {

    private static final DateTimeFormatter[] FORMATOS_FECHA = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("yyyy/M/d"), DateTimeFormatter.ofPattern("d.M.yyyy")
    };
    private static final Map<String, String> ALIAS = new HashMap<>();
    private static final String MARCA_VACIA = "SIN MARCA";
    private static final String MODELO_VACIO = "SIN MODELO";

    private static void alias(String campo, String... nombres) {
        for (String n : nombres) ALIAS.put(n, campo);
    }

    static {
        alias("codigo", "codigo sena", "codigo", "cod sena", "codigo de la maquina", "placa", "placa sena", "codigo inventario", "no inventario", "numero de inventario");
        alias("descripcion", "descripcion", "descripcion de la maquina", "nombre", "nombre de la maquina", "maquina");
        alias("tipo", "tipo", "tipo maquina", "tipo de maquina", "tipo de equipo", "clase de maquina");
        alias("marca", "marca");
        alias("modelo", "modelo", "referencia");
        alias("estado", "estado", "estado operativo", "estado de la maquina", "estado de maquina");
        alias("sede", "sede");
        alias("area", "area");
        alias("ambienteAula", "ambiente de aprendizaje", "ambiente aprendizaje", "ubicacion fisica", "ubicacion", "aula");
        alias("ambienteGen", "ambiente");
        alias("fechaCompra", "fecha de compra", "fecha compra", "fecha de adquisicion", "fecha adquisicion", "fecha ingreso");
        alias("valor", "valor", "valor maquina", "valor de la maquina", "valor maquina cop", "valor de compra", "costo");
        alias("garantia", "garantia", "tiene garantia");
        alias("fechaGarantia", "fecha fin garantia", "fecha fin de garantia", "fecha de garantia", "fecha vencimiento garantia", "vencimiento garantia", "fin de garantia");
        alias("cuentadanteDocumento", "cuentadante documento", "documento cuentadante", "documento del cuentadante", "cedula cuentadante",
                "cedula del cuentadante", "documento responsable", "cc cuentadante");
        alias("cuentadanteNombre", "cuentadante nombre", "cuentadante", "nombre cuentadante", "nombre del cuentadante", "responsable");
    }

    private static final String[][] ETIQUETAS = {
        {"codigo", "Código SENA"}, {"descripcion", "Descripción"}, {"tipo", "Tipo de máquina"}, {"marca", "Marca"}, {"modelo", "Modelo"},
        {"estado", "Estado"}, {"sede", "Sede"}, {"area", "Área"}, {"ambiente", "Ambiente de aprendizaje"}, {"fechaCompra", "Fecha de compra"},
        {"valor", "Valor"}, {"garantia", "Garantía"}, {"fechaGarantia", "Fecha fin de garantía"},
        {"cuentadanteDocumento", "Cuentadante (documento)"}, {"cuentadanteNombre", "Cuentadante (nombre)"}
    };

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;

    public ImportacionService(JdbcTemplate jdbc, PlatformTransactionManager gestor) {
        this.jdbc = jdbc;
        this.transaccion = new TransactionTemplate(gestor);
    }

    // =====================================================================================================
    // Modelos para la pantalla (campos publicos: Thymeleaf los lee directo)
    // =====================================================================================================

    public static class Fila implements Serializable {
        public int fila;
        public String codigo = "", descripcion = "", tipo = "", marca = "", modelo = "", estado = "", sede = "", area = "", ambiente = "";
        public String cuentadanteDocumento = "", cuentadanteNombre = "";
        public LocalDate fechaCompra, fechaGarantia;
        public BigDecimal valor;
        public boolean tieneGarantia, hayDatoGarantia;
        public Integer idCuentadante;
        public String accion = "NUEVA"; // NUEVA, ACTUALIZA, OMITE, ERROR
        public List<String> avisos = new ArrayList<>();
        public List<String> errores = new ArrayList<>();
    }

    public static class Analisis implements Serializable {
        public String token = java.util.UUID.randomUUID().toString();
        public String archivo = "";
        public int totalFilas, nuevas, actualizan, omitidas, conError, sinCuentadante, conAvisos;
        public List<Fila> filas = new ArrayList<>();
        public List<String> columnasDetectadas = new ArrayList<>();
        public List<String> columnasIgnoradas = new ArrayList<>();
        public Map<String, List<String>> catalogosNuevos = new LinkedHashMap<>();
        public int totalCatalogosNuevos;
    }

    public static class Resultado implements Serializable {
        public int creadas, actualizadas, omitidas, asignadas, catalogosCreados;
        public Map<String, Integer> catalogos = new LinkedHashMap<>();
        public List<String> avisos = new ArrayList<>();
    }

    // =====================================================================================================
    // Analisis (solo lectura)
    // =====================================================================================================

    public Analisis analizar(String nombreArchivo, byte[] datos, Integer idSedePorDefecto) throws IOException {
        List<List<String>> hoja = LectorHoja.leer(nombreArchivo, datos);
        int filaTitulos = buscarTitulos(hoja);
        if (filaTitulos < 0) {
            throw new IOException("No encontré los títulos de las columnas. El archivo debe tener al menos la columna \"Código SENA\" y otras como Tipo, Marca, Estado o Ambiente. Descarga la plantilla para ver el formato.");
        }

        List<String> titulos = hoja.get(filaTitulos);
        Map<String, Integer> col = new HashMap<>();
        Analisis a = new Analisis();
        a.archivo = nombreArchivo;
        for (int i = 0; i < titulos.size(); i++) {
            String t = titulos.get(i);
            if (t.isBlank()) continue;
            String campo = ALIAS.get(normal(t));
            if (campo == null) { a.columnasIgnoradas.add(t); continue; }
            if (!col.containsKey(campo)) col.put(campo, i);
        }
        // "Ambiente" suelto: si el archivo ya trae "Ubicación física"/"Ambiente de aprendizaje", "Ambiente" es el area (como en el inventario del complejo).
        if (col.containsKey("ambienteGen")) {
            if (col.containsKey("ambienteAula") && !col.containsKey("area")) col.put("area", col.remove("ambienteGen"));
            else if (!col.containsKey("ambienteAula")) col.put("ambienteAula", col.remove("ambienteGen"));
            else a.columnasIgnoradas.add(titulos.get(col.remove("ambienteGen")));
        }
        if (col.containsKey("ambienteAula")) col.put("ambiente", col.remove("ambienteAula"));
        for (String[] e : ETIQUETAS) {
            if (col.containsKey(e[0])) a.columnasDetectadas.add(e[1] + "  ←  columna \"" + titulos.get(col.get(e[0])) + "\"");
        }

        // datos de la base necesarios para validar
        Map<String, Object[]> existentes = new HashMap<>();
        jdbc.query("SELECT idMaquinas, UPPER(codigo_sena), baja FROM ficha_maquina", rs -> {
            existentes.put(rs.getString(2).trim(), new Object[] {rs.getInt(1), rs.getBoolean(3)});
        });
        Map<String, Integer> estados = estados();
        Cuentadantes cuentadantes = cuentadantes();
        String sedeDefecto = idSedePorDefecto == null ? "" : texto(jdbc.queryForList("SELECT nombre_sede FROM sede WHERE idSede = ?", String.class, idSedePorDefecto).stream().findFirst().orElse(""));

        Map<String, Integer> codigosVistos = new HashMap<>();
        LocalDate hoy = LocalDate.now();
        for (int r = filaTitulos + 1; r < hoja.size(); r++) {
            List<String> celdas = hoja.get(r);
            if (celdas.stream().allMatch(String::isBlank)) continue;
            Fila f = new Fila();
            f.fila = r + 1;
            f.codigo = limpiar(celda(celdas, col, "codigo"));
            f.descripcion = limpiar(celda(celdas, col, "descripcion"));
            f.tipo = limpiar(celda(celdas, col, "tipo"));
            f.marca = limpiar(celda(celdas, col, "marca"));
            f.modelo = limpiar(celda(celdas, col, "modelo"));
            f.sede = limpiar(celda(celdas, col, "sede"));
            f.area = limpiar(celda(celdas, col, "area"));
            f.ambiente = limpiar(celda(celdas, col, "ambiente"));
            f.cuentadanteDocumento = limpiar(celda(celdas, col, "cuentadanteDocumento"));
            f.cuentadanteNombre = limpiar(celda(celdas, col, "cuentadanteNombre"));
            a.totalFilas++;

            // codigo
            f.codigo = f.codigo.toUpperCase();
            if (f.codigo.isEmpty()) f.errores.add("Falta el código SENA.");
            else if (f.codigo.length() > 45) f.errores.add("El código SENA tiene más de 45 caracteres.");
            else if (codigosVistos.containsKey(f.codigo)) f.errores.add("El código " + f.codigo + " ya aparece en la fila " + codigosVistos.get(f.codigo) + " del archivo.");
            else codigosVistos.put(f.codigo, f.fila);

            // estado
            String estadoTexto = limpiar(celda(celdas, col, "estado"));
            f.estado = mapearEstado(estadoTexto);
            if (f.estado == null) f.errores.add(estadoTexto.isEmpty() ? "Falta el estado (Funcional, En mantenimiento o Fuera de servicio)."
                    : "Estado no reconocido: \"" + estadoTexto + "\". Usa Funcional, En mantenimiento o Fuera de servicio.");
            else if (!estados.containsKey(f.estado)) f.errores.add("El estado " + f.estado + " no existe en el sistema.");

            // obligatorios
            if (f.tipo.isEmpty()) f.errores.add("Falta el tipo de máquina.");
            if (f.ambiente.isEmpty()) f.errores.add("Falta el ambiente de aprendizaje (o ubicación física).");
            if (f.sede.isEmpty()) {
                if (sedeDefecto.isEmpty()) f.errores.add("Falta la sede (y no elegiste una sede por defecto).");
                else { f.sede = sedeDefecto; f.avisos.add("Sin sede en el archivo: se usó " + sedeDefecto + "."); }
            }

            // descripcion y longitudes
            if (f.descripcion.isEmpty()) {
                f.descripcion = (f.tipo + " " + f.marca + " " + f.modelo).trim().replaceAll("\\s+", " ");
                if (!f.descripcion.isEmpty()) f.avisos.add("Sin descripción: se usó \"" + recortar(f.descripcion, 255) + "\".");
                else f.errores.add("Falta la descripción.");
            }
            f.descripcion = avisarRecorte(f, "La descripción", f.descripcion, 255);
            f.tipo = avisarRecorte(f, "El tipo", f.tipo, 45);
            f.marca = avisarRecorte(f, "La marca", f.marca, 45);
            f.modelo = avisarRecorte(f, "El modelo", f.modelo, 45);
            f.ambiente = avisarRecorte(f, "El ambiente", f.ambiente, 45);
            f.area = avisarRecorte(f, "El área", f.area, 150);
            f.sede = avisarRecorte(f, "La sede", f.sede, 150);
            if (f.marca.isEmpty()) f.avisos.add("Sin marca: se guardará como " + MARCA_VACIA + ".");
            if (f.modelo.isEmpty()) f.avisos.add("Sin modelo: se guardará como " + MODELO_VACIO + ".");

            // fechas, valor y garantia
            f.fechaCompra = fechaOAviso(f, "fecha de compra", celda(celdas, col, "fechaCompra"), hoy);
            f.fechaGarantia = fechaOAviso(f, "fecha fin de garantía", celda(celdas, col, "fechaGarantia"), null);
            String valorTexto = celda(celdas, col, "valor");
            if (!valorTexto.isBlank()) {
                f.valor = parsearValor(valorTexto);
                if (f.valor == null) f.avisos.add("El valor \"" + valorTexto + "\" no es un número válido: se ignoró.");
                else if (f.valor.compareTo(new BigDecimal("9999999999")) > 0 || f.valor.signum() < 0) { f.avisos.add("El valor está fuera de rango: se ignoró."); f.valor = null; }
            }
            String garantiaTexto = celda(celdas, col, "garantia");
            if (col.containsKey("garantia") || col.containsKey("fechaGarantia")) f.hayDatoGarantia = true;
            if (f.fechaGarantia != null) f.tieneGarantia = true;
            else if (!garantiaTexto.isBlank()) {
                LocalDate fg = parsearFecha(garantiaTexto);
                if (fg != null) { f.fechaGarantia = fg; f.tieneGarantia = true; }
                else f.tieneGarantia = esAfirmativo(garantiaTexto);
            }

            // cuentadante
            if (!f.cuentadanteDocumento.isEmpty() || !f.cuentadanteNombre.isEmpty()) {
                f.idCuentadante = cuentadantes.buscar(f.cuentadanteDocumento, f.cuentadanteNombre);
                if (f.idCuentadante == null) {
                    a.sinCuentadante++;
                    f.avisos.add("No encontré al cuentadante (" + (f.cuentadanteNombre.isEmpty() ? f.cuentadanteDocumento : f.cuentadanteNombre)
                            + "): la máquina se importa sin asignar.");
                }
            }

            // accion
            if (!f.errores.isEmpty()) { f.accion = "ERROR"; a.conError++; }
            else {
                Object[] ya = existentes.get(f.codigo);
                if (ya == null) { f.accion = "NUEVA"; a.nuevas++; }
                else if ((Boolean) ya[1]) { f.accion = "OMITE"; a.omitidas++; f.avisos.add("Está dada de baja: no se modifica."); }
                else { f.accion = "ACTUALIZA"; a.actualizan++; }
            }
            if (!f.avisos.isEmpty()) a.conAvisos++;
            a.filas.add(f);
        }
        if (a.totalFilas == 0) throw new IOException("El archivo no tiene filas de datos debajo de los títulos.");
        if (a.totalFilas > LectorHoja.MAX_FILAS) throw new IOException("El archivo tiene " + a.totalFilas + " filas; el máximo por importación es " + LectorHoja.MAX_FILAS + ".");
        calcularCatalogosNuevos(a);
        return a;
    }

    private void calcularCatalogosNuevos(Analisis a) {
        Catalogos c = cargarCatalogos();
        Map<String, Map<String, String>> nuevos = new LinkedHashMap<>();
        for (String k : new String[] {"Tipos de máquina", "Marcas", "Modelos", "Sedes", "Áreas", "Ambientes"}) nuevos.put(k, new LinkedHashMap<>());
        for (Fila f : a.filas) {
            if (!f.errores.isEmpty() || f.accion.equals("OMITE")) continue;
            String marca = f.marca.isEmpty() ? MARCA_VACIA : f.marca;
            String modelo = f.modelo.isEmpty() ? MODELO_VACIO : f.modelo;
            if (!c.tipos.containsKey(clave(f.tipo))) nuevos.get("Tipos de máquina").putIfAbsent(clave(f.tipo), f.tipo);
            Integer idMarca = c.marcas.get(clave(marca));
            if (idMarca == null) nuevos.get("Marcas").putIfAbsent(clave(marca), marca);
            if (idMarca == null || !c.modelos.containsKey(idMarca + "|" + clave(modelo))) nuevos.get("Modelos").putIfAbsent(clave(marca) + "|" + clave(modelo), marca + " " + modelo);
            Integer idSede = c.sedes.get(clave(f.sede));
            if (idSede == null) nuevos.get("Sedes").putIfAbsent(clave(f.sede), f.sede);
            if (!f.area.isEmpty() && (idSede == null || !c.areas.containsKey(idSede + "|" + clave(f.area)))) nuevos.get("Áreas").putIfAbsent(clave(f.sede) + "|" + clave(f.area), f.area);
            if (idSede == null || !c.ambientes.containsKey(idSede + "|" + clave(f.ambiente))) nuevos.get("Ambientes").putIfAbsent(clave(f.sede) + "|" + clave(f.ambiente), f.ambiente);
        }
        for (Map.Entry<String, Map<String, String>> e : nuevos.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            a.catalogosNuevos.put(e.getKey(), new ArrayList<>(new TreeSet<>(e.getValue().values())));
            a.totalCatalogosNuevos += e.getValue().size();
        }
    }

    // =====================================================================================================
    // Importacion (escribe, todo o nada)
    // =====================================================================================================

    public Resultado importar(Analisis a, boolean actualizarExistentes) {
        return transaccion.execute(estado -> {
            Resultado res = new Resultado();
            Catalogos c = cargarCatalogos();
            Map<String, Integer> estados = estados();
            Map<String, Object[]> existentes = new HashMap<>();
            jdbc.query("SELECT idMaquinas, UPPER(codigo_sena), baja FROM ficha_maquina", rs -> {
                existentes.put(rs.getString(2).trim(), new Object[] {rs.getInt(1), rs.getBoolean(3)});
            });
            Map<String, Integer> contadores = new LinkedHashMap<>();
            int sinMostrar = 0;
            for (Fila f : a.filas) {
                if (!f.errores.isEmpty()) continue;
                Object[] ya = existentes.get(f.codigo);
                if (ya != null && ((Boolean) ya[1] || !actualizarExistentes)) { res.omitidas++; continue; }

                int idTipo = obtenerTipo(c, f.tipo, contadores);
                int idMarca = obtenerMarca(c, f.marca.isEmpty() ? MARCA_VACIA : f.marca, contadores);
                int idModelo = obtenerModelo(c, idMarca, f.modelo.isEmpty() ? MODELO_VACIO : f.modelo, contadores);
                int idSede = obtenerSede(c, f.sede, contadores);
                int idAmbiente = obtenerAmbiente(c, idSede, f.ambiente, contadores);
                if (!f.area.isEmpty()) obtenerArea(c, idSede, f.area, contadores);
                int idEstado = estados.get(f.estado);
                LocalDate compra = f.fechaCompra;
                LocalDate garantia = f.fechaGarantia;

                int idMaquina;
                if (ya == null) {
                    idMaquina = insertarConId("INSERT INTO ficha_maquina (codigo_sena, descripcion, Modelo_idModelo, Estado_de_Maquina_idEstado_de_Maquina, "
                            + "Tipo_Maquina_idTipo_Maquina, Ambientes_Aprendizaje_idAmbientes_de_aprendizaje, fecha_ubicacion_ambiente, Sede_idSede, Area, "
                            + "fecha_compra, valor_maquina, tiene_garantia, fecha_garantia, Marca_idMarca) VALUES (?, ?, ?, ?, ?, ?, CURDATE(), ?, ?, ?, ?, ?, ?, ?)",
                            f.codigo, f.descripcion, idModelo, idEstado, idTipo, idAmbiente, idSede, f.area.isEmpty() ? null : f.area,
                            compra, f.valor, f.tieneGarantia, garantia, idMarca);
                    res.creadas++;
                } else {
                    idMaquina = (Integer) ya[0];
                    // Solo se sobrescribe lo que el archivo trae; lo vacio no borra lo que ya estaba.
                    jdbc.update("UPDATE ficha_maquina SET descripcion = ?, Modelo_idModelo = ?, Marca_idMarca = ?, Estado_de_Maquina_idEstado_de_Maquina = ?, "
                            + "Tipo_Maquina_idTipo_Maquina = ?, fecha_ubicacion_ambiente = IF(Ambientes_Aprendizaje_idAmbientes_de_aprendizaje <> ?, CURDATE(), fecha_ubicacion_ambiente), "
                            + "Ambientes_Aprendizaje_idAmbientes_de_aprendizaje = ?, Sede_idSede = ?, Area = COALESCE(?, Area), "
                            + "fecha_compra = COALESCE(?, fecha_compra), valor_maquina = COALESCE(?, valor_maquina), "
                            + "tiene_garantia = IF(?, ?, tiene_garantia), fecha_garantia = IF(?, ?, fecha_garantia) WHERE idMaquinas = ?",
                            f.descripcion, idModelo, idMarca, idEstado, idTipo, idAmbiente, idAmbiente, idSede, f.area.isEmpty() ? null : f.area,
                            compra, f.valor, f.hayDatoGarantia, f.tieneGarantia, f.hayDatoGarantia, garantia, idMaquina);
                    res.actualizadas++;
                }
                if (f.idCuentadante != null) {
                    jdbc.update("INSERT INTO maquina_cuentadante (Ficha_Maquina_idMaquinas, Usuario_idUsuario, fecha_asignacion) VALUES (?, ?, NOW()) "
                            + "ON DUPLICATE KEY UPDATE Usuario_idUsuario = VALUES(Usuario_idUsuario), fecha_asignacion = NOW()", idMaquina, f.idCuentadante);
                    res.asignadas++;
                }
                for (String aviso : f.avisos) {
                    if (!aviso.startsWith("No encontré al cuentadante")) continue;
                    if (res.avisos.size() < 40) res.avisos.add("Fila " + f.fila + " (" + f.codigo + "): " + aviso);
                    else sinMostrar++;
                }
            }
            if (sinMostrar > 0) res.avisos.add("…y " + sinMostrar + " máquinas más con cuentadante que no se encontró.");
            res.catalogos = contadores;
            res.catalogosCreados = contadores.values().stream().mapToInt(Integer::intValue).sum();
            return res;
        });
    }

    // ---------------- catalogos ----------------

    private static class Catalogos {
        Map<String, Integer> tipos = new HashMap<>(), marcas = new HashMap<>(), modelos = new HashMap<>(), sedes = new HashMap<>(),
                areas = new HashMap<>(), ambientes = new HashMap<>();
    }

    private Catalogos cargarCatalogos() {
        Catalogos c = new Catalogos();
        jdbc.query("SELECT idTipo_Maquina, descripcion_maquina FROM tipo_maquina", rs -> { c.tipos.putIfAbsent(clave(rs.getString(2)), rs.getInt(1)); });
        jdbc.query("SELECT idMarca, descripcion_marca FROM marca", rs -> { c.marcas.putIfAbsent(clave(rs.getString(2)), rs.getInt(1)); });
        jdbc.query("SELECT idModelo, descripcion_modelo, Marca_idMarca FROM modelo", rs -> { c.modelos.putIfAbsent(rs.getInt(3) + "|" + clave(rs.getString(2)), rs.getInt(1)); });
        jdbc.query("SELECT idSede, nombre_sede FROM sede", rs -> { c.sedes.putIfAbsent(clave(rs.getString(2)), rs.getInt(1)); });
        jdbc.query("SELECT idArea, nombre_area, Sede_idSede FROM area", rs -> { c.areas.putIfAbsent(rs.getInt(3) + "|" + clave(rs.getString(2)), rs.getInt(1)); });
        jdbc.query("SELECT idAmbientes_de_aprendizaje, descripcion_ambiente, Sede_idSede FROM ambientes_aprendizaje",
                rs -> { c.ambientes.putIfAbsent(rs.getInt(3) + "|" + clave(rs.getString(2)), rs.getInt(1)); });
        return c;
    }

    private int obtenerTipo(Catalogos c, String nombre, Map<String, Integer> cont) {
        String k = clave(nombre);
        Integer id = c.tipos.get(k);
        if (id != null) return id;
        id = siguienteId("tipo_maquina", "idTipo_Maquina");
        jdbc.update("INSERT INTO tipo_maquina (idTipo_Maquina, descripcion_maquina) VALUES (?, ?)", id, nombre);
        c.tipos.put(k, id); contar(cont, "Tipos de máquina");
        return id;
    }

    private int obtenerMarca(Catalogos c, String nombre, Map<String, Integer> cont) {
        String k = clave(nombre);
        Integer id = c.marcas.get(k);
        if (id != null) return id;
        id = insertarConId("INSERT INTO marca (descripcion_marca) VALUES (?)", nombre);
        c.marcas.put(k, id); contar(cont, "Marcas");
        return id;
    }

    private int obtenerModelo(Catalogos c, int idMarca, String nombre, Map<String, Integer> cont) {
        String k = idMarca + "|" + clave(nombre);
        Integer id = c.modelos.get(k);
        if (id != null) return id;
        id = insertarConId("INSERT INTO modelo (descripcion_modelo, Marca_idMarca) VALUES (?, ?)", nombre, idMarca);
        c.modelos.put(k, id); contar(cont, "Modelos");
        return id;
    }

    private int obtenerSede(Catalogos c, String nombre, Map<String, Integer> cont) {
        String k = clave(nombre);
        Integer id = c.sedes.get(k);
        if (id != null) return id;
        id = siguienteId("sede", "idSede");
        jdbc.update("INSERT INTO sede (idSede, nombre_sede) VALUES (?, ?)", id, nombre);
        c.sedes.put(k, id); contar(cont, "Sedes");
        return id;
    }

    private int obtenerArea(Catalogos c, int idSede, String nombre, Map<String, Integer> cont) {
        String k = idSede + "|" + clave(nombre);
        Integer id = c.areas.get(k);
        if (id != null) return id;
        id = siguienteId("area", "idArea");
        jdbc.update("INSERT INTO area (idArea, nombre_area, Sede_idSede) VALUES (?, ?, ?)", id, nombre, idSede);
        c.areas.put(k, id); contar(cont, "Áreas");
        return id;
    }

    private int obtenerAmbiente(Catalogos c, int idSede, String nombre, Map<String, Integer> cont) {
        String k = idSede + "|" + clave(nombre);
        Integer id = c.ambientes.get(k);
        if (id != null) return id;
        id = siguienteId("ambientes_aprendizaje", "idAmbientes_de_aprendizaje");
        jdbc.update("INSERT INTO ambientes_aprendizaje (idAmbientes_de_aprendizaje, descripcion_ambiente, Sede_idSede) VALUES (?, ?, ?)", id, nombre, idSede);
        c.ambientes.put(k, id); contar(cont, "Ambientes");
        return id;
    }

    private static void contar(Map<String, Integer> cont, String clave) {
        cont.merge(clave, 1, Integer::sum);
    }

    private int siguienteId(String tabla, String columna) {
        Integer n = jdbc.queryForObject("SELECT COALESCE(MAX(" + columna + "), 0) + 1 FROM " + tabla, Integer.class);
        return n == null ? 1 : n;
    }

    private int insertarConId(String sql, Object... parametros) {
        KeyHolder llaves = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < parametros.length; i++) ps.setObject(i + 1, parametros[i]);
            return ps;
        }, llaves);
        Number n = llaves.getKey();
        if (n == null) throw new IllegalStateException("No se obtuvo el id del registro creado.");
        return n.intValue();
    }

    private Map<String, Integer> estados() {
        Map<String, Integer> m = new HashMap<>();
        jdbc.query("SELECT idEstado_de_Maquina, UPPER(nombre_tipo) FROM estado_de_maquina", rs -> { m.putIfAbsent(rs.getString(2).trim(), rs.getInt(1)); });
        return m;
    }

    // ---------------- cuentadantes ----------------

    private static class Cuentadantes {
        Map<String, Integer> porDocumento = new HashMap<>();
        Map<Integer, List<String>> palabrasPorId = new LinkedHashMap<>();

        Integer buscar(String documento, String nombre) {
            String doc = documento == null ? "" : documento.replaceAll("\\D", "");
            if (!doc.isEmpty() && porDocumento.containsKey(doc)) return porDocumento.get(doc);
            List<String> buscadas = palabras(nombre);
            if (buscadas.isEmpty()) return null;
            Integer unico = null;
            for (Map.Entry<Integer, List<String>> e : palabrasPorId.entrySet()) {
                List<String> p = e.getValue();
                boolean coincide = p.containsAll(buscadas) || buscadas.containsAll(p) && p.size() >= 2;
                if (coincide && Math.min(p.size(), buscadas.size()) >= 2) {
                    if (unico != null) return null; // ambiguo: mejor no asignar a la persona equivocada
                    unico = e.getKey();
                }
            }
            return unico;
        }
    }

    private Cuentadantes cuentadantes() {
        Cuentadantes c = new Cuentadantes();
        jdbc.query("SELECT u.idUsuario, u.nombre, u.apellido, u.documento_identidad FROM usuario u JOIN rol r ON r.idRol = u.Rol_idRol "
                + "WHERE u.activo = 1 AND r.descripcion_rol = 'Cuentadante'", rs -> {
            int id = rs.getInt(1);
            String doc = texto(rs.getString(4)).replaceAll("\\D", "");
            if (!doc.isEmpty()) c.porDocumento.putIfAbsent(doc, id);
            c.palabrasPorId.put(id, palabras(texto(rs.getString(2)) + " " + texto(rs.getString(3))));
        });
        return c;
    }

    private static List<String> palabras(String texto) {
        String n = normal(texto);
        if (n.isEmpty()) return List.of();
        List<String> lista = new ArrayList<>(Arrays.asList(n.split(" ")));
        lista.sort(String::compareTo);
        return lista;
    }

    // =====================================================================================================
    // Lectura de valores
    // =====================================================================================================

    private static int buscarTitulos(List<List<String>> hoja) {
        int mejor = -1, mejorCuenta = 0;
        for (int i = 0; i < Math.min(15, hoja.size()); i++) {
            int cuenta = 0;
            boolean tieneCodigo = false;
            for (String t : hoja.get(i)) {
                String campo = t.isBlank() ? null : ALIAS.get(normal(t));
                if (campo != null) { cuenta++; if (campo.equals("codigo")) tieneCodigo = true; }
            }
            if (tieneCodigo && cuenta >= 2 && cuenta > mejorCuenta) { mejor = i; mejorCuenta = cuenta; }
        }
        return mejor;
    }

    private static String celda(List<String> fila, Map<String, Integer> col, String campo) {
        Integer i = col.get(campo);
        return i == null || i >= fila.size() ? "" : fila.get(i);
    }

    /** Clave para comparar nombres de catalogo: sin tildes, mayusculas ni espacios ("COMPLEJOSUR" = "Complejo Sur"). */
    static String clave(String s) {
        return normal(s).replace(" ", "");
    }

    static String normal(String s) {
        if (s == null) return "";
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9ñ]+", " ").trim();
    }

    private static String texto(String s) {
        return s == null ? "" : s.trim();
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.replace(' ', ' ').trim().replaceAll("\\s+", " ");
    }

    private static String recortar(String s, int max) {
        return s.length() > max ? s.substring(0, max).trim() : s;
    }

    private static String avisarRecorte(Fila f, String que, String valor, int max) {
        if (valor.length() <= max) return valor;
        f.avisos.add(que + " tenía " + valor.length() + " caracteres y se recortó a " + max + ".");
        return recortar(valor, max);
    }

    private static String mapearEstado(String texto) {
        String n = normal(texto);
        if (n.isEmpty()) return null;
        if (n.contains("no funcion") || n.contains("fuera") || n.contains("dan") || n.contains("inserv") || n.contains("averi") || n.contains("baja")) return "FUERA DE SERVICIO";
        if (n.contains("mantenim") || n.contains("reparac")) return "MANTENIMIENTO";
        if (n.contains("funcion") || n.contains("operativ") || n.contains("bueno") || n.contains("buen estado") || n.equals("ok")) return "FUNCIONAL";
        return null;
    }

    private static boolean esAfirmativo(String t) {
        String n = normal(t);
        return n.equals("si") || n.equals("s") || n.equals("x") || n.equals("1") || n.equals("true") || n.equals("verdadero") || n.contains("vigente") || n.equals("con garantia");
    }

    private static LocalDate fechaOAviso(Fila f, String nombre, String texto, LocalDate maxima) {
        if (texto == null || texto.isBlank()) return null;
        LocalDate d = parsearFecha(texto);
        if (d == null) { f.avisos.add("La " + nombre + " \"" + texto + "\" no es una fecha válida: se ignoró."); return null; }
        if (d.getYear() < 1950 || (maxima != null && d.isAfter(maxima)) || d.getYear() > 2100) {
            f.avisos.add("La " + nombre + " (" + d + ") no es razonable: se ignoró.");
            return null;
        }
        return d;
    }

    static LocalDate parsearFecha(String texto) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) return null;
        if (t.length() > 10 && t.matches("\\d{4}-\\d{2}-\\d{2}.*")) t = t.substring(0, 10);
        for (DateTimeFormatter f : FORMATOS_FECHA) {
            try { return LocalDate.parse(t, f); } catch (DateTimeParseException e) { /* siguiente formato */ }
        }
        if (t.matches("\\d{5}(\\.\\d+)?")) { // numero de serie de Excel guardado como texto
            long serie = (long) Double.parseDouble(t);
            if (serie > 20000 && serie < 80000) return LocalDate.of(1899, 12, 30).plusDays(serie);
        }
        return null;
    }

    static BigDecimal parsearValor(String texto) {
        String t = texto.replaceAll("[^0-9.,-]", "");
        if (t.isEmpty() || t.equals("-")) return null;
        int punto = t.lastIndexOf('.'), coma = t.lastIndexOf(',');
        try {
            if (punto >= 0 && coma >= 0) { // el ultimo separador es el decimal
                if (coma > punto) t = t.replace(".", "").replace(',', '.'); else t = t.replace(",", "");
            } else if (punto >= 0 || coma >= 0) {
                char sep = punto >= 0 ? '.' : ',';
                int cuantos = (int) t.chars().filter(x -> x == sep).count();
                int desde = t.lastIndexOf(sep);
                boolean miles = cuantos > 1 || t.length() - desde - 1 == 3;
                t = miles ? t.replace(String.valueOf(sep), "") : t.replace(sep, '.');
            }
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
