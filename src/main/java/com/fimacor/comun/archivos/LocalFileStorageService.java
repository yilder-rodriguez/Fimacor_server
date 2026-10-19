package com.fimacor.comun.archivos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Implementacion de desarrollo: guarda en un directorio local configurable
 * (por defecto ./fimacor-data/uploads, fuera del jar). En Railway este
 * directorio es efimero (se pierde en cada redeploy) - ver FileStorageService.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path raiz;

    public LocalFileStorageService(@Value("${fimacor.storage.local-dir:./fimacor-data/uploads}") String directorioBase) {
        this.raiz = Path.of(directorioBase).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String carpeta, String nombreArchivo, InputStream contenido) throws IOException {
        Path directorio = raiz.resolve(carpeta).normalize();
        Files.createDirectories(directorio);
        Path destino = directorio.resolve(nombreArchivo).normalize();
        if (!destino.startsWith(directorio)) {
            throw new IOException("Nombre de archivo invalido");
        }
        Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
        return nombreArchivo;
    }

    @Override
    public Resource cargar(String carpeta, String nombreArchivo) {
        Path ruta = raiz.resolve(carpeta).resolve(nombreArchivo).normalize();
        if (!ruta.startsWith(raiz) || !Files.isRegularFile(ruta)) return null;
        return new FileSystemResource(ruta);
    }

    @Override
    public boolean eliminar(String carpeta, String nombreArchivo) {
        Path ruta = raiz.resolve(carpeta).resolve(nombreArchivo).normalize();
        if (!ruta.startsWith(raiz)) return false;
        try {
            return Files.deleteIfExists(ruta);
        } catch (IOException e) {
            return false;
        }
    }
}
