package com.fimacor.maquinas.entity;

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

/** Registro inmutable de cada baja o reactivacion de una maquina. */
@Entity
@Table(name = "baja_maquina_historial")
public class BajaMaquinaHistorial {

    public static final String BAJA = "BAJA";
    public static final String REACTIVACION = "REACTIVACION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idBajaHistorial")
    private Integer idBajaHistorial;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "Ficha_Maquina_idMaquinas", nullable = false)
    private FichaMaquina maquina;

    @Column(name = "accion", nullable = false, length = 20)
    private String accion;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "usuario_nombre", nullable = false, length = 120)
    private String usuarioNombre;

    @Column(name = "novedad_id")
    private Integer novedadId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public Integer getIdBajaHistorial() { return idBajaHistorial; }
    public FichaMaquina getMaquina() { return maquina; }
    public void setMaquina(FichaMaquina maquina) { this.maquina = maquina; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public Integer getNovedadId() { return novedadId; }
    public void setNovedadId(Integer novedadId) { this.novedadId = novedadId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
