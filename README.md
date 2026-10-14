# 🏦 Core Banking, Personas & GestoPago REST API

Aplicación web empresarial desarrollada en **Spring Boot 3.3.6** (Java 17) bajo un diseño de arquitectura RESTful robusto y seguro. Integra servicios bancarios (gestión de clientes, cuentas y usuarios), persistencia en **PostgreSQL**, aceleración en memoria mediante **Redis** (patrón *Cache-Aside* con fallback a base de datos), integración externa con el proveedor **GestoPago**, y documentación interactiva estandarizada con **Swagger UI (OpenAPI 3)**.

---

## 📋 Tabla de Contenidos

1. [Arquitectura y Principios de Diseño](#-arquitectura-y-principios-de-diseño)
2. [Estandarización de Peticiones y Control por Banderas](#-estandarización-de-peticiones-y-control-por-banderas)
3. [Estructura de Respuesta Uniforme (GenericResponse)](#-estructura-de-respuesta-uniforme-genericresponse)
4. [Validaciones Jakarta y Manejo Centralizado de Excepciones](#-validaciones-jakarta-y-manejo-centralizado-de-excepciones)
5. [Estrategia de Caché y Respaldo (Redis & PostgreSQL)](#-estrategia-de-caché-y-respaldo-redis--postgresql)
6. [Documentación Interactiva con Swagger UI (OpenAPI 3)](#-documentación-interactiva-con-swagger-ui-openapi-3)
7. [Guía de Despliegue en Render (Render.com)](#-guía-de-despliegue-en-render-rendercom)
8. [Instrucciones de Ejecución Local](#-instrucciones-de-ejecución-local)

---

## 🛠️ Arquitectura y Principios de Diseño

- **Lenguaje & Plataforma:** Java 17 LTS, Spring Boot 3.3.6.
- **Sin IDs en la Ruta (`@PathVariable`):** Por directriz arquitectónica y prevención de vulnerabilidades IDOR (*Insecure Direct Object Reference*), ninguna ruta de negocio expone identificadores en la URL. Todas las consultas, actualizaciones y bajas se transmiten en el cuerpo (`@RequestBody`) y se controlan mediante **banderas**.
- **Respuestas Normalizadas:** Todos los endpoints responden bajo el contrato de `GenericResponse` o `GenericDataResponse<T>`.
- **Manejo Centralizado de Excepciones:** Un único `@RestControllerAdvice` (`GlobalExceptionHandler`) captura validaciones y reglas de negocio, garantizando respuestas uniformes y seguras sin fugar datos sensibles ni trazas del sistema.
- **Seguridad & Autorización:** Spring Security 6 con autenticación JWT Bearer y verificación de titularidad del recurso (`SecurityHelper`).
- **Persistencia & Caché:** PostgreSQL con Spring Data JPA y Flyway; Redis con Lettuce Client para lectura prioritaria en memoria.

---

## 🚩 Estandarización de Peticiones y Control por Banderas

En lugar de crear múltiples rutas o exponer IDs en la URL, las operaciones se consolidan en endpoints base que evalúan el atributo `bandera` dentro del JSON:

### 1. Módulo de Usuarios (`/usuarios`)

| Endpoint | Método | Request Body | Descripción de Banderas |
| :--- | :---: | :--- | :--- |
| `/usuarios/consultar` | `POST` | `UsuarioConsultaRequest` | **Bandera 1:** Consulta por ID (`id` requerido).<br>**Bandera 2:** Consulta por Correo (`correo` requerido).<br>**Bandera 3:** Listar todos los usuarios.<br>*(Incluye `usarCache: true/false` para alternar entre caché o BD sin duplicar endpoints)* |
| `/usuarios/actualizar` | `PUT` | `UsuarioActualizaRequest` | **Bandera 1:** Actualizar Contraseña (`passwordActual`, `nuevaPassword`).<br>**Bandera 2:** Actualizar Estado Activo (`activo: true/false`). |

#### Ejemplo: Consulta de Usuario por ID
```json
POST /usuarios/consultar
Content-Type: application/json

{
  "bandera": 1,
  "id": 1,
  "usarCache": true
}
```

#### Ejemplo: Actualización de Contraseña
```json
PUT /usuarios/actualizar
Content-Type: application/json

{
  "id": 1,
  "bandera": 1,
  "passwordActual": "Temporal123#",
  "nuevaPassword": "PasswordSegura2026!"
}
```

---

### 2. Módulo de Clientes (`/clientes`)

| Endpoint | Método | Request Body | Descripción de Banderas |
| :--- | :---: | :--- | :--- |
| `/clientes` | `POST` | `ClienteRequestDto` | Registro de nuevo cliente con Bean Validation (Retorna HTTP 201). |
| `/clientes/consultar` | `POST` | `ClienteConsultaRequest` | **Bandera 1:** Por ID.<br>**Bandera 2:** Por CURP.<br>**Bandera 3:** Por RFC.<br>**Bandera 4:** Por Correo Electrónico.<br>**Bandera 5:** Por Número de Cuenta.<br>**Bandera 6:** Clientes Activos.<br>**Bandera 7:** Rango de Fechas (`fechaInicio`, `fechaFin`).<br>**Bandera 8:** Todos los Clientes. |
| `/clientes/actualizar` | `PUT` | `ClienteActualizaRequest` | **Bandera 1:** Actualización general de datos personales, contacto y domicilio.<br>**Bandera 2:** Actualización laboral y de ingresos. *(Protege CURP, RFC y No. Cuenta)* |
| `/clientes/eliminar` | `PUT` | `ClienteEliminaRequest` | **Bandera 1:** Baja lógica (desactiva cliente, cuentas y usuario asociado). |

#### Ejemplo: Consulta de Cliente por CURP
```json
POST /clientes/consultar
Content-Type: application/json

{
  "bandera": 2,
  "curp": "GARC850101HDFRRN09"
}
```

#### Ejemplo: Baja Lógica de Cliente
```json
PUT /clientes/eliminar
Content-Type: application/json

{
  "id": 10,
  "bandera": 1,
  "motivo": "Cancelación solicitada por el titular"
}
```

---

### 3. Módulo de Cuentas (`/cuentas`)

| Endpoint | Método | Request Body | Descripción de Banderas |
| :--- | :---: | :--- | :--- |
| `/cuentas/consultar` | `POST` | `CuentaConsultaRequest` | **Bandera 1:** Detalle por Número de Cuenta.<br>**Bandera 2:** Consulta exclusiva de Saldo.<br>**Bandera 3:** Cuentas Activas. |

---

### 4. Módulo de Consulta de Personas (`/api/v1/personas`)

| Endpoint | Método | Request Body | Descripción de Banderas |
| :--- | :---: | :--- | :--- |
| `/api/v1/personas/consultar` | `POST` | `PersonaConsultaRequest` | **Bandera 1:** Por ID (con lectura prioritaria en Redis y respaldo en PostgreSQL).<br>**Bandera 2:** Por Nombre de Usuario (Username). |
| `/api/v1/personas/cache/limpiar` | `POST` | `PersonaCacheRequest` | **Bandera 1:** Invalidar/Limpiar clave de caché en Redis por ID. |

---

## 📦 Estructura de Respuesta Uniforme (`GenericResponse`)

Todas las operaciones de la API responden con una estructura estándar:

### Respuesta Exitosa con Datos (`GenericDataResponse<T>`)
```json
{
  "codigo": 200,
  "mensaje": "Consulta de usuario realizada exitosamente.",
  "datos": {
    "id": 1,
    "clienteId": 1,
    "correo": "cliente@banco.com",
    "activo": true,
    "fechaCreacion": "2026-10-13T10:00:00"
  }
}
```

### Respuesta Exitosa Simple (`GenericResponse`)
```json
{
  "codigo": 200,
  "mensaje": "Cliente ID 10 desactivado exitosamente (baja lógica)."
}
```

### Respuesta de Error Centralizada
```json
{
  "codigo": 400,
  "mensaje": "Error de validación: id: El ID debe ser un número entero positivo mayor a 0; bandera: La bandera de consulta es obligatoria"
}
```

---

## 🛡️ Validaciones Jakarta y Manejo Centralizado de Excepciones

Las entradas se validan mediante anotaciones de **Jakarta Bean Validation** en los DTOs:
- `@NotNull`, `@NotBlank`: Obligatoriedad y campos no vacíos.
- `@Size(min, max)`: Longitud de cadenas y caracteres.
- `@Pattern(regexp)`: Expresiones regulares para CURP, RFC, Contraseñas fuertes y nombres.
- `@Positive`: Identificadores numéricos mayores a cero.
- `@Email`: Formato estricto de correo electrónico.
- `@Past`: Fechas en el pasado (p. ej. fecha de nacimiento).

### Mapeo Centralizado en `GlobalExceptionHandler`:
| Excepción | Código HTTP | Mensaje / Comportamiento |
| :--- | :---: | :--- |
| `MethodArgumentNotValidException` | `400 BAD_REQUEST` | Lista concatenada de campos inválidos y su regla infringida. |
| `ConstraintViolationException` | `400 BAD_REQUEST` | Detalle de restricción en parámetros. |
| `ReglaNegocioException`, `ContrasenaInvalidaException` | `400 BAD_REQUEST` | Violación de reglas funcionales o contraseñas débiles. |
| `CredencialesInvalidasException` | `401 UNAUTHORIZED` | Contraseña o credenciales incorrectas. |
| `UsuarioInactivoException`, `AccesoDenegadoException` | `403 FORBIDDEN` | Usuario inactivo o intento de acceso a ID no propio (IDOR). |
| `*NoEncontradoException` | `404 NOT_FOUND` | Recurso no encontrado en BD ni en caché. |
| `*DuplicadoException`, `ClienteYaRegistradoException` | `409 CONFLICT` | Duplicidad en CURP, RFC o Correo. |
| `Exception` (Cualquier error no controlado) | `500 INTERNAL_SERVER_ERROR` | Mensaje genérico seguro. Se oculta el stack trace. |

---

## 🗄️ Estrategia de Caché y Respaldo (Redis & PostgreSQL)

Se implementa el patrón **Cache-Aside** transparente en la capa de servicios:
1. **Lectura Prioritaria en Redis:** Al consultar por ID o clave, se busca primero en Redis.
2. **Fallback a PostgreSQL:** Si no está en caché (*Cache Miss*), se consulta en la base de datos PostgreSQL.
3. **Poblado Automático (TTL 10 min):** El resultado se almacena en Redis para acelerar las siguientes consultas.
4. **Invalidación de Caché:** Al actualizar o desactivar un usuario/cliente, se purga la clave en Redis para prevenir inconsistencias de datos.

---

## 📑 Documentación Interactiva con Swagger UI (OpenAPI 3)

La documentación se genera automáticamente mediante **Springdoc OpenAPI** e incluye soporte para **Bearer Authentication (JWT)**:

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON Spec:** `http://localhost:8080/v3/api-docs`

### Cómo probar endpoints protegidos en Swagger:
1. Dirígete a la sección **Autenticación** y ejecuta `POST /auth/login`.
2. Copia el token retornado en el campo `datos.token`.
3. Haz clic en el botón superior **Authorize 🔓** en Swagger UI.
4. Pega el token y presiona **Authorize**. Ahora todos los endpoints se invocarán con el encabezado `Authorization: Bearer <token>`.

---

## ☁️ Guía de Despliegue en Render (Render.com)

Render permite desplegar esta aplicación como un **Web Service**, conectándola a instancias administradas de **PostgreSQL** y **Redis**.

### Paso 1: Crear Servicios de Base de Datos y Caché en Render
1. **PostgreSQL en Render:**
   - En tu panel de Render, presiona **New +** > **PostgreSQL**.
   - Asigna un nombre (p. ej. `banco-postgres`).
   - Copia la **Internal Database URL** generada (formato `postgresql://user:pass@host/dbname`).
2. **Redis:**
   - Puedes usar una instancia de **Redis en Render** o un proveedor como **Upstash** o **Redis Cloud**.
   - Copia la URL de conexión (formato `rediss://default:password@host:port` o `redis://host:6379`).

---

### Paso 2: Crear el Web Service en Render

#### Opción A: Despliegue con Docker (Recomendada)
El repositorio incluye un `Dockerfile` optimizado en dos fases (compilación en Gradle y ejecución en Eclipse Temurin 17 JRE Alpine):

1. En Render, selecciona **New +** > **Web Service**.
2. Conecta tu repositorio de GitHub / GitLab.
3. En **Environment**, selecciona **Docker**.
4. Render detectará automáticamente el archivo `Dockerfile`.

#### Opción B: Despliegue Nativo (Gradle)
Si prefieres compilar directamente en Render:
- **Environment:** `Java`
- **Build Command:** `./gradlew bootJar -x test`
- **Start Command:** `java -jar build/libs/*.jar`

---

### Paso 3: Configurar Variables de Entorno en Render
En la pestaña **Environment** de tu Web Service en Render, añade las siguientes variables:

| Variable de Entorno | Valor de Ejemplo / Descripción |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host-render>:5432/<dbname>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `<usuario-postgres-render>` |
| `SPRING_DATASOURCE_PASSWORD` | `<password-postgres-render>` |
| `REDIS_URL` | `redis://<host-redis>:6379` o URL de Upstash/Redis Cloud |
| `JWT_SECRET` | Clave secreta robusta de 64+ caracteres para firmar tokens JWT |
| `JWT_EXPIRATION_MS` | `86400000` (24 horas en milisegundos) |
| `GESTOPAGO_AUTH_URL` | `https://gestopago.portalventas.net` |
| `GESTOPAGO_AUTH_ID_DISTRIBUIDOR` | ID de distribuidor asignado |
| `GESTOPAGO_AUTH_CODIGO_DISPOSITIVO`| Código de dispositivo |
| `GESTOPAGO_AUTH_PASSWORD` | Contraseña del servicio externo |
| `GESTOPAGO_AUTH_API_KEY` | Llave API de GestoPago |
| `PORT` | `8080` (Puerto estándar en Render) |

---

### Paso 4: Validar el Despliegue en Render
Una vez finalizado el despliegue exitoso:
- **URL Base:** `https://<tu-app>.onrender.com`
- **Swagger UI en Producción:** `https://<tu-app>.onrender.com/swagger-ui/index.html`
- **Actuator Health Check:** `https://<tu-app>.onrender.com/actuator/health`

---

## 💻 Instrucciones de Ejecución Local

### Prerrequisitos
- **Java 17 JDK** o superior instalado.
- **PostgreSQL** corriendo localmente en el puerto `5432`.
- **Redis** corriendo localmente en el puerto `6379`.

### 1. Variables de Entorno Locales (`.env`)
Crea o verifica el archivo `.env` en la raíz del proyecto:
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/puntored
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=admin
REDIS_URL=redis://localhost:6379
JWT_SECRET=ClaveSecretaSuperSeguraParaGenerarTokensJWTEmpleadaEnElBanco123456!
JWT_EXPIRATION_MS=86400000
```

### 2. Comandos Gradle
```powershell
# Compilar fuentes
.\gradlew compileJava

# Ejecutar pruebas unitarias
.\gradlew test

# Iniciar el servidor localmente
.\gradlew bootRun
```

### 3. Ejecución Completa con Docker Compose
Si tienes Docker Desktop instalado, puedes levantar PostgreSQL, Redis y la API en conjunto:
```powershell
docker compose up -d
```
Accede inmediatamente a Swagger UI en: `http://localhost:8080/swagger-ui/index.html`.
