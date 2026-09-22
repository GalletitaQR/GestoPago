package com.proyecto.servicios.controller;

import com.proyecto.servicios.service.GestoPagoProductoSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/gestopago")
public class GestoPagoProductoController {

    private final GestoPagoProductoSyncService productoSyncService;

    public GestoPagoProductoController(GestoPagoProductoSyncService productoSyncService) {
        this.productoSyncService = productoSyncService;
    }

    /**
     * Endpoint GET para consultar la lista de productos guardada en la BD local / Redis.
     * Retorna respuesta en Map<String, Object> con código, mensaje, origen y datos.
     */
    @GetMapping(value = "/getProductList", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getProductList() {
        try {
            Map<String, Object> respuesta = productoSyncService.obtenerProductosBdConRespuesta();
            Integer codigo = (Integer) respuesta.getOrDefault("codigo", 200);
            return new ResponseEntity<>(respuesta, HttpStatus.valueOf(codigo));
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("codigo", 500);
            errorMap.put("mensaje", "Error inesperado al consultar la lista de productos: " + e.getMessage());
            return new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint POST para ejecutar una sincronización manual del catálogo.
     * Valida las 4 reglas del catálogo y responde con un Map<String, Object>.
     */
    @PostMapping(value = "/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> forceSync() {
        try {
            Map<String, Object> resultado = productoSyncService.sincronizarProductosConRespuesta();
            Integer codigo = (Integer) resultado.getOrDefault("codigo", 200);
            return new ResponseEntity<>(resultado, HttpStatus.valueOf(codigo));
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("codigo", 500);
            errorMap.put("mensaje", "Error inesperado al ejecutar sincronización: " + e.getMessage());
            return new ResponseEntity<>(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
