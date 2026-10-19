package com.fimacor.catalogos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.catalogos.entity.Marca;

public interface MarcaRepository extends JpaRepository<Marca, Integer> {
    List<Marca> findAllByOrderByIdMarcaDesc();
    List<Marca> findAllByOrderByDescripcionMarcaAsc();
}
