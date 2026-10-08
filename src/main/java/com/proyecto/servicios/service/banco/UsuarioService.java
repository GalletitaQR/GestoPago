package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.*;
import com.proyecto.servicios.model.banco.UpdatePasswordDto;
import com.proyecto.servicios.model.banco.UsuarioResponseDto;
import com.proyecto.servicios.repositorys.banco.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@Slf4j
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&!#%&'()*+,-./:;<=>?@\\[\\]^_`{|}~])[A-Za-z\\d@$!%*?&!#%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

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
        log.info("Contraseña actualizada exitosamente para el usuario ID: {}", id);
    }

    @Transactional
    public void desactivarUsuarioPorClienteId(Long clienteId) {
        usuarioRepository.findByClienteId(clienteId).ifPresent(usuario -> {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
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
