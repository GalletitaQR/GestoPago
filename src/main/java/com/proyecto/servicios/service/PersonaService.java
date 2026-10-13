package com.proyecto.servicios.service;

import com.proyecto.servicios.model.EliminaPersonaRequest;

import com.proyecto.servicios.model.PersonasRequest;

import java.util.Map;

import com.proyecto.servicios.model.GenericResponse;


public interface PersonaService {
    GenericResponse creaPersona(PersonasRequest personasRequest);
    GenericResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest);
    GenericResponse actualizaPersona(PersonasRequest personasRequest);
    Map<String, Object> login(Map<String, Object> loginRequest);
}
