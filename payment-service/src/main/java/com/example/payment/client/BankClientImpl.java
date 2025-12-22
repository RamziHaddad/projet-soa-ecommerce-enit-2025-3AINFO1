package com.example.payment.client;

import com.example.payment.dto.PaymentRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class BankClientImpl implements BankClient {
    private static final Logger LOG = LoggerFactory.getLogger(BankClientImpl.class);

    @ConfigProperty(name = "bank.api.url", defaultValue = "http://localhost:8081/bank/api/pay")
    String bankApiUrl;

    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    public boolean processPayment(PaymentRequest request) {
        try {
            String json = String.format("{\"paymentId\":\"%s\",\"userId\":\"%s\",\"cardNumber\":\"%s\",\"amount\":%s}",
                    request.paymentId, request.userId, request.cardNumber, request.amount);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(bankApiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body().contains("\"status\":\"SUCCESS\"")) {
                return true;
            }
            LOG.info("Bank returned status {} with body: {}", resp.statusCode(), resp.body());
        } catch (Exception e) {
            LOG.warn("Error calling bank API {}", bankApiUrl, e);
        }
        return false;
    }
}
