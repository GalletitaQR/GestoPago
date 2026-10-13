package com.proyecto.servicios.service.banco;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.security.SecurityHelper;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.*;
import com.proyecto.servicios.model.banco.UpdatePasswordDto;
import com.proyecto.servicios.model.banco.UsuarioActualizaRequest;
import com.proyecto.servicios.model.banco.UsuarioConsultaRequest;
import com.proyecto.servicios.model.banco.UsuarioResponseDto;
import com.proyecto.servicios.repositorys.banco.UsuarioRepository;
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
     * Consulta de usuarios unificada y controlada por banderas sin ID en ruta.
     * Soporta caché transparente (Cache-Aside) y control de permisos contra IDOR.
     */
    @Transactional(readOnly = true)
    public Object consultarPorBandera(UsuarioConsultaRequest request) {
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

    /**
     * Actualización de usuarios unificada y controlada por banderas sin ID en ruta.
     */
    @Transactional
    public void actualizarPorBandera(UsuarioActualizaRequest request) {
        log.info("Iniciando actualización de usuario ID: {} con bandera: {}", request.getId(), request.getBandera());

        securityHelper.validarAccesoAUsuario(request.getId());

        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));

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
                usuarioRepository.save(usuario);
                invalidarCache(usuario.getId());
                log.info("Contraseña actualizada exitosamente para usuario ID: {}", usuario.getId());
                break;

            case 2: // Actualización de estado activo / inactivo
                if (request.getActivo() == null) {
                    throw new ReglaNegocioException("El estado 'activo' es obligatorio para la bandera 2.");
                }
                usuario.setActivo(request.getActivo());
                usuarioRepository.save(usuario);
                invalidarCache(usuario.getId());
                log.info("Estado activo actualizado a {} para usuario ID: {}", request.getActivo(), usuario.getId());
                break;

            default:
                throw new ReglaNegocioException("Bandera de actualización inválida: " + request.getBandera());
        }
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
