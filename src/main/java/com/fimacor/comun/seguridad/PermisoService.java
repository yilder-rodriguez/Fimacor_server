package com.fimacor.comun.seguridad;

import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.RolPermisoRepository;
import com.fimacor.usuarios.repository.RolRepository;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Verifica permisos comparando texto: revisa si el permiso normalizado (sin
 * acentos ni mayusculas) contiene las claves pedidas. Es un matching
 * flexible a proposito, para no romper los permisos ya guardados en la
 * base de datos.
 */
@Service
public class PermisoService {

    private final RolRepository rolRepository;
    private final RolPermisoRepository rolPermisoRepository;

    public PermisoService(RolRepository rolRepository, RolPermisoRepository rolPermisoRepository) {
        this.rolRepository = rolRepository;
        this.rolPermisoRepository = rolPermisoRepository;
    }

    public boolean tienePermiso(Integer idRol, String... claves) {
        if (idRol == null || idRol <= 0) return false;
        if (esRolTotal(idRol)) return true;
        Set<String> permisos = permisosTexto(idRol);
        if (permisos.isEmpty()) return false;
        for (String permiso : permisos) {
            String permisoNormalizado = normalizar(permiso);
            for (String clave : claves) {
                if (permisoNormalizado.contains(normalizar(clave))) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean tienePermisoConTodas(Integer idRol, String... claves) {
        if (idRol == null || idRol <= 0) return false;
        if (esRolTotal(idRol)) return true;
        Set<String> permisos = permisosTexto(idRol);
        if (permisos.isEmpty()) return false;
        for (String permiso : permisos) {
            String permisoNormalizado = normalizar(permiso);
            boolean coincide = true;
            for (String clave : claves) {
                if (!permisoNormalizado.contains(normalizar(clave))) {
                    coincide = false;
                    break;
                }
            }
            if (coincide) return true;
        }
        return false;
    }

    /** Instructor al que el Administrador le marco la casilla "actua como tecnico". */
    public boolean actuaComoTecnico(Usuario usuario) {
        return usuario != null && usuario.isActuaComoTecnico() && usuario.getRol() != null
                && normalizar(usuario.getRol().getDescripcionRol()).contains("instructor");
    }

    /** Puede atenderse como tecnico: por los permisos de su rol o por la casilla del Instructor. */
    public boolean puedeAtenderComoTecnico(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) return false;
        int idRol = usuario.getRol().getIdRol();
        return tienePermisoConTodas(idRol, "reparacion", "reparar")
                || tienePermisoConTodas(idRol, "mantenimiento", "confirmar")
                || actuaComoTecnico(usuario);
    }

    public boolean esRolTotal(Integer idRol) {
        if (idRol == null || idRol <= 0) return false;
        String descripcion = rolRepository.findById(idRol).map(r -> r.getDescripcionRol()).orElse(null);
        String normalizado = normalizar(descripcion);
        return normalizado.contains("administrador");
    }

    private Set<String> permisosTexto(Integer idRol) {
        Set<String> permisos = new HashSet<>();
        permisos.addAll(rolPermisoRepository.codigosPermisoPorRol(idRol));
        permisos.addAll(rolPermisoRepository.descripcionesPermisoPorRol(idRol));
        return permisos;
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
