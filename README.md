# Banking System (Java + MySQL, Desktop + Web UI)

A 2nd-year university project with two frontends on one banking backend:
a Swing desktop app and a browser web app (JDK HttpServer + JSON API).
Features: login, deposits, withdrawals, transfers, mini statements,
interest, and an admin view. Built with OOP, JDBC, Swing, and HTTP.
Targets Java 17+.

## Features

- Open Savings / Current accounts (auto account number from 100001)
- Login with account number + hashed PIN (SHA-256, never stored plain)
- Deposit, withdraw, transfer between accounts (atomic DB transactions)
- Mini statement (last 50 transactions), balance enquiry
- 4% interest credit for savings accounts, change PIN
- Admin panel listing all accounts (`admin` / `admin123`)
- Rules: savings keeps min balance Rs.1000; current allows Rs.10,000 overdraft

## Concepts used (for viva)

OOP (inheritance `Account` → `SavingsAccount`/`CurrentAccount`, abstraction,
encapsulation), JDBC (`PreparedStatement`, generated keys, commit/rollback),
Swing (frames, dialogs, tables, event listeners), checked vs unchecked
exceptions, `BigDecimal` for money, SHA-256 hashing, layered design
(model → dao → service → ui).

## Project structure

```
src/Main.java                 entry point, opens the desktop login window
src/WebMain.java              entry point, starts the web app on http://localhost:8080
src/com/bank/model/          Customer, Account, SavingsAccount, CurrentAccount, Transaction
src/com/bank/exception/      BankException, InsufficientFundsException, AuthenticationException
src/com/bank/util/           PasswordUtil (SHA-256), ValidationUtil
src/com/bank/dao/            DatabaseConnection + DAOs (JDBC)
src/com/bank/service/        BankService (business rules + transactions)
src/com/bank/ui/             LoginFrame, RegisterDialog, DashboardFrame, HistoryDialog, AdminFrame
src/com/bank/web/            WebServer (REST API + static files), Json helper
web/                         browser frontend (index.html, app.js, styles.css)
sql/schema.sql               database + tables
db.properties                DB connection settings (edit password as needed)
lib/                         put mysql-connector-j.jar here
```

## Setup

1. **Database:** `mysql -u root -p < sql/schema.sql` (already done on this machine)
2. **Driver:** already in `lib/` (see `lib/README.md` for a fresh copy)
3. **Config:** edit `db.user` / `db.password` in `db.properties` if needed

## Run

Two frontends share the same backend and database — run either or both.

**Desktop (Swing):** run `Main` in IntelliJ (first add `lib/*.jar` via
File → Project Structure → Modules → Dependencies → + JARs).

**Web (browser):** run `WebMain`, then open http://localhost:8080.

**Terminal:**

```bash
javac -d out $(find src -name "*.java")
java -cp "out:lib/*" Main      # desktop (Windows: "out;lib/*")
java -cp "out:lib/*" WebMain   # web on port 8080 (WebMain 9000 for another port)
```

API: POST /api/register /api/login /api/deposit /api/withdraw
/api/transfer /api/change-pin /api/interest /api/admin/login,
GET /api/account /api/balance /api/history?limit=50 /api/admin/accounts.
Register also logs you in and returns the full account details for the receipt page.
Logged-in calls send the login token in the X-Token header.

## Deploy

Backend + MySQL on Render, static frontend on Vercel — see [DEPLOY.md](DEPLOY.md).

## Viva Q&A (short answers)

- **Why `BigDecimal`, not `double`?** Exact decimal math; doubles round badly.
- **Why `PreparedStatement`?** Prevents SQL injection, precompiles SQL.
- **Transfer safety?** One connection, auto-commit off, both updates + both
  history rows commit together, rollback on any failure.
- **Where is inheritance?** `SavingsAccount`/`CurrentAccount` extend abstract
  `Account` and override `validateWithdrawal`.
- **Checked vs unchecked?** `BankException` (checked) forces callers to handle
  rule violations; `IllegalArgumentException` (unchecked) for bad input.
- **PIN security?** SHA-256 hash in DB; login compares hashes, never plain PINs.
