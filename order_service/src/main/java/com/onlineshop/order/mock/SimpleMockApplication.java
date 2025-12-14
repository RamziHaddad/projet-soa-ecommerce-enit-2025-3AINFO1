package com.onlineshop.order.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Application Spring Boot minimale pour les services mock
 * Sans aucune dépendance JPA ou base de données
 * Scan uniquement les packages mock pour éviter les conflits
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.onlineshop.order.mock")
@RestController
public class SimpleMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleMockApplication.class, args);
    }

    @GetMapping("/health")
    public String health() {
        return "OK - Mock Services are running";
    }

    @GetMapping("/actuator/health")
    public String actuatorHealth() {
        return "{\"status\":\"UP\"}";
    }

    // Mock endpoints for different services
    @GetMapping("/inventory/health")
    public String inventoryHealth() {
        return "{\"service\":\"inventory\",\"status\":\"UP\"}";
    }

    @GetMapping("/payment/health")
    public String paymentHealth() {
        return "{\"service\":\"payment\",\"status\":\"UP\"}";
    }

    @GetMapping("/shipping/health")
    public String shippingHealth() {
        return "{\"service\":\"shipping\",\"status\":\"UP\"}";
    }

    @GetMapping("/communication/health")
    public String communicationHealth() {
        return "{\"service\":\"communication\",\"status\":\"UP\"}";
    }
}
