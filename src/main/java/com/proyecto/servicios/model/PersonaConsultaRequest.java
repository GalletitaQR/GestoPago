package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Petición para consulta de personas controlada por banderas sin ID en ruta")
public class PersonaConsultaRequest {

    @Schema(description = "Bandera de consulta: 1=Por ID, 2=Por Username", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La bandera de consulta es obligatoria")
    @Min(value = 1, message = "La bandera mínima permitida es 1 (1: Por ID, 2: Por Username)")
    @Max(value = 2, message = "La bandera máxima permitida es 2 (1: Por ID, 2: Por Username)")
    private Integer bandera;

    @Schema(description = "ID único de la persona (requerido si bandera=1)", example = "1")
    @Positive(message = "El ID de la persona debe ser un número entero positivo mayor a 0")
    private Integer id;

    @Schema(description = "Nombre de usuario (requerido si bandera=2)", example = "zackg")
    @Size(max = 50, message = "El username no puede exceder 50 caracteres")
    private String username;
}
