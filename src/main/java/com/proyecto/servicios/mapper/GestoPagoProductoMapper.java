package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface GestoPagoProductoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "codigoProducto", source = "codigoProducto")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "descripcion", source = "descripcion")
    @Mapping(target = "categoria", source = "categoria")
    @Mapping(target = "precio", source = "precio")
    @Mapping(target = "costo", ignore = true)
    @Mapping(target = "comision", ignore = true)
    @Mapping(target = "activo", constant = "true")
    GestoPagoProducto toEntity(GestoPagoProductDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "codigoProducto", source = "codigoProducto")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "descripcion", source = "descripcion")
    @Mapping(target = "categoria", source = "categoria")
    @Mapping(target = "precio", source = "precio")
    @Mapping(target = "costo", ignore = true)
    @Mapping(target = "comision", ignore = true)
    @Mapping(target = "activo", ignore = true)
    void updateEntity(GestoPagoProductDto dto, @MappingTarget GestoPagoProducto entity);
}

