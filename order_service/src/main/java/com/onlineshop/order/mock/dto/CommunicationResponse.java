package com.onlineshop.order.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de réponse du service Communication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationResponse {
    
    private boolean success;
    private String message;
    private String notificationId;
    private String recipient;
    private String channel; // EMAIL, SMS, PUSH
    private LocalDateTime sentAt;
    private String status;
    
    public enum CommunicationChannel {
        EMAIL, SMS, PUSH_NOTIFICATION
    }
    
    public enum NotificationStatus {
        PENDING, SENT, DELIVERED, FAILED
    }
}
