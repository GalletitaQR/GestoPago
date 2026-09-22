# 🚀 Proyecto Core REST API - GestoPago, Redis & PostgreSQL

Este proyecto es una aplicación web en **Spring Boot 3.3.6** (Java 17) estructurada bajo el patrón de arquitectura RESTful. Cuenta con integración al proveedor externo **GestoPago**, almacenamiento persistente en **PostgreSQL**, aceleración mediante caché en **Redis** (patrón *Cache-Aside* con lectura prioritaria y respaldo en la base de datos), validaciones estricta de parámetros de entrada, y documentación interactiva expuesta mediante **Swagger UI (OpenAPI 3)**.

---

## 📋 Tabla de Contenidos

1. [Arquitectura y Tecnologías](#-arquitectura-y-tecnologías)
2. [Configuración del Entorno (.env y application.properties)](#-configuración-del-entorno)
3. [Integración GestoPago y Reglas de Negocio](#-integración-gestopago-y-reglas-de-negocio)
4. [Estrategia de Caché y Respaldo (Redis & PostgreSQL)](#-estrategia-de-caché-y-respaldo-redis--postgresql)
5. [Documentación de APIs REST & Swagger UI](#-documentación-de-apis-rest--swagger-ui)
6. [Validaciones y Manejo de Excepciones](#-validaciones-y-manejo-de-excepciones)
7. [Instrucciones de Ejecución](#-instrucciones-de-ejecución)

---

## 🛠️ Arquitectura y Tecnologías

- **Lenguaje & Framework:** Java 17, Spring Boot 3.3.6
- **Persistencia Relacional:** PostgreSQL, Spring Data JPA, Hibernate 6.5.3, Flyway (Migraciones de BD)
- **Base de Datos en Memoria / Caché:** Spring Data Redis, Lettuce Client
- **Integración de Servicios Externos:** Spring Cloud OpenFeign, Jackson Dataformat XML / JSON
- **Documentación API:** Springdoc OpenAPI UI 2.2.0 (Swagger 3)
- **Construcción y Herramientas:** Gradle, Lombok, MapStruct

---

## ⚙️ Configuración del Entorno

La aplicación requiere la configuración de variables de entorno (almacenadas en `.env` o en el entorno del sistema):

### Archivo `.env` / `application.properties`
```properties
# Configuración de Base de Datos PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/gestopago_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=root

# Configuración de Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Credenciales de Servicio Externo GestoPago
GESTOPAGO_AUTH_URL=https://api.gestopago.com
GESTOPAGO_AUTH_ID_DISTRIBUIDOR=12345
GESTOPAGO_AUTH_CODIGO_DISPOSITIVO=DEV_01
GESTOPAGO_AUTH_PASSWORD=contrasena_gestopago
GESTOPAGO_AUTH_API_KEY=YSX1HpAFum4TpCecyFBxs4eIjAlbhKqK6fpcSQp8

# Tasa de Sincronización Programada (24 Horas = 86400000 ms)
GESTOPAGO_PRODUCTOS_REFRESH_RATE_MS=86400000
```

---

## 🔄 Integración GestoPago y Reglas de Negocio

El servicio `GestoPagoProductoSyncService` interactúa con la API de GestoPago para mantener actualizado el catálogo local de productos. Cuenta con un servicio de autenticación (`GestoPagoTokenService`) que obtiene y renueva el token JWT automáticamente.

### 📜 Reglas de Negocio para la Sincronización del Catálogo
Al consultar el catálogo externo de productos de GestoPago, se evalúan estrictamente 4 reglas de negocio en el método `sincronizarProductosConRespuesta()`:

1. **Regla 1 (Catálogo Vacío):** Si el catálogo recibido del servicio externo viene nulo o con 0 elementos, **NO se realiza la actualización** en las bases de datos locales.
2. **Regla 2 (Catálogo Menor):** Si el tamaño del nuevo catálogo recibido es **menor** que el total de productos actualmente almacenados en PostgreSQL, **NO se realiza la actualización**.
3. **Regla 3 (Mismo Tamaño):** Si el catálogo recibido tiene el **mismo número de registros** que la base de datos local, **NO se realiza la actualización** (se conserva la información existente).
4. **Regla 4 (Catálogo Mayor):** Si y solo si el catálogo recibido es **MAYOR** que el almacenamiento local:
   - **Paso 1:** Se guardan o actualizan las entidades primero en **PostgreSQL**.
   - **Paso 2:** Tras asegurar la persistencia en PostgreSQL, se actualiza la clave de caché `gestopago:productos` en **Redis**.

> 💡 **Sincronización Programada:** Se ejecuta una tarea en segundo plano (`@Scheduled`) cada 24 horas para garantizar la coherencia del catálogo.

---

## 🗄️ Estrategia de Caché y Respaldo (Redis & PostgreSQL)

Se implementó el patrón **Cache-Aside** en dos niveles para maximizar el rendimiento y la tolerancia a fallos:

```
                  ┌──────────────────────┐
                  │   Petición Cliente   │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │   ¿Existe en Redis?  │
                  └──────┬────────┬──────┘
                         │        │
               SÍ        │        │ NO / Cache Miss / Error
      ┌──────────────────┘        └──────────────────┐
      ▼                                              ▼
┌──────────┐                               ┌────────────────────┐
│ Responder│                               │ Consultar Respaldo │
│  REDIS   │                               │    POSTGRESQL      │
└──────────┘                               └─────────┬──────────┘
                                                     │
                                                     ▼
                                           ┌────────────────────┐
                                           │ Guardar en Redis & │
                                           │ Responder POSTGRES │
                                           └────────────────────┘
```

### 1. Consulta de Productos (`/api/gestopago/getProductList`)
- **Búsqueda en Redis:** Se consulta la clave `gestopago:productos`.
- **Respaldo en PostgreSQL:** Si la clave está vacía o Redis no está disponible, se consulta la tabla `gestopago_productos` en PostgreSQL.
- **Poblado Automático:** Los registros de PostgreSQL se guardan inmediatamente en Redis para que las siguientes consultas respondan a velocidad en memoria.
- **Auto-recuperación:** Si la BD local está totalmente vacía, se dispara la sincronización inicial con GestoPago.

### 2. Consulta de Personas (`/api/v1/personas/{id}` y `/api/v1/personas/buscar`)
- **Búsqueda en Redis:** Busca mediante la clave `persona:id:{id}` o `persona:user:{username}`.
- **Respaldo en PostgreSQL:** Si no existe en Redis, consulta en la base de datos PostgreSQL mediante `PersonasRepository`.
- **TTL (Time to Live):** Si se encuentra en PostgreSQL, los datos se guardan en Redis con un tiempo de expiración de **10 minutos**.

---

## 📑 Documentación de APIs REST & Swagger UI

Toda la API se encuentra documentada e interactiva mediante **Springdoc OpenAPI 3**.

- **URL de Swagger UI:** `http://localhost:8080/swagger-ui/index.html` (o en el puerto donde ejecutes la aplicación).

### Tabla de Endpoints Principales

| Módulo | Método | Endpoint | Descripción | Validaciones |
| :--- | :--- | :--- | :--- | :--- |
| **Productos** | `GET` | `/api/gestopago/getProductList` | Consulta catálogo de productos. Prioriza Redis, respaldo PostgreSQL. | N/A |
| **Productos** | `POST` | `/api/gestopago/sync` | Ejecuta la sincronización manual según las 4 reglas. | Auth Token |
| **Personas** | `GET` | `/api/v1/personas/{id}` | Consulta datos de persona por ID. | `@PathVariable @Positive` (> 0) |
| **Personas** | `GET` | `/api/v1/personas/buscar` | Consulta datos de persona por Username. | `@RequestParam @NotBlank` |
| **Personas** | `DELETE` | `/api/v1/personas/cache/{id}` | Limpia la clave de caché de una persona en Redis. | `@PathVariable @Positive` |
| **Personas** | `POST` | `/personas` | Crea un registro de persona en PostgreSQL. | `@Valid @RequestBody` |
| **Personas** | `PUT` | `/personasActualiza` | Actualiza datos de persona. | `@Valid @RequestBody` |
| **Personas** | `PUT` | `/personasElimina` | Elimina persona por nombre. | `@Valid @RequestBody` |
| **Seguridad** | `POST` | `/login` | Autenticación de usuario. | Username & Password obligatorios |

---

## 🛡️ Validaciones y Manejo de Excepciones

La aplicación cuenta con un manejador global de excepciones (`GlobalExceptionHandler`) que evita que el servidor responda errores no estructurados o caídas inesperadas (*"no truena"*):

- **Error 400 (Bad Request):**
  - Ocurre cuando fallan las validaciones de entrada (`@Positive`, `@NotBlank`, `@Valid`).
  - Retorna un JSON explicativo con la lista de campos inválidos.
- **Error 404 (Not Found):**
  - Ocurre al buscar una persona o producto que no existe en Redis ni en PostgreSQL mediante la excepción `RecursoNoEncontradoException`.
- **Error 500 (Internal Server Error):**
  - Captura excepciones inesperadas del sistema retornando una respuesta JSON estandarizada con `codigo` y `mensaje`.

---

## 🚀 Instrucciones de Ejecución

### Prerrequisitos
- **Java 17 JDK** o superior instalado.
- **PostgreSQL** (Ejecutándose en puerto `5432` con la BD creada).
- **Redis** (Ejecutándose en puerto `6379`).

### Comandos de Construcción y Ejecución (Gradle)

1. **Compilar el proyecto:**
   ```powershell
   .\gradlew compileJava
   ```

2. **Ejecutar pruebas unitarias:**
   ```powershell
   .\gradlew test
   ```

3. **Iniciar la aplicación:**
   ```powershell
   .\gradlew bootRun
   ```

4. **Construir Docker Image (Opcional):**
   ```powershell
   .\gradlew clean build dockerImage dockerUp
   ```
