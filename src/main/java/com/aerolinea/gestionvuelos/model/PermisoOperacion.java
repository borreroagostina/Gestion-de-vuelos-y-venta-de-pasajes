package com.aerolinea.gestionvuelos.model;

public enum PermisoOperacion {
  BUSCAR_VUELOS("Buscar vuelos", "Consultar vuelos por origen, destino, fecha y clase."),
  COMPRAR_PASAJES("Comprar pasajes", "Comprar hasta 9 pasajes y recibir el ticket electronico."),
  GESTIONAR_MIS_VIAJES(
      "Gestionar mis viajes", "Revisar reservas, cambios de horario y alertas del itinerario."),
  GESTIONAR_VENTAS_MOSTRADOR(
      "Gestionar ventas en mostrador",
      "Registrar ventas, emitir comprobantes y asistir pagos presenciales."),
  ADMINISTRAR_RESERVAS(
      "Administrar reservas", "Confirmar reservas y asistir a pasajeros en el check-in."),
  CONSULTAR_DISPONIBILIDAD(
      "Consultar disponibilidad",
      "Ver disponibilidad de asientos y vuelos activos por ruta y horario."),
  GESTIONAR_VUELOS(
      "Gestionar vuelos", "Crear, modificar o cancelar vuelos, rutas, horarios y precios."),
  GESTIONAR_CATALOGO(
      "Gestionar catalogos", "Administrar aeropuertos, aeronaves y configuraciones operativas."),
  CONSULTAR_REPORTES(
      "Consultar reportes", "Acceder a reportes operativos, ventas y ocupacion de vuelos."),
  GESTIONAR_USUARIOS(
      "Gestionar usuarios", "Supervisar usuarios internos, roles y alertas del sistema.");

  private final String titulo;
  private final String descripcion;

  PermisoOperacion(String titulo, String descripcion) {
    this.titulo = titulo;
    this.descripcion = descripcion;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getDescripcion() {
    return descripcion;
  }
}
