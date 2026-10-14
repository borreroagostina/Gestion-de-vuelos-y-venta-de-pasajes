package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertTrue;

import com.aerolinea.gestionvuelos.dto.DatosPasajeroForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public class DatosPasajeroFormValidationTest {

  private static final int BIRTH_YEAR = 1990;
  private static final int BIRTH_DAY = 12;

  private Validator validator;

  @Before
  public void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  public void shouldRejectBlankName() {
    final DatosPasajeroForm form = buildValidForm();
    form.setNombre(" ");

    assertHasViolationForField(form, "nombre", "El nombre es obligatorio");
  }

  @Test
  public void shouldRejectInvalidDocument() {
    final DatosPasajeroForm form = buildValidForm();
    form.setDocumento("12 34");

    assertHasViolationForField(
        form, "documento", "El DNI/Pasaporte solo puede contener letras, números y guiones");
  }

  @Test
  public void shouldRejectFutureBirthDate() {
    final DatosPasajeroForm form = buildValidForm();
    form.setFechaNacimiento(LocalDate.now().plusDays(1));

    assertHasViolationForField(
        form, "fechaNacimiento", "La fecha de nacimiento debe ser anterior a hoy");
  }

  private void assertHasViolationForField(
      DatosPasajeroForm form, String field, String expectedMessage) {
    final Set<ConstraintViolation<DatosPasajeroForm>> violations = validator.validate(form);

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

  private DatosPasajeroForm buildValidForm() {
    final DatosPasajeroForm form = new DatosPasajeroForm();
    form.setReservaAsientoId(1L);
    form.setNumeroAsiento("07A");
    form.setClase("Economy");
    form.setNombre("Ana");
    form.setApellido("Pérez");
    form.setDocumento("ABC12345");
    form.setFechaNacimiento(LocalDate.of(BIRTH_YEAR, 5, BIRTH_DAY));
    return form;
  }
}
