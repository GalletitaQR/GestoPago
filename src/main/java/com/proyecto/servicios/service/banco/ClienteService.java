package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Domicilio;
import com.proyecto.servicios.exception.banco.*;
import com.proyecto.servicios.model.banco.ClienteRequestDto;
import com.proyecto.servicios.model.banco.ClienteResponseDto;
import com.proyecto.servicios.model.banco.DomicilioDto;
import com.proyecto.servicios.repositorys.banco.ClienteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@Slf4j
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaService cuentaService;

    /**
     * Registra un nuevo cliente validando las reglas de negocio y creando su cuenta bancaria.
     */
    @Transactional
    public ClienteResponseDto registrarCliente(ClienteRequestDto request) {
        log.info("Iniciando registro de cliente con CURP: {} y RFC: {}", request.getCurp(), request.getRfc());

        // 1. Validar Mayoría de Edad (18+ años)
        validarMayoriaDeEdad(request.getFechaNacimiento());

        // 2. Validar Unicidad de CURP
        if (clienteRepository.existsByCurp(request.getCurp())) {
            log.error("Ya existe un cliente registrado con la CURP: {}", request.getCurp());
            throw new CurpDuplicadaException("Ya existe un cliente registrado con la CURP: " + request.getCurp());
        }

        // 3. Validar Unicidad de RFC
        if (clienteRepository.existsByRfc(request.getRfc())) {
            log.error("Ya existe un cliente registrado con el RFC: {}", request.getRfc());
            throw new RfcDuplicadoException("Ya existe un cliente registrado con el RFC: " + request.getRfc());
        }

        // 4. Validar Unicidad de Correo Electrónico
        if (clienteRepository.existsByCorreo(request.getCorreo())) {
            log.error("Ya existe un cliente registrado con el correo: {}", request.getCorreo());
            throw new CorreoDuplicadoException("Ya existe un cliente registrado con el correo: " + request.getCorreo());
        }

        // 5. Crear Entidad Cliente
        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre().trim())
                .segundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null)
                .apellidoPaterno(request.getApellidoPaterno().trim())
                .apellidoMaterno(request.getApellidoMaterno().trim())
                .fechaNacimiento(request.getFechaNacimiento())
                .curp(request.getCurp().trim().toUpperCase())
                .rfc(request.getRfc().trim().toUpperCase())
                .sexo(request.getSexo())
                .nacionalidad(request.getNacionalidad().trim())
                .estadoCivil(request.getEstadoCivil())
                .correo(request.getCorreo().trim().toLowerCase())
                .telefonoMovil(request.getTelefonoMovil().trim())
                .telefonoAlternativo(request.getTelefonoAlternativo() != null ? request.getTelefonoAlternativo().trim() : null)
                .ocupacion(request.getOcupacion().trim())
                .empresa(request.getEmpresa().trim())
                .ingresoMensual(request.getIngresoMensual())
                .activo(true)
                .build();

        // 6. Crear Entidad Domicilio
        DomicilioDto domDto = request.getDomicilio();
        Domicilio domicilio = Domicilio.builder()
                .calle(domDto.getCalle().trim())
                .numeroExterior(domDto.getNumeroExterior().trim())
                .numeroInterior(domDto.getNumeroInterior() != null ? domDto.getNumeroInterior().trim() : null)
                .colonia(domDto.getColonia().trim())
                .municipio(domDto.getMunicipio().trim())
                .estado(domDto.getEstado().trim())
                .codigoPostal(domDto.getCodigoPostal().trim())
                .pais(domDto.getPais().trim())
                .cliente(cliente)
                .build();

        cliente.setDomicilio(domicilio);

        Cliente guardado = clienteRepository.save(cliente);
        log.info("Cliente registrado exitosamente con ID: {}", guardado.getId());

        // 7. Crear automáticamente la Cuenta Bancaria
        var cuenta = cuentaService.crearCuentaParaCliente(guardado);

        ClienteResponseDto response = mapToResponseDto(guardado);
        response.setNumeroCuenta(cuenta.getNumeroCuenta());
        return response;
    }

    public void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ReglaNegocioException("La fecha de nacimiento es obligatoria.");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            log.warn("Intento de registro de menor de edad. Edad calculada: {}", edad);
            throw new ReglaNegocioException("El cliente debe ser mayor de edad (18 años o más). Edad actual: " + edad);
        }
    }

    public ClienteResponseDto mapToResponseDto(Cliente cliente) {
        Domicilio dom = cliente.getDomicilio();
        DomicilioDto domDto = null;
        if (dom != null) {
            domDto = DomicilioDto.builder()
                    .calle(dom.getCalle())
                    .numeroExterior(dom.getNumeroExterior())
                    .numeroInterior(dom.getNumeroInterior())
                    .colonia(dom.getColonia())
                    .municipio(dom.getMunicipio())
                    .estado(dom.getEstado())
                    .codigoPostal(dom.getCodigoPostal())
                    .pais(dom.getPais())
                    .build();
        }

        return ClienteResponseDto.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .segundoNombre(cliente.getSegundoNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .sexo(cliente.getSexo())
                .nacionalidad(cliente.getNacionalidad())
                .estadoCivil(cliente.getEstadoCivil())
                .correo(cliente.getCorreo())
                .telefonoMovil(cliente.getTelefonoMovil())
                .telefonoAlternativo(cliente.getTelefonoAlternativo())
                .domicilio(domDto)
                .ocupacion(cliente.getOcupacion())
                .empresa(cliente.getEmpresa())
                .ingresoMensual(cliente.getIngresoMensual())
                .activo(cliente.getActivo())
                .fechaCreacion(cliente.getFechaCreacion())
                .fechaActualizacion(cliente.getFechaActualizacion())
                .build();
    }
}
