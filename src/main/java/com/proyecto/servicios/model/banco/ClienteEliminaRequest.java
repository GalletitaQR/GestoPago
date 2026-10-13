package com.proyecto.servicios.model.banco;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@Schema(description = "Petición para baja lógica de clientes controlada por banderas sin ID en ruta")
public class ClienteEliminaRequest {

    @Schema(description = "ID del cliente a desactivar", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número entero positivo mayor a 0")
    private Long id;

    @Schema(description = "Bandera de operación: 1=Baja lógica / Desactivación", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La bandera de eliminación es obligatoria")
    @Min(value = 1, message = "La bandera debe ser 1 (Baja lógica)")
    @Max(value = 1, message = "La bandera debe ser 1 (Baja lógica)")
    private Integer bandera;

    @Schema(description = "Motivo de la baja", example = "Solicitud voluntaria del titular")
    private String motivo;
}
