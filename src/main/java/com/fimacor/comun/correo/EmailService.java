package com.fimacor.comun.correo;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envio de correos usando JavaMailSender de Spring. Las credenciales SMTP
 * se leen de las variables de entorno FIMACOR_SMTP_USER/FIMACOR_SMTP_PASSWORD
 * (ver application.yml), nunca quedan escritas en el codigo.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCodigoRegistro(String destinatario, String nombreUsuario, String codigo) throws Exception {
        enviarConCodigo(destinatario, "Verificación de cuenta - Sistema FIMACOR",
                "Gracias por registrarte en el sistema <strong>FIMACOR</strong>. Para activar tu cuenta ingresa el siguiente código de verificación:",
                nombreUsuario, codigo);
    }

    public void enviarCodigoRecuperacion(String destinatario, String nombreUsuario, String codigo) throws Exception {
        enviarConCodigo(destinatario, "Restablecimiento de contraseña - Sistema FIMACOR",
                "Hemos recibido una solicitud para restablecer tu contraseña en el sistema FIMACOR.",
                nombreUsuario, codigo);
    }

    public void enviarCodigoAccesoAdministrativo(String destinatario, String nombreUsuario, String codigo) throws Exception {
        enviarConCodigo(destinatario, "Código de acceso administrativo - Sistema FIMACOR",
                "Se solicitó un acceso rápido administrativo para crear un usuario en el sistema FIMACOR. Si no fuiste tú, ignora este mensaje.",
                nombreUsuario, codigo);
    }

    private void enviarConCodigo(String destinatario, String asunto, String parrafo, String nombreUsuario, String codigo) throws Exception {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setFrom(remitente(), "FIMACOR | SENA");

        String html = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #ddd;border-radius:10px;overflow:hidden'>"
                + "<div style='background:#0d1b2a;padding:20px;text-align:center'>"
                + "<img src='cid:logoFimacor' style='width:100%;max-width:280px'/>"
                + "</div>"
                + "<div style='padding:25px;background:#fff'>"
                + "<p>Hola, <strong>" + html(nombreUsuario) + "</strong></p>"
                + "<p style='color:#555'>" + parrafo + "</p>"
                + "<div style='background:#f4f4f4;border-left:4px solid #2E7D32;padding:15px;font-size:26px;font-weight:bold;letter-spacing:6px;text-align:center;margin:15px 0'>"
                + html(codigo) + "</div>"
                + "<p style='color:#555'>Este código es válido por <strong>5 minutos</strong>. Si no fuiste tú, puedes ignorar este mensaje.</p>"
                + "<hr style='border:none;border-top:1px solid #ddd;margin:15px 0'/>"
                + "<p style='color:#555;font-size:13px'>Equipo FIMACOR<br>Servicio Nacional de Aprendizaje - SENA</p>"
                + "</div>"
                + "<div style='background:#0d1b2a;padding:15px;text-align:center;font-size:11px;color:#aaa'>"
                + "Este es un mensaje automático, por favor no responder."
                + "</div>"
                + "</div>";

        helper.setText(html, true);
        try {
            helper.addInline("logoFimacor", new ClassPathResource("static/Img/Logoemblema.png"));
        } catch (Exception ignorado) {
            // El correo se envia igual sin el logo si el recurso no esta disponible.
        }
        if (!BrevoEmailUtil.enviarSiEstaActivo(mensaje)) {
            mailSender.send(mensaje);
        }
    }

    public record Adjunto(String nombre, Resource contenido) {
        public static Adjunto pdf(String nombre, byte[] bytes) {
            return new Adjunto(nombre, new ByteArrayResource(bytes));
        }

        public static Adjunto png(String nombre, byte[] bytes) {
            return new Adjunto(nombre, new ByteArrayResource(bytes));
        }
    }

    /** Envia el correo de solicitud de reubicacion o el de acta finalizada, segun el caso. */
    public void enviarReubicacion(String destinatario, String nombreDestinatario, String asunto, String parrafo,
            List<String[]> filas, List<Adjunto> adjuntos) throws Exception {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
        helper.setTo(destinatario);
        helper.setSubject(asunto + " - FIMACOR");
        helper.setFrom(remitente(), "FIMACOR | Reubicaciones");

        StringBuilder filasHtml = new StringBuilder();
        for (String[] fila : filas) {
            filasHtml.append("<tr><td style='padding:10px;border:1px solid #e5e7eb;background:#f8faf8;font-weight:bold;width:34%'>")
                    .append(html(fila[0])).append("</td><td style='padding:10px;border:1px solid #e5e7eb'>")
                    .append(html(fila[1] == null || fila[1].isBlank() ? "Sin dato" : fila[1])).append("</td></tr>");
        }

        String html = "<div style='font-family:Arial,sans-serif;max-width:640px;margin:auto;border:1px solid #ddd;border-radius:10px;overflow:hidden'>"
                + "<div style='background:#0d1b2a;padding:20px;text-align:center'>"
                + "<img src='cid:logoFimacor' style='width:100%;max-width:260px'/>"
                + "</div>"
                + "<div style='padding:25px;background:#fff'>"
                + "<p>Hola, <strong>" + html(nombreDestinatario) + "</strong></p>"
                + "<p style='color:#555'>" + parrafo + "</p>"
                + "<table style='width:100%;border-collapse:collapse;margin:18px 0;color:#333'>" + filasHtml + "</table>"
                + "<p style='color:#555'>La aprobación o el rechazo se gestionan desde la campanita de notificaciones de FIMACOR.</p>"
                + "<hr style='border:none;border-top:1px solid #ddd;margin:18px 0'/>"
                + "<p style='color:#555;font-size:13px'>Equipo FIMACOR<br>Servicio Nacional de Aprendizaje - SENA</p>"
                + "</div>"
                + "<div style='background:#0d1b2a;padding:15px;text-align:center;font-size:11px;color:#aaa'>"
                + "2026 FIMACOR - Sistema de Control de Máquinas Textiles<br>Este es un mensaje automático, por favor no responder."
                + "</div>"
                + "</div>";

        helper.setText(html, true);
        try {
            helper.addInline("logoFimacor", new ClassPathResource("static/Img/Sena.png"));
        } catch (Exception ignorado) {
            // El correo se envia igual sin el logo si el recurso no esta disponible.
        }
        for (Adjunto adjunto : adjuntos) {
            if (adjunto != null && adjunto.contenido() != null) {
                helper.addAttachment(adjunto.nombre(), adjunto.contenido());
            }
        }
        if (!BrevoEmailUtil.enviarSiEstaActivo(mensaje)) {
            mailSender.send(mensaje);
        }
    }

    private String remitente() {
        String usuario = System.getenv("FIMACOR_SMTP_USER");
        return usuario == null || usuario.isBlank() ? "noreply@fimacor.local" : usuario;
    }

    private String html(String valor) {
        return valor == null ? "" : valor
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
