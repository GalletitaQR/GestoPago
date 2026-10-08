package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.banco.CuentaResponseDto;
import com.proyecto.servicios.service.banco.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    /**
     * GET /cuentas/activas - Consultar cuentas activas
     */
    @GetMapping("/activas")
    public ResponseEntity<List<CuentaResponseDto>> obtenerCuentasActivas() {
        return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
    }

    /**
     * GET /cuentas/{numeroCuenta} - Consultar cuenta por número de cuenta
     */
    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponseDto> obtenerCuentaPorNumero(@PathVariable String numeroCuenta) {
        CuentaResponseDto response = cuentaService.obtenerCuentaPorNumero(numeroCuenta);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /cuentas/{numeroCuenta}/saldo - Consultar saldo de una cuenta
     */
    @GetMapping("/{numeroCuenta}/saldo")
    public ResponseEntity<Map<String, Object>> obtenerSaldo(@PathVariable String numeroCuenta) {
        BigDecimal saldo = cuentaService.obtenerSaldo(numeroCuenta);
        Map<String, Object> response = new HashMap<>();
        response.put("numeroCuenta", numeroCuenta);
        response.put("saldo", saldo);
        return ResponseEntity.ok(response);
    }
}
