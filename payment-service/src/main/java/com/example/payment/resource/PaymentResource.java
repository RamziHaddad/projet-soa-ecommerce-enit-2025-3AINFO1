package com.example.payment.resource;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.service.PaymentService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

@Path("/paiement")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentResource.class);

    @Inject
    PaymentService paymentService;

    @POST
    @Blocking
    public CompletionStage<Response> processPayment(PaymentRequest request) {
        LOG.info("Received payment request for paymentId: {}", request.paymentId);

        return paymentService.processPayment(request)
                .thenApply(response -> {
                    LOG.info("Payment processed for paymentId: {} with status: {}", request.paymentId, response.status);
                    return Response.ok(response).build();
                })
                .exceptionally(throwable -> {
                    LOG.error("Error processing payment for paymentId: {}", request.paymentId, throwable);
                    PaymentResponse errorResponse = new PaymentResponse(request.paymentId, "ERROR", "Internal server error");
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
                });
    }
}
