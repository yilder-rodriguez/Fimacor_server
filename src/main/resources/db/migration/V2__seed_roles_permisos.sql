-- Datos de referencia del sistema (roles y permisos), no datos de negocio.
-- Necesarios para que un entorno nuevo tenga con que autenticar/autorizar.
-- No se siembran usuarios, maquinas ni catalogos especificos del centro SENA:
-- esos son datos reales de produccion, no datos de referencia del sistema.

INSERT INTO `rol` (`idRol`, `descripcion_rol`) VALUES
  (1,'Administrador'),(2,'Aprendiz'),(3,'Instructor'),(4,'Logistica'),(5,'Tecnico'),(6,'Cuentadante'),(7,'Subdireccion');

INSERT INTO `permiso` (`idPermiso`, `codigo_permiso`, `descripcion`) VALUES
  (1,'usuarios.ver','Ver usuarios'),(2,'usuarios.crear','Crear usuarios'),(3,'usuarios.editar','Editar usuarios'),
  (4,'usuarios.eliminar','Eliminar usuarios'),(5,'roles.gestionar','Gestionar roles'),(6,'permisos.gestionar','Gestionar permisos'),
  (7,'maquinas.ver','Ver maquinas'),(8,'maquinas.crear','Crear maquinas'),(9,'maquinas.editar','Editar maquinas'),
  (10,'maquinas.eliminar','Eliminar maquinas'),(11,'maquinas.reubicar','Reubicar maquinas'),(12,'maquinas.asignar','Asignar maquinas'),
  (13,'maquinas.asignadas','Ver maquinas asignadas'),(14,'mantenimiento.ver','Ver mantenimiento'),(15,'mantenimiento.gestionar','Gestionar mantenimiento'),
  (16,'mantenimiento.programar','Programar mantenimiento'),(17,'mantenimiento.confirmar','Confirmar reparaciones'),(18,'mantenimiento.historial','Ver historial mantenimiento'),
  (19,'manuales.ver','Ver manuales'),(20,'manuales.subir','Subir manuales'),(21,'manuales.editar','Editar manuales'),
  (22,'manuales.eliminar','Eliminar manuales'),(23,'novedades.ver','Ver novedades'),(24,'novedades.reportar','Reportar novedades'),
  (25,'reubicacion.ver','Ver reubicaciones'),(26,'reubicacion.registrar','Registrar reubicaciones'),(27,'reparacion.ver','Ver reparaciones'),
  (28,'reparacion.reparar','Registrar reparaciones'),(29,'configuracion.ver','Ver configuracion'),(30,'configuracion.gestionar','Gestionar configuracion'),
  (31,'reportes.ver','Ver reportes'),(32,'perfil.ver','Ver perfil'),(33,'perfil.editar','Editar perfil'),
  (34,'usuarios.estado','Activar o inactivar usuarios'),(35,'maquinas.administrar','Administrar maquinas'),(36,'mantenimiento.administrar','Administrar mantenimiento'),
  (37,'mantenimiento.eliminar','Eliminar mantenimiento'),(38,'maquina.crear','Crear maquina'),(39,'maquina.registrar','Registrar maquina'),
  (40,'maquina.editar','Editar maquina'),(41,'maquina.eliminar','Eliminar maquina'),(42,'maquina.reubicar','Reubicar maquina'),
  (43,'reubicacion.reubicar','Reubicar maquinas'),(44,'reportes.falla','Reportar fallas'),(45,'reporte.falla','Reportar falla'),
  (46,'mantenimientos.gestionar','Gestionar mantenimientos'),(47,'mantenimientos.programar','Programar mantenimientos');

INSERT INTO `rol_permiso` (`Permiso_idPermiso`, `Rol_idRol`) VALUES
  (1,1),(2,1),(3,1),(4,1),(5,1),(6,1),(7,1),(8,1),(9,1),(10,1),(11,1),(12,1),(13,1),(14,1),(15,1),(16,1),(17,1),(18,1),(19,1),(20,1),
  (21,1),(22,1),(23,1),(24,1),(25,1),(26,1),(27,1),(28,1),(29,1),(30,1),(31,1),(32,1),(33,1),(34,1),(35,1),(36,1),(37,1),(38,1),(39,1),(40,1),
  (41,1),(42,1),(43,1),(44,1),(45,1),(46,1),(47,1),
  (7,2),(19,2),(24,2),(32,2),(33,2),(44,2),(45,2),
  (7,3),(14,3),(16,3),(19,3),(23,3),(24,3),(32,3),(33,3),(44,3),(45,3),(46,3),(47,3),
  (7,4),(8,4),(9,4),(11,4),(14,4),(15,4),(16,4),(18,4),(19,4),(20,4),(21,4),(23,4),(25,4),(26,4),(27,4),(29,4),(30,4),(31,4),(32,4),(35,4),
  (36,4),(38,4),(39,4),(40,4),(42,4),(43,4),(44,4),(45,4),(46,4),(47,4),
  (7,5),(14,5),(15,5),(17,5),(18,5),(19,5),(23,5),(27,5),(28,5),(32,5),(36,5),(46,5),
  (13,6),(24,6),(32,6),(33,6),(44,6),(45,6),
  (1,7),(2,7),(3,7),(4,7),(5,7),(6,7),(7,7),(8,7),(9,7),(10,7),(11,7),(12,7),(13,7),(14,7),(15,7),(16,7),(17,7),(18,7),(19,7),(20,7),
  (21,7),(22,7),(23,7),(24,7),(25,7),(26,7),(27,7),(28,7),(29,7),(30,7),(31,7),(32,7),(33,7),(34,7),(35,7),(36,7),(37,7),(38,7),(39,7),(40,7),
  (41,7),(42,7),(43,7),(44,7),(45,7),(46,7),(47,7);
