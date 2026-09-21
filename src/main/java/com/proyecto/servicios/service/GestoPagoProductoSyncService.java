package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoProductDto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListXmlResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoProductoSyncService {

    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoProductoRepository productoRepository;
    private final GestoPagoTokenService tokenService;
    private final GestoPagoProductoMapper productoMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.api-key:YSX1HpAFum4TpCecyFBxs4eIjAlbhKqK6fpcSQp8}")
    private String apiKey;

    public GestoPagoProductoSyncService(GestoPagoProductClient gestoPagoProductClient,
                                       GestoPagoProductoRepository productoRepository,
                                       GestoPagoTokenService tokenService,
                                       GestoPagoProductoMapper productoMapper) {
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.productoRepository = productoRepository;
        this.tokenService = tokenService;
        this.productoMapper = productoMapper;
    }

    /**
     * Sincroniza la lista de productos de GestoPago cada 1 día (86400000 ms).
     */
    @Scheduled(fixedRateString = "${gestopago.productos.refresh-rate-ms:86400000}", initialDelay = 10000)
    @Transactional
    public void sincronizarProductos() {
        log.info("Iniciando sincronización programada (diaria) de productos GestoPago...");
        try {
            Optional<GestoPagoToken> tokenOpt = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);

            if (tokenOpt.isEmpty() || tokenOpt.get().getToken() == null) {
                log.warn("No existe token activo de GestoPago. Intentando renovar...");
                tokenService.renovarToken();
                tokenOpt = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
            }

            if (tokenOpt.isEmpty() || tokenOpt.get().getToken() == null) {
                log.error("No se pudo obtener token válido de GestoPago para sincronizar productos.");
                return;
            }

            String authHeader = "Bearer " + tokenOpt.get().getToken();
            GestoPagoProductListXmlResponse response = gestoPagoProductClient.getProductList(authHeader, idDistribuidor, apiKey);

            if (response == null || response.getProductos() == null || response.getProductos().isEmpty()) {
                log.warn("La respuesta del catálogo de GestoPago no contiene productos. Mensaje: {}",
                        response != null && response.getMensaje() != null ? response.getMensaje().getTexto() : "N/A");
                return;
            }

            List<GestoPagoProducto> productosAActualizar = new ArrayList<>();
            for (GestoPagoProductDto dto : response.getProductos()) {
                String codigo = dto.getCodigoProducto();
                if (codigo == null || codigo.isBlank()) {
                    continue;
                }
                GestoPagoProducto entity = productoRepository.findByCodigoProducto(codigo)
                        .map(existing -> {
                            productoMapper.updateEntity(dto, existing);
                            return existing;
                        })
                        .orElseGet(() -> productoMapper.toEntity(dto));

                productosAActualizar.add(entity);
            }

            productoRepository.saveAll(productosAActualizar);
            log.info("Sincronización diaria finalizada con éxito. Total productos en BD: {}", productosAActualizar.size());

        } catch (Exception e) {
            log.error("Error al sincronizar productos de GestoPago: {}", e.getMessage(), e);
        }
    }

    /**
     * Obtiene los productos almacenados en la base de datos local.
     * Si la BD está vacía, dispara la sincronización inicial.
     */
    @Transactional(readOnly = true)
    public List<GestoPagoProducto> obtenerProductosBd() {
        List<GestoPagoProducto> productos = productoRepository.findByActivoTrue();
        if (productos.isEmpty()) {
            log.info("BD sin productos registrados. Ejecutando sincronización inicial...");
            sincronizarProductos();
            productos = productoRepository.findByActivoTrue();
        }
        return productos;
    }
}