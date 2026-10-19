package com.fimacor.novedades.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.novedades.entity.Novedad;

public interface NovedadRepository extends JpaRepository<Novedad, Integer> {
    List<Novedad> findAllByOrderByIdNovedadesDesc();

    List<Novedad> findByMaquina_IdMaquinasOrderByIdNovedadesDesc(Integer idMaquina);
}
