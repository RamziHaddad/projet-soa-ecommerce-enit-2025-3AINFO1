package com.onlineshop.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration for database and JPA
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.onlineshop.order.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    // Additional database configuration can be added here
}
