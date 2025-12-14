package com.onlineshop.order.mock;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration spécifique pour les services mock
 * Exclut JPA et la base de données
 */
@Configuration
@Profile("mock")
public class MockConfig {
    
    // Cette classe permet d'exclure les configurations JPA en mode mock
    // Spring Boot n'essaiera pas de se connecter à la base de données
}
