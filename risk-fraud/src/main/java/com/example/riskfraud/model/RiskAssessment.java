package com.example.riskfraud.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {
    private String transactionId;
    private RiskLevel riskLevel;
    private double riskScore;
    private String reason;
    private boolean approved;
    private LocalDateTime assessmentTime;
    private String assessmentId;

    public static RiskAssessment fromTransaction(Transaction transaction, double riskScore) {
        RiskLevel riskLevel = RiskLevel.fromScore(riskScore);
        return RiskAssessment.builder()
                .transactionId(transaction.getId())
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .approved(riskScore < 0.7) // Example threshold
                .reason("Risk assessment completed")
                .assessmentTime(LocalDateTime.now())
                .assessmentId(java.util.UUID.randomUUID().toString())
                .build();
    }
}
