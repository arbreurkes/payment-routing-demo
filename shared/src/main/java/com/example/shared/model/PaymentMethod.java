package com.example.shared.model;

/** Represents different payment methods supported by the system. */
public enum PaymentMethod {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("American Express"),
    DISCOVER("Discover"),
    ACCEL("Accel"),
    STAR("Star"),
    NYCE("NYCE"),
    PULSE("Pulse"),
    MAESTRO("Maestro");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the display name of the payment method.
     *
     * @return The display name of the payment method
     */
    public String getDisplayName() {
        return displayName;
    }
}
