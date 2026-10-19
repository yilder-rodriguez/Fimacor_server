package com.fimacor.importacion.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Genera las plantillas de Excel que SENA llena. Cada una trae los titulos exactos que el importador lee, listas
 * desplegables con los valores reales del sistema (sedes, ambientes, roles, tipos...), una hoja de instrucciones y
 * una de ejemplo. Como salen del propio sistema, siempre estan al dia con lo que hay en la base.
 */
@Service
public class PlantillasService {

    private static final int FILAS_VALIDADAS = LectorHoja.MAX_FILAS + 1;

    private final JdbcTemplate jdbc;

    public PlantillasService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public byte[] generar(TipoImportacion tipo) throws IOException {
        List<Columna> columnas = Definiciones.columnas(tipo);
        Map<String, List<String>> listas = listasDelSistema();
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos est = new Estilos(libro);

            Sheet datos = libro.createSheet(tipo.hoja);
            Sheet instrucciones = libro.createSheet("Instrucciones");
            Sheet ejemplo = libro.createSheet("Ejemplo");
            Sheet hojaListas = libro.createSheet("Listas");

            Map<String, String> rangos = escribirListas(hojaListas, columnas, listas);
            escribirDatos(libro, datos, columnas, rangos, est);
            escribirEjemplo(ejemplo, columnas, est);
            escribirInstrucciones(instrucciones, tipo, columnas, listas, est);

            libro.setSheetHidden(libro.getSheetIndex(hojaListas), true);
            libro.setActiveSheet(0);
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    public byte[] generarTodasEnZip() throws IOException {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (TipoImportacion t : TipoImportacion.values()) {
                zip.putNextEntry(new ZipEntry(t.orden() + "_" + t.archivo));
                zip.write(generar(t));
                zip.closeEntry();
            }
            zip.finish();
            return salida.toByteArray();
        }
    }

    /** Lista de las contrasenas generadas al importar usuarios (para entregarlas una por una). */
    public byte[] credenciales(List<String[]> filas) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos est = new Estilos(libro);
            Sheet hoja = libro.createSheet("Contraseñas iniciales");
            String[] titulos = {"Nombre", "Correo (usuario para entrar)", "Rol", "Contraseña inicial"};
            Row cab = hoja.createRow(0);
            for (int i = 0; i < titulos.length; i++) {
                Cell c = cab.createCell(i);
                c.setCellValue(titulos[i]);
                c.setCellStyle(est.cabeceraObligatoria);
                hoja.setColumnWidth(i, 34 * 256);
            }
            int r = 1;
            for (String[] f : filas) {
                Row fila = hoja.createRow(r++);
                for (int i = 0; i < 4; i++) fila.createCell(i).setCellValue(f[i]);
            }
            Row aviso = hoja.createRow(r + 1);
            Cell n = aviso.createCell(0);
            n.setCellValue("Entrega cada contraseña a su dueño y pídele que la cambie al entrar. Guarda este archivo en un lugar seguro o bórralo.");
            n.setCellStyle(est.nota);
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    // ------------------------------------------------------------------ listas del sistema

