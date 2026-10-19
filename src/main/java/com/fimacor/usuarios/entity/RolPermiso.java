package com.fimacor.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol_permiso")
public class RolPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRol_Permiso")
    private Integer idRolPermiso;

    @ManyToOne
    @JoinColumn(name = "Rol_idRol", nullable = false)
    private Rol rol;

    @ManyToOne
    @JoinColumn(name = "Permiso_idPermiso", nullable = false)
    private Permiso permiso;

    public Integer getIdRolPermiso() {
        return idRolPermiso;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Permiso getPermiso() {
        return permiso;
    }

    public void setPermiso(Permiso permiso) {
        this.permiso = permiso;
    }
}
