package com.fimacor.maquinas.service;

import com.fimacor.catalogos.repository.AmbienteAprendizajeRepository;
import com.fimacor.catalogos.repository.AreaRepository;
import com.fimacor.catalogos.repository.EstadoMaquinaRepository;
import com.fimacor.catalogos.entity.Marca;
import com.fimacor.catalogos.repository.MarcaRepository;
import com.fimacor.catalogos.entity.Modelo;
import com.fimacor.catalogos.repository.ModeloRepository;
import com.fimacor.catalogos.repository.SedeRepository;
import com.fimacor.catalogos.repository.TipoMaquinaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fimacor.catalogos.entity.AmbienteAprendizaje;
import com.fimacor.catalogos.entity.Area;
import com.fimacor.catalogos.entity.EstadoMaquina;
import com.fimacor.catalogos.entity.Sede;
import com.fimacor.catalogos.entity.TipoMaquina;
import com.fimacor.maquinas.entity.FichaMaquina;
import com.fimacor.maquinas.repository.FichaMaquinaRepository;

/** Registro y administracion de maquinas. */
@Service
public class MaquinasService {

    private final FichaMaquinaRepository fichaMaquinaRepository;
    private final ModeloRepository modeloRepository;
    private final MarcaRepository marcaRepository;
    private final TipoMaquinaRepository tipoMaquinaRepository;
    private final EstadoMaquinaRepository estadoMaquinaRepository;
    private final AmbienteAprendizajeRepository ambienteRepository;
    private final SedeRepository sedeRepository;
    private final AreaRepository areaRepository;

    public MaquinasService(FichaMaquinaRepository fichaMaquinaRepository, ModeloRepository modeloRepository,
            MarcaRepository marcaRepository, TipoMaquinaRepository tipoMaquinaRepository,
            EstadoMaquinaRepository estadoMaquinaRepository, AmbienteAprendizajeRepository ambienteRepository,
            SedeRepository sedeRepository, AreaRepository areaRepository) {
        this.fichaMaquinaRepository = fichaMaquinaRepository;
        this.modeloRepository = modeloRepository;
        this.marcaRepository = marcaRepository;
        this.tipoMaquinaRepository = tipoMaquinaRepository;
        this.estadoMaquinaRepository = estadoMaquinaRepository;
        this.ambienteRepository = ambienteRepository;
        this.sedeRepository = sedeRepository;
        this.areaRepository = areaRepository;
    }

    public List<FichaMaquina> listar() {
        return fichaMaquinaRepository.findByBajaFalseOrderByIdMaquinasDesc();
    }

    public List<Modelo> listarModelos() {
        return modeloRepository.findAllByOrderByIdModeloDesc();
    }

    public List<Marca> listarMarcas() {
        return marcaRepository.findAllByOrderByDescripcionMarcaAsc();
    }

    public List<com.fimacor.catalogos.entity.TipoMaquina> listarTipos() {
        return tipoMaquinaRepository.findAllByOrderByIdTipoMaquinaDesc();
    }

    public List<com.fimacor.catalogos.entity.EstadoMaquina> listarEstados() {
        return estadoMaquinaRepository.findAllByOrderByIdEstadoDeMaquinaAsc();
    }

    public List<com.fimacor.catalogos.entity.AmbienteAprendizaje> listarAmbientes() {
        return ambienteRepository.findAllByOrderByIdAmbientesDeAprendizajeDesc();
    }

    public List<com.fimacor.catalogos.entity.Sede> listarSedes() {
        return sedeRepository.findAllByOrderByNombreSedeAsc();
    }

    public List<com.fimacor.catalogos.entity.Area> listarAreas() {
        return areaRepository.findAllByOrderByNombreAreaAsc();
    }

    /** id: solo lo trae el registro de una maquina nueva (para poder asignarle el cuentadante de una vez). */
    public record Resultado(boolean ok, String error, Integer id) {
        static Resultado exito() {
            return new Resultado(true, null, null);
        }
        static Resultado exito(Integer id) {
            return new Resultado(true, null, id);
        }
        static Resultado fallo(String motivo) {
            return new Resultado(false, motivo, null);
        }
    }

