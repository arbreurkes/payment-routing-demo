package com.example.paymentrouting.service.impl;

import com.example.paymentrouting.model.RoutingCost;
import com.example.paymentrouting.model.RoutingResult;
import com.example.paymentrouting.service.PaymentRoutingService;
import com.example.shared.model.PaymentMethod;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the PaymentRoutingService that selects the most cost-effective payment network
 * based on configured fee structures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRoutingServiceImpl implements PaymentRoutingService {

    // Default fee structures for different payment methods
    // In a real application, these would come from a configuration or database
    private static final Map<String, RoutingCost> DEFAULT_FEES = new HashMap<>();

    static {
        // Initialize default fee structures with authorization rates
        // Signature networks (Visa, MC, Amex, Discover) have higher auth rates but higher fees

        // PAN-based costs
        DEFAULT_FEES.put(
                getKey(PaymentMethod.VISA, false),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.VISA)
                        .fixedFee(new BigDecimal("0.10"))
                        .percentageFee(new BigDecimal("0.015"))
                        .authorizationRate(0.82)
                        .isToken(false)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.MASTERCARD, false),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.MASTERCARD)
                        .fixedFee(new BigDecimal("0.12"))
                        .percentageFee(new BigDecimal("0.014"))
                        .authorizationRate(0.83)
                        .isToken(false)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.AMEX, false),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.AMEX)
                        .fixedFee(new BigDecimal("0.15"))
                        .percentageFee(new BigDecimal("0.022"))
                        .authorizationRate(0.80)
                        .isToken(false)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.DISCOVER, false),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.DISCOVER)
                        .fixedFee(new BigDecimal("0.10"))
                        .percentageFee(new BigDecimal("0.016"))
                        .authorizationRate(0.85)
                        .isToken(false)
                        .build());

        // Debit networks have lower fees but also lower authorization rates
        DEFAULT_FEES.put(
                getKey(PaymentMethod.ACCEL, false),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.ACCEL)
                        .fixedFee(new BigDecimal("0.05"))
                        .percentageFee(new BigDecimal("0.005"))
                        .authorizationRate(0.80)
                        .isToken(false)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.STAR, false),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.STAR)
                        .fixedFee(new BigDecimal("0.04"))
                        .percentageFee(new BigDecimal("0.004"))
                        .authorizationRate(0.79)
                        .isToken(false)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.NYCE, false),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.NYCE)
                        .fixedFee(new BigDecimal("0.03"))
                        .percentageFee(new BigDecimal("0.003"))
                        .authorizationRate(0.77)
                        .isToken(false)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.PULSE, false),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.PULSE)
                        .fixedFee(new BigDecimal("0.03"))
                        .percentageFee(new BigDecimal("0.0035"))
                        .authorizationRate(0.84)
                        .isToken(false)
                        .build());

        // Maestro has characteristics of both signature and debit networks
        DEFAULT_FEES.put(
                getKey(PaymentMethod.MAESTRO, false),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.MAESTRO)
                        .fixedFee(new BigDecimal("0.08"))
                        .percentageFee(new BigDecimal("0.01"))
                        .authorizationRate(0.83)
                        .isToken(false)
                        .build());

        // Token-based costs (slightly lower fees, higher auth rates)
        DEFAULT_FEES.put(
                getKey(PaymentMethod.VISA, true),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.VISA)
                        .fixedFee(new BigDecimal("0.09"))
                        .percentageFee(new BigDecimal("0.014"))
                        .authorizationRate(0.85)
                        .isToken(true)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.MASTERCARD, true),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.MASTERCARD)
                        .fixedFee(new BigDecimal("0.11"))
                        .percentageFee(new BigDecimal("0.013"))
                        .authorizationRate(0.86)
                        .isToken(true)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.AMEX, true),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.AMEX)
                        .fixedFee(new BigDecimal("0.14"))
                        .percentageFee(new BigDecimal("0.021"))
                        .authorizationRate(0.83)
                        .isToken(true)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.DISCOVER, true),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.DISCOVER)
                        .fixedFee(new BigDecimal("0.09"))
                        .percentageFee(new BigDecimal("0.015"))
                        .authorizationRate(0.87)
                        .isToken(true)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.ACCEL, true),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.ACCEL)
                        .fixedFee(new BigDecimal("0.04"))
                        .percentageFee(new BigDecimal("0.0045"))
                        .authorizationRate(0.82)
                        .isToken(true)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.STAR, true),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.STAR)
                        .fixedFee(new BigDecimal("0.03"))
                        .percentageFee(new BigDecimal("0.0035"))
                        .authorizationRate(0.81)
                        .isToken(true)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.NYCE, true),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.NYCE)
                        .fixedFee(new BigDecimal("0.025"))
                        .percentageFee(new BigDecimal("0.0025"))
                        .authorizationRate(0.79)
                        .isToken(true)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.PULSE, true),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.PULSE)
                        .fixedFee(new BigDecimal("0.025"))
                        .percentageFee(new BigDecimal("0.003"))
                        .authorizationRate(0.86)
                        .isToken(true)
                        .build());

        DEFAULT_FEES.put(
                getKey(PaymentMethod.MAESTRO, true),
                RoutingCost.builder()
                        .paymentMethod(PaymentMethod.MAESTRO)
                        .fixedFee(new BigDecimal("0.07"))
                        .percentageFee(new BigDecimal("0.009"))
                        .authorizationRate(0.85)
                        .isToken(true)
                        .build());
    }

    /** Helper method to create a unique key for the fee map */
    private static String getKey(PaymentMethod method, boolean isToken) {
        return method.name() + (isToken ? "_TOKEN" : "_PAN");
    }

    @Override
    public RoutingResult findOptimalNetwork(
            BigDecimal amount, String currency, Set<PaymentMethod> availableNetworks) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        if (availableNetworks == null || availableNetworks.isEmpty()) {
            log.warn("No available payment networks provided");
            return getDefaultRoutingResult(amount, currency);
        }

        // Calculate costs for all available networks, for both token and PAN options
        Map<String, BigDecimal> costs = new HashMap<>();

        // Check both token and PAN options for each network
        for (PaymentMethod method : availableNetworks) {
            // Check PAN option
            String panKey = getKey(method, false);
            if (DEFAULT_FEES.containsKey(panKey)) {
                RoutingCost cost = DEFAULT_FEES.get(panKey);
                costs.put(panKey, cost.calculateExpectedCost(amount));
            }

            // Check token option
            String tokenKey = getKey(method, true);
            if (DEFAULT_FEES.containsKey(tokenKey)) {
                RoutingCost cost = DEFAULT_FEES.get(tokenKey);
                costs.put(tokenKey, cost.calculateExpectedCost(amount));
            }
        }

        if (costs.isEmpty()) {
            log.warn("No valid routing options found for networks: {}", availableNetworks);
            return RoutingResult.noValidOptions(amount, currency);
        }

        // Find the option with the lowest expected cost
        Map.Entry<String, BigDecimal> bestOption =
                costs.entrySet().stream()
                        .min(Map.Entry.comparingByValue())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Failed to determine optimal network"));

        String bestKey = bestOption.getKey();
        RoutingCost selectedCost = DEFAULT_FEES.get(bestKey);
        boolean useToken = selectedCost.isToken();
        PaymentMethod selectedMethod = selectedCost.getPaymentMethod();

        log.info(
                "Selected payment network: {} with expected cost: ${} (Auth Rate: {}%, Token: {})",
                selectedMethod,
                bestOption.getValue().setScale(4, RoundingMode.HALF_UP),
                String.format("%.1f", selectedCost.getAuthorizationRate() * 100),
                useToken ? "Yes" : "No");

        // Convert the costs map to a map of PaymentMethod to BigDecimal for the result
        Map<PaymentMethod, BigDecimal> methodCosts = new HashMap<>();
        costs.forEach(
                (key, value) -> {
                    PaymentMethod method = DEFAULT_FEES.get(key).getPaymentMethod();
                    // If we already have an entry for this method, keep the lower cost
                    if (!methodCosts.containsKey(method)
                            || value.compareTo(methodCosts.get(method)) < 0) {
                        methodCosts.put(method, value);
                    }
                });

        return RoutingResult.builder()
                .selectedPaymentMethod(selectedMethod)
                .estimatedCost(bestOption.getValue())
                .transactionAmount(amount)
                .currency(currency)
                .useToken(useToken)
                .allOptions(methodCosts)
                .build();
    }

    @Override
    public RoutingResult getDefaultRoutingResult(BigDecimal amount, String currency) {
        log.warn(
                "No valid routing options available for transaction amount: {}, currency: {}",
                amount,
                currency);
        return RoutingResult.noValidOptions(amount, currency);
    }

    @Override
    public BigDecimal getNetworkCost(
            PaymentMethod paymentMethod, BigDecimal amount, String currency) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        // Default to non-token cost
        String key = getKey(paymentMethod, false);
        if (!DEFAULT_FEES.containsKey(key)) {
            throw new IllegalArgumentException("Unknown payment method: " + paymentMethod);
        }

        return DEFAULT_FEES.get(key).calculateCost(amount);
    }

    /**
     * Gets the default fee structure for a payment method.
     *
     * @param paymentMethod The payment method
     * @param isToken Whether to get the token or PAN fee structure
     * @return The routing cost structure
     */
    public static RoutingCost getDefaultFeeStructure(PaymentMethod paymentMethod, boolean isToken) {
        return DEFAULT_FEES.get(getKey(paymentMethod, isToken));
    }
}
