package com.example.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.cardnetwork.emulator.CardAuthorizationResult;
import com.example.cardnetwork.emulator.CardProcessor;
import com.example.cardtoken.model.CardToken;
import com.example.cardtoken.model.TokenStatus;
import com.example.cardtoken.service.CardTokenService;
import com.example.payment.api.dto.CardDetailsDto;
import com.example.payment.api.dto.CardPaymentRequest;
import com.example.payment.api.dto.PaymentResponse;
import com.example.payment.exception.PaymentNotFoundException;
import com.example.payment.model.Amount;
import com.example.payment.model.CardBinInfo;
import com.example.payment.model.Payment;
import com.example.payment.model.PaymentStatus;
import com.example.payment.repository.PaymentRepository;
import com.example.paymentrouting.model.RoutingResult;
import com.example.paymentrouting.service.PaymentRoutingService;
import com.example.riskfraud.model.RiskAssessment;
import com.example.riskfraud.model.RiskLevel;
import com.example.riskfraud.service.RiskAssessmentService;
import com.example.shared.model.CardDetails;
import com.example.shared.model.PaymentMethod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;

    @Mock private CardProcessor cardProcessor;

    @Mock private BinLookupService binLookupService;

    @Mock private PaymentRoutingService paymentRoutingService;

    @Mock private RiskAssessmentService riskAssessmentService;

    @Mock private CardTokenService cardTokenService;

    @InjectMocks private PaymentServiceImpl paymentService;

    private CardPaymentRequest cardPaymentRequest;
    private CardPaymentRequest tokenPaymentRequest;
    private CardDetails cardDetails;
    private CardToken cardToken;
    private Payment payment;
    private String merchantId;
    private String tokenReference;
    private String paymentId;
    private String transactionId;
    private List<CardBinInfo> binMatches;
    private RoutingResult routingResult;
    private CardAuthorizationResult authorizationResult;
    private RiskAssessment riskAssessment;

    @BeforeEach
    void setUp() {
        merchantId = "test-merchant-123";
        tokenReference = "token-reference-123";
        paymentId = "PMT1000";
        transactionId = UUID.randomUUID().toString();

        // Set up card payment request
        cardPaymentRequest = new CardPaymentRequest();
        cardPaymentRequest.setMerchantReference("test-payment-123");
        cardPaymentRequest.setAmount(BigDecimal.valueOf(100.00));
        cardPaymentRequest.setCurrency("USD");

        CardDetailsDto cardDetailsDto = new CardDetailsDto();
        cardDetailsDto.setCardNumber("4500123456789010");
        cardDetailsDto.setCardholderName("John Doe");
        cardDetailsDto.setExpiryMonth(12);
        cardDetailsDto.setExpiryYear(2030);
        cardDetailsDto.setCvv("123");
        cardPaymentRequest.setCardDetails(cardDetailsDto);

        // Set up token payment request
        tokenPaymentRequest = new CardPaymentRequest();
        tokenPaymentRequest.setMerchantReference("token-payment-123");
        tokenPaymentRequest.setAmount(BigDecimal.valueOf(200.00));
        tokenPaymentRequest.setCurrency("USD");
        tokenPaymentRequest.setTokenReference(tokenReference);

        // Set up card details
        cardDetails =
                CardDetails.builder()
                        .cardNumber("4500123456789010")
                        .cardholderName("John Doe")
                        .expiryMonth(12)
                        .expiryYear(2030)
                        .cvv("123")
                        .build();

        // Set up card token
        cardToken =
                CardToken.builder()
                        .tokenReference(tokenReference)
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

        // Set up bin matches
        CardBinInfo binInfo =
                CardBinInfo.builder()
                        .bin("450012")
                        .paymentMethod(PaymentMethod.VISA)
                        .cardType("CREDIT")
                        .issuer("VISA")
                        .issuerName("Visa")
                        .countryCode("US")
                        .build();
        binMatches = Collections.singletonList(binInfo);

        // Set up routing result
        routingResult =
                RoutingResult.builder()
                        .selectedPaymentMethod(PaymentMethod.VISA)
                        .estimatedCost(BigDecimal.valueOf(1.95))
                        .transactionAmount(BigDecimal.valueOf(100.00))
                        .currency("USD")
                        .build();

        // Set up authorization result
        authorizationResult =
                CardAuthorizationResult.success("123456", PaymentMethod.VISA, transactionId);

        // Set up risk assessment
        riskAssessment = RiskAssessment.builder().riskScore(0.2).riskLevel(RiskLevel.LOW).build();

        // Set up payment
        payment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference(cardPaymentRequest.getMerchantReference())
                        .amount(
                                Amount.of(
                                        cardPaymentRequest.getAmount(),
                                        cardPaymentRequest.getCurrency()))
                        .status(PaymentStatus.AUTHORIZED)
                        .authCode("123456")
                        .rrn("RRN12345678")
                        .selectedNetwork(PaymentMethod.VISA)
                        .routingCost(BigDecimal.valueOf(1.95))
                        .riskScore(0.2)
                        .riskLevel(RiskLevel.LOW)
                        .transactionId(transactionId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
    }

    @Test
    void authorize_WithValidCardDetails_ShouldReturnSuccessResponse() {
        // Arrange
        when(paymentRepository.existsByMerchantReferenceAndMerchantId(
                        cardPaymentRequest.getMerchantReference(), merchantId))
                .thenReturn(false);

        when(binLookupService.lookup(anyString())).thenReturn(binMatches);

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        when(riskAssessmentService.assessRisk(any())).thenReturn(riskAssessment);

        when(paymentRoutingService.findOptimalNetwork(any(BigDecimal.class), anyString(), anySet()))
                .thenReturn(routingResult);

        when(cardProcessor.authorize(
                        any(CardDetails.class),
                        any(BigDecimal.class),
                        anyString(),
                        any(PaymentMethod.class)))
                .thenReturn(authorizationResult);

        // Act
        PaymentResponse response = paymentService.authorize(cardPaymentRequest, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
        assertEquals(paymentId, response.getPaymentId());
        assertEquals("123456", response.getAuthCode());
        assertNotNull(response.getRrn());
        assertEquals(PaymentMethod.VISA.name(), response.getSelectedNetwork());
        assertEquals(BigDecimal.valueOf(1.95), response.getRoutingCost());

        // Simplified verification - just ensure critical services were called
        verify(paymentRepository)
                .existsByMerchantReferenceAndMerchantId(
                        eq(cardPaymentRequest.getMerchantReference()), eq(merchantId));
        verify(riskAssessmentService).assessRisk(any());
        verify(cardProcessor)
                .authorize(
                        any(CardDetails.class),
                        any(BigDecimal.class),
                        anyString(),
                        eq(PaymentMethod.VISA));
    }

    @Test
    void authorize_WithDuplicateMerchantReference_ShouldReturnErrorResponse() {
        // Arrange
        // Need to setup binLookupService or provide token to ensure we get past initial validation
        // Using token-based approach since it has less dependencies
        when(cardTokenService.getToken(anyString())).thenReturn(Optional.of(cardToken));

        when(cardTokenService.detokenize(anyString())).thenReturn(Optional.of(cardDetails));

        when(paymentRepository.existsByMerchantReferenceAndMerchantId(anyString(), anyString()))
                .thenReturn(true); // This should trigger the duplicate reference error

        // Create a token-based request to avoid BIN lookup
        CardPaymentRequest tokenRequest = new CardPaymentRequest();
        tokenRequest.setMerchantReference("test-payment-123");
        tokenRequest.setAmount(BigDecimal.valueOf(100.00));
        tokenRequest.setCurrency("USD");
        tokenRequest.setTokenReference(tokenReference);

        // Act
        PaymentResponse response = paymentService.authorize(tokenRequest, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertNotNull(response.getStatusMessage());
        assertTrue(
                response.getStatusMessage().toLowerCase().contains("duplicate")
                        || response.getStatusMessage().toLowerCase().contains("already exists"));

        // Verify the merchant reference check was performed
        verify(paymentRepository).existsByMerchantReferenceAndMerchantId(anyString(), anyString());

        // These shouldn't be called after duplicate check fails
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void authorize_WithInvalidBin_ShouldReturnErrorResponse() {
        // Arrange
        when(binLookupService.lookup(anyString())).thenReturn(Collections.emptyList());

        // Act
        PaymentResponse response = paymentService.authorize(cardPaymentRequest, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertNotNull(response.getStatusMessage());

        // Verify that BIN lookup is called, but merchant reference check is never reached
        // due to the exception thrown when bin matches are empty
        verify(binLookupService, atLeastOnce()).lookup(anyString());
        verify(paymentRepository, never())
                .existsByMerchantReferenceAndMerchantId(anyString(), anyString());
    }

    @Test
    void authorize_WithHighRisk_ShouldReturnErrorResponse() {
        // Arrange
        // Setup token-based payment to bypass card validation
        when(cardTokenService.getToken(anyString())).thenReturn(Optional.of(cardToken));

        when(cardTokenService.detokenize(anyString())).thenReturn(Optional.of(cardDetails));

        // Allow the payment to be created
        when(paymentRepository.existsByMerchantReferenceAndMerchantId(anyString(), anyString()))
                .thenReturn(false);

        Payment savedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference("test-reference")
                        .amount(Amount.of(BigDecimal.valueOf(100.00), "USD"))
                        .status(PaymentStatus.FAILED)
                        .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // This is the key part - return a high risk assessment
        RiskAssessment highRisk =
                RiskAssessment.builder().riskScore(0.9).riskLevel(RiskLevel.CRITICAL).build();

        when(riskAssessmentService.assessRisk(any())).thenReturn(highRisk);

        // Create a token-based request to ensure we reach the risk assessment
        CardPaymentRequest tokenRequest = new CardPaymentRequest();
        tokenRequest.setMerchantReference("test-payment-123");
        tokenRequest.setAmount(BigDecimal.valueOf(100.00));
        tokenRequest.setCurrency("USD");
        tokenRequest.setTokenReference(tokenReference);

        // Act
        PaymentResponse response = paymentService.authorize(tokenRequest, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertNotNull(response.getStatusMessage());
        assertTrue(response.getStatusMessage().toLowerCase().contains("risk"));

        // Verify risk assessment was performed
        verify(cardTokenService).getToken(anyString());
        verify(cardTokenService).detokenize(anyString());
        verify(paymentRepository).existsByMerchantReferenceAndMerchantId(anyString(), anyString());
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(riskAssessmentService).assessRisk(any());
    }

    @Test
    void authorize_WithValidToken_ShouldReturnSuccessResponse() {
        // Arrange
        when(paymentRepository.existsByMerchantReferenceAndMerchantId(
                        tokenPaymentRequest.getMerchantReference(), merchantId))
                .thenReturn(false);

        when(cardTokenService.getToken(tokenReference)).thenReturn(Optional.of(cardToken));

        when(cardTokenService.detokenize(tokenReference)).thenReturn(Optional.of(cardDetails));

        payment.setMerchantReference(tokenPaymentRequest.getMerchantReference());
        payment.getAmount().setValue(tokenPaymentRequest.getAmount());
        payment.setTokenReference(tokenReference);

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        when(riskAssessmentService.assessRisk(any())).thenReturn(riskAssessment);

        when(paymentRoutingService.findOptimalNetwork(any(BigDecimal.class), anyString(), anySet()))
                .thenReturn(routingResult);

        when(cardProcessor.authorize(
                        any(CardDetails.class),
                        any(BigDecimal.class),
                        anyString(),
                        any(PaymentMethod.class)))
                .thenReturn(authorizationResult);

        // Act
        PaymentResponse response = paymentService.authorize(tokenPaymentRequest, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
        assertEquals(paymentId, response.getPaymentId());
        assertEquals("123456", response.getAuthCode());
        assertNotNull(response.getRrn());
        assertEquals(PaymentMethod.VISA.name(), response.getSelectedNetwork());

        // Verify only essential interactions
        verify(paymentRepository)
                .existsByMerchantReferenceAndMerchantId(
                        eq(tokenPaymentRequest.getMerchantReference()), eq(merchantId));
        verify(cardTokenService).getToken(eq(tokenReference));
        verify(cardProcessor)
                .authorize(
                        any(CardDetails.class),
                        any(BigDecimal.class),
                        anyString(),
                        eq(PaymentMethod.VISA));
    }

    @Test
    void authorize_WithInvalidToken_ShouldReturnErrorResponse() {
        // Arrange
        // Setup a payment request with a token reference, but we'll have the token lookup return
        // empty
        when(cardTokenService.getToken(anyString())).thenReturn(Optional.empty());

        // We never reach the merchant reference check, so no need to mock it

        // Create a token-based request
        CardPaymentRequest tokenRequest = new CardPaymentRequest();
        tokenRequest.setMerchantReference("test-payment-123");
        tokenRequest.setAmount(BigDecimal.valueOf(100.00));
        tokenRequest.setCurrency("USD");
        tokenRequest.setTokenReference(tokenReference);

        // Act
        PaymentResponse response = paymentService.authorize(tokenRequest, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertNotNull(response.getStatusMessage());
        assertTrue(
                response.getStatusMessage().toLowerCase().contains("invalid token")
                        || response.getStatusMessage().toLowerCase().contains("token not found"));

        // Verify that token lookup is performed but we never reach merchant reference check
        verify(cardTokenService).getToken(anyString());
        verify(paymentRepository, never())
                .existsByMerchantReferenceAndMerchantId(anyString(), anyString());

        // These shouldn't be called after token validation fails
        verify(cardTokenService, never()).detokenize(anyString());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void authorize_WithExpiredToken_ShouldReturnErrorResponse() {
        // Arrange
        // Setup a token with EXPIRED status
        CardToken expiredToken =
                CardToken.builder()
                        .tokenReference(tokenReference)
                        .status(TokenStatus.EXPIRED)
                        .build();

        // Token lookup will return our expired token
        when(cardTokenService.getToken(anyString())).thenReturn(Optional.of(expiredToken));

        // Create a token-based request
        CardPaymentRequest tokenRequest = new CardPaymentRequest();
        tokenRequest.setMerchantReference("test-payment-123");
        tokenRequest.setAmount(BigDecimal.valueOf(100.00));
        tokenRequest.setCurrency("USD");
        tokenRequest.setTokenReference(tokenReference);

        // Act
        PaymentResponse response = paymentService.authorize(tokenRequest, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertNotNull(response.getStatusMessage());
        assertTrue(
                response.getStatusMessage().toLowerCase().contains("token is not active")
                        || response.getStatusMessage().toLowerCase().contains("expired"));

        // Verify that token lookup is performed but we never reach merchant reference check
        verify(cardTokenService).getToken(anyString());
        verify(paymentRepository, never())
                .existsByMerchantReferenceAndMerchantId(anyString(), anyString());

        // These shouldn't be called after token validation fails
        verify(cardTokenService, never()).detokenize(anyString());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void authorize_WithAuthorizationFailure_ShouldReturnErrorResponse() {
        // Arrange
        when(paymentRepository.existsByMerchantReferenceAndMerchantId(
                        cardPaymentRequest.getMerchantReference(), merchantId))
                .thenReturn(false);

        when(binLookupService.lookup(anyString())).thenReturn(binMatches);

        Payment savedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference(cardPaymentRequest.getMerchantReference())
                        .amount(
                                Amount.of(
                                        cardPaymentRequest.getAmount(),
                                        cardPaymentRequest.getCurrency()))
                        .status(PaymentStatus.FAILED)
                        .selectedNetwork(PaymentMethod.VISA)
                        .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        when(riskAssessmentService.assessRisk(any())).thenReturn(riskAssessment);

        when(paymentRoutingService.findOptimalNetwork(any(BigDecimal.class), anyString(), anySet()))
                .thenReturn(routingResult);

        CardAuthorizationResult failedAuth = CardAuthorizationResult.failed("Declined by issuer");

        when(cardProcessor.authorize(
                        any(CardDetails.class),
                        any(BigDecimal.class),
                        anyString(),
                        any(PaymentMethod.class)))
                .thenReturn(failedAuth);

        // Act
        PaymentResponse response = paymentService.authorize(cardPaymentRequest, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertNotNull(response.getStatusMessage());

        // Verify only essential interactions
        verify(paymentRepository)
                .existsByMerchantReferenceAndMerchantId(
                        eq(cardPaymentRequest.getMerchantReference()), eq(merchantId));
        verify(cardProcessor)
                .authorize(
                        any(CardDetails.class),
                        any(BigDecimal.class),
                        anyString(),
                        any(PaymentMethod.class));
    }

    @Test
    void capture_WithValidPayment_ShouldReturnSuccessResponse() {
        // Arrange
        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(payment));

        when(cardProcessor.capture(
                        transactionId,
                        payment.getAmount().getValue(),
                        payment.getAmount().getCurrency()))
                .thenReturn(true);

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        PaymentResponse response = paymentService.capture(paymentId, null, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.CAPTURED, response.getStatus());
        assertEquals(paymentId, response.getPaymentId());
        assertEquals("123456", response.getAuthCode());
        assertNotNull(response.getRrn());

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor)
                .capture(
                        transactionId,
                        payment.getAmount().getValue(),
                        payment.getAmount().getCurrency());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void capture_WithPartialAmount_ShouldReturnPartialCaptureResponse() {
        // Arrange
        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(payment));

        BigDecimal partialAmount = BigDecimal.valueOf(50.00);

        when(cardProcessor.capture(transactionId, partialAmount, payment.getAmount().getCurrency()))
                .thenReturn(true);

        Payment capturedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference(payment.getMerchantReference())
                        .amount(payment.getAmount())
                        .status(PaymentStatus.PARTIALLY_CAPTURED)
                        .authCode(payment.getAuthCode())
                        .rrn(payment.getRrn())
                        .selectedNetwork(payment.getSelectedNetwork())
                        .routingCost(payment.getRoutingCost())
                        .transactionId(payment.getTransactionId())
                        .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(capturedPayment);

        // Act
        PaymentResponse response = paymentService.capture(paymentId, partialAmount, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.PARTIALLY_CAPTURED, response.getStatus());
        assertEquals(paymentId, response.getPaymentId());
        assertEquals(partialAmount, response.getAmount());

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor)
                .capture(transactionId, partialAmount, payment.getAmount().getCurrency());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void capture_WithInvalidPaymentId_ShouldReturnErrorResponse() {
        // Arrange
        String invalidPaymentId = "PMT9999";
        when(paymentRepository.findByIdAndMerchantId(invalidPaymentId, merchantId))
                .thenReturn(Optional.empty());

        // Act
        PaymentResponse response = paymentService.capture(invalidPaymentId, null, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(response.getStatusMessage().contains("not found"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(invalidPaymentId, merchantId);
        verify(cardProcessor, never()).capture(anyString(), any(BigDecimal.class), anyString());
    }

    @Test
    void capture_WithNonAuthorizedPayment_ShouldReturnErrorResponse() {
        // Arrange
        Payment createdPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference("test-reference")
                        .amount(Amount.of(BigDecimal.valueOf(100.00), "USD"))
                        .status(PaymentStatus.CREATED)
                        .build();

        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(createdPayment));

        // Act
        PaymentResponse response = paymentService.capture(paymentId, null, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(
                response.getStatusMessage().contains("Only authorized payments can be captured"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor, never()).capture(anyString(), any(BigDecimal.class), anyString());
    }

    @Test
    void capture_WithCaptureFailure_ShouldReturnErrorResponse() {
        // Arrange
        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(payment));

        when(cardProcessor.capture(
                        transactionId,
                        payment.getAmount().getValue(),
                        payment.getAmount().getCurrency()))
                .thenReturn(false);

        // Act
        PaymentResponse response = paymentService.capture(paymentId, null, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(response.getStatusMessage().contains("Capture failed"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor)
                .capture(
                        transactionId,
                        payment.getAmount().getValue(),
                        payment.getAmount().getCurrency());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void modify_WithValidPayment_ShouldReturnSuccessResponse() {
        // Arrange
        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(payment));

        BigDecimal newAmount = BigDecimal.valueOf(150.00);

        Payment modifiedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference(payment.getMerchantReference())
                        .amount(Amount.of(newAmount, payment.getAmount().getCurrency()))
                        .status(payment.getStatus())
                        .authCode(payment.getAuthCode())
                        .rrn(payment.getRrn())
                        .selectedNetwork(payment.getSelectedNetwork())
                        .routingCost(payment.getRoutingCost())
                        .transactionId(payment.getTransactionId())
                        .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(modifiedPayment);

        // Act
        PaymentResponse response = paymentService.modify(paymentId, newAmount, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
        assertEquals(paymentId, response.getPaymentId());
        assertEquals(newAmount, response.getAmount());

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void modify_WithInvalidPaymentId_ShouldReturnErrorResponse() {
        // Arrange
        String invalidPaymentId = "PMT9999";
        when(paymentRepository.findByIdAndMerchantId(invalidPaymentId, merchantId))
                .thenReturn(Optional.empty());

        BigDecimal newAmount = BigDecimal.valueOf(150.00);

        // Act
        PaymentResponse response = paymentService.modify(invalidPaymentId, newAmount, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(response.getStatusMessage().contains("not found"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(invalidPaymentId, merchantId);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void modify_WithNonAuthorizedPayment_ShouldReturnErrorResponse() {
        // Arrange
        Payment capturedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference("test-reference")
                        .amount(Amount.of(BigDecimal.valueOf(100.00), "USD"))
                        .status(PaymentStatus.CAPTURED)
                        .build();

        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(capturedPayment));

        BigDecimal newAmount = BigDecimal.valueOf(150.00);

        // Act
        PaymentResponse response = paymentService.modify(paymentId, newAmount, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(
                response.getStatusMessage().contains("Only authorized payments can be modified"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void modify_WithNegativeAmount_ShouldReturnErrorResponse() {
        // Arrange
        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(payment));

        BigDecimal negativeAmount = BigDecimal.valueOf(-50.00);

        // Act
        PaymentResponse response = paymentService.modify(paymentId, negativeAmount, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(response.getStatusMessage().contains("New amount must be positive"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void cancel_WithValidPayment_ShouldReturnSuccessResponse() {
        // Arrange
        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(payment));

        when(cardProcessor.voidAuthorization(transactionId)).thenReturn(true);

        Payment cancelledPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference(payment.getMerchantReference())
                        .amount(payment.getAmount())
                        .status(PaymentStatus.CANCELLED)
                        .authCode(payment.getAuthCode())
                        .rrn(payment.getRrn())
                        .selectedNetwork(payment.getSelectedNetwork())
                        .routingCost(payment.getRoutingCost())
                        .transactionId(payment.getTransactionId())
                        .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(cancelledPayment);

        // Act
        PaymentResponse response = paymentService.cancel(paymentId, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.CANCELLED, response.getStatus());
        assertEquals(paymentId, response.getPaymentId());

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor).voidAuthorization(transactionId);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void cancel_WithInvalidPaymentId_ShouldReturnErrorResponse() {
        // Arrange
        String invalidPaymentId = "PMT9999";
        when(paymentRepository.findByIdAndMerchantId(invalidPaymentId, merchantId))
                .thenReturn(Optional.empty());

        // Act
        PaymentResponse response = paymentService.cancel(invalidPaymentId, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(response.getStatusMessage().contains("not found"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(invalidPaymentId, merchantId);
        verify(cardProcessor, never()).voidAuthorization(anyString());
    }

    @Test
    void cancel_WithNonAuthorizedPayment_ShouldReturnErrorResponse() {
        // Arrange
        Payment capturedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference("test-reference")
                        .amount(Amount.of(BigDecimal.valueOf(100.00), "USD"))
                        .status(PaymentStatus.CAPTURED)
                        .build();

        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(capturedPayment));

        // Act
        PaymentResponse response = paymentService.cancel(paymentId, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(
                response.getStatusMessage().contains("Only authorized payments can be cancelled"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor, never()).voidAuthorization(anyString());
    }

    @Test
    void cancel_WithVoidFailure_ShouldReturnErrorResponse() {
        // Arrange
        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(payment));

        when(cardProcessor.voidAuthorization(transactionId)).thenReturn(false);

        // Act
        PaymentResponse response = paymentService.cancel(paymentId, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(response.getStatusMessage().contains("Void failed"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor).voidAuthorization(transactionId);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refund_WithValidPayment_ShouldReturnSuccessResponse() {
        // Arrange
        Payment capturedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference("test-reference")
                        .amount(Amount.of(BigDecimal.valueOf(100.00), "USD"))
                        .status(PaymentStatus.CAPTURED)
                        .authCode("123456")
                        .rrn("RRN12345678")
                        .selectedNetwork(PaymentMethod.VISA)
                        .routingCost(BigDecimal.valueOf(1.95))
                        .transactionId(transactionId)
                        .build();

        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(capturedPayment));

        when(cardProcessor.refund(
                        transactionId,
                        capturedPayment.getAmount().getValue(),
                        capturedPayment.getAmount().getCurrency()))
                .thenReturn(true);

        Payment refundedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference(capturedPayment.getMerchantReference())
                        .amount(capturedPayment.getAmount())
                        .status(PaymentStatus.REFUNDED)
                        .authCode(capturedPayment.getAuthCode())
                        .rrn(capturedPayment.getRrn())
                        .selectedNetwork(capturedPayment.getSelectedNetwork())
                        .routingCost(capturedPayment.getRoutingCost())
                        .transactionId(capturedPayment.getTransactionId())
                        .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);

        // Act
        PaymentResponse response = paymentService.refund(paymentId, null, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertEquals(paymentId, response.getPaymentId());
        assertEquals(capturedPayment.getAmount().getValue(), response.getAmount());

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor)
                .refund(
                        transactionId,
                        capturedPayment.getAmount().getValue(),
                        capturedPayment.getAmount().getCurrency());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void refund_WithPartialAmount_ShouldReturnPartialRefundResponse() {
        // Arrange
        Payment capturedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference("test-reference")
                        .amount(Amount.of(BigDecimal.valueOf(100.00), "USD"))
                        .status(PaymentStatus.CAPTURED)
                        .authCode("123456")
                        .rrn("RRN12345678")
                        .selectedNetwork(PaymentMethod.VISA)
                        .routingCost(BigDecimal.valueOf(1.95))
                        .transactionId(transactionId)
                        .build();

        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(capturedPayment));

        BigDecimal partialAmount = BigDecimal.valueOf(50.00);

        when(cardProcessor.refund(
                        transactionId, partialAmount, capturedPayment.getAmount().getCurrency()))
                .thenReturn(true);

        Payment partiallyRefundedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference(capturedPayment.getMerchantReference())
                        .amount(capturedPayment.getAmount())
                        .status(PaymentStatus.PARTIALLY_REFUNDED)
                        .authCode(capturedPayment.getAuthCode())
                        .rrn(capturedPayment.getRrn())
                        .selectedNetwork(capturedPayment.getSelectedNetwork())
                        .routingCost(capturedPayment.getRoutingCost())
                        .transactionId(capturedPayment.getTransactionId())
                        .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(partiallyRefundedPayment);

        // Act
        PaymentResponse response = paymentService.refund(paymentId, partialAmount, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, response.getStatus());
        assertEquals(paymentId, response.getPaymentId());
        assertEquals(partialAmount, response.getAmount());

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor)
                .refund(transactionId, partialAmount, capturedPayment.getAmount().getCurrency());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void refund_WithInvalidPaymentId_ShouldReturnErrorResponse() {
        // Arrange
        String invalidPaymentId = "PMT9999";
        when(paymentRepository.findByIdAndMerchantId(invalidPaymentId, merchantId))
                .thenReturn(Optional.empty());

        // Act
        PaymentResponse response = paymentService.refund(invalidPaymentId, null, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(response.getStatusMessage().contains("not found"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(invalidPaymentId, merchantId);
        verify(cardProcessor, never()).refund(anyString(), any(BigDecimal.class), anyString());
    }

    @Test
    void refund_WithNonCapturedPayment_ShouldReturnErrorResponse() {
        // Arrange
        Payment authorizedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference("test-reference")
                        .amount(Amount.of(BigDecimal.valueOf(100.00), "USD"))
                        .status(PaymentStatus.AUTHORIZED)
                        .build();

        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(authorizedPayment));

        // Act
        PaymentResponse response = paymentService.refund(paymentId, null, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(response.getStatusMessage().contains("Only captured payments can be refunded"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor, never()).refund(anyString(), any(BigDecimal.class), anyString());
    }

    @Test
    void refund_WithRefundFailure_ShouldReturnErrorResponse() {
        // Arrange
        Payment capturedPayment =
                Payment.builder()
                        .id(paymentId)
                        .merchantId(merchantId)
                        .merchantReference("test-reference")
                        .amount(Amount.of(BigDecimal.valueOf(100.00), "USD"))
                        .status(PaymentStatus.CAPTURED)
                        .transactionId(transactionId)
                        .build();

        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(capturedPayment));

        when(cardProcessor.refund(
                        transactionId,
                        capturedPayment.getAmount().getValue(),
                        capturedPayment.getAmount().getCurrency()))
                .thenReturn(false);

        // Act
        PaymentResponse response = paymentService.refund(paymentId, null, merchantId);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertTrue(response.getStatusMessage().contains("Refund failed"));

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
        verify(cardProcessor)
                .refund(
                        transactionId,
                        capturedPayment.getAmount().getValue(),
                        capturedPayment.getAmount().getCurrency());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void getPaymentStatus_WithValidPaymentId_ShouldReturnCorrectStatus() {
        // Arrange
        when(paymentRepository.findByIdAndMerchantId(paymentId, merchantId))
                .thenReturn(Optional.of(payment));

        // Act
        PaymentStatus status = paymentService.getPaymentStatus(paymentId, merchantId);

        // Assert
        assertEquals(PaymentStatus.AUTHORIZED, status);

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(paymentId, merchantId);
    }

    @Test
    void getPaymentStatus_WithInvalidPaymentId_ShouldThrowException() {
        // Arrange
        String invalidPaymentId = "PMT9999";
        when(paymentRepository.findByIdAndMerchantId(invalidPaymentId, merchantId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                PaymentNotFoundException.class,
                () -> {
                    paymentService.getPaymentStatus(invalidPaymentId, merchantId);
                });

        // Verify interactions
        verify(paymentRepository).findByIdAndMerchantId(invalidPaymentId, merchantId);
    }
}
