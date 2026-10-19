package com.fimacor.catalogos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.catalogos.entity.AmbienteAprendizaje;

public interface AmbienteAprendizajeRepository extends JpaRepository<AmbienteAprendizaje, Integer> {
    List<AmbienteAprendizaje> findAllByOrderByIdAmbientesDeAprendizajeDesc();
}
