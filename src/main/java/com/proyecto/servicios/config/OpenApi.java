package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Consulta Personas - Redis & PostgreSQL")
                        .version("1.0")
                        .description("API RESTful con estrategia de caché en Redis y respaldo de base de datos PostgreSQL."))
                .addServersItem(new Server().url("/").description("Servidor por defecto (Ruta relativa)"));
    }
}


