package com.fimacor.maquinas.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.maquinas.entity.BajaMaquinaHistorial;

public interface BajaMaquinaHistorialRepository extends JpaRepository<BajaMaquinaHistorial, Integer> {
    List<BajaMaquinaHistorial> findTop50ByOrderByFechaDescIdBajaHistorialDesc();
}
