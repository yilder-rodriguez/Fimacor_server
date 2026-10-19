package com.fimacor.maquinas.entity;

import com.fimacor.catalogos.entity.AmbienteAprendizaje;
import com.fimacor.catalogos.entity.EstadoMaquina;
import com.fimacor.catalogos.entity.Marca;
import com.fimacor.catalogos.entity.Modelo;
import com.fimacor.catalogos.entity.Sede;
import com.fimacor.catalogos.entity.TipoMaquina;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Mapeo completo de `ficha_maquina`, la entidad ancla del sistema. */
@Entity
@Table(name = "ficha_maquina")
public class FichaMaquina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMaquinas")
    private Integer idMaquinas;

    @Column(name = "codigo_sena", nullable = false, length = 45, unique = true)
    private String codigoSena;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    // EAGER a proposito (marca/modelo/tipoMaquina/ambiente): las vistas de maquinas muestran estos
    // catalogos fuera de la sesion de Hibernate (open-in-view=false); son tablas pequenas.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Modelo_idModelo")
    private Modelo modelo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Marca_idMarca", nullable = false)
    private Marca marca;

    // EAGER a proposito: MenuService lee el nombre del estado fuera de la sesion de Hibernate
    // (open-in-view=false), y el estado se necesita siempre junto a la maquina para el panel de fallas.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Estado_de_Maquina_idEstado_de_Maquina", nullable = false)
    private EstadoMaquina estado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Tipo_Maquina_idTipo_Maquina", nullable = false)
    private TipoMaquina tipoMaquina;

    /** Sin relacion JPA a proposito, para evitar una referencia circular con Reubicacion_Maquina. */
    @Column(name = "Reubicacion_Maquina_idRehubicacion_Maquina")
    private Integer reubicacionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Ambientes_Aprendizaje_idAmbientes_de_aprendizaje", nullable = false)
    private AmbienteAprendizaje ambiente;

    @Column(name = "fecha_ubicacion_ambiente")
    private LocalDate fechaUbicacionAmbiente;

    // EAGER a proposito: menu.html muestra la sede de la maquina en el detalle de reubicacion
    // fuera de la sesion de Hibernate (open-in-view=false).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Sede_idSede", nullable = false)
    private Sede sede;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column(name = "valor_maquina", precision = 12, scale = 2)
    private BigDecimal valorMaquina;

    @Column(name = "tiene_garantia", nullable = false)
    private boolean tieneGarantia;

    @Column(name = "fecha_garantia")
    private LocalDate fechaGarantia;

    public Integer getIdMaquinas() {
        return idMaquinas;
    }

    public void setIdMaquinas(Integer idMaquinas) {
        this.idMaquinas = idMaquinas;
    }

    public String getCodigoSena() {
        return codigoSena;
    }

    public void setCodigoSena(String codigoSena) {
        this.codigoSena = codigoSena;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Modelo getModelo() {
        return modelo;
    }

    public void setModelo(Modelo modelo) {
        this.modelo = modelo;
    }

    public Marca getMarca() {
        return marca;
    }

    public void setMarca(Marca marca) {
        this.marca = marca;
    }

    public EstadoMaquina getEstado() {
        return estado;
    }

    public void setEstado(EstadoMaquina estado) {
        this.estado = estado;
    }

    public TipoMaquina getTipoMaquina() {
        return tipoMaquina;
    }

    public void setTipoMaquina(TipoMaquina tipoMaquina) {
        this.tipoMaquina = tipoMaquina;
    }

    public Integer getReubicacionId() {
        return reubicacionId;
    }

    public void setReubicacionId(Integer reubicacionId) {
        this.reubicacionId = reubicacionId;
    }

    public AmbienteAprendizaje getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(AmbienteAprendizaje ambiente) {
        this.ambiente = ambiente;
    }

    public LocalDate getFechaUbicacionAmbiente() {
        return fechaUbicacionAmbiente;
    }

    public void setFechaUbicacionAmbiente(LocalDate fechaUbicacionAmbiente) {
        this.fechaUbicacionAmbiente = fechaUbicacionAmbiente;
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

    public LocalDate getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(LocalDate fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public BigDecimal getValorMaquina() {
        return valorMaquina;
    }

    public void setValorMaquina(BigDecimal valorMaquina) {
        this.valorMaquina = valorMaquina;
    }

    public boolean isTieneGarantia() {
        return tieneGarantia;
    }

    public void setTieneGarantia(boolean tieneGarantia) {
        this.tieneGarantia = tieneGarantia;
    }

    public LocalDate getFechaGarantia() {
        return fechaGarantia;
    }

    public void setFechaGarantia(LocalDate fechaGarantia) {
        this.fechaGarantia = fechaGarantia;
    }

    @Column(name = "baja", nullable = false)
    private boolean baja = false;

    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @Column(name = "motivo_baja", length = 500)
    private String motivoBaja;

    @Column(name = "baja_por", length = 120)
    private String bajaPor;

    public boolean isBaja() { return baja; }
    public void setBaja(boolean baja) { this.baja = baja; }
    public LocalDateTime getFechaBaja() { return fechaBaja; }
    public void setFechaBaja(LocalDateTime fechaBaja) { this.fechaBaja = fechaBaja; }
    public String getMotivoBaja() { return motivoBaja; }
    public void setMotivoBaja(String motivoBaja) { this.motivoBaja = motivoBaja; }
    public String getBajaPor() { return bajaPor; }
    public void setBajaPor(String bajaPor) { this.bajaPor = bajaPor; }
}
