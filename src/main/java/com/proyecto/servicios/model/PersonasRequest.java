package com.proyecto.servicios.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
public class PersonasRequest {
    @NotNull(message = "El nombre es obligatorio")
    @NotBlank(message = "El nombre no puede estar en blanco")
    @Max(value = 50, message = "El nombre no puede tener mas de 50 caracteres")
    @Min(value = 3, message = "El nombre debe tener al menos 3 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El nombre solo debe contener letras")
    private String nombre;

    @NotNull(message = "El apellido paterno es obligatorio")
    @NotBlank(message = "El apellido paterno no puede estar en blanco")
    @Max(value = 50, message = "El apellido paterno no puede tener mas de 50 caracteres")
    @Min(value = 3, message = "El apellido paterno debe tener al menos 3 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El apellido paterno solo debe contener letras")
    private String apellidoP;

    @NotNull(message = "El apellido materno es obligatorio")
    @NotBlank(message = "El apellido materno no puede estar en blanco")
    @Max(value = 50, message = "El apellido materno no puede tener mas de 50 caracteres")
    @Min(value = 3, message = "El apellido materno debe tener al menos 3 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El apellido materno solo debe contener letras")
    private String apellidoMaterno;

}
