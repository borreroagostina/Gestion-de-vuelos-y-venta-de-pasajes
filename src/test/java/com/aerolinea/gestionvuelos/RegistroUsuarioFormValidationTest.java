package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertTrue;

import com.aerolinea.gestionvuelos.dto.RegistroUsuarioForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public class RegistroUsuarioFormValidationTest {

  private Validator validator;

  @Before
  public void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  public void shouldRejectInvalidEmailFormat() {
    final RegistroUsuarioForm form = buildValidForm();
    form.setEmail("email-invalido");

    assertHasViolationForField(form, "email", "El formato del email es inválido");
  }

  @Test
  public void shouldRejectBlankName() {
    final RegistroUsuarioForm form = buildValidForm();
    form.setNombre(" ");

    assertHasViolationForField(form, "nombre", "El nombre es obligatorio");
  }

  @Test
  public void shouldRejectBlankSurname() {
    final RegistroUsuarioForm form = buildValidForm();
    form.setApellido(" ");

    assertHasViolationForField(form, "apellido", "El apellido es obligatorio");
  }

  @Test
  public void shouldRejectShortDocument() {
    final RegistroUsuarioForm form = buildValidForm();
    form.setDocumento("123");

    assertHasViolationForField(
        form, "documento", "El documento debe tener entre 6 y 30 caracteres");
  }

  @Test
  public void shouldRejectInvalidPhone() {
    final RegistroUsuarioForm form = buildValidForm();
    form.setTelefono("abc123");

    assertHasViolationForField(
        form, "telefono", "El teléfono debe contener solo números y caracteres válidos");
  }

  @Test
  public void shouldRejectPasswordShorterThanMinimum() {
    final RegistroUsuarioForm form = buildValidForm();
    form.setPassword("Abc12");

    assertHasViolationForField(
        form, "password", "La contraseña debe tener entre 8 y 72 caracteres");
  }

  private void assertHasViolationForField(
      RegistroUsuarioForm form, String field, String expectedMessage) {
    final Set<ConstraintViolation<RegistroUsuarioForm>> violations = validator.validate(form);

    assertTrue(
        "Violations found: "
            + violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", ")),
        violations.stream()
            .anyMatch(
                violation ->
                    field.equals(violation.getPropertyPath().toString())
                        && expectedMessage.equals(violation.getMessage())));
  }

  private RegistroUsuarioForm buildValidForm() {
    final RegistroUsuarioForm form = new RegistroUsuarioForm();
    form.setNombre("Ana");
    form.setApellido("Pérez");
    form.setDocumento("32123123");
    form.setEmail("pasajero@example.com");
    form.setTelefono("+54 11 4444-5555");
    form.setPassword("Clave123");
    return form;
  }
}
