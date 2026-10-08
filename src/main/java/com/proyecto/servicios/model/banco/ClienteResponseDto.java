package com.proyecto.servicios.model.banco;

import com.proyecto.servicios.entity.banco.EstadoCivil;
import com.proyecto.servicios.entity.banco.Sexo;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteResponseDto {

    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private Sexo sexo;
    private String nacionalidad;
    private EstadoCivil estadoCivil;
    private String correo;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private DomicilioDto domicilio;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String numeroCuenta;
}
