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
