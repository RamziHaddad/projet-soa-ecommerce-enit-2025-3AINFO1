package com.ecommerce.inventory.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrdersClient {

    private final RestTemplate restTemplate;

    @Value("${orders.service.url:http://localhost:8081}")
    private String ordersServiceUrl;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getOrderItems(String orderId) {
        String url = ordersServiceUrl + "/orders/" + orderId + "/items";
        log.info("Fetching order items from {}", url);
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }
}
