package com.fimacor.catalogos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.catalogos.entity.Sede;

public interface SedeRepository extends JpaRepository<Sede, Integer> {
    List<Sede> findAllByOrderByIdSedeDesc();
    List<Sede> findAllByOrderByNombreSedeAsc();
}
