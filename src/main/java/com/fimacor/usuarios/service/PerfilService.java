package com.fimacor.usuarios.service;

import com.fimacor.usuarios.entity.Usuario;
import com.fimacor.usuarios.repository.UsuarioRepository;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autoedicion del propio perfil desde el modal de Inicio: datos de contacto
 * y cambio de contrasena. A proposito no permite tocar correo ni rol, eso
 * sigue siendo exclusivo del panel de administracion de usuarios.
 */
@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    private static final int LADO_FOTO = 256;
    private static final int MAX_BYTES_FOTO = 10 * 1024 * 1024;
    private static final long MAX_PIXELES_FOTO = 50_000_000L;

    public PerfilService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    public record Resultado(boolean ok, String error, Usuario usuario) {
        static Resultado exito(Usuario usuario) {
            return new Resultado(true, null, usuario);
        }
        static Resultado fallo(String motivo) {
            return new Resultado(false, motivo, null);
        }
    }

    @Transactional
    public Resultado actualizarDatos(int idUsuario, String nombre, String apellido, String telefono) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) return Resultado.fallo("usuario");

        String nombreLimpio = limpiar(nombre);
        String apellidoLimpio = limpiar(apellido);
        String telefonoLimpio = limpiar(telefono);
        if (nombreLimpio.isEmpty() || apellidoLimpio.isEmpty() || telefonoLimpio.isEmpty()) return Resultado.fallo("datos");

        usuario.setNombre(limitar(nombreLimpio, 45));
        usuario.setApellido(limitar(apellidoLimpio, 45));
        usuario.setNumeroTelefono(limitar(telefonoLimpio, 45));
        usuarioRepository.save(usuario);
        return Resultado.exito(usuario);
    }

    @Transactional
    public Resultado cambiarContrasena(int idUsuario, String actual, String nueva, String confirmar) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) return Resultado.fallo("usuario");
        if (actual == null || !passwordEncoder.matches(actual, usuario.getContrasenaUsuario())) {
            return Resultado.fallo("actual");
        }
        if (nueva == null || !nueva.equals(confirmar)) return Resultado.fallo("confirmacion");
        if (!esFuerte(nueva)) return Resultado.fallo("debil");
        if (passwordEncoder.matches(nueva, usuario.getContrasenaUsuario())) return Resultado.fallo("igual");

        usuario.setContrasenaUsuario(passwordEncoder.encode(nueva));
        usuarioRepository.save(usuario);
        return Resultado.exito(usuario);
    }

    // ---- Foto de perfil: va en columnas aparte (no en la entidad) para no cargarla en cada consulta de usuario ----

    /** Recorta al centro en cuadrado, reduce a 256 px y la guarda como JPG. */
    public Resultado guardarFoto(int idUsuario, byte[] datos) {
        if (datos == null || datos.length == 0 || datos.length > MAX_BYTES_FOTO) return Resultado.fallo("foto");
        byte[] jpg;
        try {
            jpg = prepararFoto(datos);
        } catch (IOException | RuntimeException e) {
            return Resultado.fallo("foto");
        }
        if (jpg == null) return Resultado.fallo("foto");
        int filas = jdbc.update("UPDATE usuario SET foto_perfil = ?, foto_perfil_version = ? WHERE idUsuario = ?",
                jpg, System.currentTimeMillis(), idUsuario);
        return filas > 0 ? Resultado.exito(null) : Resultado.fallo("usuario");
    }

    public void quitarFoto(int idUsuario) {
        jdbc.update("UPDATE usuario SET foto_perfil = NULL, foto_perfil_version = NULL WHERE idUsuario = ?", idUsuario);
    }

    /** Version de la foto (para el ETag) o null si la persona no tiene foto. */
    public Long versionFoto(int idUsuario) {
        List<Long> filas = jdbc.queryForList(
                "SELECT foto_perfil_version FROM usuario WHERE idUsuario = ? AND foto_perfil IS NOT NULL", Long.class, idUsuario);
        return filas.isEmpty() ? null : filas.get(0);
    }

    public byte[] foto(int idUsuario) {
        List<byte[]> filas = jdbc.query("SELECT foto_perfil FROM usuario WHERE idUsuario = ?",
                (rs, i) -> rs.getBytes(1), idUsuario);
        return filas.isEmpty() ? null : filas.get(0);
    }

    private byte[] prepararFoto(byte[] datos) throws IOException {
        BufferedImage origen;
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(datos))) {
            if (entrada == null) return null;
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) return null;
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada);
                // Se revisan las medidas antes de decodificar, para no gastar memoria con imagenes gigantes.
                if ((long) lector.getWidth(0) * lector.getHeight(0) > MAX_PIXELES_FOTO) return null;
                origen = lector.read(0);
            } finally {
                lector.dispose();
            }
        }
        if (origen == null) return null;

        int lado = Math.min(origen.getWidth(), origen.getHeight());
        int x = (origen.getWidth() - lado) / 2;
        int y = (origen.getHeight() - lado) / 2;
        int destino = Math.min(LADO_FOTO, lado);
        BufferedImage rgb = new BufferedImage(destino, destino, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, destino, destino);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(origen, 0, 0, destino, destino, x, y, x + lado, y + lado, null);
        } finally {
            g.dispose();
        }

        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpg").next();
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
                ImageOutputStream destinoImg = ImageIO.createImageOutputStream(salida)) {
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(0.85f);
            escritor.setOutput(destinoImg);
            escritor.write(null, new IIOImage(rgb, null, null), parametros);
            destinoImg.flush();
            return salida.toByteArray();
        } finally {
            escritor.dispose();
        }
    }

    /** Mismas reglas que el registro publico (ver Registro_usuario.js). */
    private boolean esFuerte(String contrasena) {
        return contrasena.length() >= 8
                && contrasena.matches(".*[A-Z].*")
                && contrasena.matches(".*[a-z].*")
                && contrasena.matches(".*[0-9].*")
                && contrasena.matches(".*[.@#$%^&*!_\\-+=/].*");
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String limitar(String valor, int maximo) {
        return valor.length() > maximo ? valor.substring(0, maximo) : valor;
    }
}
