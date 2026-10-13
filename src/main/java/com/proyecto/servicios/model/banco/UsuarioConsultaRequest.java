package com.proyecto.servicios.model.banco;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "Petición para consulta de usuarios controlada por banderas sin ID en ruta")
public class UsuarioConsultaRequest {

    @Schema(description = "Bandera de operación: 1=Por ID, 2=Por Correo, 3=Listar todos", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La bandera de consulta es obligatoria")
    @Min(value = 1, message = "La bandera mínima permitida es 1 (1: Por ID, 2: Por Correo, 3: Todos)")
    @Max(value = 3, message = "La bandera máxima permitida es 3")
    private Integer bandera;

    @Schema(description = "ID del usuario (requerido si bandera=1)", example = "1")
    @Positive(message = "El ID debe ser un número entero positivo mayor a 0")
    private Long id;

    @Schema(description = "Correo del usuario (requerido si bandera=2)", example = "usuario@correo.com")
    @Email(message = "Formato de correo electrónico inválido")
    private String correo;

    @Schema(description = "Indica si se consulta en caché (true) o directo en base de datos (false)", example = "true")
    @Builder.Default
    private Boolean usarCache = true;
}
