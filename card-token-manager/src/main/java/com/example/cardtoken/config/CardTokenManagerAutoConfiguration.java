package com.example.cardtoken.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the card token manager module. This enables component scanning and
 * auto-wiring of the token management services.
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.example.cardtoken")
public class CardTokenManagerAutoConfiguration {
    // Auto-configuration class to enable component scanning
}
