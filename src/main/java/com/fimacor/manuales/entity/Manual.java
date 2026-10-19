package com.fimacor.manuales.entity;

import com.fimacor.maquinas.entity.FichaMaquina;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "manuales")
public class Manual {

    @Id
    @Column(name = "idManuales")
    private Integer idManuales;

    @Column(name = "nombre_del_manual", nullable = false, length = 45)
    private String nombreDelManual;

    @Column(name = "descripcion_manual", nullable = false, length = 45)
    private String descripcionManual;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDate fechaSubida;

    /** "archivo" (PDF guardado via FileStorageService) o "link" (URL externa). */
    @Column(name = "fuente_manual", nullable = false, length = 10)
    private String fuenteManual;

    @Column(name = "ruta_manual", length = 500)
    private String rutaManual;

    // EAGER a proposito: con open-in-view=false la sesion de Hibernate ya esta
    // cerrada cuando la plantilla (maquinas.html) lee manual.tipoManual.descripcionManual.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Tipos_Manuales_idTipos_Manuales", nullable = false)
    private TipoManual tipoManual;

    // EAGER a proposito: MenuService arma el texto de la notificacion fuera de la sesion de Hibernate
    // (open-in-view=false), y necesita el codigo/descripcion de la maquina para cada manual.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Ficha_Maquina_idMaquinas", nullable = false)
    private FichaMaquina maquina;

    public Integer getIdManuales() {
        return idManuales;
    }

    public void setIdManuales(Integer idManuales) {
        this.idManuales = idManuales;
    }

    public String getNombreDelManual() {
        return nombreDelManual;
    }

    public void setNombreDelManual(String nombreDelManual) {
        this.nombreDelManual = nombreDelManual;
    }

    public String getDescripcionManual() {
        return descripcionManual;
    }

    public void setDescripcionManual(String descripcionManual) {
        this.descripcionManual = descripcionManual;
    }

    public LocalDate getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDate fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public String getFuenteManual() {
        return fuenteManual;
    }

    public void setFuenteManual(String fuenteManual) {
        this.fuenteManual = fuenteManual;
    }

    public String getRutaManual() {
        return rutaManual;
    }

    public void setRutaManual(String rutaManual) {
        this.rutaManual = rutaManual;
    }

    public TipoManual getTipoManual() {
        return tipoManual;
    }

    public void setTipoManual(TipoManual tipoManual) {
        this.tipoManual = tipoManual;
    }

    public FichaMaquina getMaquina() {
        return maquina;
    }

    public void setMaquina(FichaMaquina maquina) {
        this.maquina = maquina;
    }

    public boolean esEnlace() {
        return "link".equalsIgnoreCase(fuenteManual);
    }
}
