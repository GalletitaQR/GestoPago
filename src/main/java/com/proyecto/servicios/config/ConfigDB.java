package com.proyecto.servicios.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.proyecto.servicios.repositorys.sf",
                "com.proyecto.servicios.repositorys.gestopago",
                "com.proyecto.servicios.repositorys.banco"
        },
        transactionManagerRef = "sfTransactionManager",
        entityManagerFactoryRef = "sfEntityManagerFactory"
)
public class ConfigDB {
    @Autowired
    private Environment env;

    @Bean(name="sfDatasource")
    public DataSource sfDatasource(){
        HikariConfig config = new HikariConfig();

        // 1. Obtener URL de base de datos
        String url = env.getProperty("SPRING_DATASOURCE_URL");
        if (url == null || url.trim().isEmpty()) url = env.getProperty("DATABASE_URL");
        if (url == null || url.trim().isEmpty()) url = System.getenv("SPRING_DATASOURCE_URL");
        if (url == null || url.trim().isEmpty()) url = System.getenv("DATABASE_URL");
        if (url == null || url.trim().isEmpty()) url = env.getProperty("spring.datasource.url");
        if (url == null || url.trim().isEmpty()) url = "jdbc:postgresql://localhost:5432/puntored";

        // 2. Obtener Usuario (soporta typo RING_DATASOURCE_USERNAME de Render)
        String username = env.getProperty("SPRING_DATASOURCE_USERNAME");
        if (username == null || username.trim().isEmpty()) username = env.getProperty("RING_DATASOURCE_USERNAME");
        if (username == null || username.trim().isEmpty()) username = System.getenv("SPRING_DATASOURCE_USERNAME");
        if (username == null || username.trim().isEmpty()) username = System.getenv("RING_DATASOURCE_USERNAME");
        if (username == null || username.trim().isEmpty()) username = env.getProperty("DATABASE_USERNAME");
        if (username == null || username.trim().isEmpty()) username = env.getProperty("spring.datasource.username");

        // 3. Obtener Contraseña
        String password = env.getProperty("SPRING_DATASOURCE_PASSWORD");
        if (password == null || password.trim().isEmpty()) password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        if (password == null || password.trim().isEmpty()) password = env.getProperty("DATABASE_PASSWORD");
        if (password == null || password.trim().isEmpty()) password = System.getenv("DATABASE_PASSWORD");
        if (password == null || password.trim().isEmpty()) password = env.getProperty("spring.datasource.password");

        // 4. Si la URL contiene credenciales (ej: postgres://user:pass@host:port/db o jdbc:postgresql://user:pass@host:port/db)
        if (url.startsWith("postgres://") || url.startsWith("postgresql://") || url.contains("@")) {
            try {
                String parseUrl = url;
                if (parseUrl.startsWith("jdbc:postgresql://")) {
                    parseUrl = parseUrl.substring(5); // postgresql://...
                }
                String dummyUrl = parseUrl.replaceFirst("^postgresql?://", "http://");
                java.net.URI uri = new java.net.URI(dummyUrl);

                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String userInfo = uri.getUserInfo();

                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    password = parts[1];
                }

                url = "jdbc:postgresql://" + host + ":" + port + (path != null ? path : "");
                log.info("URL de PostgreSQL convertida a JDBC limpio: {}", url);
            } catch (Exception e) {
                log.warn("Fallo al parsear URI de base de datos, usando reemplazo directo: {}", e.getMessage());
                url = url.replaceFirst("^postgresql?://", "jdbc:postgresql://");
            }
        }

        // Si username sigue siendo el valor por defecto "postgres", verificar si en el entorno existe RING_DATASOURCE_USERNAME
        if ("postgres".equalsIgnoreCase(username)) {
            String ringUser = System.getenv("RING_DATASOURCE_USERNAME");
            if (ringUser != null && !ringUser.trim().isEmpty()) {
                username = ringUser;
            }
        }

        // Para Render PostgreSQL externo, asegurar sslmode=require si no está presente
        if (url.contains(".render.com") && !url.contains("sslmode")) {
            url += (url.contains("?") ? "&" : "?") + "sslmode=require";
        }

        log.info("Conectando sfDatasource con JDBC URL '{}' y usuario '{}'", url, username);

        config.setJdbcUrl(url);
        if (username != null && !username.trim().isEmpty()) {
            config.setUsername(username);
        }
        if (password != null && !password.trim().isEmpty()) {
            config.setPassword(password);
        }

        config.setMaximumPoolSize(10);
        config.setMaxLifetime(1880000);
        config.setConnectionTimeout(30000);
        config.setValidationTimeout(5000);
        config.setMinimumIdle(2);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("sfDatasource");

        return new HikariDataSource(config);
    }

    @Bean(name="sfEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean sfEntityManagerFactory(@Qualifier("sfDatasource") DataSource dataSource){
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(
                "com.proyecto.servicios.entity.sf",
                "com.proyecto.servicios.entity.gestopago",
                "com.proyecto.servicios.entity.banco"
        );
        em.setPersistenceUnitName("sfDatasource");
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.show-sql", false);
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("jakarta.persistence.query.timeout", 600000);

        return em;
    }
 @Bean(name="sfTransactionManager")
 public PlatformTransactionManager sfTransactionManager(@Qualifier("sfEntityManagerFactory") EntityManagerFactory sfEntityManagerFactory){
        return new JpaTransactionManager(sfEntityManagerFactory);

 }

}
