-- El Instructor heredo del sistema original el permiso suelto "mantenimientos.gestionar"
-- (plural). El sistema compara permisos por texto de forma flexible, asi que lo tomaba como
-- "mantenimiento.gestionar" y, por URL directa, un Instructor podia confirmar mantenimientos
-- asignados a otros tecnicos y entrar a Admin Mant. (reprogramar, cambiar tecnico, cancelar).
-- Su trabajo con mantenimientos es solo programar (mantenimiento.programar / mantenimientos.programar),
-- que se conservan.

DELETE rp FROM `rol_permiso` rp
JOIN `permiso` p ON p.`idPermiso` = rp.`Permiso_idPermiso`
JOIN `rol` r ON r.`idRol` = rp.`Rol_idRol`
WHERE r.`descripcion_rol` = 'Instructor'
  AND p.`codigo_permiso` = 'mantenimientos.gestionar';
