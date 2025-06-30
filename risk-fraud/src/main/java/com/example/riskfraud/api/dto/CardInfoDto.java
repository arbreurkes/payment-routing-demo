package com.example.riskfraud.api.dto;

import com.example.shared.model.CardInfo;
import com.example.shared.model.PaymentMethod;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Currency;

/** DTO for card information in API requests/responses. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardInfoDto {
    @NotNull private PaymentMethod paymentMethod;

    private String bin;
    private String lastFour;
    private String issuer;
    private String country;
    private boolean debit;
    private boolean tokenized;
    private Currency currency;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardholderName;

    /** Converts this DTO to a domain model object. */
    public CardInfo toCardInfo() {
        return CardInfo.builder()
                .paymentMethod(this.paymentMethod)
                .bin(this.bin)
                .lastFour(this.lastFour)
                .issuer(this.issuer)
                .country(this.country)
                .debit(this.debit)
                .expiryMonth(this.expiryMonth)
                .expiryYear(this.expiryYear)
                .cardholderName(this.cardholderName)
                .build();
    }

    /** Creates a DTO from a domain model object. */
    public static CardInfoDto fromCardInfo(CardInfo cardInfo) {
        if (cardInfo == null) {
            return null;
        }
        return CardInfoDto.builder()
                .paymentMethod(cardInfo.getPaymentMethod())
                .bin(cardInfo.getBin())
                .lastFour(cardInfo.getLastFour())
                .issuer(cardInfo.getIssuer())
                .country(cardInfo.getCountry())
                .debit(cardInfo.isDebit())
                .expiryMonth(cardInfo.getExpiryMonth())
                .expiryYear(cardInfo.getExpiryYear())
                .cardholderName(cardInfo.getCardholderName())
                .build();
    }
}
