package com.fimacor.usuarios.util;

import com.fimacor.usuarios.entity.Permiso;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agrupa los permisos por modulo (prefijo del codigo, p. ej. "maquinas.ver" -> Maquinas) para que
 * la matriz de permisos siga ordenada aunque se creen permisos nuevos. Los prefijos en singular o
 * duplicados heredados (maquina/maquinas, novedad/novedades, reporte.falla) caen en el mismo modulo.
 */
public final class PermisoModulos {

    public record Modulo(String clave, String nombre, String icono, List<Permiso> permisos) {
    }

    /** Cuantos permisos de cada modulo tiene un rol (para las tarjetas de roles). */
    public record ModuloResumen(String nombre, String icono, int asignados, int total) {
    }

    private record Definicion(String nombre, String icono) {
    }

    private static final Map<String, Definicion> MODULOS = new LinkedHashMap<>();

    static {
        MODULOS.put("acceso", new Definicion("Usuarios y acceso", "bi-people"));
        MODULOS.put("maquinas", new Definicion("Máquinas", "bi-gear-wide-connected"));
        MODULOS.put("mantenimiento", new Definicion("Mantenimiento", "bi-tools"));
        MODULOS.put("reparacion", new Definicion("Reparaciones", "bi-wrench-adjustable"));
        MODULOS.put("novedades", new Definicion("Novedades y fallas", "bi-exclamation-triangle"));
        MODULOS.put("reubicacion", new Definicion("Reubicación", "bi-arrow-left-right"));
        MODULOS.put("manuales", new Definicion("Guía y manuales", "bi-journal-text"));
        MODULOS.put("reportes", new Definicion("Reportes", "bi-bar-chart-line"));
        MODULOS.put("configuracion", new Definicion("Configuración", "bi-sliders"));
        MODULOS.put("perfil", new Definicion("Perfil", "bi-person-circle"));
        MODULOS.put("otros", new Definicion("Otros permisos", "bi-three-dots"));
    }

    private PermisoModulos() {
    }

    public static List<Modulo> agrupar(List<Permiso> permisos) {
        Map<String, List<Permiso>> grupos = new LinkedHashMap<>();
        MODULOS.keySet().forEach(clave -> grupos.put(clave, new ArrayList<>()));
        for (Permiso permiso : permisos) {
            grupos.get(modulo(permiso.getCodigoPermiso())).add(permiso);
        }
        List<Modulo> resultado = new ArrayList<>();
        grupos.forEach((clave, lista) -> {
            if (!lista.isEmpty()) {
                Definicion d = MODULOS.get(clave);
                resultado.add(new Modulo(clave, d.nombre(), d.icono(), lista));
            }
        });
        return resultado;
    }

    public static List<ModuloResumen> resumen(List<Modulo> modulos, Set<Integer> asignados) {
        List<ModuloResumen> resultado = new ArrayList<>();
        for (Modulo modulo : modulos) {
            int marcados = (int) modulo.permisos().stream()
                    .filter(p -> asignados != null && asignados.contains(p.getIdPermiso())).count();
            if (marcados > 0) {
                resultado.add(new ModuloResumen(modulo.nombre(), modulo.icono(), marcados, modulo.permisos().size()));
            }
        }
        return resultado;
    }

    static String modulo(String codigo) {
        String c = codigo == null ? "" : Normalizer.normalize(codigo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().trim();
        String prefijo = c.contains(".") ? c.substring(0, c.indexOf('.')) : c;
        if (c.endsWith(".falla")) return "novedades";
        return switch (prefijo) {
            case "usuarios", "usuario", "roles", "rol", "permisos", "permiso" -> "acceso";
            case "maquinas", "maquina" -> "maquinas";
            case "mantenimiento", "mantenimientos" -> "mantenimiento";
            case "reparacion", "reparaciones" -> "reparacion";
            case "novedades", "novedad" -> "novedades";
            case "reubicacion", "reubicaciones" -> "reubicacion";
            case "manuales", "manual" -> "manuales";
            case "reportes", "reporte" -> "reportes";
            case "configuracion" -> "configuracion";
            case "perfil" -> "perfil";
            default -> "otros";
        };
    }
}
