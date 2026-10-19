package com.fimacor.importacion.service;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.UsuarioRepository;
import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Importacion de usuarios, novedades y mantenimientos desde las plantillas de Excel. Igual que la de maquinas, tiene
 * dos tiempos: analizar (lee, valida y dice que pasaria, sin escribir nada) e importar (todo en una sola transaccion:
 * o entra todo o no entra nada). No envia notificaciones ni cambia el estado de las maquinas.
 */
@Service
public class ImportacionDatosService {

    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern HORA = Pattern.compile("^(\\d{1,2})\\s*[:.hH]\\s*(\\d{2})$");
    private static final Pattern FECHA_HORA = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})[ T](\\d{1,2}):(\\d{2}).*$");
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom AZAR = new SecureRandom();
    private static final String AUTOR = "Importación de datos";
    // Las fechas y horas se pasan como java.time (no java.sql.Date/Timestamp): asi se guardan tal cual, sin que la zona horaria
    // del equipo las corra de hora (la conexion usa serverTimezone=UTC).

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;
    private final UsuarioRepository usuarioRepository;
    private final PermisoService permisoService;
    private final PasswordEncoder passwordEncoder;

    public ImportacionDatosService(JdbcTemplate jdbc, PlatformTransactionManager gestor, UsuarioRepository usuarioRepository,
            PermisoService permisoService, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.transaccion = new TransactionTemplate(gestor);
        this.usuarioRepository = usuarioRepository;
        this.permisoService = permisoService;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================================================================================
    // Modelos para la pantalla (campos publicos: Thymeleaf los lee directo)
    // =====================================================================================================

    public static class FilaDatos implements Serializable {
        public int fila;
        public Map<String, String> v = new LinkedHashMap<>();
        public String accion = "NUEVA"; // NUEVA, OMITE, ERROR
        public List<String> avisos = new ArrayList<>();
        public List<String> errores = new ArrayList<>();
        public Map<String, Object> r = new HashMap<>(); // valores ya resueltos (ids, fechas...)
        public String etiqueta = "";
    }

    public static class AnalisisDatos implements Serializable {
        public String token = java.util.UUID.randomUUID().toString();
        public String tipo = "";
        public String nombreTipo = "";
        public String archivo = "";
        public int totalFilas, nuevas, omitidas, conError, conAvisos;
        public List<FilaDatos> filas = new ArrayList<>();
        public List<String> columnasDetectadas = new ArrayList<>();
        public List<String> columnasIgnoradas = new ArrayList<>();
        public List<String> camposMuestra = new ArrayList<>();
        public List<String> titulosMuestra = new ArrayList<>();
    }

    public static class ResultadoDatos implements Serializable {
        public String tipo = "";
        public String nombreTipo = "";
        public int creadas, omitidas;
        public List<String> avisos = new ArrayList<>();
        /** Contrasenas generadas (nombre, correo, rol, contrasena): solo para usuarios, se entregan una vez. */
        public List<String[]> credenciales = new ArrayList<>();
    }

    // =====================================================================================================
    // Analisis
    // =====================================================================================================

    public AnalisisDatos analizar(TipoImportacion tipo, String nombreArchivo, byte[] datos) throws IOException {
        List<Columna> columnas = Definiciones.columnas(tipo);
        List<List<String>> hoja = LectorHoja.leer(nombreArchivo, datos);

        Map<String, String> alias = new HashMap<>();
        for (Columna c : columnas) {
            alias.put(ImportacionService.normal(c.titulo()), c.campo());
            for (String a : c.alias()) alias.putIfAbsent(ImportacionService.normal(a), c.campo());
        }
        int filaTitulos = buscarTitulos(hoja, alias);
        if (filaTitulos < 0) {
            throw new IOException("No encontré los títulos de las columnas. Usa la plantilla de " + tipo.nombre.toLowerCase()
                    + " (botón \"Descargar plantilla\") y no cambies los títulos de la primera fila.");
        }

        AnalisisDatos a = new AnalisisDatos();
        a.tipo = tipo.clave;
        a.nombreTipo = tipo.nombre;
        a.archivo = nombreArchivo;
        List<String> titulos = hoja.get(filaTitulos);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < titulos.size(); i++) {
            String t = titulos.get(i);
            if (t.isBlank()) continue;
            String campo = alias.get(ImportacionService.normal(t));
            if (campo == null) a.columnasIgnoradas.add(t);
            else if (!col.containsKey(campo)) col.put(campo, i);
        }
        List<String> faltan = new ArrayList<>();
        for (Columna c : columnas) {
            if (col.containsKey(c.campo())) a.columnasDetectadas.add(c.titulo() + "  ←  columna \"" + titulos.get(col.get(c.campo())) + "\"");
            else if (c.obligatoria()) faltan.add(c.titulo());
        }
        if (!faltan.isEmpty()) {
            throw new IOException("Faltan columnas obligatorias: " + String.join(", ", faltan) + ". Usa la plantilla de " + tipo.nombre.toLowerCase() + ".");
        }
        a.camposMuestra = Definiciones.camposMuestra(tipo);
        for (String campo : a.camposMuestra) {
            columnas.stream().filter(c -> c.campo().equals(campo)).findFirst().ifPresent(c -> a.titulosMuestra.add(c.titulo()));
        }

        List<FilaDatos> filas = new ArrayList<>();
        for (int r = filaTitulos + 1; r < hoja.size(); r++) {
            List<String> celdas = hoja.get(r);
            if (celdas.stream().allMatch(String::isBlank)) continue;
            FilaDatos f = new FilaDatos();
            f.fila = r + 1;
            for (Columna c : columnas) {
                Integer i = col.get(c.campo());
                f.v.put(c.campo(), i == null || i >= celdas.size() ? "" : limpiar(celdas.get(i)));
            }
            filas.add(f);
        }
        if (filas.isEmpty()) throw new IOException("El archivo no tiene filas de datos debajo de los títulos.");
        if (filas.size() > LectorHoja.MAX_FILAS) {
            throw new IOException("El archivo tiene " + filas.size() + " filas; el máximo por importación es " + LectorHoja.MAX_FILAS + ".");
        }

        switch (tipo) {
            case USUARIOS -> validarUsuarios(filas);
            case NOVEDADES -> validarNovedades(filas);
            case MANTENIMIENTOS -> validarMantenimientos(filas);
            default -> throw new IOException("Este tipo se importa por otro camino.");
        }

        for (FilaDatos f : filas) {
            if (!f.errores.isEmpty()) f.accion = "ERROR";
            switch (f.accion) {
                case "ERROR" -> a.conError++;
                case "OMITE" -> a.omitidas++;
                default -> a.nuevas++;
            }
            if (!f.avisos.isEmpty()) a.conAvisos++;
        }
        a.totalFilas = filas.size();
        a.filas = filas;
        return a;
    }

    private static int buscarTitulos(List<List<String>> hoja, Map<String, String> alias) {
        int mejor = -1, mejorCuenta = 1;
        for (int i = 0; i < Math.min(15, hoja.size()); i++) {
            Set<String> campos = new HashSet<>();
            for (String t : hoja.get(i)) {
                if (t.isBlank()) continue;
                String campo = alias.get(ImportacionService.normal(t));
                if (campo != null) campos.add(campo);
            }
            if (campos.size() > mejorCuenta) { mejor = i; mejorCuenta = campos.size(); }
        }
        return mejor;
    }

    // =====================================================================================================
    // Usuarios
    // =====================================================================================================

    private void validarUsuarios(List<FilaDatos> filas) {
        Map<String, Object[]> roles = new LinkedHashMap<>();
        jdbc.query("SELECT idRol, descripcion_rol FROM rol ORDER BY idRol", rs -> {
            roles.putIfAbsent(ImportacionService.clave(rs.getString(2)), new Object[] {rs.getInt(1), rs.getString(2).trim()});
        });
        Set<String> correosBd = new HashSet<>(), documentosBd = new HashSet<>();
        jdbc.query("SELECT LOWER(TRIM(correo)), TRIM(documento_identidad) FROM usuario", rs -> {
            correosBd.add(rs.getString(1));
            documentosBd.add(soloAlfanumerico(rs.getString(2)));
        });
        Set<String> correosVistos = new HashSet<>(), documentosVistos = new HashSet<>();

        for (FilaDatos f : filas) {
            String nombre = f.v.get("nombre"), apellido = f.v.get("apellido");
            if (nombre.isEmpty()) f.errores.add("Falta el nombre.");
            if (apellido.isEmpty()) f.errores.add("Faltan los apellidos.");
            if (nombre.length() > 45) f.errores.add("El nombre tiene más de 45 caracteres.");
            if (apellido.length() > 45) f.errores.add("Los apellidos tienen más de 45 caracteres.");
            f.etiqueta = (nombre + " " + apellido).trim();

            String correo = f.v.get("correo").toLowerCase();
            if (correo.isEmpty()) f.errores.add("Falta el correo electrónico.");
            else if (!CORREO.matcher(correo).matches()) f.errores.add("El correo \"" + correo + "\" no es válido.");
            else if (correo.length() > 45) f.errores.add("El correo tiene más de 45 caracteres.");
            f.v.put("correo", correo);

            String documento = soloAlfanumerico(f.v.get("documento"));
            if (documento.isEmpty()) f.errores.add("Falta el número de documento.");
            else if (documento.length() > 45) f.errores.add("El número de documento es demasiado largo.");
            f.v.put("documento", documento);

            String telefono = f.v.get("telefono");
            if (telefono.length() > 45) f.errores.add("El teléfono tiene más de 45 caracteres.");

            String tipoDoc = tipoDocumento(f.v.get("tipoDoc"));
            if (tipoDoc == null) f.errores.add("Tipo de documento no válido: \"" + f.v.get("tipoDoc") + "\". Usa CC, TI o CE.");
            f.v.put("tipoDoc", tipoDoc == null ? f.v.get("tipoDoc") : tipoDoc);

            Object[] rol = buscarRol(roles, f.v.get("rol"));
            boolean esInstructor = false;
            if (f.v.get("rol").isEmpty()) f.errores.add("Falta el rol.");
            else if (rol == null) f.errores.add("Rol no reconocido: \"" + f.v.get("rol") + "\". Roles válidos: " + roles.values().stream().map(o -> (String) o[1]).toList() + ".");
            else {
                esInstructor = ((String) rol[1]).toLowerCase().contains("instructor");
                f.r.put("idRol", rol[0]);
                f.v.put("rol", (String) rol[1]);
                if (((String) rol[1]).toLowerCase().contains("administrador")) f.avisos.add("Se creará con rol Administrador (acceso total al sistema).");
            }
            boolean actuaTecnico = esAfirmativo(f.v.get("actuaTecnico"));
            if (actuaTecnico && !esInstructor && rol != null) {
                f.avisos.add("\"Instructor también es técnico\" solo aplica a Instructores: se ignoró.");
                actuaTecnico = false;
            }
            f.r.put("esInstructor", esInstructor);
            f.r.put("actuaTecnico", actuaTecnico);

            String clave = f.v.get("contrasena");
            if (!clave.isEmpty() && (clave.length() < 8 || clave.length() > 72)) f.errores.add("La contraseña inicial debe tener entre 8 y 72 caracteres (o déjala vacía para que se genere).");

            if (!correo.isEmpty() && !correosVistos.add(correo)) f.errores.add("El correo " + correo + " ya aparece antes en el archivo.");
            if (!documento.isEmpty() && !documentosVistos.add(documento)) f.errores.add("El documento " + documento + " ya aparece antes en el archivo.");

            if (f.errores.isEmpty()) {
                boolean porCorreo = correosBd.contains(correo), porDocumento = documentosBd.contains(documento);
                if (porCorreo || porDocumento) {
                    f.accion = "OMITE";
                    f.avisos.add("Ya existe un usuario con ese " + (porCorreo ? "correo" : "documento") + ": no se modifica.");
                }
            }
        }
    }

    private static Object[] buscarRol(Map<String, Object[]> roles, String texto) {
        String k = ImportacionService.clave(texto);
        if (k.isEmpty()) return null;
        if (roles.containsKey(k)) return roles.get(k);
        Object[] unico = null;
        for (Map.Entry<String, Object[]> e : roles.entrySet()) {
            if (e.getKey().contains(k) || k.contains(e.getKey())) {
                if (unico != null) return null;
                unico = e.getValue();
            }
        }
        return unico;
    }

    private static String tipoDocumento(String texto) {
        String n = ImportacionService.normal(texto);
        if (n.isEmpty() || n.equals("cc") || n.startsWith("cedula de ciudadania") || n.equals("cedula")) return "CC";
        if (n.equals("ti") || n.startsWith("tarjeta")) return "TI";
        if (n.equals("ce") || n.startsWith("cedula de extranjeria")) return "CE";
        return null;
    }

    private ResultadoDatos importarUsuarios(AnalisisDatos a) {
        ResultadoDatos res = nuevoResultado(a);
        Set<String> codigosUsados = new HashSet<>();
        for (FilaDatos f : a.filas) {
            if (!f.errores.isEmpty()) continue;
            if (f.accion.equals("OMITE")) { res.omitidas++; continue; }
            String documento = f.v.get("documento");
            List<Integer> docs = jdbc.queryForList("SELECT idDocumento_Identidad FROM documento_identidad WHERE numero_documento = ? LIMIT 1", Integer.class, documento);
            int idDoc;
            if (docs.isEmpty()) {
                idDoc = insertarConId("INSERT INTO documento_identidad (descripcion_Documento, numero_documento) VALUES (?, ?)", f.v.get("tipoDoc"), documento);
            } else {
                idDoc = docs.get(0);
            }
            String clave = f.v.get("contrasena");
            boolean generada = clave.isEmpty();
            if (generada) clave = generarContrasena();
            boolean instructor = Boolean.TRUE.equals(f.r.get("esInstructor"));
            String codigo = instructor ? generarCodigoInstructor(f.v.get("nombre"), codigosUsados) : null;
            jdbc.update("INSERT INTO usuario (nombre, apellido, documento_identidad, numero_telefono, correo, contrasena_usuario, fecha_expiracion, "
                    + "Rol_idRol, Documento_Identidad_idDocumento_Identidad, activo, codigo_instructor, actua_como_tecnico) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)",
                    f.v.get("nombre"), f.v.get("apellido"), documento, f.v.get("telefono"), f.v.get("correo"), passwordEncoder.encode(clave),
                    LocalDate.now().plusYears(1).atStartOfDay(), f.r.get("idRol"), idDoc, codigo,
                    Boolean.TRUE.equals(f.r.get("actuaTecnico")));
            res.creadas++;
            if (generada) res.credenciales.add(new String[] {f.etiqueta, f.v.get("correo"), f.v.get("rol"), clave});
        }
        return res;
    }

    private String generarContrasena() {
        String letras = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz", digitos = "23456789", simbolos = "#$%&*+";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(letras.charAt(AZAR.nextInt(letras.length())));
        for (int i = 0; i < 3; i++) sb.append(digitos.charAt(AZAR.nextInt(digitos.length())));
        sb.append(simbolos.charAt(AZAR.nextInt(simbolos.length())));
        for (int i = 0; i < 2; i++) sb.append(letras.charAt(AZAR.nextInt(letras.length())));
        return sb.toString();
    }

    /** Mismo formato que el codigo de instructor del modulo de usuarios: 4 letras del nombre y 4 digitos. */
    private String generarCodigoInstructor(String nombre, Set<String> usados) {
        String letras = Normalizer.normalize(nombre == null ? "" : nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^A-Za-z]", "").toUpperCase();
        letras = (letras + "INS").substring(0, 4);
        for (int intento = 0; intento < 300; intento++) {
            String codigo = letras + String.format("%04d", AZAR.nextInt(10000));
            Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE UPPER(codigo_instructor) = ?", Integer.class, codigo);
            if ((existe == null || existe == 0) && usados.add(codigo)) return codigo;
        }
        String respaldo = "INS" + String.format("%05d", AZAR.nextInt(100000));
        usados.add(respaldo);
        return respaldo;
    }

    // =====================================================================================================
    // Novedades
    // =====================================================================================================

    private record Maquina(int id, boolean baja, String descripcion, Integer idSede, String area, Integer idAmbiente) {
    }

    private Map<String, Maquina> maquinas() {
        Map<String, Maquina> m = new HashMap<>();
        jdbc.query("SELECT idMaquinas, UPPER(TRIM(codigo_sena)), baja, descripcion, Sede_idSede, area, Ambientes_Aprendizaje_idAmbientes_de_aprendizaje FROM ficha_maquina", rs -> {
            m.put(rs.getString(2), new Maquina(rs.getInt(1), rs.getBoolean(3), rs.getString(4), (Integer) rs.getObject(5), rs.getString(6), (Integer) rs.getObject(7)));
        });
        return m;
    }

    private void validarNovedades(List<FilaDatos> filas) {
        Map<String, Maquina> maquinas = maquinas();
        Personas personas = personas();
        Set<String> existentes = new HashSet<>();
        jdbc.query("SELECT Ficha_Maquina_idMaquinas, fecha_novedad, descripcion_novedad FROM novedades", rs -> {
            existentes.add(rs.getInt(1) + "|" + rs.getDate(2).toLocalDate() + "|" + claveTexto(rs.getString(3)));
        });
        Set<String> vistas = new HashSet<>();
        LocalDate hoy = LocalDate.now();

        for (FilaDatos f : filas) {
            String codigo = f.v.get("codigo").toUpperCase();
            f.etiqueta = codigo;
            f.v.put("codigo", codigo);
            Maquina maquina = codigo.isEmpty() ? null : maquinas.get(codigo);
            if (codigo.isEmpty()) f.errores.add("Falta el código SENA de la máquina.");
            else if (maquina == null) f.errores.add("La máquina " + codigo + " no existe en el sistema. Impórtala antes con la plantilla de Máquinas.");

            LocalDate fecha = ImportacionService.parsearFecha(f.v.get("fecha"));
            if (f.v.get("fecha").isEmpty()) f.errores.add("Falta la fecha de la novedad.");
            else if (fecha == null) f.errores.add("La fecha \"" + f.v.get("fecha") + "\" no es válida (usa 2026-03-15 o 15/03/2026).");
            else if (fecha.isAfter(hoy.plusDays(1)) || fecha.getYear() < 2000) f.errores.add("La fecha " + fecha + " no es razonable.");

            LocalTime hora = horaOAviso(f, f.v.get("hora"), LocalTime.of(8, 0));
            String tipoFalla = tipoFalla(f.v.get("tipoFalla"));
            if (f.v.get("tipoFalla").isEmpty()) f.errores.add("Falta el tipo de falla.");
            else if (tipoFalla == null) f.errores.add("Tipo de falla no reconocido: \"" + f.v.get("tipoFalla") + "\". Usa Mecánica, Eléctrica, Electrónica u Otro.");

            String descripcion = f.v.get("descripcion");
            if (descripcion.isEmpty()) f.errores.add("Falta la descripción de la falla.");
            descripcion = recortarConAviso(f, "La descripción", descripcion, 500);

            Integer reporta = f.v.get("reportadoPor").isEmpty() ? null : personas.buscar(f.v.get("reportadoPor"));
            if (f.v.get("reportadoPor").isEmpty()) f.errores.add("Falta quién reportó la novedad.");
            else if (reporta == null) f.errores.add("No encontré al usuario que reporta (" + f.v.get("reportadoPor") + "). Impórtalo antes con la plantilla de Usuarios.");

            String estado = estadoNovedad(f.v.get("estado"));
            if (f.v.get("estado").isEmpty()) f.errores.add("Falta el estado de la novedad.");
            else if (estado == null) f.errores.add("Estado no reconocido: \"" + f.v.get("estado") + "\". Usa Reportada, Asignada, En reparación, Finalizada por técnico, Cerrada, Cancelada o Devuelta.");

            boolean exigeTecnico = "ASIGNADA".equals(estado) || "EN_REPARACION".equals(estado) || "FINALIZADA_TECNICO".equals(estado);
            Integer tecnico = null;
            String textoTecnico = f.v.get("tecnico");
            if (!textoTecnico.isEmpty()) {
                Integer id = personas.buscar(textoTecnico);
                if (id != null && personas.tecnicos.contains(id)) tecnico = id;
                else if (exigeTecnico) f.errores.add(id == null ? "No encontré al técnico (" + textoTecnico + "). Impórtalo antes con la plantilla de Usuarios."
                        : "El usuario " + textoTecnico + " no puede atender reparaciones (no es técnico).");
                else f.avisos.add("No encontré a un técnico válido (" + textoTecnico + "): se guardó sin técnico.");
            } else if (exigeTecnico) {
                f.errores.add("El estado " + f.v.get("estado") + " necesita el técnico asignado.");
            }
            if ("REPORTADA".equals(estado) && tecnico != null) { tecnico = null; f.avisos.add("Una novedad Reportada no lleva técnico: se ignoró."); }

            // fechas del flujo
            LocalDateTime base = fecha == null ? null : LocalDateTime.of(fecha, hora);
            LocalDateTime asignacion = null, inicio = null, fin = null;
            if (tecnico != null && base != null) {
                asignacion = fechaHoraOAviso(f, "fecha de asignación", f.v.get("fechaAsignacion"), base);
                boolean ejecutada = "EN_REPARACION".equals(estado) || "FINALIZADA_TECNICO".equals(estado) || "CERRADA".equals(estado);
                if (ejecutada) inicio = fechaHoraOAviso(f, "fecha de inicio de la reparación", f.v.get("fechaInicio"), asignacion);
                if ("FINALIZADA_TECNICO".equals(estado) || ("CERRADA".equals(estado))) fin = fechaHoraOAviso(f, "fecha de fin de la reparación", f.v.get("fechaFin"), inicio);
                if (asignacion != null && asignacion.isBefore(base)) f.avisos.add("La fecha de asignación es anterior a la de la novedad.");
                if (inicio != null && asignacion != null && inicio.isBefore(asignacion)) f.avisos.add("El inicio de la reparación es anterior a la asignación.");
                if (fin != null && inicio != null && fin.isBefore(inicio)) f.avisos.add("El fin de la reparación es anterior al inicio.");
            }
            String obsReparacion = recortarConAviso(f, "La observación de la reparación", f.v.get("obsReparacion"), 500);
            if (obsReparacion.isEmpty() && ("FINALIZADA_TECNICO".equals(estado) || ("CERRADA".equals(estado) && tecnico != null))) {
                obsReparacion = "Sin observación registrada.";
                f.avisos.add("Sin observación de la reparación: se guardó \"Sin observación registrada.\".");
            }
            String obsGestion = recortarConAviso(f, "La observación de gestión", f.v.get("obsGestion"), 500);

            f.r.put("idMaquina", maquina == null ? null : maquina.id());
            f.r.put("idSede", maquina == null ? null : maquina.idSede());
            f.r.put("area", maquina == null ? null : maquina.area());
            f.r.put("idAmbiente", maquina == null ? null : maquina.idAmbiente());
            f.r.put("fecha", fecha);
            f.r.put("hora", hora);
            f.r.put("tipoFalla", tipoFalla);
            f.r.put("descripcion", descripcion);
            f.r.put("reporta", reporta);
            f.r.put("estado", estado);
            f.r.put("tecnico", tecnico);
            f.r.put("asignacion", asignacion);
            f.r.put("inicio", inicio);
            f.r.put("fin", fin);
            f.r.put("obsReparacion", obsReparacion.isEmpty() ? null : obsReparacion);
            f.r.put("obsGestion", obsGestion.isEmpty() ? null : obsGestion);
            if (estado != null) f.v.put("estado", NOMBRES_ESTADO_NOVEDAD.getOrDefault(estado, f.v.get("estado")));
            if (tipoFalla != null) f.v.put("tipoFalla", tipoFalla);
            if (fecha != null) f.v.put("fecha", fecha.toString());

            if (f.errores.isEmpty()) {
                if (maquina.baja()) {
                    f.accion = "OMITE";
                    f.avisos.add("La máquina está dada de baja: no se registra la novedad.");
                } else {
                    String llave = maquina.id() + "|" + fecha + "|" + claveTexto(descripcion);
                    if (existentes.contains(llave)) { f.accion = "OMITE"; f.avisos.add("Esta novedad ya está registrada: se omite."); }
                    else if (!vistas.add(llave)) { f.accion = "OMITE"; f.avisos.add("Esta novedad se repite en el archivo: se omite."); }
                }
            }
        }
    }

    private static final Map<String, String> NOMBRES_ESTADO_NOVEDAD = Map.of(
            "REPORTADA", "Reportada", "ASIGNADA", "Asignada", "EN_REPARACION", "En reparación", "FINALIZADA_TECNICO", "Finalizada por técnico",
            "CERRADA", "Cerrada", "CANCELADA", "Cancelada", "DEVUELTA", "Devuelta");

    private static String estadoNovedad(String texto) {
        String n = ImportacionService.normal(texto);
        if (n.isEmpty()) return null;
        if (n.contains("en reparacion") || n.contains("en proceso")) return "EN_REPARACION";
        if (n.startsWith("finalizad") || n.startsWith("reparad")) return "FINALIZADA_TECNICO";
        if (n.startsWith("reportad")) return "REPORTADA";
        if (n.startsWith("asignad")) return "ASIGNADA";
        if (n.startsWith("cerrad")) return "CERRADA";
        if (n.startsWith("cancelad")) return "CANCELADA";
        if (n.startsWith("devuelt")) return "DEVUELTA";
        return null;
    }

    private static String tipoFalla(String texto) {
        String n = ImportacionService.normal(texto);
        if (n.startsWith("mecanic")) return "Mecanica";
        if (n.startsWith("electric")) return "Electrica";
        if (n.startsWith("electronic")) return "Electronica";
        if (n.equals("otro") || n.equals("otra") || n.equals("otros")) return "Otro";
        return null;
    }

    private ResultadoDatos importarNovedades(AnalisisDatos a) {
        ResultadoDatos res = nuevoResultado(a);
        for (FilaDatos f : a.filas) {
            if (!f.errores.isEmpty()) continue;
            if (f.accion.equals("OMITE")) { res.omitidas++; continue; }
            Map<String, Object> r = f.r;
            jdbc.update("INSERT INTO novedades (fecha_novedad, hora_novedad, tipo_falla, descripcion_novedad, Sede_idSede, area, "
                    + "Ambientes_Aprendizaje_idAmbientes_de_aprendizaje, Usuario_idUsuario, Ficha_Maquina_idMaquinas, estado_flujo, tecnico_asignado_id, "
                    + "asignado_por_id, fecha_asignacion, fecha_inicio_reparacion, fecha_fin_reparacion, observacion_reparacion, observacion_gestion) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, ?)",
                    r.get("fecha"), r.get("hora"), r.get("tipoFalla"), r.get("descripcion"),
                    r.get("idSede"), r.get("area"), r.get("idAmbiente"), r.get("reporta"), r.get("idMaquina"), r.get("estado"), r.get("tecnico"),
                    r.get("asignacion"), r.get("inicio"), r.get("fin"),
                    r.get("obsReparacion"), r.get("obsGestion"));
            res.creadas++;
        }
        return res;
    }

    // =====================================================================================================
    // Mantenimientos
    // =====================================================================================================

    private void validarMantenimientos(List<FilaDatos> filas) {
        Map<String, Maquina> maquinas = maquinas();
        Personas personas = personas();
        Map<String, Object[]> tipos = new LinkedHashMap<>();
        jdbc.query("SELECT idTipo_Manteniento, descripcion_matenimiento FROM tipo_manteniento ORDER BY idTipo_Manteniento", rs -> {
            tipos.putIfAbsent(ImportacionService.clave(rs.getString(2)), new Object[] {rs.getInt(1), rs.getString(2).trim()});
        });
        Set<String> existentes = new HashSet<>();
        jdbc.query("SELECT Ficha_Maquina_idMaquinas, fecha_mantenimiento, Tipo_Manteniento_idTipo_Manteniento, Usuario_idUsuario FROM mantenimiento", rs -> {
            existentes.add(rs.getInt(1) + "|" + rs.getDate(2).toLocalDate() + "|" + rs.getInt(3) + "|" + rs.getInt(4));
        });
        Set<String> vistos = new HashSet<>();

        for (FilaDatos f : filas) {
            String codigo = f.v.get("codigo").toUpperCase();
            f.etiqueta = codigo;
            f.v.put("codigo", codigo);
            Maquina maquina = codigo.isEmpty() ? null : maquinas.get(codigo);
            if (codigo.isEmpty()) f.errores.add("Falta el código SENA de la máquina.");
            else if (maquina == null) f.errores.add("La máquina " + codigo + " no existe en el sistema. Impórtala antes con la plantilla de Máquinas.");

            Object[] tipo = buscarTipo(tipos, f.v.get("tipo"));
            if (f.v.get("tipo").isEmpty()) f.errores.add("Falta el tipo de mantenimiento.");
            else if (tipo == null) f.errores.add("Tipo de mantenimiento no reconocido: \"" + f.v.get("tipo") + "\". Tipos del sistema: " + tipos.values().stream().map(o -> (String) o[1]).toList() + ".");

            LocalDate fecha = ImportacionService.parsearFecha(f.v.get("fecha"));
            if (f.v.get("fecha").isEmpty()) f.errores.add("Falta la fecha del mantenimiento.");
            else if (fecha == null) f.errores.add("La fecha \"" + f.v.get("fecha") + "\" no es válida (usa 2026-03-15 o 15/03/2026).");
            else if (fecha.getYear() < 2000 || fecha.getYear() > 2100) f.errores.add("La fecha " + fecha + " no es razonable.");

            LocalTime hora = horaOAviso(f, f.v.get("hora"), null);
            String frecuencia = frecuencia(f.v.get("frecuencia"));
            if (f.v.get("frecuencia").isEmpty()) f.errores.add("Falta la frecuencia.");
            else if (frecuencia == null) f.errores.add("Frecuencia no reconocida: \"" + f.v.get("frecuencia") + "\". Usa Mensual, Trimestral, Semestral o Anual.");

            Integer tecnico = null;
            if (f.v.get("tecnico").isEmpty()) f.errores.add("Falta el técnico responsable.");
            else {
                Integer id = personas.buscar(f.v.get("tecnico"));
                if (id == null) f.errores.add("No encontré al técnico (" + f.v.get("tecnico") + "). Impórtalo antes con la plantilla de Usuarios.");
                else if (!personas.tecnicos.contains(id)) f.errores.add("El usuario " + f.v.get("tecnico") + " no puede atender mantenimientos (debe ser Técnico, o Instructor marcado como técnico).");
                else tecnico = id;
            }

            String tareas = f.v.get("tareas");
            if (tareas.isEmpty()) f.errores.add("Faltan las tareas del mantenimiento.");
            tareas = recortarConAviso(f, "Las tareas", tareas, 255);

            String estado = estadoMantenimiento(f.v.get("estado"));
            if (f.v.get("estado").isEmpty()) f.errores.add("Falta el estado del mantenimiento.");
            else if (estado == null) f.errores.add("Estado no reconocido: \"" + f.v.get("estado") + "\". Usa Programado, En mantenimiento, Finalizado o Cerrado.");

            LocalDateTime programado = fecha == null ? null : LocalDateTime.of(fecha, hora == null ? LocalTime.of(8, 0) : hora);
            LocalDateTime fin = null;
            if (programado != null && ("FINALIZADO".equals(estado) || "CERRADO".equals(estado))) {
                fin = fechaHoraOAviso(f, "fecha de finalización", f.v.get("fechaFin"), programado);
                if (fin.isBefore(programado)) f.avisos.add("La fecha de finalización es anterior a la del mantenimiento.");
            }
            String observacion = recortarConAviso(f, "La observación", f.v.get("observacion"), 300);
            if (observacion.isEmpty() && ("FINALIZADO".equals(estado) || "CERRADO".equals(estado))) {
                observacion = "Sin observación registrada.";
                f.avisos.add("Sin observación del técnico: se guardó \"Sin observación registrada.\".");
            }
            LocalDate proximo = null;
            if (!f.v.get("proximo").isEmpty()) {
                proximo = ImportacionService.parsearFecha(f.v.get("proximo"));
                if (proximo == null) f.avisos.add("El próximo mantenimiento \"" + f.v.get("proximo") + "\" no es una fecha válida: se calculó con la frecuencia.");
            }
            if (proximo == null && fecha != null && frecuencia != null) proximo = proximaFecha(fecha, frecuencia);

            f.r.put("idMaquina", maquina == null ? null : maquina.id());
            f.r.put("descripcionMaquina", maquina == null ? "" : maquina.descripcion());
            f.r.put("idTipo", tipo == null ? null : tipo[0]);
            f.r.put("nombreTipo", tipo == null ? null : tipo[1]);
            f.r.put("fecha", fecha);
            f.r.put("hora", hora == null ? null : String.format("%02d:%02d", hora.getHour(), hora.getMinute()));
            f.r.put("frecuencia", frecuencia);
            f.r.put("tecnico", tecnico);
            f.r.put("nombreTecnico", tecnico == null ? null : personas.nombre(tecnico));
            f.r.put("tareas", tareas);
            f.r.put("estado", estado);
            f.r.put("programado", programado);
            f.r.put("fin", fin);
            f.r.put("observacion", observacion);
            f.r.put("proximo", proximo);
            if (tipo != null) f.v.put("tipo", (String) tipo[1]);
            if (fecha != null) f.v.put("fecha", fecha.toString());
            if (estado != null) f.v.put("estado", NOMBRES_ESTADO_MANT.getOrDefault(estado, f.v.get("estado")));

            if (f.errores.isEmpty()) {
                if (maquina.baja()) {
                    f.accion = "OMITE";
                    f.avisos.add("La máquina está dada de baja: no se registra el mantenimiento.");
                } else {
                    String llave = maquina.id() + "|" + fecha + "|" + tipo[0] + "|" + tecnico;
                    if (existentes.contains(llave)) { f.accion = "OMITE"; f.avisos.add("Este mantenimiento ya está registrado: se omite."); }
                    else if (!vistos.add(llave)) { f.accion = "OMITE"; f.avisos.add("Este mantenimiento se repite en el archivo: se omite."); }
                }
            }
        }
    }

    private static final Map<String, String> NOMBRES_ESTADO_MANT = Map.of(
            "ASIGNADO", "Programado", "EN MANTENIMIENTO", "En mantenimiento", "FINALIZADO", "Finalizado", "CERRADO", "Cerrado");

    private static Object[] buscarTipo(Map<String, Object[]> tipos, String texto) {
        String k = ImportacionService.clave(texto);
        if (k.isEmpty()) return null;
        if (tipos.containsKey(k)) return tipos.get(k);
        Object[] unico = null;
        for (Map.Entry<String, Object[]> e : tipos.entrySet()) {
            if (e.getKey().startsWith(k) || k.startsWith(e.getKey())) {
                if (unico != null) return null;
                unico = e.getValue();
            }
        }
        return unico;
    }

    private static String frecuencia(String texto) {
        String n = ImportacionService.normal(texto);
        if (n.startsWith("mensual")) return "mensual";
        if (n.startsWith("trimestral")) return "trimestral";
        if (n.startsWith("semestral")) return "semestral";
        if (n.startsWith("anual")) return "anual";
        return null;
    }

    private static LocalDate proximaFecha(LocalDate fecha, String frecuencia) {
        return switch (frecuencia) {
            case "trimestral" -> fecha.plusMonths(3);
            case "semestral" -> fecha.plusMonths(6);
            case "anual" -> fecha.plusYears(1);
            default -> fecha.plusMonths(1);
        };
    }

    private static String estadoMantenimiento(String texto) {
        String n = ImportacionService.normal(texto);
        if (n.isEmpty()) return null;
        if (n.startsWith("cerrad")) return "CERRADO";
        if (n.startsWith("finalizad") || n.startsWith("reparad") || n.startsWith("realizad") || n.startsWith("terminad")) return "FINALIZADO";
        if (n.contains("en mantenimiento") || n.contains("en proceso") || n.contains("en curso")) return "EN MANTENIMIENTO";
        if (n.startsWith("programad") || n.startsWith("asignad") || n.startsWith("pendient")) return "ASIGNADO";
        return null;
    }

    private ResultadoDatos importarMantenimientos(AnalisisDatos a) {
        ResultadoDatos res = nuevoResultado(a);
        Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(idHistorial_Mantenimiento), 0) FROM historial_mantenimiento", Integer.class);
        int idHistorial = max == null ? 0 : max;
        for (FilaDatos f : a.filas) {
            if (!f.errores.isEmpty()) continue;
            if (f.accion.equals("OMITE")) { res.omitidas++; continue; }
            Map<String, Object> r = f.r;
            String nombreTecnico = (String) r.get("nombreTecnico");
            String nombreTipo = (String) r.get("nombreTipo");
            String estado = (String) r.get("estado");
            LocalDate fecha = (LocalDate) r.get("fecha");
            LocalDate proximo = (LocalDate) r.get("proximo");
            String hora = (String) r.get("hora");
            String tareas = (String) r.get("tareas");
            LocalDateTime programado = (LocalDateTime) r.get("programado");
            LocalDateTime fin = (LocalDateTime) r.get("fin");

            jdbc.update("INSERT INTO mantenimiento (fecha_mantenimiento, fecha_proximo_mantenimiento, descripcion_Mantenimiento, frecuencia, hora_programada, tareas, "
                    + "evidencia_mantenimiento, Tipo_Manteniento_idTipo_Manteniento, Ficha_Maquina_idMaquinas, Usuario_idUsuario, programado_por_usuario_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, '', ?, ?, ?, NULL)",
                    fecha, proximo, recortar(nombreTipo + " | Asignado a " + nombreTecnico, 90), r.get("frecuencia"), hora, tareas,
                    r.get("idTipo"), r.get("idMaquina"), r.get("tecnico"));

            // El historial lleva el estado en el texto, igual que cuando el sistema programa y atiende el mantenimiento.
            String asignacion = programado.format(SELLO);
            StringBuilder texto = new StringBuilder("ESTADO: ASIGNADO | Tipo mantenimiento: " + nombreTipo + " | Asignado por: " + AUTOR
                    + " | Responsable asignado: " + nombreTecnico + " | Fecha asignacion: " + asignacion + " | Fecha programada: " + fecha
                    + (hora != null ? " | Hora programada: " + hora : "") + " | Proximo mantenimiento: " + proximo + " | Frecuencia: " + r.get("frecuencia")
                    + " | Tareas: " + tareas + " | Confirmacion tecnica: pendiente");
            String fechaFinal = proximo.toString();
            if (!estado.equals("ASIGNADO")) {
                texto = new StringBuilder(texto.toString().replace("ESTADO: ASIGNADO", "ESTADO: EN MANTENIMIENTO")).append(" | Inicio tecnico: ").append(asignacion);
            }
            if (estado.equals("FINALIZADO") || estado.equals("CERRADO")) {
                String momento = fin.format(SELLO);
                texto = new StringBuilder(texto.toString().replace("ESTADO: EN MANTENIMIENTO", "ESTADO: FINALIZADO"))
                        .append(" | Reparado por: ").append(nombreTecnico).append(" | Fecha reparacion: ").append(momento)
                        .append(" | Observacion tecnica: ").append(r.get("observacion"));
                fechaFinal = momento;
            }
            if (estado.equals("CERRADO")) {
                texto = new StringBuilder(texto.toString().replace("ESTADO: FINALIZADO", "ESTADO: CERRADO"))
                        .append(" | Cerrado por Logistica: ").append(AUTOR).append(" | Observacion cierre: Cerrado al importar los datos.");
            } else if (estado.equals("EN MANTENIMIENTO")) {
                fechaFinal = proximo.toString();
            }
            idHistorial++;
            jdbc.update("INSERT INTO historial_mantenimiento (idHistorial_Mantenimiento, tipo_maquina, fecha_inicio, fecha_final, tecnico_encargado, descripcion_accion, "
                    + "Ficha_Maquina_idMaquinas, Usuario_idUsuario) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    idHistorial, recortar((String) r.get("descripcionMaquina"), 45), asignacion, fechaFinal, recortar(nombreTecnico, 45),
                    recortar(texto.toString(), 1200), r.get("idMaquina"), r.get("tecnico"));
            res.creadas++;
        }
        return res;
    }

    // =====================================================================================================
    // Importar (punto de entrada)
    // =====================================================================================================

    public ResultadoDatos importar(AnalisisDatos a) {
        TipoImportacion tipo = TipoImportacion.de(a.tipo);
        return transaccion.execute(estado -> switch (tipo) {
            case USUARIOS -> importarUsuarios(a);
            case NOVEDADES -> importarNovedades(a);
            case MANTENIMIENTOS -> importarMantenimientos(a);
            default -> throw new IllegalStateException("Este tipo se importa por otro camino.");
        });
    }

    private ResultadoDatos nuevoResultado(AnalisisDatos a) {
        ResultadoDatos r = new ResultadoDatos();
        r.tipo = a.tipo;
        r.nombreTipo = a.nombreTipo;
        int mostrados = 0, extra = 0;
        for (FilaDatos f : a.filas) {
            if (f.errores.isEmpty() && f.accion.equals("OMITE")) {
                if (mostrados < 30) { r.avisos.add("Fila " + f.fila + " (" + f.etiqueta + "): " + String.join(" ", f.avisos)); mostrados++; } else extra++;
            }
        }
        if (extra > 0) r.avisos.add("…y " + extra + " filas más que se omitieron.");
        return r;
    }

    // =====================================================================================================
    // Personas (usuarios activos y quienes pueden atender como tecnico)
    // =====================================================================================================

    private static final class Personas {
        final Map<String, Integer> porCorreo = new HashMap<>(), porDocumento = new HashMap<>();
        final Map<Integer, List<String>> palabras = new LinkedHashMap<>();
        final Map<Integer, String> nombres = new HashMap<>();
        final Set<Integer> tecnicos = new HashSet<>();

        Integer buscar(String texto) {
            if (texto == null || texto.isBlank()) return null;
            String t = texto.trim();
            Integer id = porCorreo.get(t.toLowerCase());
            if (id != null) return id;
            String doc = soloAlfanumerico(t);
            if (!doc.isEmpty() && (id = porDocumento.get(doc)) != null) return id;
            List<String> buscadas = palabrasDe(t);
            if (buscadas.size() < 2) return null;
            Integer unico = null;
            for (Map.Entry<Integer, List<String>> e : palabras.entrySet()) {
                if (e.getValue().containsAll(buscadas) || buscadas.containsAll(e.getValue())) {
                    if (unico != null) return null; // ambiguo: mejor no elegir a la persona equivocada
                    unico = e.getKey();
                }
            }
            return unico;
        }

        String nombre(int id) {
            return nombres.getOrDefault(id, "");
        }
    }

    private Personas personas() {
        Personas p = new Personas();
        for (Usuario u : usuarioRepository.findAll()) {
            if (!u.isActivo() || u.getIdUsuario() == null) continue;
            int id = u.getIdUsuario();
            if (u.getCorreo() != null) p.porCorreo.putIfAbsent(u.getCorreo().trim().toLowerCase(), id);
            String doc = soloAlfanumerico(u.getDocumentoIdentidad());
            if (!doc.isEmpty()) p.porDocumento.putIfAbsent(doc, id);
            String nombre = ((u.getNombre() == null ? "" : u.getNombre()) + " " + (u.getApellido() == null ? "" : u.getApellido())).trim();
            p.nombres.put(id, nombre);
            p.palabras.put(id, palabrasDe(nombre));
            if (permisoService.puedeAtenderComoTecnico(u)) p.tecnicos.add(id);
        }
        return p;
    }

    private static List<String> palabrasDe(String texto) {
        String n = ImportacionService.normal(texto);
        if (n.isEmpty()) return List.of();
        List<String> l = new ArrayList<>(List.of(n.split(" ")));
        l.sort(String::compareTo);
        return l;
    }

    // =====================================================================================================
    // Utilidades
    // =====================================================================================================

    private int insertarConId(String sql, Object... parametros) {
        org.springframework.jdbc.support.KeyHolder llaves = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(con -> {
            java.sql.PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < parametros.length; i++) ps.setObject(i + 1, parametros[i]);
            return ps;
        }, llaves);
        Number n = llaves.getKey();
        if (n == null) throw new IllegalStateException("No se obtuvo el id del registro creado.");
        return n.intValue();
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.replace(' ', ' ').trim().replaceAll("\\s+", " ");
    }

    private static String soloAlfanumerico(String s) {
        return s == null ? "" : s.replaceAll("[^A-Za-z0-9]", "");
    }

    private static String recortar(String s, int max) {
        return s == null ? "" : (s.length() > max ? s.substring(0, max).trim() : s);
    }

    private static String recortarConAviso(FilaDatos f, String que, String valor, int max) {
        if (valor == null || valor.length() <= max) return valor == null ? "" : valor;
        f.avisos.add(que + " tenía " + valor.length() + " caracteres y se recortó a " + max + ".");
        return recortar(valor, max);
    }

    private static String claveTexto(String s) {
        String n = ImportacionService.normal(s).replace(" ", "");
        return n.length() > 80 ? n.substring(0, 80) : n;
    }

    private static boolean esAfirmativo(String t) {
        String n = ImportacionService.normal(t);
        return n.equals("si") || n.equals("s") || n.equals("x") || n.equals("1") || n.equals("true") || n.equals("verdadero");
    }

    private static LocalTime horaOAviso(FilaDatos f, String texto, LocalTime porDefecto) {
        if (texto == null || texto.isBlank()) return porDefecto;
        Matcher m = HORA.matcher(texto.trim());
        if (m.matches()) {
            int h = Integer.parseInt(m.group(1)), min = Integer.parseInt(m.group(2));
            if (h < 24 && min < 60) return LocalTime.of(h, min);
        }
        f.avisos.add("La hora \"" + texto + "\" no es válida (usa 14:30)" + (porDefecto == null ? ": se ignoró." : ": se usó " + porDefecto + "."));
        return porDefecto;
    }

    /** Lee una fecha (con hora opcional); si falta o no es valida usa el valor por defecto. */
    private static LocalDateTime fechaHoraOAviso(FilaDatos f, String nombre, String texto, LocalDateTime porDefecto) {
        if (texto == null || texto.isBlank()) return porDefecto;
        Matcher m = FECHA_HORA.matcher(texto.trim());
        if (m.matches()) {
            LocalDate d = ImportacionService.parsearFecha(m.group(1));
            int h = Integer.parseInt(m.group(2)), min = Integer.parseInt(m.group(3));
            if (d != null && h < 24 && min < 60) return d.atTime(h, min);
        }
        LocalDate d = ImportacionService.parsearFecha(texto);
        if (d != null && d.getYear() >= 2000 && d.getYear() <= 2100) {
            return porDefecto != null && porDefecto.toLocalDate().equals(d) ? porDefecto : d.atTime(porDefecto == null ? LocalTime.of(8, 0) : porDefecto.toLocalTime());
        }
        f.avisos.add("La " + nombre + " \"" + texto + "\" no es una fecha válida: se usó " + (porDefecto == null ? "ninguna" : porDefecto.toLocalDate()) + ".");
        return porDefecto;
    }
}
