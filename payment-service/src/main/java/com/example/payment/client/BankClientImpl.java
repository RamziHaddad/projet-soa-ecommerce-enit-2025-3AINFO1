package com.example.payment.client;

import com.example.payment.dto.PaymentRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class BankClientImpl implements BankClient {

    private static final String BANK_API_BASE = "http://localhost:8082/bank/api";

    private final Client client = ClientBuilder.newClient();

    @Override
    public boolean processPayment(PaymentRequest request) {
        Response response = client.target(BANK_API_BASE + "/pay")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(Map.of(
                        "paymentId", request.paymentId(),
                        "cardNumber", request.cardNumber(),
                        "amount", request.amount()
                )));
        Map<String, Object> result = response.readEntity(Map.class);
        return "SUCCESS".equals(result.get("status"));
    }

    @Override
    public boolean refundPayment(UUID paymentId, BigDecimal amount) {
        Response response = client.target(BANK_API_BASE + "/refund")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(Map.of(
                        "paymentId", paymentId.toString(),
                        "amount", amount
                )));
        Map<String, Object> result = response.readEntity(Map.class);
        return "SUCCESS".equals(result.get("status"));
    }

    @Override
    public boolean cancelPayment(UUID paymentId) {
        Response response = client.target(BANK_API_BASE + "/cancel")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(Map.of("paymentId", paymentId.toString())));
        Map<String, Object> result = response.readEntity(Map.class);
        return "SUCCESS".equals(result.get("status"));
    }
}
