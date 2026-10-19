package com.fimacor.maquinas.entity;

import com.fimacor.usuarios.entity.Usuario;
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

/** Asignacion de una maquina a un cuentadante. Una maquina tiene a lo sumo un cuentadante. */
@Entity
@Table(name = "maquina_cuentadante")
public class MaquinaCuentadante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMaquina_Cuentadante")
    private Integer idMaquinaCuentadante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Ficha_Maquina_idMaquinas", nullable = false)
    private FichaMaquina maquina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario_idUsuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    public Integer getIdMaquinaCuentadante() {
        return idMaquinaCuentadante;
    }

    public void setIdMaquinaCuentadante(Integer idMaquinaCuentadante) {
        this.idMaquinaCuentadante = idMaquinaCuentadante;
    }

    public FichaMaquina getMaquina() {
        return maquina;
    }

    public void setMaquina(FichaMaquina maquina) {
        this.maquina = maquina;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }
}
