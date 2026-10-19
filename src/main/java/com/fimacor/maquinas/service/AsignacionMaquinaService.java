package com.fimacor.maquinas.service;

import com.fimacor.usuarios.entity.NotificacionUsuario;
import com.fimacor.usuarios.repository.NotificacionUsuarioRepository;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.UsuarioRepository;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.entity.MaquinaCuentadante;
import com.fimacor.maquinas.repository.FichaMaquinaRepository;
import com.fimacor.maquinas.repository.MaquinaCuentadanteRepository;

/** Asigna maquinas a los usuarios cuentadante. */
@Service
public class AsignacionMaquinaService {

    private final MaquinaCuentadanteRepository asignacionRepository;
    private final FichaMaquinaRepository fichaMaquinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionUsuarioRepository notificacionRepository;

    public AsignacionMaquinaService(MaquinaCuentadanteRepository asignacionRepository,
            FichaMaquinaRepository fichaMaquinaRepository, UsuarioRepository usuarioRepository,
            NotificacionUsuarioRepository notificacionRepository) {
        this.asignacionRepository = asignacionRepository;
        this.fichaMaquinaRepository = fichaMaquinaRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
    }

    public List<Usuario> listarCuentadantes() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.isActivo() && u.getRol() != null && "cuentadante".equals(normalizar(u.getRol().getDescripcionRol())))
                .sorted(java.util.Comparator.comparing(Usuario::getNombre).thenComparing(Usuario::getApellido))
                .toList();
    }

    public List<MaquinaCuentadante> listarTodas() {
        return asignacionRepository.findAllByOrderByFechaAsignacionDescMaquina_CodigoSenaAsc().stream()
                .filter(a -> a.getMaquina() != null && !a.getMaquina().isBaja()).toList();
    }

    public List<MaquinaCuentadante> listarPorUsuario(int idUsuario) {
        return asignacionRepository.findByUsuario_IdUsuarioOrderByFechaAsignacionDescMaquina_CodigoSenaAsc(idUsuario).stream()
                .filter(a -> a.getMaquina() != null && !a.getMaquina().isBaja()).toList();
    }

    public long contarPorUsuario(int idUsuario) {
        return listarPorUsuario(idUsuario).size();
    }

    public record Resultado(boolean ok, String error) {
        static Resultado exito() {
            return new Resultado(true, null);
        }
        static Resultado fallo(String motivo) {
            return new Resultado(false, motivo);
        }
    }

    @Transactional
    public Resultado asignar(int idMaquina, int idUsuario) {
        FichaMaquina maquina = fichaMaquinaRepository.findById(idMaquina).orElse(null);
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        boolean esCuentadante = usuario != null && usuario.getRol() != null
                && "cuentadante".equals(normalizar(usuario.getRol().getDescripcionRol()));
        if (maquina == null || maquina.isBaja() || usuario == null || !esCuentadante) return Resultado.fallo("datos");

        MaquinaCuentadante asignacion = asignacionRepository.findByMaquina_IdMaquinas(idMaquina).orElseGet(MaquinaCuentadante::new);
        asignacion.setMaquina(maquina);
        asignacion.setUsuario(usuario);
        asignacion.setFechaAsignacion(LocalDateTime.now());
        asignacionRepository.save(asignacion);

        String codigo = maquina.getCodigoSena() != null && !maquina.getCodigoSena().isBlank() ? maquina.getCodigoSena() : "ID " + idMaquina;
        String descripcion = maquina.getDescripcion() != null && !maquina.getDescripcion().isBlank() ? " - " + maquina.getDescripcion() : "";
        NotificacionUsuario n = new NotificacionUsuario();
        n.setUsuario(usuario);
        n.setTitulo("Máquina asignada");
        n.setMensaje("Te asignaron la máquina " + codigo + descripcion + ".");
        n.setEnlace("/maquinas/mis-asignadas");
        n.setTipo("ok");
        n.setIcono("M");
        notificacionRepository.save(n);
        return Resultado.exito();
    }

    public boolean eliminar(int idAsignacion) {
        if (!asignacionRepository.existsById(idAsignacion)) return false;
        asignacionRepository.deleteById(idAsignacion);
        return true;
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim().toLowerCase();
    }
}
