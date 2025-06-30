package com.example.payment.model;

import com.example.shared.model.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Contains information derived from a card's BIN (Bank Identification Number). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardBinInfo {
    private String bin;
    private PaymentMethod paymentMethod; // e.g., VISA, MASTERCARD
    private String cardType; // e.g., CREDIT, DEBIT, PREPAID
    private String issuer; // e.g., JPMORGAN_CHASE, BANK_OF_AMERICA
    private String issuerName; // e.g., "JPMorgan Chase", "Bank of America"
    private String countryCode; // ISO 3166-1 alpha-2 country code
    private String countryName; // Full country name
    private String productType; // e.g., "Platinum", "World Elite"
    private boolean prepaid; // Whether it's a prepaid card
    private boolean corporate; // Whether it's a corporate card
    private boolean commercial; // Whether it's a commercial card
    private String bankWebsite; // Bank's website
    private String bankPhone; // Bank's customer service phone
}
