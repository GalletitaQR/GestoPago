package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaCacheRequest;
import com.proyecto.servicios.model.PersonaConsultaDTO;
import com.proyecto.servicios.model.PersonaConsultaRequest;
import com.proyecto.servicios.model.RespuestaConsultaPersona;
import com.proyecto.servicios.service.PersonaConsultaService;
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
@RequestMapping("/api/v1/personas")
@Validated
@CrossOrigin(origins = "*")
@Tag(name = "Consulta de Personas (Redis / PostgreSQL)", description = "Endpoints para consultar e invalidar datos guardados en Redis con respaldo en PostgreSQL, controlados por banderas en cuerpo de petición sin IDs en ruta.")
public class PersonaConsultaController {

    @Autowired
    private PersonaConsultaService personaConsultaService;

    @Operation(
            summary = "Consultar persona por banderas",
            description = "Consulta la persona sin ID en la ruta. Se controla mediante la bandera enviada en el cuerpo de la petición: Bandera 1 = Por ID (Redis / Respaldo PostgreSQL con TTL de 10 minutos), Bandera 2 = Por Username. Valida parámetros de entrada con Jakarta."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Persona encontrada exitosamente (indica si proviene de REDIS o POSTGRESQL)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaConsultaPersona.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos o bandera incorrecta",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "Persona no encontrada ni en Redis ni en PostgreSQL",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class)))
    })
    @PostMapping(value = "/consultar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaConsultaPersona> consultarPersona(@Valid @RequestBody PersonaConsultaRequest request) {
        PersonaConsultaDTO datos = personaConsultaService.consultarPorBandera(request);
        String msj = "Consulta exitosa desde " + datos.getFuenteDatos();
        return ResponseEntity.ok(new RespuestaConsultaPersona(200, msj, datos));
    }

    @Operation(
            summary = "Limpiar/Invalidar caché de persona en Redis",
            description = "Permite invalidar manualmente la clave de una persona en Redis controlada por bandera en el cuerpo de petición (Bandera 1 = Por ID). No expone identificadores en la ruta."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clave de caché eliminada exitosamente de Redis",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "ID o bandera inválida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "404", description = "Clave no encontrada en Redis",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GenericResponse.class)))
    })
    @PostMapping(value = "/cache/limpiar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> limpiarCache(@Valid @RequestBody PersonaCacheRequest request) {
        boolean eliminado = personaConsultaService.limpiarCachePorBandera(request);
        if (eliminado) {
            return ResponseEntity.ok(GenericResponse.exito(
                    "La clave de caché para el ID " + request.getId() + " fue eliminada correctamente de Redis."
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "No se encontró la clave en Redis o no se pudo eliminar para el ID: " + request.getId()
            ));
        }
    }
}
