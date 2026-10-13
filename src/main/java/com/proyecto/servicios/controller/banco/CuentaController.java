package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.GenericDataResponse;
import com.proyecto.servicios.model.banco.CuentaConsultaRequest;
import com.proyecto.servicios.service.banco.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cuentas")
@Validated
@CrossOrigin(origins = "*")
@Tag(name = "Controlador de Cuentas", description = "Endpoints para consulta de cuentas bancarias controlados por banderas en cuerpo de petición, sin IDs en ruta.")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    /**
     * POST /cuentas/consultar - Consultar cuentas bancarias por banderas en el cuerpo (sin ID en ruta)
     * Bandera 1: Por número de cuenta
     * Bandera 2: Consultar saldo
     * Bandera 3: Cuentas activas
     */
    @Operation(
            summary = "Consultar cuentas bancarias por banderas",
            description = "Consulta cuentas y saldos sin ID en la ruta. Bandera 1=Detalle de cuenta, Bandera 2=Saldo, Bandera 3=Cuentas activas."
    )
    @PostMapping(value = "/consultar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericDataResponse<Object>> consultarCuenta(@Valid @RequestBody CuentaConsultaRequest request) {
        Object datos = cuentaService.consultarPorBandera(request);
        return ResponseEntity.ok(GenericDataResponse.exito("Consulta de cuenta realizada exitosamente.", datos));
    }
}
