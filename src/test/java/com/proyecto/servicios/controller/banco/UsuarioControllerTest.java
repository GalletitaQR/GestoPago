package com.proyecto.servicios.controller.banco;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.exception.banco.CorreoDuplicadoException;
import com.proyecto.servicios.exception.banco.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.banco.*;
import com.proyecto.servicios.service.banco.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("POST /usuarios - Debe registrar un nuevo usuario y responder 201 Created")
    void registrarUsuario_Exitoso() throws Exception {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .clienteId(1L)
                .correo("nuevo.usuario@banco.com")
                .password("Password123#")
                .build();

        UsuarioResponseDto responseDto = UsuarioResponseDto.builder()
                .id(10L)
                .clienteId(1L)
                .correo("nuevo.usuario@banco.com")
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(usuarioService.crearUsuario(any(UsuarioCrearRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(201))
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado exitosamente."))
                .andExpect(jsonPath("$.datos.id").value(10))
                .andExpect(jsonPath("$.datos.correo").value("nuevo.usuario@banco.com"))
                .andExpect(jsonPath("$.datos.activo").value(true));

        verify(usuarioService, times(1)).crearUsuario(any(UsuarioCrearRequest.class));
    }

    @Test
    @DisplayName("POST /usuarios - Debe retornar 400 Bad Request si la validación Bean Validation falla (correo inválido)")
    void registrarUsuario_ValidacionFallida_Retorna400() throws Exception {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .clienteId(1L)
                .correo("correo-invalido") // Formato inválido
                .password("pass")          // Menor a 8 caracteres, sin mayúscula ni símbolo
                .build();

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        verify(usuarioService, never()).crearUsuario(any());
    }

    @Test
    @DisplayName("POST /usuarios - Debe retornar 409 Conflict si el correo ya existe")
    void registrarUsuario_CorreoDuplicado_Retorna409() throws Exception {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .clienteId(1L)
                .correo("duplicado@banco.com")
                .password("Password123#")
                .build();

        when(usuarioService.crearUsuario(any(UsuarioCrearRequest.class)))
                .thenThrow(new CorreoDuplicadoException("El correo ya se encuentra registrado por otro usuario."));

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje").value("El correo ya se encuentra registrado por otro usuario."));
    }

    @Test
    @DisplayName("POST /usuarios/consultar - Debe consultar usuarios combinando filtros en cuerpo y responder 200 OK")
    void consultarUsuario_FiltrosCombinados_Retorna200() throws Exception {
        UsuarioConsultaRequest request = UsuarioConsultaRequest.builder()
                .correo("usuario")
                .activo(true)
                .build();

        UsuarioResponseDto dto = UsuarioResponseDto.builder()
                .id(1L)
                .correo("usuario@banco.com")
                .activo(true)
                .build();

        when(usuarioService.consultarPorBandera(any(UsuarioConsultaRequest.class))).thenReturn(List.of(dto));

        mockMvc.perform(post("/usuarios/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(200))
                .andExpect(jsonPath("$.mensaje").value("Consulta de usuario(s) realizada exitosamente."))
                .andExpect(jsonPath("$.datos[0].id").value(1));

        verify(usuarioService, times(1)).consultarPorBandera(any(UsuarioConsultaRequest.class));
    }

    @Test
    @DisplayName("PUT /usuarios/actualizar - Debe actualizar usuario con DTO en cuerpo y responder 200 OK")
    void actualizarUsuario_Exitoso() throws Exception {
        UsuarioActualizaRequest request = UsuarioActualizaRequest.builder()
                .id(10L)
                .correo("actualizado@banco.com")
                .activo(true)
                .build();

        UsuarioResponseDto responseDto = UsuarioResponseDto.builder()
                .id(10L)
                .correo("actualizado@banco.com")
                .activo(true)
                .build();

        when(usuarioService.actualizarPorBandera(any(UsuarioActualizaRequest.class))).thenReturn(responseDto);

        mockMvc.perform(put("/usuarios/actualizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(200))
                .andExpect(jsonPath("$.datos.correo").value("actualizado@banco.com"));

        verify(usuarioService, times(1)).actualizarPorBandera(any(UsuarioActualizaRequest.class));
    }

    @Test
    @DisplayName("PUT /usuarios/actualizar - Debe retornar 404 Not Found si el usuario no existe")
    void actualizarUsuario_NoEncontrado_Retorna404() throws Exception {
        UsuarioActualizaRequest request = UsuarioActualizaRequest.builder()
                .id(999L)
                .correo("actualizado@banco.com")
                .build();

        when(usuarioService.actualizarPorBandera(any(UsuarioActualizaRequest.class)))
                .thenThrow(new UsuarioNoEncontradoException("Usuario no encontrado con ID: 999"));

        mockMvc.perform(put("/usuarios/actualizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404))
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado con ID: 999"));
    }

    @Test
    @DisplayName("PUT /usuarios/eliminar - Debe realizar baja lógica sin ID en ruta y responder 200 OK")
    void eliminarUsuario_Exitoso() throws Exception {
        UsuarioEliminaRequest request = UsuarioEliminaRequest.builder()
                .id(10L)
                .motivo("Inactivación solicitada por el usuario")
                .build();

        doNothing().when(usuarioService).eliminarUsuario(any(UsuarioEliminaRequest.class));

        mockMvc.perform(put("/usuarios/eliminar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(200))
                .andExpect(jsonPath("$.mensaje").value("Usuario con ID 10 desactivado exitosamente (baja lógica)."));

        verify(usuarioService, times(1)).eliminarUsuario(any(UsuarioEliminaRequest.class));
    }
}
