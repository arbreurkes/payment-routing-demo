package com.example.cardnetwork.emulator;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the card network emulator module. This enables component scanning and
 * auto-wiring of the card processor implementation.
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.example.cardnetwork.emulator")
public class CardNetworkEmulatorAutoConfiguration {
    // Auto-configuration class to enable component scanning
}
