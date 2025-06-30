package com.example.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

/** Represents the details of a payment card. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDetails {
    private String cardNumber;
    private String cardholderName;
    private int expiryMonth;
    private int expiryYear;
    private String cvv;

    /**
     * Validates that the card is not expired.
     *
     * @return true if the card is not expired, false otherwise
     */
    public boolean isNotExpired() {
        YearMonth expiryDate = YearMonth.of(expiryYear, expiryMonth);
        YearMonth currentDate = YearMonth.now();
        return !expiryDate.isBefore(currentDate);
    }

    /**
     * Gets the card's BIN (first 6 digits).
     *
     * @return The BIN or empty string if card number is too short
     */
    public String getBin() {
        return cardNumber != null && cardNumber.length() >= 6 ? cardNumber.substring(0, 6) : "";
    }

    /**
     * Gets the last 4 digits of the card number.
     *
     * @return The last 4 digits or empty string if card number is too short
     */
    public String getLastFour() {
        return cardNumber != null && cardNumber.length() >= 4
                ? cardNumber.substring(cardNumber.length() - 4)
                : "";
    }
}
