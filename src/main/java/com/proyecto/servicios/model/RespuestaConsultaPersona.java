package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta estándar para la consulta de persona")
public class RespuestaConsultaPersona extends GenericResponse {

    @Schema(description = "Datos de la persona consultada")
    private PersonaConsultaDTO datos;

    public RespuestaConsultaPersona(Integer codigo, String mensaje, PersonaConsultaDTO datos) {
        setCodigo(codigo);
        setMensaje(mensaje);
        this.datos = datos;
    }
}
