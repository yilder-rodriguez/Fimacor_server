package com.fimacor.mantenimiento.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.mantenimiento.entity.Mantenimiento;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Integer> {
    List<Mantenimiento> findAllByOrderByIdMantenimientoDesc();

    List<Mantenimiento> findByTecnico_IdUsuarioOrderByIdMantenimientoDesc(Integer idUsuario);

    List<Mantenimiento> findByMaquina_IdMaquinasOrderByIdMantenimientoDesc(Integer idMaquina);
}
