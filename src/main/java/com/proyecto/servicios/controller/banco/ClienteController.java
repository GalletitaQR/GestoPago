package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.banco.ClienteRequestDto;
import com.proyecto.servicios.model.banco.ClienteResponseDto;
import com.proyecto.servicios.service.banco.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    /**
     * POST /clientes - Registrar nuevo cliente
     */
    @PostMapping
    public ResponseEntity<ClienteResponseDto> registrarCliente(@Valid @RequestBody ClienteRequestDto request) {
        ClienteResponseDto response = clienteService.registrarCliente(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /clientes - Consultar todos los clientes
     */
    @GetMapping
    public ResponseEntity<List<ClienteResponseDto>> obtenerTodosLosClientes() {
        return ResponseEntity.ok(clienteService.obtenerTodosLosClientes());
    }

    /**
     * GET /clientes/{id} - Consultar cliente por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDto> obtenerClientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerClientePorId(id));
    }

    /**
     * GET /clientes/curp/{curp} - Consultar cliente por CURP
     */
    @GetMapping("/curp/{curp}")
    public ResponseEntity<ClienteResponseDto> obtenerClientePorCurp(@PathVariable String curp) {
        return ResponseEntity.ok(clienteService.obtenerClientePorCurp(curp));
    }

    /**
     * GET /clientes/rfc/{rfc} - Consultar cliente por RFC
     */
    @GetMapping("/rfc/{rfc}")
    public ResponseEntity<ClienteResponseDto> obtenerClientePorRfc(@PathVariable String rfc) {
        return ResponseEntity.ok(clienteService.obtenerClientePorRfc(rfc));
    }

    /**
     * GET /clientes/correo/{correo} - Consultar cliente por correo
     */
    @GetMapping("/correo/{correo}")
    public ResponseEntity<ClienteResponseDto> obtenerClientePorCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(clienteService.obtenerClientePorCorreo(correo));
    }

    /**
     * GET /clientes/numero-cuenta/{numeroCuenta} - Consultar cliente por número de cuenta
     */
    @GetMapping("/numero-cuenta/{numeroCuenta}")
    public ResponseEntity<ClienteResponseDto> obtenerClientePorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(clienteService.obtenerClientePorNumeroCuenta(numeroCuenta));
    }

    /**
     * GET /clientes/activos - Consultar clientes activos
     */
    @GetMapping("/activos")
    public ResponseEntity<List<ClienteResponseDto>> obtenerClientesActivos() {
        return ResponseEntity.ok(clienteService.obtenerClientesActivos());
    }

    /**
     * GET /clientes/rango-fechas - Obtener clientes registrados en un rango de fechas
     */
    @GetMapping("/rango-fechas")
    public ResponseEntity<List<ClienteResponseDto>> obtenerClientesPorRangoFechas(
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam("fin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(clienteService.obtenerClientesPorRangoFechas(inicio, fin));
    }

    /**
     * PUT /clientes/{id} - Actualizar información del cliente
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDto> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody com.proyecto.servicios.model.banco.ClienteUpdateDto request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }
}
