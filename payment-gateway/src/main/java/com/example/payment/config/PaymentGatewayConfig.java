package com.example.payment.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the payment gateway application. This enables component scanning for all
 * required modules.
 */
@Configuration
@ComponentScan(
        basePackages = {
            "com.example.payment",
            "com.example.paymentrouting",
            "com.example.cardtoken",
            "com.example.cardnetwork",
            "com.example.riskfraud"
        })
public class PaymentGatewayConfig {
    // Configuration beans can be added here if needed
}
