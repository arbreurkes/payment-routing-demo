package com.example.payment.api.dto;

import com.example.payment.model.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Represents the response for a payment operation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String paymentId;
    private String merchantReference;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String statusMessage;
    private LocalDateTime timestamp;

    // Payment processing details
    private String authCode;
    private String rrn; // Retrieval Reference Number

    // Routing information
    private String selectedNetwork;
    private BigDecimal routingCost;

    // Indicates whether the payment was processed using a token
    private boolean usedToken;

    public static PaymentResponse success(
            String paymentId,
            String merchantReference,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String authCode,
            String selectedNetwork,
            BigDecimal routingCost,
            String rrn,
            boolean usedToken) {
        return PaymentResponse.builder()
                .paymentId(paymentId)
                .merchantReference(merchantReference)
                .amount(amount)
                .currency(currency)
                .status(status)
                .statusMessage("Payment processed successfully")
                .timestamp(LocalDateTime.now())
                .authCode(authCode)
                .rrn(rrn)
                .selectedNetwork(selectedNetwork)
                .routingCost(routingCost)
                .usedToken(usedToken)
                .build();
    }

    public static PaymentResponse success(
            String paymentId,
            String merchantReference,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String authCode,
            String selectedNetwork,
            BigDecimal routingCost,
            String rrn) {
        return success(
                paymentId,
                merchantReference,
                amount,
                currency,
                status,
                authCode,
                selectedNetwork,
                routingCost,
                rrn,
                false);
    }

    public static PaymentResponse error(
            String paymentId, String merchantReference, String errorMessage) {
        return PaymentResponse.builder()
                .paymentId(paymentId)
                .merchantReference(merchantReference)
                .status(PaymentStatus.FAILED)
                .statusMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .usedToken(false)
                .build();
    }
}
