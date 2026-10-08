package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.CredencialesInvalidasException;
import com.proyecto.servicios.exception.banco.UsuarioInactivoException;
import com.proyecto.servicios.model.banco.LoginRequestDto;
import com.proyecto.servicios.model.banco.LoginResponseDto;
import com.proyecto.servicios.repositorys.banco.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioActivo;
    private Cliente clienteActivo;
    private LoginRequestDto loginDto;

    @BeforeEach
    void setUp() {
        clienteActivo = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .activo(true)
                .build();

        usuarioActivo = Usuario.builder()
                .id(10L)
                .correo("juan@example.com")
                .password("$2a$10$encodedPassword")
                .activo(true)
                .cliente(clienteActivo)
                .build();

        loginDto = LoginRequestDto.builder()
                .correo("juan@example.com")
                .password("Password123!")
                .build();
    }

    @Test
    @DisplayName("Debe autenticar correctamente y retornar token JWT")
    void login_Exitoso() {
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Password123!", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtService.generarToken(usuarioActivo)).thenReturn("mocked-jwt-token-12345");
        when(jwtService.getJwtExpirationMs()).thenReturn(86400000L);

        LoginResponseDto response = authService.login(loginDto);

        assertNotNull(response);
        assertEquals("mocked-jwt-token-12345", response.getToken());
        assertEquals("Bearer", response.getTipoToken());
        assertEquals("juan@example.com", response.getCorreo());
        assertEquals(10L, response.getUsuarioId());
        assertEquals(1L, response.getClienteId());
    }

    @Test
    @DisplayName("Debe lanzar CredencialesInvalidasException cuando el correo no existe")
    void login_CorreoNoExiste_LanzaExcepcion() {
        when(usuarioRepository.findByCorreo("inexistente@example.com")).thenReturn(Optional.empty());

        loginDto.setCorreo("inexistente@example.com");

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(loginDto));
    }

    @Test
    @DisplayName("Debe lanzar CredencialesInvalidasException cuando la contraseña es incorrecta")
    void login_PasswordIncorrecto_LanzaExcepcion() {
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("WrongPass123!", "$2a$10$encodedPassword")).thenReturn(false);

        loginDto.setPassword("WrongPass123!");

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(loginDto));
    }

    @Test
    @DisplayName("Debe lanzar UsuarioInactivoException cuando el usuario está inactivo")
    void login_UsuarioInactivo_LanzaExcepcion() {
        usuarioActivo.setActivo(false);
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuarioActivo));

        assertThrows(UsuarioInactivoException.class, () -> authService.login(loginDto));
    }

    @Test
    @DisplayName("Debe lanzar UsuarioInactivoException cuando el cliente asociado está inactivo")
    void login_ClienteInactivo_LanzaExcepcion() {
        clienteActivo.setActivo(false);
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuarioActivo));

        assertThrows(UsuarioInactivoException.class, () -> authService.login(loginDto));
    }
}
