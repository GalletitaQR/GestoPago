package com.proyecto.servicios.model.banco;

import com.proyecto.servicios.entity.banco.EstatusCuenta;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaResponseDto {

    private Long id;
    private Long clienteId;
    private String nombreCliente;
    private String numeroCuenta;
    private BigDecimal saldo;
    private EstatusCuenta estatus;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
