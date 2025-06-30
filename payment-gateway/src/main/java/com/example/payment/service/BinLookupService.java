package com.example.payment.service;

import com.example.payment.model.BinRange;
import com.example.payment.model.CardBinInfo;
import com.example.shared.model.PaymentMethod;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Service for looking up card information based on BIN (Bank Identification Number). */
@Slf4j
@Service
public class BinLookupService {
    // In-memory storage for BIN ranges
    private final ConcurrentMap<String, List<BinRange>> binRanges = new ConcurrentHashMap<>();
    private final List<BinRange> allRanges = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadDefaultBinRanges();
        // Sort all ranges by length of bin for more specific matches first
        allRanges.sort(Comparator.comparingInt(r -> -r.getStartBin().length()));
    }

    /**
     * Look up card information by BIN.
     *
     * @param bin The first 6-8 digits of the card number
     * @return List of CardBinInfo containing all matching card details, or empty list if not found
     */
    public List<CardBinInfo> lookup(String bin) {
        if (bin == null || bin.length() < 6 || bin.length() > 8) {
            return List.of();
        }

        List<CardBinInfo> matches = new ArrayList<>();

        // Find all matching ranges
        for (BinRange range : allRanges) {
            if (isBinInRange(bin, range.getStartBin(), range.getEndBin())) {
                matches.add(createCardBinInfo(bin, range));
            }
        }

        // If no exact range matches, try pattern matching
        if (matches.isEmpty()) {
            return findMatchingIinPattern(bin);
        }

        return matches;
    }

    private boolean isBinInRange(String bin, String startBin, String endBin) {
        // Pad with zeros for comparison if needed
        String paddedBin =
                bin.length() < startBin.length()
                        ? bin + "0".repeat(startBin.length() - bin.length())
                        : bin.substring(0, startBin.length());

        return paddedBin.compareTo(startBin) >= 0 && paddedBin.compareTo(endBin) <= 0;
    }

    private List<CardBinInfo> findMatchingIinPattern(String bin) {
        List<CardBinInfo> results = new ArrayList<>();

        // Common IIN patterns for major card networks
        if (bin.startsWith("4")) {
            results.add(createVisaBinInfo(bin));
        } else if (bin.matches("^5[1-5].*")) {
            results.add(createMastercardBinInfo(bin));
        } else if (bin.startsWith("34") || bin.startsWith("37")) {
            results.add(createAmexBinInfo(bin));
        } else if (bin.startsWith("6")) {
            results.add(createDiscoverBinInfo(bin));
        }

        return results;
    }

    private CardBinInfo createCardBinInfo(String bin, BinRange range) {
        return CardBinInfo.builder()
                .bin(bin)
                .paymentMethod(range.getPaymentMethod())
                .cardType(range.getCardType())
                .issuer(range.getIssuer())
                .issuerName(range.getIssuerName())
                .countryCode(range.getCountryCode())
                .productType(range.getProductType())
                .prepaid(range.isPrepaid())
                .corporate(range.isCorporate())
                .commercial(range.isCommercial())
                .build();
    }

    private CardBinInfo createVisaBinInfo(String bin) {
        return CardBinInfo.builder()
                .bin(bin)
                .paymentMethod(PaymentMethod.VISA)
                .cardType("CREDIT")
                .issuer("VISA")
                .issuerName("Visa")
                .countryCode("US")
                .prepaid(false)
                .corporate(false)
                .commercial(false)
                .build();
    }

    private CardBinInfo createMastercardBinInfo(String bin) {
        return CardBinInfo.builder()
                .bin(bin)
                .paymentMethod(PaymentMethod.MASTERCARD)
                .cardType("CREDIT")
                .issuer("MASTERCARD")
                .issuerName("Mastercard")
                .countryCode("US")
                .prepaid(false)
                .corporate(false)
                .commercial(false)
                .build();
    }

    private CardBinInfo createAmexBinInfo(String bin) {
        return CardBinInfo.builder()
                .bin(bin)
                .paymentMethod(PaymentMethod.AMEX)
                .cardType("CREDIT")
                .issuer("AMERICAN_EXPRESS")
                .issuerName("American Express")
                .countryCode("US")
                .prepaid(false)
                .corporate(bin.startsWith("37"))
                .commercial(false)
                .build();
    }

    private CardBinInfo createDiscoverBinInfo(String bin) {
        return CardBinInfo.builder()
                .bin(bin)
                .paymentMethod(PaymentMethod.DISCOVER)
                .cardType("CREDIT")
                .issuer("DISCOVER")
                .issuerName("Discover")
                .countryCode("US")
                .prepaid(false)
                .corporate(false)
                .commercial(false)
                .build();
    }

    private void loadDefaultBinRanges() {
        // In a real implementation, this would come from a database or external service
        // This is sample data for demonstration purposes

        // Visa
        allRanges.add(
                BinRange.builder()
                        .startBin("400000")
                        .endBin("499999")
                        .paymentMethod(PaymentMethod.VISA)
                        .cardType("CREDIT")
                        .issuer("VISA")
                        .issuerName("Visa")
                        .countryCode("US")
                        .build());

        // Mastercard
        allRanges.add(
                BinRange.builder()
                        .startBin("510000")
                        .endBin("559999")
                        .paymentMethod(PaymentMethod.MASTERCARD)
                        .cardType("CREDIT")
                        .issuer("MASTERCARD")
                        .issuerName("Mastercard")
                        .countryCode("US")
                        .build());

        // American Express
        allRanges.add(
                BinRange.builder()
                        .startBin("340000")
                        .endBin("349999")
                        .paymentMethod(PaymentMethod.AMEX)
                        .cardType("CREDIT")
                        .issuer("AMERICAN_EXPRESS")
                        .issuerName("American Express")
                        .countryCode("US")
                        .build());

        // Discover
        allRanges.add(
                BinRange.builder()
                        .startBin("601100")
                        .endBin("601109")
                        .paymentMethod(PaymentMethod.DISCOVER)
                        .cardType("CREDIT")
                        .issuer("DISCOVER")
                        .issuerName("Discover")
                        .countryCode("US")
                        .build());

        // Accel (debit network)
        allRanges.add(
                BinRange.builder()
                        .startBin("600000")
                        .endBin("600099")
                        .paymentMethod(PaymentMethod.ACCEL)
                        .cardType("DEBIT")
                        .issuer("ACCEL")
                        .issuerName("Accel")
                        .countryCode("US")
                        .build());

        // Star (debit network)
        allRanges.add(
                BinRange.builder()
                        .startBin("600110")
                        .endBin("600199")
                        .paymentMethod(PaymentMethod.STAR)
                        .cardType("DEBIT")
                        .issuer("STAR")
                        .issuerName("Star")
                        .countryCode("US")
                        .build());

        // NYCE (debit network)
        allRanges.add(
                BinRange.builder()
                        .startBin("600200")
                        .endBin("600299")
                        .paymentMethod(PaymentMethod.NYCE)
                        .cardType("DEBIT")
                        .issuer("NYCE")
                        .issuerName("NYCE")
                        .countryCode("US")
                        .build());

        // Pulse (debit network)
        allRanges.add(
                BinRange.builder()
                        .startBin("600300")
                        .endBin("600399")
                        .paymentMethod(PaymentMethod.PULSE)
                        .cardType("DEBIT")
                        .issuer("PULSE")
                        .issuerName("Pulse")
                        .countryCode("US")
                        .build());

        // Maestro (debit network, international)
        allRanges.add(
                BinRange.builder()
                        .startBin("500000")
                        .endBin("509999")
                        .paymentMethod(PaymentMethod.MAESTRO)
                        .cardType("DEBIT")
                        .issuer("MAESTRO")
                        .issuerName("Maestro")
                        .countryCode("GLOBAL")
                        .build());

        // Overlapping ranges for US Debit networks
        // This range is shared between Visa and Accel
        allRanges.add(
                BinRange.builder()
                        .startBin("453200")
                        .endBin("453299")
                        .paymentMethod(PaymentMethod.VISA)
                        .cardType("DEBIT")
                        .issuer("VISA")
                        .issuerName("Visa Debit")
                        .countryCode("US")
                        .build());

        // Accel network overlap with Visa
        allRanges.add(
                BinRange.builder()
                        .startBin("453200")
                        .endBin("453210")
                        .paymentMethod(PaymentMethod.ACCEL)
                        .cardType("DEBIT")
                        .issuer("BANK_OF_AMERICA")
                        .issuerName("Bank of America (Accel)")
                        .countryCode("US")
                        .build());

        // Mastercard Debit with NYCE overlap
        allRanges.add(
                BinRange.builder()
                        .startBin("520000")
                        .endBin("520099")
                        .paymentMethod(PaymentMethod.MASTERCARD)
                        .cardType("DEBIT")
                        .issuer("MASTERCARD")
                        .issuerName("Mastercard Debit")
                        .countryCode("US")
                        .build());

        // NYCE network overlap with Mastercard
        allRanges.add(
                BinRange.builder()
                        .startBin("520050")
                        .endBin("520099")
                        .paymentMethod(PaymentMethod.NYCE)
                        .cardType("DEBIT")
                        .issuer("CHASE")
                        .issuerName("Chase (NYCE)")
                        .countryCode("US")
                        .build());

        // Discover with Pulse overlap
        allRanges.add(
                BinRange.builder()
                        .startBin("601120")
                        .endBin("601129")
                        .paymentMethod(PaymentMethod.DISCOVER)
                        .cardType("DEBIT")
                        .issuer("DISCOVER")
                        .issuerName("Discover Debit")
                        .countryCode("US")
                        .build());

        // Pulse network overlap with Discover
        allRanges.add(
                BinRange.builder()
                        .startBin("601125")
                        .endBin("601129")
                        .paymentMethod(PaymentMethod.PULSE)
                        .cardType("DEBIT")
                        .issuer("WELLS_FARGO")
                        .issuerName("Wells Fargo (Pulse)")
                        .countryCode("US")
                        .build());
    }

    /**
     * Add or update a BIN range in the lookup service.
     *
     * @param range The BIN range to add or update
     */
    public void addOrUpdateBinRange(BinRange range) {
        String key = range.getStartBin().substring(0, 6);

        // Remove any existing range with the same start and end bins
        List<BinRange> ranges = binRanges.getOrDefault(key, new ArrayList<>());
        ranges.removeIf(
                r ->
                        r.getStartBin().equals(range.getStartBin())
                                && r.getEndBin().equals(range.getEndBin()));

        // Remove from allRanges as well
        allRanges.removeIf(
                r ->
                        r.getStartBin().equals(range.getStartBin())
                                && r.getEndBin().equals(range.getEndBin()));

        // Add the new/updated range
        binRanges.computeIfAbsent(key, k -> new ArrayList<>()).add(range);
        allRanges.add(range);

        // Resort the list
        allRanges.sort(Comparator.comparingInt(r -> -r.getStartBin().length()));
    }
}
