package com.example.payment.repository;

import com.example.payment.model.Payment;

import java.util.List;
import java.util.Optional;

/** Repository for accessing and managing Payment entities in memory. */
public interface PaymentRepository {

    /**
     * Saves a payment entity.
     *
     * @param payment the payment to save
     * @return the saved payment
     */
    Payment save(Payment payment);

    /**
     * Finds a payment by ID.
     *
     * @param id the payment ID
     * @return an Optional containing the payment if found
     */
    Optional<Payment> findById(String id);

    /**
     * Find a payment by ID and merchant ID.
     *
     * @param id the payment ID
     * @param merchantId the merchant ID
     * @return an Optional containing the payment if found
     */
    Optional<Payment> findByIdAndMerchantId(String id, String merchantId);

    /**
     * Check if a payment exists with the given merchant reference and merchant ID.
     *
     * @param merchantReference the merchant reference
     * @param merchantId the merchant ID
     * @return true if a payment exists, false otherwise
     */
    boolean existsByMerchantReferenceAndMerchantId(String merchantReference, String merchantId);

    /**
     * Find a payment by merchant reference and merchant ID.
     *
     * @param merchantReference the merchant reference
     * @param merchantId the merchant ID
     * @return an Optional containing the payment if found
     */
    Optional<Payment> findByMerchantReferenceAndMerchantId(
            String merchantReference, String merchantId);

    /**
     * Returns all payments.
     *
     * @return a list of all payments
     */
    List<Payment> findAll();

    /**
     * Deletes a payment by ID.
     *
     * @param id the payment ID
     */
    void deleteById(String id);

    /** Deletes all payments. */
    void deleteAll();
}
