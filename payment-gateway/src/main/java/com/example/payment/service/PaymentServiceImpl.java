package com.example.payment.service;

import com.example.cardnetwork.emulator.CardAuthorizationResult;
import com.example.cardnetwork.emulator.CardProcessor;
import com.example.cardtoken.model.CardToken;
import com.example.cardtoken.service.CardTokenService;
import com.example.payment.api.dto.CardDetailsDto;
import com.example.payment.api.dto.CardPaymentRequest;
import com.example.payment.api.dto.PaymentResponse;
import com.example.payment.exception.PaymentException;
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
import com.example.riskfraud.model.Transaction;
import com.example.riskfraud.service.RiskAssessmentService;
import com.example.shared.model.CardDetails;
import com.example.shared.model.CardInfo;
import com.example.shared.model.PaymentMethod;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/** Implementation of the PaymentService interface. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final CardProcessor cardProcessor;
    private final BinLookupService binLookupService;
    private final PaymentRoutingService paymentRoutingService;
    private final RiskAssessmentService riskAssessmentService;
    private final CardTokenService cardTokenService;

    @Override
    public PaymentResponse authorize(CardPaymentRequest request, String merchantId) {
        try {
            // Validate the request format
            if (!request.isValid()) {
                throw new PaymentException(
                        "Request must contain either card details or token reference, but not both");
            }

            CardDetails cardDetails;
            Set<PaymentMethod> availableNetworks = new HashSet<>();
            CardToken token = null;

            // Handle token-based payment
            if (request.hasTokenReference()) {
                // Retrieve token and validate
                Optional<CardToken> tokenOpt =
                        cardTokenService.getToken(request.getTokenReference());
                if (tokenOpt.isEmpty()) {
                    throw new PaymentException(
                            "Invalid token reference: " + request.getTokenReference());
                }

                token = tokenOpt.get();
                if (!token.isActive()) {
                    throw new PaymentException(
                            "Token is not active: " + request.getTokenReference());
                }

                // Get card details from token
                Optional<CardDetails> cardDetailsOpt =
                        cardTokenService.detokenize(request.getTokenReference());
                if (cardDetailsOpt.isEmpty()) {
                    throw new PaymentException(
                            "Failed to detokenize card: " + request.getTokenReference());
                }

                cardDetails = cardDetailsOpt.get();

                // Use the payment methods from the token
                availableNetworks.addAll(token.getPaymentMethods());

                log.info("Processing payment with token {}", request.getTokenReference());
            } else {
                // Handle card details-based payment
                log.info("Processing payment with card details");
                // Validate card details
                validateCardDetails(request.getCardDetails());

                // Convert CardDetailsDto to CardDetails
                CardDetailsDto cardDetailsDto = request.getCardDetails();
                cardDetails =
                        CardDetails.builder()
                                .cardNumber(cardDetailsDto.getCardNumber())
                                .cardholderName(cardDetailsDto.getCardholderName())
                                .expiryMonth(cardDetailsDto.getExpiryMonth())
                                .expiryYear(cardDetailsDto.getExpiryYear())
                                .cvv(cardDetailsDto.getCvv())
                                .build();

                // Check if there's an existing token for this card
                List<CardToken> existingTokens =
                        cardTokenService.findValidTokensForCard(cardDetails);
                if (!existingTokens.isEmpty()) {
                    token = existingTokens.get(0);
                    log.info("Found existing token {} for card", token.getTokenReference());

                    // Add token networks to available networks
                    availableNetworks.addAll(token.getPaymentMethods());
                }

                // Look up BIN information to get available networks for the card
                String cardNumber = cardDetailsDto.getCardNumber();
                String bin = cardNumber.length() >= 6 ? cardNumber.substring(0, 6) : cardNumber;

                // Look up BIN information to get available networks for the card
                List<CardBinInfo> binMatches = binLookupService.lookup(bin);

                if (binMatches.isEmpty()) {
                    // If no BIN matches found, fail the payment
                    log.warn("No BIN matches found for {}, rejecting payment", bin);
                    throw new PaymentException("Invalid card number: No BIN information found");
                }

                // Add card networks to available networks
                binMatches.stream()
                        .map(CardBinInfo::getPaymentMethod)
                        .filter(Objects::nonNull)
                        .forEach(availableNetworks::add);
            }

            // Check for duplicate transaction
            if (paymentRepository.existsByMerchantReferenceAndMerchantId(
                    request.getMerchantReference(), merchantId)) {
                throw new PaymentException(
                        "Duplicate merchant reference: " + request.getMerchantReference());
            }

            // Create and save initial payment record
            Payment payment = createPaymentFromRequest(request, merchantId, token);
            payment = paymentRepository.save(payment);

            // Perform risk assessment
            Transaction transaction = createTransactionFromRequest(payment, request, merchantId);
            RiskAssessment riskAssessment = riskAssessmentService.assessRisk(transaction);

            // Update payment with risk assessment
            payment.setRiskScore(riskAssessment.getRiskScore());
            payment.setRiskLevel(riskAssessment.getRiskLevel());

            // Reject high-risk transactions
            if (riskAssessment.getRiskLevel() == RiskLevel.CRITICAL) {
                payment.updateStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new PaymentException("Transaction rejected due to high risk");
            }

            // Find optimal payment network based on cost
            RoutingResult routingResult =
                    paymentRoutingService.findOptimalNetwork(
                            request.getAmount(), request.getCurrency(), availableNetworks);

            if (!routingResult.hasValidOption()) {
                payment.updateStatus(PaymentStatus.FAILED);
                payment = paymentRepository.save(payment);
                return PaymentResponse.builder()
                        .paymentId(payment.getId())
                        .merchantReference(payment.getMerchantReference())
                        .amount(payment.getAmount().getValue())
                        .currency(payment.getAmount().getCurrency())
                        .status(PaymentStatus.FAILED)
                        .statusMessage("No valid routing options available for this transaction")
                        .usedToken(false)
                        .build();
            }

            // Update payment with selected network and routing info
            payment.setSelectedNetwork(routingResult.getSelectedPaymentMethod());
            payment.setRoutingCost(routingResult.getEstimatedCost());

            // Set token reference if the routing result indicates to use token and we have a token
            boolean useToken = routingResult.isUseToken() && token != null;
            if (useToken) {
                payment.setTokenReference(token.getTokenReference());
                log.info(
                        "Using token {} for payment based on routing decision",
                        token.getTokenReference());
            } else if (token != null) {
                log.info("Token available but not used based on routing decision");
            }

            payment = paymentRepository.save(payment);

            // Process the authorization with the card processor
            CardAuthorizationResult authResult;
            try {
                authResult =
                        cardProcessor.authorize(
                                cardDetails,
                                request.getAmount(),
                                payment.getId(),
                                routingResult.getSelectedPaymentMethod());
            } catch (Exception e) {
                // Handle processor errors
                payment.updateStatus(PaymentStatus.FAILED);
                payment = paymentRepository.save(payment);
                throw new PaymentException("Authorization failed: " + e.getMessage(), e);
            }

            // Update payment with authorization result
            if (authResult.isSuccess()) {
                payment.authorize(authResult.getAuthCode(), authResult.getRrn());
                payment.setTransactionId(authResult.getTransactionId());
                log.info(
                        "Payment {} authorized successfully via {}",
                        payment.getId(),
                        routingResult.getSelectedPaymentMethod());
            } else {
                payment.updateStatus(PaymentStatus.FAILED);
                log.warn(
                        "Payment {} authorization failed: {}",
                        payment.getId(),
                        authResult.getErrorMessage());
            }

            payment = paymentRepository.save(payment);

            // Build and return response
            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .merchantReference(payment.getMerchantReference())
                    .amount(payment.getAmount().getValue())
                    .currency(payment.getAmount().getCurrency())
                    .status(payment.getStatus())
                    .statusMessage(
                            authResult.isSuccess()
                                    ? "Payment authorized successfully"
                                    : authResult.getErrorMessage())
                    .timestamp(java.time.LocalDateTime.now())
                    .authCode(payment.getAuthCode())
                    .rrn(payment.getRrn())
                    .selectedNetwork(
                            payment.getSelectedNetwork() != null
                                    ? payment.getSelectedNetwork().name()
                                    : null)
                    .routingCost(payment.getRoutingCost())
                    .usedToken(useToken)
                    .build();
        } catch (PaymentException e) {
            log.error("Payment authorization failed: {}", e.getMessage());
            return PaymentResponse.builder()
                    .paymentId(null)
                    .merchantReference(request.getMerchantReference())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(PaymentStatus.FAILED)
                    .statusMessage(e.getMessage())
                    .usedToken(request.hasTokenReference())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error during payment authorization: {}", e.getMessage(), e);
            return PaymentResponse.builder()
                    .paymentId(null)
                    .merchantReference(request.getMerchantReference())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(PaymentStatus.FAILED)
                    .statusMessage("System error occurred")
                    .usedToken(request.hasTokenReference())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public PaymentResponse capture(String paymentId, BigDecimal amount, String merchantId) {
        Payment payment = null;
        try {
            // First, try to get the payment - this is the only repository call we should make
            Optional<Payment> paymentOpt =
                    paymentRepository.findByIdAndMerchantId(paymentId, merchantId);
            if (paymentOpt.isEmpty()) {
                throw new PaymentException("Payment not found: " + paymentId);
            }

            payment = paymentOpt.get();

            if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
                throw new PaymentException("Only authorized payments can be captured");
            }

            // Use the transaction ID from the payment for capture
            String transactionId = payment.getTransactionId();
            if (transactionId == null) {
                throw new PaymentException("No transaction ID found for payment: " + paymentId);
            }

            // If amount is null, capture the full authorized amount
            BigDecimal captureAmount = amount != null ? amount : payment.getAmount().getValue();

            // Validate capture amount
            if (captureAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PaymentException("Capture amount must be positive");
            }

            if (captureAmount.compareTo(payment.getAmount().getValue()) > 0) {
                throw new PaymentException(
                        "Capture amount cannot exceed the authorized amount: "
                                + payment.getAmount().getValue());
            }

            // Process capture through card processor
            boolean captureSuccess =
                    cardProcessor.capture(
                            transactionId, captureAmount, payment.getAmount().getCurrency());

            if (!captureSuccess) {
                throw new PaymentException("Capture failed for payment: " + paymentId);
            }

            // Update payment status
            if (captureAmount.compareTo(payment.getAmount().getValue()) == 0) {
                payment.updateStatus(PaymentStatus.CAPTURED);
            } else {
                payment.updateStatus(PaymentStatus.PARTIALLY_CAPTURED);
            }
            payment = paymentRepository.save(payment);

            // Build and return response
            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .merchantReference(payment.getMerchantReference())
                    .amount(captureAmount)
                    .currency(payment.getAmount().getCurrency())
                    .status(payment.getStatus())
                    .statusMessage("Payment processed successfully")
                    .timestamp(java.time.LocalDateTime.now())
                    .authCode(payment.getAuthCode())
                    .rrn(payment.getRrn())
                    .selectedNetwork(
                            payment.getSelectedNetwork() != null
                                    ? payment.getSelectedNetwork().name()
                                    : null)
                    .routingCost(payment.getRoutingCost())
                    .usedToken(payment.getTokenReference() != null)
                    .build();
        } catch (PaymentException e) {
            log.error("Payment capture failed: {}", e.getMessage());
            // Use the payment object we already retrieved if available
            if (payment != null) {
                return PaymentResponse.builder()
                        .paymentId(paymentId)
                        .merchantReference(payment.getMerchantReference())
                        .amount(payment.getAmount().getValue())
                        .currency(payment.getAmount().getCurrency())
                        .status(PaymentStatus.FAILED)
                        .statusMessage(e.getMessage())
                        .timestamp(java.time.LocalDateTime.now())
                        .usedToken(payment.getTokenReference() != null)
                        .build();
            } else {
                return PaymentResponse.builder()
                        .paymentId(paymentId)
                        .status(PaymentStatus.FAILED)
                        .statusMessage(e.getMessage())
                        .timestamp(java.time.LocalDateTime.now())
                        .usedToken(false)
                        .build();
            }
        } catch (Exception e) {
            log.error("Unexpected error during payment capture", e);
            // Use the payment object we already retrieved if available
            if (payment != null) {
                return PaymentResponse.builder()
                        .paymentId(paymentId)
                        .merchantReference(payment.getMerchantReference())
                        .amount(payment.getAmount().getValue())
                        .currency(payment.getAmount().getCurrency())
                        .status(PaymentStatus.FAILED)
                        .statusMessage("System error occurred")
                        .timestamp(java.time.LocalDateTime.now())
                        .usedToken(payment.getTokenReference() != null)
                        .build();
            } else {
                return PaymentResponse.builder()
                        .paymentId(paymentId)
                        .status(PaymentStatus.FAILED)
                        .statusMessage("System error occurred")
                        .timestamp(java.time.LocalDateTime.now())
                        .usedToken(false)
                        .build();
            }
        }
    }

    @Override
    public PaymentResponse modify(String paymentId, BigDecimal newAmount, String merchantId) {
        try {
            Payment payment = getPayment(paymentId, merchantId);

            if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
                throw new PaymentException("Only authorized payments can be modified");
            }

            if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PaymentException("New amount must be positive");
            }

            // Update payment amount
            payment.getAmount().setValue(newAmount);
            payment = paymentRepository.save(payment);

            return PaymentResponse.success(
                    payment.getId(),
                    payment.getMerchantReference(),
                    payment.getAmount().getValue(),
                    payment.getAmount().getCurrency(),
                    payment.getStatus(),
                    payment.getAuthCode(),
                    payment.getSelectedNetwork() != null
                            ? payment.getSelectedNetwork().name()
                            : null,
                    payment.getRoutingCost(),
                    payment.getRrn());
        } catch (Exception e) {
            log.error("Payment modification failed: {}", e.getMessage(), e);
            return PaymentResponse.error(paymentId, null, e.getMessage());
        }
    }

    @Override
    public PaymentResponse cancel(String paymentId, String merchantId) {
        try {
            Payment payment = getPayment(paymentId, merchantId);

            if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
                throw new PaymentException("Only authorized payments can be cancelled");
            }

            // Process void through card processor
            boolean success = cardProcessor.voidAuthorization(payment.getTransactionId());

            if (!success) {
                throw new PaymentException("Void failed for payment: " + paymentId);
            }

            // Update payment status
            payment.updateStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

            return PaymentResponse.success(
                    payment.getId(),
                    payment.getMerchantReference(),
                    payment.getAmount().getValue(),
                    payment.getAmount().getCurrency(),
                    payment.getStatus(),
                    payment.getAuthCode(),
                    payment.getSelectedNetwork() != null
                            ? payment.getSelectedNetwork().name()
                            : null,
                    payment.getRoutingCost(),
                    payment.getRrn());
        } catch (Exception e) {
            log.error("Payment cancellation failed: {}", e.getMessage(), e);
            return PaymentResponse.error(paymentId, null, e.getMessage());
        }
    }

    @Override
    public PaymentResponse refund(String paymentId, BigDecimal amount, String merchantId) {
        try {
            Payment payment = getPayment(paymentId, merchantId);

            if (payment.getStatus() != PaymentStatus.CAPTURED
                    && payment.getStatus() != PaymentStatus.PARTIALLY_CAPTURED) {
                throw new PaymentException("Only captured payments can be refunded");
            }

            // If amount is null, refund the full amount
            BigDecimal refundAmount = amount != null ? amount : payment.getAmount().getValue();

            // Process refund through card processor
            boolean success =
                    cardProcessor.refund(
                            payment.getTransactionId(),
                            refundAmount,
                            payment.getAmount().getCurrency());

            if (!success) {
                throw new PaymentException("Refund failed for payment: " + paymentId);
            }

            // Update payment status based on refund amount
            if (refundAmount.compareTo(payment.getAmount().getValue()) == 0) {
                payment.updateStatus(PaymentStatus.REFUNDED);
            } else {
                payment.updateStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            paymentRepository.save(payment);

            return PaymentResponse.success(
                    payment.getId(),
                    payment.getMerchantReference(),
                    refundAmount,
                    payment.getAmount().getCurrency(),
                    payment.getStatus(),
                    payment.getAuthCode(),
                    payment.getSelectedNetwork() != null
                            ? payment.getSelectedNetwork().name()
                            : null,
                    payment.getRoutingCost(),
                    payment.getRrn());
        } catch (Exception e) {
            log.error("Payment refund failed: {}", e.getMessage(), e);
            return PaymentResponse.error(paymentId, null, e.getMessage());
        }
    }

    @Override
    public PaymentStatus getPaymentStatus(String paymentId, String merchantId) {
        Payment payment = getPayment(paymentId, merchantId);
        return payment.getStatus();
    }

    /**
     * Helper method to create CardInfo from either card details or token reference
     *
     * @param request The payment request containing either card details or token reference
     * @param existingToken Optional token that was already retrieved (to avoid duplicate lookups)
     * @return CardInfo object with appropriate data
     */
    private CardInfo createCardInfoFromRequest(
            CardPaymentRequest request, CardToken existingToken) {
        if (request.hasCardDetails()) {
            CardDetailsDto cardDetails = request.getCardDetails();
            String cardNumber = cardDetails.getCardNumber();
            String bin = cardNumber.length() >= 6 ? cardNumber.substring(0, 6) : cardNumber;

            // Look up BIN information to get available networks for the card
            List<CardBinInfo> binMatches = binLookupService.lookup(bin);

            // Select the most appropriate BIN match
            CardBinInfo selectedBinInfo =
                    selectBestBinMatch(binMatches, bin, request.getAmount(), request.getCurrency());

            return CardInfo.builder()
                    .bin(bin)
                    .lastFour(cardNumber.substring(Math.max(0, cardNumber.length() - 4)))
                    .paymentMethod(selectedBinInfo.getPaymentMethod())
                    .debit("DEBIT".equals(selectedBinInfo.getCardType()))
                    .issuer(selectedBinInfo.getIssuer())
                    .country(selectedBinInfo.getCountryCode())
                    .expiryMonth(cardDetails.getExpiryMonth())
                    .expiryYear(cardDetails.getExpiryYear())
                    .cardholderName(cardDetails.getCardholderName())
                    .prepaid(selectedBinInfo.isPrepaid())
                    .corporate(selectedBinInfo.isCorporate())
                    .commercial(selectedBinInfo.isCommercial())
                    .productType(selectedBinInfo.getProductType())
                    .build();
        } else if (request.hasTokenReference()) {
            // For token-based payments, create card info from token
            CardToken token = existingToken;

            // Only look up the token if it wasn't provided
            if (token == null) {
                Optional<CardToken> tokenOpt =
                        cardTokenService.getToken(request.getTokenReference());
                if (tokenOpt.isPresent()) {
                    token = tokenOpt.get();
                } else {
                    return null; // Token not found
                }
            }

            // Use the first payment method from the token
            PaymentMethod paymentMethod = token.getPaymentMethods().iterator().next();

            return CardInfo.builder()
                    .bin(token.getTokenBin())
                    .lastFour(token.getLastFour())
                    .paymentMethod(paymentMethod)
                    .expiryMonth(token.getExpiryMonth())
                    .expiryYear(token.getExpiryYear())
                    .build();
        }

        // Fallback to null if neither card details nor valid token reference is present
        return null;
    }

    /** Creates a Transaction object from the payment request for risk assessment. */
    private Transaction createTransactionFromRequest(
            Payment payment, CardPaymentRequest request, String merchantId) {

        // Build and return the transaction using the CardInfo from the payment
        return Transaction.builder()
                .transactionReference(request.getMerchantReference())
                .amount(request.getAmount())
                .cardInfo(payment.getCardInfo())
                .merchantId(merchantId)
                .merchantName(
                        "Unknown") // This would come from merchant service in a real implementation
                .merchantCategoryCode("default") // This would come from merchant service
                .transactionTime(java.time.LocalDateTime.now())
                .transactionChannel("API") // Could be determined from request headers
                .build();
    }

    private Payment createPaymentFromRequest(
            CardPaymentRequest request, String merchantId, CardToken existingToken) {
        CardInfo cardInfo = createCardInfoFromRequest(request, existingToken);

        if (cardInfo == null) {
            throw new PaymentException("Failed to create card info from request");
        }

        // Create payment using the factory method
        return Payment.builder()
                .merchantId(merchantId)
                .merchantReference(request.getMerchantReference())
                .amount(Amount.of(request.getAmount(), request.getCurrency()))
                .cardInfo(cardInfo)
                .selectedNetwork(cardInfo.getPaymentMethod())
                .build();
    }

    /**
     * Selects the most appropriate BIN match from multiple possible matches using the payment
     * routing service. This ensures consistent routing decisions throughout the payment flow.
     *
     * @param binMatches List of potential BIN matches
     * @param bin The BIN to match
     * @param amount The transaction amount (optional)
     * @param currency The transaction currency (optional)
     * @return The selected CardBinInfo
     */
    private CardBinInfo selectBestBinMatch(
            List<CardBinInfo> binMatches, String bin, BigDecimal amount, String currency) {
        if (binMatches.isEmpty()) {
            // If no BIN matches found, fail the payment
            log.warn("No BIN matches found for {}, rejecting payment", bin);
            throw new PaymentException("Invalid card number: No BIN information found");
        }

        // Convert to set of payment methods
        Set<PaymentMethod> availableNetworks =
                binMatches.stream()
                        .map(CardBinInfo::getPaymentMethod)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        if (availableNetworks.isEmpty()) {
            // If no valid payment methods found, fail the payment
            log.warn("No valid payment methods found for BIN {}", bin);
            throw new PaymentException("Invalid card number: No valid payment methods found");
        }

        // Use default amount and currency if not provided
        BigDecimal routingAmount = amount != null ? amount : BigDecimal.valueOf(100);
        String routingCurrency = currency != null ? currency : "USD";

        // Use payment routing service to find optimal network
        RoutingResult routingResult =
                paymentRoutingService.findOptimalNetwork(
                        routingAmount, routingCurrency, availableNetworks);

        if (!routingResult.hasValidOption()) {
            // If no valid routing options, fail the payment
            log.warn("No valid routing options found for BIN {}", bin);
            throw new PaymentException("Invalid card number: No valid routing options found");
        }

        // Find the BIN info that matches the selected payment method
        PaymentMethod selectedMethod = routingResult.getSelectedPaymentMethod();

        // Find the most specific BIN match for the selected payment method
        return binMatches.stream()
                .filter(binInfo -> selectedMethod.equals(binInfo.getPaymentMethod()))
                .max(Comparator.comparingInt(b -> b.getBin() != null ? b.getBin().length() : 0))
                .orElse(
                        binMatches.get(
                                0)); // Fallback to first match if no match for selected method
    }

    private void validateCardDetails(CardDetailsDto cardDetails) {
        // Check if card is expired
        YearMonth expiryDate =
                YearMonth.of(cardDetails.getExpiryYear(), cardDetails.getExpiryMonth());
        if (expiryDate.isBefore(YearMonth.now())) {
            throw new PaymentException("Card has expired");
        }

        // Additional card validation can be added here (e.g., Luhn check)
    }

    private Payment getPayment(String paymentId, String merchantId) {
        return paymentRepository
                .findByIdAndMerchantId(paymentId, merchantId)
                .orElseThrow(
                        () ->
                                new PaymentNotFoundException(
                                        "Payment not found with id: " + paymentId));
    }
}
