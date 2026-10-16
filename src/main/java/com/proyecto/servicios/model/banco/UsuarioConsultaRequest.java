package com.proyecto.servicios.model.banco;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Petición para consulta dinámica de usuarios con combinación de filtros sin ID en ruta")
public class UsuarioConsultaRequest {

    @Schema(description = "Bandera opcional de operación: 1=Por ID, 2=Por Correo, 3=Listar todos, null/omitido=Búsqueda dinámica multicriterio", example = "1")
    @Min(value = 1, message = "La bandera mínima permitida es 1 (1: Por ID, 2: Por Correo, 3: Todos)")
    @Max(value = 3, message = "La bandera máxima permitida es 3")
    private Integer bandera;

    @Schema(description = "ID del usuario a filtrar", example = "1")
    @Positive(message = "El ID debe ser un número entero positivo mayor a 0")
    private Long id;

    @Schema(description = "ID del cliente bancario asociado a filtrar", example = "1")
    @Positive(message = "El ID del cliente debe ser un número entero positivo mayor a 0")
    private Long clienteId;

    @Schema(description = "Correo del usuario o coincidencia parcial a filtrar", example = "usuario@correo.com")
    private String correo;

    @Schema(description = "Estado de actividad a filtrar (true=Activo, false=Inactivo)", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha inicial de creación para filtro por rango (ISO-8601)", example = "2026-01-01T00:00:00")
    private LocalDateTime fechaCreacionDesde;

    @Schema(description = "Fecha final de creación para filtro por rango (ISO-8601)", example = "2026-12-31T23:59:59")
    private LocalDateTime fechaCreacionHasta;

    @Schema(description = "Indica si se consulta en caché (true) o directo en base de datos (false)", example = "true")
    @Builder.Default
    private Boolean usarCache = true;
}
