package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.banco.UpdatePasswordDto;
import com.proyecto.servicios.model.banco.UsuarioResponseDto;
import com.proyecto.servicios.service.banco.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * GET /usuarios/{id} - Consultar usuario por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> obtenerUsuarioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(id));
    }

    /**
     * PUT /usuarios/{id}/password - Actualizar contraseña de usuario
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<Map<String, Object>> actualizarPassword(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePasswordDto request) {
        usuarioService.actualizarPassword(id, request);
        Map<String, Object> response = new HashMap<>();
        response.put("codigo", 200);
        response.put("mensaje", "Contraseña actualizada exitosamente.");
        return ResponseEntity.ok(response);
    }
}
