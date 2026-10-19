package com.fimacor.comun.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resuelve la URL/usuario/password de MySQL probando varias variables de
 * entorno en orden: FIMACOR_DB_URL, JDBC_DATABASE_URL, MYSQL_URL/DATABASE_URL
 * (formato mysql://), o las variables sueltas de Railway (MYSQLHOST/MYSQLPORT/...).
 */
@Configuration
public class FimacorDataSourceConfig {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String OPCIONES_MYSQL =
            "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";

    @Bean
    public DataSource dataSource() {
        ConfiguracionBD config = cargarConfiguracion();
        return DataSourceBuilder.create()
                .driverClassName(DRIVER)
                .url(config.url)
                .username(config.user)
                .password(config.password)
                .build();
    }

    private static ConfiguracionBD cargarConfiguracion() {
        String jdbcUrl = valor("FIMACOR_DB_URL");
        String urlOriginalConCredenciales = jdbcUrl;
        if (vacio(jdbcUrl)) {
            jdbcUrl = valor("JDBC_DATABASE_URL");
            urlOriginalConCredenciales = jdbcUrl;
        }
        if (vacio(jdbcUrl)) {
            urlOriginalConCredenciales = valor("MYSQL_URL");
            jdbcUrl = convertirMysqlUrl(urlOriginalConCredenciales);
        }
        if (vacio(jdbcUrl)) {
            urlOriginalConCredenciales = valor("DATABASE_URL");
            jdbcUrl = convertirMysqlUrl(urlOriginalConCredenciales);
        }

        String user = primerValor("FIMACOR_DB_USER", "MYSQLUSER", "MYSQL_USER");
        String password = primerValor("FIMACOR_DB_PASSWORD", "MYSQLPASSWORD", "MYSQL_PASSWORD");

        if (!vacio(jdbcUrl)) {
            CredencialesUrl credenciales = extraerCredenciales(urlOriginalConCredenciales);
            if (vacio(user)) user = credenciales.user;
            if (vacio(password)) password = credenciales.password;
            return new ConfiguracionBD(agregarOpciones(jdbcUrl), defecto(user, "root"), defecto(password, ""));
        }

        String host = primerValor("FIMACOR_DB_HOST", "MYSQLHOST", "MYSQL_HOST");
        String port = primerValor("FIMACOR_DB_PORT", "MYSQLPORT", "MYSQL_PORT");
        String database = primerValor("FIMACOR_DB_NAME", "MYSQLDATABASE", "MYSQL_DATABASE", "MYSQLDB");

        host = defecto(host, "localhost");
        port = defecto(port, "3306");
        database = defecto(database, "fimacor");
        user = defecto(user, "root");
        password = defecto(password, "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + OPCIONES_MYSQL;
        return new ConfiguracionBD(url, user, password);
    }

    private static String convertirMysqlUrl(String rawUrl) {
        if (vacio(rawUrl)) return "";
        String url = rawUrl.trim();
        if (url.startsWith("jdbc:mysql://")) return url;
        if (!url.startsWith("mysql://")) return "";

        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 3306;
            String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
            if (vacio(host) || vacio(database)) return "";
            return "jdbc:mysql://" + host + ":" + port + "/" + database;
        } catch (Exception e) {
            return "";
        }
    }

    private static CredencialesUrl extraerCredenciales(String jdbcUrl) {
        try {
            String normalizada = jdbcUrl.replaceFirst("^jdbc:", "");
            URI uri = URI.create(normalizada);
            String userInfo = uri.getUserInfo();
            if (vacio(userInfo)) return new CredencialesUrl("", "");
            String[] partes = userInfo.split(":", 2);
            String user = decodificar(partes[0]);
            String password = partes.length > 1 ? decodificar(partes[1]) : "";
            return new CredencialesUrl(user, password);
        } catch (Exception e) {
            return new CredencialesUrl("", "");
        }
    }

    private static String agregarOpciones(String jdbcUrl) {
        String separador = jdbcUrl.contains("?") ? "&" : "?";
        return jdbcUrl + separador + OPCIONES_MYSQL;
    }

    private static String primerValor(String... claves) {
        for (String clave : claves) {
            String valor = valor(clave);
            if (!vacio(valor)) return valor;
        }
        return "";
    }

    private static String valor(String clave) {
        String valor = System.getenv(clave);
        if (vacio(valor)) valor = System.getProperty(clave);
        return valor == null ? "" : valor.trim();
    }

    private static String defecto(String valor, String defecto) {
        return vacio(valor) ? defecto : valor.trim();
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private static String decodificar(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }

    private static class ConfiguracionBD {
        final String url;
        final String user;
        final String password;

        ConfiguracionBD(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }
    }

    private static class CredencialesUrl {
        final String user;
        final String password;

        CredencialesUrl(String user, String password) {
            this.user = user;
            this.password = password;
        }
    }
}
