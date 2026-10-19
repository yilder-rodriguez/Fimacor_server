package com.fimacor.catalogos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "area")
public class Area {

    @Id
    @Column(name = "idArea")
    private Integer idArea;

    @Column(name = "nombre_area", nullable = false, length = 150)
    private String nombreArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Sede_idSede")
    private Sede sede;

    public Integer getIdArea() {
        return idArea;
    }

    public void setIdArea(Integer idArea) {
        this.idArea = idArea;
    }

    public String getNombreArea() {
        return nombreArea;
    }

    public void setNombreArea(String nombreArea) {
        this.nombreArea = nombreArea;
    }

    public Sede getSede() {
        return sede;
    }

    public void setSede(Sede sede) {
        this.sede = sede;
    }
}
