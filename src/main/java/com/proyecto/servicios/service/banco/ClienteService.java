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

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Registra un nuevo cliente validando las reglas de negocio, creando su cuenta bancaria y usuario de acceso.
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

        // 8. Crear automáticamente el Usuario de Acceso
        usuarioService.crearUsuarioParaCliente(guardado, request.getPassword());

        ClienteResponseDto response = mapToResponseDto(guardado);
        response.setNumeroCuenta(cuenta.getNumeroCuenta());
        return response;
    }

    @Transactional(readOnly = true)
    public java.util.List<ClienteResponseDto> obtenerTodosLosClientes() {
        return clienteRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteResponseDto obtenerClientePorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + id));
        return mapToResponseDto(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDto obtenerClientePorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp.trim().toUpperCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con CURP: " + curp));
        return mapToResponseDto(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDto obtenerClientePorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc.trim().toUpperCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con RFC: " + rfc));
        return mapToResponseDto(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDto obtenerClientePorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con correo: " + correo));
        return mapToResponseDto(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDto obtenerClientePorNumeroCuenta(String numeroCuenta) {
        Cliente cliente = clienteRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con número de cuenta: " + numeroCuenta));
        return mapToResponseDto(cliente);
    }

    @Transactional(readOnly = true)
    public java.util.List<ClienteResponseDto> obtenerClientesActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(this::mapToResponseDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.util.List<ClienteResponseDto> obtenerClientesPorRangoFechas(LocalDate inicio, LocalDate fin) {
        var inicioDateTime = inicio.atStartOfDay();
        var finDateTime = fin.atTime(23, 59, 59);
        return clienteRepository.findByFechaCreacionBetween(inicioDateTime, finDateTime).stream()
                .map(this::mapToResponseDto)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Actualiza la información de un cliente existente.
     * Bloquea explícitamente la modificación de CURP, RFC y Número de Cuenta.
     */
    @Transactional
    public ClienteResponseDto actualizarCliente(Long id, com.proyecto.servicios.model.banco.ClienteUpdateDto request) {
        log.info("Iniciando actualización de cliente con ID: {}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + id));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException("No se puede modificar la información de un cliente inactivo.");
        }

        // Bloqueo de modificación de CURP, RFC y Número de Cuenta
        if (request.getCurp() != null && !request.getCurp().trim().equalsIgnoreCase(cliente.getCurp())) {
            throw new ReglaNegocioException("No está permitido modificar la CURP del cliente.");
        }
        if (request.getRfc() != null && !request.getRfc().trim().equalsIgnoreCase(cliente.getRfc())) {
            throw new ReglaNegocioException("No está permitido modificar el RFC del cliente.");
        }
        if (request.getNumeroCuenta() != null) {
            throw new ReglaNegocioException("No está permitido modificar el número de cuenta del cliente.");
        }

        // Validar correo si cambió
        String nuevoCorreo = request.getCorreo().trim().toLowerCase();
        if (!cliente.getCorreo().equalsIgnoreCase(nuevoCorreo) && clienteRepository.existsByCorreo(nuevoCorreo)) {
            throw new CorreoDuplicadoException("El correo " + nuevoCorreo + " ya está registrado por otro cliente.");
        }

        // Validar mayoría de edad si cambió la fecha
        validarMayoriaDeEdad(request.getFechaNacimiento());

        // Actualizar datos personales
        cliente.setNombre(request.getNombre().trim());
        cliente.setSegundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null);
        cliente.setApellidoPaterno(request.getApellidoPaterno().trim());
        cliente.setApellidoMaterno(request.getApellidoMaterno().trim());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad().trim());
        cliente.setEstadoCivil(request.getEstadoCivil());

        // Actualizar datos de contacto
        cliente.setCorreo(nuevoCorreo);
        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo() != null ? request.getTelefonoAlternativo().trim() : null);

        // Actualizar información laboral
        cliente.setOcupacion(request.getOcupacion().trim());
        cliente.setEmpresa(request.getEmpresa().trim());
        cliente.setIngresoMensual(request.getIngresoMensual());

        // Actualizar Domicilio
        DomicilioDto domDto = request.getDomicilio();
        Domicilio dom = cliente.getDomicilio();
        if (dom == null) {
            dom = new Domicilio();
            dom.setCliente(cliente);
        }
        dom.setCalle(domDto.getCalle().trim());
        dom.setNumeroExterior(domDto.getNumeroExterior().trim());
        dom.setNumeroInterior(domDto.getNumeroInterior() != null ? domDto.getNumeroInterior().trim() : null);
        dom.setColonia(domDto.getColonia().trim());
        dom.setMunicipio(domDto.getMunicipio().trim());
        dom.setEstado(domDto.getEstado().trim());
        dom.setCodigoPostal(domDto.getCodigoPostal().trim());
        dom.setPais(domDto.getPais().trim());

        cliente.setDomicilio(dom);

        Cliente actualizado = clienteRepository.save(cliente);
        log.info("Cliente ID: {} actualizado correctamente.", actualizado.getId());
        return mapToResponseDto(actualizado);
    }

    /**
     * Realiza la baja lógica del cliente.
     * Desactiva al cliente y sus cuentas asociadas.
     */
    @Transactional
    public void desactivarCliente(Long id) {
        log.info("Iniciando baja lógica del cliente ID: {}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + id));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException("El cliente ya se encuentra inactivo.");
        }

        cliente.setActivo(false);
        clienteRepository.save(cliente);

        // Desactivar sus cuentas bancarias asociadas (Solo clientes activos pueden tener cuentas activas)
        cuentaService.desactivarCuentasDeCliente(id);

        // Desactivar usuario de acceso asociado
        usuarioService.desactivarUsuarioPorClienteId(id);

        log.info("Baja lógica completada para cliente ID: {}", id);
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
