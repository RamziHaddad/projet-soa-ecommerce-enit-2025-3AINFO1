package com.example.delivery.service;

import com.example.delivery.dto.DeliveryRequest;
import com.example.delivery.dto.DeliveryResponse;
import com.example.delivery.model.Delivery;
import com.example.delivery.repository.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public DeliveryResponse createDelivery(DeliveryRequest request) {
        // si une livraison existe déjà pour l'orderId, on peut la retourner au lieu de créer une nouvelle
        Optional<Delivery> existing = deliveryRepository.findByOrderId(request.getOrderId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        Delivery d = new Delivery();
        d.setOrderId(request.getOrderId());
		d.setCustomerId(request.getCustomerId());
        d.setAddress(request.getAddress());
        d.setStatus("PENDING");
        d.setTrackingNumber(generateTrackingNumber());
        d.setCreatedAt(LocalDateTime.now());

        Delivery saved = deliveryRepository.save(d);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getByOrderId(Long orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public DeliveryResponse updateStatus(Long id, String status) {
        Delivery d = deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + id));
        d.setStatus(status);
        Delivery saved = deliveryRepository.save(d);
        return toResponse(saved);
    }

	public boolean cancelDelivery(Long id) {
		Delivery delivery = deliveryRepository.findById(id).orElse(null);
		if (delivery == null) return false;
		delivery.setStatus("CANCELLED");
		deliveryRepository.save(delivery);
		return true;
	}


    private DeliveryResponse toResponse(Delivery d) {
        DeliveryResponse r = new DeliveryResponse();
        r.setDeliveryId(d.getId());
        r.setOrderId(d.getOrderId());
		r.setCustomerId(d.getCustomerId());
        r.setStatus(d.getStatus());
        r.setTrackingNumber(d.getTrackingNumber());
        r.setCreatedAt(d.getCreatedAt());
        return r;
    }

    private String generateTrackingNumber() {
        // simple génération : TRK-<UUID short>
        return "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
