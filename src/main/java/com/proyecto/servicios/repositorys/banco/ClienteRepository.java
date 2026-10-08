package com.proyecto.servicios.repositorys.banco;

import com.proyecto.servicios.entity.banco.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreo(String correo);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT c FROM Cliente c JOIN Cuenta cu ON c.id = cu.cliente.id WHERE cu.numeroCuenta = :numeroCuenta")
    Optional<Cliente> findByNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);
}
