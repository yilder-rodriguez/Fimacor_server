package com.fimacor.comun.menu;

import com.fimacor.comun.seguridad.PermisoService;
import com.fimacor.usuarios.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.Normalizer;
import java.util.Enumeration;
import org.springframework.stereotype.Service;
import com.fimacor.mantenimiento.entity.Mantenimiento;

/**
 * Calcula la barra lateral unica (fragments/nav.html) que aparece en todas las vistas internas.
 * Los permisos de los modulos operativos se calculan aparte de MenuService a proposito: las
 * reglas de cada uno son ligeramente distintas y no deben mezclarse. Los accesos de
 * administracion (roles, configuracion, admin de maquinas/mantenimiento) replican las reglas
 * de MenuService para que la barra lateral muestre lo mismo que mostraba el menu principal.
 */
@Service
public class NavBarService {

    private final PermisoService permisoService;

    public NavBarService(PermisoService permisoService) {
        this.permisoService = permisoService;
    }

    public record NavBar(boolean puedeVerMaquinas, boolean puedeVerMantenimiento, boolean puedeProgramarMantenimiento,
            boolean puedeReubicarMaquinas, boolean puedeVerHistorialMantenimiento, boolean puedeVerReportes,
            boolean puedeVerUsuarios, boolean puedeAsignarMaquinas, boolean puedeVerMaquinasAsignadas,
            boolean puedeVerManuales, boolean puedeCrearUsuarios, boolean esLogistica, String paginaActual,
            String qrBaseOrigin, String usuarioCorreo, boolean esTecnico, boolean puedeVerRolesPermisos,
            boolean puedeGestionarSistema, boolean puedeAdministrarMaquinas, boolean puedeAdministrarMantenimiento,
            String usuarioNombre, String usuarioRol, String usuarioIniciales, boolean menuNovedades,
            boolean menuReubicacion, boolean puedeVerTablero) {

        public boolean activo(String pagina) {
            return pagina.equals(paginaActual);
        }
    }

    public NavBar construir(Usuario usuario, HttpServletRequest request) {
        int idRol = usuario != null && usuario.getRol() != null ? usuario.getRol().getIdRol() : 0;
        String nombreRol = usuario != null && usuario.getRol() != null ? usuario.getRol().getDescripcionRol() : "";
        String rolNorm = normalizar(nombreRol);

        boolean esTecnico = rolNorm.contains("tecnico");
        boolean esSubdireccion = rolNorm.contains("subdireccion");
        boolean esCuentadante = rolNorm.contains("cuentadante");
        boolean esInstructor = rolNorm.contains("instructor");
        boolean esLogistica = rolNorm.contains("logistica");
        boolean esAdministrador = rolNorm.contains("admin");

        boolean puedeVerUsuarios = permisoService.tienePermisoConTodas(idRol, "usuarios", "ver");
        boolean puedeCrearUsuarios = permisoService.tienePermisoConTodas(idRol, "usuarios", "crear");
        boolean puedeVerMaquinas = permisoService.tienePermisoConTodas(idRol, "maquinas", "ver");
        boolean puedeProgramarMantenimiento = !esLogistica && !esInstructor
                && (permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "programar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "programar"));
        boolean puedeVerMantenimiento = puedeProgramarMantenimiento || esLogistica;
        boolean puedeVerHistorialMantenimiento = puedeProgramarMantenimiento
                || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "historial")
                || permisoService.tienePermisoConTodas(idRol, "reparacion", "ver");
        boolean puedeVerReportes = !esTecnico && !esInstructor
                && (permisoService.tienePermisoConTodas(idRol, "novedades", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "novedad", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "reporte", "falla")
                || permisoService.tienePermisoConTodas(idRol, "reportes", "falla"));
        boolean puedeReubicarMaquinas = !esLogistica
                && (permisoService.tienePermisoConTodas(idRol, "reubicacion", "registrar")
                || permisoService.tienePermisoConTodas(idRol, "reubicacion", "reubicar")
                || permisoService.tienePermisoConTodas(idRol, "maquinas", "reubicar")
                || permisoService.tienePermisoConTodas(idRol, "maquina", "reubicar")
                || esSubdireccion);
        // El Instructor solo puede consultar manuales desde la ficha de cada maquina (por eso el
        // enlace de arriba lo lleva a una pantalla que solo le muestra una advertencia); tres
        // pruebas distintas lo reportaron como confuso, asi que se oculta el enlace para ese rol.
        boolean puedeVerManuales = !esTecnico && !esLogistica && !esInstructor && (permisoService.tienePermisoConTodas(idRol, "manuales", "ver")
                || permisoService.tienePermiso(idRol, "manual", "manuales"));
        boolean puedeAsignarMaquinas = permisoService.tienePermisoConTodas(idRol, "maquinas", "asignar");
        boolean puedeVerTablero = permisoService.tienePermisoConTodas(idRol, "tablero", "ver");
        boolean puedeVerMaquinasAsignadas = esCuentadante && permisoService.tienePermisoConTodas(idRol, "maquinas", "asignadas");

        // Mismas reglas que MenuService.calcular para los accesos de administracion.
        boolean puedeVerRolesPermisos = permisoService.tienePermisoConTodas(idRol, "roles", "gestionar")
                || permisoService.tienePermisoConTodas(idRol, "permisos", "gestionar") || esAdministrador;
        boolean puedeGestionarSistema = (permisoService.tienePermisoConTodas(idRol, "roles", "gestionar")
                && permisoService.tienePermisoConTodas(idRol, "configuracion", "gestionar")) || esAdministrador;
        boolean puedeAdministrarMaquinas = !esTecnico && !esLogistica
                && (permisoService.tienePermisoConTodas(idRol, "maquinas", "administrar")
                    || (permisoService.tienePermisoConTodas(idRol, "maquinas", "editar") && permisoService.tienePermisoConTodas(idRol, "maquinas", "eliminar"))
                    || esAdministrador);
        boolean puedeAdministrarMantenimiento = !esTecnico && !esLogistica && !esInstructor
                && (permisoService.tienePermisoConTodas(idRol, "mantenimiento", "administrar")
                    || permisoService.tienePermisoConTodas(idRol, "mantenimiento", "gestionar")
                    || permisoService.tienePermisoConTodas(idRol, "mantenimientos", "gestionar")
                    || esAdministrador);

        // El menu principal mostraba Novedad y Reubicacion con reglas algo mas amplias (Instructor
        // reporta novedades; Cuentadante y Subdireccion entran a reubicacion). La barra lateral unica
        // muestra la union de ambas reglas para que ningun rol pierda un acceso que ya tenia.
        boolean menuNovedades = !esTecnico && (permisoService.tienePermisoConTodas(idRol, "novedades", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "novedad", "reportar")
                || permisoService.tienePermisoConTodas(idRol, "reporte", "falla")
                || permisoService.tienePermisoConTodas(idRol, "reportes", "falla"));
        boolean menuReubicacion = !esLogistica && (puedeReubicarMaquinas || esCuentadante || esSubdireccion);

        String nombre = usuario != null ? ((usuario.getNombre() == null ? "" : usuario.getNombre().trim()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido().trim())).trim() : "";
        String iniciales = iniciales(usuario);

        return new NavBar(puedeVerMaquinas, puedeVerMantenimiento, puedeProgramarMantenimiento, puedeReubicarMaquinas,
                puedeVerHistorialMantenimiento, puedeVerReportes, puedeVerUsuarios, puedeAsignarMaquinas,
                puedeVerMaquinasAsignadas, puedeVerManuales, puedeCrearUsuarios, esLogistica, request.getRequestURI(),
                origenPublico(request), usuario != null ? usuario.getCorreo() : "", esTecnico, puedeVerRolesPermisos,
                puedeGestionarSistema, puedeAdministrarMaquinas, puedeAdministrarMantenimiento,
                nombre, nombreRol, iniciales, menuNovedades, menuReubicacion, puedeVerTablero);
    }

