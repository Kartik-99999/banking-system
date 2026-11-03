package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A loan with EMI schedule; repaid in parts until outstanding hits zero. */
public class Loan {
    public enum Type {
        HOME, PERSONAL, EDUCATION
    }

    private int loanId;
    private final long accountNo;
    private final Type loanType;
    private final BigDecimal principal;
    private final BigDecimal annualRate;
    private final int tenureMonths;
    private final BigDecimal emi;
    private final BigDecimal outstanding;
    private final String status;
    private final LocalDateTime createdAt;

    public Loan(int loanId, long accountNo, Type loanType, BigDecimal principal,
                BigDecimal annualRate, int tenureMonths, BigDecimal emi,
                BigDecimal outstanding, String status, LocalDateTime createdAt) {
        this.loanId = loanId;
        this.accountNo = accountNo;
        this.loanType = loanType;
        this.principal = principal;
        this.annualRate = annualRate;
        this.tenureMonths = tenureMonths;
        this.emi = emi;
        this.outstanding = outstanding;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getLoanId() {
        return loanId;
    }

    public long getAccountNo() {
        return accountNo;
    }

    public Type getLoanType() {
        return loanType;
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

    public BigDecimal getEmi() {
        return emi;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
