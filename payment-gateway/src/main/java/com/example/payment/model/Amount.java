package com.example.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Represents a monetary amount with currency. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Amount {
    private BigDecimal value;
    private String currency;

    /**
     * Creates a new Amount with the specified value and currency.
     *
     * @param value the monetary value
     * @param currency the currency code (ISO 4217)
     * @return a new Amount instance
     * @throws IllegalArgumentException if value is null or negative, or currency is null/blank
     */
    public static Amount of(BigDecimal value, String currency) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount value must be non-null and non-negative");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code must not be null or empty");
        }

        return Amount.builder().value(value).currency(currency.trim().toUpperCase()).build();
    }

    /**
     * Adds the specified amount to this amount.
     *
     * @param other the amount to add
     * @return a new Amount representing the sum
     * @throws IllegalArgumentException if the currencies don't match
     */
    public Amount add(Amount other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return Amount.of(this.value.add(other.value), this.currency);
    }

    /**
     * Subtracts the specified amount from this amount.
     *
     * @param other the amount to subtract
     * @return a new Amount representing the difference
     * @throws IllegalArgumentException if the currencies don't match or result would be negative
     */
    public Amount subtract(Amount other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract amounts with different currencies");
        }
        BigDecimal newValue = this.value.subtract(other.value);
        if (newValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Resulting amount cannot be negative");
        }
        return Amount.of(newValue, this.currency);
    }
}
