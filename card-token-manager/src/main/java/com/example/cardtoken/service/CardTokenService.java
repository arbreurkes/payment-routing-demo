package com.example.cardtoken.service;

import com.example.cardtoken.model.CardToken;
import com.example.cardtoken.model.TokenStatus;
import com.example.shared.model.CardDetails;
import com.example.shared.model.PaymentMethod;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for card tokenization operations that can be used by other modules. This service provides
 * a simplified interface to the token management functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardTokenService {

    private final TokenManagementService tokenManagementService;

    /**
     * Tokenizes a card for the specified payment method.
     *
     * @param cardDetails The card details to tokenize
     * @param paymentMethod The payment method to use for tokenization
     * @return The generated card token
     */
    public CardToken tokenize(CardDetails cardDetails, PaymentMethod paymentMethod) {
        log.info("Tokenizing card for payment method: {}", paymentMethod);
        return tokenManagementService.tokenize(cardDetails, paymentMethod);
    }

    /**
     * Tokenizes a card for multiple payment methods. This is useful when a card can be processed
     * through multiple networks.
     *
     * @param cardDetails The card details to tokenize
     * @param paymentMethods The set of payment methods to use for tokenization
     * @return A card token that supports multiple payment methods
     */
    public CardToken tokenizeForMultipleNetworks(
            CardDetails cardDetails, Set<PaymentMethod> paymentMethods) {
        log.info("Tokenizing card for multiple payment methods: {}", paymentMethods);
        return tokenManagementService.tokenizeForMultipleNetworks(cardDetails, paymentMethods);
    }

    /**
     * Retrieves a token by its reference.
     *
     * @param tokenReference The token reference
     * @return An Optional containing the token if found
     */
    public Optional<CardToken> getToken(String tokenReference) {
        return tokenManagementService.getToken(tokenReference);
    }

    /**
     * Detokenizes a token to retrieve the original card details.
     *
     * @param tokenReference The token reference
     * @return An Optional containing the card details if the token is found and active
     */
    public Optional<CardDetails> detokenize(String tokenReference) {
        return tokenManagementService.detokenize(tokenReference);
    }

    /**
     * Suspends a token.
     *
     * @param tokenReference The token reference
     * @return true if the token was suspended, false if not found
     */
    public boolean suspendToken(String tokenReference) {
        return tokenManagementService
                .updateTokenStatus(tokenReference, TokenStatus.SUSPENDED)
                .isPresent();
    }

    /**
     * Reactivates a suspended token.
     *
     * @param tokenReference The token reference
     * @return true if the token was reactivated, false if not found
     */
    public boolean reactivateToken(String tokenReference) {
        return tokenManagementService
                .updateTokenStatus(tokenReference, TokenStatus.ACTIVE)
                .isPresent();
    }

    /**
     * Deletes a token.
     *
     * @param tokenReference The token reference
     * @return true if the token was deleted, false if not found
     */
    public boolean deleteToken(String tokenReference) {
        return tokenManagementService.deleteToken(tokenReference);
    }

    /**
     * Refreshes a token, extending its expiry date by the specified number of months.
     *
     * @param tokenReference The token reference
     * @param additionalMonths The number of months to extend the token's validity
     * @return true if the token was refreshed, false if not found
     */
    public boolean refreshToken(String tokenReference, int additionalMonths) {
        return tokenManagementService.refreshToken(tokenReference, additionalMonths).isPresent();
    }

    /**
     * Checks if a token is active.
     *
     * @param tokenReference The token reference
     * @return true if the token is active, false otherwise
     */
    public boolean isTokenActive(String tokenReference) {
        Optional<CardToken> token = tokenManagementService.getToken(tokenReference);
        return token.isPresent() && token.get().isActive();
    }

    /**
     * Finds all valid tokens for a specific payment method that can be used for a card.
     *
     * @param cardDetails The card details
     * @return A list of active tokens that can be used for the card and payment method
     */
    public List<CardToken> findValidTokensForCard(CardDetails cardDetails) {
        log.info("Finding valid tokens for card ending with {}.", cardDetails.getLastFour());

        // First check if there are existing tokens for this card by token value
        // In a real implementation, we would use a secure lookup based on a fingerprint or hash of
        // the PAN
        // For this demo, we'll use the tokensByValue map as a simulation
        String cardNumber = cardDetails.getCardNumber();

        // Find tokens by token value (simulating PAN lookup)
        // In a real implementation, we would search in a more sophisticated way
        Optional<CardToken> existingToken = tokenManagementService.findByTokenValue(cardNumber);

        if (existingToken.isEmpty()) {
            // Try looking up by last 4 digits (common fallback in real systems)
            existingToken =
                    findTokenByLastFour(
                            cardDetails.getLastFour(),
                            cardDetails.getExpiryMonth(),
                            cardDetails.getExpiryYear());
        }

        if (existingToken.isPresent()) {
            CardToken token = existingToken.get();

            // Check if the token is active
            if (token.isActive()) {
                log.info(
                        "Found existing valid token {} for card ending with {}",
                        token.getTokenReference(),
                        cardDetails.getLastFour());
                return List.of(token);
            }

            // If token exists but is not active, create a new one
            log.info(
                    "Found existing token {} for card ending with {} but it's not active, creating new token",
                    token.getTokenReference(),
                    cardDetails.getLastFour());
        }

        return List.of();
    }

    /**
     * Helper method to find a token by the last four digits of the card and expiry date. This is a
     * simplified implementation for demonstration purposes.
     *
     * @param lastFour The last four digits of the card
     * @param expiryMonth The expiry month
     * @param expiryYear The expiry year
     * @return An Optional containing the token if found
     */
    private Optional<CardToken> findTokenByLastFour(
            String lastFour, int expiryMonth, int expiryYear) {
        // In a real implementation, this would query a database
        // For this demo, we'll return an empty Optional
        return Optional.empty();
    }

    /**
     * Adds a payment method to an existing token.
     *
     * @param tokenReference The token reference
     * @param paymentMethod The payment method to add
     * @return true if the payment method was added, false if the token was not found
     */
    public boolean addPaymentMethodToToken(String tokenReference, PaymentMethod paymentMethod) {
        log.info("Adding payment method {} to token {}", paymentMethod, tokenReference);
        return tokenManagementService
                .addPaymentMethodToToken(tokenReference, paymentMethod)
                .isPresent();
    }

    /**
     * Checks if a token supports a specific payment method.
     *
     * @param tokenReference The token reference
     * @param paymentMethod The payment method to check
     * @return true if the token supports the payment method, false otherwise
     */
    public boolean supportsPaymentMethod(String tokenReference, PaymentMethod paymentMethod) {
        Optional<CardToken> token = tokenManagementService.getToken(tokenReference);
        return token.isPresent() && token.get().supportsPaymentMethod(paymentMethod);
    }
}
