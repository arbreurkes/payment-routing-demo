package com.example.payment.model;

/** Represents the status of a payment transaction. */
public enum PaymentStatus {
    /** Payment has been created but not yet processed */
    CREATED("Created"),

    /** Payment has been authorized but not yet captured */
    AUTHORIZED("Authorized"),

    /** Payment has been captured (funds transferred) */
    CAPTURED("Captured"),

    /** Payment has been partially captured */
    PARTIALLY_CAPTURED("Partially Captured"),

    /** Payment has been fully refunded */
    REFUNDED("Refunded"),

    /** Payment has been partially refunded */
    PARTIALLY_REFUNDED("Partially Refunded"),

    /** Payment has been voided/cancelled before capture */
    CANCELLED("Cancelled"),

    /** Payment has failed */
    FAILED("Failed");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
