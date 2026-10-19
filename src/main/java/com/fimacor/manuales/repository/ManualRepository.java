package com.fimacor.manuales.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.manuales.entity.Manual;

public interface ManualRepository extends JpaRepository<Manual, Integer> {
    List<Manual> findAllByMaquina_IdMaquinasOrderByIdManualesDesc(Integer idMaquina);
}
