package com.example.payment.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.Data;

import java.math.BigDecimal;

/** Represents a request to process a card payment. */
@Data
public class CardPaymentRequest {
    @NotBlank(message = "Merchant reference is required")
    private String merchantReference;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    private String currency;

    @Valid private CardDetailsDto cardDetails;

    private String tokenReference;

    /**
     * Validates that either card details or token reference is provided, but not both. This is used
     * by the controller to validate the request before processing.
     *
     * @return true if the request is valid, false otherwise
     */
    public boolean isValid() {
        return (cardDetails != null && tokenReference == null)
                || (cardDetails == null && tokenReference != null);
    }

    /**
     * Checks if this request contains card details.
     *
     * @return true if card details are present
     */
    public boolean hasCardDetails() {
        return cardDetails != null;
    }

    /**
     * Checks if this request contains a token reference.
     *
     * @return true if token reference is present
     */
    public boolean hasTokenReference() {
        return tokenReference != null && !tokenReference.isEmpty();
    }

    // Additional payment details can be added here (e.g., billing address, customer info)
}
