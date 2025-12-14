package com.onlineshop.order.mock.controller;

import com.onlineshop.order.mock.dto.CommunicationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mock controller pour le service Communication
 * Simule les réponses du service de notifications
 */
@RestController
@RequestMapping("/api/communication")
@RequiredArgsConstructor
@Slf4j
public class CommunicationMockController {
    
    @PostMapping("/send-email")
    public ResponseEntity<CommunicationResponse> sendEmail(@RequestBody EmailRequest request) {
        log.info("Mock Communication Service - Sending email to: {}", request.getRecipient());
        
        // Simulation d'envoi d'email (95% de succès)
        boolean sendSuccess = Math.random() > 0.05;
        
        CommunicationResponse response = CommunicationResponse.builder()
                .success(sendSuccess)
                .message(sendSuccess ? "Email envoyé avec succès" : "Échec de l'envoi d'email")
                .notificationId(sendSuccess ? "EMAIL-" + UUID.randomUUID().toString().substring(0, 8) : null)
                .recipient(request.getRecipient())
                .channel(CommunicationResponse.CommunicationChannel.EMAIL.name())
                .sentAt(sendSuccess ? LocalDateTime.now() : null)
                .status(sendSuccess ? CommunicationResponse.NotificationStatus.SENT.name() : CommunicationResponse.NotificationStatus.FAILED.name())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/send-sms")
    public ResponseEntity<CommunicationResponse> sendSms(@RequestBody SmsRequest request) {
        log.info("Mock Communication Service - Sending SMS to: {}", request.getRecipient());
        
        // Simulation d'envoi de SMS (90% de succès)
        boolean sendSuccess = Math.random() > 0.10;
        
        CommunicationResponse response = CommunicationResponse.builder()
                .success(sendSuccess)
                .message(sendSuccess ? "SMS envoyé avec succès" : "Échec de l'envoi de SMS")
                .notificationId(sendSuccess ? "SMS-" + UUID.randomUUID().toString().substring(0, 8) : null)
                .recipient(request.getRecipient())
                .channel(CommunicationResponse.CommunicationChannel.SMS.name())
                .sentAt(sendSuccess ? LocalDateTime.now() : null)
                .status(sendSuccess ? CommunicationResponse.NotificationStatus.SENT.name() : CommunicationResponse.NotificationStatus.FAILED.name())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/send-notification")
    public ResponseEntity<CommunicationResponse> sendPushNotification(@RequestBody PushNotificationRequest request) {
        log.info("Mock Communication Service - Sending push notification to: {}", request.getRecipient());
        
        // Simulation d'envoi de notification push (85% de succès)
        boolean sendSuccess = Math.random() > 0.15;
        
        CommunicationResponse response = CommunicationResponse.builder()
                .success(sendSuccess)
                .message(sendSuccess ? "Notification push envoyée" : "Échec de l'envoi de notification")
                .notificationId(sendSuccess ? "PUSH-" + UUID.randomUUID().toString().substring(0, 8) : null)
                .recipient(request.getRecipient())
                .channel(CommunicationResponse.CommunicationChannel.PUSH_NOTIFICATION.name())
                .sentAt(sendSuccess ? LocalDateTime.now() : null)
                .status(sendSuccess ? CommunicationResponse.NotificationStatus.SENT.name() : CommunicationResponse.NotificationStatus.FAILED.name())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{notificationId}")
    public ResponseEntity<CommunicationResponse> getNotificationStatus(@PathVariable String notificationId) {
        log.info("Mock Communication Service - Getting status for notification: {}", notificationId);
        
        // Simulation de récupération du statut
        CommunicationResponse response = CommunicationResponse.builder()
                .success(true)
                .message("Statut récupéré")
                .notificationId(notificationId)
                .recipient("customer@example.com")
                .channel(CommunicationResponse.CommunicationChannel.EMAIL.name())
                .sentAt(LocalDateTime.now().minusMinutes(5))
                .status(CommunicationResponse.NotificationStatus.DELIVERED.name())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/bulk-send")
    public ResponseEntity<CommunicationResponse> sendBulkNotifications(@RequestBody BulkNotificationRequest request) {
        log.info("Mock Communication Service - Sending bulk notifications to {} recipients", request.getRecipients().size());
        
        // Simulation d'envoi en masse (taux de succès global)
        boolean bulkSuccess = Math.random() > 0.20; // 80% de succès global
        
        int successCount = request.getRecipients().size() - (int)(request.getRecipients().size() * 0.1);
        
        CommunicationResponse response = CommunicationResponse.builder()
                .success(bulkSuccess)
                .message(bulkSuccess ? 
                    String.format("Notifications envoyées avec succès (%d/%d)", successCount, request.getRecipients().size()) :
                    "Échec de l'envoi en masse")
                .notificationId(bulkSuccess ? "BULK-" + UUID.randomUUID().toString().substring(0, 8) : null)
                .recipient("bulk-notification")
                .channel(request.getChannel())
                .sentAt(bulkSuccess ? LocalDateTime.now() : null)
                .status(bulkSuccess ? CommunicationResponse.NotificationStatus.SENT.name() : CommunicationResponse.NotificationStatus.FAILED.name())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    // Classes internes pour les requêtes
    public static class EmailRequest {
        private String recipient;
        private String subject;
        private String body;
        private String templateId;
        
        // Getters et setters
        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
    }
    
    public static class SmsRequest {
        private String recipient;
        private String message;
        private String senderId;
        
        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
    }
    
    public static class PushNotificationRequest {
        private String recipient;
        private String title;
        private String message;
        private String data;
        
        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }
    
    public static class BulkNotificationRequest {
        private String channel;
        private String subject;
        private String message;
        private java.util.List<String> recipients;
        
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public java.util.List<String> getRecipients() { return recipients; }
        public void setRecipients(java.util.List<String> recipients) { this.recipients = recipients; }
    }
}
