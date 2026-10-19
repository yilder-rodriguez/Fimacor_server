package com.fimacor.usuarios.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.fimacor.usuarios.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // JOIN FETCH: el rol se necesita siempre junto al usuario autenticado (permisos, vista),
    // y para entonces la sesion de Hibernate de esta consulta ya se cerro (open-in-view: false).
    @Query("select u from Usuario u join fetch u.rol where lower(trim(u.correo)) = lower(trim(:correo))")
    Optional<Usuario> buscarPorCorreo(@Param("correo") String correo);

    List<Usuario> findByRol_IdRolOrderByNombreAscApellidoAsc(Integer idRol);

    boolean existsByCorreo(String correo);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByNumeroTelefono(String numeroTelefono);

    @Query("select u from Usuario u where u.activo = true and lower(u.rol.descripcionRol) like '%subdireccion%' "
            + "order by u.rol.descripcionRol, u.nombre, u.apellido")
    List<Usuario> listarResponsablesAutorizacionReubicacion();

    // Valida el reporte de novedad por QR sin login: solo un Instructor activo con este codigo exacto.
    @Query("select u from Usuario u join fetch u.rol where upper(trim(u.codigoInstructor)) = upper(trim(:codigo)) "
            + "and u.activo = true and lower(u.rol.descripcionRol) like '%instructor%'")
    Optional<Usuario> buscarInstructorPorCodigo(@Param("codigo") String codigo);

    boolean existsByCodigoInstructorIgnoreCaseAndIdUsuarioNot(String codigoInstructor, Integer idUsuario);

    List<Usuario> findByCodigoInstructorIsNullAndRol_DescripcionRolContainingIgnoreCase(String descripcionRol);

    List<Usuario> findByActivoTrueAndRol_DescripcionRolContainingIgnoreCase(String descripcionRol);
}
