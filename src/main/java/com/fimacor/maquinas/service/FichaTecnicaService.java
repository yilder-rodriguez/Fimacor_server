package com.fimacor.maquinas.service;

import com.fimacor.mantenimiento.entity.HistorialMantenimiento;
import com.fimacor.mantenimiento.entity.Mantenimiento;
import com.fimacor.mantenimiento.repository.HistorialMantenimientoRepository;
import com.fimacor.mantenimiento.repository.MantenimientoRepository;
import com.fimacor.manuales.entity.Manual;
import com.fimacor.manuales.repository.ManualRepository;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.repository.FichaMaquinaRepository;
import com.fimacor.novedades.entity.Novedad;
import com.fimacor.novedades.repository.NovedadRepository;
import com.fimacor.reubicacion.entity.ReubicacionMaquina;
import com.fimacor.reubicacion.repository.ReubicacionMaquinaRepository;
import com.fimacor.usuarios.entity.Usuario;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arma la ficha tecnica en PDF de una maquina: datos principales, ubicacion, compra y garantia,
 * reubicacion vigente, mantenimientos, novedades, historial y manuales. El diseno (encabezado de
 * marca, tarjetas, tablas y etiquetas de estado) lo dibuja FichaTecnicaPdf con OpenPDF.
 */
@Service
public class FichaTecnicaService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FichaMaquinaRepository fichaMaquinaRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final HistorialMantenimientoRepository historialMantenimientoRepository;
    private final NovedadRepository novedadRepository;
    private final ManualRepository manualRepository;
    private final ReubicacionMaquinaRepository reubicacionMaquinaRepository;

    public FichaTecnicaService(FichaMaquinaRepository fichaMaquinaRepository, MantenimientoRepository mantenimientoRepository,
            HistorialMantenimientoRepository historialMantenimientoRepository, NovedadRepository novedadRepository,
            ManualRepository manualRepository, ReubicacionMaquinaRepository reubicacionMaquinaRepository) {
        this.fichaMaquinaRepository = fichaMaquinaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.historialMantenimientoRepository = historialMantenimientoRepository;
        this.novedadRepository = novedadRepository;
        this.manualRepository = manualRepository;
        this.reubicacionMaquinaRepository = reubicacionMaquinaRepository;
    }

    public Optional<FichaMaquina> buscar(int idMaquina) {
        return fichaMaquinaRepository.findById(idMaquina);
    }

    // En transaccion: con open-in-view apagado, el tecnico de cada novedad/mantenimiento (carga perezosa) se leia sin sesion y
    // la ficha (la que abre el QR) fallaba con error 500 en cuanto la maquina tenia una novedad con tecnico asignado.
    @Transactional(readOnly = true)
    public byte[] generarPdf(FichaMaquina maquina) {
        String codigo = valor(maquina.getCodigoSena(), "Sin código");
        FichaTecnicaPdf pdf = new FichaTecnicaPdf(codigo, LocalDateTime.now(ZoneId.of("America/Bogota")).format(FECHA_HORA));

        String estado = maquina.getEstado() != null ? valor(maquina.getEstado().getNombreTipo(), "Sin estado") : "Sin estado";
        List<FichaTecnicaPdf.Etiqueta> etiquetas = new ArrayList<>();
        etiquetas.add(new FichaTecnicaPdf.Etiqueta(estado, FichaTecnicaPdf.tono(estado)));
        if (maquina.isBaja()) etiquetas.add(new FichaTecnicaPdf.Etiqueta("Dada de baja", FichaTecnicaPdf.TONO_ROJO));
        if (maquina.isTieneGarantia() && maquina.getFechaGarantia() != null) {
            boolean vigente = !maquina.getFechaGarantia().isBefore(LocalDate.now());
            etiquetas.add(new FichaTecnicaPdf.Etiqueta(vigente ? "Garantía vigente" : "Garantía vencida",
                    vigente ? FichaTecnicaPdf.TONO_VERDE : FichaTecnicaPdf.TONO_GRIS));
        }
        pdf.titulo(valor(maquina.getDescripcion(), "Máquina sin descripción"), etiquetas);

        if (maquina.isBaja()) {
            pdf.aviso("Máquina dada de baja",
                    "Fecha: " + (maquina.getFechaBaja() != null ? maquina.getFechaBaja().format(FECHA_HORA) : "Sin fecha")
                    + "   ·   Responsable: " + valor(maquina.getBajaPor(), "Sin registro")
                    + "\nMotivo: " + valor(maquina.getMotivoBaja(), "Sin motivo"), FichaTecnicaPdf.TONO_ROJO);
        }

        pdf.tarjeta("Datos principales", List.of(
                new FichaTecnicaPdf.Dato("Código SENA", codigo),
                new FichaTecnicaPdf.Dato("Tipo", maquina.getTipoMaquina() != null ? maquina.getTipoMaquina().getDescripcionMaquina() : "Sin tipo"),
                new FichaTecnicaPdf.Dato("Marca", maquina.getMarca() != null ? maquina.getMarca().getDescripcionMarca() : "Sin marca"),
                new FichaTecnicaPdf.Dato("Modelo", maquina.getModelo() != null ? maquina.getModelo().getDescripcionModelo() : "Sin modelo"),
                new FichaTecnicaPdf.Dato("Estado", estado),
                new FichaTecnicaPdf.Dato("Descripción del estado", maquina.getEstado() != null
                        ? valor(maquina.getEstado().getDescripcionEstado(), "Sin descripción") : "Sin descripción")), "");

        pdf.tarjeta("Ubicación", List.of(
                new FichaTecnicaPdf.Dato("Sede", maquina.getSede() != null ? maquina.getSede().getNombreSede() : "Sin sede"),
                new FichaTecnicaPdf.Dato("Área", valor(maquina.getArea(), "Sin área")),
                new FichaTecnicaPdf.Dato("Ambiente", maquina.getAmbiente() != null ? maquina.getAmbiente().getDescripcionAmbiente() : "Sin ambiente"),
                new FichaTecnicaPdf.Dato("Fecha de ubicación", fecha(maquina.getFechaUbicacionAmbiente()))), "");

        pdf.tarjeta("Compra y garantía", List.of(
                new FichaTecnicaPdf.Dato("Fecha de compra", fecha(maquina.getFechaCompra())),
                new FichaTecnicaPdf.Dato("Valor de la máquina", maquina.getValorMaquina() != null
                        ? "$ " + NumberFormat.getIntegerInstance(new Locale("es", "CO")).format(maquina.getValorMaquina()) : "Sin valor"),
                new FichaTecnicaPdf.Dato("Garantía", maquina.isTieneGarantia() ? "Sí" : "No"),
                new FichaTecnicaPdf.Dato("Vence la garantía", maquina.isTieneGarantia() ? fecha(maquina.getFechaGarantia()) : "No aplica")), "");

        ReubicacionMaquina reubicacion = maquina.getReubicacionId() != null
                ? reubicacionMaquinaRepository.findById(maquina.getReubicacionId()).orElse(null) : null;
        pdf.tarjeta("Reubicación vigente", reubicacion == null ? List.of() : List.of(
                new FichaTecnicaPdf.Dato("Fecha", valor(reubicacion.getFecha(), "Sin fecha")),
                new FichaTecnicaPdf.Dato("Tipo de traslado", valor(reubicacion.getTipoTraslado(), "Interno")),
                new FichaTecnicaPdf.Dato("Origen", valor(reubicacion.getOrigenTexto(), "Sin origen")),
                new FichaTecnicaPdf.Dato("Destino", valor(reubicacion.getDestinoTexto(), "Sin destino")),
                new FichaTecnicaPdf.Dato("Autorizado por", valor(reubicacion.getAutorizadoPor(), "Sin responsable")),
                new FichaTecnicaPdf.Dato("Observación", valor(reubicacion.getObservacion(), "Sin observación"))),
                "Esta máquina no tiene una reubicación registrada.");

        List<String[]> filasMantenimiento = new ArrayList<>();
        for (Mantenimiento m : mantenimientoRepository.findByMaquina_IdMaquinasOrderByIdMantenimientoDesc(maquina.getIdMaquinas())) {
            filasMantenimiento.add(new String[] {
                    fecha(m.getFechaMantenimiento()), fecha(m.getFechaProximoMantenimiento()),
                    m.getTipoMantenimiento() != null ? valor(m.getTipoMantenimiento().getDescripcionMantenimiento(), "Sin tipo") : "Sin tipo",
                    valor(m.getFrecuencia(), "Sin frecuencia"), persona(m.getTecnico(), "Sin técnico"), valor(m.getTareas(), "Sin tareas")});
        }
        pdf.tabla(new FichaTecnicaPdf.Tabla("Mantenimientos programados", "Sin mantenimientos programados.",
                new String[] {"Fecha", "Próximo", "Tipo", "Frecuencia", "Técnico", "Tareas"},
                new float[] {62, 62, 70, 62, 80, 150}, filasMantenimiento, -1));

        List<String[]> filasNovedades = new ArrayList<>();
        for (Novedad n : novedadRepository.findByMaquina_IdMaquinasOrderByIdNovedadesDesc(maquina.getIdMaquinas())) {
            filasNovedades.add(new String[] {
                    fecha(n.getFechaNovedad()), valor(n.getTipoFalla(), "Sin tipo"), valor(n.getDescripcionNovedad(), "Sin descripción"),
                    persona(n.getTecnicoAsignado(), "Sin asignar"), estadoNovedad(n.getEstadoFlujo())});
        }
        pdf.tabla(new FichaTecnicaPdf.Tabla("Novedades", "Sin novedades registradas.",
                new String[] {"Fecha", "Tipo de falla", "Descripción", "Técnico", "Estado"},
                new float[] {62, 72, 190, 90, 80}, filasNovedades, 4));

        List<String[]> filasHistorial = new ArrayList<>();
        for (HistorialMantenimiento h : historialMantenimientoRepository.findByMaquina_IdMaquinasOrderByIdHistorialMantenimientoDesc(maquina.getIdMaquinas())) {
            filasHistorial.add(new String[] {
                    valor(h.getFechaInicio(), "Sin inicio"), valor(h.getFechaFinal(), "Sin final"),
                    valor(h.getTecnicoEncargado(), "Sin técnico"),
                    valor(h.getDescripcionAccion(), "Sin detalle").replace(" | ", "\n")});
        }
        pdf.tabla(new FichaTecnicaPdf.Tabla("Historial de mantenimiento", "Sin historial registrado.",
                new String[] {"Inicio", "Fin", "Técnico", "Acción realizada"},
                new float[] {90, 90, 100, 210}, filasHistorial, -1));

        List<String[]> filasManuales = new ArrayList<>();
        for (Manual manual : manualRepository.findAllByMaquina_IdMaquinasOrderByIdManualesDesc(maquina.getIdMaquinas())) {
            filasManuales.add(new String[] {
                    valor(manual.getNombreDelManual(), "Manual"),
                    manual.getTipoManual() != null ? valor(manual.getTipoManual().getDescripcionManual(), "Sin tipo") : "Sin tipo",
                    "link".equalsIgnoreCase(manual.getFuenteManual()) ? "Enlace" : "Archivo", fecha(manual.getFechaSubida())});
        }
        pdf.tabla(new FichaTecnicaPdf.Tabla("Manuales", "Sin manuales registrados.",
                new String[] {"Nombre", "Tipo", "Fuente", "Fecha de carga"},
                new float[] {200, 110, 70, 90}, filasManuales, -1));

        return pdf.terminar();
    }

    private String fecha(LocalDate fecha) {
        return fecha != null ? fecha.format(FECHA) : "Sin fecha";
    }

    private String persona(Usuario usuario, String defecto) {
        if (usuario == null) return defecto;
        String nombre = (valor(usuario.getNombre(), "") + " " + valor(usuario.getApellido(), "")).trim();
        return nombre.isEmpty() ? defecto : nombre;
    }

    private String estadoNovedad(String estado) {
        if (estado == null || estado.isBlank()) return "Reportada";
        return switch (estado.toUpperCase(Locale.ROOT)) {
            case "REPORTADA" -> "Reportada";
            case "ASIGNADA" -> "Asignada";
            case "EN_REPARACION" -> "En reparación";
            case "FINALIZADA_TECNICO" -> "Reparada";
            case "CERRADA" -> "Cerrada";
            case "CANCELADA" -> "Cancelada";
            case "DEVUELTA" -> "Devuelta";
            default -> estado.charAt(0) + estado.substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    private String valor(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}
