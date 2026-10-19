package com.fimacor.importacion.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Lee una hoja de calculo (.xlsx, .xls o .csv) y la devuelve como filas de texto. Las celdas de fecha salen como
 * yyyy-MM-dd y los numeros sin notacion cientifica, para que el resto de la importacion trate todo como texto.
 */
final class LectorHoja {

    static final int MAX_FILAS = 5000;

    private LectorHoja() {
    }

    static List<List<String>> leer(String nombreArchivo, byte[] datos) throws IOException {
        String nombre = nombreArchivo == null ? "" : nombreArchivo.toLowerCase();
        if (nombre.endsWith(".csv") || nombre.endsWith(".txt")) return leerCsv(datos);
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xlsm") || nombre.endsWith(".xls")) return leerExcel(datos);
        throw new IOException("Formato no admitido. Usa un archivo .xlsx, .xls o .csv.");
    }

    // ---------------- Excel ----------------

    private static List<List<String>> leerExcel(byte[] datos) throws IOException {
        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(datos))) {
            Sheet hoja = elegirHoja(libro);
            if (hoja == null) throw new IOException("El archivo no tiene hojas con datos.");
            DataFormatter formato = new DataFormatter(java.util.Locale.US);
            FormulaEvaluator evaluador = libro.getCreationHelper().createFormulaEvaluator();
            List<List<String>> filas = new ArrayList<>();
            int ultima = hoja.getLastRowNum();
            for (int i = 0; i <= ultima; i++) {
                Row fila = hoja.getRow(i);
                List<String> celdas = new ArrayList<>();
                if (fila != null) {
                    int n = Math.max(0, fila.getLastCellNum());
                    for (int c = 0; c < n; c++) celdas.add(texto(fila.getCell(c), formato, evaluador));
                }
                filas.add(celdas);
                if (filas.size() > MAX_FILAS + 20) throw new IOException("El archivo tiene demasiadas filas (maximo " + MAX_FILAS + " por importacion).");
            }
            return filas;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().startsWith("El archivo tiene demasiadas")) throw new IOException(e.getMessage());
            throw new IOException("No se pudo leer el archivo. Verifica que sea un Excel (.xlsx o .xls) válido y que no esté protegido con contraseña.", e);
        }
    }

    /** Prefiere una hoja llamada "inventario"; si no, la primera que tenga mas de una fila. */
    private static Sheet elegirHoja(Workbook libro) {
        Sheet primera = null;
        for (int i = 0; i < libro.getNumberOfSheets(); i++) {
            Sheet h = libro.getSheetAt(i);
            if (libro.isSheetHidden(i) || libro.isSheetVeryHidden(i)) continue;
            String nombre = h.getSheetName().toLowerCase();
            // Las plantillas de FIMACOR traen hojas de apoyo (instrucciones, ejemplo, listas) que no son datos.
            if (nombre.contains("instruccion") || nombre.contains("ejemplo") || nombre.equals("listas")) continue;
            if (nombre.contains("inventario") && h.getLastRowNum() >= 1) return h;
            if (primera == null && h.getLastRowNum() >= 1) primera = h;
        }
        return primera;
    }

    private static String texto(Cell celda, DataFormatter formato, FormulaEvaluator evaluador) {
        if (celda == null) return "";
        try {
            CellType tipo = celda.getCellType();
            if (tipo == CellType.FORMULA) tipo = evaluador.evaluateFormulaCell(celda);
            switch (tipo) {
                case STRING:
                    return celda.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(celda)) {
                        java.time.LocalDateTime momento = celda.getLocalDateTimeCellValue();
                        if (celda.getNumericCellValue() < 1) return String.format("%02d:%02d", momento.getHour(), momento.getMinute()); // solo hora
                        if (momento.getHour() == 0 && momento.getMinute() == 0) return momento.toLocalDate().toString();
                        return momento.toLocalDate() + String.format(" %02d:%02d", momento.getHour(), momento.getMinute());
                    }
                    return numero(celda.getNumericCellValue());
                case BOOLEAN:
                    return celda.getBooleanCellValue() ? "Sí" : "No";
                case BLANK:
                    return "";
                default:
                    return formato.formatCellValue(celda, evaluador).trim();
            }
        } catch (RuntimeException e) {
            return formato.formatCellValue(celda).trim();
        }
    }

    private static String numero(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return Long.toString((long) d);
        return BigDecimal.valueOf(d).toPlainString();
    }

    // ---------------- CSV ----------------

    private static List<List<String>> leerCsv(byte[] datos) throws IOException {
        String contenido = decodificar(datos);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);
        int fin = contenido.indexOf('\n');
        String primera = fin < 0 ? contenido : contenido.substring(0, fin);
        char separador = ';';
        int mejor = -1;
        for (char c : new char[] {';', ',', '\t'}) {
            int n = (int) primera.chars().filter(x -> x == c).count();
            if (n > mejor) { mejor = n; separador = c; }
        }

        List<List<String>> filas = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < contenido.length(); i++) {
            char c = contenido.charAt(i);
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < contenido.length() && contenido.charAt(i + 1) == '"') { celda.append('"'); i++; }
                    else entreComillas = false;
                } else celda.append(c);
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == separador) {
                actual.add(celda.toString().trim()); celda.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < contenido.length() && contenido.charAt(i + 1) == '\n') i++;
                actual.add(celda.toString().trim()); celda.setLength(0);
                filas.add(actual); actual = new ArrayList<>();
                if (filas.size() > MAX_FILAS + 20) throw new IOException("El archivo tiene demasiadas filas (maximo " + MAX_FILAS + " por importacion).");
            } else celda.append(c);
        }
        if (celda.length() > 0 || !actual.isEmpty()) { actual.add(celda.toString().trim()); filas.add(actual); }
        return filas;
    }

    private static String decodificar(byte[] datos) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(datos)).toString();
        } catch (CharacterCodingException e) {
            return new String(datos, StandardCharsets.ISO_8859_1); // CSV guardado desde Excel en Windows
        }
    }
}
