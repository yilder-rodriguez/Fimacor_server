package com.fimacor.reubicacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Bitacora simple de movimientos fisicos de una maquina. */
@Entity
@Table(name = "ubicacion_maquina")
public class UbicacionMaquina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUbicacion_Maquina")
    private Integer idUbicacionMaquina;

    @Column(name = "Fecha", nullable = false, length = 45)
    private String fecha;

    @Column(name = "Ambiente", nullable = false, length = 45)
    private String ambiente;

    public Integer getIdUbicacionMaquina() {
        return idUbicacionMaquina;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }
}
