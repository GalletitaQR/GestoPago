package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoProductDto {

    @JacksonXmlProperty(localName = "codigo")
    @JsonProperty("codigo")
    private String codigoProducto;

    @JacksonXmlProperty(localName = "nombre")
    @JsonProperty("nombre")
    private String nombre;

    @JacksonXmlProperty(localName = "descripcion")
    @JsonProperty("descripcion")
    private String descripcion;

    @JacksonXmlProperty(localName = "categoria")
    @JsonProperty("categoria")
    private String categoria;

    @JacksonXmlProperty(localName = "precio")
    @JsonProperty("precio")
    private BigDecimal precio;

    @JacksonXmlProperty(localName = "costo")
    @JsonProperty("costo")
    private BigDecimal costo;

    @JacksonXmlProperty(localName = "comision")
    @JsonProperty("comision")
    private BigDecimal comision;

    @JacksonXmlProperty(localName = "activo")
    @JsonProperty("activo")
    private Boolean activo;
}
