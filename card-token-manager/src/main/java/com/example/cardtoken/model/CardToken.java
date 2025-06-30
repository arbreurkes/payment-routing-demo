package com.example.cardtoken.model;

import com.example.shared.model.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** Represents a tokenized card in the system. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardToken {
    /** Unique token reference ID */
    private String tokenReference;

    /** The payment networks that can use this token */
    @Builder.Default private Set<PaymentMethod> paymentMethods = new HashSet<>();

    /** The token value that replaces the PAN (Primary Account Number) */
    private String tokenValue;

    /** The BIN (first 6 digits) of the token */
    private String tokenBin;

    /** The last 4 digits of the original card number (preserved for display purposes) */
    private String lastFour;

    /** The expiration month of the token */
    private int expiryMonth;

    /** The expiration year of the token */
    private int expiryYear;

    /**
     * The original PAN (encrypted or masked, depending on implementation) In a real system, this
     * would be securely encrypted
     */
    private String maskedPan;

    /** When the token was created */
    private LocalDateTime createdAt;

    /** When the token expires */
    private LocalDateTime expiresAt;

    /** Status of the token (ACTIVE, SUSPENDED, EXPIRED) */
    private TokenStatus status;

    /**
     * Checks if the token is active
     *
     * @return true if the token is active and not expired
     */
    public boolean isActive() {
        return status == TokenStatus.ACTIVE
                && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }

    /**
     * Adds a payment method to this token
     *
     * @param paymentMethod The payment method to add
     * @return this token for method chaining
     */
    public CardToken addPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethods == null) {
            paymentMethods = new HashSet<>();
        }
        paymentMethods.add(paymentMethod);
        return this;
    }

    /**
     * Checks if this token supports the specified payment method
     *
     * @param paymentMethod The payment method to check
     * @return true if this token supports the payment method
     */
    public boolean supportsPaymentMethod(PaymentMethod paymentMethod) {
        return paymentMethods != null && paymentMethods.contains(paymentMethod);
    }
}
