package com.example.riskfraud.service;

import com.example.riskfraud.model.RiskAssessment;
import com.example.riskfraud.model.Transaction;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

@Service
public class RiskAssessmentService {

    private final Random random = new Random();

    public RiskAssessment assessRisk(Transaction transaction) {
        double riskScore = calculateRiskScore(transaction);
        return RiskAssessment.fromTransaction(transaction, riskScore);
    }

    private double calculateRiskScore(Transaction transaction) {
        double score = 0.0;

        // 1. Transaction amount risk (0-0.3)
        score += calculateAmountRisk(transaction.getAmount());

        // 2. User behavior risk (0-0.3)
        score += calculateUserBehaviorRisk(transaction);

        // 3. Merchant risk (0-0.2)
        score += calculateMerchantRisk(transaction.getMerchantCategoryCode());

        // 4. Device and location risk (0-0.2)
        score += calculateDeviceAndLocationRisk(transaction);

        // Ensure score is between 0 and 1
        return Math.min(1.0, Math.max(0.0, score));
    }

    private double calculateAmountRisk(BigDecimal amount) {
        // Higher amounts are riskier, but not linearly
        double amountValue = amount.doubleValue();
        if (amountValue < 100) return 0.1;
        if (amountValue < 1000) return 0.15;
        if (amountValue < 5000) return 0.2;
        if (amountValue < 10000) return 0.3;
        return 0.35; // Very high amount
    }

    private double calculateUserBehaviorRisk(Transaction transaction) {
        double score = 0.0;

        // New user with no history is medium risk
        if (transaction.getPreviousSuccessfulTransactions() == 0) {
            score += 0.2;
        } else {
            // Calculate success rate
            double successRate =
                    (double) transaction.getPreviousSuccessfulTransactions()
                            / (transaction.getPreviousSuccessfulTransactions()
                                    + transaction.getPreviousFailedTransactions());

            if (successRate < 0.7) score += 0.25; // Low success rate is risky
            else if (successRate > 0.95) score += 0.05; // Very reliable user

            // High number of chargebacks is very risky
            if (transaction.getPreviousChargebacks() > 0) {
                score += 0.1 * transaction.getPreviousChargebacks();
            }
        }

        return Math.min(0.3, score); // Cap at 0.3 for this factor
    }

    private double calculateMerchantRisk(String merchantCategoryCode) {
        // In a real system, this would use a database of merchant risk profiles
        // For now, we'll use some example categories
        if (merchantCategoryCode == null) return 0.1;

        return switch (merchantCategoryCode.toUpperCase()) { // Groceries
            case "5411", "5412" -> // Discount stores
                    0.05; // Jewelry
            case "5944", "5941" -> // Sports equipment
                    0.15; // Wire transfers
            case "4829", "6051" -> // Cryptocurrency
                    0.25;
            default -> 0.1; // Average risk
        };
    }

    private double calculateDeviceAndLocationRisk(Transaction transaction) {
        double score = 0.0;

        // High risk if using VPN/TOR
        if (isSuspiciousIp(transaction.getUserIpAddress())) {
            score += 0.15;
        }

        // New device or device with bad history
        if (isNewOrRiskyDevice(transaction.getUserDeviceId())) {
            score += 0.1;
        }

        // Location mismatch between billing and IP country
        if (hasSuspiciousLocation(transaction)) {
            score += 0.15;
        }

        return Math.min(0.2, score); // Cap at 0.2 for this factor
    }

    // Mock methods - in a real system, these would check against databases or services
    private boolean isSuspiciousIp(String ip) {
        // Check against known VPN/proxy/TOR exits
        // For demo, randomly return true 10% of the time
        return random.nextDouble() < 0.1;
    }

    private boolean isNewOrRiskyDevice(String deviceId) {
        // Check device reputation
        // For demo, randomly return true 5% of the time
        return random.nextDouble() < 0.05;
    }

    private boolean hasSuspiciousLocation(Transaction transaction) {
        // Check if billing country matches IP country
        // For demo, randomly return true 8% of the time
        return random.nextDouble() < 0.08;
    }
}
