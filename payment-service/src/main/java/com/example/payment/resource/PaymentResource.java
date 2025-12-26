package com.example.payment.resource;

import com.example.payment.dto.PaymentDetails;
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

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

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

        try {
            PaymentResponse response = paymentService.processPayment(request);
            LOG.info("Payment processed for paymentId: {} with status: {}", request.paymentId, response.status);
            return CompletableFuture.completedFuture(Response.ok(response).build());
        } catch (Exception e) {
            LOG.error("Error processing payment for paymentId: {}", request.paymentId, e);
            PaymentResponse errorResponse = new PaymentResponse(request.paymentId, "ERROR", "Internal server error");
            return CompletableFuture.completedFuture(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build());
        }
    }

    @GET
    @Path("/{paymentId}")
    public Response getPaymentStatus(@PathParam("paymentId") String paymentId) {
        LOG.info("Received request to get payment status for paymentId: {}", paymentId);

        try {
            UUID paymentUUID = UUID.fromString(paymentId);
            PaymentResponse response = paymentService.getPaymentStatus(paymentUUID);

            if (response != null) {
                LOG.info("Payment status retrieved for paymentId: {} - status: {}", paymentId, response.status);
                return Response.ok(response).build();
            } else {
                LOG.warn("Payment not found for paymentId: {}", paymentId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new PaymentResponse(paymentId, "NOT_FOUND", "Payment not found"))
                    .build();
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid paymentId format: {}", paymentId, e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new PaymentResponse(paymentId, "ERROR", "Invalid payment ID format"))
                .build();
        } catch (Exception e) {
            LOG.error("Error retrieving payment status for paymentId: {}", paymentId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new PaymentResponse(paymentId, "ERROR", "Internal server error"))
                .build();
        }
    }

    @GET
    @Path("/user/{userId}")
    public Response getPaymentsByUser(@PathParam("userId") String userId) {
        LOG.info("Received request to get payments for userId: {}", userId);

        try {
            UUID userUUID = UUID.fromString(userId);
            var payments = paymentService.getPaymentsByUser(userUUID);
            LOG.info("Retrieved {} payments for userId: {}", payments.size(), userId);
            return Response.ok(payments).build();
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid userId format: {}", userId, e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid user ID format")
                .build();
        } catch (Exception e) {
            LOG.error("Error retrieving payments for userId: {}", userId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Internal server error")
                .build();
        }
    }

    @GET
    @Path("/{paymentId}/details")
    public Response getPaymentDetails(@PathParam("paymentId") String paymentId) {
        LOG.info("Received request to get payment details for paymentId: {}", paymentId);

        try {
            UUID paymentUUID = UUID.fromString(paymentId);
            var details = paymentService.getPaymentDetails(paymentUUID);

            if (details != null) {
                LOG.info("Payment details retrieved for paymentId: {}", paymentId);
                return Response.ok(details).build();
            } else {
                LOG.warn("Payment not found for paymentId: {}", paymentId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new PaymentResponse(paymentId, "NOT_FOUND", "Payment not found"))
                    .build();
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid paymentId format: {}", paymentId, e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new PaymentResponse(paymentId, "ERROR", "Invalid payment ID format"))
                .build();
        } catch (Exception e) {
            LOG.error("Error retrieving payment details for paymentId: {}", paymentId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new PaymentResponse(paymentId, "ERROR", "Internal server error"))
                .build();
        }
    }

    @PUT
    @Path("/{paymentId}/cancel")
    public Response cancelPayment(@PathParam("paymentId") String paymentId) {
        LOG.info("Received request to cancel payment for paymentId: {}", paymentId);

        try {
            UUID paymentUUID = UUID.fromString(paymentId);
            PaymentResponse response = paymentService.cancelPayment(paymentUUID);

            if (response != null) {
                LOG.info("Payment cancellation processed for paymentId: {} - status: {}", paymentId, response.status);
                return Response.ok(response).build();
            } else {
                LOG.warn("Payment not found for paymentId: {}", paymentId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new PaymentResponse(paymentId, "NOT_FOUND", "Payment not found"))
                    .build();
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid paymentId format: {}", paymentId, e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new PaymentResponse(paymentId, "ERROR", "Invalid payment ID format"))
                .build();
        } catch (Exception e) {
            LOG.error("Error cancelling payment for paymentId: {}", paymentId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new PaymentResponse(paymentId, "ERROR", "Internal server error"))
                .build();
        }
    }

    @POST
    @Path("/webhook")
    public Response handleWebhook(String payload) {
        LOG.info("Received webhook payload: {}", payload);

        try {
            String result = paymentService.processWebhook(payload);
            LOG.info("Webhook processed successfully");
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.error("Error processing webhook", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Webhook processing failed")
                .build();
        }
    }
}
