package com.fimacor.maquinas.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.maquinas.entity.MaquinaCuentadante;

public interface MaquinaCuentadanteRepository extends JpaRepository<MaquinaCuentadante, Integer> {

    // La maquina y el usuario se cargan junto con la asignacion: el servicio y la plantilla los usan
    // fuera de una sesion de Hibernate (open-in-view esta apagado) y si no, falla con LazyInitializationException.
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"maquina", "usuario"})
    List<MaquinaCuentadante> findAllByOrderByFechaAsignacionDescMaquina_CodigoSenaAsc();

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"maquina", "usuario"})
    List<MaquinaCuentadante> findByUsuario_IdUsuarioOrderByFechaAsignacionDescMaquina_CodigoSenaAsc(Integer idUsuario);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"maquina", "usuario"})
    Optional<MaquinaCuentadante> findByMaquina_IdMaquinas(Integer idMaquina);

    long countByUsuario_IdUsuario(Integer idUsuario);
}
