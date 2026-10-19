-- Centro de ayuda: manuales de usuario del sistema (PDF) y enlaces a videos para quien maneja la aplicacion.
-- No es lo mismo que los manuales de cada maquina (tabla manuales).
-- Los PDF se guardan en la propia base de datos para que no dependan de carpetas del servidor ni se pierdan al desplegar.
-- idRol: a que rol va dirigido; NULL = para todos.

CREATE TABLE `ayuda_recurso` (
  `idAyuda` int NOT NULL AUTO_INCREMENT,
  `titulo` varchar(120) NOT NULL,
  `descripcion` varchar(300) NULL,
  `tipo` varchar(10) NOT NULL,
  `fuente` varchar(10) NOT NULL,
  `enlace` varchar(500) NULL,
  `archivo` mediumblob NULL,
  `nombre_archivo` varchar(150) NULL,
  `idRol` int NULL,
  `creado_por` varchar(150) NULL,
  `creado_en` datetime NOT NULL,
  PRIMARY KEY (`idAyuda`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
