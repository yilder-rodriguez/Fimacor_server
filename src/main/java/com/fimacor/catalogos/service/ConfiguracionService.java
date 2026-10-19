package com.fimacor.catalogos.service;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import com.fimacor.catalogos.entity.AmbienteAprendizaje;
import com.fimacor.catalogos.entity.Area;
import com.fimacor.catalogos.entity.EstadoMaquina;
import com.fimacor.catalogos.entity.Marca;
import com.fimacor.catalogos.entity.Modelo;
import com.fimacor.catalogos.entity.Sede;
import com.fimacor.catalogos.entity.TipoMaquina;
import com.fimacor.catalogos.repository.AmbienteAprendizajeRepository;
import com.fimacor.catalogos.repository.AreaRepository;
import com.fimacor.catalogos.repository.EstadoMaquinaRepository;
import com.fimacor.catalogos.repository.MarcaRepository;
import com.fimacor.catalogos.repository.ModeloRepository;
import com.fimacor.catalogos.repository.SedeRepository;
import com.fimacor.catalogos.repository.TipoMaquinaRepository;
import com.fimacor.mantenimiento.entity.TipoMantenimiento;
import com.fimacor.mantenimiento.repository.TipoMantenimientoRepository;
import com.fimacor.manuales.entity.TipoManual;
import com.fimacor.manuales.repository.TipoManualRepository;
import com.fimacor.reubicacion.entity.UbicacionMaquina;
import com.fimacor.reubicacion.repository.UbicacionMaquinaRepository;

/**
 * Administra los catalogos: Tipo_Maquina, Estado_de_Maquina, Marca, Modelo,
 * Sede, Ambientes_Aprendizaje, Area, Tipo_Manteniento y Tipos_Manuales.
 * Tipo_Maquina, Estado_de_Maquina, Sede, Ambientes_Aprendizaje y Area no son
 * AUTO_INCREMENT en la base de datos, asi que el siguiente id se calcula a
 * mano (MAX(id) + 1); Tipo_Manteniento y Tipos_Manuales si son AUTO_INCREMENT.
 * Ubicacion_maquina y Reubicacion_maquina son bitacoras que ya se escriben
 * desde la reubicacion de maquinas (ver ReubicacionService), asi que aqui solo
 * se listan de solo lectura -- editarlas a mano rompe la trazabilidad.
 */
@Service
public class ConfiguracionService {

    private final TipoMaquinaRepository tipoMaquinaRepository;
    private final EstadoMaquinaRepository estadoMaquinaRepository;
    private final MarcaRepository marcaRepository;
    private final ModeloRepository modeloRepository;
    private final SedeRepository sedeRepository;
    private final AmbienteAprendizajeRepository ambienteRepository;
    private final AreaRepository areaRepository;
    private final TipoMantenimientoRepository tipoMantenimientoRepository;
    private final TipoManualRepository tipoManualRepository;
    private final UbicacionMaquinaRepository ubicacionMaquinaRepository;

    public ConfiguracionService(TipoMaquinaRepository tipoMaquinaRepository, EstadoMaquinaRepository estadoMaquinaRepository,
            MarcaRepository marcaRepository, ModeloRepository modeloRepository, SedeRepository sedeRepository,
            AmbienteAprendizajeRepository ambienteRepository, AreaRepository areaRepository,
            TipoMantenimientoRepository tipoMantenimientoRepository, TipoManualRepository tipoManualRepository,
            UbicacionMaquinaRepository ubicacionMaquinaRepository) {
        this.tipoMaquinaRepository = tipoMaquinaRepository;
        this.estadoMaquinaRepository = estadoMaquinaRepository;
        this.marcaRepository = marcaRepository;
        this.modeloRepository = modeloRepository;
        this.sedeRepository = sedeRepository;
        this.ambienteRepository = ambienteRepository;
        this.areaRepository = areaRepository;
        this.tipoMantenimientoRepository = tipoMantenimientoRepository;
        this.tipoManualRepository = tipoManualRepository;
        this.ubicacionMaquinaRepository = ubicacionMaquinaRepository;
    }

    // ---- Tipo de maquina ----

    public List<TipoMaquina> listarTiposMaquina() {
        return tipoMaquinaRepository.findAllByOrderByIdTipoMaquinaDesc();
    }

    public boolean crearTipoMaquina(String descripcion) {
        TipoMaquina tipo = new TipoMaquina();
        tipo.setIdTipoMaquina(siguienteId(tipoMaquinaRepository.findAll().stream().map(TipoMaquina::getIdTipoMaquina)));
        tipo.setDescripcionMaquina(limitar(descripcion));
        return guardar(() -> tipoMaquinaRepository.save(tipo));
    }

