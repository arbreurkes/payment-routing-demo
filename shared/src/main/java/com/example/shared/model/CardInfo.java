package com.example.shared.model;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Currency;

/** Represents payment card information that can be used across different modules. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardInfo {
    /** The payment method (e.g., VISA, MASTERCARD, STAR, etc.) */
    @NotNull private PaymentMethod paymentMethod;

    /**
     * The first 6-8 digits of the card number (BIN). For debit networks, this might be the routing
     * number or other identifier.
     */
    private String bin;

    /** The last 4 digits of the card number. */
    private String lastFour;

    /** The card issuer (e.g., Chase, BofA, etc.). */
    private String issuer;

    /** The country where the card was issued (ISO 3166-1 alpha-2). */
    private String country;

    /** The country code (ISO 3166-1 alpha-2) associated with the card. */
    private String countryCode;

    /** Indicates if the card is a debit card. */
    private boolean debit;

    /** Indicates if the card is tokenized. */
    private boolean tokenized;

    /** The card's currency. */
    private Currency currency;

    /** The card's expiration month (1-12). */
    private Integer expiryMonth;

    /** The card's expiration year (4 digits). */
    private Integer expiryYear;

    /** The cardholder's name. */
    private String cardholderName;

    /** Indicates if the card is a prepaid card. */
    private boolean prepaid;

    /** Indicates if the card is a corporate card. */
    private boolean corporate;

    /** Indicates if the card is a commercial card. */
    private boolean commercial;

    /** The card's product type. */
    private String productType;

    /** The card's card type (e.g., CREDIT, DEBIT, PREPAID). */
    private String cardType;
}
