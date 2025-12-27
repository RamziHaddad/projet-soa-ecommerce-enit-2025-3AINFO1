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
        // Configure Feign logging level for better debugging
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options requestOptions() {
        // Configure connection and read timeouts
        // connectionTimeout: 5 seconds, readTimeout: 10 seconds
        return new Request.Options(
                java.time.Duration.ofMillis(5000), // connection timeout in milliseconds
                java.time.Duration.ofMillis(10000), // read timeout in milliseconds
                true // follow redirects
        );
    }

    @Bean
    public Retryer retryer() {
        // Configure retry strategy: max 3 attempts with exponential backoff
        return new Retryer.Default(
                1000, // initial period in milliseconds
                3000, // max period in milliseconds
                3 // max attempts
        );
    }
}
