package com.example.paymentrouting.model;

import com.example.shared.model.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Represents the cost of routing a transaction through a specific payment network. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingCost {
    /** The payment method (network) being used. */
    private PaymentMethod paymentMethod;

    /** The fixed fee per transaction in the transaction currency. */
    private BigDecimal fixedFee;

    /** The percentage fee per transaction (e.g., 0.015 for 1.5%). */
    private BigDecimal percentageFee;

    /**
     * The base authorization success rate (0.0 to 1.0). This represents the probability that a
     * transaction will be approved.
     */
    private double authorizationRate;

    /** Indicates whether this cost structure is for a tokenized card (true) or PAN (false). */
    private boolean isToken;

    /**
     * Gets the authorization success rate for this payment method.
     *
     * @return The authorization rate between 0.0 and 1.0
     */
    public double getAuthorizationRate() {
        return authorizationRate;
    }

    /**
     * The estimated total cost for a given transaction amount.
     *
     * @param amount The transaction amount
     * @return The total estimated cost
     */
    public BigDecimal calculateCost(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percentageAmount = amount.multiply(percentageFee);
        return fixedFee.add(percentageAmount);
    }

    /**
     * Gets the expected cost adjusted for authorization rate. This gives a more accurate cost
     * estimate by considering the likelihood of success.
     *
     * @param amount The transaction amount
     * @return The expected cost considering authorization rate
     */
    public BigDecimal calculateExpectedCost(BigDecimal amount) {
        BigDecimal baseCost = calculateCost(amount);
        if (authorizationRate <= 0) {
            return baseCost;
        }
        // Scale the cost by the inverse of the authorization rate
        // Lower auth rate means higher effective cost
        double scale = 1.0 / Math.max(0.01, authorizationRate);
        return baseCost.multiply(BigDecimal.valueOf(scale));
    }
}
