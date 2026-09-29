package com.proyecto.servicios.repositorys.banco;

import com.proyecto.servicios.entity.banco.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByClienteId(Long clienteId);

    boolean existsByCorreo(String correo);
}
