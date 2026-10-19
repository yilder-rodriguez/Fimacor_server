package com.fimacor.novedades.entity;

import com.fimacor.catalogos.entity.AmbienteAprendizaje;
import com.fimacor.catalogos.entity.Sede;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.usuarios.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Reporte de falla y su flujo de atencion, con estadoFlujo como maquina de
 * estados explicita (REPORTADA -&gt; ASIGNADA -&gt; EN_REPARACION -&gt;
 * FINALIZADA_TECNICO -&gt; CERRADA, o DEVUELTA/CANCELADA).
 */
@Entity
@Table(name = "novedades")
public class Novedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idnovedades")
    private Integer idNovedades;

    @Column(name = "fecha_novedad", nullable = false)
    private LocalDate fechaNovedad;

    @Column(name = "hora_novedad", nullable = false)
    private LocalTime horaNovedad;

    @Column(name = "tipo_falla", length = 80)
    private String tipoFalla;

    @Column(name = "descripcion_novedad", nullable = false, length = 500)
    private String descripcionNovedad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Sede_idSede")
    private Sede sede;

    @Column(name = "area", length = 100)
    private String area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Ambientes_Aprendizaje_idAmbientes_de_aprendizaje")
    private AmbienteAprendizaje ambiente;

    /** Quien reporta la falla. */
    // EAGER a proposito: novedades.html muestra nombre/apellido de quien reporta fuera de la
    // sesion de Hibernate (open-in-view=false).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Usuario_idUsuario", nullable = false)
    private Usuario reportadoPor;

    // EAGER a proposito: MenuService arma el texto de la notificacion fuera de la sesion de Hibernate
    // (open-in-view=false), y necesita el codigo/descripcion de la maquina para cada novedad.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Ficha_Maquina_idMaquinas", nullable = false)
    private FichaMaquina maquina;

    @Column(name = "estado_flujo", nullable = false, length = 40)
    private String estadoFlujo = "REPORTADA";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_asignado_id")
    private Usuario tecnicoAsignado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_por_id")
    private Usuario asignadoPor;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_inicio_reparacion")
    private LocalDateTime fechaInicioReparacion;

    @Column(name = "fecha_fin_reparacion")
    private LocalDateTime fechaFinReparacion;

    @Column(name = "observacion_reparacion", length = 500)
    private String observacionReparacion;

    @Column(name = "observacion_gestion", length = 500)
    private String observacionGestion;

    public Integer getIdNovedades() {
        return idNovedades;
    }

    public void setIdNovedades(Integer idNovedades) {
        this.idNovedades = idNovedades;
    }

    public LocalDate getFechaNovedad() {
        return fechaNovedad;
    }

    public void setFechaNovedad(LocalDate fechaNovedad) {
        this.fechaNovedad = fechaNovedad;
    }

    public LocalTime getHoraNovedad() {
        return horaNovedad;
    }

    public void setHoraNovedad(LocalTime horaNovedad) {
        this.horaNovedad = horaNovedad;
    }

    public String getTipoFalla() {
        return tipoFalla;
    }

    public void setTipoFalla(String tipoFalla) {
        this.tipoFalla = tipoFalla;
    }

    public String getDescripcionNovedad() {
        return descripcionNovedad;
    }

    public void setDescripcionNovedad(String descripcionNovedad) {
        this.descripcionNovedad = descripcionNovedad;
    }

    public Sede getSede() {
        return sede;
    }

    public void setSede(Sede sede) {
        this.sede = sede;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public AmbienteAprendizaje getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(AmbienteAprendizaje ambiente) {
        this.ambiente = ambiente;
    }

    public Usuario getReportadoPor() {
        return reportadoPor;
    }

    public void setReportadoPor(Usuario reportadoPor) {
        this.reportadoPor = reportadoPor;
    }

    public FichaMaquina getMaquina() {
        return maquina;
    }

    public void setMaquina(FichaMaquina maquina) {
        this.maquina = maquina;
    }

    public String getEstadoFlujo() {
        return estadoFlujo;
    }

    public void setEstadoFlujo(String estadoFlujo) {
        this.estadoFlujo = estadoFlujo;
    }

    public Usuario getTecnicoAsignado() {
        return tecnicoAsignado;
    }

    public void setTecnicoAsignado(Usuario tecnicoAsignado) {
        this.tecnicoAsignado = tecnicoAsignado;
    }

    public Usuario getAsignadoPor() {
        return asignadoPor;
    }

    public void setAsignadoPor(Usuario asignadoPor) {
        this.asignadoPor = asignadoPor;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public LocalDateTime getFechaInicioReparacion() {
        return fechaInicioReparacion;
    }

    public void setFechaInicioReparacion(LocalDateTime fechaInicioReparacion) {
        this.fechaInicioReparacion = fechaInicioReparacion;
    }

    public LocalDateTime getFechaFinReparacion() {
        return fechaFinReparacion;
    }

    public void setFechaFinReparacion(LocalDateTime fechaFinReparacion) {
        this.fechaFinReparacion = fechaFinReparacion;
    }

    public String getObservacionReparacion() {
        return observacionReparacion;
    }

    public void setObservacionReparacion(String observacionReparacion) {
        this.observacionReparacion = observacionReparacion;
    }

    public String getObservacionGestion() {
        return observacionGestion;
    }

    public void setObservacionGestion(String observacionGestion) {
        this.observacionGestion = observacionGestion;
    }

    /** NINGUNA, PENDIENTE, APROBADA o DESESTIMADA: solicitud de baja de la maquina hecha por la Cuentadante. */
    @Column(name = "solicitud_baja", nullable = false, length = 12)
    private String solicitudBaja = "NINGUNA";

    public String getSolicitudBaja() { return solicitudBaja; }
    public void setSolicitudBaja(String solicitudBaja) { this.solicitudBaja = solicitudBaja; }
}
