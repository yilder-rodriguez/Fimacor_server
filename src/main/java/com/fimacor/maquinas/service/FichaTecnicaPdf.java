package com.fimacor.maquinas.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

/**
 * Dibuja la ficha tecnica con OpenPDF siguiendo el mismo lenguaje Material 3 de las pantallas del QR
 * (fimacor-material.css): tarjeta oscura de marca con costura punteada, secciones numeradas, tarjetas
 * tonales, tablas editoriales y chips de estado con punto. Solo se encarga del diseno; los datos
 * vienen armados desde FichaTecnicaService.
 */
final class FichaTecnicaPdf {

    // Paleta Material 3 (misma que los tokens --md-* de fimacor-material.css).
    static final Color PRIMARY = new Color(46, 125, 50);
    static final Color ON_PRIMARY_CONTAINER = new Color(7, 42, 12);
    static final Color PRIMARY_CONTAINER = new Color(200, 235, 198);
    static final Color SURFACE_CONTAINER_LOW = new Color(241, 245, 236);
    static final Color SURFACE_CONTAINER_HIGH = new Color(229, 234, 224);
    static final Color ON_SURFACE = new Color(25, 28, 24);
    static final Color ON_SURFACE_VARIANT = new Color(66, 73, 64);
    static final Color OUTLINE = new Color(114, 121, 112);
    static final Color OUTLINE_VARIANT = new Color(194, 201, 189);
    static final Color INVERSE_SURFACE = new Color(13, 27, 42);
    static final Color INVERSE_ON_SURFACE = new Color(199, 211, 221);
    static final Color INVERSE_PRIMARY = new Color(143, 216, 148);
    static final Color COSTURA = new Color(58, 93, 79);
    static final Color CHIP_OSCURO = new Color(35, 48, 62);

    /** Contenedor tonal y su color de texto, como los chips de la interfaz. */
    record Tono(Color fondo, Color texto) {
    }

    static final Tono TONO_VERDE = new Tono(PRIMARY_CONTAINER, ON_PRIMARY_CONTAINER);
    static final Tono TONO_AMARILLO = new Tono(new Color(255, 224, 143), new Color(38, 26, 0));
    static final Tono TONO_ROJO = new Tono(new Color(255, 218, 214), new Color(65, 0, 2));
    static final Tono TONO_GRIS = new Tono(SURFACE_CONTAINER_HIGH, ON_SURFACE_VARIANT);

    /** Un dato "etiqueta: valor" para las tarjetas. */
    record Dato(String etiqueta, String valor) {
    }

    record Etiqueta(String texto, Tono tono) {
    }

    /** Una tabla de la ficha. columnaEstado = indice de la columna que se pinta como chip (o -1). */
    record Tabla(String titulo, String vacio, String[] encabezados, float[] anchos, List<String[]> filas, int columnaEstado) {
    }

    private static final float ANCHO_UTIL = 523;
    private final Document documento;
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private int seccion;

