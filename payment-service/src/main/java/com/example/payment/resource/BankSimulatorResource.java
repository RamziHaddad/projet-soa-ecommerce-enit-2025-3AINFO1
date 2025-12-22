package com.example.payment.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/bank/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BankSimulatorResource {
    private static final Logger LOG = LoggerFactory.getLogger(BankSimulatorResource.class);

    @ConfigProperty(name = "payment.webhook.url", defaultValue = "http://localhost:8082/paiement/webhook")
    String paymentWebhookUrl;

    /**
     * Simulates an external bank payment API.
     * Rules:
     * - If cardNumber ends with "0000" => FAILED
     * - If amount > 10000 => FAILED
     * - Otherwise SUCCESS (80% probability)
     * Also posts a webhook to the payment service webhook endpoint asynchronously to simulate callbacks.
     */
    @POST
    @Path("/pay")
    public Response pay(Map<String, Object> payload) {
        LOG.info("BankSimulator received payload: {}", payload);

        String cardNumber = payload.getOrDefault("cardNumber", "").toString();
        BigDecimal amount = new BigDecimal(payload.getOrDefault("amount", "0").toString());

        boolean success;
        if (cardNumber.endsWith("0000") || amount.compareTo(new BigDecimal("10000")) > 0) {
            success = false;
        } else {
            success = Math.random() > 0.2; // 80% success
        }

        String status = success ? "SUCCESS" : "FAILED";

        // Fire-and-forget webhook to payment service to simulate external callback
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String webhookPayload = String.format("{\"paymentId\":\"%s\",\"status\":\"%s\"}",
                        payload.getOrDefault("paymentId", ""), status);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(paymentWebhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(webhookPayload))
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                LOG.info("Webhook POST returned {}: {}", resp.statusCode(), resp.body());
            } catch (Exception e) {
                LOG.warn("Failed to send webhook", e);
            }
        });

        return Response.ok(Map.of("status", status)).build();
    }
}
