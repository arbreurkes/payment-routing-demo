package com.example.cardnetwork.emulator;

import com.example.shared.model.PaymentMethod;

import java.util.Random;

/** Represents the result of a card authorization operation. */
public class CardAuthorizationResult {
    private final boolean success;
    private final String authCode;
    private final String errorMessage;
    private final PaymentMethod paymentMethod;
    private final String rrn; // Retrieval Reference Number (12 characters)
    private final String transactionId; // ID of the transaction in the processor

    private static final String ALLOWED_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int RRN_LENGTH = 12;
    private static final Random RANDOM = new Random();

    private CardAuthorizationResult(
            boolean success,
            String authCode,
            String errorMessage,
            PaymentMethod paymentMethod,
            String rrn,
            String transactionId) {
        this.success = success;
        this.authCode = authCode;
        this.errorMessage = errorMessage;
        this.paymentMethod = paymentMethod;
        this.rrn = rrn;
        this.transactionId = transactionId;
    }

    /**
     * Generates a random alphanumeric string of specified length.
     *
     * @param length The length of the string to generate
     * @return A random alphanumeric string
     */
    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALLOWED_CHARS.charAt(RANDOM.nextInt(ALLOWED_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Creates a successful authorization result.
     *
     * @param authCode The authorization code from the processor
     * @param paymentMethod The payment method used for authorization
     * @param transactionId The ID of the transaction in the processor
     * @return A successful CardAuthorizationResult with a generated RRN
     */
    public static CardAuthorizationResult success(
            String authCode, PaymentMethod paymentMethod, String transactionId) {
        return new CardAuthorizationResult(
                true,
                authCode,
                null,
                paymentMethod,
                generateRandomString(RRN_LENGTH),
                transactionId);
    }

    /**
     * Creates a successful authorization result.
     *
     * @param authCode The authorization code from the processor
     * @param paymentMethod The payment method used for authorization
     * @return A successful CardAuthorizationResult with a generated RRN
     */
    public static CardAuthorizationResult success(String authCode, PaymentMethod paymentMethod) {
        return success(authCode, paymentMethod, null);
    }

    /**
     * Creates a failed authorization result.
     *
     * @param errorMessage The error message describing the failure
     * @return A failed CardAuthorizationResult
     */
    public static CardAuthorizationResult failed(String errorMessage) {
        return new CardAuthorizationResult(false, null, errorMessage, null, null, null);
    }

    /**
     * Checks if the authorization was successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets the authorization code.
     *
     * @return The authorization code or null if authorization failed
     */
    public String getAuthCode() {
        return authCode;
    }

    /**
     * Gets the error message.
     *
     * @return The error message or null if authorization was successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the payment method used for authorization.
     *
     * @return The payment method or null if authorization failed
     */
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    /**
     * Gets the Retrieval Reference Number (RRN).
     *
     * @return The RRN or null if authorization failed
     */
    public String getRrn() {
        return rrn;
    }

    /**
     * Gets the transaction ID.
     *
     * @return The transaction ID or null if authorization failed
     */
    public String getTransactionId() {
        return transactionId;
    }
}
