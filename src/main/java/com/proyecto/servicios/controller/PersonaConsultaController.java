package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaConsultaDTO;
import com.proyecto.servicios.model.RespuestaConsultaPersona;
import com.proyecto.servicios.service.PersonaConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/personas")
@Validated
@CrossOrigin(origins = "*")
@Tag(name = "Consulta de Personas (Redis / PostgreSQL)", description = "Endpoints para consultar datos guardados en Redis con respaldo automático en PostgreSQL y validaciones de entrada.")
public class PersonaConsultaController {


    @Autowired
    private PersonaConsultaService personaConsultaService;

    @Operation(
            summary = "Consultar persona por ID",
            description = "Consulta la persona en Redis. Si no existe en Redis, realiza una consulta de respaldo en PostgreSQL y almacena el resultado en Redis con TTL de 10 minutos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Persona encontrada exitosamente (indica si proviene de REDIS o POSTGRESQL)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaConsultaPersona.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido o no positivo (Validación fallida)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "Persona no encontrada ni en Redis ni en PostgreSQL",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class)))
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaConsultaPersona> obtenerPorId(
            @Parameter(description = "ID único de la persona a consultar (debe ser entero positivo mayor a 0)", example = "1", required = true)
            @PathVariable
            @Positive(message = "El ID de la persona debe ser un número entero positivo mayor a 0")
            Integer id
    ) {
        PersonaConsultaDTO datos = personaConsultaService.obtenerPorId(id);
        String msj = "Consulta exitosa desde " + datos.getFuenteDatos();
        return ResponseEntity.ok(new RespuestaConsultaPersona(200, msj, datos));
    }

    @Operation(
            summary = "Buscar persona por nombre de usuario (Username)",
            description = "Busca a una persona por su username primero en la caché de Redis y como respaldo en PostgreSQL."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Persona encontrada exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaConsultaPersona.class))),
            @ApiResponse(responseCode = "400", description = "Nombre de usuario vacío o inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "Persona no encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class)))
    })
    @GetMapping(value = "/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaConsultaPersona> obtenerPorUsername(
            @Parameter(description = "Nombre de usuario (username) a buscar", example = "zackg", required = true)
            @RequestParam
            @NotBlank(message = "El nombre de usuario (username) es obligatorio y no puede estar en blanco")
            String username
    ) {
        PersonaConsultaDTO datos = personaConsultaService.obtenerPorUsername(username);
        String msj = "Consulta exitosa desde " + datos.getFuenteDatos();
        return ResponseEntity.ok(new RespuestaConsultaPersona(200, msj, datos));
    }

    @Operation(
            summary = "Limpiar/Invalidar caché de persona en Redis",
            description = "Permite eliminar manualmente la clave de una persona de Redis para probar la consulta de respaldo a PostgreSQL."
    )
    @DeleteMapping(value = "/cache/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> limpiarCache(
            @Parameter(description = "ID de la persona a limpiar de la caché", example = "1", required = true)
            @PathVariable
            @Positive(message = "El ID debe ser mayor a 0")
            Integer id
    ) {
        boolean eliminado = personaConsultaService.limpiarCache(id);
        GenericResponse response = new GenericResponse();
        if (eliminado) {
            response.setCodigo(200);
            response.setMensaje("La clave de caché para el ID " + id + " fue eliminada correctamente de Redis.");
            return ResponseEntity.ok(response);
        } else {
            response.setCodigo(404);
            response.setMensaje("No se encontró la clave en Redis o no se pudo eliminar para el ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}
