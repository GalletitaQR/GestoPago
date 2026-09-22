package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoProductDto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListXmlResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class GestoPagoProductoSyncService {

    public static final String REDIS_KEY_PRODUCTOS = "gestopago:productos";

    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoProductoRepository productoRepository;
    private final GestoPagoProductoMapper productoMapper;
    private final GestoPagoTokenService tokenService;
    private final RedisTemplate<String, Object> redisTemplate;



    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.api-key:YSX1HpAFum4TpCecyFBxs4eIjAlbhKqK6fpcSQp8}")
    private String apiKey;

    @Autowired
    public GestoPagoProductoSyncService(GestoPagoProductClient gestoPagoProductClient,
                                       GestoPagoProductoRepository productoRepository,
                                       GestoPagoTokenService tokenService,
                                       GestoPagoProductoMapper productoMapper,
                                       @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.productoRepository = productoRepository;
        this.tokenService = tokenService;
        this.productoMapper = productoMapper;
        this.redisTemplate = redisTemplate;
    }


    /**
     * Sincronización programada diaria (cada 24 horas).
     */
    @Scheduled(fixedRateString = "${gestopago.productos.refresh-rate-ms:86400000}", initialDelay = 10000)
    public void sincronizarProductosProgramado() {
        log.info("Iniciando sincronización programada de productos GestoPago...");
        sincronizarProductosConRespuesta();
    }

    /**
     * Sincroniza la lista de productos validando las 4 reglas de negocio:
     * 1. Si el nuevo catálogo viene vacío/nulo -> NO actualizar.
     * 2. Si el tamaño del nuevo catálogo es menor que el de la BD -> NO actualizar.
     * 3. Si el tamaño del nuevo catálogo es igual al de la BD -> NO actualizar.
     * 4. Si y solamente si el catálogo es mayor -> actualizar primero en Postgres, y después en Redis.
     * 
     * Todas las excepciones son capturadas con try-catch ("no tiene que tronar").
     * Retorna una respuesta estructurada en Map<String, Object>.
     */
    @Transactional
    public Map<String, Object> sincronizarProductosConRespuesta() {
        Map<String, Object> respuesta = new HashMap<>();
        try {
            Optional<GestoPagoToken> tokenOpt = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);

            if (tokenOpt.isEmpty() || tokenOpt.get().getToken() == null) {
                log.warn("No existe token activo de GestoPago. Intentando renovar...");
                tokenService.renovarToken();
                tokenOpt = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
            }

            if (tokenOpt.isEmpty() || tokenOpt.get().getToken() == null) {
                log.error("No se pudo obtener token válido de GestoPago.");
                respuesta.put("codigo", 401);
                respuesta.put("actualizado", false);
                respuesta.put("mensaje", "No se pudo obtener token válido de GestoPago.");
                return respuesta;
            }

            String authHeader = "Bearer " + tokenOpt.get().getToken();
            GestoPagoProductListXmlResponse response = gestoPagoProductClient.getProductList(authHeader, idDistribuidor, apiKey);

            List<GestoPagoProductDto> nuevosProductos = (response != null && response.getProductos() != null)
                    ? response.getProductos() : Collections.emptyList();

            long countBdActual = 0;
            try {
                countBdActual = productoRepository.count();
            } catch (Exception e) {
                log.error("Error al consultar el conteo actual de productos en PostgreSQL BD: {}", e.getMessage(), e);
            }

            int tamanoNuevo = nuevosProductos.size();

            // REGLA 1: Si viene vacío -> NO actualizar
            if (nuevosProductos.isEmpty()) {
                log.warn("Regla 1: El catálogo recibido viene vacío o nulo. No se actualiza.");
                respuesta.put("codigo", 200);
                respuesta.put("actualizado", false);
                respuesta.put("mensaje", "Regla 1: El catálogo recibido viene vacío. No se realiza actualización.");
                respuesta.put("tamanoNuevo", 0);
                respuesta.put("tamanoActualBD", countBdActual);
                return respuesta;
            }

            // REGLA 2: Si el tamaño es menor -> NO actualizar
            if (tamanoNuevo < countBdActual) {
                log.warn("Regla 2: El tamaño del nuevo catálogo ({}) es menor que el de la BD ({}). No se actualiza.", tamanoNuevo, countBdActual);
                respuesta.put("codigo", 200);
                respuesta.put("actualizado", false);
                respuesta.put("mensaje", "Regla 2: El tamaño del catálogo recibido (" + tamanoNuevo + ") es menor que el actual en BD (" + countBdActual + "). No se realiza actualización.");
                respuesta.put("tamanoNuevo", tamanoNuevo);
                respuesta.put("tamanoActualBD", countBdActual);
                return respuesta;
            }

            // REGLA 3: Si tienen el mismo tamaño -> NO actualizar (sigue teniendo la misma información)
            if (tamanoNuevo == countBdActual) {
                log.info("Regla 3: El catálogo recibido ({}) tiene el mismo tamaño que en BD ({}). No se actualiza.", tamanoNuevo, countBdActual);
                respuesta.put("codigo", 200);
                respuesta.put("actualizado", false);
                respuesta.put("mensaje", "Regla 3: El catálogo recibido (" + tamanoNuevo + ") tiene el mismo tamaño que el actual en BD (" + countBdActual + "). Sigue teniendo la misma información.");
                respuesta.put("tamanoNuevo", tamanoNuevo);
                respuesta.put("tamanoActualBD", countBdActual);
                return respuesta;
            }

            // REGLA 4: Si es mayor -> SÍ actualizar (Postgres primero, después Redis)
            log.info("Regla 4: El nuevo catálogo ({}) es MAYOR que el catálogo actual en BD ({}). Procediendo a actualizar...", tamanoNuevo, countBdActual);

            List<GestoPagoProducto> productosAActualizar = new ArrayList<>();
            for (GestoPagoProductDto dto : nuevosProductos) {
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

            // 1. Guardar en Postgres
            try {
                productoRepository.saveAll(productosAActualizar);
                log.info("Catálogo guardado exitosamente en PostgreSQL. Total guardados: {}", productosAActualizar.size());
                respuesta.put("guardadoPostgres", true);
            } catch (Exception e) {
                log.error("Excepción al guardar catálogo en PostgreSQL: {}", e.getMessage(), e);
                respuesta.put("codigo", 500);
                respuesta.put("actualizado", false);
                respuesta.put("guardadoPostgres", false);
                respuesta.put("mensaje", "Error al guardar catálogo en PostgreSQL: " + e.getMessage());
                return respuesta; // Si Postgres falla, no guardamos en Redis
            }

            // 2. Guardar en Redis después de Postgres
            boolean redisExito = false;
            try {
                if (redisTemplate != null) {
                    redisTemplate.opsForValue().set(REDIS_KEY_PRODUCTOS, productosAActualizar);
                    log.info("Catálogo guardado exitosamente en Redis con la clave: {}", REDIS_KEY_PRODUCTOS);
                    redisExito = true;
                } else {
                    log.warn("redisTemplate es nulo. No se pudo guardar en Redis.");
                }
            } catch (Exception e) {
                log.error("Excepción al guardar catálogo en Redis (la app continúa de forma segura sin tronar): {}", e.getMessage(), e);
                respuesta.put("errorRedis", e.getMessage());
            }

            respuesta.put("codigo", 200);
            respuesta.put("actualizado", true);
            respuesta.put("guardadoPostgres", true);
            respuesta.put("guardadoRedis", redisExito);
            respuesta.put("mensaje", "Catálogo actualizado correctamente al ser mayor que el existente.");
            respuesta.put("tamanoNuevo", tamanoNuevo);
            respuesta.put("tamanoAnteriorBD", countBdActual);
            respuesta.put("tamanoNuevoBD", productosAActualizar.size());
            respuesta.put("data", productosAActualizar);

        } catch (Throwable t) {
            log.error("Excepción global no esperada durante la sincronización de productos (capturada con try-catch): {}", t.getMessage(), t);
            respuesta.put("codigo", 500);
            respuesta.put("actualizado", false);
            respuesta.put("mensaje", "Error interno durante la sincronización de catálogo: " + t.getMessage());
        }

        return respuesta;
    }

    /**
     * Obtiene los productos almacenados. Intenta leer de Redis primero o de Postgres,
     * devolviendo los datos estructurados en un Map<String, Object>.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerProductosBdConRespuesta() {
        Map<String, Object> respuesta = new HashMap<>();
        List<GestoPagoProducto> productos = new ArrayList<>();
        String origen = "PostgreSQL";

        try {
            // Intentar consultar Redis primero
            if (redisTemplate != null) {
                try {
                    Object redisData = redisTemplate.opsForValue().get(REDIS_KEY_PRODUCTOS);
                    if (redisData instanceof List<?>) {
                        @SuppressWarnings("unchecked")
                        List<GestoPagoProducto> listFromRedis = (List<GestoPagoProducto>) redisData;
                        if (!listFromRedis.isEmpty()) {
                            productos = listFromRedis;
                            origen = "Redis";
                            log.info("Productos obtenidos desde cache Redis. Total: {}", productos.size());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Falló lectura desde Redis, recurriendo a PostgreSQL: {}", e.getMessage());
                }
            }

            if (productos.isEmpty()) {
                productos = productoRepository.findByActivoTrue();
                origen = "PostgreSQL";
                if (productos.isEmpty()) {
                    log.info("BD local vacía. Disparando sincronización inicial...");
                    Map<String, Object> syncResult = sincronizarProductosConRespuesta();
                    if (Boolean.TRUE.equals(syncResult.get("actualizado"))) {
                        productos = productoRepository.findByActivoTrue();
                    }
                }
            }

            respuesta.put("codigo", 200);
            respuesta.put("mensaje", "Productos obtenidos exitosamente.");
            respuesta.put("origen", origen);
            respuesta.put("total", productos.size());
            respuesta.put("data", productos);

        } catch (Exception e) {
            log.error("Excepción al obtener productos (capturada con try-catch): {}", e.getMessage(), e);
            respuesta.put("codigo", 500);
            respuesta.put("mensaje", "Error al obtener productos: " + e.getMessage());
            respuesta.put("data", Collections.emptyList());
        }

        return respuesta;
    }

    /**
     * Mantiene compatibilidad con llamadas existentes que esperan List<GestoPagoProducto>.
     */
    @Transactional(readOnly = true)
    public List<GestoPagoProducto> obtenerProductosBd() {
        try {
            Map<String, Object> resp = obtenerProductosBdConRespuesta();
            Object data = resp.get("data");
            if (data instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<GestoPagoProducto> lista = (List<GestoPagoProducto>) data;
                return lista;
            }
        } catch (Exception e) {
            log.error("Error en obtenerProductosBd: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Mantiene compatibilidad para invocar sincronizar sin retorno.
     */
    public void sincronizarProductos() {
        sincronizarProductosConRespuesta();
    }
}