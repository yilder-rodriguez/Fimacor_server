package com.fimacor.mantenimiento.entity;

import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.usuarios.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bitacora de ejecucion de un mantenimiento/reparacion. El estado real vive
 * codificado como texto dentro de descripcionAccion ("ESTADO: ASIGNADO",
 * "ESTADO: EN MANTENIMIENTO", "ESTADO: FINALIZADO", "ESTADO: CERRADO",
 * "ESTADO: REPARADO"). Se sabe que no es el diseño ideal, pero cambiarlo
 * implicaria tocar datos ya guardados, asi que se deja igual.
 */
@Entity
@Table(name = "historial_mantenimiento")
public class HistorialMantenimiento {

    @Id
    @Column(name = "idHistorial_Mantenimiento")
    private Integer idHistorialMantenimiento;

    @Column(name = "tipo_maquina", nullable = false, length = 45)
    private String tipoMaquina;

    @Column(name = "fecha_inicio", nullable = false, length = 45)
    private String fechaInicio;

    @Column(name = "fecha_final", nullable = false, length = 45)
    private String fechaFinal;

    @Column(name = "tecnico_encargado", nullable = false, length = 45)
    private String tecnicoEncargado;

    @Column(name = "descripcion_accion", nullable = false, length = 1200)
    private String descripcionAccion;

    // EAGER a proposito: MenuService arma el texto de la notificacion fuera de la sesion de Hibernate
    // (open-in-view=false), y necesita el codigo/descripcion de la maquina para cada historial.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Ficha_Maquina_idMaquinas", nullable = false)
    private FichaMaquina maquina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario_idUsuario", nullable = false)
    private Usuario tecnico;

    public Integer getIdHistorialMantenimiento() {
        return idHistorialMantenimiento;
    }

    public void setIdHistorialMantenimiento(Integer idHistorialMantenimiento) {
        this.idHistorialMantenimiento = idHistorialMantenimiento;
    }

    public String getTipoMaquina() {
        return tipoMaquina;
    }

    public void setTipoMaquina(String tipoMaquina) {
        this.tipoMaquina = tipoMaquina;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getFechaFinal() {
        return fechaFinal;
    }

    public void setFechaFinal(String fechaFinal) {
        this.fechaFinal = fechaFinal;
    }

    public String getTecnicoEncargado() {
        return tecnicoEncargado;
    }

    public void setTecnicoEncargado(String tecnicoEncargado) {
        this.tecnicoEncargado = tecnicoEncargado;
    }

    public String getDescripcionAccion() {
        return descripcionAccion;
    }

    public void setDescripcionAccion(String descripcionAccion) {
        this.descripcionAccion = descripcionAccion;
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

    public boolean estaAsignado() {
        return descripcionAccion != null && descripcionAccion.contains("ESTADO: ASIGNADO")
                && !descripcionAccion.contains("ESTADO: REPARADO");
    }
}
