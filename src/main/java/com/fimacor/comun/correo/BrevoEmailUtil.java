package com.fimacor.comun.correo;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Entrega los correos por Brevo en Railway, sin alterar el SMTP usado localmente.
 * La clave nunca se almacena en el proyecto: se lee exclusivamente del entorno.
 */
public final class BrevoEmailUtil {

    private static final String ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private BrevoEmailUtil() {
    }

    public static boolean estaActivo() {
        String apiKey = variable("BREVO_API_KEY");
        return !apiKey.isEmpty() && (!variable("RAILWAY_ENVIRONMENT").isEmpty()
                || !variable("RAILWAY_PUBLIC_DOMAIN").isEmpty());
    }

    /**
     * Devuelve true cuando el mensaje fue enviado por Brevo; false deja que el
     * llamador use SMTP local.
     */
    public static boolean enviarSiEstaActivo(MimeMessage mensaje) throws Exception {
        if (!estaActivo()) {
            return false;
        }

        mensaje.saveChanges();
        Address[] remitentes = mensaje.getFrom();
        if (remitentes == null || remitentes.length == 0 || !(remitentes[0] instanceof InternetAddress)) {
            throw new IllegalStateException("El correo no tiene remitente para Brevo.");
        }

        InternetAddress remitenteOriginal = (InternetAddress) remitentes[0];
        String correoRemitente = configurar("BREVO_SENDER_EMAIL", remitenteOriginal.getAddress());
        String nombreRemitente = configurar("BREVO_SENDER_NAME", remitenteOriginal.getPersonal());
        if (nombreRemitente.isEmpty()) {
            nombreRemitente = "FIMACOR | SENA";
        }

        List<String> destinatarios = direcciones(mensaje.getRecipients(Message.RecipientType.TO));
        if (destinatarios.isEmpty()) {
            throw new IllegalStateException("El correo no tiene destinatario para Brevo.");
        }

        ContenidoCorreo contenido = new ContenidoCorreo();
        extraerContenido(mensaje.getContent(), contenido);
        if (contenido.html.isEmpty()) {
            contenido.html = "<p>FIMACOR</p>";
        }

        StringBuilder json = new StringBuilder(512);
        json.append('{')
                .append("\"sender\":{\"email\":\"").append(escapar(correoRemitente))
                .append("\",\"name\":\"").append(escapar(nombreRemitente)).append("\"},")
                .append("\"to\":[");
        for (int i = 0; i < destinatarios.size(); i++) {
            if (i > 0) json.append(',');
            json.append("{\"email\":\"").append(escapar(destinatarios.get(i))).append("\"}");
        }
        json.append("],\"subject\":\"").append(escapar(mensaje.getSubject())).append("\"")
                .append(",\"htmlContent\":\"").append(escapar(contenido.html)).append("\"");
        if (!contenido.adjuntos.isEmpty()) {
            json.append(",\"attachment\":[");
            for (int i = 0; i < contenido.adjuntos.size(); i++) {
                if (i > 0) json.append(',');
                Adjunto adjunto = contenido.adjuntos.get(i);
                json.append("{\"content\":\"").append(adjunto.base64)
                        .append("\",\"name\":\"").append(escapar(adjunto.nombre)).append("\"}");
            }
            json.append(']');
        }
        json.append('}');

        HttpURLConnection conexion = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        conexion.setRequestMethod("POST");
        conexion.setConnectTimeout(20000);
        conexion.setReadTimeout(30000);
        conexion.setDoOutput(true);
        conexion.setRequestProperty("accept", "application/json");
        conexion.setRequestProperty("content-type", "application/json; charset=UTF-8");
        conexion.setRequestProperty("api-key", variable("BREVO_API_KEY"));
        byte[] cuerpo = json.toString().getBytes(StandardCharsets.UTF_8);
        conexion.setFixedLengthStreamingMode(cuerpo.length);
        try (OutputStream salida = conexion.getOutputStream()) {
            salida.write(cuerpo);
        }

        int estado = conexion.getResponseCode();
        if (estado < 200 || estado >= 300) {
            throw new IllegalStateException("Brevo no pudo enviar el correo (HTTP " + estado + "): "
                    + leerRespuesta(conexion));
        }
        return true;
    }

