package com.fimacor.catalogos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ambientes_aprendizaje")
public class AmbienteAprendizaje {

    @Id
    @Column(name = "idAmbientes_de_aprendizaje")
    private Integer idAmbientesDeAprendizaje;

    @Column(name = "descripcion_ambiente", nullable = false, length = 45)
    private String descripcionAmbiente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Sede_idSede")
    private Sede sede;

    public Integer getIdAmbientesDeAprendizaje() {
        return idAmbientesDeAprendizaje;
    }

    public void setIdAmbientesDeAprendizaje(Integer idAmbientesDeAprendizaje) {
        this.idAmbientesDeAprendizaje = idAmbientesDeAprendizaje;
    }

    public String getDescripcionAmbiente() {
        return descripcionAmbiente;
    }

    public void setDescripcionAmbiente(String descripcionAmbiente) {
        this.descripcionAmbiente = descripcionAmbiente;
    }

    public Sede getSede() {
        return sede;
    }

    public void setSede(Sede sede) {
        this.sede = sede;
    }
}
