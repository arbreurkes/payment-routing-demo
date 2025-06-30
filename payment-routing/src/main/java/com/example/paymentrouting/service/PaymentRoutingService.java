package com.example.paymentrouting.service;

import com.example.paymentrouting.model.RoutingResult;
import com.example.shared.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.Set;

/** Service responsible for determining the optimal payment network for a transaction. */
public interface PaymentRoutingService {

    /**
     * Finds the optimal payment network for a transaction.
     *
     * @param amount The transaction amount
     * @param currency The transaction currency
     * @param availableNetworks Set of available payment networks for the card
     * @return RoutingResult containing the selected network and cost information
     */
    RoutingResult findOptimalNetwork(
            BigDecimal amount, String currency, Set<PaymentMethod> availableNetworks);

    /**
     * Gets the estimated cost for using a specific payment network.
     *
     * @param paymentMethod The payment method to get the cost for
     * @param amount The transaction amount
     * @param currency The transaction currency
     * @return The estimated cost
     */
    BigDecimal getNetworkCost(PaymentMethod paymentMethod, BigDecimal amount, String currency);

    /**
     * Returns a default routing result when no valid options are found. Uses VISA as the default
     * payment method.
     *
     * @param amount The transaction amount
     * @param currency The transaction currency
     * @return A default routing result
     */
    RoutingResult getDefaultRoutingResult(BigDecimal amount, String currency);
}