    public Resultado registrar(String codigoSena, String descripcion, int idTipo, int idMarca, int idModelo,
            int idEstado, int idAmbiente, int idSede, String area, LocalDate fechaCompra, String valorMaquinaTexto,
            boolean tieneGarantia, LocalDate fechaGarantia) {
        String codigo = limitar(codigoSena, 45).toUpperCase();
        String descripcionLimpia = limitar(descripcion, 255);
        if (codigo.isEmpty() || descripcionLimpia.isEmpty() || area.isBlank()) return Resultado.fallo("datos");

        Modelo modelo = modeloRepository.findById(idModelo).orElse(null);
        if (modelo == null || modelo.getMarca() == null || !modelo.getMarca().getIdMarca().equals(idMarca)) {
            return Resultado.fallo("datos");
        }
        Marca marca = marcaRepository.findById(idMarca).orElse(null);
        var tipo = tipoMaquinaRepository.findById(idTipo).orElse(null);
        var estado = estadoMaquinaRepository.findById(idEstado).orElse(null);
        var ambiente = ambienteRepository.findById(idAmbiente).orElse(null);
        var sede = sedeRepository.findById(idSede).orElse(null);
        if (marca == null || tipo == null || estado == null || ambiente == null || sede == null) {
            return Resultado.fallo("datos");
        }

        if (fichaMaquinaRepository.findByCodigoSenaIgnoreCase(codigo).isPresent()) {
            return Resultado.fallo("codigo");
        }

        FichaMaquina f = new FichaMaquina();
        f.setCodigoSena(codigo);
        f.setDescripcion(descripcionLimpia);
        f.setModelo(modelo);
        f.setMarca(marca);
        f.setTipoMaquina(tipo);
        f.setEstado(estado);
        f.setAmbiente(ambiente);
        f.setFechaUbicacionAmbiente(LocalDate.now());
        f.setSede(sede);
        f.setArea(limitar(area, 100));
        f.setFechaCompra(fechaCompra);
        f.setValorMaquina(normalizarValorMoneda(valorMaquinaTexto));
        f.setTieneGarantia(tieneGarantia);
        f.setFechaGarantia(tieneGarantia ? fechaGarantia : null);
        FichaMaquina guardada = fichaMaquinaRepository.save(f);
        return Resultado.exito(guardada.getIdMaquinas());
    }

    public Resultado actualizar(int idMaquina, String codigoSena, String descripcion, int idTipo, int idMarca,
            int idModelo, int idEstado, int idAmbiente, int idSede, String area, LocalDate fechaCompra,
            String valorMaquinaTexto, boolean tieneGarantia, LocalDate fechaGarantia) {
        FichaMaquina actual = fichaMaquinaRepository.findById(idMaquina).orElse(null);
        if (actual == null) return Resultado.fallo("datos");

        String codigo = limitar(codigoSena, 45).toUpperCase();
        String descripcionLimpia = limitar(descripcion, 255);
        Modelo modelo = modeloRepository.findById(idModelo).orElse(null);
        if (codigo.isEmpty() || descripcionLimpia.isEmpty() || modelo == null
                || modelo.getMarca() == null || !modelo.getMarca().getIdMarca().equals(idMarca)) {
            return Resultado.fallo("datos");
        }
        Marca marca = marcaRepository.findById(idMarca).orElse(null);
        var tipo = tipoMaquinaRepository.findById(idTipo).orElse(null);
        var estado = estadoMaquinaRepository.findById(idEstado).orElse(null);
        var ambiente = ambienteRepository.findById(idAmbiente).orElse(null);
        var sede = sedeRepository.findById(idSede).orElse(null);
        if (marca == null || tipo == null || estado == null || ambiente == null || sede == null) {
            return Resultado.fallo("datos");
        }

        boolean cambioAmbiente = actual.getAmbiente() == null || !actual.getAmbiente().getIdAmbientesDeAprendizaje().equals(idAmbiente);

        actual.setCodigoSena(codigo);
        actual.setDescripcion(descripcionLimpia);
        actual.setModelo(modelo);
        actual.setMarca(marca);
        actual.setTipoMaquina(tipo);
        actual.setEstado(estado);
        actual.setAmbiente(ambiente);
        actual.setFechaUbicacionAmbiente(cambioAmbiente ? LocalDate.now() : actual.getFechaUbicacionAmbiente());
        actual.setSede(sede);
        actual.setArea(limitar(area, 100));
        actual.setFechaCompra(fechaCompra);
        actual.setValorMaquina(normalizarValorMoneda(valorMaquinaTexto));
        actual.setTieneGarantia(tieneGarantia);
        actual.setFechaGarantia(tieneGarantia ? fechaGarantia : null);
        fichaMaquinaRepository.save(actual);
        return Resultado.exito();
    }

    private BigDecimal normalizarValorMoneda(String valor) {
        String limpio = valor == null ? "" : valor.trim().replace(" ", "");
        if (limpio.isEmpty()) return null;
        if (limpio.contains(",") && limpio.lastIndexOf(",") > limpio.lastIndexOf(".")) {
            limpio = limpio.replace(".", "").replace(",", ".");
        } else {
            limpio = limpio.replace(",", "");
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String limitar(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
