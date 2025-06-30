package com.example.payment.model;

import com.example.shared.model.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a range of BINs that share the same characteristics. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinRange {
    private String startBin;
    private String endBin;
    private PaymentMethod paymentMethod;
    private String cardType;
    private String issuer;
    private String issuerName;
    private String countryCode;
    private String productType;
    private boolean prepaid;
    private boolean corporate;
    private boolean commercial;
}
