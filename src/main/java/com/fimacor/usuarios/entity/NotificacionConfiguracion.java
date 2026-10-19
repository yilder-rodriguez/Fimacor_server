package com.fimacor.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import com.fimacor.catalogos.controller.ConfiguracionController;

/**
 * Notificaciones de cambios de configuración para la campanita del administrador.
 * Por ahora nada escribe en esta tabla todavía: falta conectar el aviso desde
 * ConfiguracionController cuando se guarde un cambio de catálogo.
 */
@Entity
@Table(name = "notificaciones_configuracion")
public class NotificacionConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @Column(name = "titulo", nullable = false, length = 120)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 255)
    private String mensaje;

    @Column(name = "enlace", nullable = false, length = 180)
    private String enlace;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo = "admin";

    @Column(name = "icono", nullable = false, length = 6)
    private String icono = "A";

    @Column(name = "creado_por", nullable = false, length = 120)
    private String creadoPor;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public Integer getIdNotificacion() {
        return idNotificacion;
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
        this.mensaje = mensaje;
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

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
