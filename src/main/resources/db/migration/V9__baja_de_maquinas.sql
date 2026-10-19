-- Baja de maquinas (acordado con los directivos): una maquina nunca se elimina, se da de
-- baja conservando todo su historial. La Cuentadante la solicita desde una novedad y el
-- Administrador la ejecuta o la desestima.

ALTER TABLE `ficha_maquina`
  ADD COLUMN `baja` tinyint(1) NOT NULL DEFAULT 0,
  ADD COLUMN `fecha_baja` datetime NULL,
  ADD COLUMN `motivo_baja` varchar(500) NULL,
  ADD COLUMN `baja_por` varchar(120) NULL;

-- NINGUNA | PENDIENTE | APROBADA | DESESTIMADA
ALTER TABLE `novedades`
  ADD COLUMN `solicitud_baja` varchar(12) NOT NULL DEFAULT 'NINGUNA';

-- Registro inmutable de cada baja y reactivacion (quien, cuando y por que).
CREATE TABLE `baja_maquina_historial` (
  `idBajaHistorial` int(11) NOT NULL AUTO_INCREMENT,
  `Ficha_Maquina_idMaquinas` int(11) NOT NULL,
  `accion` varchar(20) NOT NULL,
  `motivo` varchar(500) NOT NULL,
  `usuario_id` int(11) NULL,
  `usuario_nombre` varchar(120) NOT NULL,
  `novedad_id` int(11) NULL,
  `fecha` datetime NOT NULL,
  PRIMARY KEY (`idBajaHistorial`),
  KEY `idx_baja_historial_maquina` (`Ficha_Maquina_idMaquinas`),
  CONSTRAINT `fk_baja_historial_maquina` FOREIGN KEY (`Ficha_Maquina_idMaquinas`) REFERENCES `ficha_maquina` (`idMaquinas`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
