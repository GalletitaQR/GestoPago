package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.config.security.SecurityHelper;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.*;
import com.proyecto.servicios.model.banco.*;
import com.proyecto.servicios.repositorys.banco.ClienteRepository;
import com.proyecto.servicios.repositorys.banco.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private UsuarioService usuarioService;

    private Cliente cliente;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .correo("juan@example.com")
                .build();

        usuario = Usuario.builder()
                .id(10L)
                .cliente(cliente)
                .correo("juan@example.com")
                .password("$2a$10$oldHash")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Debe crear usuario con contraseña cifrada en BCrypt exitosamente")
    void crearUsuarioParaCliente_Exitoso() {
        when(usuarioRepository.existsByCorreo("juan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$encodedPasswordHash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(10L);
            return u;
        });

        Usuario result = usuarioService.crearUsuarioParaCliente(cliente, "Password123!");

        assertNotNull(result);
        assertEquals("juan@example.com", result.getCorreo());
        assertEquals("$2a$10$encodedPasswordHash", result.getPassword());
        assertTrue(result.getActivo());
        verify(passwordEncoder, times(1)).encode("Password123!");
    }

    @Test
    @DisplayName("Debe registrar un nuevo usuario mediante UsuarioCrearRequest exitosamente")
    void crearUsuario_Exitoso() {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .clienteId(1L)
                .correo("nuevo@banco.com")
                .password("Password123#")
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByClienteId(1L)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo("nuevo@banco.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(50L);
            return u;
        });

        UsuarioResponseDto response = usuarioService.crearUsuario(request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals(1L, response.getClienteId());
        assertEquals("nuevo@banco.com", response.getCorreo());
        assertTrue(response.getActivo());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe lanzar ClienteNoEncontradoException al crear usuario si el cliente no existe")
    void crearUsuario_ClienteNoExiste_LanzaExcepcion() {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .clienteId(99L)
                .correo("nuevo@banco.com")
                .password("Password123#")
                .build();

        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> usuarioService.crearUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar ReglaNegocioException si el cliente ya cuenta con un usuario")
    void crearUsuario_ClienteYaTieneUsuario_LanzaExcepcion() {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .clienteId(1L)
                .correo("nuevo@banco.com")
                .password("Password123#")
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByClienteId(1L)).thenReturn(Optional.of(usuario));

        assertThrows(ReglaNegocioException.class, () -> usuarioService.crearUsuario(request));
    }

    @Test
    @DisplayName("Debe lanzar CorreoDuplicadoException si el correo ya existe al crear usuario")
    void crearUsuario_CorreoDuplicado_LanzaExcepcion() {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .clienteId(1L)
                .correo("juan@example.com")
                .password("Password123#")
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByClienteId(1L)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo("juan@example.com")).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> usuarioService.crearUsuario(request));
    }

    @Test
    @DisplayName("Debe consultar usuarios dinámicamente combinando filtros con Specification")
    void consultarPorBandera_Dinamico_Exitoso() {
        UsuarioConsultaRequest request = UsuarioConsultaRequest.builder()
                .correo("juan")
                .activo(true)
                .build();

        when(usuarioRepository.findAll(any(Specification.class))).thenReturn(List.of(usuario));

        Object resultado = usuarioService.consultarPorBandera(request);

        assertNotNull(resultado);
        assertTrue(resultado instanceof List);
        List<?> lista = (List<?>) resultado;
        assertEquals(1, lista.size());
        UsuarioResponseDto dto = (UsuarioResponseDto) lista.get(0);
        assertEquals("juan@example.com", dto.getCorreo());
    }

    @Test
    @DisplayName("Debe actualizar información general del usuario sin tocar campos del servidor")
    void actualizarPorBandera_ActualizacionGeneral_Exitoso() {
        UsuarioActualizaRequest request = UsuarioActualizaRequest.builder()
                .id(10L)
                .correo("nuevo.juan@example.com")
                .activo(false)
                .build();

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreoAndIdNot("nuevo.juan@example.com", 10L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponseDto response = usuarioService.actualizarPorBandera(request);

        assertNotNull(response);
        assertEquals("nuevo.juan@example.com", response.getCorreo());
        assertFalse(response.getActivo());
        verify(securityHelper).validarAccesoAUsuario(10L);
    }

    @Test
    @DisplayName("Debe realizar la baja lógica de un usuario exitosamente")
    void eliminarUsuario_Exitoso() {
        UsuarioEliminaRequest request = UsuarioEliminaRequest.builder()
                .id(10L)
                .motivo("Baja por solicitud del cliente")
                .build();

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        usuarioService.eliminarUsuario(request);

        assertFalse(usuario.getActivo());
        verify(usuarioRepository, times(1)).save(usuario);
        verify(securityHelper).validarAccesoAUsuario(10L);
    }

    @Test
    @DisplayName("Debe lanzar UsuarioNoEncontradoException al dar de baja si el usuario no existe")
    void eliminarUsuario_NoExiste_LanzaExcepcion() {
        UsuarioEliminaRequest request = UsuarioEliminaRequest.builder()
                .id(999L)
                .build();

        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> usuarioService.eliminarUsuario(request));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la contraseña no cumple la complejidad requerida")
    void crearUsuarioParaCliente_ContrasenaInvalida_LanzaExcepcion() {
        when(usuarioRepository.existsByCorreo("juan@example.com")).thenReturn(false);

        assertThrows(ContrasenaInvalidaException.class,
                () -> usuarioService.crearUsuarioParaCliente(cliente, "simplepass"));
    }

    @Test
    @DisplayName("Debe actualizar la contraseña cuando la contraseña actual coincide")
    void actualizarPassword_Exitoso() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("OldPass123!", "$2a$10$oldHash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("$2a$10$newHash");

        UpdatePasswordDto dto = UpdatePasswordDto.builder()
                .passwordActual("OldPass123!")
                .nuevaPassword("NewPass123!")
                .build();

        usuarioService.actualizarPassword(10L, dto);

        assertEquals("$2a$10$newHash", usuario.getPassword());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    @DisplayName("Debe lanzar excepción al cambiar contraseña si la actual es incorrecta")
    void actualizarPassword_PasswordActualErronea_LanzaExcepcion() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("WrongPass", "$2a$10$oldHash")).thenReturn(false);

        UpdatePasswordDto dto = UpdatePasswordDto.builder()
                .passwordActual("WrongPass")
                .nuevaPassword("NewPass123!")
                .build();

        assertThrows(CredencialesInvalidasException.class, () -> usuarioService.actualizarPassword(10L, dto));
    }
}
