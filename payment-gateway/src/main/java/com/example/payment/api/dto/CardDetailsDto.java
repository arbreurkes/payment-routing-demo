package com.example.payment.api.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

/** Contains card details for payment processing. */
@Data
public class CardDetailsDto {
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Invalid card number")
    private String cardNumber;

    @NotBlank(message = "Cardholder name is required")
    private String cardholderName;

    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Invalid expiry month")
    @Max(value = 12, message = "Invalid expiry month")
    private Integer expiryMonth;

    @NotNull(message = "Expiry year is required")
    @Min(value = 2023, message = "Card has expired")
    private Integer expiryYear;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "Invalid CVV")
    private String cvv;

    private boolean saveCard = false;
}
