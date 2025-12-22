package com.onlineshop.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.Request;
import feign.Retryer;

/**
 * Configuration for Feign clients
 */
@Configuration
public class FeignConfig {
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        // Default to BASIC logging
        return Logger.Level.BASIC;
    }
    
    @Bean
    public Request.Options requestOptions() {
        // Connection timeout 5s, read timeout 15s
        return new Request.Options(5000, 15000);
    }
    
    @Bean
    public Retryer retryer() {
        // Default retryer: 1s initial interval, 2s max interval, 3 attempts
        return new Retryer.Default(1000, 2000, 3);
    }
}
