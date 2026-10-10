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
        String url = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        if (url == null || url.trim().isEmpty()) {
            url = "jdbc:postgresql://localhost:5432/puntored";
        }

        // Si la URL proviene de proveedores Cloud (Render/Heroku) con formato postgres:// o postgresql://
        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            try {
                String dummyUrl = url.replaceFirst("^postgresql?://", "http://");
                java.net.URI uri = new java.net.URI(dummyUrl);

                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String userInfo = uri.getUserInfo();

                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    if (username == null || username.trim().isEmpty()) {
                        username = parts[0];
                    }
                    if (password == null || password.trim().isEmpty()) {
                        password = parts[1];
                    }
                }

                url = "jdbc:postgresql://" + host + ":" + port + (path != null ? path : "");
                log.info("URL de PostgreSQL convertida a formato JDBC limpia: {}", url);
            } catch (Exception e) {
                log.warn("Fallo al parsear URI de base de datos, usando reemplazo directo: {}", e.getMessage());
                url = url.replaceFirst("^postgresql?://", "jdbc:postgresql://");
            }
        }

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
