package com.proyecto.servicios.model.banco;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Petición para consulta de cuentas bancarias controlada por banderas sin ID en ruta")
public class CuentaConsultaRequest {

    @Schema(description = "Bandera de operación: 1=Por número de cuenta, 2=Consultar saldo, 3=Cuentas activas", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La bandera de consulta es obligatoria")
    @Min(value = 1, message = "La bandera mínima permitida es 1")
    @Max(value = 3, message = "La bandera máxima permitida es 3")
    private Integer bandera;

    @Schema(description = "Número de cuenta (requerido si bandera=1 o bandera=2)", example = "1234567890")
    private String numeroCuenta;
}
