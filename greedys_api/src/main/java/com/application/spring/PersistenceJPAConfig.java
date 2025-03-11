package com.application.spring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableJpaRepositories(basePackages = "com.application.persistence.dao")
@EnableTransactionManagement
public class PersistenceJPAConfig {

    private final Environment env;

    public PersistenceJPAConfig(Environment env) {
        this.env = env;
    }

    @Bean
    DataSource dataSource() {
        String dbPassword = "";
        try {
            dbPassword = new String(Files.readAllBytes(Paths.get("/run/secrets/db_password"))).trim();
        } catch (IOException e) {
            e.printStackTrace();
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
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.application.persistence.model");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());

        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
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
}
