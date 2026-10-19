package com.fimacor.catalogos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.catalogos.entity.EstadoMaquina;

public interface EstadoMaquinaRepository extends JpaRepository<EstadoMaquina, Integer> {
    List<EstadoMaquina> findAllByOrderByIdEstadoDeMaquinaAsc();
}
