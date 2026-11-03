package com.bank.service;

import com.bank.dao.AccountDAO;
import com.bank.dao.CardDAO;
import com.bank.dao.CustomerDAO;
import com.bank.dao.DatabaseConnection;
import com.bank.dao.FixedDepositDAO;
import com.bank.dao.LoanDAO;
import com.bank.dao.TransactionDAO;
import com.bank.exception.AuthenticationException;
import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.DebitCard;
import com.bank.model.FixedDeposit;
import com.bank.model.Loan;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.util.PasswordUtil;
import com.bank.util.ValidationUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Business logic: registration, login, deposits, withdrawals, transfers.
 * Money changes always run inside a DB transaction with rollback on failure.
 */
public class BankService {
    private final CustomerDAO customers = new CustomerDAO();
    private final AccountDAO accounts = new AccountDAO();
    private final TransactionDAO transactions = new TransactionDAO();
    private final FixedDepositDAO fds = new FixedDepositDAO();
    private final LoanDAO loans = new LoanDAO();
    private final CardDAO cards = new CardDAO();

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

    // ---------------- Fixed deposits ----------------

    /** Rate slab: 6.50% under 12 months, 7.00% up to 24, 7.50% beyond. */
    public static BigDecimal fdRateFor(int months) {
        if (months > 24) {
            return new BigDecimal("7.50");
        }
        if (months >= 12) {
            return new BigDecimal("7.00");
        }
        return new BigDecimal("6.50");
    }

