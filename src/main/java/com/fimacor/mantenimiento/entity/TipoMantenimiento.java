package com.fimacor.mantenimiento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_manteniento")
public class TipoMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipo_Manteniento")
    private Integer idTipoManteniento;

    @Column(name = "descripcion_matenimiento", nullable = false, length = 45)
    private String descripcionMantenimiento;

    public Integer getIdTipoManteniento() {
        return idTipoManteniento;
    }

    public void setIdTipoManteniento(Integer idTipoManteniento) {
        this.idTipoManteniento = idTipoManteniento;
    }

    public String getDescripcionMantenimiento() {
        return descripcionMantenimiento;
    }

    public void setDescripcionMantenimiento(String descripcionMantenimiento) {
        this.descripcionMantenimiento = descripcionMantenimiento;
    }
}
