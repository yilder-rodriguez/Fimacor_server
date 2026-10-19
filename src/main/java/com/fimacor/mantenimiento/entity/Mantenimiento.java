package com.fimacor.mantenimiento.entity;

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

/** Mantenimiento programado (agenda). El seguimiento de ejecucion vive en HistorialMantenimiento. */
@Entity
@Table(name = "mantenimiento")
public class Mantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMantenimiento")
    private Integer idMantenimiento;

    @Column(name = "fecha_mantenimiento", nullable = false)
    private LocalDate fechaMantenimiento;

    @Column(name = "fecha_proximo_mantenimiento", nullable = false)
    private LocalDate fechaProximoMantenimiento;

    @Column(name = "descripcion_Mantenimiento", nullable = false, length = 90)
    private String descripcionMantenimiento;

    @Column(name = "frecuencia", nullable = false, length = 45)
    private String frecuencia;

    @Column(name = "hora_programada", length = 5)
    private String horaProgramada;

    @Column(name = "tareas", nullable = false, length = 255)
    private String tareas;

    @Column(name = "evidencia_mantenimiento", length = 1500)
    private String evidenciaMantenimiento;

    // EAGER a proposito: mantenimiento-programar.html muestra el nombre del tipo fuera de la sesion
    // de Hibernate (open-in-view=false).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Tipo_Manteniento_idTipo_Manteniento", nullable = false)
    private TipoMantenimiento tipoMantenimiento;

    // EAGER a proposito: MenuService arma el texto de la notificacion fuera de la sesion de Hibernate
    // (open-in-view=false), y necesita el codigo/descripcion de la maquina para cada mantenimiento.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Ficha_Maquina_idMaquinas", nullable = false)
    private FichaMaquina maquina;

    /** Tecnico responsable. */
    // EAGER a proposito: mantenimiento-programar.html muestra nombre/apellido/correo del tecnico
    // fuera de la sesion de Hibernate (open-in-view=false).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Usuario_idUsuario", nullable = false)
    private Usuario tecnico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programado_por_usuario_id")
    private Usuario programadoPor;

    public Integer getIdMantenimiento() {
        return idMantenimiento;
    }

    public void setIdMantenimiento(Integer idMantenimiento) {
        this.idMantenimiento = idMantenimiento;
    }

    public LocalDate getFechaMantenimiento() {
        return fechaMantenimiento;
    }

    public void setFechaMantenimiento(LocalDate fechaMantenimiento) {
        this.fechaMantenimiento = fechaMantenimiento;
    }

    public LocalDate getFechaProximoMantenimiento() {
        return fechaProximoMantenimiento;
    }

    public void setFechaProximoMantenimiento(LocalDate fechaProximoMantenimiento) {
        this.fechaProximoMantenimiento = fechaProximoMantenimiento;
    }

    public String getDescripcionMantenimiento() {
        return descripcionMantenimiento;
    }

    public void setDescripcionMantenimiento(String descripcionMantenimiento) {
        this.descripcionMantenimiento = descripcionMantenimiento;
    }

    public String getFrecuencia() {
        return frecuencia;
    }

    public void setFrecuencia(String frecuencia) {
        this.frecuencia = frecuencia;
    }

    public String getHoraProgramada() {
        return horaProgramada;
    }

    public void setHoraProgramada(String horaProgramada) {
        this.horaProgramada = horaProgramada;
    }

    public String getTareas() {
        return tareas;
    }

    public void setTareas(String tareas) {
        this.tareas = tareas;
    }

    public String getEvidenciaMantenimiento() {
        return evidenciaMantenimiento;
    }

    public void setEvidenciaMantenimiento(String evidenciaMantenimiento) {
        this.evidenciaMantenimiento = evidenciaMantenimiento;
    }

    public TipoMantenimiento getTipoMantenimiento() {
        return tipoMantenimiento;
    }

    public void setTipoMantenimiento(TipoMantenimiento tipoMantenimiento) {
        this.tipoMantenimiento = tipoMantenimiento;
    }

    public FichaMaquina getMaquina() {
        return maquina;
    }

    public void setMaquina(FichaMaquina maquina) {
        this.maquina = maquina;
    }

    public Usuario getTecnico() {
        return tecnico;
    }

    public void setTecnico(Usuario tecnico) {
        this.tecnico = tecnico;
    }

    public Usuario getProgramadoPor() {
        return programadoPor;
    }

    public void setProgramadoPor(Usuario programadoPor) {
        this.programadoPor = programadoPor;
    }
}
