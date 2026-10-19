package com.fimacor.maquinas.util;

import java.text.Normalizer;

/** Helpers de estado y texto para el listado de maquinas. */
public final class MaquinaTexto {

    private MaquinaTexto() {
    }

    public static boolean requiereReparacion(String estado) {
        String n = normalizar(estado);
        return n.contains("mantenimiento") || n.contains("dan") || n.contains("falla")
                || n.contains("critic") || n.contains("fuera de servicio") || n.contains("fuera servicio");
    }

    public static String claseEstado(String nombreEstado) {
        String n = normalizar(nombreEstado);
        if (n.contains("fuera de servicio") || n.contains("fuera servicio")) {
            return "fueraserviciomaquinasregistradas badge rounded-pill bg-danger-subtle text-danger-emphasis border border-danger-subtle";
        }
        if (n.contains("mantenimiento")) {
            return "mantenimientomaquinasregistradas badge rounded-pill bg-warning-subtle text-warning-emphasis border border-warning-subtle";
        }
        if (n.contains("dan")) return "danadamaquinasregistradas badge rounded-pill bg-danger-subtle text-danger-emphasis border border-danger-subtle";
        if (n.contains("obsolet")) return "obsoletamaquinasregistradas badge rounded-pill bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle";
        return "operativamaquinasregistradas badge rounded-pill bg-success-subtle text-success-emphasis border border-success-subtle";
    }

    /** Color del chip de estado en las pantallas Material del QR. */
    public static String claseChipMaterial(String nombreEstado) {
        String n = normalizar(nombreEstado);
        if (n.contains("fuera de servicio") || n.contains("fuera servicio") || n.contains("dan")) return "md-chip--error";
        if (n.contains("mantenimiento")) return "md-chip--warning";
        if (n.contains("obsolet") || n.isBlank()) return "md-chip--neutral";
        return "md-chip--success";
    }

    private static String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
