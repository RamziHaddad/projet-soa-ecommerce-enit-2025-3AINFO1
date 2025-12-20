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
}
