package com.fimacor.comun.archivos;

import java.io.InputStream;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * Abstrae donde viven los archivos subidos (manuales, evidencias...), para
 * que no dependan del ciclo de vida del contenedor. Hay una implementacion
 * local para desarrollo (LocalFileStorageService) y queda pendiente una
 * compatible con S3 (Cloudflare R2) para produccion.
 */
public interface FileStorageService {

    /** Guarda el archivo bajo una "carpeta" logica (ej. "manuales") y devuelve la clave para recuperarlo despues. */
    String guardar(String carpeta, String nombreArchivo, InputStream contenido) throws java.io.IOException;

    Resource cargar(String carpeta, String nombreArchivo);

    boolean eliminar(String carpeta, String nombreArchivo);

    /**
     * Tipo de contenido real segun la extension del archivo. Sin esto el
     * navegador recibe application/octet-stream y descarga la evidencia en
     * vez de mostrarla como vista previa, aunque el header diga "inline".
     */
    static MediaType tipoContenido(String nombreArchivo) {
        String nombre = nombreArchivo == null ? "" : nombreArchivo.toLowerCase();
        if (nombre.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (nombre.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (nombre.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (nombre.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
