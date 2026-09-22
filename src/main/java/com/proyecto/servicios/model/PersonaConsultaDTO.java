package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO con los datos detallados de la persona y el origen de la consulta")
public class PersonaConsultaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID único de la persona", example = "1")
    private Integer id;

    @Schema(description = "Nombre de la persona", example = "Zack")
    private String nombre;

    @Schema(description = "Apellido paterno", example = "González")
    private String apellidoP;

    @Schema(description = "Apellido materno", example = "Pérez")
    private String apellidoMaterno;

    @Schema(description = "Nombre de usuario", example = "zackg")
    private String username;

    @Schema(description = "Rol del usuario en el sistema", example = "ROLE_USER")
    private String rol;

    @Schema(description = "Indica si la cuenta está activa", example = "true")
    private Boolean activo;

    @Schema(description = "Fuente de donde se obtuvieron los datos", example = "REDIS", allowableValues = {"REDIS", "POSTGRESQL"})
    private String fuenteDatos;
}
