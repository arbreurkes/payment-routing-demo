package com.example.cardtoken.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.cardtoken.model.CardToken;
import com.example.cardtoken.model.TokenStatus;
import com.example.shared.model.CardDetails;
import com.example.shared.model.PaymentMethod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class CardTokenServiceTest {

    @Mock private TokenManagementService tokenManagementService;

    @InjectMocks private CardTokenService cardTokenService;

    private CardDetails validCardDetails;
    private CardToken validCardToken;
    private String validTokenReference;

    @BeforeEach
    void setUp() {
        // Setup valid card details for testing
        validCardDetails =
                CardDetails.builder()
                        .cardNumber("4500123456789010")
                        .cardholderName("John Doe")
                        .expiryMonth(12)
                        .expiryYear(2030)
                        .cvv("123")
                        .build();

        // Setup valid token reference
        validTokenReference = UUID.randomUUID().toString();

        // Setup valid card token
        validCardToken =
                CardToken.builder()
                        .tokenReference(validTokenReference)
                        .tokenValue("token-value-123456")
                        .lastFour("9010")
                        .expiryMonth(12)
                        .expiryYear(2030)
                        .paymentMethods(
                                new HashSet<>(Collections.singletonList(PaymentMethod.VISA)))
                        .tokenBin("490000")
                        .status(TokenStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusYears(3))
                        .build();
    }

    @Test
    void tokenize_WithValidCardDetails_ShouldReturnCardToken() {
        // Arrange
        when(tokenManagementService.tokenize(any(CardDetails.class), eq(PaymentMethod.VISA)))
                .thenReturn(validCardToken);

        // Act
        CardToken result = cardTokenService.tokenize(validCardDetails, PaymentMethod.VISA);

        // Assert
        assertNotNull(result);
        assertEquals(validTokenReference, result.getTokenReference());
        assertEquals("9010", result.getLastFour());
        assertEquals(12, result.getExpiryMonth());
        assertEquals(2030, result.getExpiryYear());
        assertTrue(result.getPaymentMethods().contains(PaymentMethod.VISA));
        assertEquals(TokenStatus.ACTIVE, result.getStatus());
        verify(tokenManagementService).tokenize(validCardDetails, PaymentMethod.VISA);
    }

    @Test
    void tokenizeForMultipleNetworks_WithValidCardDetails_ShouldReturnCardToken() {
        // Arrange
        Set<PaymentMethod> paymentMethods =
                new HashSet<>(Arrays.asList(PaymentMethod.VISA, PaymentMethod.MASTERCARD));
        CardToken multiNetworkToken =
                CardToken.builder()
                        .tokenReference(validTokenReference)
                        .tokenValue("token-value-123456")
                        .lastFour("9010")
                        .expiryMonth(12)
                        .expiryYear(2030)
                        .paymentMethods(paymentMethods)
                        .tokenBin("490000")
                        .status(TokenStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusYears(3))
                        .build();

        when(tokenManagementService.tokenizeForMultipleNetworks(
                        any(CardDetails.class), eq(paymentMethods)))
                .thenReturn(multiNetworkToken);

        // Act
        CardToken result =
                cardTokenService.tokenizeForMultipleNetworks(validCardDetails, paymentMethods);

        // Assert
        assertNotNull(result);
        assertEquals(validTokenReference, result.getTokenReference());
        assertEquals(2, result.getPaymentMethods().size());
        assertTrue(result.getPaymentMethods().contains(PaymentMethod.VISA));
        assertTrue(result.getPaymentMethods().contains(PaymentMethod.MASTERCARD));
        verify(tokenManagementService)
                .tokenizeForMultipleNetworks(validCardDetails, paymentMethods);
    }

    @Test
    void getToken_WithValidReference_ShouldReturnToken() {
        // Arrange
        when(tokenManagementService.getToken(validTokenReference))
                .thenReturn(Optional.of(validCardToken));

        // Act
        Optional<CardToken> result = cardTokenService.getToken(validTokenReference);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(validTokenReference, result.get().getTokenReference());
        verify(tokenManagementService).getToken(validTokenReference);
    }

    @Test
    void getToken_WithInvalidReference_ShouldReturnEmpty() {
        // Arrange
        String invalidReference = "invalid-reference";
        when(tokenManagementService.getToken(invalidReference)).thenReturn(Optional.empty());

        // Act
        Optional<CardToken> result = cardTokenService.getToken(invalidReference);

        // Assert
        assertFalse(result.isPresent());
        verify(tokenManagementService).getToken(invalidReference);
    }

    @Test
    void detokenize_WithValidReference_ShouldReturnCardDetails() {
        // Arrange
        when(tokenManagementService.detokenize(validTokenReference))
                .thenReturn(Optional.of(validCardDetails));

        // Act
        Optional<CardDetails> result = cardTokenService.detokenize(validTokenReference);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(validCardDetails.getCardNumber(), result.get().getCardNumber());
        verify(tokenManagementService).detokenize(validTokenReference);
    }

    @Test
    void detokenize_WithInvalidReference_ShouldReturnEmpty() {
        // Arrange
        String invalidReference = "invalid-reference";
        when(tokenManagementService.detokenize(invalidReference)).thenReturn(Optional.empty());

        // Act
        Optional<CardDetails> result = cardTokenService.detokenize(invalidReference);

        // Assert
        assertFalse(result.isPresent());
        verify(tokenManagementService).detokenize(invalidReference);
    }

    @Test
    void suspendToken_WithValidReference_ShouldReturnTrue() {
        // Arrange
        CardToken suspendedToken =
                CardToken.builder()
                        .tokenReference(validTokenReference)
                        .status(TokenStatus.SUSPENDED)
                        .build();

        when(tokenManagementService.updateTokenStatus(validTokenReference, TokenStatus.SUSPENDED))
                .thenReturn(Optional.of(suspendedToken));

        // Act
        boolean result = cardTokenService.suspendToken(validTokenReference);

        // Assert
        assertTrue(result);
        verify(tokenManagementService)
                .updateTokenStatus(validTokenReference, TokenStatus.SUSPENDED);
    }

    @Test
    void suspendToken_WithInvalidReference_ShouldReturnFalse() {
        // Arrange
        String invalidReference = "invalid-reference";
        when(tokenManagementService.updateTokenStatus(invalidReference, TokenStatus.SUSPENDED))
                .thenReturn(Optional.empty());

        // Act
        boolean result = cardTokenService.suspendToken(invalidReference);

        // Assert
        assertFalse(result);
        verify(tokenManagementService).updateTokenStatus(invalidReference, TokenStatus.SUSPENDED);
    }

    @Test
    void reactivateToken_WithValidReference_ShouldReturnTrue() {
        // Arrange
        CardToken reactivatedToken =
                CardToken.builder()
                        .tokenReference(validTokenReference)
                        .status(TokenStatus.ACTIVE)
                        .build();

        when(tokenManagementService.updateTokenStatus(validTokenReference, TokenStatus.ACTIVE))
                .thenReturn(Optional.of(reactivatedToken));

        // Act
        boolean result = cardTokenService.reactivateToken(validTokenReference);

        // Assert
        assertTrue(result);
        verify(tokenManagementService).updateTokenStatus(validTokenReference, TokenStatus.ACTIVE);
    }

    @Test
    void isTokenActive_WithActiveToken_ShouldReturnTrue() {
        // Arrange
        when(tokenManagementService.getToken(validTokenReference))
                .thenReturn(Optional.of(validCardToken));

        // Act
        boolean result = cardTokenService.isTokenActive(validTokenReference);

        // Assert
        assertTrue(result);
        verify(tokenManagementService).getToken(validTokenReference);
    }

    @Test
    void isTokenActive_WithSuspendedToken_ShouldReturnFalse() {
        // Arrange
        CardToken suspendedToken =
                CardToken.builder()
                        .tokenReference(validTokenReference)
                        .status(TokenStatus.SUSPENDED)
                        .build();

        when(tokenManagementService.getToken(validTokenReference))
                .thenReturn(Optional.of(suspendedToken));

        // Act
        boolean result = cardTokenService.isTokenActive(validTokenReference);

        // Assert
        assertFalse(result);
        verify(tokenManagementService).getToken(validTokenReference);
    }

    @Test
    void supportsPaymentMethod_WithSupportedMethod_ShouldReturnTrue() {
        // Arrange
        when(tokenManagementService.getToken(validTokenReference))
                .thenReturn(Optional.of(validCardToken));

        // Act
        boolean result =
                cardTokenService.supportsPaymentMethod(validTokenReference, PaymentMethod.VISA);

        // Assert
        assertTrue(result);
        verify(tokenManagementService).getToken(validTokenReference);
    }

    @Test
    void supportsPaymentMethod_WithUnsupportedMethod_ShouldReturnFalse() {
        // Arrange
        when(tokenManagementService.getToken(validTokenReference))
                .thenReturn(Optional.of(validCardToken));

        // Act
        boolean result =
                cardTokenService.supportsPaymentMethod(
                        validTokenReference, PaymentMethod.MASTERCARD);

        // Assert
        assertFalse(result);
        verify(tokenManagementService).getToken(validTokenReference);
    }
}
