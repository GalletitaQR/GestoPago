package com.proyecto.servicios.model.banco;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO de respuesta para información de usuario")
public class UsuarioResponseDto {

    @Schema(description = "Identificador único del usuario", example = "1")
    private Long id;

    @Schema(description = "Identificador del cliente asociado", example = "1")
    private Long clienteId;

    @Schema(description = "Correo electrónico del usuario", example = "usuario@correo.com")
    private String correo;

    @Schema(description = "Estado del usuario (true=Activo, false=Inactivo)", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha de creación controlada por el servidor", example = "2026-10-15T12:00:00")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha de última modificación controlada por el servidor", example = "2026-10-15T12:00:00")
    private LocalDateTime fechaActualizacion;
}