    public boolean actualizarTipoMaquina(int id, String descripcion) {
        return tipoMaquinaRepository.findById(id).map(tipo -> {
            tipo.setDescripcionMaquina(limitar(descripcion));
            return guardar(() -> tipoMaquinaRepository.save(tipo));
        }).orElse(false);
    }

    public boolean eliminarTipoMaquina(int id) {
        return eliminar(() -> tipoMaquinaRepository.deleteById(id));
    }

    // ---- Estado de maquina ----

    public List<EstadoMaquina> listarEstadosMaquina() {
        return estadoMaquinaRepository.findAllByOrderByIdEstadoDeMaquinaAsc();
    }

    public boolean crearEstadoMaquina(String nombreTipo, String descripcionEstado) {
        EstadoMaquina estado = new EstadoMaquina();
        estado.setIdEstadoDeMaquina(siguienteId(estadoMaquinaRepository.findAll().stream().map(EstadoMaquina::getIdEstadoDeMaquina)));
        estado.setNombreTipo(limitar(nombreTipo));
        estado.setDescripcionEstado(limitar(descripcionEstado));
        return guardar(() -> estadoMaquinaRepository.save(estado));
    }

    public boolean actualizarEstadoMaquina(int id, String nombreTipo, String descripcionEstado) {
        return estadoMaquinaRepository.findById(id).map(estado -> {
            estado.setNombreTipo(limitar(nombreTipo));
            estado.setDescripcionEstado(limitar(descripcionEstado));
            return guardar(() -> estadoMaquinaRepository.save(estado));
        }).orElse(false);
    }

    public boolean eliminarEstadoMaquina(int id) {
        return eliminar(() -> estadoMaquinaRepository.deleteById(id));
    }

    // ---- Marca ----

    public List<Marca> listarMarcas() {
        return marcaRepository.findAllByOrderByIdMarcaDesc();
    }

    public List<Marca> listarMarcasOrdenAlfabetico() {
        return marcaRepository.findAllByOrderByDescripcionMarcaAsc();
    }

    public boolean crearMarca(String descripcion) {
        Marca marca = new Marca();
        marca.setDescripcionMarca(limitar(descripcion));
        return guardar(() -> marcaRepository.save(marca));
    }

    public boolean actualizarMarca(int id, String descripcion) {
        return marcaRepository.findById(id).map(marca -> {
            marca.setDescripcionMarca(limitar(descripcion));
            return guardar(() -> marcaRepository.save(marca));
        }).orElse(false);
    }

    public boolean eliminarMarca(int id) {
        return eliminar(() -> marcaRepository.deleteById(id));
    }

    // ---- Modelo ----

    public List<Modelo> listarModelos() {
        return modeloRepository.findAllByOrderByIdModeloDesc();
    }

    public boolean crearModelo(String descripcion, int idMarca) {
        return marcaRepository.findById(idMarca).map(marca -> {
            Modelo modelo = new Modelo();
            modelo.setDescripcionModelo(limitar(descripcion));
            modelo.setMarca(marca);
            return guardar(() -> modeloRepository.save(modelo));
        }).orElse(false);
    }

    public boolean actualizarModelo(int id, String descripcion, int idMarca) {
        Marca marca = marcaRepository.findById(idMarca).orElse(null);
        if (marca == null) return false;
        return modeloRepository.findById(id).map(modelo -> {
            modelo.setDescripcionModelo(limitar(descripcion));
            modelo.setMarca(marca);
            return guardar(() -> modeloRepository.save(modelo));
        }).orElse(false);
    }

    public boolean eliminarModelo(int id) {
        return eliminar(() -> modeloRepository.deleteById(id));
    }

    // ---- Sede ----

    public List<Sede> listarSedes() {
        return sedeRepository.findAllByOrderByIdSedeDesc();
    }

    public List<Sede> listarSedesOrdenAlfabetico() {
        return sedeRepository.findAllByOrderByNombreSedeAsc();
    }

    public boolean crearSede(String nombre) {
        Sede sede = new Sede();
        sede.setIdSede(siguienteId(sedeRepository.findAll().stream().map(Sede::getIdSede)));
        sede.setNombreSede(limitar(nombre));
        return guardar(() -> sedeRepository.save(sede));
    }

    public boolean actualizarSede(int id, String nombre) {
        return sedeRepository.findById(id).map(sede -> {
            sede.setNombreSede(limitar(nombre));
            return guardar(() -> sedeRepository.save(sede));
        }).orElse(false);
    }

    public boolean eliminarSede(int id) {
        return eliminar(() -> sedeRepository.deleteById(id));
    }

    // ---- Ambiente de aprendizaje ----

    public List<AmbienteAprendizaje> listarAmbientes() {
        return ambienteRepository.findAllByOrderByIdAmbientesDeAprendizajeDesc();
    }

