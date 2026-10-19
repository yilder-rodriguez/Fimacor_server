-- El catalogo "Ambientes" (y "Areas", aun no migrado) permite asociar una sede,
-- pero esa columna solo existe en el legado por DDL en tiempo de ejecucion
-- (Configuracion.jsp -> asegurarSedeEnCatalogos()). Se agrega aqui como su
-- propia version en vez de editar V1 ya aplicado.

ALTER TABLE `ambientes_aprendizaje` ADD COLUMN `Sede_idSede` INT NULL;
ALTER TABLE `area` ADD COLUMN `Sede_idSede` INT NULL;

UPDATE `ambientes_aprendizaje` SET `Sede_idSede` = (SELECT idSede FROM `sede` WHERE UPPER(nombre_sede) = 'COMPLEJO SUR' LIMIT 1) WHERE `Sede_idSede` IS NULL;
UPDATE `area` SET `Sede_idSede` = (SELECT idSede FROM `sede` WHERE UPPER(nombre_sede) = 'COMPLEJO SUR' LIMIT 1) WHERE `Sede_idSede` IS NULL;
