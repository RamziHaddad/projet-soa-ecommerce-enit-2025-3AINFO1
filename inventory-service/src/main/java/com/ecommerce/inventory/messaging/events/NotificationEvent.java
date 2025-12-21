package com.ecommerce.inventory.messaging.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    public enum Type { RESERVED, CANCELLED, CONFIRMED }
    private Type type;
    private String orderId;
    private String productId;
    private Integer quantity;
    private String message;
}
