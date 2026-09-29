package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.banco.CredencialesInvalidasException;
import com.proyecto.servicios.model.banco.UpdatePasswordDto;
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
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .correo("juan@example.com")
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
    @DisplayName("Debe lanzar excepción si la contraseña no cumple la complejidad requerida")
    void crearUsuarioParaCliente_ContrasenaInvalida_LanzaExcepcion() {
        when(usuarioRepository.existsByCorreo("juan@example.com")).thenReturn(false);

        // Sin número ni carácter especial
        assertThrows(ContrasenaInvalidaException.class,
                () -> usuarioService.crearUsuarioParaCliente(cliente, "simplepass"));
    }

    @Test
    @DisplayName("Debe actualizar la contraseña cuando la contraseña actual coincide")
    void actualizarPassword_Exitoso() {
        Usuario usuario = Usuario.builder()
                .id(10L)
                .correo("juan@example.com")
                .password("$2a$10$oldHash")
                .activo(true)
                .build();

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
        Usuario usuario = Usuario.builder()
                .id(10L)
                .correo("juan@example.com")
                .password("$2a$10$oldHash")
                .activo(true)
                .build();

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("WrongPass", "$2a$10$oldHash")).thenReturn(false);

        UpdatePasswordDto dto = UpdatePasswordDto.builder()
                .passwordActual("WrongPass")
                .nuevaPassword("NewPass123!")
                .build();

        assertThrows(CredencialesInvalidasException.class, () -> usuarioService.actualizarPassword(10L, dto));
    }
}
