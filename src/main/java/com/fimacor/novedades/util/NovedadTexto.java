package com.fimacor.novedades.util;

/** Helpers de texto para las novedades. */
public final class NovedadTexto {

    private NovedadTexto() {
    }

    public static String textoCorto(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo - 3) + "...";
    }

    /** Compatibilidad con filas antiguas que traen "| Detalle:" embebido en la descripcion. */
    public static String detalle(String valor) {
        if (valor == null) return "";
        String limpio = valor.trim();
        int posDetalle = limpio.toLowerCase().indexOf("| detalle:");
        if (posDetalle >= 0) {
            limpio = limpio.substring(posDetalle + "| detalle:".length()).trim();
        }
        return limpio;
    }

    public static String estadoTexto(String estado) {
        String limpio = estado == null || estado.trim().isEmpty() ? "REPORTADA" : estado.trim();
        return switch (limpio) {
            case "REPORTADA" -> "Reportada";
            case "ASIGNADA" -> "Asignada";
            case "EN_REPARACION" -> "En reparación";
            case "FINALIZADA_TECNICO" -> "Finalizada por técnico";
            case "CERRADA" -> "Cerrada";
            case "CANCELADA" -> "Cancelada";
            case "DEVUELTA" -> "Devuelta";
            default -> limpio;
        };
    }

    public static String estadoClase(String estado) {
        String limpio = estado == null || estado.trim().isEmpty() ? "REPORTADA" : estado.trim().toLowerCase().replace('_', '-');
        String color = switch (limpio) {
            case "asignada" -> "bg-primary-subtle text-primary-emphasis border border-primary-subtle";
            case "en-reparacion" -> "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
            case "finalizada-tecnico", "cerrada" -> "bg-success-subtle text-success-emphasis border border-success-subtle";
            case "cancelada" -> "bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle";
            case "devuelta" -> "bg-info-subtle text-info-emphasis border border-info-subtle";
            default -> "bg-danger-subtle text-danger-emphasis border border-danger-subtle";
        };
        return "novedad-estado-badge novedad-estado-" + limpio + " badge rounded-pill " + color;
    }
}
