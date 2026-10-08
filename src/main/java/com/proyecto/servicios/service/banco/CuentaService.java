package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.EstatusCuenta;
import com.proyecto.servicios.exception.banco.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.banco.ReglaNegocioException;
import com.proyecto.servicios.model.banco.CuentaResponseDto;
import com.proyecto.servicios.repositorys.banco.CuentaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CuentaService {

    @Autowired
    private CuentaRepository cuentaRepository;

    private static final String PREFIX_CUENTA = "7420";
    private final Random random = new Random();

    /**
     * Crea automáticamente una cuenta bancaria única asociada al cliente.
     */
    @Transactional
    public Cuenta crearCuentaParaCliente(Cliente cliente) {
        log.info("Generando cuenta bancaria única para el cliente ID: {}", cliente.getId());

        String numeroCuenta = generarNumeroCuentaUnico();
        BigDecimal saldoInicial = BigDecimal.ZERO;

        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El saldo inicial no puede ser negativo.");
        }

        Cuenta cuenta = Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .saldo(saldoInicial)
                .estatus(EstatusCuenta.ACTIVA)
                .build();

        Cuenta guardada = cuentaRepository.save(cuenta);
        log.info("Cuenta bancaria creada exitosamente. Número de Cuenta: {}", guardada.getNumeroCuenta());
        return guardada;
    }

    @Transactional(readOnly = true)
    public CuentaResponseDto obtenerCuentaPorNumero(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada con número: " + numeroCuenta));
        return mapToResponseDto(cuenta);
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerSaldo(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada con número: " + numeroCuenta));
        return cuenta.getSaldo();
    }

    @Transactional(readOnly = true)
    public List<CuentaResponseDto> obtenerCuentasActivas() {
        return cuentaRepository.findByEstatus(EstatusCuenta.ACTIVA).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void desactivarCuentasDeCliente(Long clienteId) {
        List<Cuenta> cuentas = cuentaRepository.findByClienteId(clienteId);
        for (Cuenta cuenta : cuentas) {
            cuenta.setEstatus(EstatusCuenta.INACTIVA);
            cuentaRepository.save(cuenta);
        }
        log.info("Cuentas del cliente ID {} desactivadas.", clienteId);
    }

    private String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            long randomDigits = 1000000000L + (long) (random.nextDouble() * 9000000000L);
            numeroCuenta = PREFIX_CUENTA + randomDigits;
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));
        return numeroCuenta;
    }

    public CuentaResponseDto mapToResponseDto(Cuenta cuenta) {
        String nombreCliente = cuenta.getCliente() != null
                ? (cuenta.getCliente().getNombre() + " " + cuenta.getCliente().getApellidoPaterno())
                : "N/A";

        return CuentaResponseDto.builder()
                .id(cuenta.getId())
                .clienteId(cuenta.getCliente() != null ? cuenta.getCliente().getId() : null)
                .nombreCliente(nombreCliente)
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .estatus(cuenta.getEstatus())
                .fechaCreacion(cuenta.getFechaCreacion())
                .fechaActualizacion(cuenta.getFechaActualizacion())
                .build();
    }
}
