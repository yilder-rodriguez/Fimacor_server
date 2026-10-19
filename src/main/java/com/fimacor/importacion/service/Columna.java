package com.fimacor.importacion.service;

/**
 * Una columna de una plantilla. El titulo es el nombre exacto que lleva en el Excel; los alias son otros nombres que el
 * importador tambien entiende (por si traen su propio archivo). La lista dice de donde salen las opciones del desplegable.
 */
record Columna(String campo, String titulo, boolean obligatoria, int ancho, Formato formato, String lista, boolean estricta,
               String ayuda, String ejemplo1, String ejemplo2, String... alias) {

    enum Formato { TEXTO, FECHA, FECHA_HORA, NUMERO }

    static Columna texto(String campo, String titulo, boolean obligatoria, int ancho, String ayuda, String ej1, String ej2, String... alias) {
        return new Columna(campo, titulo, obligatoria, ancho, Formato.TEXTO, null, false, ayuda, ej1, ej2, alias);
    }

    static Columna fecha(String campo, String titulo, boolean obligatoria, String ayuda, String ej1, String ej2, String... alias) {
        return new Columna(campo, titulo, obligatoria, 18, Formato.FECHA, null, false, ayuda, ej1, ej2, alias);
    }

    static Columna fechaHora(String campo, String titulo, boolean obligatoria, String ayuda, String ej1, String ej2, String... alias) {
        return new Columna(campo, titulo, obligatoria, 20, Formato.FECHA_HORA, null, false, ayuda, ej1, ej2, alias);
    }

    static Columna lista(String campo, String titulo, boolean obligatoria, int ancho, String lista, boolean estricta, String ayuda, String ej1, String ej2, String... alias) {
        return new Columna(campo, titulo, obligatoria, ancho, Formato.TEXTO, lista, estricta, ayuda, ej1, ej2, alias);
    }

    static Columna numero(String campo, String titulo, boolean obligatoria, String ayuda, String ej1, String ej2, String... alias) {
        return new Columna(campo, titulo, obligatoria, 16, Formato.NUMERO, null, false, ayuda, ej1, ej2, alias);
    }
}