    private Map<String, List<String>> listasDelSistema() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("tipoMaquina", consulta("SELECT DISTINCT descripcion_maquina FROM tipo_maquina ORDER BY 1"));
        m.put("marca", consulta("SELECT DISTINCT descripcion_marca FROM marca ORDER BY 1"));
        m.put("sede", consulta("SELECT DISTINCT nombre_sede FROM sede ORDER BY 1"));
        m.put("area", consulta("SELECT DISTINCT nombre_area FROM area ORDER BY 1"));
        m.put("ambiente", consulta("SELECT DISTINCT descripcion_ambiente FROM ambientes_aprendizaje ORDER BY 1"));
        m.put("rol", consulta("SELECT descripcion_rol FROM rol ORDER BY idRol"));
        m.put("tipoMantenimiento", consulta("SELECT DISTINCT descripcion_matenimiento FROM tipo_manteniento ORDER BY 1"));
        m.put("estadoMaquina", List.of(Definiciones.ESTADOS_MAQUINA));
        m.put("tipoDoc", List.of(Definiciones.TIPOS_DOCUMENTO));
        m.put("siNo", List.of(Definiciones.SI_NO));
        m.put("frecuencia", List.of(Definiciones.FRECUENCIAS));
        m.put("tipoFalla", List.of(Definiciones.TIPOS_FALLA));
        m.put("estadoNovedad", List.of(Definiciones.ESTADOS_NOVEDAD));
        m.put("estadoMantenimiento", List.of(Definiciones.ESTADOS_MANTENIMIENTO));
        return m;
    }

    private List<String> consulta(String sql) {
        try {
            List<String> r = new ArrayList<>();
            for (String s : jdbc.queryForList(sql, String.class)) {
                if (s != null && !s.isBlank() && !r.contains(s.trim())) r.add(s.trim());
            }
            return r;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** Escribe en la hoja oculta "Listas" una columna por cada lista que usa la plantilla; devuelve el rango de cada una. */
    private Map<String, String> escribirListas(Sheet hoja, List<Columna> columnas, Map<String, List<String>> listas) {
        Map<String, String> rangos = new LinkedHashMap<>();
        int col = 0;
        for (Columna c : columnas) {
            if (c.lista() == null || rangos.containsKey(c.lista())) continue;
            List<String> valores = listas.getOrDefault(c.lista(), List.of());
            if (valores.isEmpty()) continue;
            Row cab = fila(hoja, 0);
            cab.createCell(col).setCellValue(c.titulo());
            for (int i = 0; i < valores.size(); i++) fila(hoja, i + 1).createCell(col).setCellValue(valores.get(i));
            String letra = org.apache.poi.ss.util.CellReference.convertNumToColString(col);
            rangos.put(c.lista(), "Listas!$" + letra + "$2:$" + letra + "$" + (valores.size() + 1));
            col++;
        }
        return rangos;
    }

    private static Row fila(Sheet hoja, int i) {
        Row r = hoja.getRow(i);
        return r != null ? r : hoja.createRow(i);
    }

    // ------------------------------------------------------------------ hoja de datos

    private void escribirDatos(XSSFWorkbook libro, Sheet hoja, List<Columna> columnas, Map<String, String> rangos, Estilos est) {
        Row cab = hoja.createRow(0);
        cab.setHeightInPoints(34);
        CreationHelper ayudante = libro.getCreationHelper();
        Drawing<?> dibujo = hoja.createDrawingPatriarch();
        DataValidationHelper validador = hoja.getDataValidationHelper();

        for (int i = 0; i < columnas.size(); i++) {
            Columna c = columnas.get(i);
            Cell celda = cab.createCell(i);
            celda.setCellValue(c.obligatoria() ? c.titulo() + " *" : c.titulo());
            celda.setCellStyle(c.obligatoria() ? est.cabeceraObligatoria : est.cabeceraOpcional);
            hoja.setColumnWidth(i, Math.max(c.ancho(), c.titulo().length() + 4) * 256);
            hoja.setDefaultColumnStyle(i, est.deColumna(c.formato()));

            ClientAnchor ancla = ayudante.createClientAnchor();
            ancla.setCol1(i);
            ancla.setCol2(i + 3);
            ancla.setRow1(0);
            ancla.setRow2(5);
            Comment nota = dibujo.createCellComment(ancla);
            nota.setString(ayudante.createRichTextString((c.obligatoria() ? "OBLIGATORIO. " : "Opcional. ") + c.ayuda()));
            nota.setAuthor("FIMACOR");
            celda.setCellComment(nota);

            DataValidationConstraint restriccion = null;
            if (c.lista() != null) {
                String rango = rangos.get(c.lista());
                String[] fijas = valoresFijos(c.lista());
                if (fijas != null) restriccion = validador.createExplicitListConstraint(fijas);
                else if (rango != null) restriccion = validador.createFormulaListConstraint(rango);
            }
            if (restriccion != null) {
                DataValidation v = validador.createValidation(restriccion, new CellRangeAddressList(1, FILAS_VALIDADAS, i, i));
                v.setSuppressDropDownArrow(true); // en POI, true = mostrar la flecha del desplegable
                v.setShowErrorBox(c.estricta());
                if (c.estricta()) {
                    v.setErrorStyle(DataValidation.ErrorStyle.STOP);
                    v.createErrorBox("Valor no válido", "Elige una opción de la lista desplegable.");
                }
                v.setShowPromptBox(true);
                v.createPromptBox(recortar(c.titulo(), 32), recortar(c.ayuda(), 250));
                hoja.addValidationData(v);
            }
        }
        hoja.createFreezePane(0, 1);
        hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, columnas.size() - 1));
    }

    private static String[] valoresFijos(String lista) {
        return switch (lista) {
            case "estadoMaquina" -> Definiciones.ESTADOS_MAQUINA;
            case "tipoDoc" -> Definiciones.TIPOS_DOCUMENTO;
            case "siNo" -> Definiciones.SI_NO;
            case "frecuencia" -> Definiciones.FRECUENCIAS;
            case "tipoFalla" -> Definiciones.TIPOS_FALLA;
            case "estadoNovedad" -> Definiciones.ESTADOS_NOVEDAD;
            case "estadoMantenimiento" -> Definiciones.ESTADOS_MANTENIMIENTO;
            default -> null;
        };
    }

    // ------------------------------------------------------------------ ejemplo e instrucciones

    private void escribirEjemplo(Sheet hoja, List<Columna> columnas, Estilos est) {
        Row cab = hoja.createRow(0);
        cab.setHeightInPoints(34);
        Row f1 = hoja.createRow(1), f2 = hoja.createRow(2);
        for (int i = 0; i < columnas.size(); i++) {
            Columna c = columnas.get(i);
            Cell h = cab.createCell(i);
            h.setCellValue(c.obligatoria() ? c.titulo() + " *" : c.titulo());
            h.setCellStyle(c.obligatoria() ? est.cabeceraObligatoria : est.cabeceraOpcional);
            hoja.setColumnWidth(i, Math.max(c.ancho(), c.titulo().length() + 4) * 256);
            Cell a = f1.createCell(i);
            a.setCellValue(c.ejemplo1());
            a.setCellStyle(est.ejemplo);
            Cell b = f2.createCell(i);
            b.setCellValue(c.ejemplo2());
            b.setCellStyle(est.ejemplo);
        }
        Row aviso = hoja.createRow(4);
        Cell n = aviso.createCell(0);
        n.setCellValue("Esta hoja es solo un ejemplo de cómo se llena. Los datos que se importan son los de la primera hoja.");
        n.setCellStyle(est.nota);
        hoja.createFreezePane(0, 1);
    }

    private void escribirInstrucciones(Sheet hoja, TipoImportacion tipo, List<Columna> columnas, Map<String, List<String>> listas, Estilos est) {
        int r = 0;
        r = linea(hoja, r, "Plantilla de " + tipo.nombre.toLowerCase() + " — FIMACOR", est.titulo);
        r = linea(hoja, r, tipo.descripcion, est.normal);
        r++;
        r = linea(hoja, r, "ORDEN RECOMENDADO PARA CARGAR LA INFORMACIÓN", est.subtitulo);
        for (TipoImportacion t : TipoImportacion.values()) {
            r = linea(hoja, r, t.orden() + ". " + t.nombre + (t == tipo ? "   ← esta plantilla" : ""), t == tipo ? est.resaltado : est.normal);
        }
        r++;
        r = linea(hoja, r, "CÓMO LLENARLA", est.subtitulo);
        for (String s : reglasGenerales(tipo)) r = linea(hoja, r, s, est.normal);
        r++;
        r = linea(hoja, r, "COLUMNAS (en la primera hoja; las marcadas con * son obligatorias)", est.subtitulo);
        Row cab = hoja.createRow(r++);
        String[] titulos = {"Columna", "¿Obligatoria?", "Qué escribir", "Opciones válidas", "Ejemplo"};
        for (int i = 0; i < titulos.length; i++) {
            Cell c = cab.createCell(i);
            c.setCellValue(titulos[i]);
            c.setCellStyle(est.cabeceraOpcional);
        }
        for (Columna c : columnas) {
            Row fila = hoja.createRow(r++);
            fila.createCell(0).setCellValue(c.titulo());
            fila.createCell(1).setCellValue(c.obligatoria() ? "Sí" : "No");
            fila.createCell(2).setCellValue(c.ayuda());
            fila.createCell(3).setCellValue(opcionesTexto(c, listas));
            fila.createCell(4).setCellValue(c.ejemplo1().isEmpty() ? c.ejemplo2() : c.ejemplo1());
            for (int i = 0; i < 5; i++) fila.getCell(i).setCellStyle(est.celdaTabla);
        }
        r++;
        r = linea(hoja, r, "NOTAS DE ESTA PLANTILLA", est.subtitulo);
        for (String s : notas(tipo)) r = linea(hoja, r, s, est.normal);
        hoja.setColumnWidth(0, 34 * 256);
        hoja.setColumnWidth(1, 14 * 256);
        hoja.setColumnWidth(2, 70 * 256);
        hoja.setColumnWidth(3, 50 * 256);
        hoja.setColumnWidth(4, 34 * 256);
    }

    private static int linea(Sheet hoja, int fila, String texto, XSSFCellStyle estilo) {
        Cell c = hoja.createRow(fila).createCell(0);
        c.setCellValue(texto);
        c.setCellStyle(estilo);
        return fila + 1;
    }

    private static String opcionesTexto(Columna c, Map<String, List<String>> listas) {
        if (c.lista() == null) return "";
        List<String> v = listas.getOrDefault(c.lista(), List.of());
        if (v.isEmpty()) return c.estricta() ? "(lista vacía en el sistema)" : "(cualquier texto)";
        String texto = String.join(", ", v.size() > 12 ? v.subList(0, 12) : v) + (v.size() > 12 ? ", … (" + v.size() + " opciones en el desplegable)" : "");
        return c.estricta() ? texto : texto + "  (también puedes escribir uno nuevo)";
    }

    private static List<String> reglasGenerales(TipoImportacion tipo) {
        List<String> l = new ArrayList<>();
        l.add("• Llena la primera hoja, una fila por registro, desde la fila 2. No cambies ni borres los títulos de la fila 1.");
        l.add("• Pasa el mouse sobre cada título para ver qué va en esa columna. Las celdas con flecha ▼ tienen una lista de opciones: elígelas de ahí.");
        l.add("• Las columnas con * son obligatorias; si una fila no las trae, no se importa y el sistema te dice cuál es el problema.");
        l.add("• Fechas: 2026-03-15 o 15/03/2026 (o una celda de fecha de Excel). Horas: 14:30.");
        l.add("• Máximo " + LectorHoja.MAX_FILAS + " filas por archivo. También se acepta guardar la hoja como .csv.");
        l.add("• Antes de guardar nada, el sistema te muestra un resumen de lo que va a pasar y las filas con errores. Puedes corregir y volver a subir.");
        return l;
    }

    private static List<String> notas(TipoImportacion tipo) {
        return switch (tipo) {
            case USUARIOS -> List.of(
                "• Si el correo o el número de documento ya existen en el sistema, esa fila se omite: no se cambia a nadie que ya esté registrado.",
                "• Si no escribes contraseña, el sistema genera una por persona y, al terminar, te deja descargar la lista para entregársela a cada quien.",
                "• Los Instructores reciben automáticamente su código para reportar novedades por QR.",
                "• Las cuentas se crean activas y vigentes por un año.");
            case MAQUINAS -> List.of(
                "• Si el Código SENA ya existe, la máquina se actualiza con lo que traiga el archivo; lo que dejes vacío no borra el dato que ya tenía.",
                "• Si el tipo, la marca, el modelo, la sede, el área o el ambiente no existen, se crean automáticamente (antes de importar verás la lista).",
                "• Para asignar el cuentadante, primero impórtalo en la plantilla de Usuarios con el rol Cuentadante.",
                "• Las máquinas dadas de baja no se modifican.");
            case NOVEDADES -> List.of(
                "• Importa antes Usuarios y Máquinas: la máquina y las personas deben existir.",
                "• La sede, el área y el ambiente de la novedad se toman de la máquina.",
                "• Si la novedad ya está registrada (misma máquina, fecha y descripción) se omite, así puedes volver a subir el archivo sin duplicar.",
                "• No se envían notificaciones ni se cambia el estado de la máquina: ese estado sale de la plantilla de Máquinas.");
            case MANTENIMIENTOS -> List.of(
                "• Importa antes Usuarios y Máquinas: la máquina y el técnico deben existir.",
                "• El técnico debe poder atender mantenimientos (rol Técnico, o Instructor marcado como técnico).",
                "• Si ya existe el mismo mantenimiento (misma máquina, tipo, fecha y técnico) se omite, así puedes volver a subir el archivo sin duplicar.",
                "• No se envían notificaciones ni se cambia el estado de la máquina: ese estado sale de la plantilla de Máquinas.");
        };
    }

    private static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // ------------------------------------------------------------------ estilos

    private static final class Estilos {
        final XSSFCellStyle cabeceraObligatoria, cabeceraOpcional, ejemplo, nota, titulo, subtitulo, normal, resaltado, celdaTabla;
        private final XSSFWorkbook libro;
        private XSSFCellStyle texto, fecha, fechaHora, numero;

        Estilos(XSSFWorkbook libro) {
            this.libro = libro;
            cabeceraObligatoria = cabecera(new XSSFColor(new byte[] {(byte) 0x2E, (byte) 0x7D, (byte) 0x32}, null), true);
            cabeceraOpcional = cabecera(new XSSFColor(new byte[] {(byte) 0xC8, (byte) 0xE6, (byte) 0xC9}, null), false);
            ejemplo = base(false, false, 0);
            ejemplo.getFont().setItalic(true);
            ejemplo.getFont().setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            nota = base(false, false, 0);
            nota.getFont().setItalic(true);
            titulo = base(true, false, 16);
            subtitulo = base(true, false, 12);
            subtitulo.getFont().setColor(new XSSFColor(new byte[] {(byte) 0x1B, (byte) 0x5E, (byte) 0x20}, null));
            normal = base(false, false, 0);
            resaltado = base(true, false, 0);
            celdaTabla = base(false, true, 0);
            celdaTabla.setWrapText(true);
            celdaTabla.setVerticalAlignment(VerticalAlignment.TOP);
        }

        private XSSFCellStyle base(boolean negrita, boolean borde, int tamano) {
            XSSFCellStyle s = libro.createCellStyle();
            XSSFFont f = libro.createFont();
            f.setBold(negrita);
            if (tamano > 0) f.setFontHeightInPoints((short) tamano);
            s.setFont(f);
            if (borde) {
                s.setBorderBottom(BorderStyle.THIN);
                s.setBorderTop(BorderStyle.THIN);
                s.setBorderLeft(BorderStyle.THIN);
                s.setBorderRight(BorderStyle.THIN);
            }
            return s;
        }

        private XSSFCellStyle cabecera(XSSFColor fondo, boolean textoBlanco) {
            XSSFCellStyle s = base(true, true, 0);
            s.setFillForegroundColor(fondo);
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setWrapText(true);
            if (textoBlanco) s.getFont().setColor(IndexedColors.WHITE.getIndex());
            return s;
        }

        XSSFCellStyle deColumna(Columna.Formato formato) {
            switch (formato) {
                case FECHA:
                    if (fecha == null) { fecha = libro.createCellStyle(); fecha.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd")); }
                    return fecha;
                case FECHA_HORA:
                    if (fechaHora == null) { fechaHora = libro.createCellStyle(); fechaHora.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd hh:mm")); }
                    return fechaHora;
                case NUMERO:
                    if (numero == null) { numero = libro.createCellStyle(); numero.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("General")); }
                    return numero;
                default:
                    // Texto: conserva ceros a la izquierda en documentos y codigos, y horas como "14:30".
                    if (texto == null) { texto = libro.createCellStyle(); texto.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("@")); }
                    return texto;
            }
        }
    }
}