    public boolean crearAmbiente(String descripcion, int idSede) {
        return sedeRepository.findById(idSede).map(sede -> {
            AmbienteAprendizaje ambiente = new AmbienteAprendizaje();
            ambiente.setIdAmbientesDeAprendizaje(
                    siguienteId(ambienteRepository.findAll().stream().map(AmbienteAprendizaje::getIdAmbientesDeAprendizaje)));
            ambiente.setDescripcionAmbiente(limitar(descripcion));
            ambiente.setSede(sede);
            return guardar(() -> ambienteRepository.save(ambiente));
        }).orElse(false);
    }

    public boolean actualizarAmbiente(int id, String descripcion, int idSede) {
        Sede sede = sedeRepository.findById(idSede).orElse(null);
        if (sede == null) return false;
        return ambienteRepository.findById(id).map(ambiente -> {
            ambiente.setDescripcionAmbiente(limitar(descripcion));
            ambiente.setSede(sede);
            return guardar(() -> ambienteRepository.save(ambiente));
        }).orElse(false);
    }

    public boolean eliminarAmbiente(int id) {
        return eliminar(() -> ambienteRepository.deleteById(id));
    }

    // ---- Area ----

    public List<Area> listarAreas() {
        return areaRepository.findAllByOrderByNombreAreaAsc();
    }

    public boolean crearArea(String nombre, int idSede) {
        return sedeRepository.findById(idSede).map(sede -> {
            Area area = new Area();
            area.setIdArea(siguienteId(areaRepository.findAll().stream().map(Area::getIdArea)));
            area.setNombreArea(limitar(nombre));
            area.setSede(sede);
            return guardar(() -> areaRepository.save(area));
        }).orElse(false);
    }

    public boolean actualizarArea(int id, String nombre, int idSede) {
        Sede sede = sedeRepository.findById(idSede).orElse(null);
        if (sede == null) return false;
        return areaRepository.findById(id).map(area -> {
            area.setNombreArea(limitar(nombre));
            area.setSede(sede);
            return guardar(() -> areaRepository.save(area));
        }).orElse(false);
    }

    public boolean eliminarArea(int id) {
        return eliminar(() -> areaRepository.deleteById(id));
    }

    // ---- Tipo de mantenimiento ----

    public List<TipoMantenimiento> listarTiposMantenimiento() {
        return tipoMantenimientoRepository.findAllByOrderByIdTipoMantenientoAsc();
    }

    public boolean crearTipoMantenimiento(String descripcion) {
        TipoMantenimiento tipo = new TipoMantenimiento();
        tipo.setDescripcionMantenimiento(limitar(descripcion));
        return guardar(() -> tipoMantenimientoRepository.save(tipo));
    }

    public boolean actualizarTipoMantenimiento(int id, String descripcion) {
        return tipoMantenimientoRepository.findById(id).map(tipo -> {
            tipo.setDescripcionMantenimiento(limitar(descripcion));
            return guardar(() -> tipoMantenimientoRepository.save(tipo));
        }).orElse(false);
    }

    public boolean eliminarTipoMantenimiento(int id) {
        return eliminar(() -> tipoMantenimientoRepository.deleteById(id));
    }

    // ---- Tipo de manual ----

    public List<TipoManual> listarTiposManual() {
        return tipoManualRepository.findAllByOrderByIdTiposManualesAsc();
    }

    public boolean crearTipoManual(String descripcion) {
        TipoManual tipo = new TipoManual();
        tipo.setDescripcionManual(limitar(descripcion));
        return guardar(() -> tipoManualRepository.save(tipo));
    }

    public boolean actualizarTipoManual(int id, String descripcion) {
        return tipoManualRepository.findById(id).map(tipo -> {
            tipo.setDescripcionManual(limitar(descripcion));
            return guardar(() -> tipoManualRepository.save(tipo));
        }).orElse(false);
    }

    public boolean eliminarTipoManual(int id) {
        return eliminar(() -> tipoManualRepository.deleteById(id));
    }

    // ---- Ubicaciones (bitacora de solo lectura) ----

    public List<UbicacionMaquina> listarUbicaciones() {
        return ubicacionMaquinaRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "idUbicacionMaquina"));
    }

    // ---- utilidades compartidas ----

    private int siguienteId(java.util.stream.Stream<Integer> ids) {
        return ids.mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private String limitar(String valor) {
        String limpio = valor == null ? "" : valor.trim();
        return limpio.length() > 150 ? limpio.substring(0, 150) : limpio;
    }

    // Cada llamada a repository.save()/deleteById() ya corre en su propia
    // transaccion (Spring Data JPA), no hace falta @Transactional aqui.
    private boolean guardar(Runnable accion) {
        try {
            accion.run();
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    private boolean eliminar(Runnable accion) {
        try {
            accion.run();
            return true;
        } catch (DataIntegrityViolationException e) {
            // Si el registro esta en uso (llave foranea), no se borra.
            return false;
        }
    }
}
