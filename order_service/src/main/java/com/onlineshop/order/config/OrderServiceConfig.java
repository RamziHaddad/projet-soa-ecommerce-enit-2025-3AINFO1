package com.onlineshop.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 * Configuration class for Order Service specific settings
 */
@Configuration
@Getter
public class OrderServiceConfig {

    /**
     * Time window (in hours) during which completed orders can still be cancelled
     * Default: 24 hours
     */
    @Value("${order.cancellation.completed-window-hours:24}")
    private int cancellationWindowHours;
}
