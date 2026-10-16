package com.proyecto.servicios.repositorys.banco;

import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.model.banco.UsuarioConsultaRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Especificación de Spring Data JPA para construir consultas dinámicas
 * de usuarios combinando filtros opcionales en un único endpoint.
 */
public class UsuarioSpecification {

    private UsuarioSpecification() {
        // Constructor privado para clase utilitaria
    }

    public static Specification<Usuario> conFiltros(UsuarioConsultaRequest filtros) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filtros == null) {
                return cb.conjunction();
            }

            // Filtro por ID de usuario
            if (filtros.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filtros.getId()));
            }

            // Filtro por ID de cliente asociado
            if (filtros.getClienteId() != null) {
                predicates.add(cb.equal(root.get("cliente").get("id"), filtros.getClienteId()));
            }

            // Filtro por Correo (coincidencia parcial, insensible a mayúsculas/minúsculas)
            if (filtros.getCorreo() != null && !filtros.getCorreo().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("correo")),
                        "%" + filtros.getCorreo().trim().toLowerCase() + "%"
                ));
            }

            // Filtro por Estado Activo/Inactivo
            if (filtros.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), filtros.getActivo()));
            }

            // Filtro por Rango de Fecha de Creación (Desde)
            if (filtros.getFechaCreacionDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), filtros.getFechaCreacionDesde()));
            }

            // Filtro por Rango de Fecha de Creación (Hasta)
            if (filtros.getFechaCreacionHasta() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), filtros.getFechaCreacionHasta()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
