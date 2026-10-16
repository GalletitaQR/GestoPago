package com.proyecto.servicios.service.banco;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.security.SecurityHelper;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.*;
import com.proyecto.servicios.model.banco.*;
import com.proyecto.servicios.repositorys.banco.ClienteRepository;
import com.proyecto.servicios.repositorys.banco.UsuarioRepository;
import com.proyecto.servicios.repositorys.banco.UsuarioSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UsuarioService {

    private static final String CACHE_KEY_USUARIO_ID = "usuario:id:";
    private static final long CACHE_TTL_MINUTOS = 10;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityHelper securityHelper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&!#%&'()*+,-./:;<=>?@\\[\\]^_`{|}~])[A-Za-z\\d@$!%*?&!#%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    /**
     * Registro / Creación de un nuevo usuario de acceso para un cliente existente.
     * Valida reglas de negocio: existencia del cliente, no duplicidad de usuario por cliente,
     * no duplicidad de correo y fortaleza de contraseña cifrada con BCrypt.
     */
    @Transactional
    public UsuarioResponseDto crearUsuario(UsuarioCrearRequest request) {
        log.info("Creando nuevo usuario para cliente ID: {} con correo: {}", request.getClienteId(), request.getCorreo());

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + request.getClienteId()));

        if (usuarioRepository.findByClienteId(cliente.getId()).isPresent()) {
            throw new ReglaNegocioException("El cliente con ID " + cliente.getId() + " ya cuenta con un usuario de acceso registrado.");
        }

        String correoNormalizado = request.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correoNormalizado)) {
            throw new CorreoDuplicadoException("El correo " + correoNormalizado + " ya se encuentra registrado por otro usuario.");
        }

        validarFortalezaContrasena(request.getPassword());

        String passwordEncoded = passwordEncoder.encode(request.getPassword());

        Usuario usuario = Usuario.builder()
                .cliente(cliente)
                .correo(correoNormalizado)
                .password(passwordEncoded)
                .activo(true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario creado exitosamente con ID: {}", guardado.getId());
        return mapToResponseDto(guardado);
    }

    /**
     * Consulta de usuarios unificada y dinámica sin ID en ruta.
     * Permite:
     * 1) Consultas controladas por banderas (1=ID, 2=Correo, 3=Todos).
     * 2) Consultas dinámicas combinando múltiples filtros opcionales (ID, ClienteId, Correo, Activo, Fechas)
     *    mediante Spring Data JPA Specification.
     */
    @Transactional(readOnly = true)
    public Object consultarPorBandera(UsuarioConsultaRequest request) {
        if (request.getBandera() != null) {
            log.info("Consultando usuario por bandera: {}", request.getBandera());

            switch (request.getBandera()) {
                case 1: // Consulta por ID
                    if (request.getId() == null) {
                        throw new ReglaNegocioException("El ID es obligatorio para la bandera 1 (Consulta por ID).");
                    }
                    securityHelper.validarAccesoAUsuario(request.getId());
                    return obtenerUsuarioPorIdConCache(request.getId(), Boolean.TRUE.equals(request.getUsarCache()));

                case 2: // Consulta por Correo
                    if (request.getCorreo() == null || request.getCorreo().isBlank()) {
                        throw new ReglaNegocioException("El correo es obligatorio para la bandera 2 (Consulta por Correo).");
                    }
                    Usuario usuarioPorCorreo = obtenerUsuarioPorCorreo(request.getCorreo().trim());
                    securityHelper.validarAccesoAUsuario(usuarioPorCorreo.getId());
                    return mapToResponseDto(usuarioPorCorreo);

                case 3: // Listar todos los usuarios
                    return usuarioRepository.findAll().stream()
                            .map(this::mapToResponseDto)
                            .collect(Collectors.toList());

                default:
                    throw new ReglaNegocioException("Bandera de consulta inválida: " + request.getBandera());
            }
        }

        // Búsqueda dinámica con combinación de filtros multicriterio (bandera no especificada)
        log.info("Ejecutando consulta dinámica multicriterio de usuarios con filtros combinados.");
        if (request.getId() != null) {
            securityHelper.validarAccesoAUsuario(request.getId());
        }

        List<Usuario> usuarios = usuarioRepository.findAll(UsuarioSpecification.conFiltros(request));
        return usuarios.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Actualización de usuarios unificada sin ID en ruta.
     * Admite actualización por banderas (1=Contraseña, 2=Estado) o actualización general de datos.
     */
    @Transactional
    public UsuarioResponseDto actualizarPorBandera(UsuarioActualizaRequest request) {
        log.info("Iniciando actualización de usuario ID: {} con bandera: {}", request.getId(), request.getBandera());

        securityHelper.validarAccesoAUsuario(request.getId());

        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));

        if (request.getBandera() != null) {
            switch (request.getBandera()) {
                case 1: // Actualización de contraseña
                    if (!Boolean.TRUE.equals(usuario.getActivo())) {
                        throw new UsuarioInactivoException("El usuario se encuentra inactivo.");
                    }
                    if (request.getPasswordActual() == null || request.getPasswordActual().isBlank()) {
                        throw new ReglaNegocioException("La contraseña actual es obligatoria para la bandera 1.");
                    }
                    if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
                        throw new CredencialesInvalidasException("La contraseña actual proporcionada es incorrecta.");
                    }
                    if (request.getNuevaPassword() == null || request.getNuevaPassword().isBlank()) {
                        throw new ReglaNegocioException("La nueva contraseña es obligatoria.");
                    }

                    validarFortalezaContrasena(request.getNuevaPassword());
                    usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
                    break;

                case 2: // Actualización de estado activo / inactivo
                    if (request.getActivo() == null) {
                        throw new ReglaNegocioException("El estado 'activo' es obligatorio para la bandera 2.");
                    }
                    usuario.setActivo(request.getActivo());
                    break;

                default:
                    throw new ReglaNegocioException("Bandera de actualización inválida: " + request.getBandera());
            }
        } else {
            // Actualización general de campos modificables (correo, estado, contraseña)
            if (request.getCorreo() != null && !request.getCorreo().isBlank()) {
                String nuevoCorreo = request.getCorreo().trim().toLowerCase();
                if (usuarioRepository.existsByCorreoAndIdNot(nuevoCorreo, usuario.getId())) {
                    throw new CorreoDuplicadoException("El correo " + nuevoCorreo + " ya se encuentra registrado por otro usuario.");
                }
                usuario.setCorreo(nuevoCorreo);
            }

            if (request.getActivo() != null) {
                usuario.setActivo(request.getActivo());
            }

            if (request.getNuevaPassword() != null && !request.getNuevaPassword().isBlank()) {
                if (request.getPasswordActual() == null || request.getPasswordActual().isBlank()) {
                    throw new ReglaNegocioException("Debe ingresar la contraseña actual para establecer una nueva.");
                }
                if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
                    throw new CredencialesInvalidasException("La contraseña actual proporcionada es incorrecta.");
                }
                validarFortalezaContrasena(request.getNuevaPassword());
                usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
            }
        }

        Usuario guardado = usuarioRepository.save(usuario);
        invalidarCache(usuario.getId());
        log.info("Usuario ID: {} actualizado exitosamente.", usuario.getId());
        return mapToResponseDto(guardado);
    }

    /**
     * Baja lógica de usuario sin ID en ruta.
     */
    @Transactional
    public void eliminarUsuario(UsuarioEliminaRequest request) {
        log.info("Iniciando baja lógica de usuario ID: {}", request.getId());
        securityHelper.validarAccesoAUsuario(request.getId());

        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        invalidarCache(usuario.getId());
        log.info("Usuario ID: {} desactivado exitosamente (baja lógica). Motivo: {}", request.getId(), request.getMotivo());
    }

    /**
     * Consulta con caché transparente integrada (Redis con fallback a BD).
     */
    public UsuarioResponseDto obtenerUsuarioPorIdConCache(Long id, boolean usarCache) {
        String cacheKey = CACHE_KEY_USUARIO_ID + id;

        if (usarCache && redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    UsuarioResponseDto dto = convertirADto(cached);
                    if (dto != null) {
                        log.info("Usuario ID {} obtenido desde caché Redis.", id);
                        return dto;
                    }
                }
            } catch (Exception e) {
                log.warn("Error al consultar caché en Redis ({}), consultando BD...", e.getMessage());
            }
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + id));

        UsuarioResponseDto responseDto = mapToResponseDto(usuario);

        if (usarCache && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, responseDto, CACHE_TTL_MINUTOS, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("No se pudo guardar usuario en caché Redis: {}", e.getMessage());
            }
        }

        return responseDto;
    }

    private void invalidarCache(Long id) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(CACHE_KEY_USUARIO_ID + id);
                log.info("Caché invalidada para usuario ID: {}", id);
            } catch (Exception e) {
                log.warn("No se pudo invalidar la caché de usuario: {}", e.getMessage());
            }
        }
    }

    private UsuarioResponseDto convertirADto(Object obj) {
        if (obj instanceof UsuarioResponseDto) {
            return (UsuarioResponseDto) obj;
        }
        if (objectMapper != null) {
            try {
                return objectMapper.convertValue(obj, UsuarioResponseDto.class);
            } catch (Exception e) {
                log.warn("Error deserializando usuario de Redis: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * Crea automáticamente un usuario de acceso activo con contraseña cifrada en BCrypt.
     */
    @Transactional
    public Usuario crearUsuarioParaCliente(Cliente cliente, String passwordPlano) {
        log.info("Creando usuario de acceso para cliente ID: {} con correo: {}", cliente.getId(), cliente.getCorreo());

        if (usuarioRepository.existsByCorreo(cliente.getCorreo())) {
            throw new CorreoDuplicadoException("El correo " + cliente.getCorreo() + " ya cuenta con un usuario de acceso registrado.");
        }

        validarFortalezaContrasena(passwordPlano);

        String passwordEncoded = passwordEncoder.encode(passwordPlano);

        Usuario usuario = Usuario.builder()
                .cliente(cliente)
                .correo(cliente.getCorreo())
                .password(passwordEncoded)
                .activo(true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario de acceso creado exitosamente con ID: {}", guardado.getId());
        return guardado;
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto obtenerUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + id));
        return mapToResponseDto(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con correo: " + correo));
    }

    @Transactional
    public void actualizarPassword(Long id, UpdatePasswordDto request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + id));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsuarioInactivoException("El usuario se encuentra inactivo.");
        }

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new CredencialesInvalidasException("La contraseña actual proporcionada es incorrecta.");
        }

        validarFortalezaContrasena(request.getNuevaPassword());

        usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(usuario);
        invalidarCache(id);
        log.info("Contraseña actualizada exitosamente para el usuario ID: {}", id);
    }

    @Transactional
    public void desactivarUsuarioPorClienteId(Long clienteId) {
        usuarioRepository.findByClienteId(clienteId).ifPresent(usuario -> {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
            invalidarCache(usuario.getId());
            log.info("Usuario ID {} desactivado automáticamente por baja lógica del cliente ID {}.", usuario.getId(), clienteId);
        });
    }

    public void validarFortalezaContrasena(String password) {
        if (password == null || !pattern.matcher(password).matches()) {
            throw new ContrasenaInvalidaException(
                    "La contraseña debe contener mínimo 8 caracteres, al menos una mayúscula, una minúscula, un número y un carácter especial."
            );
        }
    }

    public UsuarioResponseDto mapToResponseDto(Usuario usuario) {
        return UsuarioResponseDto.builder()
                .id(usuario.getId())
                .clienteId(usuario.getCliente() != null ? usuario.getCliente().getId() : null)
                .correo(usuario.getCorreo())
                .activo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }
}
