package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A fixed deposit: locked principal earning annual simple interest. */
public class FixedDeposit {
    private int fdId;
    private final long accountNo;
    private final BigDecimal principal;
    private final BigDecimal annualRate;
    private final int tenureMonths;
    private final LocalDate startDate;
    private final LocalDate maturityDate;
    private final BigDecimal maturityAmount;
    private String status;
    private LocalDateTime closedAt;

    public FixedDeposit(int fdId, long accountNo, BigDecimal principal, BigDecimal annualRate,
                        int tenureMonths, LocalDate startDate, LocalDate maturityDate,
                        BigDecimal maturityAmount, String status, LocalDateTime closedAt) {
        this.fdId = fdId;
        this.accountNo = accountNo;
        this.principal = principal;
        this.annualRate = annualRate;
        this.tenureMonths = tenureMonths;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.maturityAmount = maturityAmount;
        this.status = status;
        this.closedAt = closedAt;
    }

    public int getFdId() {
        return fdId;
    }

    public long getAccountNo() {
        return accountNo;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public BigDecimal getMaturityAmount() {
        return maturityAmount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}
