package com.fimacor.usuarios.service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fimacor.usuarios.entity.Permiso;
import com.fimacor.usuarios.entity.Rol;
import com.fimacor.usuarios.entity.RolPermiso;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.PermisoRepository;
import com.fimacor.usuarios.repository.RolPermisoRepository;
import com.fimacor.usuarios.repository.RolRepository;
import com.fimacor.usuarios.repository.UsuarioRepository;

/** Administracion de roles y permisos. */
@Service
public class RolesPermisosService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final UsuarioRepository usuarioRepository;

    public RolesPermisosService(RolRepository rolRepository, PermisoRepository permisoRepository,
            RolPermisoRepository rolPermisoRepository, UsuarioRepository usuarioRepository) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.rolPermisoRepository = rolPermisoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Roles que el codigo del sistema reconoce por nombre: no se pueden renombrar ni eliminar. */
    private static final Set<String> ROLES_DEL_SISTEMA =
            Set.of("administrador", "instructor", "logistica", "tecnico profesional", "tecnico pasante", "cuentadante",
                    "subdireccion");

    public boolean esRolDelSistema(Rol rol) {
        return rol != null && ROLES_DEL_SISTEMA.contains(normalizar(rol.getDescripcionRol()));
    }

    public Set<Integer> idsRolesDelSistema() {
        Set<Integer> ids = new HashSet<>();
        for (Rol rol : rolRepository.findAll()) {
            if (esRolDelSistema(rol)) ids.add(rol.getIdRol());
        }
        return ids;
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAllByOrderByIdRolAsc();
    }

    public List<Permiso> listarPermisos() {
        return permisoRepository.findAllByOrderByIdPermisoAsc();
    }

    public Set<Integer> permisosPorRol(int idRol) {
        return new HashSet<>(rolPermisoRepository.idsPermisoPorRol(idRol));
    }

    public List<Usuario> usuariosPorRol(int idRol) {
        return usuarioRepository.findByRol_IdRolOrderByNombreAscApellidoAsc(idRol);
    }

    /** Si ya existe un rol con esa descripcion (sin acentos/mayusculas), reutiliza sus permisos en vez de duplicar. */
    @Transactional
    public boolean crearRol(String descripcion, List<Integer> permisosSeleccionados) {
        String limpio = limitar(descripcion, 45);
        if (limpio.isEmpty()) return false;

        Rol existente = buscarPorDescripcionNormalizada(limpio);
        if (existente != null) {
            guardarPermisosRol(existente.getIdRol(), permisosSeleccionados);
            return true;
        }

        Rol rol = new Rol();
        rol.setDescripcionRol(limpio);
        rol = rolRepository.save(rol);
        guardarPermisosRol(rol.getIdRol(), permisosSeleccionados);
        return true;
    }

    @Transactional
    public boolean actualizarRol(int idRol, String descripcion) {
        String limpio = limitar(descripcion, 45);
        if (idRol == 0 || limpio.isEmpty()) return false;
        return rolRepository.findById(idRol).map(rol -> {
            // Los permisos de un rol base si se pueden cambiar, pero su nombre no: el sistema lo usa para reconocerlo.
            if (esRolDelSistema(rol)) return normalizar(limpio).equals(normalizar(rol.getDescripcionRol()));
            rol.setDescripcionRol(limpio);
            rolRepository.save(rol);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean eliminarRol(int idRol) {
        if (idRol == 0) return false;
        if (!usuariosPorRol(idRol).isEmpty()) return false;
        if (esRolDelSistema(rolRepository.findById(idRol).orElse(null))) return false;
        rolPermisoRepository.eliminarPorRol(idRol);
        rolRepository.deleteById(idRol);
        return true;
    }

    @Transactional
    public boolean guardarPermisosRol(int idRol, List<Integer> permisosSeleccionados) {
        if (idRol == 0) return false;
        Rol rol = rolRepository.findById(idRol).orElse(null);
        if (rol == null) return false;

        rolPermisoRepository.eliminarPorRol(idRol);
        if (permisosSeleccionados != null) {
            for (Integer idPermiso : permisosSeleccionados) {
                Permiso permiso = permisoRepository.findById(idPermiso).orElse(null);
                if (permiso == null) continue;
                RolPermiso rp = new RolPermiso();
                rp.setRol(rol);
                rp.setPermiso(permiso);
                rolPermisoRepository.save(rp);
            }
        }
        return true;
    }

    public boolean crearPermiso(String codigo, String descripcion) {
        String codigoLimpio = limitar(codigo, 45);
        String descripcionLimpia = limitar(descripcion, 45);
        if (codigoLimpio.isEmpty() || descripcionLimpia.isEmpty()) return false;
        Permiso permiso = new Permiso();
        permiso.setCodigoPermiso(codigoLimpio);
        permiso.setDescripcion(descripcionLimpia);
        permisoRepository.save(permiso);
        return true;
    }

    public boolean actualizarPermiso(int idPermiso, String codigo, String descripcion) {
        String codigoLimpio = limitar(codigo, 45);
        String descripcionLimpia = limitar(descripcion, 45);
        if (idPermiso == 0 || codigoLimpio.isEmpty() || descripcionLimpia.isEmpty()) return false;
        return permisoRepository.findById(idPermiso).map(permiso -> {
            permiso.setCodigoPermiso(codigoLimpio);
            permiso.setDescripcion(descripcionLimpia);
            permisoRepository.save(permiso);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean eliminarPermiso(int idPermiso) {
        if (idPermiso == 0) return false;
        rolPermisoRepository.eliminarPorPermiso(idPermiso);
        permisoRepository.deleteById(idPermiso);
        return true;
    }

    private Rol buscarPorDescripcionNormalizada(String descripcion) {
        String buscado = normalizar(descripcion);
        for (Rol rol : rolRepository.findAll()) {
            if (normalizar(rol.getDescripcionRol()).equals(buscado)) return rol;
        }
        return null;
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private String limitar(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
