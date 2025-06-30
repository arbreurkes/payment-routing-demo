package com.example.payment.repository;

import com.example.payment.model.Payment;

import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of PaymentRepository for demo and testing purposes. In a production
 * environment, this would be replaced with a proper database implementation.
 */
@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> merchantReferences = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    @Override
    public Payment save(Payment payment) {
        // Only generate an ID if one doesn't already exist
        if (payment.getId() == null || payment.getId().isEmpty()) {
            payment.setId(generateId());
        }

        payments.put(payment.getId(), payment);

        // Track merchant references to enforce uniqueness
        String merchantId = payment.getMerchantId();
        String merchantReference = payment.getMerchantReference();

        if (merchantId != null && merchantReference != null) {
            merchantReferences
                    .computeIfAbsent(merchantId, k -> new HashSet<>())
                    .add(merchantReference);
        }

        return payment;
    }

    @Override
    public Optional<Payment> findById(String id) {
        return Optional.ofNullable(payments.get(id));
    }

    @Override
    public Optional<Payment> findByIdAndMerchantId(String id, String merchantId) {
        return findById(id).filter(p -> p.getMerchantId().equals(merchantId));
    }

    @Override
    public boolean existsByMerchantReferenceAndMerchantId(
            String merchantReference, String merchantId) {
        Set<String> references = merchantReferences.get(merchantId);
        return references != null && references.contains(merchantReference);
    }

    @Override
    public Optional<Payment> findByMerchantReferenceAndMerchantId(
            String merchantReference, String merchantId) {
        return payments.values().stream()
                .filter(
                        p ->
                                p.getMerchantId().equals(merchantId)
                                        && p.getMerchantReference().equals(merchantReference))
                .findFirst();
    }

    @Override
    public List<Payment> findAll() {
        return new ArrayList<>(payments.values());
    }

    @Override
    public void deleteById(String id) {
        Payment payment = payments.remove(id);
        if (payment != null) {
            Set<String> references = merchantReferences.get(payment.getMerchantId());
            if (references != null) {
                references.remove(payment.getMerchantReference());
            }
        }
    }

    @Override
    public void deleteAll() {
        payments.clear();
        merchantReferences.clear();
    }

    private String generateId() {
        return "PMT" + sequence.getAndIncrement();
    }
}
