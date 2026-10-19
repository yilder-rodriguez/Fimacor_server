-- Tablero de gerencia: vista de indicadores del inventario de maquinas para quienes toman decisiones.
-- El Administrador lo ve siempre (rol total). Se deja asignado a Subdireccion y el Administrador puede darselo
-- a otros roles desde la pantalla de Roles y Permisos (permiso tablero.ver).

INSERT INTO `permiso` (`codigo_permiso`, `descripcion`)
SELECT 'tablero.ver', 'Ver el tablero de gerencia'
WHERE NOT EXISTS (SELECT 1 FROM `permiso` x WHERE x.`codigo_permiso` = 'tablero.ver');

INSERT IGNORE INTO `rol_permiso` (`Permiso_idPermiso`, `Rol_idRol`)
SELECT p.`idPermiso`, r.`idRol`
FROM `permiso` p
JOIN `rol` r ON r.`descripcion_rol` IN ('Subdireccion')
WHERE p.`codigo_permiso` = 'tablero.ver';
