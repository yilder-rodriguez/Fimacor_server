-- Casilla que habilita el Administrador para que un Instructor tambien pueda
-- actuar como Tecnico (se le pueden asignar reparaciones y mantenimientos).
-- Por defecto nadie la tiene, asi que el comportamiento actual no cambia.

ALTER TABLE `usuario` ADD COLUMN `actua_como_tecnico` TINYINT(1) NOT NULL DEFAULT 0;
