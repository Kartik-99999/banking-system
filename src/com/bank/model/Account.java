package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Abstract base for all account types.
 * Common state lives here; each subclass enforces its own balance rule.
 */
public abstract class Account {
    protected final long accountNo;
    protected final int customerId;
    protected String pinHash;
    protected BigDecimal balance;
    protected final LocalDateTime createdAt;

    protected Account(long accountNo, int customerId, String pinHash,
                      BigDecimal balance, LocalDateTime createdAt) {
        this.accountNo = accountNo;
        this.customerId = customerId;
        this.pinHash = pinHash;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    /** Rebuilds the right subclass from the account_type column. */
    public static Account create(String type, long accountNo, int customerId,
                                 String pinHash, BigDecimal balance, LocalDateTime createdAt) {
        if ("CURRENT".equalsIgnoreCase(type)) {
            return new CurrentAccount(accountNo, customerId, pinHash, balance, createdAt);
        }
        return new SavingsAccount(accountNo, customerId, pinHash, balance, createdAt);
    }

    public abstract String getAccountType();

    /** Each account type enforces its own withdrawal rule. */
    public abstract void validateWithdrawal(BigDecimal amount) throws InsufficientFundsException;

    public long getAccountNo() {
        return accountNo;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
