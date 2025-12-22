package com.example.payment;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class BankSimulatorResourceTest {

    @Test
    public void testBankPaySuccess() {
        Map<String, Object> payload = Map.of(
                "paymentId", "550e8400-e29b-41d4-a716-446655440100",
                "userId", "550e8400-e29b-41d4-a716-446655440101",
                "cardNumber", "1234567890123456",
                "amount", BigDecimal.valueOf(10.00)
        );

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/bank/api/pay")
                .then()
                .statusCode(200)
                .body("status", is("SUCCESS"));
    }

    @Test
    public void testBankPayFailureByCard() {
        Map<String, Object> payload = Map.of(
                "paymentId", "550e8400-e29b-41d4-a716-446655440110",
                "userId", "550e8400-e29b-41d4-a716-446655440111",
                "cardNumber", "1234567890120000",
                "amount", BigDecimal.valueOf(10.00)
        );

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/bank/api/pay")
                .then()
                .statusCode(200)
                .body("status", is("FAILED"));
    }
}
