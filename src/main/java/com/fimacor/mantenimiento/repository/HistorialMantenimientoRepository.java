package com.fimacor.mantenimiento.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.mantenimiento.entity.HistorialMantenimiento;

public interface HistorialMantenimientoRepository extends JpaRepository<HistorialMantenimiento, Integer> {
    List<HistorialMantenimiento> findAllByOrderByIdHistorialMantenimientoDesc();

    List<HistorialMantenimiento> findByMaquina_IdMaquinasOrderByIdHistorialMantenimientoDesc(Integer idMaquina);
}
