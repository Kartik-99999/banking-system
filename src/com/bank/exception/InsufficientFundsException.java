package com.bank.exception;

/** Thrown when a withdrawal or transfer would breach the balance rule. */
public class InsufficientFundsException extends BankException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
