-- Notificaciones de cambios de catalogo para la campanita del administrador
-- (legacy: ConfiguracionCatalogoServlet.asegurarTablaNotificaciones / Menu.jsp).
-- Ningun flujo escribe aqui todavia (ver comentario en ConfiguracionController);
-- se crea la tabla para que la lectura de la campanita ya quede lista.

CREATE TABLE `notificaciones_configuracion` (
  `id_notificacion` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `titulo` VARCHAR(120) NOT NULL,
  `mensaje` VARCHAR(255) NOT NULL,
  `enlace` VARCHAR(180) NOT NULL,
  `tipo` VARCHAR(20) NOT NULL DEFAULT 'admin',
  `icono` VARCHAR(6) NOT NULL DEFAULT 'A',
  `creado_por` VARCHAR(120) NOT NULL,
  `fecha_creacion` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