    private static BigDecimal fdMaturity(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal factor = BigDecimal.ONE.add(
                annualRate.movePointLeft(2).multiply(new BigDecimal(months))
                        .divide(new BigDecimal(12), 10, RoundingMode.HALF_UP));
        return principal.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /** Opens an FD, locking the principal out of the balance. */
    public FixedDeposit openFD(long accountNo, BigDecimal principal, int months)
            throws BankException, SQLException {
        if (principal == null || principal.compareTo(new BigDecimal("5000")) < 0) {
            throw new IllegalArgumentException("FD principal must be at least Rs.5000.");
        }
        if (months < 6 || months > 60) {
            throw new IllegalArgumentException("FD tenure must be 6 to 60 months.");
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            account.validateWithdrawal(principal);
            BigDecimal rate = fdRateFor(months);
            LocalDate start = LocalDate.now();
            BigDecimal newBalance = account.getBalance().subtract(principal);
            con.setAutoCommit(false);
            try {
                accounts.updateBalance(con, accountNo, newBalance);
                int fdId = fds.create(con, new FixedDeposit(0, accountNo, principal, rate, months,
                        start, start.plusMonths(months), fdMaturity(principal, rate, months),
                        "OPEN", null));
                transactions.insert(con, new Transaction(accountNo, Transaction.Type.FD_OPEN,
                        principal, newBalance, "FD opened (" + months + " mo @ " + rate + "%)"));
                con.commit();
                return fds.findById(con, fdId);
            } catch (Exception e) {
                rollbackQuietly(con);
                throw e;
            }
        }
    }

    public List<FixedDeposit> listFDs(long accountNo) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return fds.findByAccountNo(con, accountNo);
        }
    }

    /**
     * Closes an FD. After maturity the full value is credited; early closure
     * pays 4% for completed months (principal only if under 3 months).
     * Returns the credited amount.
     */
    public BigDecimal closeFD(long accountNo, int fdId) throws BankException, SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            FixedDeposit fd = fds.findById(con, fdId);
            if (fd == null || fd.getAccountNo() != accountNo) {
                throw new BankException("FD not found.");
            }
            if (!"OPEN".equals(fd.getStatus())) {
                throw new BankException("FD is already closed.");
            }
            BigDecimal credit;
            String desc;
            if (!LocalDate.now().isBefore(fd.getMaturityDate())) {
                credit = fd.getMaturityAmount();
                desc = "FD matured";
            } else {
                long elapsed = ChronoUnit.MONTHS.between(fd.getStartDate(), LocalDate.now());
                if (elapsed < 3) {
                    credit = fd.getPrincipal();
                    desc = "FD closed early (no interest under 3 months)";
                } else {
                    credit = fdMaturity(fd.getPrincipal(), new BigDecimal("4.00"), (int) elapsed);
                    desc = "FD closed early @ 4%";
                }
            }
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            BigDecimal newBalance = account.getBalance().add(credit);
            con.setAutoCommit(false);
            try {
                accounts.updateBalance(con, accountNo, newBalance);
                fds.markClosed(con, fdId);
                transactions.insert(con, new Transaction(accountNo, Transaction.Type.FD_CLOSE,
                        credit, newBalance, desc));
                con.commit();
            } catch (Exception e) {
                rollbackQuietly(con);
                throw e;
            }
            return credit;
        }
    }

    // ---------------- Loans ----------------

    public static BigDecimal loanRateFor(String type) {
        if ("HOME".equals(type)) {
            return new BigDecimal("8.50");
        }
        if ("EDUCATION".equals(type)) {
            return new BigDecimal("9.00");
        }
        return new BigDecimal("12.00"); // PERSONAL
    }

    /** Standard EMI formula, rounded to paise. */
    public static BigDecimal emiFor(BigDecimal principal, BigDecimal annualRate, int months) {
        double r = annualRate.doubleValue() / 1200.0;
        double pow = Math.pow(1 + r, months);
        return BigDecimal.valueOf(principal.doubleValue() * r * pow / (pow - 1))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Instant loan up to 5x the balance; amount is credited immediately. */
    public Loan applyLoan(long accountNo, String loanType, BigDecimal principal, int months)
            throws BankException, SQLException {
        String type = loanType == null ? "" : loanType.trim().toUpperCase();
        Loan.Type t;
        try {
            t = Loan.Type.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loan type must be HOME, PERSONAL or EDUCATION.");
        }
        requirePositive(principal);
        if (months < 6 || months > 60) {
            throw new IllegalArgumentException("Loan tenure must be 6 to 60 months.");
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            BigDecimal maxLoan = account.getBalance().max(BigDecimal.ZERO)
                    .multiply(new BigDecimal("5")).setScale(2, RoundingMode.HALF_UP);
            if (principal.compareTo(maxLoan) > 0) {
                throw new BankException("Loan limit is 5x your balance (max Rs." + maxLoan + ").");
            }
            BigDecimal rate = loanRateFor(type);
            BigDecimal newBalance = account.getBalance().add(principal);
            con.setAutoCommit(false);
            try {
                accounts.updateBalance(con, accountNo, newBalance);
                int loanId = loans.create(con, new Loan(0, accountNo, t, principal, rate, months,
                        emiFor(principal, rate, months), principal, "ACTIVE", null));
                transactions.insert(con, new Transaction(accountNo, Transaction.Type.LOAN_DISBURSE,
                        principal, newBalance, type + " loan disbursed"));
                con.commit();
                return loans.findById(con, loanId);
            } catch (Exception e) {
                rollbackQuietly(con);
                throw e;
            }
        }
    }

    public List<Loan> listLoans(long accountNo) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return loans.findByAccountNo(con, accountNo);
        }
    }

    /** Repays part or all of a loan from the balance. Returns new outstanding. */
    public BigDecimal repayLoan(long accountNo, int loanId, BigDecimal amount)
            throws BankException, SQLException {
        requirePositive(amount);
        try (Connection con = DatabaseConnection.getConnection()) {
            Loan loan = loans.findById(con, loanId);
            if (loan == null || loan.getAccountNo() != accountNo) {
                throw new BankException("Loan not found.");
            }
            if (!"ACTIVE".equals(loan.getStatus())) {
                throw new BankException("Loan is already closed.");
            }
            if (amount.compareTo(loan.getOutstanding()) > 0) {
                throw new IllegalArgumentException(
                        "Amount exceeds outstanding Rs." + loan.getOutstanding());
            }
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            account.validateWithdrawal(amount);
            BigDecimal outstanding = loan.getOutstanding().subtract(amount);
            String status = outstanding.compareTo(BigDecimal.ZERO) == 0 ? "CLOSED" : "ACTIVE";
            BigDecimal newBalance = account.getBalance().subtract(amount);
            con.setAutoCommit(false);
            try {
                accounts.updateBalance(con, accountNo, newBalance);
                loans.updateOutstanding(con, loanId, outstanding, status);
                loans.addPayment(con, loanId, amount);
                transactions.insert(con, new Transaction(accountNo, Transaction.Type.LOAN_REPAY,
                        amount, newBalance, "Loan repayment"));
                con.commit();
            } catch (Exception e) {
                rollbackQuietly(con);
                throw e;
            }
            return outstanding;
        }
    }

    // ---------------- Debit cards ----------------

    /** Issue result. The CVV is shown once and never stored. */
    public static class CardIssue {
        private final String number;
        private final String expiry;
        private final String cvv;

        public CardIssue(String number, String expiry, String cvv) {
            this.number = number;
            this.expiry = expiry;
            this.cvv = cvv;
        }

        public String getNumber() {
            return number;
        }

        public String getExpiry() {
            return expiry;
        }

        public String getCvv() {
            return cvv;
        }
    }

    public CardIssue issueCard(long accountNo) throws BankException, SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            Account account = accounts.findByAccountNo(con, accountNo);
            requireExists(account);
            DebitCard existing = cards.findLatestByAccountNo(con, accountNo);
            if (existing != null && existing.isActive()) {
                throw new BankException("Block your current card before issuing a new one.");
            }
            LocalDate now = LocalDate.now();
            String expiry = String.format("%02d/%02d", now.getMonthValue(), (now.getYear() + 5) % 100);
            String cvv = String.format("%03d", new Random().nextInt(1000));
            String number = generateCardNumber();
            con.setAutoCommit(false);
            try {
                if (existing != null) {
                    cards.setActive(con, existing.getCardId(), false);
                }
                cards.create(con, new DebitCard(0, accountNo, number, expiry, true, null));
                con.commit();
            } catch (Exception e) {
                rollbackQuietly(con);
                throw e;
            }
            return new CardIssue(number, expiry, cvv);
        }
    }

    public DebitCard getCard(long accountNo) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return cards.findLatestByAccountNo(con, accountNo);
        }
    }

    public void setCardActive(long accountNo, boolean active) throws BankException, SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            DebitCard card = cards.findLatestByAccountNo(con, accountNo);
            if (card == null) {
                throw new BankException("No card issued yet.");
            }
            cards.setActive(con, card.getCardId(), active);
        }
    }

    /** 16-digit RuPay-style number with a valid Luhn check digit. */
    private static String generateCardNumber() {
        Random rnd = new Random();
        int[] d = new int[15];
        d[0] = 6;
        d[1] = 0;
        d[2] = 7;
        d[3] = 0;
        for (int i = 4; i < 15; i++) {
            d[i] = rnd.nextInt(10);
        }
        int sum = 0;
        for (int i = 0; i < 15; i++) {
            int digit = d[14 - i];
            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        sb.append((10 - (sum % 10)) % 10);
        return sb.toString();
    }
}
