package com.proyecto.servicios.model.banco;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Petición para consulta de clientes controlada por banderas sin ID en ruta")
public class ClienteConsultaRequest {

    @Schema(description = "Bandera de operación: 1=Por ID, 2=Por CURP, 3=Por RFC, 4=Por Correo, 5=Por No. Cuenta, 6=Activos, 7=Rango Fechas, 8=Todos", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La bandera de consulta es obligatoria")
    @Min(value = 1, message = "La bandera mínima permitida es 1")
    @Max(value = 8, message = "La bandera máxima permitida es 8")
    private Integer bandera;

    @Schema(description = "ID del cliente (requerido si bandera=1)", example = "1")
    @Positive(message = "El ID debe ser mayor a 0")
    private Long id;

    @Schema(description = "CURP del cliente (requerido si bandera=2)", example = "JUAP800101HDFRRN01")
    @Pattern(regexp = "^$|^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$", message = "Formato de CURP inválido")
    private String curp;

    @Schema(description = "RFC del cliente (requerido si bandera=3)", example = "JUAP8001011A2")
    @Pattern(regexp = "^$|^[A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3}$", message = "Formato de RFC inválido")
    private String rfc;

    @Schema(description = "Correo del cliente (requerido si bandera=4)", example = "cliente@correo.com")
    @Email(message = "Formato de correo electrónico inválido")
    private String correo;

    @Schema(description = "Número de cuenta bancaria (requerido si bandera=5)", example = "1234567890")
    private String numeroCuenta;

    @Schema(description = "Fecha inicial (requerida si bandera=7)", example = "2024-01-01")
    private LocalDate fechaInicio;

    @Schema(description = "Fecha final (requerida si bandera=7)", example = "2024-12-31")
    private LocalDate fechaFin;
}
