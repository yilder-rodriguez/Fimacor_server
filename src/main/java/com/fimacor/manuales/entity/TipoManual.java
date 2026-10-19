package com.fimacor.manuales.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipos_manuales")
public class TipoManual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipos_Manuales")
    private Integer idTiposManuales;

    @Column(name = "descripcion_manual", nullable = false, length = 45)
    private String descripcionManual;

    public Integer getIdTiposManuales() {
        return idTiposManuales;
    }

    public void setIdTiposManuales(Integer idTiposManuales) {
        this.idTiposManuales = idTiposManuales;
    }

    public String getDescripcionManual() {
        return descripcionManual;
    }

    public void setDescripcionManual(String descripcionManual) {
        this.descripcionManual = descripcionManual;
    }
}
