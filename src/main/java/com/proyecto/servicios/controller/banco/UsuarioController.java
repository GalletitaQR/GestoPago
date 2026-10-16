package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.model.GenericDataResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.banco.*;
import com.proyecto.servicios.service.banco.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@Validated
@CrossOrigin(origins = "*")
@Tag(name = "Controlador de Usuarios", description = "Endpoints para registro, consulta dinámica con filtros combinados, actualización y baja lógica de usuarios mediante DTOs en cuerpo de petición, sin IDs en ruta ni parámetros de URL.")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * POST /usuarios - Registrar / Crear nuevo usuario
     * Recibe los datos mediante UsuarioCrearRequest en el cuerpo de la petición.
     */
    @Operation(
            summary = "Registrar nuevo usuario",
            description = "Registra un nuevo usuario asociado a un cliente bancario. Valida fortaleza de contraseña, unicidad de correo y ausencia de usuario previo para el cliente. Los campos de control (id, fechas, estado) son gestionados por el servidor."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericDataResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o reglas de negocio incumplidas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente bancario asociado no encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "409", description = "Correo electrónico ya registrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class)))
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericDataResponse<UsuarioResponseDto>> registrarUsuario(@Valid @RequestBody UsuarioCrearRequest request) {
        UsuarioResponseDto usuario = usuarioService.crearUsuario(request);
        return new ResponseEntity<>(
                GenericDataResponse.creado("Usuario registrado exitosamente.", usuario),
                HttpStatus.CREATED
        );
    }

    /**
     * POST /usuarios/consultar - Endpoint único de consulta dinámica con filtros combinados o por banderas
     * No utiliza @PathVariable ni @RequestParam; todos los criterios se reciben en el cuerpo.
     */
    @Operation(
            summary = "Consultar usuarios (búsqueda dinámica combinable o por banderas)",
            description = "Endpoint único de consulta. Permite combinar múltiples filtros opcionales (id, clienteId, correo, activo, fechas de creación) o utilizar banderas heredadas (1=ID, 2=Correo, 3=Todos). No utiliza variables de ruta ni parámetros URL."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta realizada exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericDataResponse.class))),
            @ApiResponse(responseCode = "400", description = "Criterios de consulta inválidos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado a la información del usuario",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class)))
    })
    @PostMapping(value = "/consultar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericDataResponse<Object>> consultarUsuario(@Valid @RequestBody UsuarioConsultaRequest request) {
        Object datos = usuarioService.consultarPorBandera(request);
        return ResponseEntity.ok(GenericDataResponse.exito("Consulta de usuario(s) realizada exitosamente.", datos));
    }

    /**
     * PUT /usuarios/actualizar - Actualización de usuario sin ID en ruta
     * Recibe los datos a modificar mediante UsuarioActualizaRequest en el cuerpo.
     */
    @Operation(
            summary = "Actualizar usuario",
            description = "Actualiza información del usuario (correo, estado activo, contraseña). El ID objetivo se recibe en el cuerpo de la petición. Impide modificar fechas o campos controlados por el servidor."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericDataResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o contraseña actual incorrecta",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuario inactivo o sin autorización",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "409", description = "El nuevo correo ya pertenece a otro usuario",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class)))
    })
    @PutMapping(value = "/actualizar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericDataResponse<UsuarioResponseDto>> actualizarUsuario(@Valid @RequestBody UsuarioActualizaRequest request) {
        UsuarioResponseDto actualizado = usuarioService.actualizarPorBandera(request);
        return ResponseEntity.ok(GenericDataResponse.exito("Usuario actualizado exitosamente.", actualizado));
    }

    /**
     * PUT /usuarios/eliminar - Baja lógica de usuario sin ID en ruta
     * Recibe el identificador mediante UsuarioEliminaRequest en el cuerpo.
     */
    @Operation(
            summary = "Baja lógica de usuario",
            description = "Desactiva lógicamente al usuario en el sistema sin eliminar su registro histórico. El ID se recibe exclusivamente en el cuerpo de la petición sin variables de ruta."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario desactivado exitosamente (baja lógica)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido en el cuerpo de la petición",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado para modificar este usuario",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class)))
    })
    @PutMapping(value = "/eliminar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> eliminarUsuario(@Valid @RequestBody UsuarioEliminaRequest request) {
        usuarioService.eliminarUsuario(request);
        return ResponseEntity.ok(GenericResponse.exito("Usuario con ID " + request.getId() + " desactivado exitosamente (baja lógica)."));
    }
}
