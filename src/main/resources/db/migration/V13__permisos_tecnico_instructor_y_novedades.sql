-- Auditoria de roles (05/10/2026), contra la matriz real de produccion.
--
-- 1) El Tecnico (profesional y pasante) solo registra las reparaciones que le asignan, pero heredo del
--    sistema original mantenimiento.administrar, mantenimiento.gestionar y mantenimientos.gestionar.
--    Con ellos podia programar mantenimientos, reprogramarlos, cambiar el tecnico, cancelarlos e iniciar
--    mantenimientos asignados a otros tecnicos. Se le quitan.
-- 2) El Instructor solo reporta novedades (y, con la casilla del Administrador, atiende lo que le asignen
--    como tecnico). Podia programar mantenimientos por URL directa con mantenimiento.programar y
--    mantenimientos.programar. Se le quitan esos y mantenimiento.ver.
-- 3) Los permisos novedades.gestionar/asignar y novedad.gestionar/asignar existian en produccion pero no
--    en ninguna migracion (se habian creado a mano); se registran aqui para que una base nueva los tenga.
--    En produccion esta parte no cambia nada.

DELETE rp FROM `rol_permiso` rp
JOIN `permiso` p ON p.`idPermiso` = rp.`Permiso_idPermiso`
JOIN `rol` r ON r.`idRol` = rp.`Rol_idRol`
WHERE (r.`descripcion_rol` IN ('Tecnico profesional', 'Tecnico pasante')
        AND p.`codigo_permiso` IN ('mantenimiento.administrar', 'mantenimiento.gestionar', 'mantenimientos.gestionar'))
   OR (r.`descripcion_rol` = 'Instructor'
        AND p.`codigo_permiso` IN ('mantenimiento.ver', 'mantenimiento.programar', 'mantenimientos.programar'));

INSERT INTO `permiso` (`codigo_permiso`, `descripcion`)
SELECT v.codigo, v.descripcion FROM (
    SELECT 'novedades.gestionar' AS codigo, 'Gestionar novedades' AS descripcion
    UNION ALL SELECT 'novedades.asignar', 'Asignar tecnico a novedad'
    UNION ALL SELECT 'novedad.gestionar', 'Gestionar novedad'
    UNION ALL SELECT 'novedad.asignar', 'Asignar tecnico a novedad'
) v
WHERE NOT EXISTS (SELECT 1 FROM `permiso` x WHERE x.`codigo_permiso` = v.codigo);

INSERT IGNORE INTO `rol_permiso` (`Permiso_idPermiso`, `Rol_idRol`)
SELECT p.`idPermiso`, r.`idRol`
FROM `permiso` p
JOIN `rol` r ON r.`descripcion_rol` IN ('Administrador', 'Logistica')
WHERE p.`codigo_permiso` IN ('novedades.gestionar', 'novedades.asignar', 'novedad.gestionar', 'novedad.asignar');
