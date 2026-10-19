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
import com.fimacor.usuarios.repository.UsuarioRepository;

/**
 * Mapea la tabla `usuario`. Las columnas se declaran explicitas porque el
 * esquema usa mixed-case (idUsuario, Rol_idRol...) y la estrategia de
 * nombres por defecto de Hibernate las convertiria a snake_case.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUsuario")
    private Integer idUsuario;

    @Column(name = "nombre", nullable = false, length = 45)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 45)
    private String apellido;

    @Column(name = "documento_identidad", nullable = false, length = 45)
    private String documentoIdentidad;

    @Column(name = "numero_telefono", nullable = false, length = 45)
    private String numeroTelefono;

    @Column(name = "correo", nullable = false, length = 45)
    private String correo;

    @Column(name = "contrasena_usuario", nullable = false, length = 255)
    private String contrasenaUsuario;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    // EAGER a proposito: con open-in-view=false la sesion de Hibernate se cierra
    // apenas termina el repositorio, y el rol se necesita siempre junto al usuario
    // (permisos, vista) incluso fuera de esa sesion (ver comentario en UsuarioRepository).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Rol_idRol", nullable = false)
    private Rol rol;

    @Column(name = "Documento_Identidad_idDocumento_Identidad", nullable = false)
    private Integer documentoIdentidadId;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "foto_carnet", columnDefinition = "LONGTEXT")
    private String fotoCarnet;

    /** Codigo corto para validar reportes de novedad por QR sin login (solo usuarios Instructor). */
    @Column(name = "codigo_instructor", length = 8)
    private String codigoInstructor;

    /** Lo habilita el Administrador: un Instructor con esta casilla tambien puede actuar como Tecnico. */
    @Column(name = "actua_como_tecnico", nullable = false)
    private boolean actuaComoTecnico = false;

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public void setDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
    }

    public String getNumeroTelefono() {
        return numeroTelefono;
    }

    public void setNumeroTelefono(String numeroTelefono) {
        this.numeroTelefono = numeroTelefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasenaUsuario() {
        return contrasenaUsuario;
    }

    public void setContrasenaUsuario(String contrasenaUsuario) {
        this.contrasenaUsuario = contrasenaUsuario;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Integer getDocumentoIdentidadId() {
        return documentoIdentidadId;
    }

    public void setDocumentoIdentidadId(Integer documentoIdentidadId) {
        this.documentoIdentidadId = documentoIdentidadId;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getFotoCarnet() {
        return fotoCarnet;
    }

    public void setFotoCarnet(String fotoCarnet) {
        this.fotoCarnet = fotoCarnet;
    }

    public String getCodigoInstructor() {
        return codigoInstructor;
    }

    public void setCodigoInstructor(String codigoInstructor) {
        this.codigoInstructor = codigoInstructor;
    }

    public boolean isActuaComoTecnico() {
        return actuaComoTecnico;
    }

    public void setActuaComoTecnico(boolean actuaComoTecnico) {
        this.actuaComoTecnico = actuaComoTecnico;
    }
}
