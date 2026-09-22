package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.PersonaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class PersonasServiceImpl implements PersonaService {

    @Autowired
    private PersonasRepository personasRepository;

    @Override
    public PersonaResponse creaPersona(PersonasRequest personasRequest) {
        PersonaResponse person = new PersonaResponse();
        Personas persona = new Personas();
        persona.setNombre(personasRequest.getNombre());
        persona.setApellidoMaterno(personasRequest.getApellidoMaterno());
        persona.setApellidoP(personasRequest.getApellidoP());
        personasRepository.save(persona);
        person.setCodigo(1);
        person.setMensaje("Exito");
        BeanUtils.copyProperties(persona, person);

        return person;
    }

    @Override
    public GenericResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest) {
        GenericResponse genericResponse = new GenericResponse();

        Optional<Personas> existePersona = personasRepository.findByNombre(eliminaPersonaRequest.getNombre());
        if (existePersona.isPresent()) {
            Personas personaElimina = existePersona.get();
            personasRepository.delete(personaElimina);
            genericResponse.setCodigo(0);
            genericResponse.setMensaje("La persona ha sido eliminada correctamente");
        } else {
            genericResponse.setCodigo(1);
            genericResponse.setMensaje("La persona no existe ");
        }
        return genericResponse;
    }

    @Override
    public GenericResponse actualizaPersona(PersonasRequest personasRequest) {
        GenericResponse genericResponse = new GenericResponse();
        Optional<Personas> existePersona = personasRepository.findByNombre(personasRequest.getNombre());
        if (existePersona.isPresent()) {
            Personas personaActualiza = existePersona.get();
            personaActualiza.setApellidoP(personasRequest.getApellidoP());
            personaActualiza.setApellidoMaterno(personasRequest.getApellidoMaterno());
            personasRepository.save(personaActualiza);
            genericResponse.setCodigo(0);
            genericResponse.setMensaje("la persona ha sido actualizada correctamente");
        } else {
            genericResponse.setCodigo(1);
            genericResponse.setMensaje("La persona no existe ");
        }
        return genericResponse;
    }

    /**
     * Iniciar sesión con un solo rol por ahora (ROLE_USER).
     * Petición y respuesta mediante Map<String, Object>.
     * Captura de todas las excepciones con try-catch ("no tiene que tronar").
     */
    @Override
    public Map<String, Object> login(Map<String, Object> loginRequest) {
        Map<String, Object> respuesta = new HashMap<>();
        try {
            if (loginRequest == null || loginRequest.isEmpty()) {
                respuesta.put("codigo", 400);
                respuesta.put("mensaje", "La petición de login no puede estar vacía.");
                return respuesta;
            }

            String username = (String) loginRequest.get("username");
            if (username == null || username.isBlank()) {
                username = (String) loginRequest.get("email");
            }
            if (username == null || username.isBlank()) {
                username = (String) loginRequest.get("nombre");
            }

            String password = (String) loginRequest.get("password");

            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                respuesta.put("codigo", 400);
                respuesta.put("mensaje", "El usuario/email y contraseña son obligatorios.");
                return respuesta;
            }

            Optional<Personas> personaOpt = personasRepository.findByUsernameOrNombre(username, username);

            if (personaOpt.isEmpty()) {
                log.warn("Intento de login fallido: usuario no encontrado ({})", username);
                respuesta.put("codigo", 401);
                respuesta.put("mensaje", "Credenciales inválidas.");
                return respuesta;
            }

            Personas persona = personaOpt.get();

            if (Boolean.FALSE.equals(persona.getActivo())) {
                log.warn("Intento de login fallido: usuario inactivo ({})", username);
                respuesta.put("codigo", 403);
                respuesta.put("mensaje", "El usuario se encuentra inactivo.");
                return respuesta;
            }

            // Validación de contraseña (plana o si se setea vacía)
            if (persona.getPassword() == null || !persona.getPassword().equals(password)) {
                log.warn("Intento de login fallido: contraseña incorrecta para usuario ({})", username);
                respuesta.put("codigo", 401);
                respuesta.put("mensaje", "Credenciales inválidas.");
                return respuesta;
            }

            // Manejo de rol único por ahora (ROLE_USER)
            String rol = (persona.getRol() != null && !persona.getRol().isBlank()) ? persona.getRol() : "ROLE_USER";

            Map<String, Object> datosUsuario = new HashMap<>();
            datosUsuario.put("id", persona.getId());
            datosUsuario.put("nombre", persona.getNombre());
            datosUsuario.put("username", persona.getUsername() != null ? persona.getUsername() : persona.getNombre());
            datosUsuario.put("rol", rol);

            respuesta.put("codigo", 200);
            respuesta.put("mensaje", "Login exitoso.");
            respuesta.put("usuario", datosUsuario);
            respuesta.put("token", UUID.randomUUID().toString());

        } catch (Throwable t) {
            log.error("Excepción en proceso de login (capturada con try-catch): {}", t.getMessage(), t);
            respuesta.put("codigo", 500);
            respuesta.put("mensaje", "Error interno al procesar el login: " + t.getMessage());
        }

        return respuesta;
    }
}
