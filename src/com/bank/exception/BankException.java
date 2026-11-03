package com.bank.exception;

/** Base checked exception for all banking rule violations. */
public class BankException extends Exception {
    public BankException(String message) {
        super(message);
    }
}
