package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta genérica con datos tipados")
public class GenericDataResponse<T> extends GenericResponse {

    @Schema(description = "Cuerpo de datos de la respuesta")
    private T datos;

    public GenericDataResponse(Integer codigo, String mensaje, T datos) {
        super(codigo, mensaje);
        this.datos = datos;
    }

    public static <T> GenericDataResponse<T> exito(String mensaje, T datos) {
        return new GenericDataResponse<>(200, mensaje, datos);
    }

    public static <T> GenericDataResponse<T> creado(String mensaje, T datos) {
        return new GenericDataResponse<>(201, mensaje, datos);
    }
}
