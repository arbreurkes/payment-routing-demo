package com.example.paymentrouting.config;

import com.example.paymentrouting.service.PaymentRoutingService;
import com.example.paymentrouting.service.impl.PaymentRoutingServiceImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration class for payment routing components. */
@Configuration
public class PaymentRoutingConfig {

    /**
     * Creates and configures the payment routing service.
     *
     * @return Configured PaymentRoutingService instance
     */
    @Bean
    public PaymentRoutingService paymentRoutingService() {
        return new PaymentRoutingServiceImpl();
    }
}
