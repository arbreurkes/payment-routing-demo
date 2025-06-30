package com.example.riskfraud.api.dto;

import com.example.riskfraud.model.Transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO for transaction risk assessment requests. */
@Data
public class TransactionRequest {
    private String transactionId;
    private String transactionReference;

    @NotNull private BigDecimal amount;

    @Valid private CardInfoDto cardInfo;

    @NotNull private String merchantId;

    private String merchantName;
    private String merchantCategoryCode;

    @NotNull private String userId;

    private String userEmail;
    private String userIpAddress;
    private String userDeviceId;
    private String billingAddress;
    private String shippingAddress;
    private String transactionChannel;
    private boolean isRecurring;
    private int previousChargebacks;
    private int previousSuccessfulTransactions;
    private int previousFailedTransactions;

    /** Converts this DTO to a Transaction domain object. */
    public Transaction toTransaction() {
        return Transaction.builder()
                .id(this.transactionId)
                .transactionReference(this.transactionReference)
                .amount(this.amount)
                .cardInfo(this.cardInfo != null ? this.cardInfo.toCardInfo() : null)
                .merchantId(this.merchantId)
                .merchantName(this.merchantName)
                .merchantCategoryCode(this.merchantCategoryCode)
                .userId(this.userId)
                .userEmail(this.userEmail)
                .userIpAddress(this.userIpAddress)
                .userDeviceId(this.userDeviceId)
                .billingAddress(this.billingAddress)
                .shippingAddress(this.shippingAddress)
                .transactionTime(LocalDateTime.now())
                .transactionChannel(this.transactionChannel)
                .isRecurring(this.isRecurring)
                .previousChargebacks(this.previousChargebacks)
                .previousSuccessfulTransactions(this.previousSuccessfulTransactions)
                .previousFailedTransactions(this.previousFailedTransactions)
                .build();
    }
}