    private static void extraerContenido(Object objeto, ContenidoCorreo contenido) throws Exception {
        if (objeto instanceof Multipart) {
            Multipart multipart = (Multipart) objeto;
            for (int i = 0; i < multipart.getCount(); i++) {
                extraerParte(multipart.getBodyPart(i), contenido);
            }
        } else if (objeto != null && contenido.html.isEmpty()) {
            contenido.html = String.valueOf(objeto);
        }
    }

    private static void extraerParte(BodyPart parte, ContenidoCorreo contenido) throws Exception {
        if (parte.isMimeType("multipart/*")) {
            extraerContenido(parte.getContent(), contenido);
            return;
        }
        if (parte.isMimeType("text/html")) {
            if (contenido.html.isEmpty()) contenido.html = String.valueOf(parte.getContent());
            return;
        }
        String nombre = parte.getFileName();
        boolean adjunto = Part.ATTACHMENT.equalsIgnoreCase(parte.getDisposition()) || nombre != null;
        // Los logos CID son solo decorativos; Brevo no conserva el cid de JavaMail.
        if (adjunto && parte.getHeader("Content-ID") == null) {
            contenido.adjuntos.add(new Adjunto(nombre == null ? "adjunto" : nombre, leerBytes(parte.getInputStream())));
        }
    }

    private static List<String> direcciones(Address[] direcciones) {
        List<String> resultado = new ArrayList<>();
        if (direcciones == null) return resultado;
        for (Address direccion : direcciones) {
            if (direccion instanceof InternetAddress) {
                resultado.add(((InternetAddress) direccion).getAddress());
            }
        }
        return resultado;
    }

    private static byte[] leerBytes(InputStream entrada) throws Exception {
        try (InputStream flujo = entrada; ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            byte[] bloque = new byte[8192];
            int leidos;
            while ((leidos = flujo.read(bloque)) != -1) salida.write(bloque, 0, leidos);
            return salida.toByteArray();
        }
    }

    private static String leerRespuesta(HttpURLConnection conexion) {
        try {
            InputStream entrada = conexion.getErrorStream();
            if (entrada == null) return "sin detalle";
            String respuesta = new String(leerBytes(entrada), StandardCharsets.UTF_8).trim();
            return respuesta.length() > 500 ? respuesta.substring(0, 500) : respuesta;
        } catch (Exception e) {
            return "sin detalle";
        }
    }

    private static String configurar(String nombre, String defecto) {
        String valor = variable(nombre);
        return valor.isEmpty() ? (defecto == null ? "" : defecto.trim()) : valor;
    }

    private static String variable(String nombre) {
        String valor = System.getProperty(nombre);
        if (valor == null || valor.trim().isEmpty()) valor = System.getenv(nombre);
        return valor == null ? "" : valor.trim();
    }

    private static String escapar(String valor) {
        if (valor == null) return "";
        StringBuilder resultado = new StringBuilder(valor.length() + 16);
        for (int i = 0; i < valor.length(); i++) {
            char caracter = valor.charAt(i);
            switch (caracter) {
                case '\\': resultado.append("\\\\"); break;
                case '"': resultado.append("\\\""); break;
                case '\n': resultado.append("\\n"); break;
                case '\r': resultado.append("\\r"); break;
                case '\t': resultado.append("\\t"); break;
                default:
                    if (caracter < 0x20) resultado.append(String.format("\\u%04x", (int) caracter));
                    else resultado.append(caracter);
            }
        }
        return resultado.toString();
    }

    private static final class ContenidoCorreo {
        private String html = "";
        private final List<Adjunto> adjuntos = new ArrayList<>();
    }

    private static final class Adjunto {
        private final String nombre;
        private final String base64;

        private Adjunto(String nombre, byte[] datos) {
            this.nombre = nombre;
            this.base64 = Base64.getEncoder().encodeToString(datos);
        }
    }
}
