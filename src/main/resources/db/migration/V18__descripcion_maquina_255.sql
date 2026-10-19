-- La descripcion de la maquina pasa de 45 a 255 caracteres: las hojas de inventario de SENA traen descripciones
-- tecnicas largas (marca, motor, tipo de puntada...) que se recortaban al importarlas.
ALTER TABLE `ficha_maquina` MODIFY COLUMN `descripcion` VARCHAR(255) NOT NULL;
