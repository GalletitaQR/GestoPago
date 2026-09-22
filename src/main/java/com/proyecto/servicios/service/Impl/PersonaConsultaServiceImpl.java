package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.PersonaConsultaDTO;
import com.proyecto.servicios.model.RecursoNoEncontradoException;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.PersonaConsultaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PersonaConsultaServiceImpl implements PersonaConsultaService {

    private static final String CACHE_KEY_ID = "persona:id:";
    private static final String CACHE_KEY_USER = "persona:user:";
    private static final long CACHE_TTL_MINUTES = 10;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PersonasRepository personasRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PersonaConsultaDTO obtenerPorId(Integer id) {
        String cacheKey = CACHE_KEY_ID + id;

        // 1. Intentar consultar en Redis
        try {
            Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
            if (cachedObj != null) {
                PersonaConsultaDTO dto = convertirADTO(cachedObj);
                if (dto != null) {
                    dto.setFuenteDatos("REDIS");
                    log.info("Éxito: Datos obtenidos desde la caché de Redis para ID={}", id);
                    return dto;
                }
            }
        } catch (Exception e) {
            log.warn("Advertencia: No se pudo consultar en Redis ({}), se procede a consultar respaldo PostgreSQL", e.getMessage());
        }

        // 2. Fallback / Respaldo a PostgreSQL
        log.info("Cache Miss en Redis para ID={}. Consultando en base de datos PostgreSQL...", id);
        Optional<Personas> personaOpt = personasRepository.findById(id);

        if (personaOpt.isEmpty()) {
            log.error("Error: Persona con ID={} no existe ni en Redis ni en PostgreSQL", id);
            throw new RecursoNoEncontradoException("No se encontró ninguna persona registrada con el ID: " + id);
        }

        // 3. Mapear datos desde PostgreSQL y guardar en Redis para futuras consultas
        Personas persona = personaOpt.get();
        PersonaConsultaDTO dto = mapearEntidadADTO(persona, "POSTGRESQL");

        guardarEnCacheSilencioso(cacheKey, dto);
        if (persona.getUsername() != null && !persona.getUsername().isBlank()) {
            guardarEnCacheSilencioso(CACHE_KEY_USER + persona.getUsername(), dto);
        }

        return dto;
    }

    @Override
    public PersonaConsultaDTO obtenerPorUsername(String username) {
        String cacheKey = CACHE_KEY_USER + username.trim().toLowerCase();

        // 1. Intentar consultar en Redis
        try {
            Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
            if (cachedObj != null) {
                PersonaConsultaDTO dto = convertirADTO(cachedObj);
                if (dto != null) {
                    dto.setFuenteDatos("REDIS");
                    log.info("Éxito: Datos obtenidos desde la caché de Redis para username='{}'", username);
                    return dto;
                }
            }
        } catch (Exception e) {
            log.warn("Advertencia: No se pudo consultar en Redis ({}), se procede a consultar respaldo PostgreSQL", e.getMessage());
        }

        // 2. Fallback / Respaldo a PostgreSQL
        log.info("Cache Miss en Redis para username='{}'. Consultando en base de datos PostgreSQL...", username);
        Optional<Personas> personaOpt = personasRepository.findByUsernameOrNombre(username, username);

        if (personaOpt.isEmpty()) {
            log.error("Error: Persona con username='{}' no existe ni en Redis ni en PostgreSQL", username);
            throw new RecursoNoEncontradoException("No se encontró ninguna persona registrada con el username/nombre: " + username);
        }

        // 3. Mapear entidad PostgreSQL y guardar en Redis
        Personas persona = personaOpt.get();
        PersonaConsultaDTO dto = mapearEntidadADTO(persona, "POSTGRESQL");

        guardarEnCacheSilencioso(cacheKey, dto);
        if (persona.getId() != null) {
            guardarEnCacheSilencioso(CACHE_KEY_ID + persona.getId(), dto);
        }

        return dto;
    }

    @Override
    public boolean limpiarCache(Integer id) {
        try {
            String keyId = CACHE_KEY_ID + id;
            Boolean deleted = redisTemplate.delete(keyId);
            log.info("Caché invalidada para clave {}: {}", keyId, deleted);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.error("Error al limpiar la caché para ID={}: {}", id, e.getMessage());
            return false;
        }
    }

    private PersonaConsultaDTO mapearEntidadADTO(Personas persona, String fuente) {
        return PersonaConsultaDTO.builder()
                .id(persona.getId())
                .nombre(persona.getNombre())
                .apellidoP(persona.getApellidoP())
                .apellidoMaterno(persona.getApellidoMaterno())
                .username(persona.getUsername() != null ? persona.getUsername() : persona.getNombre())
                .rol(persona.getRol())
                .activo(persona.getActivo())
                .fuenteDatos(fuente)
                .build();
    }

    private PersonaConsultaDTO convertirADTO(Object obj) {
        if (obj instanceof PersonaConsultaDTO personaConsultaDTO) {
            return personaConsultaDTO;
        }
        try {
            return objectMapper.convertValue(obj, PersonaConsultaDTO.class);
        } catch (Exception e) {
            log.error("Error al convertir objeto de Redis a PersonaConsultaDTO: {}", e.getMessage());
            return null;
        }
    }

    private void guardarEnCacheSilencioso(String key, PersonaConsultaDTO dto) {
        try {
            redisTemplate.opsForValue().set(key, dto, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Datos guardados en Redis clave='{}' con TTL={} min", key, CACHE_TTL_MINUTES);
        } catch (Exception e) {
            log.warn("No se pudo guardar la clave '{}' en Redis: {}", key, e.getMessage());
        }
    }
}
