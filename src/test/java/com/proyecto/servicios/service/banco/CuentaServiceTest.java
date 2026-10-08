package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.EstatusCuenta;
import com.proyecto.servicios.exception.banco.CuentaNoEncontradaException;
import com.proyecto.servicios.model.banco.CuentaResponseDto;
import com.proyecto.servicios.repositorys.banco.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @InjectMocks
    private CuentaService cuentaService;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .id(1L)
                .nombre("Maria")
                .apellidoPaterno("Gómez")
                .correo("maria@example.com")
                .build();
    }

    @Test
    @DisplayName("Debe crear una cuenta automáticamente para el cliente")
    void crearCuentaParaCliente_Exitoso() {
        when(cuentaRepository.existsByNumeroCuenta(any())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> {
            Cuenta c = invocation.getArgument(0);
            c.setId(100L);
            return c;
        });

        Cuenta result = cuentaService.crearCuentaParaCliente(cliente);

        assertNotNull(result);
        assertEquals(cliente, result.getCliente());
        assertEquals(EstatusCuenta.ACTIVA, result.getEstatus());
        assertEquals(BigDecimal.ZERO, result.getSaldo());
        assertTrue(result.getNumeroCuenta().startsWith("7420"));
        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
    }

    @Test
    @DisplayName("Debe consultar cuenta por número de cuenta exitosamente")
    void obtenerCuentaPorNumero_Exitoso() {
        Cuenta cuenta = Cuenta.builder()
                .id(100L)
                .cliente(cliente)
                .numeroCuenta("7420999999999")
                .saldo(new BigDecimal("1500.00"))
                .estatus(EstatusCuenta.ACTIVA)
                .build();

        when(cuentaRepository.findByNumeroCuenta("7420999999999")).thenReturn(Optional.of(cuenta));

        CuentaResponseDto dto = cuentaService.obtenerCuentaPorNumero("7420999999999");

        assertNotNull(dto);
        assertEquals("7420999999999", dto.getNumeroCuenta());
        assertEquals(new BigDecimal("1500.00"), dto.getSaldo());
        assertEquals(EstatusCuenta.ACTIVA, dto.getEstatus());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la cuenta no existe")
    void obtenerCuentaPorNumero_NoEncontrada_LanzaExcepcion() {
        when(cuentaRepository.findByNumeroCuenta("0000000000")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.obtenerCuentaPorNumero("0000000000"));
    }
}
