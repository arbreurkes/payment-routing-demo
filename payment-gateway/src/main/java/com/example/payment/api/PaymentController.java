package com.example.payment.api;

import com.example.cardtoken.model.CardToken;
import com.example.cardtoken.service.CardTokenService;
import com.example.payment.api.dto.CardDetailsDto;
import com.example.payment.api.dto.CardPaymentRequest;
import com.example.payment.api.dto.PaymentResponse;
import com.example.payment.api.dto.TokenResponse;
import com.example.payment.service.PaymentService;
import com.example.shared.model.CardDetails;
import com.example.shared.model.PaymentMethod;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment API", description = "API for processing card payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final CardTokenService cardTokenService;

    @PostMapping("/authorize")
    @Operation(summary = "Authorize a payment")
    public ResponseEntity<PaymentResponse> authorize(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Valid @RequestBody CardPaymentRequest request) {
        return ResponseEntity.ok(paymentService.authorize(request, merchantId));
    }

    @PostMapping("/{paymentId}/capture")
    @Operation(summary = "Capture an authorized payment")
    public ResponseEntity<PaymentResponse> capture(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @PathVariable String paymentId,
            @RequestParam(required = false) BigDecimal amount) {
        if (amount == null) {
            // If amount is not specified, capture the full authorized amount
            return ResponseEntity.ok(paymentService.capture(paymentId, null, merchantId));
        }
        return ResponseEntity.ok(paymentService.capture(paymentId, amount, merchantId));
    }

    @PutMapping("/{paymentId}/modify")
    @Operation(summary = "Modify an authorized payment")
    public ResponseEntity<PaymentResponse> modify(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @PathVariable String paymentId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(paymentService.modify(paymentId, amount, merchantId));
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Cancel an authorized payment")
    public ResponseEntity<PaymentResponse> cancel(
            @RequestHeader("X-Merchant-Id") String merchantId, @PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.cancel(paymentId, merchantId));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a captured payment")
    public ResponseEntity<PaymentResponse> refund(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @PathVariable String paymentId,
            @RequestParam(required = false) BigDecimal amount) {
        if (amount == null) {
            // If amount is not specified, refund the full captured amount
            return ResponseEntity.ok(paymentService.refund(paymentId, null, merchantId));
        }
        return ResponseEntity.ok(paymentService.refund(paymentId, amount, merchantId));
    }

    @GetMapping("/{paymentId}/status")
    @Operation(summary = "Get payment status")
    public ResponseEntity<PaymentResponse> getStatus(
            @RequestHeader("X-Merchant-Id") String merchantId, @PathVariable String paymentId) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(paymentId);
        response.setStatus(paymentService.getPaymentStatus(paymentId, merchantId));
        response.setTimestamp(java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tokenize")
    @Operation(summary = "Tokenize card details")
    public ResponseEntity<TokenResponse> tokenize(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @Valid @RequestBody CardDetailsDto cardDetailsDto,
            @RequestParam(required = false) PaymentMethod paymentMethod) {

        // Convert DTO to domain model
        CardDetails cardDetails =
                CardDetails.builder()
                        .cardNumber(cardDetailsDto.getCardNumber())
                        .cardholderName(cardDetailsDto.getCardholderName())
                        .expiryMonth(cardDetailsDto.getExpiryMonth())
                        .expiryYear(cardDetailsDto.getExpiryYear())
                        .cvv(cardDetailsDto.getCvv())
                        .build();

        // If payment method is not specified, use VISA as default
        PaymentMethod method = paymentMethod != null ? paymentMethod : PaymentMethod.VISA;

        // Tokenize the card
        CardToken token = cardTokenService.tokenize(cardDetails, method);

        // Create response
        TokenResponse response = new TokenResponse();
        response.setTokenReference(token.getTokenReference());
        response.setLastFour(token.getLastFour());
        response.setExpiryMonth(token.getExpiryMonth());
        response.setExpiryYear(token.getExpiryYear());
        response.setPaymentMethods(token.getPaymentMethods());
        response.setTokenBin(token.getTokenBin());
        response.setCreatedAt(token.getCreatedAt());
        response.setExpiresAt(token.getExpiresAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tokens/{tokenReference}")
    @Operation(summary = "Retrieve token details")
    public ResponseEntity<TokenResponse> retrieveToken(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @PathVariable String tokenReference) {

        // Retrieve token
        Optional<CardToken> tokenOpt = cardTokenService.getToken(tokenReference);

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CardToken token = tokenOpt.get();

        // Create response
        TokenResponse response = new TokenResponse();
        response.setTokenReference(token.getTokenReference());
        response.setLastFour(token.getLastFour());
        response.setExpiryMonth(token.getExpiryMonth());
        response.setExpiryYear(token.getExpiryYear());
        response.setPaymentMethods(token.getPaymentMethods());
        response.setTokenBin(token.getTokenBin());
        response.setCreatedAt(token.getCreatedAt());
        response.setExpiresAt(token.getExpiresAt());
        response.setStatus(token.getStatus());

        return ResponseEntity.ok(response);
    }
}
