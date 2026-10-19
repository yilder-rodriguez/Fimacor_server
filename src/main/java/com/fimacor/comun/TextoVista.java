package com.fimacor.comun;

import java.text.Normalizer;

/** Utilidades de texto compartidas por las vistas. */
public final class TextoVista {

    private TextoVista() {
    }

    public static String acentuarCatalogo(String valor) {
        if (valor == null) return "";

        String texto = valor.trim();
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();

        if ("tecnico".equals(normalizado)) return "Técnico";
        if ("tecnico profesional".equals(normalizado)) return "Técnico profesional";
        if ("tecnico pasante".equals(normalizado)) return "Técnico pasante";
        if ("logistica".equals(normalizado)) return "Logística";
        if ("gestion".equals(normalizado)) return "Gestión";
        if ("operacion".equals(normalizado)) return "Operación";
        if ("administracion".equals(normalizado)) return "Administración";
        if ("programacion".equals(normalizado)) return "Programación";

        return texto;
    }
}
