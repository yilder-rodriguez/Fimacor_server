package com.fimacor.comun.menu;

import com.fimacor.maquinas.entity.FichaMaquina;
import java.text.Normalizer;
import java.util.Map;

/** Helpers de texto y prioridad para las notificaciones del panel principal. */
public final class MenuTexto {

    private MenuTexto() {
    }

    /** Enlaces de notificaciones guardados desde antes de la migracion, todavia
     * en formato JSP (ver web/*.jsp del sistema original). */
    private static final Map<String, String> RUTAS_JSP_LEGADO = Map.ofEntries(
            Map.entry("menu.jsp", "/menu"),
            Map.entry("maquinas_registradas.jsp", "/maquinas"),
            Map.entry("administracion_maquinas.jsp", "/maquinas/administracion"),
            Map.entry("registro_maquina.jsp", "/maquinas/registrar"),
            Map.entry("asignar_maquinas.jsp", "/maquinas/asignar"),
            Map.entry("mis_maquinas_asignadas.jsp", "/maquinas/mis-asignadas"),
            Map.entry("mantenimientos_preventivos.jsp", "/mantenimiento/programar"),
            Map.entry("administracion_mantenimiento.jsp", "/mantenimiento/administracion"),
            Map.entry("historial_mantenimiento.jsp", "/mantenimiento/historial"),
            Map.entry("reubicacion_maquina.jsp", "/reubicacion"),
            Map.entry("reporte_falla.jsp", "/novedades"),
            Map.entry("tabla_usuario.jsp", "/usuarios"),
            Map.entry("manuales.jsp", "/manuales"),
            Map.entry("roles.jsp", "/roles"),
            Map.entry("permisos.jsp", "/permisos"),
            Map.entry("configuracion.jsp", "/configuracion"),
            Map.entry("login.jsp", "/login"),
            Map.entry("registro_usuario.jsp", "/registro"),
            Map.entry("verificar_registro.jsp", "/registro/verificar"),
            Map.entry("olvide_contrasena.jsp", "/recuperar"),
            Map.entry("verificar_codigo.jsp", "/recuperar/verificar"));

    /** Convierte un enlace guardado en el formato JSP viejo (Menu.jsp?x=1,
     * /FIMACOR_PROYECTO_NETBEANS/Configuracion.jsp?x=1) al equivalente actual en
     * Spring Boot. Hace falta porque hay notificaciones que quedaron guardadas en
     * la base de datos desde antes de la migracion. Si el enlace ya viene en
     * formato nuevo (no contiene ".jsp") se devuelve tal cual. */
    public static String normalizarEnlace(String enlace) {
        if (enlace == null || enlace.isBlank()) return enlace;
        int puntoJsp = enlace.toLowerCase().indexOf(".jsp");
        if (puntoJsp < 0) return enlace;

        int inicioArchivo = enlace.lastIndexOf('/', puntoJsp) + 1;
        String archivo = enlace.substring(inicioArchivo, puntoJsp + 4).toLowerCase();
        String resto = enlace.substring(puntoJsp + 4);

        String rutaNueva = RUTAS_JSP_LEGADO.get(archivo);
        return rutaNueva != null ? rutaNueva + resto : "/menu";
    }

    public static String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    public static boolean esEstadoActivo(String nombre, String descripcion) {
        String texto = normalizar((nombre != null ? nombre : "") + " " + (descripcion != null ? descripcion : ""));
        boolean fueraDeServicio = texto.contains("fuera") && texto.contains("servicio");
        return !fueraDeServicio && (texto.contains("activo") || texto.contains("operativ")
                || texto.contains("disponible") || texto.contains("funcional") || texto.contains("en servicio"));
    }

    public static boolean esEstadoCritico(String nombre, String descripcion) {
        String texto = normalizar((nombre != null ? nombre : "") + " " + (descripcion != null ? descripcion : ""));
        return texto.contains("dan") || texto.contains("aver") || texto.contains("fall")
                || texto.contains("mantenimiento") || texto.contains("fuera");
    }

    public static String iniciales(String nombre, String apellido) {
        String n = nombre != null && !nombre.trim().isEmpty() ? nombre.trim().substring(0, 1) : "U";
        String a = apellido != null && !apellido.trim().isEmpty() ? apellido.trim().substring(0, 1) : "";
        return (n + a).toUpperCase();
    }

    public static String valorTexto(String valor, String defecto) {
        return valor == null || valor.trim().isEmpty() ? defecto : valor;
    }

    public static String referenciaMaquina(FichaMaquina maquina) {
        if (maquina == null) return "Máquina sin código";
        String codigo = maquina.getCodigoSena() != null && !maquina.getCodigoSena().trim().isEmpty()
                ? maquina.getCodigoSena() : "Sin código";
        return codigo + " - " + maquina.getDescripcion();
    }

    public static String resumenHistorial(String descripcion) {
        if (descripcion == null) return "Sin detalle registrado";
        return descripcion.replace("ESTADO: ASIGNADO |", "")
                .replace("ESTADO: ASIGNADO", "")
                .replace("| ESTADO: REPARADO", "")
                .replace("ESTADO: REPARADO", "")
                .replace("EN SERVICIO | DATOS:", "Datos:")
                .replace("| DATOS:", "Datos:")
                .trim();
    }

    public static int prioridad(String tipo, String titulo) {
        if (tipo == null) return 9;
        String tituloNorm = normalizar(titulo);
        if ("red".equals(tipo)) return 1;
        if ("warn".equals(tipo) && (tituloNorm.contains("asign") || tituloNorm.contains("mantenimiento"))) return 2;
        if ("warn".equals(tipo)) return 3;
        if ("admin".equals(tipo)) return 4;
        return 5;
    }

    public static String grupo(int prioridad) {
        if (prioridad == 1) return "Críticas";
        if (prioridad <= 3) return "Pendientes";
        if (prioridad == 4) return "Administración";
        return "Informativas";
    }

    public static String idNotificacion(String enlace, String titulo, String mensaje, String meta) {
        String base = enlace + "|" + titulo + "|" + mensaje + "|" + meta;
        return Integer.toHexString(base.hashCode());
    }

    public static String reubicacionIdDesdeEnlace(String enlace) {
        if (enlace == null) return "";
        int marca = enlace.indexOf("solicitudReubicacion=");
        if (marca < 0) return "";
        String resto = enlace.substring(marca + "solicitudReubicacion=".length());
        return resto.replaceAll("[^0-9].*$", "");
    }
}
