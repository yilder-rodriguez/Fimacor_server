package com.fimacor.usuarios.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.usuarios.entity.Permiso;

public interface PermisoRepository extends JpaRepository<Permiso, Integer> {
    List<Permiso> findAllByOrderByIdPermisoAsc();
}
