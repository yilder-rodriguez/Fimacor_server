package com.fimacor.maquinas.service;

import com.fimacor.maquinas.entity.BajaMaquinaHistorial;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.repository.BajaMaquinaHistorialRepository;
import com.fimacor.maquinas.repository.FichaMaquinaRepository;
import com.fimacor.novedades.entity.Novedad;
import com.fimacor.novedades.repository.NovedadRepository;
import com.fimacor.reubicacion.repository.ReubicacionMaquinaRepository;
import com.fimacor.usuarios.entity.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baja de maquinas. Una maquina nunca se elimina (trazabilidad): se marca como dada de
 * baja, sale de listados y formularios, y conserva todo su historial. Cada baja y cada
 * reactivacion queda registrada con quien, cuando y por que.
 */
@Service
public class BajaMaquinaService {

    private static final List<String> NOVEDAD_TERMINADA = List.of("FINALIZADA_TECNICO", "CERRADA", "CANCELADA");

    private final FichaMaquinaRepository maquinaRepository;
    private final NovedadRepository novedadRepository;
    private final ReubicacionMaquinaRepository reubicacionRepository;
    private final BajaMaquinaHistorialRepository historialRepository;

    public BajaMaquinaService(FichaMaquinaRepository maquinaRepository, NovedadRepository novedadRepository,
            ReubicacionMaquinaRepository reubicacionRepository, BajaMaquinaHistorialRepository historialRepository) {
        this.maquinaRepository = maquinaRepository;
        this.novedadRepository = novedadRepository;
        this.reubicacionRepository = reubicacionRepository;
        this.historialRepository = historialRepository;
    }

    public record Resultado(boolean ok, String error) {
        static Resultado exito() {
            return new Resultado(true, null);
        }
        static Resultado fallo(String motivo) {
            return new Resultado(false, motivo);
        }
    }

    public List<FichaMaquina> listarDadasDeBaja() {
        return maquinaRepository.findByBajaTrueOrderByFechaBajaDesc();
    }

    public List<BajaMaquinaHistorial> listarHistorial() {
        return historialRepository.findTop50ByOrderByFechaDescIdBajaHistorialDesc();
    }

    /**
     * Da de baja una maquina. Si viene de una solicitud (idNovedadOrigen), esa novedad la cierra
     * quien la aprueba; las demas novedades abiertas de la maquina se cancelan porque ya no
     * tiene sentido repararlas.
     */
    @Transactional
    public Resultado darDeBaja(int idMaquina, String motivo, Usuario por, Integer idNovedadOrigen) {
        String motivoLimpio = limitar(motivo, 500);
        if (motivoLimpio.isBlank()) return Resultado.fallo("motivo");
        FichaMaquina maquina = maquinaRepository.findById(idMaquina).orElse(null);
        if (maquina == null) return Resultado.fallo("maquina");
        if (maquina.isBaja()) return Resultado.fallo("ya_baja");
        if (reubicacionRepository.existsByMaquina_IdMaquinasAndEstadoAutorizacionIgnoreCase(idMaquina, "Pendiente")) {
            return Resultado.fallo("traslado_pendiente");
        }

        LocalDateTime ahora = LocalDateTime.now();
        maquina.setBaja(true);
        maquina.setFechaBaja(ahora);
        maquina.setMotivoBaja(motivoLimpio);
        maquina.setBajaPor(nombre(por));
        maquinaRepository.save(maquina);
        registrar(maquina, BajaMaquinaHistorial.BAJA, motivoLimpio, por, idNovedadOrigen, ahora);

        for (Novedad n : novedadRepository.findByMaquina_IdMaquinasOrderByIdNovedadesDesc(idMaquina)) {
            if (idNovedadOrigen != null && idNovedadOrigen.equals(n.getIdNovedades())) continue;
            if (NOVEDAD_TERMINADA.contains(n.getEstadoFlujo())) continue;
            n.setEstadoFlujo("CANCELADA");
            if ("PENDIENTE".equals(n.getSolicitudBaja())) n.setSolicitudBaja("DESESTIMADA");
            n.setObservacionGestion(limitar("Maquina dada de baja: " + motivoLimpio, 500));
            novedadRepository.save(n);
        }
        return Resultado.exito();
    }

    @Transactional
    public Resultado reactivar(int idMaquina, String motivo, Usuario por) {
        String motivoLimpio = limitar(motivo, 500);
        if (motivoLimpio.isBlank()) return Resultado.fallo("motivo");
        FichaMaquina maquina = maquinaRepository.findById(idMaquina).orElse(null);
        if (maquina == null) return Resultado.fallo("maquina");
        if (!maquina.isBaja()) return Resultado.fallo("no_baja");

        LocalDateTime ahora = LocalDateTime.now();
        maquina.setBaja(false);
        maquina.setFechaBaja(null);
        maquina.setMotivoBaja(null);
        maquina.setBajaPor(null);
        maquinaRepository.save(maquina);
        registrar(maquina, BajaMaquinaHistorial.REACTIVACION, motivoLimpio, por, null, ahora);
        return Resultado.exito();
    }

    private void registrar(FichaMaquina maquina, String accion, String motivo, Usuario por, Integer idNovedad,
            LocalDateTime fecha) {
        BajaMaquinaHistorial h = new BajaMaquinaHistorial();
        h.setMaquina(maquina);
        h.setAccion(accion);
        h.setMotivo(motivo);
        h.setUsuarioId(por != null ? por.getIdUsuario() : null);
        h.setUsuarioNombre(nombre(por));
        h.setNovedadId(idNovedad);
        h.setFecha(fecha);
        historialRepository.save(h);
    }

    private String nombre(Usuario usuario) {
        if (usuario == null) return "Sistema";
        return limitar((usuario.getNombre() + " " + usuario.getApellido()).trim(), 120);
    }

    private String limitar(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
