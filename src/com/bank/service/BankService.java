package com.bank.service;

import com.bank.dao.AccountDAO;
import com.bank.dao.CustomerDAO;
import com.bank.dao.DatabaseConnection;
import com.bank.dao.TransactionDAO;
import com.bank.exception.AuthenticationException;
import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.util.PasswordUtil;
import com.bank.util.ValidationUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Business logic: registration, login, deposits, withdrawals, transfers.
 * Money changes always run inside a DB transaction with rollback on failure.
 */
public class BankService {
    private final CustomerDAO customers = new CustomerDAO();
    private final AccountDAO accounts = new AccountDAO();
    private final TransactionDAO transactions = new TransactionDAO();

    /** A logged-in user: their account plus holder details. */
    public static class Session {
        private final Account account;
        private final Customer customer;

        public Session(Account account, Customer customer) {
            this.account = account;
            this.customer = customer;
        }

        public Account getAccount() {
            return account;
        }

        public Customer getCustomer() {
            return customer;
        }
    }

    /** Registers a customer and opens an account. Returns the new account number. */
    public long register(String fullName, String email, String phone, String type,
                         String pin, BigDecimal initialDeposit) throws BankException, SQLException {
        ValidationUtil.requireNonBlank(fullName, "Full name");
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePhone(phone);
        ValidationUtil.validatePin(pin);
        String accountType = type == null ? "" : type.trim().toUpperCase();
        if (!accountType.equals("SAVINGS") && !accountType.equals("CURRENT")) {
            throw new IllegalArgumentException("Account type must be SAVINGS or CURRENT.");
        }
        if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative.");
        }
        if (accountType.equals("SAVINGS") && initialDeposit.compareTo(SavingsAccount.MIN_BALANCE) < 0) {
            throw new BankException("Savings account needs a minimum opening deposit of Rs."
                    + SavingsAccount.MIN_BALANCE);
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            if (customers.findByEmail(con, email.trim()) != null) {
                throw new BankException("This email is already registered.");
            }
            con.setAutoCommit(false);
            try {
                int customerId = customers.create(con,
                        new Customer(fullName.trim(), email.trim(), phone.trim()));
                long accountNo = accounts.create(con, customerId, accountType,
                        PasswordUtil.sha256(pin), initialDeposit);
                if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
                    transactions.insert(con, new Transaction(accountNo, Transaction.Type.DEPOSIT,
                            initialDeposit, initialDeposit, "Opening deposit"));
                }
                con.commit();
                return accountNo;
            } catch (Exception e) {
                rollbackQuietly(con);
                throw e;
            }
        }
    }

    /** Logs in with account number + PIN. */
    public Session login(long accountNo, String pin) throws BankException, SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            Account account = accounts.findByAccountNo(con, accountNo);
            if (account == null) {
                throw new AuthenticationException("Account not found.");
            }
            if (!PasswordUtil.verify(pin, account.getPinHash())) {
                throw new AuthenticationException("Wrong PIN.");
            }
            Customer customer = customers.findById(con, account.getCustomerId());
            return new Session(account, customer);
        }
    }

    /** Deposits money, returns the new balance. */
    public BigDecimal deposit(long accountNo, BigDecimal amount) throws BankException, SQLException {
        requirePositive(amount);
        try (Connection con = DatabaseConnection.getConnection()) {
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            BigDecimal newBalance = account.getBalance().add(amount);
            con.setAutoCommit(false);
            try {
                accounts.updateBalance(con, accountNo, newBalance);
                transactions.insert(con, new Transaction(accountNo, Transaction.Type.DEPOSIT,
                        amount, newBalance, "Cash deposit"));
                con.commit();
            } catch (Exception e) {
                rollbackQuietly(con);
                throw e;
            }
            return newBalance;
        }
    }

    /** Withdraws money, returns the new balance. */
    public BigDecimal withdraw(long accountNo, BigDecimal amount) throws BankException, SQLException {
        requirePositive(amount);
        try (Connection con = DatabaseConnection.getConnection()) {
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            account.validateWithdrawal(amount);
            BigDecimal newBalance = account.getBalance().subtract(amount);
            con.setAutoCommit(false);
            try {
                accounts.updateBalance(con, accountNo, newBalance);
                transactions.insert(con, new Transaction(accountNo, Transaction.Type.WITHDRAW,
                        amount, newBalance, "Cash withdrawal"));
                con.commit();
            } catch (Exception e) {
                rollbackQuietly(con);
                throw e;
            }
            return newBalance;
        }
    }

    /**
     * Transfers money between two accounts. Both balance updates and both
     * history rows commit together, or all roll back on any failure.
     */
    public void transfer(long fromNo, long toNo, BigDecimal amount) throws BankException, SQLException {
        requirePositive(amount);
        if (fromNo == toNo) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                Account from = accounts.findByAccountNo(con, fromNo);
                Account to = accounts.findByAccountNo(con, toNo);
                if (from == null) {
                    throw new BankException("Source account not found.");
                }
                if (to == null) {
                    throw new BankException("Destination account " + toNo + " not found.");
                }
                from.validateWithdrawal(amount);
                BigDecimal fromNew = from.getBalance().subtract(amount);
                BigDecimal toNew = to.getBalance().add(amount);
                accounts.updateBalance(con, fromNo, fromNew);
                accounts.updateBalance(con, toNo, toNew);
                transactions.insert(con, new Transaction(fromNo, Transaction.Type.TRANSFER_OUT,
                        amount, fromNew, "Transfer to " + toNo));
                transactions.insert(con, new Transaction(toNo, Transaction.Type.TRANSFER_IN,
                        amount, toNew, "Transfer from " + fromNo));
                con.commit();
            } catch (Exception e) {
                rollbackQuietly(con);
                throw e;
            }
        }
    }

    /** Latest transactions first (mini statement). */
    public List<Transaction> history(long accountNo, int limit) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return transactions.findByAccountNo(con, accountNo, limit);
        }
    }

    /** Full account row by number (for the details page). */
    public Account getAccount(long accountNo) throws BankException, SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            return account;
        }
    }

    /** Fresh balance straight from the database. */
    public BigDecimal getBalance(long accountNo) throws BankException, SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            return account.getBalance();
        }
    }

    public void changePin(long accountNo, String oldPin, String newPin) throws BankException, SQLException {
        ValidationUtil.validatePin(newPin);
        try (Connection con = DatabaseConnection.getConnection()) {
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            if (!PasswordUtil.verify(oldPin, account.getPinHash())) {
                throw new AuthenticationException("Old PIN is wrong.");
            }
            accounts.updatePin(con, accountNo, PasswordUtil.sha256(newPin));
        }
    }

    /** Credits 4% interest. Savings accounts only. Returns the new balance. */
    public BigDecimal addInterest(long accountNo) throws BankException, SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            if (!(account instanceof SavingsAccount)) {
                throw new BankException("Interest applies to savings accounts only.");
            }
            BigDecimal interest = account.getBalance()
                    .multiply(SavingsAccount.INTEREST_RATE).setScale(2, RoundingMode.HALF_UP);
            if (interest.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BankException("No interest on zero balance.");
            }
            BigDecimal newBalance = account.getBalance().add(interest);
            con.setAutoCommit(false);
            try {
                accounts.updateBalance(con, accountNo, newBalance);
                transactions.insert(con, new Transaction(accountNo, Transaction.Type.INTEREST,
                        interest, newBalance, "Interest @ 4%"));
                con.commit();
            } catch (Exception e) {
                rollbackQuietly(con);
                throw e;
            }
            return newBalance;
        }
    }

    /** All accounts, for the admin panel. */
    public List<Account> getAllAccounts() throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return accounts.findAll(con);
        }
    }

    public Customer getCustomer(int customerId) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return customers.findById(con, customerId);
        }
    }

    private void requireExists(Account account) throws BankException {
        if (account == null) {
            throw new BankException("Account not found.");
        }
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
    }

    private void rollbackQuietly(Connection con) {
        try {
            con.rollback();
        } catch (SQLException ignored) {
            // Original error matters more than a rollback failure.
        }
    }
}
