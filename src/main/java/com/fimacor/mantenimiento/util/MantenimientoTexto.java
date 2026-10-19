package com.fimacor.mantenimiento.util;

import com.fimacor.usuarios.entity.Usuario;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import com.fimacor.mantenimiento.entity.HistorialMantenimiento;
import com.fimacor.mantenimiento.entity.Mantenimiento;

/** Helpers de fecha y estado para mantenimientos e historial. */
public final class MantenimientoTexto {

    private MantenimientoTexto() {
    }

    /** Nombres de archivo de evidencia, sin entradas en blanco (el campo guarda rutas separadas por ";"). */
    public static List<String> evidencias(Mantenimiento m) {
        List<String> resultado = new ArrayList<>();
        if (m == null || m.getEvidenciaMantenimiento() == null) return resultado;
        for (String ruta : m.getEvidenciaMantenimiento().split(";")) {
            if (ruta != null && !ruta.isBlank()) resultado.add(ruta.trim());
        }
        return resultado;
    }

    public static boolean estaReparado(HistorialMantenimiento h) {
        String d = h != null ? h.getDescripcionAccion() : null;
        return d != null && (d.contains("ESTADO: FINALIZADO") || d.contains("ESTADO: REPARADO"));
    }

    public static boolean estaAsignadoHistorial(HistorialMantenimiento h) {
        String d = h != null ? h.getDescripcionAccion() : null;
        return d != null && d.contains("ESTADO: ASIGNADO") && !d.contains("ESTADO: REPARADO");
    }

    public static boolean enMantenimiento(HistorialMantenimiento h) {
        String d = h != null ? h.getDescripcionAccion() : null;
        return d != null && d.contains("ESTADO: EN MANTENIMIENTO");
    }

    /** Mantenimiento que aun no termina: asignado a un tecnico o en proceso, sin reparar ni cerrar. */
    public static boolean enCurso(HistorialMantenimiento h) {
        String d = h != null ? h.getDescripcionAccion() : null;
        return d != null && (estaAsignadoHistorial(h) || enMantenimiento(h)) && !estaReparado(h) && !d.contains("ESTADO: CERRADO");
    }

    public static boolean finalizado(HistorialMantenimiento h) {
        String d = h != null ? h.getDescripcionAccion() : null;
        return d != null && d.contains("ESTADO: FINALIZADO");
    }

    public static String estadoHistorialTexto(HistorialMantenimiento h) {
        String d = h != null ? h.getDescripcionAccion() : null;
        if (d != null && d.contains("ESTADO: CERRADO")) return "Cerrado";
        if (d != null && d.contains("ESTADO: EN MANTENIMIENTO")) return "En mantenimiento";
        if (estaReparado(h)) return "Reparado";
        if (estaAsignadoHistorial(h)) return "Asignado";
        return "Registrado";
    }

    public static String estadoHistorialClase(HistorialMantenimiento h) {
        if (estaReparado(h)) return "estado-reparado badge rounded-pill bg-success-subtle text-success-emphasis border border-success-subtle";
        if (estaAsignadoHistorial(h)) return "estado-asignado badge rounded-pill bg-primary-subtle text-primary-emphasis border border-primary-subtle";
        return "estado-registrado badge rounded-pill bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle";
    }

    public static int pasoHistorial(HistorialMantenimiento h) {
        String d = h != null ? h.getDescripcionAccion() : null;
        if (d != null && d.contains("ESTADO: CERRADO")) return 5;
        if (d != null && d.contains("ESTADO: EN MANTENIMIENTO")) return 3;
        if (estaReparado(h)) return 4;
        if (estaAsignadoHistorial(h)) return 2;
        return 1;
    }

    public static String limpiarDescripcion(HistorialMantenimiento h) {
        String d = h != null ? h.getDescripcionAccion() : null;
        if (d == null) return "";
        return d.replace("ESTADO: ASIGNADO |", "").replace("| ESTADO: REPARADO", "").trim();
    }

    public static String referenciaMaquina(HistorialMantenimiento h) {
        if (h == null) return "Máquina sin registro";
        if (h.getMaquina() != null) {
            String codigo = h.getMaquina().getCodigoSena();
            codigo = codigo != null && !codigo.isBlank() ? codigo : "Sin código";
            return codigo + " - " + h.getMaquina().getDescripcion();
        }
        return h.getTipoMaquina() != null ? h.getTipoMaquina() : "Máquina sin registro";
    }

    public static boolean asignadoAlUsuario(HistorialMantenimiento h, Usuario usuario) {
        if (h == null || usuario == null) return false;
        if (h.getTecnico() != null && usuario.getIdUsuario() != null && usuario.getIdUsuario().equals(h.getTecnico().getIdUsuario())) return true;
        String tecnicoHistorial = normalizar(h.getTecnicoEncargado());
        String nombreSesion = normalizar((usuario.getNombre() != null ? usuario.getNombre() : "")
                + " " + (usuario.getApellido() != null ? usuario.getApellido() : ""));
        return !tecnicoHistorial.isEmpty() && tecnicoHistorial.equals(nombreSesion.trim());
    }

    private static String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    public static boolean vencido(Mantenimiento m, LocalDate hoy) {
        return m != null && m.getFechaProximoMantenimiento() != null && m.getFechaProximoMantenimiento().isBefore(hoy);
    }

    public static boolean proximo(Mantenimiento m, LocalDate hoy) {
        if (m == null || m.getFechaProximoMantenimiento() == null) return false;
        long dias = ChronoUnit.DAYS.between(hoy, m.getFechaProximoMantenimiento());
        return dias >= 0 && dias <= 7;
    }

    public static boolean pendiente(Mantenimiento m, LocalDate hoy) {
        if (m == null || m.getFechaProximoMantenimiento() == null) return true;
        return !m.getFechaProximoMantenimiento().isBefore(hoy);
    }

    public static String estado(Mantenimiento m, LocalDate hoy) {
        if (vencido(m, hoy)) return "Vencido";
        if (proximo(m, hoy)) return "Próximo";
        return "Programado";
    }

    public static String estadoClase(Mantenimiento m, LocalDate hoy) {
        if (vencido(m, hoy)) return "estado-vencido badge rounded-pill bg-danger-subtle text-danger-emphasis border border-danger-subtle";
        if (proximo(m, hoy)) return "estado-proximo badge rounded-pill bg-warning-subtle text-warning-emphasis border border-warning-subtle";
        return "estado-asignado badge rounded-pill bg-success-subtle text-success-emphasis border border-success-subtle";
    }

    public static String diasRestantes(Mantenimiento m, LocalDate hoy) {
        if (m == null || m.getFechaProximoMantenimiento() == null) return "Sin fecha";
        long dias = ChronoUnit.DAYS.between(hoy, m.getFechaProximoMantenimiento());
        if (dias < 0) return "Vencido hace " + Math.abs(dias) + " día(s)";
        if (dias == 0) return "Hoy";
        return "Faltan " + dias + " día(s)";
    }
}
