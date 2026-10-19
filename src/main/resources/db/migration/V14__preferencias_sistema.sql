-- Preferencias generales del sistema que el Administrador cambia desde Configuracion.
-- Clave/valor para poder agregar nuevas opciones sin tocar el esquema.

CREATE TABLE `preferencia_sistema` (
  `clave` varchar(60) NOT NULL,
  `valor` varchar(255) NOT NULL,
  `actualizado_por` varchar(150) NULL,
  `actualizado_en` datetime NULL,
  PRIMARY KEY (`clave`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Zoom fijo al 100 % en las pantallas internas: desactivado hasta que el Administrador lo active.
-- (El login y las pantallas publicas del QR siempre lo tienen bloqueado.)
INSERT INTO `preferencia_sistema` (`clave`, `valor`) VALUES ('zoom.bloqueado', 'false');
