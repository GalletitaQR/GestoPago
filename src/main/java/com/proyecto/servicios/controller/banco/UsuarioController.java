package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.GenericDataResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.banco.UsuarioActualizaRequest;
import com.proyecto.servicios.model.banco.UsuarioConsultaRequest;
import com.proyecto.servicios.service.banco.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@Validated
@CrossOrigin(origins = "*")
@Tag(name = "Controlador de Usuarios", description = "Endpoints para consulta y actualización de usuarios controlados por banderas en cuerpo de petición, sin IDs en ruta.")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * POST /usuarios/consultar - Consulta de usuario controlada por banderas en el cuerpo (sin ID en ruta)
     * Bandera 1: Por ID
     * Bandera 2: Por Correo
     * Bandera 3: Listar todos
     */
    @Operation(
            summary = "Consultar usuarios por banderas",
            description = "Consulta usuarios sin exponer IDs en la ruta. Se controla mediante la bandera enviada en el cuerpo (1=ID, 2=Correo, 3=Todos). Integra búsqueda con respaldo en caché o base de datos."
    )
    @PostMapping(value = "/consultar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericDataResponse<Object>> consultarUsuario(@Valid @RequestBody UsuarioConsultaRequest request) {
        Object datos = usuarioService.consultarPorBandera(request);
        return ResponseEntity.ok(GenericDataResponse.exito("Consulta de usuario realizada exitosamente.", datos));
    }

    /**
     * PUT /usuarios/actualizar - Actualización de usuario controlada por banderas en el cuerpo (sin ID en ruta)
     * Bandera 1: Cambio de contraseña
     * Bandera 2: Cambio de estado activo
     */
    @Operation(
            summary = "Actualizar usuarios por banderas",
            description = "Actualiza información del usuario sin ID en la ruta. Bandera 1=Contraseña, Bandera 2=Estado activo. Aplica validación de autorización del usuario en sesión."
    )
    @PutMapping(value = "/actualizar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> actualizarUsuario(@Valid @RequestBody UsuarioActualizaRequest request) {
        usuarioService.actualizarPorBandera(request);
        String mensaje = request.getBandera() == 1 ? "Contraseña actualizada exitosamente." : "Estado de usuario actualizado exitosamente.";
        return ResponseEntity.ok(GenericResponse.exito(mensaje));
    }
}
