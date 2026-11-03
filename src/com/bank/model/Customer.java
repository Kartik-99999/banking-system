package com.bank.model;

import java.time.LocalDateTime;

/** Represents a bank customer (the account holder). */
public class Customer {
    private int customerId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDateTime createdAt;

    /** Used when registering a new customer (id assigned by the database). */
    public Customer(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    /** Used when reading a customer back from the database. */
    public Customer(int customerId, String fullName, String email, String phone, LocalDateTime createdAt) {
        this(fullName, email, phone);
        this.customerId = customerId;
        this.createdAt = createdAt;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + email + ")";
    }
}
