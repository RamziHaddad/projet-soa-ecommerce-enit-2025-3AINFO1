package com.onlineshop.order.saga.retry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "saga.retry")
public class RetryConfiguration {

    private int maxRetries = 5;
    private long maxRetryDelaySeconds = 300;

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getMaxRetryDelaySeconds() {
        return maxRetryDelaySeconds;
    }

}
