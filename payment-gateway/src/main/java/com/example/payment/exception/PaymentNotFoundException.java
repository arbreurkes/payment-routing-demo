package com.example.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception thrown when a payment is not found. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PaymentNotFoundException extends PaymentException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
