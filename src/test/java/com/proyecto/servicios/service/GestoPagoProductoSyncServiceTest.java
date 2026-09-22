package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoProductDto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListXmlResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductoSyncServiceTest {

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

    @Mock
    private GestoPagoProductoRepository productoRepository;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private GestoPagoProductoMapper productoMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private GestoPagoProductoSyncService syncService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(syncService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(syncService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(syncService, "apiKey", "test-key");

        GestoPagoToken token = new GestoPagoToken();
        token.setToken("valid-token");
        when(tokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.of(token));
    }

    @Test
    @DisplayName("Regla 1: No actualiza si el nuevo catálogo viene vacío")
    void regla1_catalogoVacio_noActualiza() {
        GestoPagoProductListXmlResponse response = new GestoPagoProductListXmlResponse();
        response.setProductos(Collections.emptyList());

        when(gestoPagoProductClient.getProductList(anyString(), anyInt(), anyString())).thenReturn(response);
        when(productoRepository.count()).thenReturn(5L);

        Map<String, Object> res = syncService.sincronizarProductosConRespuesta();

        assertEquals(200, res.get("codigo"));
        assertEquals(false, res.get("actualizado"));
        assertTrue(((String) res.get("mensaje")).contains("Regla 1"));

        verify(productoRepository, never()).saveAll(any());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Regla 2: No actualiza si el nuevo catálogo es más pequeño que el de la BD")
    void regla2_catalogoMenor_noActualiza() {
        GestoPagoProductListXmlResponse response = new GestoPagoProductListXmlResponse();
        List<GestoPagoProductDto> nuevos = List.of(createDto("P1"), createDto("P2"));
        response.setProductos(nuevos);

        when(gestoPagoProductClient.getProductList(anyString(), anyInt(), anyString())).thenReturn(response);
        when(productoRepository.count()).thenReturn(5L); // BD tiene 5, recibimos 2

        Map<String, Object> res = syncService.sincronizarProductosConRespuesta();

        assertEquals(200, res.get("codigo"));
        assertEquals(false, res.get("actualizado"));
        assertTrue(((String) res.get("mensaje")).contains("Regla 2"));

        verify(productoRepository, never()).saveAll(any());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Regla 3: No actualiza si el nuevo catálogo tiene el mismo tamaño que el de la BD")
    void regla3_catalogoIgual_noActualiza() {
        GestoPagoProductListXmlResponse response = new GestoPagoProductListXmlResponse();
        List<GestoPagoProductDto> nuevos = List.of(createDto("P1"), createDto("P2"), createDto("P3"));
        response.setProductos(nuevos);

        when(gestoPagoProductClient.getProductList(anyString(), anyInt(), anyString())).thenReturn(response);
        when(productoRepository.count()).thenReturn(3L); // BD tiene 3, recibimos 3

        Map<String, Object> res = syncService.sincronizarProductosConRespuesta();

        assertEquals(200, res.get("codigo"));
        assertEquals(false, res.get("actualizado"));
        assertTrue(((String) res.get("mensaje")).contains("Regla 3"));

        verify(productoRepository, never()).saveAll(any());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Regla 4: SÍ actualiza en Postgres y luego en Redis cuando el nuevo catálogo es MAYOR")
    void regla4_catalogoMayor_actualizaPostgresYLuegoRedis() {
        GestoPagoProductListXmlResponse response = new GestoPagoProductListXmlResponse();
        List<GestoPagoProductDto> nuevos = List.of(createDto("P1"), createDto("P2"), createDto("P3"), createDto("P4"));
        response.setProductos(nuevos);

        when(gestoPagoProductClient.getProductList(anyString(), anyInt(), anyString())).thenReturn(response);
        when(productoRepository.count()).thenReturn(2L); // BD tiene 2, recibimos 4 -> MAYOR
        when(productoRepository.findByCodigoProducto(anyString())).thenReturn(Optional.empty());
        when(productoMapper.toEntity(any())).thenAnswer(i -> {
            GestoPagoProductDto dto = i.getArgument(0);
            GestoPagoProducto p = new GestoPagoProducto();
            p.setCodigoProducto(dto.getCodigoProducto());
            return p;
        });
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Map<String, Object> res = syncService.sincronizarProductosConRespuesta();

        assertEquals(200, res.get("codigo"));
        assertEquals(true, res.get("actualizado"));
        assertEquals(true, res.get("guardadoPostgres"));
        assertEquals(true, res.get("guardadoRedis"));

        // Verificar ORDEN estricto: Primero Postgres (saveAll), luego Redis (opsForValue().set)
        InOrder inOrder = inOrder(productoRepository, redisTemplate, valueOperations);
        inOrder.verify(productoRepository).saveAll(anyList());
        inOrder.verify(redisTemplate).opsForValue();
        inOrder.verify(valueOperations).set(eq(GestoPagoProductoSyncService.REDIS_KEY_PRODUCTOS), anyList());
    }

    @Test
    @DisplayName("No truena si Redis lanza una excepción durante el guardado")
    void excepcionRedis_noTruenaLaAplicacion() {
        GestoPagoProductListXmlResponse response = new GestoPagoProductListXmlResponse();
        List<GestoPagoProductDto> nuevos = List.of(createDto("P1"), createDto("P2"));
        response.setProductos(nuevos);

        when(gestoPagoProductClient.getProductList(anyString(), anyInt(), anyString())).thenReturn(response);
        when(productoRepository.count()).thenReturn(1L); // BD 1, nuevos 2 -> Mayor
        when(productoRepository.findByCodigoProducto(anyString())).thenReturn(Optional.empty());
        when(productoMapper.toEntity(any())).thenReturn(new GestoPagoProducto());

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection error"));

        Map<String, Object> res = syncService.sincronizarProductosConRespuesta();

        // El código debe ser 200, actualizado true, y no lanzar la excepción hacia afuera
        assertEquals(200, res.get("codigo"));
        assertEquals(true, res.get("actualizado"));
        assertEquals(true, res.get("guardadoPostgres"));
        assertEquals(false, res.get("guardadoRedis"));
        assertNotNull(res.get("errorRedis"));
    }

    private GestoPagoProductDto createDto(String codigo) {
        GestoPagoProductDto dto = new GestoPagoProductDto();
        dto.setIdProducto(codigo);
        dto.setProducto("Producto " + codigo);
        return dto;
    }

}
