package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Savings account: must keep a minimum balance, earns 4% interest. */
public class SavingsAccount extends Account {
    public static final BigDecimal MIN_BALANCE = new BigDecimal("1000.00");
    public static final BigDecimal INTEREST_RATE = new BigDecimal("0.04"); // 4%

    public SavingsAccount(long accountNo, int customerId, String pinHash,
                          BigDecimal balance, LocalDateTime createdAt) {
        super(accountNo, customerId, pinHash, balance, createdAt);
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public void validateWithdrawal(BigDecimal amount) throws InsufficientFundsException {
        if (balance.subtract(amount).compareTo(MIN_BALANCE) < 0) {
            throw new InsufficientFundsException(
                    "Withdrawal denied. Savings account must keep minimum balance Rs." + MIN_BALANCE);
        }
    }
}
