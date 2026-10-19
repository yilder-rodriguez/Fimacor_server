package com.fimacor.manuales.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.manuales.entity.TipoManual;

public interface TipoManualRepository extends JpaRepository<TipoManual, Integer> {
    List<TipoManual> findAllByOrderByIdTiposManualesAsc();
}
