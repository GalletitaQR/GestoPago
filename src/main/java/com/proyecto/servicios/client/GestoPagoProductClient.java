package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListXmlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProduct", url = "${gestopago.auth.url}")
public interface GestoPagoProductClient {

    @GetMapping(
            value = "/sistema/app/jwt-gp/getProductList/",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    GestoPagoProductListXmlResponse getProductList(
            @RequestHeader("Authorization") String bearerToken
    );
}
