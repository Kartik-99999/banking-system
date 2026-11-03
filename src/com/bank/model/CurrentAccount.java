package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Current account: no minimum balance, allows overdraft up to Rs.10,000. */
public class CurrentAccount extends Account {
    public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("10000.00");

    public CurrentAccount(long accountNo, int customerId, String pinHash,
                          BigDecimal balance, LocalDateTime createdAt) {
        super(accountNo, customerId, pinHash, balance, createdAt);
    }

    @Override
    public String getAccountType() {
        return "CURRENT";
    }

    @Override
    public void validateWithdrawal(BigDecimal amount) throws InsufficientFundsException {
        BigDecimal after = balance.subtract(amount);
        if (after.compareTo(OVERDRAFT_LIMIT.negate()) < 0) {
            throw new InsufficientFundsException(
                    "Withdrawal denied. Overdraft limit is Rs." + OVERDRAFT_LIMIT);
        }
    }
}
