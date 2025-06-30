package com.example.payment.model;

import com.example.shared.model.CardInfo;
import com.example.shared.model.PaymentMethod;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Represents a payment transaction in the system. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private String id;
    private String merchantId;
    private String merchantReference;
    private Amount amount;
    private PaymentStatus status;
    private String authCode;
    private String rrn; // Retrieval Reference Number
    private CardInfo cardInfo;
    private PaymentMethod selectedNetwork;
    private BigDecimal routingCost;
    private Double riskScore;
    private com.example.riskfraud.model.RiskLevel riskLevel;
    private String tokenReference; // Reference to the card token used for this payment
    private String transactionId; // Reference to the transaction ID from the card processor
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Creates a new payment with generated ID and timestamps */
    public static Payment createNew(String merchantId, String merchantReference, Amount amount) {
        LocalDateTime now = LocalDateTime.now();
        return Payment.builder()
                .id(generatePaymentId())
                .merchantId(merchantId)
                .merchantReference(merchantReference)
                .amount(amount)
                .status(PaymentStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /** Generates a unique payment ID */
    private static String generatePaymentId() {
        return "PMT" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Updates the payment status and sets the updated timestamp */
    public void updateStatus(PaymentStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /** Updates the authorization details */
    public void authorize(String authCode, String rrn) {
        this.authCode = authCode;
        this.rrn = rrn;
        this.status = PaymentStatus.AUTHORIZED;
        this.updatedAt = LocalDateTime.now();
    }
}
