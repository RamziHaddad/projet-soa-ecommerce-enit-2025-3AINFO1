package com.example.cart.client;

import com.example.cart.dto.OrderRequest;
import com.example.cart.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service", url = "${orderservice.url}")
public interface OrderClient {

    @PostMapping("/api/orders")
    OrderResponse createOrder(@RequestBody OrderRequest orderRequest);
}
