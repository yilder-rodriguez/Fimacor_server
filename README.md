# fimacor-spring

Migración incremental de FIMACOR (Java EE/JSP en `src/`, `web/`) a Spring Boot.
Ver el plan completo de migración para contexto de fases y orden de módulos.

## Estado actual

- **Fase 1 (bootstrap)**: proyecto Maven, datasource, esquema Flyway (`V1__baseline.sql` +
  `V2__seed_roles_permisos.sql`), seguridad (login por sesión, BCrypt, port del motor de
  permisos difuso de `AccesoPermiso.java`), página de login y `/menu` de prueba.
- Módulos de negocio (usuarios, máquinas, mantenimiento, etc.) aún no migrados — siguen
  viviendo en la app Java EE original (`src/`, `web/`).

## Correr en local

Requiere Java 17+, Maven, y una base de datos MySQL (puede ser un contenedor desechable).

```bash
docker run --rm -d --name fimacor-mysql-dev \
  -e MYSQL_ROOT_PASSWORD=fimacor \
  -e MYSQL_DATABASE=fimacor \
  -p 3306:3306 \
  mysql:8

# Variables minimas (mismas que usa la app Java EE via Conexion.java / .env.example)
export FIMACOR_DB_HOST=localhost
export FIMACOR_DB_PORT=3306
export FIMACOR_DB_NAME=fimacor
export FIMACOR_DB_USER=root
export FIMACOR_DB_PASSWORD=fimacor

mvn spring-boot:run
```

La app queda en `http://localhost:8081/login`. Flyway crea el esquema automáticamente en un
contenedor vacío (o se registra con `flyway baseline` sobre una base de datos existente que ya
tenga los datos de producción — ver `V1__baseline.sql`).

Para probar el login hace falta al menos un usuario en la tabla `usuario` con una contraseña ya
hasheada en BCrypt, o correr primero la app Java EE original contra la misma base de datos:
`UsuarioDAO.asegurarContrasenasHasheadas()` migra cualquier contraseña en texto plano a BCrypt
apenas arranca.

## Variables de entorno reconocidas

Las mismas que la app original (`.env.example` en la raíz del repo):
`FIMACOR_DB_URL` / `JDBC_DATABASE_URL` / `MYSQL_URL` / `DATABASE_URL` (o las discretas
`FIMACOR_DB_HOST`/`PORT`/`NAME`/`USER`/`PASSWORD`, o las de Railway `MYSQLHOST`/etc.),
`FIMACOR_SMTP_USER`, `FIMACOR_SMTP_PASSWORD`.
