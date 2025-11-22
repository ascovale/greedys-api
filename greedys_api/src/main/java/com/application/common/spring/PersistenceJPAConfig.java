package com.application.common.spring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

@Configuration
@Profile({"dev", "docker", "prod"}) // 🔧 Per Dev MySQL, Docker e Produzione
@EnableJpaRepositories(basePackages = {
    "com.application.admin.persistence.dao",
    "com.application.agency.persistence.dao",
    "com.application.customer.persistence.dao",
    "com.application.restaurant.persistence.dao",
    "com.application.common.persistence.dao"
})
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class PersistenceJPAConfig {

    private final Environment env;

    @Bean
    DataSource dataSource() {
        System.out.println("🔧 PersistenceJPAConfig - ATTIVATO! Modalità MySQL per dev/docker/prod");
        
        String dbPassword;
        String[] activeProfiles = env.getActiveProfiles();
        boolean isDevProfile = java.util.Arrays.asList(activeProfiles).contains("dev");
        
        if (isDevProfile) {
            // Modalità DEV: legge la password dalle properties
            dbPassword = env.getProperty("spring.datasource.password");
            if (dbPassword == null || dbPassword.trim().isEmpty()) {
                throw new IllegalStateException("❌ ERRORE: Password database non configurata in application-dev.properties per profilo dev");
            }
            System.out.println("✅ Password DB caricata da application-dev.properties per profilo dev");
        } else {
            // Modalità DOCKER/PROD: legge dai Docker secrets
            try {
                String secretPath = "/run/secrets/db_password";
                if (!Files.exists(Paths.get(secretPath))) {
                    secretPath = "/run/secrets/db_password_dev";
                }
                
                if (!Files.exists(Paths.get(secretPath))) {
                    throw new IllegalStateException("❌ ERRORE: Docker secret per password database non trovato in " + secretPath);
                }
                
                dbPassword = new String(Files.readAllBytes(Paths.get(secretPath))).trim();
                System.out.println("✅ Password DB caricata da Docker secrets: " + secretPath);
            } catch (IOException e) {
                throw new IllegalStateException("❌ ERRORE: Impossibile leggere Docker secret per password database", e);
            }
        }
        
        DataSource dataSource = DataSourceBuilder.create()
                .url(env.getProperty("spring.datasource.url"))
                .username(env.getProperty("spring.datasource.username"))
                .password(dbPassword)
                .driverClassName(env.getProperty("spring.datasource.driverClassName"))
                .build();
        
        // Attende che il DB sia disponibile
        int attempts = 10; // Numero massimo di tentativi
        while (attempts > 0) {
            try {
                dataSource.getConnection().close();
                System.out.println("Database disponibile.");
                break;
            } catch (Exception ex) {
                attempts--;
                System.out.println("Database non ancora disponibile. Riprovo tra 5 secondi... Tentativi rimasti: " + attempts);
                try {
                    Thread.sleep(5000); // Attende 5 secondi prima di riprovare
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Thread interrotto mentre si attende il DB.", ie);
                }
            }
        }
        if (attempts == 0) {
            throw new IllegalStateException("Database non disponibile dopo molteplici tentativi.");
        }
        
        return dataSource;
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(
            "com.application.admin.persistence.model",
            "com.application.agency.persistence.model",
            "com.application.customer.persistence.model",
            "com.application.restaurant.persistence.model",
            "com.application.common.persistence.model"
        );

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());

        return em;
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", env.getProperty("spring.jpa.properties.hibernate.dialect"));
        return properties;
    }

    @Bean
    AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(authentication -> authentication.getName());
    }
}
