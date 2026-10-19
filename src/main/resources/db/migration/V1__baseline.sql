-- Baseline del esquema FIMACOR para Flyway.
-- Fuente: database/fimacor_unificada_actual_20260810.sql (31 tablas + 1 vista),
-- reconciliado con las columnas que la app Java EE legacy solo crea en tiempo de
-- ejecucion (ver Controlador/UsuarioDAO.java, NovedadesDAO.java):
--   - usuario.contrasena_usuario ampliada a VARCHAR(255) (un hash BCrypt mide 60).
--   - novedades: columnas del flujo de asignacion/reparacion (estado_flujo y siguientes).
-- Sobre la base de datos de produccion existente, este archivo se registra con
-- `flyway baseline -baselineVersion=1` (no se ejecuta, solo se marca como aplicado).
-- Sobre una base de datos nueva/vacia, Flyway lo ejecuta tal cual para crear el esquema.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `ambientes_aprendizaje` (
  `idAmbientes_de_aprendizaje` int(11) NOT NULL,
  `descripcion_ambiente` varchar(45) NOT NULL,
  PRIMARY KEY (`idAmbientes_de_aprendizaje`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `area` (
  `idArea` int(11) NOT NULL,
  `nombre_area` varchar(150) NOT NULL,
  PRIMARY KEY (`idArea`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `rol` (
  `idRol` int(11) NOT NULL AUTO_INCREMENT,
  `descripcion_rol` varchar(45) NOT NULL,
  PRIMARY KEY (`idRol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `documento_identidad` (
  `idDocumento_Identidad` int(11) NOT NULL AUTO_INCREMENT,
  `descripcion_Documento` varchar(45) NOT NULL,
  `numero_documento` varchar(45) NOT NULL,
  PRIMARY KEY (`idDocumento_Identidad`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `usuario` (
  `idUsuario` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(45) NOT NULL,
  `apellido` varchar(45) NOT NULL,
  `documento_identidad` varchar(45) NOT NULL,
  `numero_telefono` varchar(45) NOT NULL,
  `correo` varchar(45) NOT NULL,
  `contrasena_usuario` varchar(255) NOT NULL,
  `fecha_expiracion` datetime NOT NULL,
  `Rol_idRol` int(11) NOT NULL,
  `Documento_Identidad_idDocumento_Identidad` int(11) NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1,
  `foto_carnet` longtext DEFAULT NULL,
  PRIMARY KEY (`idUsuario`),
  UNIQUE KEY `idUsuario_UNIQUE` (`idUsuario`),
  KEY `fk_Usuario_Rol1_idx` (`Rol_idRol`),
  KEY `fk_Usuario_Documento_Identidad1_idx` (`Documento_Identidad_idDocumento_Identidad`),
  CONSTRAINT `fk_Usuario_Documento_Identidad1` FOREIGN KEY (`Documento_Identidad_idDocumento_Identidad`) REFERENCES `documento_identidad` (`idDocumento_Identidad`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Usuario_Rol1` FOREIGN KEY (`Rol_idRol`) REFERENCES `rol` (`idRol`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `auditoria_movimiento` (
  `idAuditoria` int(11) NOT NULL AUTO_INCREMENT,
  `Usuario_idUsuario` int(11) DEFAULT NULL,
  `accion` varchar(100) NOT NULL,
  `metodo` varchar(10) NOT NULL,
  `ruta` varchar(255) NOT NULL,
  `ip_actual` varchar(64) NOT NULL,
  `detalle` varchar(1000) DEFAULT NULL,
  `fecha` datetime NOT NULL,
  PRIMARY KEY (`idAuditoria`),
  KEY `idx_auditoria_usuario_fecha` (`Usuario_idUsuario`,`fecha`),
  CONSTRAINT `fk_auditoria_usuario` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `codigo_registro` (
  `idCodigoRegistro` int(11) NOT NULL,
  `correo` varchar(45) NOT NULL,
  `codigo` varchar(45) NOT NULL,
  `fecha_creacion` datetime NOT NULL DEFAULT current_timestamp(),
  `Usuario_idUsuario` int(11) NOT NULL,
  PRIMARY KEY (`idCodigoRegistro`,`correo`),
  KEY `fk_Codigo_Registro_Usuario1_idx` (`Usuario_idUsuario`),
  CONSTRAINT `fk_Codigo_Registro_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `estado_de_maquina` (
  `idEstado_de_Maquina` int(11) NOT NULL,
  `nombre_tipo` varchar(45) NOT NULL,
  `descripcion_estado` varchar(45) NOT NULL,
  PRIMARY KEY (`idEstado_de_Maquina`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `tipo_maquina` (
  `idTipo_Maquina` int(11) NOT NULL,
  `descripcion_maquina` varchar(45) NOT NULL,
  PRIMARY KEY (`idTipo_Maquina`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `sede` (
  `idSede` int(11) NOT NULL,
  `nombre_sede` varchar(150) NOT NULL,
  PRIMARY KEY (`idSede`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `marca` (
  `idMarca` int(11) NOT NULL AUTO_INCREMENT,
  `descripcion_marca` varchar(45) NOT NULL,
  PRIMARY KEY (`idMarca`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `modelo` (
  `idModelo` int(11) NOT NULL AUTO_INCREMENT,
  `descripcion_modelo` varchar(45) NOT NULL,
  `Marca_idMarca` int(11) NOT NULL,
  PRIMARY KEY (`idModelo`),
  KEY `fk_Modelo_Marca1_idx` (`Marca_idMarca`),
  CONSTRAINT `fk_Modelo_Marca1` FOREIGN KEY (`Marca_idMarca`) REFERENCES `marca` (`idMarca`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `reubicacion_maquina` (
  `idRehubicacion_Maquina` int(11) NOT NULL AUTO_INCREMENT,
  `Ficha_Maquina_idMaquinas` int(11) DEFAULT NULL,
  `ambiente_destino_id` int(11) DEFAULT NULL,
  `sede_destino_id` int(11) DEFAULT NULL,
  `area_destino` varchar(100) DEFAULT NULL,
  `fecha` varchar(45) NOT NULL,
  `tipo_traslado` varchar(20) NOT NULL DEFAULT 'Interno',
  `ambiente` varchar(45) NOT NULL,
  `destino` varchar(45) NOT NULL,
  `autorizado_por` varchar(45) NOT NULL,
  `autorizacion` varchar(45) NOT NULL,
  `observacion` varchar(45) DEFAULT NULL,
  `evidencia` varchar(500) NOT NULL,
  `descripcion_rehubicacion` varchar(45) NOT NULL,
  `estado_autorizacion` varchar(20) NOT NULL DEFAULT 'Pendiente',
  `respuesta_autorizacion` varchar(255) DEFAULT NULL,
  `fecha_respuesta_autorizacion` datetime DEFAULT NULL,
  PRIMARY KEY (`idRehubicacion_Maquina`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `ficha_maquina` (
  `idMaquinas` int(11) NOT NULL AUTO_INCREMENT,
  `codigo_sena` varchar(45) NOT NULL,
  `descripcion` varchar(45) NOT NULL,
  `Modelo_idModelo` int(11) DEFAULT NULL,
  `Estado_de_Maquina_idEstado_de_Maquina` int(11) NOT NULL,
  `Tipo_Maquina_idTipo_Maquina` int(11) NOT NULL,
  `Reubicacion_Maquina_idRehubicacion_Maquina` int(11) DEFAULT NULL,
  `Ambientes_Aprendizaje_idAmbientes_de_aprendizaje` int(11) NOT NULL,
  `fecha_ubicacion_ambiente` date DEFAULT NULL,
  `Sede_idSede` int(11) NOT NULL DEFAULT 1,
  `area` varchar(100) DEFAULT NULL,
  `fecha_compra` date DEFAULT NULL,
  `valor_maquina` decimal(12,2) DEFAULT NULL,
  `tiene_garantia` tinyint(1) NOT NULL DEFAULT 0,
  `fecha_garantia` date DEFAULT NULL,
  `Marca_idMarca` int(11) NOT NULL,
  PRIMARY KEY (`idMaquinas`),
  UNIQUE KEY `uk_ficha_maquina_codigo_sena` (`codigo_sena`),
  KEY `fk_Ficha_Maquina_Estado_de_Maquina1_idx` (`Estado_de_Maquina_idEstado_de_Maquina`),
  KEY `fk_Ficha_Maquina_Tipo_Maquina1_idx` (`Tipo_Maquina_idTipo_Maquina`),
  KEY `fk_Ficha_Maquina_Reubicacion_Maquina1_idx` (`Reubicacion_Maquina_idRehubicacion_Maquina`),
  KEY `fk_Ficha_Maquina_Ambientes_Aprendizaje1_idx` (`Ambientes_Aprendizaje_idAmbientes_de_aprendizaje`),
  KEY `fk_Ficha_Maquina_Marca1_idx` (`Marca_idMarca`),
  KEY `fk_ficha_maquina_modelo_idx` (`Modelo_idModelo`),
  KEY `fk_Ficha_Maquina_Sede1_idx` (`Sede_idSede`),
  CONSTRAINT `fk_Ficha_Maquina_Ambientes_Aprendizaje1` FOREIGN KEY (`Ambientes_Aprendizaje_idAmbientes_de_aprendizaje`) REFERENCES `ambientes_aprendizaje` (`idAmbientes_de_aprendizaje`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Ficha_Maquina_Estado_de_Maquina1` FOREIGN KEY (`Estado_de_Maquina_idEstado_de_Maquina`) REFERENCES `estado_de_maquina` (`idEstado_de_Maquina`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Ficha_Maquina_Marca1` FOREIGN KEY (`Marca_idMarca`) REFERENCES `marca` (`idMarca`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Ficha_Maquina_Reubicacion_Maquina1` FOREIGN KEY (`Reubicacion_Maquina_idRehubicacion_Maquina`) REFERENCES `reubicacion_maquina` (`idRehubicacion_Maquina`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Ficha_Maquina_Sede1` FOREIGN KEY (`Sede_idSede`) REFERENCES `sede` (`idSede`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Ficha_Maquina_Tipo_Maquina1` FOREIGN KEY (`Tipo_Maquina_idTipo_Maquina`) REFERENCES `tipo_maquina` (`idTipo_Maquina`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_ficha_maquina_modelo` FOREIGN KEY (`Modelo_idModelo`) REFERENCES `modelo` (`idModelo`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `documentacion` (
  `idReporte_mantenimientos` int(11) NOT NULL,
  `fecha_reporte` varchar(45) NOT NULL,
  `archivo_pdf` varchar(500) NOT NULL,
  `firma_digital` varchar(45) NOT NULL,
  `Mantenimiento_idMantenimiento` int(11) NOT NULL,
  PRIMARY KEY (`idReporte_mantenimientos`),
  KEY `fk_Reporte_mantenimientos_Mantenimiento1_idx` (`Mantenimiento_idMantenimiento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `tipo_manteniento` (
  `idTipo_Manteniento` int(11) NOT NULL AUTO_INCREMENT,
  `descripcion_matenimiento` varchar(45) NOT NULL,
  PRIMARY KEY (`idTipo_Manteniento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `mantenimiento` (
  `idMantenimiento` int(11) NOT NULL AUTO_INCREMENT,
  `fecha_mantenimiento` date NOT NULL,
  `fecha_proximo_mantenimiento` date NOT NULL,
  `descripcion_Mantenimiento` varchar(90) NOT NULL,
  `frecuencia` varchar(45) NOT NULL,
  `hora_programada` varchar(5) DEFAULT NULL,
  `tareas` varchar(255) NOT NULL,
  `evidencia_mantenimiento` varchar(1500) NOT NULL DEFAULT '',
  `Tipo_Manteniento_idTipo_Manteniento` int(11) NOT NULL,
  `Ficha_Maquina_idMaquinas` int(11) NOT NULL,
  `Usuario_idUsuario` int(11) NOT NULL,
  `programado_por_usuario_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`idMantenimiento`),
  KEY `fk_Mantenimiento_Tipo_Manteniento1_idx` (`Tipo_Manteniento_idTipo_Manteniento`),
  KEY `fk_Mantenimiento_Ficha_Maquina1_idx` (`Ficha_Maquina_idMaquinas`),
  KEY `idx_mantenimiento_programado_por` (`programado_por_usuario_id`),
  CONSTRAINT `fk_Mantenimiento_Ficha_Maquina1` FOREIGN KEY (`Ficha_Maquina_idMaquinas`) REFERENCES `ficha_maquina` (`idMaquinas`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Mantenimiento_Tipo_Manteniento1` FOREIGN KEY (`Tipo_Manteniento_idTipo_Manteniento`) REFERENCES `tipo_manteniento` (`idTipo_Manteniento`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

ALTER TABLE `documentacion`
  ADD CONSTRAINT `fk_Reporte_mantenimientos_Mantenimiento1` FOREIGN KEY (`Mantenimiento_idMantenimiento`) REFERENCES `mantenimiento` (`idMantenimiento`) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE TABLE `historial_mantenimiento` (
  `idHistorial_Mantenimiento` int(11) NOT NULL,
  `tipo_maquina` varchar(45) NOT NULL,
  `fecha_inicio` varchar(45) NOT NULL,
  `fecha_final` varchar(45) NOT NULL,
  `tecnico_encargado` varchar(45) NOT NULL,
  `descripcion_accion` varchar(1200) NOT NULL,
  `Ficha_Maquina_idMaquinas` int(11) NOT NULL,
  `Usuario_idUsuario` int(11) NOT NULL,
  PRIMARY KEY (`idHistorial_Mantenimiento`),
  KEY `fk_Historial_Mantenimiento_Ficha_Maquina1_idx` (`Ficha_Maquina_idMaquinas`),
  KEY `fk_Historial_Mantenimiento_Usuario1_idx` (`Usuario_idUsuario`),
  CONSTRAINT `fk_Historial_Mantenimiento_Ficha_Maquina1` FOREIGN KEY (`Ficha_Maquina_idMaquinas`) REFERENCES `ficha_maquina` (`idMaquinas`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Historial_Mantenimiento_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `tipos_manuales` (
  `idTipos_Manuales` int(11) NOT NULL AUTO_INCREMENT,
  `descripcion_manual` varchar(45) NOT NULL,
  PRIMARY KEY (`idTipos_Manuales`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `manuales` (
  `idManuales` int(11) NOT NULL,
  `nombre_del_manual` varchar(45) NOT NULL,
  `descripcion_manual` varchar(45) NOT NULL,
  `fecha_subida` date NOT NULL,
  `fuente_manual` varchar(10) NOT NULL DEFAULT 'archivo',
  `ruta_manual` varchar(500) DEFAULT NULL,
  `Tipos_Manuales_idTipos_Manuales` int(11) NOT NULL,
  `Ficha_Maquina_idMaquinas` int(11) NOT NULL,
  PRIMARY KEY (`idManuales`),
  KEY `fk_Manuales_Tipos_Manuales1_idx` (`Tipos_Manuales_idTipos_Manuales`),
  KEY `fk_Manuales_Ficha_Maquina1_idx` (`Ficha_Maquina_idMaquinas`),
  CONSTRAINT `fk_Manuales_Ficha_Maquina1` FOREIGN KEY (`Ficha_Maquina_idMaquinas`) REFERENCES `ficha_maquina` (`idMaquinas`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Manuales_Tipos_Manuales1` FOREIGN KEY (`Tipos_Manuales_idTipos_Manuales`) REFERENCES `tipos_manuales` (`idTipos_Manuales`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `maquina_cuentadante` (
  `idMaquina_Cuentadante` int(11) NOT NULL AUTO_INCREMENT,
  `Ficha_Maquina_idMaquinas` int(11) NOT NULL,
  `Usuario_idUsuario` int(11) NOT NULL,
  `fecha_asignacion` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`idMaquina_Cuentadante`),
  UNIQUE KEY `uk_maquina_cuentadante` (`Ficha_Maquina_idMaquinas`),
  KEY `idx_cuentadante_usuario` (`Usuario_idUsuario`),
  CONSTRAINT `fk_maquina_cuentadante_maquina` FOREIGN KEY (`Ficha_Maquina_idMaquinas`) REFERENCES `ficha_maquina` (`idMaquinas`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_maquina_cuentadante_usuario` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `notificaciones_configuracion` (
  `id_notificacion` int(11) NOT NULL AUTO_INCREMENT,
  `titulo` varchar(120) NOT NULL,
  `mensaje` varchar(255) NOT NULL,
  `enlace` varchar(180) NOT NULL,
  `tipo` varchar(20) NOT NULL DEFAULT 'admin',
  `icono` varchar(6) NOT NULL DEFAULT 'A',
  `creado_por` varchar(120) NOT NULL,
  `fecha_creacion` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_notificacion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `notificaciones_usuario` (
  `id_notificacion_usuario` int(11) NOT NULL AUTO_INCREMENT,
  `Usuario_idUsuario` int(11) NOT NULL,
  `titulo` varchar(120) NOT NULL,
  `mensaje` varchar(255) NOT NULL,
  `enlace` varchar(180) NOT NULL,
  `tipo` varchar(20) NOT NULL DEFAULT 'ok',
  `icono` varchar(6) NOT NULL DEFAULT 'M',
  `leida` tinyint(1) NOT NULL DEFAULT 0,
  `fecha_creacion` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_notificacion_usuario`),
  KEY `idx_notificacion_usuario` (`Usuario_idUsuario`,`fecha_creacion`),
  CONSTRAINT `fk_notificaciones_usuario_usuario` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- novedades: incluye las columnas del flujo de asignacion/reparacion que hoy
-- solo agrega NovedadesDAO.asegurarColumnasNovedad() en tiempo de ejecucion.
CREATE TABLE `novedades` (
  `idnovedades` int(11) NOT NULL AUTO_INCREMENT,
  `fecha_novedad` date NOT NULL,
  `hora_novedad` time NOT NULL,
  `tipo_falla` varchar(80) DEFAULT NULL,
  `descripcion_novedad` varchar(500) NOT NULL,
  `Sede_idSede` int(11) DEFAULT NULL,
  `area` varchar(100) DEFAULT NULL,
  `Ambientes_Aprendizaje_idAmbientes_de_aprendizaje` int(11) DEFAULT NULL,
  `Usuario_idUsuario` int(11) NOT NULL,
  `Ficha_Maquina_idMaquinas` int(11) NOT NULL,
  `estado_flujo` varchar(40) NOT NULL DEFAULT 'REPORTADA',
  `tecnico_asignado_id` int(11) DEFAULT NULL,
  `asignado_por_id` int(11) DEFAULT NULL,
  `fecha_asignacion` datetime DEFAULT NULL,
  `fecha_inicio_reparacion` datetime DEFAULT NULL,
  `fecha_fin_reparacion` datetime DEFAULT NULL,
  `observacion_reparacion` varchar(500) DEFAULT NULL,
  `observacion_gestion` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`idnovedades`),
  KEY `fk_novedades_Usuario1_idx` (`Usuario_idUsuario`),
  KEY `fk_novedades_Ficha_Maquina1_idx` (`Ficha_Maquina_idMaquinas`),
  KEY `fk_novedades_Sede1_idx` (`Sede_idSede`),
  KEY `fk_novedades_Ambiente1_idx` (`Ambientes_Aprendizaje_idAmbientes_de_aprendizaje`),
  CONSTRAINT `fk_novedades_Ambiente1` FOREIGN KEY (`Ambientes_Aprendizaje_idAmbientes_de_aprendizaje`) REFERENCES `ambientes_aprendizaje` (`idAmbientes_de_aprendizaje`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_novedades_Ficha_Maquina1` FOREIGN KEY (`Ficha_Maquina_idMaquinas`) REFERENCES `ficha_maquina` (`idMaquinas`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_novedades_Sede1` FOREIGN KEY (`Sede_idSede`) REFERENCES `sede` (`idSede`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_novedades_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `permiso` (
  `idPermiso` int(11) NOT NULL AUTO_INCREMENT,
  `codigo_permiso` varchar(45) NOT NULL,
  `descripcion` varchar(45) NOT NULL,
  PRIMARY KEY (`idPermiso`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `programacion_mantenimiento` (
  `idProgramacion_mantenimiento` int(11) NOT NULL,
  `tipo_maquina` varchar(45) NOT NULL,
  `fecha_programacion` datetime NOT NULL,
  `descripcion` varchar(45) NOT NULL,
  `novedades_idnovedades` int(11) NOT NULL,
  `Ambientes_Aprendizaje_idAmbientes_de_aprendizaje` int(11) NOT NULL,
  `Usuario_idUsuario` int(11) NOT NULL,
  PRIMARY KEY (`idProgramacion_mantenimiento`),
  KEY `fk_Programacion_mantenimiento_novedades1_idx` (`novedades_idnovedades`),
  KEY `fk_Programacion_mantenimiento_Ambientes_Aprendizaje1_idx` (`Ambientes_Aprendizaje_idAmbientes_de_aprendizaje`),
  KEY `fk_Programacion_mantenimiento_Usuario1_idx` (`Usuario_idUsuario`),
  CONSTRAINT `fk_Programacion_mantenimiento_Ambientes_Aprendizaje1` FOREIGN KEY (`Ambientes_Aprendizaje_idAmbientes_de_aprendizaje`) REFERENCES `ambientes_aprendizaje` (`idAmbientes_de_aprendizaje`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Programacion_mantenimiento_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Programacion_mantenimiento_novedades1` FOREIGN KEY (`novedades_idnovedades`) REFERENCES `novedades` (`idnovedades`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `registro_asistencia` (
  `idRegistro_Asistencia` int(11) NOT NULL,
  `descripcion_registro` varchar(45) NOT NULL,
  `Ambientes_Aprendizaje_idAmbientes de aprendizaje` int(11) NOT NULL,
  PRIMARY KEY (`idRegistro_Asistencia`),
  KEY `fk_Registro_Asistencia_Ambientes_Aprendizaje1_idx` (`Ambientes_Aprendizaje_idAmbientes de aprendizaje`),
  CONSTRAINT `fk_Registro_Asistencia_Ambientes_Aprendizaje1` FOREIGN KEY (`Ambientes_Aprendizaje_idAmbientes de aprendizaje`) REFERENCES `ambientes_aprendizaje` (`idAmbientes_de_aprendizaje`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `reporte_mantenimientos` (
  `idReporte_mantenimientos` int(11) NOT NULL AUTO_INCREMENT,
  `fecha_reporte` varchar(45) NOT NULL,
  `archivo_pdf` varchar(500) NOT NULL,
  `firma_digital` varchar(45) NOT NULL,
  `Mantenimiento_idMantenimiento` int(11) NOT NULL,
  PRIMARY KEY (`idReporte_mantenimientos`),
  KEY `fk_reporte_mantenimientos_mantenimiento_idx` (`Mantenimiento_idMantenimiento`),
  CONSTRAINT `fk_reporte_mantenimientos_mantenimiento` FOREIGN KEY (`Mantenimiento_idMantenimiento`) REFERENCES `mantenimiento` (`idMantenimiento`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

ALTER TABLE `ficha_maquina`
  ADD CONSTRAINT `fk_Ficha_Maquina_Reubicacion_Maquina1_deferred` FOREIGN KEY (`Reubicacion_Maquina_idRehubicacion_Maquina`) REFERENCES `reubicacion_maquina` (`idRehubicacion_Maquina`) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE TABLE `rol_permiso` (
  `idRol_Permiso` int(11) NOT NULL AUTO_INCREMENT,
  `Permiso_idPermiso` int(11) NOT NULL,
  `Rol_idRol` int(11) NOT NULL,
  PRIMARY KEY (`idRol_Permiso`),
  UNIQUE KEY `uk_rol_permiso` (`Rol_idRol`,`Permiso_idPermiso`),
  KEY `fk_Rol_Permiso_Permiso1_idx` (`Permiso_idPermiso`),
  KEY `fk_Rol_Permiso_Rol1_idx` (`Rol_idRol`),
  CONSTRAINT `fk_Rol_Permiso_Permiso1` FOREIGN KEY (`Permiso_idPermiso`) REFERENCES `permiso` (`idPermiso`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Rol_Permiso_Rol1` FOREIGN KEY (`Rol_idRol`) REFERENCES `rol` (`idRol`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `ubicacion_maquina` (
  `idUbicacion_Maquina` int(11) NOT NULL AUTO_INCREMENT,
  `Fecha` varchar(45) NOT NULL,
  `Ambiente` varchar(45) NOT NULL,
  PRIMARY KEY (`idUbicacion_Maquina`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `whatsapp_alerta_enviada` (
  `id_alerta` int(11) NOT NULL AUTO_INCREMENT,
  `clave_alerta` varchar(180) NOT NULL,
  `tipo_alerta` varchar(60) NOT NULL,
  `id_referencia` int(11) NOT NULL,
  `fecha_referencia` date DEFAULT NULL,
  `creado_en` datetime NOT NULL,
  PRIMARY KEY (`id_alerta`),
  UNIQUE KEY `uk_whatsapp_alerta_clave` (`clave_alerta`),
  KEY `idx_whatsapp_alerta_tipo_ref` (`tipo_alerta`,`id_referencia`),
  KEY `idx_whatsapp_alerta_fecha` (`fecha_referencia`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE VIEW `vista_cuentadantes_maquinas_asignadas` AS
SELECT
  mc.Usuario_idUsuario AS id_usuario,
  COALESCE(NULLIF(TRIM(CONCAT(COALESCE(u.nombre,''),' ',COALESCE(u.apellido,''))),''), CONCAT('Usuario ', mc.Usuario_idUsuario)) AS cuentadante,
  COALESCE(u.documento_identidad, 'Sin documento') AS documento,
  COALESCE(r.descripcion_rol, 'Sin rol') AS rol,
  COUNT(mc.idMaquina_Cuentadante) AS maquinas_asignadas
FROM maquina_cuentadante mc
LEFT JOIN usuario u ON u.idUsuario = mc.Usuario_idUsuario
LEFT JOIN rol r ON r.idRol = u.Rol_idRol
GROUP BY mc.Usuario_idUsuario, u.nombre, u.apellido, u.documento_identidad, r.descripcion_rol
ORDER BY COUNT(mc.idMaquina_Cuentadante) DESC, cuentadante;

SET FOREIGN_KEY_CHECKS = 1;
