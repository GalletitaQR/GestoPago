package com.proyecto.servicios.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
public class EliminaPersonaRequest {
    @NotNull(message = "El nombre es obligatorio")
    @NotBlank(message = "El nombre no puede estar en blanco")
    @Max(value = 50, message = "El nombre no puede tener mas de 50 caracteres")
    @Min(value = 3, message = "El nombre debe tener al menos 3 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El nombre solo debe contener letras")
    private String nombre;
}
