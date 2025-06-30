package com.example.payment.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.payment.model.BinRange;
import com.example.payment.model.CardBinInfo;
import com.example.shared.model.PaymentMethod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class BinLookupServiceTest {

    private BinLookupService binLookupService;

    @BeforeEach
    void setUp() {
        binLookupService = new BinLookupService();
        binLookupService.init(); // Initialize with default BIN ranges
    }

    @Test
    void lookup_WithVisaBin_ShouldReturnVisaInfo() {
        // Arrange
        String visaBin = "450012";

        // Act
        List<CardBinInfo> results = binLookupService.lookup(visaBin);

        // Assert
        assertFalse(results.isEmpty());
        CardBinInfo binInfo = results.get(0);
        assertEquals(PaymentMethod.VISA, binInfo.getPaymentMethod());
        assertEquals(visaBin, binInfo.getBin());
    }

    @Test
    void lookup_WithMastercardBin_ShouldReturnMastercardInfo() {
        // Arrange
        String mastercardBin = "510012";

        // Act
        List<CardBinInfo> results = binLookupService.lookup(mastercardBin);

        // Assert
        assertFalse(results.isEmpty());
        CardBinInfo binInfo = results.get(0);
        assertEquals(PaymentMethod.MASTERCARD, binInfo.getPaymentMethod());
        assertEquals(mastercardBin, binInfo.getBin());
    }

    @Test
    void lookup_WithAmexBin_ShouldReturnAmexInfo() {
        // Arrange
        String amexBin = "340012";

        // Act
        List<CardBinInfo> results = binLookupService.lookup(amexBin);

        // Assert
        assertFalse(results.isEmpty());
        CardBinInfo binInfo = results.get(0);
        assertEquals(PaymentMethod.AMEX, binInfo.getPaymentMethod());
        assertEquals(amexBin, binInfo.getBin());
    }

    @Test
    void lookup_WithDiscoverBin_ShouldReturnDiscoverInfo() {
        // Arrange
        String discoverBin = "601112";

        // Act
        List<CardBinInfo> results = binLookupService.lookup(discoverBin);

        // Assert
        assertFalse(results.isEmpty());
        CardBinInfo binInfo = results.get(0);
        assertEquals(PaymentMethod.DISCOVER, binInfo.getPaymentMethod());
        assertEquals(discoverBin, binInfo.getBin());
    }

    @Test
    void lookup_WithInvalidBin_ShouldReturnEmptyList() {
        // Arrange
        String invalidBin = "123456"; // Not matching any known pattern

        // Act
        List<CardBinInfo> results = binLookupService.lookup(invalidBin);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void lookup_WithShortBin_ShouldReturnEmptyList() {
        // Arrange
        String shortBin = "12345"; // Less than 6 digits

        // Act
        List<CardBinInfo> results = binLookupService.lookup(shortBin);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void lookup_WithNullBin_ShouldReturnEmptyList() {
        // Act
        List<CardBinInfo> results = binLookupService.lookup(null);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void lookup_WithCustomBinRange_ShouldMatchCorrectly() {
        // Arrange
        BinRange customRange =
                BinRange.builder()
                        .startBin("700000")
                        .endBin("799999")
                        .paymentMethod(PaymentMethod.VISA)
                        .cardType("DEBIT")
                        .issuer("TEST_BANK")
                        .issuerName("Test Bank")
                        .countryCode("US")
                        .build();

        binLookupService.addOrUpdateBinRange(customRange);

        String testBin = "723456";

        // Act
        List<CardBinInfo> results = binLookupService.lookup(testBin);

        // Assert
        assertFalse(results.isEmpty());
        CardBinInfo binInfo = results.get(0);
        assertEquals(PaymentMethod.VISA, binInfo.getPaymentMethod());
        assertEquals("DEBIT", binInfo.getCardType());
        assertEquals("TEST_BANK", binInfo.getIssuer());
    }

    @Test
    void lookup_WithLongerBin_ShouldMatchCorrectly() {
        // Arrange
        String longerBin = "45001234"; // 8 digits

        // Act
        List<CardBinInfo> results = binLookupService.lookup(longerBin);

        // Assert
        assertFalse(results.isEmpty());
        CardBinInfo binInfo = results.get(0);
        assertEquals(PaymentMethod.VISA, binInfo.getPaymentMethod());
    }

    @Test
    void lookup_WithMultipleMatches_ShouldReturnAllMatches() {
        // Arrange
        // Add two overlapping BIN ranges
        BinRange range1 =
                BinRange.builder()
                        .startBin("800000")
                        .endBin("899999")
                        .paymentMethod(PaymentMethod.VISA)
                        .cardType("CREDIT")
                        .issuer("BANK_A")
                        .issuerName("Bank A")
                        .countryCode("US")
                        .build();

        BinRange range2 =
                BinRange.builder()
                        .startBin("800000")
                        .endBin("809999")
                        .paymentMethod(PaymentMethod.MASTERCARD)
                        .cardType("DEBIT")
                        .issuer("BANK_B")
                        .issuerName("Bank B")
                        .countryCode("US")
                        .build();

        binLookupService.addOrUpdateBinRange(range1);
        binLookupService.addOrUpdateBinRange(range2);

        String testBin = "800123";

        // Act
        List<CardBinInfo> results = binLookupService.lookup(testBin);

        // Assert
        assertEquals(2, results.size());
        boolean foundVisa = false;
        boolean foundMastercard = false;

        for (CardBinInfo info : results) {
            if (info.getPaymentMethod() == PaymentMethod.VISA) {
                foundVisa = true;
                assertEquals("BANK_A", info.getIssuer());
            } else if (info.getPaymentMethod() == PaymentMethod.MASTERCARD) {
                foundMastercard = true;
                assertEquals("BANK_B", info.getIssuer());
            }
        }

        assertTrue(foundVisa && foundMastercard);
    }

    @Test
    void addOrUpdateBinRange_ShouldUpdateExistingRange() {
        // Create a fresh BinLookupService for this test to avoid test interference
        binLookupService = new BinLookupService();
        binLookupService.init();

        // Arrange
        BinRange originalRange =
                BinRange.builder()
                        .startBin("900000")
                        .endBin("909999")
                        .paymentMethod(PaymentMethod.VISA)
                        .cardType("CREDIT")
                        .issuer("ORIGINAL")
                        .issuerName("Original Bank")
                        .countryCode("US")
                        .build();

        BinRange updatedRange =
                BinRange.builder()
                        .startBin("900000")
                        .endBin("909999")
                        .paymentMethod(PaymentMethod.MASTERCARD) // Changed payment method
                        .cardType("DEBIT") // Changed card type
                        .issuer("UPDATED")
                        .issuerName("Updated Bank")
                        .countryCode("UK")
                        .build();

        // Add original range
        binLookupService.addOrUpdateBinRange(originalRange);

        // Verify original addition
        List<CardBinInfo> originalResults = binLookupService.lookup("900123");
        assertEquals(PaymentMethod.VISA, originalResults.get(0).getPaymentMethod());
        assertEquals("ORIGINAL", originalResults.get(0).getIssuer());

        // Act - Update the range
        binLookupService.addOrUpdateBinRange(updatedRange);

        // Assert
        List<CardBinInfo> updatedResults = binLookupService.lookup("900123");
        assertEquals(PaymentMethod.MASTERCARD, updatedResults.get(0).getPaymentMethod());
        assertEquals("DEBIT", updatedResults.get(0).getCardType());
        assertEquals("UPDATED", updatedResults.get(0).getIssuer());
        assertEquals("UK", updatedResults.get(0).getCountryCode());
    }
}
