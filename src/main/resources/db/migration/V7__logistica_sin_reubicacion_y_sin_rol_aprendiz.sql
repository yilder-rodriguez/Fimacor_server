-- Decisiones de negocio acordadas con los directivos:
--   1) Logistica no realiza reubicaciones de maquinas.
--   2) El rol Aprendiz deja de existir en el sistema.

DELETE rp FROM `rol_permiso` rp
JOIN `permiso` p ON p.`idPermiso` = rp.`Permiso_idPermiso`
JOIN `rol` r ON r.`idRol` = rp.`Rol_idRol`
WHERE r.`descripcion_rol` = 'Logistica'
  AND (p.`codigo_permiso` LIKE 'reubicacion.%' OR p.`codigo_permiso` IN ('maquinas.reubicar', 'maquina.reubicar'));

-- Las cuentas que aun tengan el rol Aprendiz no se borran (tienen historial asociado
-- en otras tablas): se inactivan para que nadie entre con un rol que ya no existe.
UPDATE `usuario` SET `activo` = 0
WHERE `Rol_idRol` IN (SELECT `idRol` FROM `rol` WHERE `descripcion_rol` = 'Aprendiz');

DELETE FROM `rol_permiso`
WHERE `Rol_idRol` IN (SELECT `idRol` FROM `rol` WHERE `descripcion_rol` = 'Aprendiz');

-- Solo se elimina el rol si ya no queda ningun usuario apuntando a el.
DELETE FROM `rol`
WHERE `descripcion_rol` = 'Aprendiz'
  AND NOT EXISTS (SELECT 1 FROM `usuario` u WHERE u.`Rol_idRol` = `rol`.`idRol`);
