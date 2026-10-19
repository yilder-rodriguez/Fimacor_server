package com.fimacor.reubicacion.entity;

import com.fimacor.catalogos.entity.AmbienteAprendizaje;
import com.fimacor.catalogos.entity.Sede;
import com.fimacor.maquinas.entity.FichaMaquina;
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

/** Solicitud de traslado de una maquina, con firma electronica de aprobacion. */
@Entity
@Table(name = "reubicacion_maquina")
public class ReubicacionMaquina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRehubicacion_Maquina")
    private Integer idRehubicacionMaquina;

    // EAGER a proposito: MenuService arma el texto de la solicitud fuera de la sesion de Hibernate
    // (open-in-view=false), y necesita el codigo/descripcion de la maquina siempre que hay una reubicacion.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Ficha_Maquina_idMaquinas")
    private FichaMaquina maquina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambiente_destino_id")
    private AmbienteAprendizaje ambienteDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_destino_id")
    private Sede sedeDestino;

    @Column(name = "area_destino", length = 100)
    private String areaDestino;

    @Column(name = "fecha", nullable = false, length = 45)
    private String fecha;

    @Column(name = "tipo_traslado", nullable = false, length = 20)
    private String tipoTraslado = "Interno";

    /** Texto libre del origen (no es llave foranea, es solo texto). */
    @Column(name = "ambiente", nullable = false, length = 45)
    private String origenTexto;

    @Column(name = "destino", nullable = false, length = 45)
    private String destinoTexto;

    @Column(name = "autorizado_por", nullable = false, length = 45)
    private String autorizadoPor;

    @Column(name = "autorizacion", nullable = false, length = 160)
    private String autorizacion;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "evidencia", nullable = false, length = 500)
    private String evidencia;

    @Column(name = "descripcion_rehubicacion", nullable = false, length = 500)
    private String descripcionReubicacion;

    @Column(name = "estado_autorizacion", nullable = false, length = 20)
    private String estadoAutorizacion = "Pendiente";

    @Column(name = "respuesta_autorizacion", columnDefinition = "TEXT")
    private String respuestaAutorizacion;

    @Column(name = "fecha_respuesta_autorizacion")
    private LocalDateTime fechaRespuestaAutorizacion;

    @Column(name = "firma_digital", columnDefinition = "MEDIUMBLOB")
    private byte[] firmaDigital;

    public Integer getIdRehubicacionMaquina() {
        return idRehubicacionMaquina;
    }

    public FichaMaquina getMaquina() {
        return maquina;
    }

    public void setMaquina(FichaMaquina maquina) {
        this.maquina = maquina;
    }

    public AmbienteAprendizaje getAmbienteDestino() {
        return ambienteDestino;
    }

    public void setAmbienteDestino(AmbienteAprendizaje ambienteDestino) {
        this.ambienteDestino = ambienteDestino;
    }

    public Sede getSedeDestino() {
        return sedeDestino;
    }

    public void setSedeDestino(Sede sedeDestino) {
        this.sedeDestino = sedeDestino;
    }

    public String getAreaDestino() {
        return areaDestino;
    }

    public void setAreaDestino(String areaDestino) {
        this.areaDestino = areaDestino;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getTipoTraslado() {
        return tipoTraslado;
    }

    public void setTipoTraslado(String tipoTraslado) {
        this.tipoTraslado = tipoTraslado;
    }

    public String getOrigenTexto() {
        return origenTexto;
    }

    public void setOrigenTexto(String origenTexto) {
        this.origenTexto = origenTexto;
    }

    public String getDestinoTexto() {
        return destinoTexto;
    }

    public void setDestinoTexto(String destinoTexto) {
        this.destinoTexto = destinoTexto;
    }

    public String getAutorizadoPor() {
        return autorizadoPor;
    }

    public void setAutorizadoPor(String autorizadoPor) {
        this.autorizadoPor = autorizadoPor;
    }

    public String getAutorizacion() {
        return autorizacion;
    }

    public void setAutorizacion(String autorizacion) {
        this.autorizacion = autorizacion;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getEvidencia() {
        return evidencia;
    }

    public void setEvidencia(String evidencia) {
        this.evidencia = evidencia;
    }

    public String getDescripcionReubicacion() {
        return descripcionReubicacion;
    }

    public void setDescripcionReubicacion(String descripcionReubicacion) {
        this.descripcionReubicacion = descripcionReubicacion;
    }

    public String getEstadoAutorizacion() {
        return estadoAutorizacion;
    }

    public void setEstadoAutorizacion(String estadoAutorizacion) {
        this.estadoAutorizacion = estadoAutorizacion;
    }

    public String getRespuestaAutorizacion() {
        return respuestaAutorizacion;
    }

    public void setRespuestaAutorizacion(String respuestaAutorizacion) {
        this.respuestaAutorizacion = respuestaAutorizacion;
    }

    public LocalDateTime getFechaRespuestaAutorizacion() {
        return fechaRespuestaAutorizacion;
    }

    public void setFechaRespuestaAutorizacion(LocalDateTime fechaRespuestaAutorizacion) {
        this.fechaRespuestaAutorizacion = fechaRespuestaAutorizacion;
    }

    public byte[] getFirmaDigital() {
        return firmaDigital;
    }

    public void setFirmaDigital(byte[] firmaDigital) {
        this.firmaDigital = firmaDigital;
    }
}
