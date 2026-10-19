package com.fimacor.usuarios.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.usuarios.entity.NotificacionConfiguracion;

public interface NotificacionConfiguracionRepository extends JpaRepository<NotificacionConfiguracion, Integer> {
    List<NotificacionConfiguracion> findFirst12ByOrderByIdNotificacionDesc();
}
