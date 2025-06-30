package com.example.payment.service;

import com.example.payment.api.dto.CardPaymentRequest;
import com.example.payment.api.dto.PaymentResponse;
import com.example.payment.model.PaymentStatus;

import java.math.BigDecimal;

/** Service interface for payment operations. */
public interface PaymentService {

    /** Authorize a payment. */
    PaymentResponse authorize(CardPaymentRequest request, String merchantId);

    /** Capture a previously authorized payment. */
    PaymentResponse capture(String paymentId, BigDecimal amount, String merchantId);

    /** Modify an authorized payment (e.g., update amount). */
    PaymentResponse modify(String paymentId, BigDecimal newAmount, String merchantId);

    /** Cancel an authorized payment. */
    PaymentResponse cancel(String paymentId, String merchantId);

    /** Refund a captured payment. */
    PaymentResponse refund(String paymentId, BigDecimal amount, String merchantId);

    /** Get payment status. */
    PaymentStatus getPaymentStatus(String paymentId, String merchantId);
}
