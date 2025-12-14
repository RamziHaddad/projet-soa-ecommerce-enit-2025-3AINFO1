package com.onlineshop.order.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Application Spring Boot minimale pour les services mock
 * Sans aucune dépendance JPA ou base de données
 */
@SpringBootApplication
@RestController
public class MockServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockServicesApplication.class, args);
    }

    @GetMapping("/health")
    public String health() {
        return "OK - Mock Services are running";
    }

    @GetMapping("/actuator/health")
    public String actuatorHealth() {
        return "{\"status\":\"UP\"}";
    }
}
