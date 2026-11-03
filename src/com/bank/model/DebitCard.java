package com.bank.model;

import java.time.LocalDateTime;

/** A debit card. Only the number + expiry are stored; the CVV is shown once and never kept. */
public class DebitCard {
    private int cardId;
    private final long accountNo;
    private final String cardNumber;
    private final String expiry;
    private final boolean active;
    private final LocalDateTime createdAt;

    public DebitCard(int cardId, long accountNo, String cardNumber, String expiry,
                     boolean active, LocalDateTime createdAt) {
        this.cardId = cardId;
        this.accountNo = accountNo;
        this.cardNumber = cardNumber;
        this.expiry = expiry;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String maskedNumber() {
        return "•••• •••• •••• " + cardNumber.substring(12);
    }

    public String groupedNumber() {
        return cardNumber.substring(0, 4) + " " + cardNumber.substring(4, 8) + " "
                + cardNumber.substring(8, 12) + " " + cardNumber.substring(12);
    }

    public int getCardId() {
        return cardId;
    }

    public long getAccountNo() {
        return accountNo;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getExpiry() {
        return expiry;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
