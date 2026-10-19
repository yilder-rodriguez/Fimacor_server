package com.fimacor.maquinas.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.maquinas.entity.FichaMaquina;

public interface FichaMaquinaRepository extends JpaRepository<FichaMaquina, Integer> {
    /** Solo maquinas activas: las dadas de baja salen de listados, formularios y contadores. */
    List<FichaMaquina> findByBajaFalseOrderByCodigoSenaAsc();

    List<FichaMaquina> findByBajaFalseOrderByIdMaquinasDesc();

    List<FichaMaquina> findByBajaTrueOrderByFechaBajaDesc();

    Optional<FichaMaquina> findByCodigoSenaIgnoreCase(String codigoSena);

    List<FichaMaquina> findByReubicacionId(Integer reubicacionId);
}
