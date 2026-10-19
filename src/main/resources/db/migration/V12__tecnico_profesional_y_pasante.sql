-- Decision de la reunion del 02/10/2026: el tecnico se divide en dos tipos.
--   Tecnico profesional: es el rol "Tecnico" de siempre (se conservan sus usuarios y permisos).
--   Tecnico pasante: nuevo rol con exactamente los mismos permisos que el profesional
--                    (registrar las reparaciones de las novedades que le asignan).

UPDATE `rol` SET `descripcion_rol` = 'Tecnico profesional' WHERE `descripcion_rol` = 'Tecnico';

INSERT INTO `rol` (`descripcion_rol`)
SELECT 'Tecnico pasante' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `rol` WHERE `descripcion_rol` = 'Tecnico pasante');

INSERT IGNORE INTO `rol_permiso` (`Permiso_idPermiso`, `Rol_idRol`)
SELECT rp.`Permiso_idPermiso`, pas.`idRol`
FROM `rol_permiso` rp
JOIN `rol` prof ON prof.`idRol` = rp.`Rol_idRol` AND prof.`descripcion_rol` = 'Tecnico profesional'
JOIN `rol` pas ON pas.`descripcion_rol` = 'Tecnico pasante';