    FichaTecnicaPdf(String codigo, String generado) {
        documento = new Document(PageSize.A4, 36, 36, 158, 58);
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            String idDocumento = "FT-" + codigo + "-" + generado.replaceAll("[^0-9]", "");
            writer.setPageEvent(new Marco(codigo, generado, idDocumento, logo()));
            documento.addTitle("Ficha técnica " + codigo);
            documento.addSubject("Documento " + idDocumento);
            documento.addCreator("FIMACOR");
            documento.open();
            // Desde la segunda pagina el encabezado es una tarjeta delgada.
            documento.setMargins(36, 36, 84, 58);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo iniciar la ficha técnica", e);
        }
    }

    byte[] terminar() {
        documento.close();
        return salida.toByteArray();
    }

    void titulo(String nombre, List<Etiqueta> etiquetas) {
        Paragraph overline = new Paragraph(espaciado("MÁQUINA REGISTRADA", fuente(7.5f, Font.BOLD, PRIMARY), 1.2f));
        overline.setSpacingAfter(2);
        agregar(overline);
        Paragraph p = new Paragraph(nombre, fuente(22, Font.NORMAL, ON_SURFACE));
        p.setLeading(26);
        p.setSpacingAfter(10);
        agregar(p);
        if (etiquetas.isEmpty()) return;
        PdfPTable fila = new PdfPTable(etiquetas.size() + 1);
        float[] anchos = new float[etiquetas.size() + 1];
        float usado = 0;
        for (int i = 0; i < etiquetas.size(); i++) {
            anchos[i] = anchoTexto(etiquetas.get(i).texto(), 8.5f) + 34;
            usado += anchos[i];
        }
        anchos[etiquetas.size()] = Math.max(1, ANCHO_UTIL - usado);
        try {
            fila.setTotalWidth(anchos);
        } catch (DocumentException e) {
            throw new IllegalStateException(e);
        }
        fila.setLockedWidth(true);
        for (Etiqueta e : etiquetas) fila.addCell(chip(e.texto(), e.tono(), 8.5f, 6));
        PdfPCell resto = new PdfPCell();
        resto.setBorder(Rectangle.NO_BORDER);
        fila.addCell(resto);
        fila.setSpacingAfter(18);
        agregar(fila);
    }

    void aviso(String titulo, String texto, Tono tono) {
        PdfPTable caja = new PdfPTable(1);
        caja.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setCellEvent(new Contenedor(tono.fondo(), null, 16));
        celda.setPadding(14);
        Paragraph p = new Paragraph();
        p.setLeading(13);
        p.add(new Chunk(titulo + "\n", fuente(10.5f, Font.BOLD, tono.texto())));
        p.add(new Chunk(texto, fuente(9, Font.NORMAL, tono.texto())));
        celda.addElement(p);
        caja.addCell(celda);
        caja.setSpacingAfter(18);
        agregar(caja);
    }

    /** Tarjeta tonal (relleno, sin borde) con los datos en dos columnas. */
    void tarjeta(String titulo, List<Dato> datos, String vacio) {
        tituloSeccion(titulo, -1);
        if (datos.isEmpty()) {
            vacio(vacio);
            return;
        }
        PdfPTable marco = new PdfPTable(1);
        marco.setWidthPercentage(100);
        marco.setKeepTogether(true);
        PdfPCell contenedor = new PdfPCell();
        contenedor.setBorder(Rectangle.NO_BORDER);
        contenedor.setCellEvent(new Contenedor(SURFACE_CONTAINER_LOW, null, 16));
        contenedor.setPadding(10);
        PdfPTable grilla = new PdfPTable(2);
        grilla.setWidthPercentage(100);
        for (Dato d : datos) {
            PdfPCell c = new PdfPCell();
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(6);
            c.setPaddingBottom(8);
            Paragraph p = new Paragraph();
            p.setLeading(13);
            p.add(espaciado(d.etiqueta().toUpperCase() + "\n", fuente(6.8f, Font.BOLD, ON_SURFACE_VARIANT), .5f));
            p.add(new Chunk(d.valor(), fuente(10.5f, Font.NORMAL, ON_SURFACE)));
            c.addElement(p);
            grilla.addCell(c);
        }
        if (datos.size() % 2 == 1) {
            PdfPCell relleno = new PdfPCell();
            relleno.setBorder(Rectangle.NO_BORDER);
            grilla.addCell(relleno);
        }
        contenedor.addElement(grilla);
        marco.addCell(contenedor);
        marco.setSpacingAfter(20);
        agregar(marco);
    }

    /** Tabla editorial dentro de una tarjeta delineada; la columna de estado se pinta como chip. */
    void tabla(Tabla t) {
        tituloSeccion(t.titulo(), t.filas().size());
        if (t.filas().isEmpty()) {
            vacio(t.vacio());
            return;
        }
        PdfPTable tabla = new PdfPTable(t.anchos());
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (String encabezado : t.encabezados()) {
            PdfPCell c = new PdfPCell(new Phrase(espaciado(encabezado.toUpperCase(), fuente(6.8f, Font.BOLD, ON_SURFACE_VARIANT), .5f)));
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(OUTLINE);
            c.setBorderWidth(.8f);
            c.setPadding(7);
            c.setPaddingTop(9);
            tabla.addCell(c);
        }
        for (int f = 0; f < t.filas().size(); f++) {
            String[] valores = t.filas().get(f);
            boolean ultima = f == t.filas().size() - 1;
            for (int i = 0; i < valores.length; i++) {
                PdfPCell c;
                if (i == t.columnaEstado()) {
                    // El chip se dibuja en la misma celda, ajustado al ancho del texto.
                    c = new PdfPCell(new Phrase(valores[i], fuente(7.5f, Font.BOLD, tono(valores[i]).texto())));
                    c.setCellEvent(new Chip(tono(valores[i]), 0, anchoTexto(valores[i], 7.5f) + 26, 5));
                    c.setPaddingLeft(23);
                    c.setPaddingTop(7);
                    c.setPaddingBottom(10);
                } else {
                    c = new PdfPCell(new Phrase(valores[i], fuente(8.5f, i == 0 ? Font.BOLD : Font.NORMAL, ON_SURFACE)));
                    c.setLeading(0, 1.3f);
                    c.setPadding(7);
                    c.setPaddingBottom(9);
                }
                c.setBorder(ultima ? Rectangle.NO_BORDER : Rectangle.BOTTOM);
                c.setBorderColor(OUTLINE_VARIANT);
                c.setBorderWidth(.5f);
                c.setVerticalAlignment(Element.ALIGN_MIDDLE);
                tabla.addCell(c);
            }
        }
        PdfPTable marco = new PdfPTable(1);
        marco.setWidthPercentage(100);
        PdfPCell contenedor = new PdfPCell(tabla);
        contenedor.setBorder(Rectangle.NO_BORDER);
        contenedor.setCellEvent(new Contenedor(Color.WHITE, OUTLINE_VARIANT, 14));
        contenedor.setPadding(8);
        marco.addCell(contenedor);
        marco.setSpacingAfter(20);
        agregar(marco);
    }

    private void vacio(String texto) {
        PdfPTable caja = new PdfPTable(1);
        caja.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente(9, Font.NORMAL, ON_SURFACE_VARIANT)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setCellEvent(new Contenedor(SURFACE_CONTAINER_LOW, null, 14));
        c.setPadding(13);
        caja.addCell(c);
        caja.setSpacingAfter(20);
        agregar(caja);
    }

    /** "01  Datos principales ............ 3 registros" con linea divisoria. */
    private void tituloSeccion(String titulo, int cantidad) {
        seccion++;
        PdfPTable fila = new PdfPTable(new float[] {26, 380, 117});
        fila.setWidthPercentage(100);
        fila.setKeepTogether(true);
        PdfPCell numero = new PdfPCell(new Phrase(String.format("%02d", seccion), fuente(10, Font.BOLD, PRIMARY)));
        PdfPCell texto = new PdfPCell(new Phrase(titulo, fuente(12, Font.BOLD, ON_SURFACE)));
        PdfPCell conteo = new PdfPCell(new Phrase(cantidad < 0 ? "" : cantidad + (cantidad == 1 ? " registro" : " registros"),
                fuente(8, Font.NORMAL, ON_SURFACE_VARIANT)));
        conteo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (PdfPCell c : new PdfPCell[] {numero, texto, conteo}) {
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(OUTLINE_VARIANT);
            c.setBorderWidth(.6f);
            c.setPaddingLeft(0);
            c.setPaddingBottom(7);
            c.setVerticalAlignment(Element.ALIGN_BOTTOM);
            fila.addCell(c);
        }
        fila.setSpacingAfter(10);
        agregar(fila);
    }

    /** Chip tonal con punto inicial (radio completo). */
    private PdfPCell chip(String texto, Tono tono, float tamano, float separacion) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente(tamano, Font.BOLD, tono.texto())));
        c.setBorder(Rectangle.NO_BORDER);
        c.setCellEvent(new Chip(tono, separacion, 0, 0));
        c.setPaddingLeft(18);
        c.setPaddingRight(8 + separacion);
        c.setPaddingTop(4.5f);
        c.setPaddingBottom(7);
        c.setNoWrap(true);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    static Tono tono(String estado) {
        String t = estado == null ? "" : Normalizer.normalize(estado, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        if (t.matches(".*(fuera|dan|aver|falla|vencid|rechaz|cancel|baja).*")) return TONO_ROJO;
        if (t.matches(".*(mantenimiento|revision|pendiente|proxim|reparacion|asignad|reportad).*")) return TONO_AMARILLO;
        if (t.matches(".*(operativ|reparad|finaliz|cerrad|aprob|complet|vigente).*")) return TONO_VERDE;
        return TONO_GRIS;
    }

    private void agregar(Element elemento) {
        try {
            documento.add(elemento);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo dibujar la ficha técnica", e);
        }
    }

    static Font fuente(float tamano, int estilo, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED, tamano, estilo, color);
    }

    private static Chunk espaciado(String texto, Font fuente, float espacio) {
        Chunk chunk = new Chunk(texto, fuente);
        chunk.setCharacterSpacing(espacio);
        return chunk;
    }

    private static float anchoTexto(String texto, float tamano) {
        return fuente(tamano, Font.BOLD, ON_SURFACE).getCalculatedBaseFont(false).getWidthPoint(texto, tamano);
    }

    private static Image logo() {
        try (InputStream in = new ClassPathResource("static/Img/Sena.png").getInputStream()) {
            Image imagen = Image.getInstance(in.readAllBytes());
            imagen.scaleToFit(30, 30);
            return imagen;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Fondo redondeado (y borde opcional) para tarjetas, tablas y avisos. */
    private record Contenedor(Color fondo, Color borde, float radio) implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell celda, Rectangle r, PdfContentByte[] lienzos) {
            PdfContentByte cb = lienzos[PdfPTable.BACKGROUNDCANVAS];
            cb.saveState();
            cb.setColorFill(fondo);
            if (borde != null) {
                cb.setColorStroke(borde);
                cb.setLineWidth(.7f);
            }
            cb.roundRectangle(r.getLeft() + .5f, r.getBottom() + .5f, r.getWidth() - 1, r.getHeight() - 1, radio);
            if (borde != null) cb.fillStroke(); else cb.fill();
            cb.restoreState();
        }
    }

    /**
     * Fondo de chip (pildora) con el punto de color a la izquierda. Con anchoFijo > 0 la pildora mide
     * eso y se centra en vertical dentro de la celda (chips dentro de tablas); si no, ocupa la celda.
     */
    private record Chip(Tono tono, float separacion, float anchoFijo, float izquierda) implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell celda, Rectangle r, PdfContentByte[] lienzos) {
            // Capa de lineas: queda sobre el fondo de la tarjeta que envuelve la tabla y bajo el texto.
            PdfContentByte cb = lienzos[PdfPTable.LINECANVAS];
            float alto = anchoFijo > 0 ? 17 : r.getHeight() - 2;
            float ancho = anchoFijo > 0 ? Math.min(anchoFijo, r.getWidth() - izquierda - 2) : r.getWidth() - 1 - separacion;
            float x = r.getLeft() + (anchoFijo > 0 ? izquierda : .5f);
            float y = anchoFijo > 0 ? r.getBottom() + (r.getHeight() - alto) / 2 : r.getBottom() + 1;
            cb.saveState();
            cb.setColorFill(tono.fondo());
            cb.roundRectangle(x, y, ancho, alto, alto / 2);
            cb.fill();
            cb.setColorFill(tono.texto());
            cb.circle(x + 9.5f, y + alto / 2, 2.4f);
            cb.fill();
            cb.restoreState();
        }
    }

    /** Encabezado de marca (tarjeta grande en la primera pagina), pie con codigo de documento y paginas. */
    private static final class Marco extends PdfPageEventHelper {
        private final String codigo;
        private final String generado;
        private final String idDocumento;
        private final Image logo;
        private PdfTemplate total;

        Marco(String codigo, String generado, String idDocumento, Image logo) {
            this.codigo = codigo;
            this.generado = generado;
            this.idDocumento = idDocumento;
            this.logo = logo;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(40, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContentUnder();
            float ancho = document.getPageSize().getWidth();
            float alto = document.getPageSize().getHeight();
            int pagina = writer.getPageNumber();
            try {
                if (pagina == 1) encabezadoGrande(cb, ancho, alto);
                else encabezadoDelgado(cb, ancho, alto);

                cb.setColorStroke(OUTLINE_VARIANT);
                cb.setLineWidth(.6f);
                cb.moveTo(36, 40);
                cb.lineTo(ancho - 36, 40);
                cb.stroke();
                texto(cb, "Documento " + idDocumento + "  ·  Generado desde el código QR de la máquina", 36, 26, 7.5f, false, ON_SURFACE_VARIANT, Element.ALIGN_LEFT);
                BaseFont base = fuente(7.5f, Font.NORMAL, ON_SURFACE_VARIANT).getCalculatedBaseFont(false);
                String paginaTexto = "Página " + pagina + " de ";
                float inicio = ancho - 36 - base.getWidthPoint("Página 99 de 99", 7.5f);
                texto(cb, paginaTexto, inicio, 26, 7.5f, false, ON_SURFACE_VARIANT, Element.ALIGN_LEFT);
                cb.addTemplate(total, inicio + base.getWidthPoint(paginaTexto, 7.5f), 26);
            } catch (DocumentException e) {
                throw new IllegalStateException(e);
            }
        }

        private void encabezadoGrande(PdfContentByte cb, float ancho, float alto) throws DocumentException {
            float x = 28, y = alto - 136, w = ancho - 56, h = 112;
            cb.setColorFill(INVERSE_SURFACE);
            cb.roundRectangle(x, y, w, h, 22);
            cb.fill();
            costura(cb, x + 7, y + 7, w - 14, h - 14, 16);

            float texto = x + 28;
            if (logo != null) {
                cb.setColorFill(Color.WHITE);
                cb.roundRectangle(x + 28, y + h / 2 - 22, 44, 44, 12);
                cb.fill();
                Image copia = Image.getInstance(logo);
                copia.setAbsolutePosition(x + 28 + (44 - copia.getScaledWidth()) / 2, y + h / 2 - 22 + (44 - copia.getScaledHeight()) / 2);
                cb.addImage(copia);
                texto = x + 86;
            }
            texto(cb, "FICHA TÉCNICA DE MÁQUINA", texto, y + h / 2 + 19, 7.5f, true, INVERSE_PRIMARY, Element.ALIGN_LEFT);
            texto(cb, "FIMACOR", texto, y + h / 2 - 3, 21, true, Color.WHITE, Element.ALIGN_LEFT);
            texto(cb, "Sistema de Control de Máquinas Textiles · CMTC – SENA", texto, y + h / 2 - 21, 8.5f, false, INVERSE_ON_SURFACE, Element.ALIGN_LEFT);

            BaseFont negrita = fuente(13, Font.BOLD, Color.WHITE).getCalculatedBaseFont(false);
            float anchoCodigo = negrita.getWidthPoint(codigo, 13) + 28;
            float xCodigo = x + w - 28 - anchoCodigo;
            cb.setColorFill(CHIP_OSCURO);
            cb.roundRectangle(xCodigo, y + h / 2 - 2, anchoCodigo, 26, 13);
            cb.fill();
            texto(cb, codigo, xCodigo + 14, y + h / 2 + 6, 13, true, Color.WHITE, Element.ALIGN_LEFT);
            texto(cb, "Generada el " + generado, x + w - 28, y + h / 2 - 18, 8, false, INVERSE_ON_SURFACE, Element.ALIGN_RIGHT);
        }

        private void encabezadoDelgado(PdfContentByte cb, float ancho, float alto) {
            float x = 28, y = alto - 64, w = ancho - 56, h = 44;
            cb.setColorFill(INVERSE_SURFACE);
            cb.roundRectangle(x, y, w, h, 14);
            cb.fill();
            costura(cb, x + 5, y + 5, w - 10, h - 10, 10);
            texto(cb, "FIMACOR  ·  Ficha técnica", x + 20, y + 17, 9.5f, true, Color.WHITE, Element.ALIGN_LEFT);
            texto(cb, codigo, x + w - 20, y + 17, 9.5f, true, INVERSE_PRIMARY, Element.ALIGN_RIGHT);
        }

        /** Linea punteada interior: la "costura" del hero de las pantallas del QR. */
        private static void costura(PdfContentByte cb, float x, float y, float w, float h, float radio) {
            cb.saveState();
            cb.setColorStroke(COSTURA);
            cb.setLineWidth(1.1f);
            cb.setLineDash(4, 3, 0);
            cb.roundRectangle(x, y, w, h, radio);
            cb.stroke();
            cb.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            BaseFont base = fuente(7.5f, Font.NORMAL, ON_SURFACE_VARIANT).getCalculatedBaseFont(false);
            total.beginText();
            total.setFontAndSize(base, 7.5f);
            total.setColorFill(ON_SURFACE_VARIANT);
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }

        private static void texto(PdfContentByte cb, String valor, float x, float y, float tamano, boolean negrita, Color color, int alineacion) {
            BaseFont base = fuente(tamano, negrita ? Font.BOLD : Font.NORMAL, color).getCalculatedBaseFont(false);
            cb.beginText();
            cb.setFontAndSize(base, tamano);
            cb.setColorFill(color);
            cb.showTextAligned(alineacion, valor, x, y, 0);
            cb.endText();
        }
    }
}
