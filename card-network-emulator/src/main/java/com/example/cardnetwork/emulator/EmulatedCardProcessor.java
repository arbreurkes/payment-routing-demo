package com.example.cardnetwork.emulator;

import com.example.shared.model.CardDetails;
import com.example.shared.model.PaymentMethod;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** An emulated implementation of CardProcessor that simulates interactions with card networks. */
@Slf4j
@Component
public class EmulatedCardProcessor implements CardProcessor {

    private static final double FAILURE_RATE = 0.05; // 5% chance of failure
    private static final double AUTHORIZATION_FAILURE_RATE =
            0.1; // 10% chance of authorization failure
    private static final Random RANDOM = new Random();

    // In-memory storage for transactions
    private final Map<String, TransactionState> transactions = new HashMap<>();

    @Override
    public CardAuthorizationResult authorize(
            CardDetails cardDetails,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod) {
        // Generate a transaction ID
        String transactionId = UUID.randomUUID().toString();

        // Simulate network latency
        simulateNetworkLatency();

        // Check for random failures
        if (RANDOM.nextDouble() < FAILURE_RATE) {
            log.warn("Network error processing transaction {}", transactionId);
            return CardAuthorizationResult.failed("Network error processing transaction");
        }

        // Check for authorization failure
        if (RANDOM.nextDouble() < AUTHORIZATION_FAILURE_RATE) {
            log.warn("Authorization declined for transaction {}", transactionId);
            return CardAuthorizationResult.failed("Authorization declined by issuer");
        }

        // Generate an authorization code (6 digits)
        String authCode = String.format("%06d", RANDOM.nextInt(1000000));

        // Store the transaction state
        transactions.put(
                transactionId,
                new TransactionState(
                        transactionId,
                        amount,
                        currency,
                        paymentMethod,
                        authCode,
                        TransactionState.Status.AUTHORIZED));

        log.info(
                "Authorization successful for transaction {}: {}{}",
                transactionId,
                amount,
                currency);

        return CardAuthorizationResult.success(authCode, paymentMethod, transactionId);
    }

    @Override
    public boolean capture(String transactionId, BigDecimal amount, String currency) {
        TransactionState transaction = transactions.get(transactionId);

        if (transaction == null) {
            log.error("Transaction {} not found", transactionId);
            return false;
        }

        if (transaction.getStatus() != TransactionState.Status.AUTHORIZED) {
            log.error("Transaction {} is not in AUTHORIZED state", transactionId);
            return false;
        }

        // Simulate network latency
        simulateNetworkLatency();

        // Check for random failures
        if (RANDOM.nextDouble() < FAILURE_RATE) {
            log.warn("Capture failed for transaction {}", transactionId);
            return false;
        }

        // Update transaction state
        transaction.setStatus(TransactionState.Status.CAPTURED);
        transaction.setCapturedAmount(amount);

        log.info("Capture successful for transaction {}: {}{}", transactionId, amount, currency);

        return true;
    }

    @Override
    public boolean voidAuthorization(String transactionId) {
        TransactionState transaction = transactions.get(transactionId);

        if (transaction == null) {
            log.error("Transaction {} not found", transactionId);
            return false;
        }

        if (transaction.getStatus() != TransactionState.Status.AUTHORIZED) {
            log.error("Transaction {} is not in AUTHORIZED state", transactionId);
            return false;
        }

        // Simulate network latency
        simulateNetworkLatency();

        // Check for random failures
        if (RANDOM.nextDouble() < FAILURE_RATE) {
            log.warn("Void failed for transaction {}", transactionId);
            return false;
        }

        // Update transaction state
        transaction.setStatus(TransactionState.Status.VOIDED);

        log.info("Void successful for transaction {}", transactionId);

        return true;
    }

    @Override
    public boolean refund(String transactionId, BigDecimal amount, String currency) {
        TransactionState transaction = transactions.get(transactionId);

        if (transaction == null) {
            log.error("Transaction {} not found", transactionId);
            return false;
        }

        if (transaction.getStatus() != TransactionState.Status.CAPTURED) {
            log.error("Transaction {} is not in CAPTURED state", transactionId);
            return false;
        }

        if (amount.compareTo(transaction.getCapturedAmount()) > 0) {
            log.error(
                    "Refund amount {} exceeds captured amount {}",
                    amount,
                    transaction.getCapturedAmount());
            return false;
        }

        // Simulate network latency
        simulateNetworkLatency();

        // Check for random failures
        if (RANDOM.nextDouble() < FAILURE_RATE) {
            log.warn("Refund failed for transaction {}", transactionId);
            return false;
        }

        // Update transaction state
        if (amount.compareTo(transaction.getCapturedAmount()) == 0) {
            transaction.setStatus(TransactionState.Status.REFUNDED);
        } else {
            transaction.setStatus(TransactionState.Status.PARTIALLY_REFUNDED);
        }
        transaction.setRefundedAmount(amount);

        log.info("Refund successful for transaction {}: {}{}", transactionId, amount, currency);

        return true;
    }

    private void simulateNetworkLatency() {
        try {
            // Simulate network latency between 50ms and 500ms
            Thread.sleep(50 + RANDOM.nextInt(450));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Internal class representing the state of a transaction. */
    private static class TransactionState {
        enum Status {
            AUTHORIZED,
            CAPTURED,
            VOIDED,
            REFUNDED,
            PARTIALLY_REFUNDED
        }

        private final String transactionId;
        private final BigDecimal amount;
        private final String currency;
        private final PaymentMethod paymentMethod;
        private final String authCode;
        private Status status;
        private BigDecimal capturedAmount;
        private BigDecimal refundedAmount;

        public TransactionState(
                String transactionId,
                BigDecimal amount,
                String currency,
                PaymentMethod paymentMethod,
                String authCode,
                Status status) {
            this.transactionId = transactionId;
            this.amount = amount;
            this.currency = currency;
            this.paymentMethod = paymentMethod;
            this.authCode = authCode;
            this.status = status;
            this.capturedAmount = BigDecimal.ZERO;
            this.refundedAmount = BigDecimal.ZERO;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public BigDecimal getCapturedAmount() {
            return capturedAmount;
        }

        public void setCapturedAmount(BigDecimal capturedAmount) {
            this.capturedAmount = capturedAmount;
        }

        public void setRefundedAmount(BigDecimal refundedAmount) {
            this.refundedAmount = refundedAmount;
        }
    }
}
