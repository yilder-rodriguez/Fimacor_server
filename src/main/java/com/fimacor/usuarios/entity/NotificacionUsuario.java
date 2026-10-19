package com.fimacor.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Campanita de notificaciones internas (notificaciones_usuario). */
@Entity
@Table(name = "notificaciones_usuario")
public class NotificacionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion_usuario")
    private Integer idNotificacionUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario_idUsuario", nullable = false)
    private Usuario usuario;

    @Column(name = "titulo", nullable = false, length = 120)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 255)
    private String mensaje;

    @Column(name = "enlace", nullable = false, length = 180)
    private String enlace;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo = "ok";

    @Column(name = "icono", nullable = false, length = 6)
    private String icono = "M";

    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public Integer getIdNotificacionUsuario() {
        return idNotificacionUsuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        // la columna admite 255: con descripciones largas de maquina el texto se recorta en vez de fallar el guardado
        this.mensaje = mensaje != null && mensaje.length() > 255 ? mensaje.substring(0, 254) + "…" : mensaje;
    }

    public String getEnlace() {
        return enlace;
    }

    public void setEnlace(String enlace) {
        this.enlace = enlace;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
