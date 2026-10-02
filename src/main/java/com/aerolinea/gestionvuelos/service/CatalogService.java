package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.repository.AeronaveRepository;
import com.aerolinea.gestionvuelos.repository.AeropuertoRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

  private final AeropuertoRepository aeropuertoRepository;
  private final AeronaveRepository aeronaveRepository;
  private final JdbcTemplate jdbcTemplate;

  public CatalogService(
      AeropuertoRepository aeropuertoRepository,
      AeronaveRepository aeronaveRepository,
      JdbcTemplate jdbcTemplate) {
    this.aeropuertoRepository = aeropuertoRepository;
    this.aeronaveRepository = aeronaveRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Aeropuerto> listarAeropuertos() {
    return aeropuertoRepository.findAllByOrderByNombreAsc();
  }

  public Page<Aeropuerto> listarAeropuertosPaginado(int page, int size, String filtro) {
    final Pageable pageable = PageRequest.of(page, size);
    if (filtro == null || filtro.isBlank()) {
      return aeropuertoRepository.findAllByOrderByNombreAsc(pageable);
    }
    return aeropuertoRepository.searchByNombreOrCiudad(filtro, pageable);
  }

  public List<Aeronave> listarAeronaves() {
    return aeronaveRepository.findAllByOrderByModeloAsc();
  }

  public Page<Aeronave> listarAeronavesPaginado(int page, int size, String filtro) {
    final Pageable pageable = PageRequest.of(page, size);
    if (filtro == null || filtro.isBlank()) {
      return aeronaveRepository.findAllByOrderByModeloAsc(pageable);
    }
    return aeronaveRepository.searchByModeloOrFabricante(filtro, pageable);
  }

  public Aeropuerto obtenerAeropuerto(Long id) {
    return aeropuertoRepository.findById(id).orElse(null);
  }

  public Aeropuerto guardarAeropuerto(Aeropuerto aeropuerto) {
    validarAeropuerto(aeropuerto);
    aeropuerto.setCodigoIata(aeropuerto.getCodigoIata().trim().toUpperCase(Locale.ROOT));
    if (aeropuertoRepository.existsByCodigoIataIgnoreCase(aeropuerto.getCodigoIata())) {
      throw new IllegalArgumentException("Ya existe un aeropuerto con ese código IATA");
    }
    return aeropuertoRepository.save(aeropuerto);
  }

  public Aeropuerto actualizarAeropuerto(Aeropuerto aeropuerto) {
    if (aeropuerto == null || aeropuerto.getId() == null) {
      throw new IllegalArgumentException("Debe indicar el aeropuerto a modificar");
    }
    final Aeropuerto existente =
        aeropuertoRepository
            .findById(aeropuerto.getId())
            .orElseThrow(() -> new IllegalArgumentException("No existe el aeropuerto indicado"));
    validarAeropuerto(aeropuerto);
    aeropuerto.setCodigoIata(aeropuerto.getCodigoIata().trim().toUpperCase(Locale.ROOT));
    if (aeropuertoRepository.existsByCodigoIataIgnoreCaseAndIdNot(
        aeropuerto.getCodigoIata(), aeropuerto.getId())) {
      throw new IllegalArgumentException("Ya existe un aeropuerto con ese código IATA");
    }
    existente.setCodigoIata(aeropuerto.getCodigoIata());
    existente.setNombre(aeropuerto.getNombre());
    existente.setCiudad(aeropuerto.getCiudad());
    existente.setPais(aeropuerto.getPais());
    return aeropuertoRepository.save(existente);
  }

  public void eliminarAeropuerto(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("Debe indicar el aeropuerto a eliminar");
    }
    if (countFlightsUsingAirport(id) > 0L) {
      throw new IllegalStateException(
          "No se puede eliminar el aeropuerto porque tiene vuelos asociados");
    }
    aeropuertoRepository.deleteById(id);
  }

  public Aeronave obtenerAeronave(Long id) {
    return aeronaveRepository.findById(id).orElse(null);
  }

  public Aeronave guardarAeronave(Aeronave aeronave) {
    validarAeronave(aeronave);
    aeronave.setCodigo(aeronave.getCodigo().trim().toUpperCase(Locale.ROOT));
    if (aeronaveRepository.existsByCodigoIgnoreCase(aeronave.getCodigo())) {
      throw new IllegalArgumentException("Ya existe una aeronave con ese código");
    }
    return aeronaveRepository.save(aeronave);
  }

  public Aeronave actualizarAeronave(Aeronave aeronave) {
    if (aeronave == null || aeronave.getId() == null) {
      throw new IllegalArgumentException("Debe indicar la aeronave a modificar");
    }
    final Aeronave existente =
        aeronaveRepository
            .findById(aeronave.getId())
            .orElseThrow(() -> new IllegalArgumentException("No existe la aeronave indicada"));
    validarAeronave(aeronave);
    aeronave.setCodigo(aeronave.getCodigo().trim().toUpperCase(Locale.ROOT));
    if (aeronaveRepository.existsByCodigoIgnoreCaseAndIdNot(
        aeronave.getCodigo(), aeronave.getId())) {
      throw new IllegalArgumentException("Ya existe una aeronave con ese código");
    }
    existente.setModelo(aeronave.getModelo());
    existente.setFabricante(aeronave.getFabricante());
    existente.setCodigo(aeronave.getCodigo());
    existente.setEconomySeats(aeronave.getEconomySeats());
    existente.setPrimeraClaseSeats(aeronave.getPrimeraClaseSeats());
    return aeronaveRepository.save(existente);
  }

  public void eliminarAeronave(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("Debe indicar la aeronave a eliminar");
    }
    if (countFlightsUsingAircraft(id) > 0L) {
      throw new IllegalStateException(
          "No se puede eliminar la aeronave porque tiene vuelos asociados");
    }
    aeronaveRepository.deleteById(id);
  }

  public void validarAeropuerto(Aeropuerto aeropuerto) {
    if (aeropuerto == null) {
      throw new IllegalArgumentException("El aeropuerto es obligatorio");
    }
    if (aeropuerto.getCodigoIata() == null || aeropuerto.getCodigoIata().isBlank()) {
      throw new IllegalArgumentException("El código IATA es obligatorio");
    }
    if (aeropuerto.getNombre() == null || aeropuerto.getNombre().isBlank()) {
      throw new IllegalArgumentException("El nombre del aeropuerto es obligatorio");
    }
    if (aeropuerto.getCiudad() == null || aeropuerto.getCiudad().isBlank()) {
      throw new IllegalArgumentException("La ciudad es obligatoria");
    }
    if (aeropuerto.getPais() == null || aeropuerto.getPais().isBlank()) {
      throw new IllegalArgumentException("El país es obligatorio");
    }
  }

  public void validarAeronave(Aeronave aeronave) {
    if (aeronave == null) {
      throw new IllegalArgumentException("La aeronave es obligatoria");
    }
    if (aeronave.getModelo() == null || aeronave.getModelo().isBlank()) {
      throw new IllegalArgumentException("El modelo es obligatorio");
    }
    if (aeronave.getFabricante() == null || aeronave.getFabricante().isBlank()) {
      throw new IllegalArgumentException("El fabricante es obligatorio");
    }
    if (aeronave.getCodigo() == null || aeronave.getCodigo().isBlank()) {
      throw new IllegalArgumentException("El código de la aeronave es obligatorio");
    }
    if (aeronave.getEconomySeats() == null || aeronave.getEconomySeats() < 0) {
      throw new IllegalArgumentException("La cantidad de asientos de Economy es inválida");
    }
    if (aeronave.getPrimeraClaseSeats() == null || aeronave.getPrimeraClaseSeats() < 0) {
      throw new IllegalArgumentException("La cantidad de asientos de Primera Clase es inválida");
    }
  }

  private long countFlightsUsingAirport(Long aeropuertoId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM vuelos WHERE origen_aeropuerto_id = ? OR destino_aeropuerto_id = ?",
        Long.class,
        aeropuertoId,
        aeropuertoId);
  }

  private long countFlightsUsingAircraft(Long aeronaveId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM vuelos WHERE aeronave_id = ?", Long.class, aeronaveId);
  }
}
