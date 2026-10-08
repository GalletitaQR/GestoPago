package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.banco.ClienteRequestDto;
import com.proyecto.servicios.model.banco.ClienteResponseDto;
import com.proyecto.servicios.service.banco.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    /**
     * Endpoint para registrar un nuevo cliente.
     */
    @PostMapping
    public ResponseEntity<ClienteResponseDto> registrarCliente(@Valid @RequestBody ClienteRequestDto request) {
        ClienteResponseDto response = clienteService.registrarCliente(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
