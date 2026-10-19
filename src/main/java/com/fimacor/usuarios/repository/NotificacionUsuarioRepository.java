package com.fimacor.usuarios.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fimacor.usuarios.entity.NotificacionUsuario;

public interface NotificacionUsuarioRepository extends JpaRepository<NotificacionUsuario, Integer> {
    List<NotificacionUsuario> findFirst20ByUsuario_IdUsuarioOrderByIdNotificacionUsuarioDesc(Integer idUsuario);
}
