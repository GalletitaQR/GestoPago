package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.Impl.PersonasServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonasServiceImplTest {

    @Mock
    private PersonasRepository personasRepository;

    @InjectMocks
    private PersonasServiceImpl personasService;

    @Test
    @DisplayName("Login exitoso con un solo rol por defecto (ROLE_USER)")
    void loginExitoso_rolUnico() {
        Personas persona = new Personas();
        persona.setId(1);
        persona.setNombre("Juan");
        persona.setUsername("juan123");
        persona.setPassword("pass123");
        persona.setRol("ROLE_USER");
        persona.setActivo(true);

        when(personasRepository.findByUsernameOrNombre("juan123", "juan123")).thenReturn(Optional.of(persona));

        Map<String, Object> request = new HashMap<>();
        request.put("username", "juan123");
        request.put("password", "pass123");

        Map<String, Object> response = personasService.login(request);

        assertEquals(200, response.get("codigo"));
        assertEquals("Login exitoso.", response.get("mensaje"));
        assertNotNull(response.get("usuario"));
        assertNotNull(response.get("token"));

        @SuppressWarnings("unchecked")
        Map<String, Object> usuario = (Map<String, Object>) response.get("usuario");
        assertEquals("ROLE_USER", usuario.get("rol"));
        assertEquals("juan123", usuario.get("username"));
    }

    @Test
    @DisplayName("Login fallido por contraseña incorrecta")
    void loginFallido_passwordIncorrecta() {
        Personas persona = new Personas();
        persona.setUsername("juan123");
        persona.setPassword("correct_pass");
        persona.setActivo(true);

        when(personasRepository.findByUsernameOrNombre("juan123", "juan123")).thenReturn(Optional.of(persona));

        Map<String, Object> request = Map.of("username", "juan123", "password", "wrong_pass");

        Map<String, Object> response = personasService.login(request);

        assertEquals(401, response.get("codigo"));
        assertEquals("Credenciales inválidas.", response.get("mensaje"));
        assertNull(response.get("token"));
    }

    @Test
    @DisplayName("Login fallido sin tronar ante petición nula o vacía")
    void loginPeticionVacia_noTruena() {
        Map<String, Object> response = personasService.login(null);

        assertEquals(400, response.get("codigo"));
        assertNotNull(response.get("mensaje"));
    }

    @Test
    @DisplayName("Login captura excepciones inesperadas sin tronar")
    void loginExcepcionBd_noTruena() {
        when(personasRepository.findByUsernameOrNombre(anyString(), anyString())).thenThrow(new RuntimeException("Error fatal de BD"));

        Map<String, Object> request = Map.of("username", "test", "password", "test");

        Map<String, Object> response = personasService.login(request);

        assertEquals(500, response.get("codigo"));
        assertTrue(((String) response.get("mensaje")).contains("Error interno"));
    }
}
