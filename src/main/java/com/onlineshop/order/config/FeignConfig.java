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
        // TODO: Configure Feign logging level
        return null;
    }
    
    @Bean
    public Request.Options requestOptions() {
        // TODO: Configure connection and read timeouts
        return null;
    }
    
    @Bean
    public Retryer retryer() {
        // TODO: Configure retry strategy
        return null;
    }
}
