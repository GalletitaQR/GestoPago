package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.GenericDataResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.banco.*;
import com.proyecto.servicios.service.banco.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@Validated
@CrossOrigin(origins = "*")
@Tag(name = "Controlador de Clientes", description = "Endpoints para registro, consulta, actualización y baja lógica de clientes controlados por banderas en cuerpo de petición, sin IDs en ruta.")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    /**
     * POST /clientes - Registrar nuevo cliente
     */
    @Operation(
            summary = "Registrar nuevo cliente",
            description = "Registra un nuevo cliente con validaciones exhaustivas de Jakarta Bean Validation. Genera automáticamente cuenta bancaria y usuario de acceso."
    )
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericDataResponse<ClienteResponseDto>> registrarCliente(@Valid @RequestBody ClienteRequestDto request) {
        ClienteResponseDto cliente = clienteService.registrarCliente(request);
        return new ResponseEntity<>(
                GenericDataResponse.exito("Cliente registrado exitosamente.", cliente),
                HttpStatus.CREATED
        );
    }

    /**
     * POST /clientes/consultar - Consultar clientes mediante banderas en cuerpo de petición (sin ID en ruta)
     * Bandera 1: Por ID
     * Bandera 2: Por CURP
     * Bandera 3: Por RFC
     * Bandera 4: Por Correo
     * Bandera 5: Por Número de Cuenta
     * Bandera 6: Clientes Activos
     * Bandera 7: Rango de Fechas
     * Bandera 8: Todos los clientes
     */
    @Operation(
            summary = "Consultar clientes por banderas",
            description = "Consulta clientes sin exponer IDs en la ruta. Se controla mediante banderas en el cuerpo (1=ID, 2=CURP, 3=RFC, 4=Correo, 5=No. Cuenta, 6=Activos, 7=Fechas, 8=Todos). Aplica autorización contra IDOR."
    )
    @PostMapping(value = "/consultar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericDataResponse<Object>> consultarCliente(@Valid @RequestBody ClienteConsultaRequest request) {
        Object resultado = clienteService.consultarPorBandera(request);
        return ResponseEntity.ok(GenericDataResponse.exito("Consulta de cliente realizada exitosamente.", resultado));
    }

    /**
     * PUT /clientes/actualizar - Actualizar cliente mediante banderas en cuerpo de petición (sin ID en ruta)
     * Bandera 1: Actualización general
     * Bandera 2: Actualización laboral/ingresos
     */
    @Operation(
            summary = "Actualizar cliente por banderas",
            description = "Actualiza datos del cliente sin ID en la ruta. Bandera 1=Datos generales, Bandera 2=Laboral/ingresos. Valida permisos del usuario autenticado."
    )
    @PutMapping(value = "/actualizar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericDataResponse<ClienteResponseDto>> actualizarCliente(@Valid @RequestBody ClienteActualizaRequest request) {
        ClienteResponseDto cliente = clienteService.actualizarPorBandera(request);
        return ResponseEntity.ok(GenericDataResponse.exito("Cliente actualizado exitosamente.", cliente));
    }

    /**
     * PUT /clientes/eliminar - Baja lógica de cliente mediante banderas en cuerpo de petición (sin ID en ruta)
     * Bandera 1: Baja lógica
     */
    @Operation(
            summary = "Baja lógica de cliente por banderas",
            description = "Desactiva lógicamente al cliente, sus cuentas bancarias y su usuario asociado. Se controla mediante bandera=1 en el cuerpo de petición."
    )
    @PutMapping(value = "/eliminar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> eliminarCliente(@Valid @RequestBody ClienteEliminaRequest request) {
        clienteService.eliminarPorBandera(request);
        return ResponseEntity.ok(GenericResponse.exito("Cliente ID " + request.getId() + " desactivado exitosamente (baja lógica)."));
    }
}
