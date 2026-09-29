package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.banco.CuentaResponseDto;
import com.proyecto.servicios.service.banco.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    /**
     * Endpoint para consultar una cuenta por número de cuenta.
     */
    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponseDto> obtenerCuentaPorNumero(@PathVariable String numeroCuenta) {
        CuentaResponseDto response = cuentaService.obtenerCuentaPorNumero(numeroCuenta);
        return ResponseEntity.ok(response);
    }
}
