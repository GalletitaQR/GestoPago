package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoXmlDeserializationTest {

    @Test
    void testXmlDeserialization() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>" +
                "<RESPONSE>" +
                " <MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE>" +
                " <PRODUCTOS>" +
                "  <producto servicio='ABIB' producto='ABIB 100' idServicio='2284' idProducto='14302' idCatTipoServicio='13' tipoFront='1' hasDigitoVerificador='false' precio='100.0' showAyuda='false' tipoReferencia='a'>" +
                "   <legend><![CDATA[Recibe soporte las 24h del dia]]></legend>" +
                "  </producto>" +
                " </PRODUCTOS>" +
                "</RESPONSE>";

        XmlMapper xmlMapper = new XmlMapper();
        GestoPagoProductListXmlResponse response = xmlMapper.readValue(xml, GestoPagoProductListXmlResponse.class);

        assertNotNull(response);
        assertNotNull(response.getMensaje());
        assertEquals("01", response.getMensaje().getCodigo());
        assertEquals("Operacion realizada con exito", response.getMensaje().getTexto());
        assertNotNull(response.getProductos());
        assertEquals(1, response.getProductos().size());

        GestoPagoProductDto dto = response.getProductos().get(0);
        assertEquals("14302", dto.getIdProducto());
        assertEquals("2284", dto.getIdServicio());
        assertEquals("ABIB 100", dto.getProducto());
        assertEquals("ABIB", dto.getServicio());
        assertEquals("14302", dto.getCodigoProducto());
        assertEquals("ABIB 100", dto.getNombre());
        assertEquals("ABIB", dto.getCategoria());
        assertEquals("Recibe soporte las 24h del dia", dto.getDescripcion());
    }
}
