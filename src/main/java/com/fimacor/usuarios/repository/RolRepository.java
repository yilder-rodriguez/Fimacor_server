package com.fimacor.usuarios.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.usuarios.entity.Rol;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    List<Rol> findAllByOrderByIdRolAsc();
}
