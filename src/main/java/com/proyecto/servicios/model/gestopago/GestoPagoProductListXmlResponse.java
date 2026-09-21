package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESPONSE")
public class GestoPagoProductListXmlResponse {

    @JacksonXmlProperty(localName = "MENSAJE")
    private Mensaje mensaje;

    @JacksonXmlElementWrapper(localName = "PRODUCTOS")
    @JacksonXmlProperty(localName = "producto")
    @JsonProperty("productos")
    private List<GestoPagoProductDto> productos = new ArrayList<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Mensaje {
        @JacksonXmlProperty(localName = "CODIGO")
        private String codigo;

        @JacksonXmlProperty(localName = "TEXTO")
        private String texto;
    }
}

