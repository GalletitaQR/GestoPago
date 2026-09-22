package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.service.PersonaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PersonaController {

    @Autowired
    private PersonaService personaService;

    @PostMapping(value = "/personas", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> crearUser(@Valid @RequestBody PersonasRequest personasRequest) {
        return new ResponseEntity<>(personaService.creaPersona(personasRequest), HttpStatus.OK);
    }

    @PutMapping(value = "/personasActualiza", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> actualizUser(@Valid @RequestBody PersonasRequest personasRequest) {
        return new ResponseEntity<>(personaService.actualizaPersona(personasRequest), HttpStatus.OK);
    }

    @PutMapping(value = "/personasElimina", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> actualizUser(@Valid @RequestBody EliminaPersonaRequest personasRequest) {
        return new ResponseEntity<>(personaService.eliminaPersona(personasRequest), HttpStatus.OK);
    }

    /**
     * Endpoint de login con peticiones y respuestas en Map.
     * Soporta solo 1 rol por ahora (ROLE_USER).
     * No truena ante excepciones.
     */
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> loginPayload) {
        try {
            Map<String, Object> response = personaService.login(loginPayload);
            Integer codigo = (Integer) response.getOrDefault("codigo", 200);
            return new ResponseEntity<>(response, HttpStatus.valueOf(codigo));
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("codigo", 500);
            errorMap.put("mensaje", "Error al procesar la petición de login: " + e.getMessage());
            return new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
