package com.fimacor.catalogos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.catalogos.entity.TipoMaquina;

public interface TipoMaquinaRepository extends JpaRepository<TipoMaquina, Integer> {
    List<TipoMaquina> findAllByOrderByIdTipoMaquinaDesc();
}
