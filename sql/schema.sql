-- Banking System database. Run once:  mysql -u root -p < sql/schema.sql
CREATE DATABASE IF NOT EXISTS bankdb;
USE bankdb;

CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    phone       VARCHAR(15)  NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounts (
    account_no  BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    account_type VARCHAR(10) NOT NULL,          -- SAVINGS or CURRENT
    pin_hash    VARCHAR(64) NOT NULL,           -- SHA-256 hex, never plain PIN
    balance     DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id)
);
-- Account numbers start at 100001 instead of 1.
ALTER TABLE accounts AUTO_INCREMENT = 100001;

CREATE TABLE IF NOT EXISTS transactions (
    txn_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no    BIGINT NOT NULL,
    txn_type      VARCHAR(15) NOT NULL,         -- DEPOSIT/WITHDRAW/TRANSFER_IN/TRANSFER_OUT/INTEREST
    amount        DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    description   VARCHAR(255),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_account FOREIGN KEY (account_no)
        REFERENCES accounts (account_no)
);

CREATE TABLE IF NOT EXISTS fixed_deposits (
    fd_id           INT AUTO_INCREMENT PRIMARY KEY,
    account_no      BIGINT NOT NULL,
    principal       DECIMAL(15, 2) NOT NULL,
    annual_rate     DECIMAL(5, 2) NOT NULL,
    tenure_months   INT NOT NULL,
    start_date      DATE NOT NULL,
    maturity_date   DATE NOT NULL,
    maturity_amount DECIMAL(15, 2) NOT NULL,
    status          VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    closed_at       TIMESTAMP NULL,
    CONSTRAINT fk_fd_account FOREIGN KEY (account_no)
        REFERENCES accounts (account_no)
);

CREATE TABLE IF NOT EXISTS loans (
    loan_id       INT AUTO_INCREMENT PRIMARY KEY,
    account_no    BIGINT NOT NULL,
    loan_type     VARCHAR(15) NOT NULL,         -- HOME / PERSONAL / EDUCATION
    principal     DECIMAL(15, 2) NOT NULL,
    annual_rate   DECIMAL(5, 2) NOT NULL,
    tenure_months INT NOT NULL,
    emi           DECIMAL(15, 2) NOT NULL,
    outstanding   DECIMAL(15, 2) NOT NULL,
    status        VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_account FOREIGN KEY (account_no)
        REFERENCES accounts (account_no)
);

CREATE TABLE IF NOT EXISTS loan_payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    loan_id    INT NOT NULL,
    amount     DECIMAL(15, 2) NOT NULL,
    paid_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_loan FOREIGN KEY (loan_id)
        REFERENCES loans (loan_id)
);

CREATE TABLE IF NOT EXISTS debit_cards (
    card_id     INT AUTO_INCREMENT PRIMARY KEY,
    account_no  BIGINT NOT NULL,
    card_number VARCHAR(16) NOT NULL UNIQUE,
    expiry      VARCHAR(5) NOT NULL,            -- MM/YY
    active      TINYINT(1) NOT NULL DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_account FOREIGN KEY (account_no)
        REFERENCES accounts (account_no)
);
