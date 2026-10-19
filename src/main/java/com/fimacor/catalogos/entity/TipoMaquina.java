package com.fimacor.catalogos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_maquina")
public class TipoMaquina {

    @Id
    @Column(name = "idTipo_Maquina")
    private Integer idTipoMaquina;

    @Column(name = "descripcion_maquina", nullable = false, length = 45)
    private String descripcionMaquina;

    public Integer getIdTipoMaquina() {
        return idTipoMaquina;
    }

    public void setIdTipoMaquina(Integer idTipoMaquina) {
        this.idTipoMaquina = idTipoMaquina;
    }

    public String getDescripcionMaquina() {
        return descripcionMaquina;
    }

    public void setDescripcionMaquina(String descripcionMaquina) {
        this.descripcionMaquina = descripcionMaquina;
    }
}
