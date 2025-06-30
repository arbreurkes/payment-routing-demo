package com.example.payment.api.dto;

import com.example.cardtoken.model.TokenStatus;
import com.example.shared.model.PaymentMethod;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/** Response DTO for token operations. */
@Data
public class TokenResponse {
    private String tokenReference;
    private String lastFour;
    private int expiryMonth;
    private int expiryYear;
    private Set<PaymentMethod> paymentMethods;
    private String tokenBin;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private TokenStatus status;
}
