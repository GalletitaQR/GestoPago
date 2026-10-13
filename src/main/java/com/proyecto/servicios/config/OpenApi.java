package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApi {

    public static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Core Banking, Personas & GestoPago REST API")
                        .version("2.0.0")
                        .description("API RESTful empresarial en Spring Boot 3 con arquitectura controlada por banderas en el cuerpo de la petición (sin IDs en la ruta), validaciones Jakarta Bean Validation, respuestas normalizadas basadas en GenericResponse, estrategia de caché en Redis con respaldo en PostgreSQL y autenticación segura mediante JWT.")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo")
                                .email("soporte@proyecto.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Servidor Local / Ruta Relativa"),
                        new Server().url("https://proyecto-core-api.onrender.com").description("Servidor de Despliegue en Render")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese su token JWT obtenido en el endpoint /auth/login para autorizar las peticiones en Swagger UI.")));
    }
}
