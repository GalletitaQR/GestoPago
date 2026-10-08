package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.EstadoCivil;
import com.proyecto.servicios.entity.banco.Sexo;
import com.proyecto.servicios.exception.banco.CorreoDuplicadoException;
import com.proyecto.servicios.exception.banco.CurpDuplicadaException;
import com.proyecto.servicios.exception.banco.ReglaNegocioException;
import com.proyecto.servicios.model.banco.ClienteRequestDto;
import com.proyecto.servicios.model.banco.ClienteResponseDto;
import com.proyecto.servicios.model.banco.DomicilioDto;
import com.proyecto.servicios.repositorys.banco.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private ClienteRequestDto requestDto;

    @BeforeEach
    void setUp() {
        DomicilioDto domicilio = DomicilioDto.builder()
                .calle("Av. Universidad")
                .numeroExterior("123")
                .colonia("Centro")
                .municipio("Guanajuato")
                .estado("Guanajuato")
                .codigoPostal("36000")
                .pais("México")
                .build();

        requestDto = ClienteRequestDto.builder()
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("López")
                .fechaNacimiento(LocalDate.of(1995, 5, 20))
                .curp("PELJ950520HGTXR09")
                .rfc("PELJ950520XXX")
                .sexo(Sexo.MASCULINO)
                .nacionalidad("Mexicana")
                .estadoCivil(EstadoCivil.SOLTERO)
                .correo("juan.perez@example.com")
                .telefonoMovil("4731234567")
                .domicilio(domicilio)
                .ocupacion("Ingeniero")
                .empresa("UTNG Tech")
                .ingresoMensual(new BigDecimal("25000.00"))
                .password("Password123!")
                .build();
    }

    @Test
    @DisplayName("Debe registrar cliente exitosamente")
    void registrarCliente_Exitoso() {
        when(clienteRepository.existsByCurp(any())).thenReturn(false);
        when(clienteRepository.existsByRfc(any())).thenReturn(false);
        when(clienteRepository.existsByCorreo(any())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        ClienteResponseDto result = clienteService.registrarCliente(requestDto);

        assertNotNull(result);
        assertEquals("Juan", result.getNombre());
        assertEquals("PELJ950520HGTXR09", result.getCurp());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el cliente es menor de edad")
    void registrarCliente_MenorDeEdad_LanzaExcepcion() {
        requestDto.setFechaNacimiento(LocalDate.now().minusYears(17));

        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(requestDto));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la CURP ya existe")
    void registrarCliente_CurpDuplicada_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(any())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> clienteService.registrarCliente(requestDto));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el Correo ya existe")
    void registrarCliente_CorreoDuplicado_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(any())).thenReturn(false);
        when(clienteRepository.existsByRfc(any())).thenReturn(false);
        when(clienteRepository.existsByCorreo(any())).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> clienteService.registrarCliente(requestDto));
        verify(clienteRepository, never()).save(any());
    }
}
