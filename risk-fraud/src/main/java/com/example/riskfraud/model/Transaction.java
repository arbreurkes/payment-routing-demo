package com.example.riskfraud.model;

import com.example.shared.model.CardInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/** Represents a financial transaction that needs risk assessment. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private String transactionReference;
    private BigDecimal amount;
    private CardInfo cardInfo;
    private String merchantId;
    private String merchantName;
    private String merchantCategoryCode;
    private String userId;
    private String userEmail;
    private String userIpAddress;
    private String userDeviceId;
    private String billingAddress;
    private String shippingAddress;
    private LocalDateTime transactionTime;
    private String transactionChannel; // WEB, MOBILE, IN_STORE, etc.
    private boolean isRecurring;
    private int previousChargebacks;
    private int previousSuccessfulTransactions;
    private int previousFailedTransactions;

    /**
     * Gets the card BIN (first 6-8 digits).
     *
     * @return The card BIN or null if not available
     */
    public String getCardBin() {
        return cardInfo != null ? cardInfo.getBin() : null;
    }

    /**
     * Gets the last 4 digits of the card number.
     *
     * @return The last 4 digits or null if not available
     */
    public String getCardLastFour() {
        return cardInfo != null ? cardInfo.getLastFour() : null;
    }

    /**
     * Gets the card issuer.
     *
     * @return The card issuer or null if not available
     */
    public String getCardIssuer() {
        return cardInfo != null ? cardInfo.getIssuer() : null;
    }

    /**
     * Gets the card country.
     *
     * @return The card country or null if not available
     */
    public String getCardCountry() {
        return cardInfo != null ? cardInfo.getCountry() : null;
    }

    public boolean isCardIsDebit() {
        return cardInfo != null && cardInfo.isDebit();
    }

    public boolean isCardIsTokenized() {
        return cardInfo != null && cardInfo.isTokenized();
    }

    public Currency getCurrency() {
        return cardInfo != null ? cardInfo.getCurrency() : null;
    }
}