    private String iniciales(Usuario usuario) {
        if (usuario == null) return "U";
        String n = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String a = usuario.getApellido() == null ? "" : usuario.getApellido().trim();
        String resultado = (n.isEmpty() ? "" : n.substring(0, 1)) + (a.isEmpty() ? "" : a.substring(0, 1));
        return resultado.isEmpty() ? "U" : resultado.toUpperCase();
    }

    /** Origen HTTPS publico usado para construir los enlaces del QR. */
    private String origenPublico(HttpServletRequest request) {
        String basePublica = System.getenv("FIMACOR_PUBLIC_BASE_URL");
        if (basePublica != null) {
            basePublica = basePublica.trim().replaceAll("/+$", "");
            if (basePublica.matches("^https?://[A-Za-z0-9.-]+(?::[0-9]+)?$")) {
                return basePublica;
            }
        }
        String dominioRailway = System.getenv("RAILWAY_PUBLIC_DOMAIN");
        if (dominioRailway != null && dominioRailway.matches("^[A-Za-z0-9.-]+$")) {
            return "https://" + dominioRailway;
        }

        String protocolo = cabeceraPublica(request, "X-Forwarded-Proto", request.getScheme());
        String host = cabeceraPublica(request, "X-Forwarded-Host", request.getHeader("Host"));
        if (host == null || host.trim().isEmpty()) host = request.getServerName();
        host = host.split(",")[0].trim();
        int puerto = request.getServerPort();
        if (host.contains(":")) puerto = -1;
        if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host)) {
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface interfaz = interfaces.nextElement();
                    if (!interfaz.isUp() || interfaz.isLoopback() || interfaz.isVirtual()) continue;
                    Enumeration<InetAddress> direcciones = interfaz.getInetAddresses();
                    while (direcciones.hasMoreElements()) {
                        InetAddress direccion = direcciones.nextElement();
                        if (direccion instanceof Inet4Address && direccion.isSiteLocalAddress()) {
                            host = direccion.getHostAddress();
                            break;
                        }
                    }
                    if (!"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host) && !"::1".equals(host)) break;
                }
            } catch (Exception e) {
                host = request.getServerName();
            }
        }
        boolean puertoDefault = puerto < 0 || ("http".equalsIgnoreCase(protocolo) && puerto == 80)
                || ("https".equalsIgnoreCase(protocolo) && puerto == 443);
        return protocolo + "://" + host + (puertoDefault ? "" : ":" + puerto);
    }

    private String cabeceraPublica(HttpServletRequest request, String nombre, String defecto) {
        String valor = request.getHeader(nombre);
        if (valor == null || valor.trim().isEmpty()) return defecto;
        valor = valor.split(",")[0].trim();
        return valor.matches("^[A-Za-z0-9:.-]+$") ? valor : defecto;
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
