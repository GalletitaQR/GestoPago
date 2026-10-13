package com.proyecto.servicios.model.banco;

import com.proyecto.servicios.entity.banco.EstadoCivil;
import com.proyecto.servicios.entity.banco.Sexo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Petición para actualización de clientes controlada por banderas sin ID en ruta")
public class ClienteActualizaRequest {

    @Schema(description = "ID del cliente a actualizar", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número entero positivo mayor a 0")
    private Long id;

    @Schema(description = "Bandera de operación: 1=Actualización general, 2=Información laboral/ingresos", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La bandera de actualización es obligatoria")
    @Min(value = 1, message = "Bandera inválida (1: Actualización general, 2: Laboral/Ingresos)")
    @Max(value = 2, message = "Bandera inválida (1: Actualización general, 2: Laboral/Ingresos)")
    private Integer bandera;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe contener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre no puede exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe contener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe contener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    private LocalDate fechaNacimiento;

    @NotNull(message = "El sexo es obligatorio")
    private Sexo sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Size(max = 50, message = "La nacionalidad no puede exceder 50 caracteres")
    private String nacionalidad;

    @NotNull(message = "El estado civil es obligatorio")
    private EstadoCivil estadoCivil;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Formato de correo electrónico inválido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos")
    private String telefonoMovil;

    private String telefonoAlternativo;

    @NotNull(message = "Los datos de domicilio son obligatorios")
    @Valid
    private DomicilioDto domicilio;

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 100, message = "La ocupación no puede exceder 100 caracteres")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    // Campos de restricción
    private String curp;
    private String rfc;
    private String numeroCuenta;
}
