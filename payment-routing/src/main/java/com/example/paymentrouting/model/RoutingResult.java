package com.example.paymentrouting.model;

import com.example.shared.model.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/** Represents the result of finding the optimal payment network for a transaction. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingResult {
    /** The selected payment method for routing. */
    private PaymentMethod selectedPaymentMethod;

    /** The estimated cost of the selected routing option. */
    private BigDecimal estimatedCost;

    /** The transaction amount this routing is for. */
    private BigDecimal transactionAmount;

    /** The currency of the transaction. */
    private String currency;

    /** Whether to use a token (true) or PAN (false) for this transaction. */
    private boolean useToken;

    /** All available routing options with their estimated costs. */
    private Map<PaymentMethod, BigDecimal> allOptions;

    /**
     * Creates a routing result indicating no valid routing options were found.
     *
     * @param amount The transaction amount (can be null)
     * @param currency The transaction currency (can be null)
     * @return A RoutingResult with no selected payment method
     */
    public static RoutingResult noValidOptions(BigDecimal amount, String currency) {
        return RoutingResult.builder()
                .selectedPaymentMethod(null)
                .estimatedCost(null)
                .transactionAmount(amount)
                .currency(currency)
                .useToken(false)
                .allOptions(Map.of())
                .build();
    }

    /**
     * Checks if a valid routing option was found.
     *
     * @return true if a valid routing option exists, false otherwise
     */
    public boolean hasValidOption() {
        return selectedPaymentMethod != null;
    }
}
