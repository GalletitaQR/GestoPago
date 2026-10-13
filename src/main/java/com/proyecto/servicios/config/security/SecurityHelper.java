package com.proyecto.servicios.config.security;

import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.exception.banco.AccesoDenegadoException;
import com.proyecto.servicios.repositorys.banco.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class SecurityHelper {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public String obtenerCorreoAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            String principal = auth.getName();
            if (!"anonymousUser".equalsIgnoreCase(principal)) {
                return principal;
            }
        }
        return null;
    }

    public void validarAccesoAUsuario(Long usuarioIdSolicitado) {
        String correoAutenticado = obtenerCorreoAutenticado();
        if (correoAutenticado == null) {
            return;
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correoAutenticado);
        if (usuarioOpt.isPresent()) {
            Usuario usuarioAutenticado = usuarioOpt.get();
            if (!usuarioAutenticado.getId().equals(usuarioIdSolicitado)) {
                log.warn("Intento de acceso no autorizado: Usuario {} intentó acceder a recursos del usuario ID {}",
                        correoAutenticado, usuarioIdSolicitado);
                throw new AccesoDenegadoException("No tiene permisos para consultar o modificar la información de este usuario.");
            }
        }
    }

    public void validarAccesoACliente(Long clienteIdSolicitado) {
        String correoAutenticado = obtenerCorreoAutenticado();
        if (correoAutenticado == null) {
            return;
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correoAutenticado);
        if (usuarioOpt.isPresent()) {
            Usuario usuarioAutenticado = usuarioOpt.get();
            if (usuarioAutenticado.getCliente() != null &&
                    !usuarioAutenticado.getCliente().getId().equals(clienteIdSolicitado)) {
                log.warn("Intento de acceso no autorizado: Usuario {} intentó acceder a recursos del cliente ID {}",
                        correoAutenticado, clienteIdSolicitado);
                throw new AccesoDenegadoException("No tiene permisos para consultar o modificar la información de este cliente.");
            }
        }
    }
}
