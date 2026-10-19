package com.fimacor.importacion.service;

import java.util.Arrays;

/** Los cuatro formatos de importacion. El orden es el recomendado para cargarlos (cada uno depende del anterior). */
public enum TipoImportacion {
    USUARIOS("usuarios", "Usuarios", "group", "Usuarios",
            "Instructores, técnicos, cuentadantes y demás personas que van a usar el sistema.",
            "plantilla_usuarios.xlsx"),
    MAQUINAS("maquinas", "Máquinas", "precision_manufacturing", "Inventario",
            "El inventario de máquinas con su ubicación, estado, garantía y cuentadante.",
            "plantilla_maquinas.xlsx"),
    NOVEDADES("novedades", "Novedades", "report_problem", "Novedades",
            "Reportes de falla de las máquinas, abiertos o ya resueltos.",
            "plantilla_novedades.xlsx"),
    MANTENIMIENTOS("mantenimientos", "Mantenimientos", "build_circle", "Mantenimientos",
            "Mantenimientos programados, en curso o ya realizados.",
            "plantilla_mantenimientos.xlsx");

    public final String clave;
    public final String nombre;
    public final String icono;
    public final String hoja;
    public final String descripcion;
    public final String archivo;

    TipoImportacion(String clave, String nombre, String icono, String hoja, String descripcion, String archivo) {
        this.clave = clave;
        this.nombre = nombre;
        this.icono = icono;
        this.hoja = hoja;
        this.descripcion = descripcion;
        this.archivo = archivo;
    }

    // Thymeleaf lee propiedades por getter
    public String getClave() { return clave; }
    public String getNombre() { return nombre; }
    public String getIcono() { return icono; }
    public String getDescripcion() { return descripcion; }

    public static TipoImportacion de(String clave) {
        if (clave == null) return MAQUINAS;
        return Arrays.stream(values()).filter(t -> t.clave.equalsIgnoreCase(clave.trim())).findFirst().orElse(MAQUINAS);
    }

    public int orden() {
        return ordinal() + 1;
    }
}
