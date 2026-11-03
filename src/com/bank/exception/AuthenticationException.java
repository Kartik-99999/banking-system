package com.bank.exception;

/** Thrown when the account number does not exist or the PIN is wrong. */
public class AuthenticationException extends BankException {
    public AuthenticationException(String message) {
        super(message);
    }
}
