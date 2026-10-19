package com.fimacor.reubicacion.util;

/** Helpers de texto para el estado de una reubicacion. */
public final class ReubicacionTexto {

    private ReubicacionTexto() {
    }

    public static String estadoTexto(String estado) {
        if (estado == null || estado.trim().isEmpty()) return "Pendiente de aprobación";
        String limpio = estado.trim();
        if ("Aceptada".equalsIgnoreCase(limpio) || "Aceptado".equalsIgnoreCase(limpio)
                || "Aprobada".equalsIgnoreCase(limpio) || "Aprobado".equalsIgnoreCase(limpio)
                || "Ejecutado".equalsIgnoreCase(limpio)) {
            return "Aprobado";
        }
        if ("Rechazada".equalsIgnoreCase(limpio) || "Rechazado".equalsIgnoreCase(limpio)) return "Cancelado";
        if ("Pendiente".equalsIgnoreCase(limpio)) return "Pendiente de aprobación";
        if ("En proceso interno".equalsIgnoreCase(limpio)) return "Aplicando traslado interno";
        return limpio;
    }

    public static String estadoClase(String estado) {
        String texto = estadoTexto(estado).toLowerCase();
        if (texto.contains("aprob")) return "estado-aprobado badge rounded-pill bg-success-subtle text-success-emphasis border border-success-subtle";
        if (texto.contains("cancel") || texto.contains("rechaz")) return "estado-cancelado badge rounded-pill bg-danger-subtle text-danger-emphasis border border-danger-subtle";
        return "estado-pendiente badge rounded-pill bg-warning-subtle text-warning-emphasis border border-warning-subtle";
    }

    public static boolean tieneActa(String estado) {
        if (estado == null) return false;
        String limpio = estado.trim();
        return "Ejecutado".equalsIgnoreCase(limpio) || "Aprobado".equalsIgnoreCase(limpio)
                || "Aceptado".equalsIgnoreCase(limpio) || "Aceptada".equalsIgnoreCase(limpio);
    }
}
