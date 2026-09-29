package com.proyecto.servicios.service.banco;

import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.CredencialesInvalidasException;
import com.proyecto.servicios.exception.banco.UsuarioInactivoException;
import com.proyecto.servicios.model.banco.LoginRequestDto;
import com.proyecto.servicios.model.banco.LoginResponseDto;
import com.proyecto.servicios.repositorys.banco.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    /**
     * Autentica un usuario y genera un token JWT.
     */
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String correoLimpio = request.getCorreo().trim().toLowerCase();
        log.info("Intento de inicio de sesión para el correo: {}", correoLimpio);

        Usuario usuario = usuarioRepository.findByCorreo(correoLimpio)
                .orElseThrow(() -> {
                    log.warn("Inicio de sesión fallido: correo no encontrado ({})", correoLimpio);
                    return new CredencialesInvalidasException("Credenciales inválidas. Correo o contraseña incorrectos.");
                });

        // Validar si el usuario o su cliente están activos
        if (!Boolean.TRUE.equals(usuario.getActivo()) ||
                (usuario.getCliente() != null && !Boolean.TRUE.equals(usuario.getCliente().getActivo()))) {
            log.warn("Acceso denegado: el usuario {} se encuentra inactivo.", correoLimpio);
            throw new UsuarioInactivoException("El usuario o cliente se encuentra inactivo. Acceso denegado.");
        }

        // Validar contraseña con BCrypt
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("Inicio de sesión fallido: contraseña incorrecta para {}", correoLimpio);
            throw new CredencialesInvalidasException("Credenciales inválidas. Correo o contraseña incorrectos.");
        }

        String token = jwtService.generarToken(usuario);
        log.info("Inicio de sesión exitoso para el correo: {}", correoLimpio);

        return LoginResponseDto.builder()
                .token(token)
                .tipoToken("Bearer")
                .correo(usuario.getCorreo())
                .usuarioId(usuario.getId())
                .clienteId(usuario.getCliente() != null ? usuario.getCliente().getId() : null)
                .expiracionMs(jwtService.getJwtExpirationMs())
                .build();
    }
}
