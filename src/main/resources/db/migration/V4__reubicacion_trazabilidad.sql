-- El flujo de firma electronica de reubicacion ensancha varias columnas y
-- agrega firma_digital en tiempo de ejecucion (Reubicacion_MaquinaDAO.java,
-- asegurarCamposTrazabilidad). Se agrega aqui como su propia version.

ALTER TABLE `reubicacion_maquina` MODIFY `autorizacion` VARCHAR(160) NOT NULL;
ALTER TABLE `reubicacion_maquina` MODIFY `descripcion_rehubicacion` VARCHAR(500) NOT NULL;
ALTER TABLE `reubicacion_maquina` MODIFY `observacion` VARCHAR(500) NULL;
ALTER TABLE `reubicacion_maquina` MODIFY `respuesta_autorizacion` TEXT NULL;
ALTER TABLE `reubicacion_maquina` ADD COLUMN `firma_digital` MEDIUMBLOB NULL AFTER `respuesta_autorizacion`;
