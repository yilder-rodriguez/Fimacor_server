-- Codigo corto y unico para usuarios con rol Instructor, usado para validar
-- reportes de novedad desde el QR de una maquina sin necesitar login completo
-- (legacy: UsuarioDAO.codigoParaGuardar / consultarInstructorPorCodigo).

ALTER TABLE `usuario` ADD COLUMN `codigo_instructor` VARCHAR(8) NULL;
