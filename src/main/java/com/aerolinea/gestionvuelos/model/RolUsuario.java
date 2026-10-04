package com.aerolinea.gestionvuelos.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;

public enum RolUsuario {
  PASAJERO(
      "ROLE_PASAJERO",
      "/pasajero",
      List.of(
          PermisoOperacion.BUSCAR_VUELOS,
          PermisoOperacion.COMPRAR_PASAJES,
          PermisoOperacion.GESTIONAR_MIS_VIAJES)),
  EMPLEADO(
      "ROLE_EMPLEADO",
      "/empleado",
      List.of(
          PermisoOperacion.GESTIONAR_VENTAS_MOSTRADOR,
          PermisoOperacion.ADMINISTRAR_RESERVAS,
          PermisoOperacion.CONSULTAR_DISPONIBILIDAD)),
  ADMIN(
      "ROLE_ADMIN",
      "/admin",
      List.of(
          PermisoOperacion.GESTIONAR_VUELOS,
          PermisoOperacion.GESTIONAR_CATALOGO,
          PermisoOperacion.CONSULTAR_REPORTES,
          PermisoOperacion.GESTIONAR_USUARIOS));

  private final String authority;
  private final String homePath;
  private final List<PermisoOperacion> permisos;

  RolUsuario(String authority, String homePath, List<PermisoOperacion> permisos) {
    this.authority = authority;
    this.homePath = homePath;
    this.permisos = permisos;
  }

  public String getAuthority() {
    return authority;
  }

  public String getHomePath() {
    return homePath;
  }

  public List<PermisoOperacion> getPermisos() {
    return permisos;
  }

  public static Optional<RolUsuario> fromAuthority(String authority) {
    return Arrays.stream(values()).filter(rol -> rol.authority.equals(authority)).findFirst();
  }

  public static Optional<RolUsuario> fromAuthorities(
      Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .map(RolUsuario::fromAuthority)
        .flatMap(Optional::stream)
        .findFirst();
  }
}
