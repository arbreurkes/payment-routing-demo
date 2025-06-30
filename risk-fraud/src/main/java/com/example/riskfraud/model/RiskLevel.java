package com.example.riskfraud.model;

public enum RiskLevel {
    LOW(0.0, 0.3, "Low risk"),
    MEDIUM(0.3, 0.7, "Medium risk"),
    HIGH(0.7, 0.9, "High risk"),
    CRITICAL(0.9, 1.0, "Critical risk");

    private final double minScore;
    private final double maxScore;
    private final String description;

    RiskLevel(double minScore, double maxScore, String description) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.description = description;
    }

    public boolean isInRange(double score) {
        return score >= minScore && score < maxScore;
    }

    public static RiskLevel fromScore(double score) {
        for (RiskLevel level : values()) {
            if (level.isInRange(score)) {
                return level;
            }
        }
        return CRITICAL; // Default to CRITICAL if score is out of expected range
    }

    public String getDescription() {
        return description;
    }
}
