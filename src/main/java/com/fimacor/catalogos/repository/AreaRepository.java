package com.fimacor.catalogos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.catalogos.entity.Area;

public interface AreaRepository extends JpaRepository<Area, Integer> {

    List<Area> findAllByOrderByNombreAreaAsc();
}
