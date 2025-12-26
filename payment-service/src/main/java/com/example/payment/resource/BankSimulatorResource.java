package com.example.payment.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/bank/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BankSimulatorResource {

    private static final Logger LOG = LoggerFactory.getLogger(BankSimulatorResource.class);

    @ConfigProperty(name = "payment.webhook.url", defaultValue = "http://localhost:8082/paiement/webhook")
    String paymentWebhookUrl;

    // -------------------------------
    // PAY
    // -------------------------------
    @POST
    @Path("/pay")
    public Response pay(Map<String, Object> payload) {
        LOG.info("BankSimulator received payment payload: {}", payload);

        String cardNumber = payload.getOrDefault("cardNumber", "").toString();
        BigDecimal amount = new BigDecimal(payload.getOrDefault("amount", "0").toString());

        boolean success = !cardNumber.endsWith("0000") && amount.compareTo(new BigDecimal("10000")) <= 0
                && Math.random() > 0.2; // 80% success

        return Response.ok(Map.of("status", success ? "SUCCESS" : "FAILED")).build();
    }

    // -------------------------------
    // REFUND
    // -------------------------------
    @POST
    @Path("/refund")
    public Response refund(Map<String, Object> payload) {
        LOG.info("BankSimulator received refund payload: {}", payload);

        String paymentId = payload.getOrDefault("paymentId", "").toString();
        BigDecimal amount = new BigDecimal(payload.getOrDefault("amount", "0").toString());

        // Example rule: refund succeeds if amount > 0
        boolean success = amount.compareTo(BigDecimal.ZERO) > 0;

        return Response.ok(Map.of("status", success ? "SUCCESS" : "FAILED")).build();
    }

    // -------------------------------
    // CANCEL
    // -------------------------------
    @POST
    @Path("/cancel")
    public Response cancel(Map<String, Object> payload) {
        LOG.info("BankSimulator received cancel request: {}", payload);

        String paymentId = payload.getOrDefault("paymentId", "").toString();

        // Example rule: always allow cancellation if paymentId is not empty
        boolean success = !paymentId.isEmpty();

        return Response.ok(Map.of("status", success ? "SUCCESS" : "FAILED")).build();
    }
}
