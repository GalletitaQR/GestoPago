package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListXmlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "gestoPagoProduct", url = "${gestopago.auth.url}")
public interface GestoPagoProductClient {

    @GetMapping(
            value = "/sistema/service/getProductList.do",
            headers = {"Accept=text/xml, application/xml"}
    )
    GestoPagoProductListXmlResponse getProductList(
            @RequestHeader("Authorization") String bearerToken,
            @RequestParam("idDistribuidor") Integer idDistribuidor,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey
    );
}

