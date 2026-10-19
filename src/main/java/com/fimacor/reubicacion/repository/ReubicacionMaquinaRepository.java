package com.fimacor.reubicacion.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.reubicacion.entity.ReubicacionMaquina;

public interface ReubicacionMaquinaRepository extends JpaRepository<ReubicacionMaquina, Integer> {
    List<ReubicacionMaquina> findAllByOrderByIdRehubicacionMaquinaDesc();

    boolean existsByMaquina_IdMaquinasAndEstadoAutorizacionIgnoreCase(Integer idMaquinas, String estadoAutorizacion);
}
