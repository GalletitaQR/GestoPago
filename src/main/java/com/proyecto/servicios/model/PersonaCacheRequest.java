package com.proyecto.servicios.model;

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
@Schema(description = "Petición para invalidar/limpiar la caché de una persona controlada por banderas sin ID en ruta")
public class PersonaCacheRequest {

    @Schema(description = "Bandera de operación: 1=Limpiar por ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La bandera de operación es obligatoria")
    @Min(value = 1, message = "La bandera debe ser 1 (Limpiar por ID)")
    @Max(value = 1, message = "La bandera debe ser 1 (Limpiar por ID)")
    private Integer bandera;

    @Schema(description = "ID de la persona a limpiar de la caché", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID de la persona es obligatorio")
    @Positive(message = "El ID debe ser un número entero positivo mayor a 0")
    private Integer id;
}
