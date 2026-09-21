package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoProductDto {

    @JacksonXmlProperty(isAttribute = true, localName = "idProducto")
    @JsonProperty("idProducto")
    private String idProducto;

    @JacksonXmlProperty(isAttribute = true, localName = "idServicio")
    @JsonProperty("idServicio")
    private String idServicio;

    @JacksonXmlProperty(isAttribute = true, localName = "producto")
    @JsonProperty("producto")
    private String producto;

    @JacksonXmlProperty(isAttribute = true, localName = "servicio")
    @JsonProperty("servicio")
    private String servicio;

    @JacksonXmlProperty(isAttribute = true, localName = "precio")
    @JsonProperty("precio")
    private BigDecimal precio;

    @JacksonXmlProperty(isAttribute = true, localName = "idCatTipoServicio")
    @JsonProperty("idCatTipoServicio")
    private String idCatTipoServicio;

    @JacksonXmlProperty(isAttribute = true, localName = "tipoFront")
    @JsonProperty("tipoFront")
    private String tipoFront;

    @JacksonXmlProperty(isAttribute = true, localName = "hasDigitoVerificador")
    @JsonProperty("hasDigitoVerificador")
    private Boolean hasDigitoVerificador;

    @JacksonXmlProperty(isAttribute = true, localName = "showAyuda")
    @JsonProperty("showAyuda")
    private Boolean showAyuda;

    @JacksonXmlProperty(isAttribute = true, localName = "tipoReferencia")
    @JsonProperty("tipoReferencia")
    private String tipoReferencia;

    @JacksonXmlProperty(localName = "legend")
    @JsonProperty("legend")
    private String legend;

    public String getCodigoProducto() {
        if (idProducto != null && !idProducto.isBlank()) {
            return idProducto;
        }
        if (idServicio != null && producto != null) {
            return idServicio + "-" + producto;
        }
        return null;
    }

    public String getNombre() {
        return producto;
    }

    public String getDescripcion() {
        return legend;
    }

    public String getCategoria() {
        return servicio;
    }
}

