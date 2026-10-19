package com.fimacor.usuarios.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import com.fimacor.usuarios.entity.RolPermiso;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, Integer> {

    @Query("select rp.permiso.codigoPermiso from RolPermiso rp where rp.rol.idRol = :idRol")
    List<String> codigosPermisoPorRol(@Param("idRol") Integer idRol);

    @Query("select rp.permiso.descripcion from RolPermiso rp where rp.rol.idRol = :idRol")
    List<String> descripcionesPermisoPorRol(@Param("idRol") Integer idRol);

    @Query("select rp.permiso.idPermiso from RolPermiso rp where rp.rol.idRol = :idRol")
    List<Integer> idsPermisoPorRol(@Param("idRol") Integer idRol);

    @Modifying
    @Query("delete from RolPermiso rp where rp.rol.idRol = :idRol")
    void eliminarPorRol(@Param("idRol") Integer idRol);

    @Modifying
    @Query("delete from RolPermiso rp where rp.permiso.idPermiso = :idPermiso")
    void eliminarPorPermiso(@Param("idPermiso") Integer idPermiso);
}
