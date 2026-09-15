package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.service.GestoPagoProductoSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gestopago")
public class GestoPagoProductoController {

    private final GestoPagoProductoSyncService productoSyncService;

    public GestoPagoProductoController(GestoPagoProductoSyncService productoSyncService) {
        this.productoSyncService = productoSyncService;
    }

    /**
     * Endpoint GET que consulta la lista de productos guardada en la BD local.
     * Evita consultar la API externa en cada petición. La BD se refresca automáticamente cada 24 horas.
     */
    @GetMapping(value = "/getProductList", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GestoPagoProducto>> getProductList() {
        List<GestoPagoProducto> productos = productoSyncService.obtenerProductosBd();
        return new ResponseEntity<>(productos, HttpStatus.OK);
    }

    /**
     * Endpoint POST para ejecutar una sincronización manual bajo demanda.
     */
    @PostMapping(value = "/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> forceSync() {
        productoSyncService.sincronizarProductos();
        return new ResponseEntity<>("Sincronización ejecutada correctamente", HttpStatus.OK);
    }
}
