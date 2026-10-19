-- Foto de perfil de cada persona (se sube desde el modal de perfil del Inicio).
-- Va en columnas aparte de foto_carnet, que es la evidencia del carne que ve el Administrador.
-- La imagen se guarda ya reducida (JPG de hasta 256 x 256), asi que pesa pocas decenas de KB.
-- foto_perfil_version sirve para que el navegador sepa cuando cambio (ETag) sin volver a descargarla.

ALTER TABLE `usuario`
  ADD COLUMN `foto_perfil` mediumblob NULL,
  ADD COLUMN `foto_perfil_version` bigint NULL;
