package com.proyecto.servicios.model.banco;

import io.swagger.v3.oas.annotations.media.Schema;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Petición para actualización de usuarios controlada por banderas sin ID en ruta")
public class UsuarioActualizaRequest {

    @Schema(description = "ID del usuario a actualizar", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID del usuario es obligatorio")
    @Positive(message = "El ID de usuario debe ser un número entero positivo mayor a 0")
    private Long id;

    @Schema(description = "Bandera de operación: 1=Cambiar contraseña, 2=Cambiar estado activo", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La bandera de actualización es obligatoria")
    @Min(value = 1, message = "Bandera inválida (1: Contraseña, 2: Estado activo)")
    @Max(value = 2, message = "Bandera inválida (1: Contraseña, 2: Estado activo)")
    private Integer bandera;

    @Schema(description = "Contraseña actual (requerida si bandera=1)", example = "Password123#")
    private String passwordActual;

    @Schema(description = "Nueva contraseña (requerida si bandera=1)", example = "NuevaPassword456$")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&!#%&'()*+,-./:;<=>?@\\[\\]^_`{|}~])[A-Za-z\\d@$!%*?&!#%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]{8,}$",
            message = "La nueva contraseña debe tener al menos 8 caracteres, mayúscula, minúscula, número y carácter especial"
    )
    private String nuevaPassword;

    @Schema(description = "Estado de actividad (requerido si bandera=2)", example = "true")
    private Boolean activo;
}
