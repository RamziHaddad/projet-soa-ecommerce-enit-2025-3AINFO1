package com.example.payment.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ApplicationScoped
public class NotificationClient {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationClient.class);

    private final HttpClient httpClient;

    @Inject
    @ConfigProperty(name = "services.order.notify.url", defaultValue = "")
    String orderNotifyUrl;

    public NotificationClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Send a blocking HTTP POST with JSON payload to configured notification URL.
     * Returns true if 2xx response is received.
     */
    public boolean sendNotification(String payload) {
        if (orderNotifyUrl == null || orderNotifyUrl.isBlank()) {
            LOG.debug("No order notification URL configured; skipping notification");
            return false;
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(orderNotifyUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            LOG.info("Notification sent; status={}", resp.statusCode());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (Exception e) {
            LOG.error("Failed to send notification to {}", orderNotifyUrl, e);
            return false;
        }
    }
}
