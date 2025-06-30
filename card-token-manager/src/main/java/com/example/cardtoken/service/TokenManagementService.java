package com.example.cardtoken.service;

import com.example.cardtoken.model.CardToken;
import com.example.cardtoken.model.TokenStatus;
import com.example.cardtoken.repository.TokenVault;
import com.example.shared.model.CardDetails;
import com.example.shared.model.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Service for managing card tokens across different payment networks. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenManagementService {

    private final TokenVault tokenVault;

    // List of token BIN ranges that can overlap across payment methods
    private static final List<TokenBinRange> TOKEN_BIN_RANGES = new ArrayList<>();

    // Default token validity in months
    private static final int DEFAULT_TOKEN_VALIDITY_MONTHS = 36;

    // Initialize token BIN ranges with potential overlaps
    static {
        // Visa token BIN ranges
        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("490000")
                        .endBin("499999")
                        .paymentMethod(PaymentMethod.VISA)
                        .build());

        // Mastercard token BIN ranges
        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("590000")
                        .endBin("599999")
                        .paymentMethod(PaymentMethod.MASTERCARD)
                        .build());

        // American Express token BIN ranges
        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("390000")
                        .endBin("399999")
                        .paymentMethod(PaymentMethod.AMEX)
                        .build());

        // Discover token BIN ranges
        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("650000")
                        .endBin("659999")
                        .paymentMethod(PaymentMethod.DISCOVER)
                        .build());

        // Debit networks token BIN ranges
        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("670000")
                        .endBin("670999")
                        .paymentMethod(PaymentMethod.ACCEL)
                        .build());

        // Overlapping ranges for VISA and ACCEL
        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("493500")
                        .endBin("493599")
                        .paymentMethod(PaymentMethod.ACCEL)
                        .build());

        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("671000")
                        .endBin("671999")
                        .paymentMethod(PaymentMethod.STAR)
                        .build());

        // Overlapping ranges for STAR and MASTERCARD
        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("593000")
                        .endBin("593999")
                        .paymentMethod(PaymentMethod.STAR)
                        .build());

        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("672000")
                        .endBin("672999")
                        .paymentMethod(PaymentMethod.NYCE)
                        .build());

        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("673000")
                        .endBin("673999")
                        .paymentMethod(PaymentMethod.PULSE)
                        .build());

        // Overlapping ranges for PULSE and DISCOVER
        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("652500")
                        .endBin("652599")
                        .paymentMethod(PaymentMethod.PULSE)
                        .build());

        TOKEN_BIN_RANGES.add(
                TokenBinRange.builder()
                        .startBin("674000")
                        .endBin("674999")
                        .paymentMethod(PaymentMethod.MAESTRO)
                        .build());
    }

    /**
     * Tokenizes a card for the specified payment method.
     *
     * @param cardDetails The card details to tokenize
     * @param paymentMethod The payment method to use for tokenization
     * @return The generated card token
     */
    public CardToken tokenize(CardDetails cardDetails, PaymentMethod paymentMethod) {
        // Generate a token value based on the payment method's token BIN range
        String tokenValue = generateTokenValue(paymentMethod);

        // Generate a unique token reference
        String tokenReference = UUID.randomUUID().toString();

        // Create a masked version of the PAN
        String maskedPan = maskPan(cardDetails.getCardNumber());

        // Create the token with an expiry date 3 years from now
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusMonths(DEFAULT_TOKEN_VALIDITY_MONTHS);

        // Build and save the token
        CardToken token =
                CardToken.builder()
                        .tokenReference(tokenReference)
                        .tokenValue(tokenValue)
                        .tokenBin(tokenValue.substring(0, 6))
                        .lastFour(cardDetails.getLastFour())
                        .expiryMonth(cardDetails.getExpiryMonth())
                        .expiryYear(cardDetails.getExpiryYear())
                        .maskedPan(maskedPan)
                        .createdAt(now)
                        .expiresAt(expiryDate)
                        .status(TokenStatus.ACTIVE)
                        .build()
                        .addPaymentMethod(paymentMethod);

        // Save the token to the vault
        tokenVault.save(token);

        log.info("Created token {} for payment method {}", tokenReference, paymentMethod);

        return token;
    }

    /**
     * Tokenizes a card for multiple payment methods.
     *
     * @param cardDetails The card details to tokenize
     * @param paymentMethods The set of payment methods to use for tokenization
     * @return The generated card token that supports multiple payment methods
     */
    public CardToken tokenizeForMultipleNetworks(
            CardDetails cardDetails, Set<PaymentMethod> paymentMethods) {
        if (paymentMethods == null || paymentMethods.isEmpty()) {
            throw new IllegalArgumentException("At least one payment method must be specified");
        }

        // Use the first payment method to generate the token value
        PaymentMethod primaryMethod = paymentMethods.iterator().next();
        String tokenValue = generateTokenValue(primaryMethod);

        // Generate a unique token reference
        String tokenReference = UUID.randomUUID().toString();

        // Create a masked version of the PAN
        String maskedPan = maskPan(cardDetails.getCardNumber());

        // Create the token with an expiry date 3 years from now
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusMonths(DEFAULT_TOKEN_VALIDITY_MONTHS);

        // Build the token with all payment methods
        CardToken token =
                CardToken.builder()
                        .tokenReference(tokenReference)
                        .tokenValue(tokenValue)
                        .tokenBin(tokenValue.substring(0, 6))
                        .lastFour(cardDetails.getLastFour())
                        .expiryMonth(cardDetails.getExpiryMonth())
                        .expiryYear(cardDetails.getExpiryYear())
                        .maskedPan(maskedPan)
                        .createdAt(now)
                        .expiresAt(expiryDate)
                        .status(TokenStatus.ACTIVE)
                        .paymentMethods(new HashSet<>(paymentMethods))
                        .build();

        // Save the token to the vault
        tokenVault.save(token);

        log.info("Created token {} for payment methods {}", tokenReference, paymentMethods);

        return token;
    }

    /**
     * Adds a payment method to an existing token.
     *
     * @param tokenReference The token reference
     * @param paymentMethod The payment method to add
     * @return The updated token, or empty if not found
     */
    public Optional<CardToken> addPaymentMethodToToken(
            String tokenReference, PaymentMethod paymentMethod) {
        Optional<CardToken> tokenOpt = tokenVault.findByTokenReference(tokenReference);

        if (tokenOpt.isPresent()) {
            CardToken token = tokenOpt.get();
            token.addPaymentMethod(paymentMethod);
            tokenVault.save(token);
            log.info("Added payment method {} to token {}", paymentMethod, tokenReference);
            return Optional.of(token);
        }

        return Optional.empty();
    }

    /**
     * Retrieves a token by its reference.
     *
     * @param tokenReference The token reference
     * @return An Optional containing the token if found
     */
    public Optional<CardToken> getToken(String tokenReference) {
        return tokenVault.findByTokenReference(tokenReference);
    }

    /**
     * Updates the status of a token.
     *
     * @param tokenReference The token reference
     * @param status The new status
     * @return The updated token, or empty if not found
     */
    public Optional<CardToken> updateTokenStatus(String tokenReference, TokenStatus status) {
        Optional<CardToken> tokenOpt = tokenVault.findByTokenReference(tokenReference);

        if (tokenOpt.isPresent()) {
            CardToken token = tokenOpt.get();
            token.setStatus(status);
            tokenVault.save(token);
            log.info("Updated token {} status to {}", tokenReference, status);
            return Optional.of(token);
        }

        return Optional.empty();
    }

    /**
     * Deletes a token by its reference.
     *
     * @param tokenReference The token reference
     * @return true if the token was deleted, false if not found
     */
    public boolean deleteToken(String tokenReference) {
        if (tokenVault.existsByTokenReference(tokenReference)) {
            tokenVault.deleteByTokenReference(tokenReference);
            log.info("Deleted token {}", tokenReference);
            return true;
        }
        return false;
    }

    /**
     * Detokenizes a token to retrieve the original card details. In a real implementation, this
     * would decrypt the stored PAN.
     *
     * @param tokenReference The token reference
     * @return An Optional containing the card details if the token is found and active
     */
    public Optional<CardDetails> detokenize(String tokenReference) {
        Optional<CardToken> tokenOpt = tokenVault.findByTokenReference(tokenReference);

        if (tokenOpt.isPresent()) {
            CardToken token = tokenOpt.get();

            // Check if the token is active
            if (!token.isActive()) {
                log.warn("Attempted to detokenize inactive token {}", tokenReference);
                return Optional.empty();
            }

            // In a real implementation, this would decrypt the PAN
            // Here we simulate it by using the masked PAN
            String cardNumber = simulateDecryptPan(token.getMaskedPan());

            // Create and return card details
            CardDetails cardDetails =
                    CardDetails.builder()
                            .cardNumber(cardNumber)
                            .expiryMonth(token.getExpiryMonth())
                            .expiryYear(token.getExpiryYear())
                            .build();

            return Optional.of(cardDetails);
        }

        return Optional.empty();
    }

    /**
     * Refreshes a token, extending its expiry date.
     *
     * @param tokenReference The token reference
     * @param additionalMonths The number of months to extend the token's validity
     * @return The updated token, or empty if not found
     */
    public Optional<CardToken> refreshToken(String tokenReference, int additionalMonths) {
        Optional<CardToken> tokenOpt = tokenVault.findByTokenReference(tokenReference);

        if (tokenOpt.isPresent()) {
            CardToken token = tokenOpt.get();

            // Extend the expiry date
            LocalDateTime newExpiryDate = token.getExpiresAt().plusMonths(additionalMonths);
            token.setExpiresAt(newExpiryDate);

            // Ensure the token is active
            token.setStatus(TokenStatus.ACTIVE);

            tokenVault.save(token);
            log.info("Refreshed token {} expiry to {}", tokenReference, newExpiryDate);

            return Optional.of(token);
        }

        return Optional.empty();
    }

    /**
     * Saves a token to the vault.
     *
     * @param token The token to save
     * @return The saved token
     */
    public CardToken save(CardToken token) {
        return tokenVault.save(token);
    }

    /**
     * Finds a token by its value.
     *
     * @param tokenValue The token value
     * @return An Optional containing the token if found
     */
    public Optional<CardToken> findByTokenValue(String tokenValue) {
        return tokenVault.findByTokenValue(tokenValue);
    }

    /**
     * Generates a token value based on the payment method's token BIN range.
     *
     * @param paymentMethod The payment method
     * @return A token value with the appropriate BIN
     */
    private String generateTokenValue(PaymentMethod paymentMethod) {
        // Find all matching token bin ranges for the payment method
        List<TokenBinRange> matchingRanges =
                TOKEN_BIN_RANGES.stream()
                        .filter(range -> range.getPaymentMethod().equals(paymentMethod))
                        .toList();

        if (matchingRanges.isEmpty()) {
            // Default to Visa token BIN range if the payment method is not found
            matchingRanges =
                    TOKEN_BIN_RANGES.stream()
                            .filter(range -> range.getPaymentMethod().equals(PaymentMethod.VISA))
                            .toList();

            if (matchingRanges.isEmpty()) {
                // Fallback to the first range if no Visa ranges are found
                matchingRanges = List.of(TOKEN_BIN_RANGES.get(0));
            }
        }

        // Randomly select one of the matching ranges
        TokenBinRange selectedRange =
                matchingRanges.get(ThreadLocalRandom.current().nextInt(matchingRanges.size()));

        // Generate a random BIN within the selected range
        String tokenBin = generateRandomBin(selectedRange.getStartBin(), selectedRange.getEndBin());

        // Generate the rest of the token (10 digits)
        StringBuilder tokenBuilder = new StringBuilder(tokenBin);
        for (int i = 0; i < 10; i++) {
            tokenBuilder.append(ThreadLocalRandom.current().nextInt(10));
        }

        return tokenBuilder.toString();
    }

    /**
     * Generates a random BIN within the specified range.
     *
     * @param startBin The start of the BIN range
     * @param endBin The end of the BIN range
     * @return A random BIN within the range
     */
    private String generateRandomBin(String startBin, String endBin) {
        int start = Integer.parseInt(startBin);
        int end = Integer.parseInt(endBin);
        int randomBin = ThreadLocalRandom.current().nextInt(start, end + 1);
        return String.format("%06d", randomBin);
    }

    /**
     * Masks a PAN for storage. In a real implementation, this would encrypt the PAN.
     *
     * @param pan The PAN to mask
     * @return The masked PAN
     */
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 13) {
            return "************";
        }

        // Keep first 6 and last 4 digits, mask the rest
        String firstSix = pan.substring(0, 6);
        String lastFour = pan.substring(pan.length() - 4);
        int maskedLength = pan.length() - 10;

        return firstSix + "*".repeat(maskedLength) + lastFour;
    }

    /**
     * Simulates decrypting a masked PAN. In a real implementation, this would decrypt the stored
     * PAN.
     *
     * @param maskedPan The masked PAN
     * @return A simulated decrypted PAN
     */
    private String simulateDecryptPan(String maskedPan) {
        // In a real implementation, this would decrypt the stored PAN
        // For simulation, we replace the masked portion with random digits
        if (maskedPan == null || maskedPan.length() < 10) {
            return "4111111111111111"; // Default test card
        }

        String firstSix = maskedPan.substring(0, 6);
        String lastFour = maskedPan.substring(maskedPan.length() - 4);
        int maskedLength = maskedPan.length() - 10;

        // Generate random digits for the masked portion
        StringBuilder middleBuilder = new StringBuilder();
        for (int i = 0; i < maskedLength; i++) {
            middleBuilder.append(ThreadLocalRandom.current().nextInt(10));
        }

        return firstSix + middleBuilder + lastFour;
    }

    /** Internal class to represent a token BIN range. */
    @Data
    @Builder
    @AllArgsConstructor
    private static class TokenBinRange {
        private String startBin;
        private String endBin;
        private PaymentMethod paymentMethod;
    }
}
