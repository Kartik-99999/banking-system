package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the transaction history (mini statement). */
public class Transaction {
    public enum Type {
        DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT, INTEREST,
        FD_OPEN, FD_CLOSE, LOAN_DISBURSE, LOAN_REPAY
    }

    private long txnId;
    private final long accountNo;
    private final Type type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;
    private final LocalDateTime createdAt;

    /** Used when recording a new transaction (id and time assigned by the DB). */
    public Transaction(long accountNo, Type type, BigDecimal amount,
                       BigDecimal balanceAfter, String description) {
        this(0, accountNo, type, amount, balanceAfter, description, null);
    }

    /** Used when reading a transaction back from the database. */
    public Transaction(long txnId, long accountNo, Type type, BigDecimal amount,
                       BigDecimal balanceAfter, String description, LocalDateTime createdAt) {
        this.txnId = txnId;
        this.accountNo = accountNo;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = createdAt;
    }

    public long getTxnId() {
        return txnId;
    }

    public long getAccountNo() {
        return accountNo;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
