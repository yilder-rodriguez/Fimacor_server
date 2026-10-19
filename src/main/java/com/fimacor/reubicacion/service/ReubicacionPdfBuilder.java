package com.fimacor.reubicacion.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Genera el PDF del acta de reubicacion escribiendo los bytes del documento
 * a mano, sin usar ninguna libreria de PDF. Se comparte entre la descarga
 * del acta y el adjunto de los correos de reubicacion, para no duplicar el
 * generador de bytes.
 */
@Component
public class ReubicacionPdfBuilder {

    private static final Charset PDF_CHARSET = Charset.forName("windows-1252");

    public byte[] generar(String titulo, List<String> lineas) {
        List<String> paginas = paginar(lineas);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        try {
            escribir(out, "%PDF-1.4\n");
            int totalObjetos = 2 + paginas.size() * 2;
            offsets.add(0);
            offsets.add(out.size());
            escribir(out, "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
            offsets.add(out.size());
            escribir(out, "2 0 obj << /Type /Pages /Kids [");
            for (int i = 0; i < paginas.size(); i++) escribir(out, (3 + i * 2) + " 0 R ");
            escribir(out, "] /Count " + paginas.size() + " >> endobj\n");

            for (int i = 0; i < paginas.size(); i++) {
                int pageObj = 3 + i * 2;
                int contentObj = pageObj + 1;
                byte[] bytes = contenidoPagina(paginas.get(i), i + 1, paginas.size(), titulo).getBytes(PDF_CHARSET);
                offsets.add(out.size());
                escribir(out, pageObj + " 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                        + "/Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >> "
                        + "/F2 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >> >> >> "
                        + "/Contents " + contentObj + " 0 R >> endobj\n");
                offsets.add(out.size());
                escribir(out, contentObj + " 0 obj << /Length " + bytes.length + " >> stream\n");
                out.write(bytes);
                escribir(out, "\nendstream endobj\n");
            }

            int xref = out.size();
            escribir(out, "xref\n0 " + (totalObjetos + 1) + "\n0000000000 65535 f \n");
            for (int i = 1; i <= totalObjetos; i++) escribir(out, String.format("%010d 00000 n \n", offsets.get(i)));
            escribir(out, "trailer << /Size " + (totalObjetos + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private List<String> paginar(List<String> lineas) {
        List<String> paginas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        int n = 0;
        for (String linea : lineas) {
            int ancho = linea.startsWith("## ") ? 72 : 88;
            for (String parte : partir(linea, ancho)) {
                if (n >= 40) {
                    paginas.add(actual.toString());
                    actual.setLength(0);
                    n = 0;
                }
                actual.append(parte).append('\n');
                n += linea.startsWith("## ") ? 2 : 1;
            }
        }
        if (actual.length() > 0) paginas.add(actual.toString());
        return paginas;
    }

    private String contenidoPagina(String texto, int pagina, int total, String titulo) {
        StringBuilder s = new StringBuilder();
        s.append("0.04 0.21 0.42 rg 0 724 612 68 re f\n");
        s.append("1 1 1 rg BT /F2 20 Tf 45 760 Td (").append(esc(titulo)).append(") Tj ET\n");
        s.append("0.82 0.91 1 rg BT /F1 9 Tf 45 742 Td (Documento de control y trazabilidad - Pagina ")
                .append(pagina).append(" de ").append(total).append(") Tj ET\n");
        int y = 700;
        for (String l : texto.split("\n")) {
            if (l.isEmpty()) {
                y -= 10;
                continue;
            }
            if (l.startsWith("## ")) {
                s.append("0.90 0.96 0.92 rg 45 ").append(y - 5).append(" 522 22 re f\n");
                s.append("0.08 0.37 0.20 rg BT /F2 11 Tf 56 ").append(y + 2).append(" Td (").append(esc(l.substring(3))).append(") Tj ET\n");
                y -= 28;
            } else {
                s.append("0.12 0.14 0.18 rg BT /F1 9 Tf 56 ").append(y).append(" Td (").append(esc(l)).append(") Tj ET\n");
                y -= 14;
            }
        }
        s.append("0.35 0.40 0.45 rg BT /F1 8 Tf 45 28 Td (FIMACOR - Documento generado automaticamente) Tj ET\n");
        return s.toString();
    }

    private List<String> partir(String texto, int max) {
        List<String> partes = new ArrayList<>();
        String limpio = limpiar(texto);
        while (limpio.length() > max) {
            int corte = limpio.lastIndexOf(' ', max);
            if (corte < 20) corte = max;
            partes.add(limpio.substring(0, corte));
            limpio = limpio.substring(corte).trim();
        }
        partes.add(limpio);
        return partes;
    }

    private void escribir(ByteArrayOutputStream out, String texto) throws IOException {
        out.write(texto.getBytes(PDF_CHARSET));
    }

    private String esc(String valor) {
        return limpiar(valor).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
    }
}
