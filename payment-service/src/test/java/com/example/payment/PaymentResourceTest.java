package com.example.payment;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class PaymentResourceTest {

    @Test
    public void testPaymentEndpoint() {
        PaymentRequest request = new PaymentRequest();
        request.paymentId = "550e8400-e29b-41d4-a716-446655440000";
        request.userId = "550e8400-e29b-41d4-a716-446655440001";
        request.cardNumber = "1234567890123456";
        request.amount = BigDecimal.valueOf(100.00);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/paiement")
                .then()
                .statusCode(200);
    }

    @Test
    public void testGetPaymentStatus() {
        // First create a payment
        PaymentRequest request = new PaymentRequest();
        request.paymentId = "550e8400-e29b-41d4-a716-446655440002";
        request.userId = "550e8400-e29b-41d4-a716-446655440003";
        request.cardNumber = "1234567890123456";
        request.amount = BigDecimal.valueOf(50.00);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/paiement")
                .then()
                .statusCode(200);

        // Now test getting the status
        given()
                .when().get("/paiement/550e8400-e29b-41d4-a716-446655440002")
                .then()
                .statusCode(200)
                .body("paymentId", is("550e8400-e29b-41d4-a716-446655440002"));
    }

    @Test
    public void testGetPaymentStatusNotFound() {
        given()
                .when().get("/paiement/550e8400-e29b-41d4-a716-446655440999")
                .then()
                .statusCode(404)
                .body("status", is("NOT_FOUND"));
    }

    @Test
    public void testGetPaymentStatusInvalidId() {
        given()
                .when().get("/paiement/invalid-uuid")
                .then()
                .statusCode(400)
                .body("status", is("ERROR"));
    }
}
