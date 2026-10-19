package com.fimacor.manuales.service;

import com.fimacor.comun.archivos.FileStorageService;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.repository.FichaMaquinaRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import com.fimacor.manuales.entity.Manual;
import com.fimacor.manuales.entity.TipoManual;
import com.fimacor.manuales.repository.ManualRepository;
import com.fimacor.manuales.repository.TipoManualRepository;

/** Logica de negocio para subir, listar y administrar manuales. */
@Service
public class ManualesService {

    private static final String CARPETA = "manuales";

    private final ManualRepository manualRepository;
    private final TipoManualRepository tipoManualRepository;
    private final FichaMaquinaRepository fichaMaquinaRepository;
    private final FileStorageService fileStorageService;

    public ManualesService(ManualRepository manualRepository, TipoManualRepository tipoManualRepository,
            FichaMaquinaRepository fichaMaquinaRepository, FileStorageService fileStorageService) {
        this.manualRepository = manualRepository;
        this.tipoManualRepository = tipoManualRepository;
        this.fichaMaquinaRepository = fichaMaquinaRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<FichaMaquina> listarMaquinas() {
        return fichaMaquinaRepository.findByBajaFalseOrderByCodigoSenaAsc();
    }

    public List<TipoManual> listarTiposManual() {
        return tipoManualRepository.findAllByOrderByIdTiposManualesAsc();
    }

    public List<Manual> listarPorMaquina(int idMaquina) {
        return manualRepository.findAllByMaquina_IdMaquinasOrderByIdManualesDesc(idMaquina);
    }

    /** Resultado de un intento de registro: exito o motivo de error. */
    public record Resultado(boolean ok, String error) {
        static Resultado exito() {
            return new Resultado(true, null);
        }
        static Resultado fallo(String motivo) {
            return new Resultado(false, motivo);
        }
    }

    public Resultado registrar(int idMaquina, int idTipoManual, String descripcion, LocalDate fechaSubida,
            String fuente, String linkManual, String nombreArchivoOriginal, InputStream archivo) {
        FichaMaquina maquina = fichaMaquinaRepository.findById(idMaquina).orElse(null);
        TipoManual tipoManual = tipoManualRepository.findById(idTipoManual).orElse(null);
        if (maquina == null || maquina.isBaja() || tipoManual == null || fechaSubida == null) return Resultado.fallo("datos");

        boolean esLink = "link".equals(fuente);
        int idManual = siguienteId();
        String nombreManual;
        String rutaManual;

        if (esLink) {
            rutaManual = normalizarUrl(linkManual);
            if (rutaManual.isEmpty()) return Resultado.fallo("link");
            nombreManual = limitar(rutaManual, 45);
        } else {
            if (archivo == null || nombreArchivoOriginal == null || nombreArchivoOriginal.isBlank()) {
                return Resultado.fallo("archivo");
            }
            if (!nombreArchivoOriginal.toLowerCase().endsWith(".pdf")) {
                return Resultado.fallo("pdf");
            }
            nombreManual = "manual_" + idManual + ".pdf";
            try {
                fileStorageService.guardar(CARPETA, nombreManual, archivo);
            } catch (IOException e) {
                return Resultado.fallo("servidor");
            }
            rutaManual = nombreManual;
        }

        Manual manual = new Manual();
        manual.setIdManuales(idManual);
        manual.setNombreDelManual(nombreManual);
        manual.setDescripcionManual(limitar(descripcion, 45));
        manual.setFechaSubida(fechaSubida);
        manual.setFuenteManual(esLink ? "link" : "archivo");
        manual.setRutaManual(rutaManual);
        manual.setMaquina(maquina);
        manual.setTipoManual(tipoManual);
        manualRepository.save(manual);
        return Resultado.exito();
    }

    public boolean eliminar(int idManual) {
        Manual manual = manualRepository.findById(idManual).orElse(null);
        if (manual == null) return false;
        manualRepository.deleteById(idManual);
        if (!manual.esEnlace() && manual.getRutaManual() != null) {
            fileStorageService.eliminar(CARPETA, manual.getRutaManual());
        }
        return true;
    }

    public Resource cargarArchivo(String nombreArchivo) {
        return fileStorageService.cargar(CARPETA, nombreArchivo);
    }

    public Manual buscar(int idManual) {
        return manualRepository.findById(idManual).orElse(null);
    }

    private int siguienteId() {
        return manualRepository.findAll().stream().mapToInt(Manual::getIdManuales).max().orElse(0) + 1;
    }

    private String normalizarUrl(String url) {
        String limpio = url == null ? "" : url.trim();
        if (limpio.isEmpty()) return "";
        if (!limpio.matches("(?i)^https?://.+")) limpio = "https://" + limpio;
        return limpio;
    }

    private String limitar(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
