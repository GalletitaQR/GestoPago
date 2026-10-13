package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.GenericDataResponse;
import com.proyecto.servicios.model.banco.LoginRequestDto;
import com.proyecto.servicios.model.banco.LoginResponseDto;
import com.proyecto.servicios.service.banco.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Validated
@CrossOrigin(origins = "*")
@Tag(name = "Autenticación", description = "Endpoints para inicio de sesión con JWT")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * POST /auth/login - Inicio de sesión
     */
    @Operation(
            summary = "Iniciar sesión",
            description = "Valida credenciales de acceso y genera token JWT de sesión."
    )
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericDataResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(GenericDataResponse.exito("Inicio de sesión exitoso.", response));
    }
}
