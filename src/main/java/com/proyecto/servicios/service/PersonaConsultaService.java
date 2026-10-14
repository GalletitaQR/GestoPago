package com.proyecto.servicios.service;

import com.proyecto.servicios.model.PersonaCacheRequest;
import com.proyecto.servicios.model.PersonaConsultaDTO;
import com.proyecto.servicios.model.PersonaConsultaRequest;

public interface PersonaConsultaService {
    
    PersonaConsultaDTO obtenerPorId(Integer id);

    PersonaConsultaDTO obtenerPorUsername(String username);

    boolean limpiarCache(Integer id);

    PersonaConsultaDTO consultarPorBandera(PersonaConsultaRequest request);

    boolean limpiarCachePorBandera(PersonaCacheRequest request);
}
