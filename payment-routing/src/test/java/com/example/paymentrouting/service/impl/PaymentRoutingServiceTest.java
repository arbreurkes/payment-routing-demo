package com.example.paymentrouting.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.example.paymentrouting.model.RoutingResult;
import com.example.shared.model.PaymentMethod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

public class PaymentRoutingServiceTest {

    private PaymentRoutingServiceImpl paymentRoutingService;

    @BeforeEach
    void setUp() {
        paymentRoutingService = new PaymentRoutingServiceImpl();
    }

    @Test
    void findOptimalNetwork_WithSinglePaymentMethod_ShouldReturnThatMethod() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String currency = "USD";
        Set<PaymentMethod> availableNetworks = Collections.singleton(PaymentMethod.VISA);

        // Act
        RoutingResult result =
                paymentRoutingService.findOptimalNetwork(amount, currency, availableNetworks);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentMethod.VISA, result.getSelectedPaymentMethod());
        assertTrue(result.hasValidOption());
    }

    @Test
    void findOptimalNetwork_WithMultiplePaymentMethods_ShouldReturnOptimalMethod() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String currency = "USD";
        Set<PaymentMethod> availableNetworks =
                new HashSet<>(
                        Arrays.asList(
                                PaymentMethod.VISA,
                                PaymentMethod.MASTERCARD,
                                PaymentMethod.DISCOVER));

        // Act
        RoutingResult result =
                paymentRoutingService.findOptimalNetwork(amount, currency, availableNetworks);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getSelectedPaymentMethod());
        assertTrue(result.hasValidOption());
        assertNotNull(result.getEstimatedCost());
    }

    @Test
    void findOptimalNetwork_WithEmptyPaymentMethods_ShouldReturnNoValidOptions() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String currency = "USD";
        Set<PaymentMethod> availableNetworks = Collections.emptySet();

        // Act
        RoutingResult result =
                paymentRoutingService.findOptimalNetwork(amount, currency, availableNetworks);

        // Assert
        assertNotNull(result);
        assertNull(result.getSelectedPaymentMethod());
        assertFalse(result.hasValidOption());
        assertTrue(result.getAllOptions().isEmpty());
    }

    @Test
    void findOptimalNetwork_WithNullPaymentMethods_ShouldReturnNoValidOptions() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String currency = "USD";

        // Act
        RoutingResult result = paymentRoutingService.findOptimalNetwork(amount, currency, null);

        // Assert
        assertNotNull(result);
        assertNull(result.getSelectedPaymentMethod());
        assertFalse(result.hasValidOption());
        assertTrue(result.getAllOptions().isEmpty());
    }

    @Test
    void findOptimalNetwork_WithNegativeAmount_ShouldThrowException() {
        // Arrange
        BigDecimal negativeAmount = BigDecimal.valueOf(-100.00);
        String currency = "USD";
        Set<PaymentMethod> availableNetworks = Collections.singleton(PaymentMethod.VISA);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    paymentRoutingService.findOptimalNetwork(
                            negativeAmount, currency, availableNetworks);
                });
    }

    @Test
    void findOptimalNetwork_WithZeroAmount_ShouldThrowException() {
        // Arrange
        BigDecimal zeroAmount = BigDecimal.ZERO;
        String currency = "USD";
        Set<PaymentMethod> availableNetworks = Collections.singleton(PaymentMethod.VISA);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    paymentRoutingService.findOptimalNetwork(
                            zeroAmount, currency, availableNetworks);
                });
    }

    @Test
    void findOptimalNetwork_WithLargeAmount_ShouldSelectLowestCostNetwork() {
        // Arrange
        BigDecimal largeAmount = BigDecimal.valueOf(10000.00);
        String currency = "USD";
        Set<PaymentMethod> availableNetworks =
                new HashSet<>(
                        Arrays.asList(
                                PaymentMethod.VISA,
                                PaymentMethod.MASTERCARD,
                                PaymentMethod.DISCOVER));

        // Act
        RoutingResult result =
                paymentRoutingService.findOptimalNetwork(largeAmount, currency, availableNetworks);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getSelectedPaymentMethod());
        assertTrue(result.hasValidOption());
    }

    @Test
    void findOptimalNetwork_WithSmallAmount_ShouldSelectLowestCostNetwork() {
        // Arrange
        BigDecimal smallAmount = BigDecimal.valueOf(10.00);
        String currency = "USD";
        Set<PaymentMethod> availableNetworks =
                new HashSet<>(
                        Arrays.asList(
                                PaymentMethod.VISA,
                                PaymentMethod.MASTERCARD,
                                PaymentMethod.DISCOVER));

        // Act
        RoutingResult result =
                paymentRoutingService.findOptimalNetwork(smallAmount, currency, availableNetworks);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getSelectedPaymentMethod());
        assertTrue(result.hasValidOption());
    }

    @Test
    void findOptimalNetwork_ShouldConsiderTokenVsPanCosts() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String currency = "USD";
        Set<PaymentMethod> availableNetworks =
                new HashSet<>(Arrays.asList(PaymentMethod.VISA, PaymentMethod.MASTERCARD));

        // Act
        RoutingResult result =
                paymentRoutingService.findOptimalNetwork(amount, currency, availableNetworks);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getSelectedPaymentMethod());
        // Token option should be selected as it has lower fees and higher auth rates
        assertTrue(result.isUseToken());
    }

    @Test
    void getNetworkCost_WithValidPaymentMethod_ShouldReturnCorrectCost() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String currency = "USD";
        PaymentMethod paymentMethod = PaymentMethod.VISA;

        // Act
        BigDecimal cost = paymentRoutingService.getNetworkCost(paymentMethod, amount, currency);

        // Assert
        assertNotNull(cost);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void getNetworkCost_WithNullPaymentMethod_ShouldThrowException() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String currency = "USD";
        PaymentMethod nullMethod = null;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    paymentRoutingService.getNetworkCost(nullMethod, amount, currency);
                });
    }

    @Test
    void getDefaultRoutingResult_ShouldReturnNoValidOptions() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String currency = "USD";

        // Act
        RoutingResult result = paymentRoutingService.getDefaultRoutingResult(amount, currency);

        // Assert
        assertNotNull(result);
        assertNull(result.getSelectedPaymentMethod());
        assertFalse(result.hasValidOption());
        assertTrue(result.getAllOptions().isEmpty());
        assertEquals(amount, result.getTransactionAmount());
        assertEquals(currency, result.getCurrency());
    }
}
