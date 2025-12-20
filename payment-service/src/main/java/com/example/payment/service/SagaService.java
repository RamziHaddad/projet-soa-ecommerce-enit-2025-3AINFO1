package com.example.payment.service;

import com.example.payment.entity.Paiement;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SagaService {

    private static final Logger LOG = LoggerFactory.getLogger(SagaService.class);

    public void startPaymentSaga(Paiement paiement) {
        LOG.info("Starting payment saga for paymentId: {}", paiement.paymentId);

        // Step 1: Validation
        if (!executeStep(paiement, "VALIDATE")) {
            compensate(paiement);
            return;
        }

        // Step 2: Processing
        if (!executeStep(paiement, "PROCESS")) {
            compensate(paiement);
            return;
        }

        // Step 3: Completion
        executeStep(paiement, "COMPLETE");
    }

    private boolean executeStep(Paiement paiement, String step) {
        LOG.info("Executing saga step: {} for paymentId: {}", step, paiement.paymentId);
        // Simulate step execution
        // In real scenario, call other services or perform actions
        return paiement.status.equals("SUCCESS") || step.equals("VALIDATE"); // Allow validate to pass
    }

    private void compensate(Paiement paiement) {
        LOG.warn("Compensating saga for paymentId: {}", paiement.paymentId);
        // Perform rollback actions
        paiement.status = "FAILED";
        paiement.nextStep = "COMPENSATED";
        paiement.persist();
    }
}
