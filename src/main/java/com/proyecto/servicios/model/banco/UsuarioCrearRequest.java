package com.proyecto.servicios.model.banco;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Petición para el registro / creación de un nuevo usuario")
public class UsuarioCrearRequest {

    @Schema(description = "ID del cliente bancario asociado al usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número entero positivo mayor a 0")
    private Long clienteId;

    @Schema(description = "Correo electrónico del usuario (será su nombre de usuario de acceso)", example = "usuario@correo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe tener un formato válido (ej. usuario@dominio.com)")
    @Size(max = 100, message = "El correo no puede exceder los 100 caracteres")
    private String correo;

    @Schema(description = "Contraseña de acceso inicial (mínimo 8 caracteres, al menos una mayúscula, minúscula, número y símbolo especial)", example = "Password123#", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 50, message = "La contraseña debe tener entre 8 y 50 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&!#%&'()*+,-./:;<=>?@\\[\\]^_`{|}~])[A-Za-z\\d@$!%*?&!#%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]{8,}$",
            message = "La contraseña debe contener mínimo 8 caracteres, al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    private String password;
}
