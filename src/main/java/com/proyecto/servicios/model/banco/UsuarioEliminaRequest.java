package com.proyecto.servicios.model.banco;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Petición para la baja lógica de un usuario sin ID en ruta")
public class UsuarioEliminaRequest {

    @Schema(description = "ID del usuario a desactivar", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID del usuario es obligatorio")
    @Positive(message = "El ID del usuario debe ser un número entero positivo mayor a 0")
    private Long id;

    @Schema(description = "Motivo de la baja lógica del usuario", example = "Baja solicitada por el usuario")
    @Size(max = 255, message = "El motivo no puede exceder los 255 caracteres")
    private String motivo;
}
