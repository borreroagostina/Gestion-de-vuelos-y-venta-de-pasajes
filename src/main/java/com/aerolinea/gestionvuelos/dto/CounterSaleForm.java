package com.aerolinea.gestionvuelos.dto;

import com.aerolinea.gestionvuelos.model.MetodoPagoMostrador;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class CounterSaleForm {

  private static final int MAX_NAME_LENGTH = 100;
  private static final int MAX_DOCUMENT_LENGTH = 30;
  private static final int MAX_EMAIL_LENGTH = 150;
  private static final int MAX_PHONE_LENGTH = 30;

  @NotBlank(message = "El nombre del cliente es obligatorio")
  @Size(max = MAX_NAME_LENGTH, message = "El nombre no puede superar 100 caracteres")
  private String clienteNombre;

  @NotBlank(message = "El apellido del cliente es obligatorio")
  @Size(max = MAX_NAME_LENGTH, message = "El apellido no puede superar 100 caracteres")
  private String clienteApellido;

  @NotBlank(message = "El documento del cliente es obligatorio")
  @Size(max = MAX_DOCUMENT_LENGTH, message = "El documento no puede superar 30 caracteres")
  private String clienteDocumento;

  @NotBlank(message = "El email del cliente es obligatorio")
  @Email(message = "El email del cliente no es válido")
  @Size(max = MAX_EMAIL_LENGTH, message = "El email no puede superar 150 caracteres")
  private String clienteEmail;

  @NotBlank(message = "El teléfono del cliente es obligatorio")
  @Size(max = MAX_PHONE_LENGTH, message = "El teléfono no puede superar 30 caracteres")
  private String clienteTelefono;

  @NotNull(message = "Seleccioná el método de pago")
  private MetodoPagoMostrador metodoPago;

  @Valid
  @NotEmpty(message = "Debes completar los datos de los pasajeros")
  private List<PasajeroViajeForm> pasajeros = new ArrayList<>();

  public static CounterSaleForm crearParaCantidad(int cantidadAsientos) {
    final CounterSaleForm form = new CounterSaleForm();
    for (int i = 0; i < cantidadAsientos; i++) {
      form.getPasajeros().add(new PasajeroViajeForm());
    }
    return form;
  }

  public String getClienteNombre() {
    return clienteNombre;
  }

  public void setClienteNombre(String clienteNombre) {
    this.clienteNombre = clienteNombre;
  }

  public String getClienteApellido() {
    return clienteApellido;
  }

  public void setClienteApellido(String clienteApellido) {
    this.clienteApellido = clienteApellido;
  }

  public String getClienteDocumento() {
    return clienteDocumento;
  }

  public void setClienteDocumento(String clienteDocumento) {
    this.clienteDocumento = clienteDocumento;
  }

  public String getClienteEmail() {
    return clienteEmail;
  }

  public void setClienteEmail(String clienteEmail) {
    this.clienteEmail = clienteEmail;
  }

  public String getClienteTelefono() {
    return clienteTelefono;
  }

  public void setClienteTelefono(String clienteTelefono) {
    this.clienteTelefono = clienteTelefono;
  }

  public MetodoPagoMostrador getMetodoPago() {
    return metodoPago;
  }

  public void setMetodoPago(MetodoPagoMostrador metodoPago) {
    this.metodoPago = metodoPago;
  }

  public List<PasajeroViajeForm> getPasajeros() {
    return pasajeros;
  }

  public void setPasajeros(List<PasajeroViajeForm> pasajeros) {
    this.pasajeros = pasajeros;
  }
}
