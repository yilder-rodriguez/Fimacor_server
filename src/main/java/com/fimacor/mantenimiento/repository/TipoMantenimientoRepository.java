package com.fimacor.mantenimiento.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.mantenimiento.entity.TipoMantenimiento;

public interface TipoMantenimientoRepository extends JpaRepository<TipoMantenimiento, Integer> {
    List<TipoMantenimiento> findAllByOrderByIdTipoMantenientoAsc();
}
