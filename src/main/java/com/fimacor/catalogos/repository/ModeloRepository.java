package com.fimacor.catalogos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.catalogos.entity.Modelo;

public interface ModeloRepository extends JpaRepository<Modelo, Integer> {
    List<Modelo> findAllByOrderByIdModeloDesc();
}
