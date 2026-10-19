-- Decision de los directivos (validada 02/10/2026): Subdireccion ya no tiene los mismos
-- permisos que el Administrador. Solo consulta maquinas y aprueba los traslados externos
-- (la aprobacion de traslados no depende de la tabla de permisos: se valida por rol).

DELETE rp FROM `rol_permiso` rp
JOIN `rol` r ON r.`idRol` = rp.`Rol_idRol`
JOIN `permiso` p ON p.`idPermiso` = rp.`Permiso_idPermiso`
WHERE r.`descripcion_rol` = 'Subdireccion'
  AND p.`codigo_permiso` NOT IN ('maquinas.ver', 'reubicacion.ver', 'perfil.ver', 'perfil.editar');

INSERT IGNORE INTO `rol_permiso` (`Permiso_idPermiso`, `Rol_idRol`)
SELECT p.`idPermiso`, r.`idRol`
FROM `permiso` p
JOIN `rol` r ON r.`descripcion_rol` = 'Subdireccion'
WHERE p.`codigo_permiso` IN ('maquinas.ver', 'reubicacion.ver', 'perfil.ver', 'perfil.editar');
