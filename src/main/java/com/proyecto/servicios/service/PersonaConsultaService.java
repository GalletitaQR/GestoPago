package com.proyecto.servicios.service;

import com.proyecto.servicios.model.PersonaConsultaDTO;

public interface PersonaConsultaService {
    
    PersonaConsultaDTO obtenerPorId(Integer id);

    PersonaConsultaDTO obtenerPorUsername(String username);

    boolean limpiarCache(Integer id);
}
