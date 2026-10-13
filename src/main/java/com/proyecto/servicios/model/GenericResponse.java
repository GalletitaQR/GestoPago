package com.proyecto.servicios.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenericResponse {
    private Integer codigo;
    private String mensaje;

    public static GenericResponse exito(String mensaje) {
        return new GenericResponse(200, mensaje);
    }

    public static GenericResponse error(Integer codigo, String mensaje) {
        return new GenericResponse(codigo, mensaje);
    }
}
