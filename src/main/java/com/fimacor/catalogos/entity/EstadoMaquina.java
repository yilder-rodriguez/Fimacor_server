package com.fimacor.catalogos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "estado_de_maquina")
public class EstadoMaquina {

    @Id
    @Column(name = "idEstado_de_Maquina")
    private Integer idEstadoDeMaquina;

    @Column(name = "nombre_tipo", nullable = false, length = 45)
    private String nombreTipo;

    @Column(name = "descripcion_estado", nullable = false, length = 45)
    private String descripcionEstado;

    public Integer getIdEstadoDeMaquina() {
        return idEstadoDeMaquina;
    }

    public void setIdEstadoDeMaquina(Integer idEstadoDeMaquina) {
        this.idEstadoDeMaquina = idEstadoDeMaquina;
    }

    public String getNombreTipo() {
        return nombreTipo;
    }

    public void setNombreTipo(String nombreTipo) {
        this.nombreTipo = nombreTipo;
    }

    public String getDescripcionEstado() {
        return descripcionEstado;
    }

    public void setDescripcionEstado(String descripcionEstado) {
        this.descripcionEstado = descripcionEstado;
    }
}
