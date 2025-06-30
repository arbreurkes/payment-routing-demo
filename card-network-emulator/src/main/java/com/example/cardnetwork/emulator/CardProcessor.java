package com.example.cardnetwork.emulator;

import com.example.shared.model.CardDetails;
import com.example.shared.model.PaymentMethod;

import java.math.BigDecimal;

/** Interface for processing card payments through different card networks. */
public interface CardProcessor {

    /**
     * Authorizes a payment with the card network.
     *
     * @param cardDetails The card details for the transaction
     * @param amount The amount to authorize
     * @param currency The currency of the transaction
     * @param paymentMethod The payment method to use for authorization
     * @return A CardAuthorizationResult indicating success or failure
     */
    CardAuthorizationResult authorize(
            CardDetails cardDetails,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod);

    /**
     * Captures a previously authorized payment.
     *
     * @param transactionId The ID of the transaction to capture
     * @param amount The amount to capture
     * @param currency The currency of the transaction
     * @return true if the capture was successful, false otherwise
     */
    boolean capture(String transactionId, BigDecimal amount, String currency);

    /**
     * Voids a previously authorized payment.
     *
     * @param transactionId The ID of the transaction to void
     * @return true if the void was successful, false otherwise
     */
    boolean voidAuthorization(String transactionId);

    /**
     * Refunds a previously captured payment.
     *
     * @param transactionId The ID of the transaction to refund
     * @param amount The amount to refund
     * @param currency The currency of the transaction
     * @return true if the refund was successful, false otherwise
     */
    boolean refund(String transactionId, BigDecimal amount, String currency);
}
