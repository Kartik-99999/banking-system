# Viva & Running Guide — Banking System

## 1. Running the project

### Prerequisites
- Java 17 or newer (`java -version` to check)
- MySQL running locally, with a `root` user (no password by default here —
  if yours has one, put it in `db.properties`)

### First-time setup (already done on this machine)
```bash
mysql -u root -p < sql/schema.sql   # creates bankdb + tables
```
- MySQL driver jar is already in `lib/`
- `db.properties` has the connection settings (`db.url`, `db.user`, `db.password`)

### Compile + run (terminal)
```bash
cd "/Users/kartikbhardwaj/IdeaProjects/Java Project"
javac -d out $(find src -name "*.java")   # no output = success
java -cp "out:lib/*" WebMain              # web app  -> http://localhost:8080
java -cp "out:lib/*" Main                 # desktop app (Swing)
```

### Run in IntelliJ
1. File → Project Structure → Modules → Dependencies → `+` JARs → select `lib/*.jar`
2. Run `Main` (desktop) or `WebMain` (web), open http://localhost:8080

### Logins
- Customers: open an account first, use the generated number (from 100001) + your PIN
- Admin: `admin` / `admin123` (web admin link, desktop Admin button)

### If something fails
| Symptom | Cause / fix |
|---|---|
| `MySQL driver not found` | jar not on classpath; use `-cp "out:lib/*"` or add it in IntelliJ |
| `Access denied for user 'root'` | set the real password in `db.properties` |
| `Unknown database 'bankdb'` | run the `schema.sql` step again |
| `Address already in use` | old server still running; stop it (Ctrl+C) or use `WebMain 9000` |
| Page not loading | server must be running AND open `http://localhost:8080` (not the file directly) |
| `javac` prints nothing | that means success; run the `java ...` command next |

---

## 2. Project summary (say this first in viva)
"A Banking System with two frontends — a Swing desktop app and a browser web
app — sharing one Java backend and one MySQL database. It supports account
opening, login with hashed PIN, deposit, withdraw, fund transfer, mini
statement, interest for savings accounts, PIN change, and an admin panel.
Transfers are atomic database transactions with rollback on failure."

## 3. Viva Q&A

### OOP
- **Inheritance?** `SavingsAccount` and `CurrentAccount` extend abstract `Account`.
- **Abstraction?** `Account` declares abstract `getAccountType()` /
  `validateWithdrawal()`; each subclass implements its own rule.
- **Encapsulation?** Fields are `private`/`protected` with getters/setters
  (e.g. `Customer`, `Account` balance).
- **Polymorphism?** `BankService` calls `account.validateWithdrawal(amount)`
  without knowing the subclass; the DAO rebuilds the right type via
  `Account.create(...)`. Also `toString()` overridden in `Customer`.
- **Why abstract class instead of interface?** Shared state (accountNo,
  balance…) plus a factory method live in one place.

### Database / JDBC
- **Tables?** `customers`, `accounts` (FK → customers), `transactions`
  (FK → accounts). Account numbers start at 100001 (`AUTO_INCREMENT`).
- **Why `PreparedStatement`?** Prevents SQL injection; precompiles SQL;
  handles quoting/types.
- **Transfer safety?** One connection, auto-commit off, two balance updates +
  two history rows commit together; any failure → `rollback()`.
- **Generated keys?** `Statement.RETURN_GENERATED_KEYS` returns the new
  customer_id / account_no after insert.
- **DAO pattern?** `CustomerDAO`/`AccountDAO`/`TransactionDAO` isolate SQL so
  the service layer never writes SQL.

### Money & validation
- **Why `BigDecimal`, not `double`?** Exact decimal arithmetic; `double`
  rounds badly (0.1 + 0.2 ≠ 0.3). Scale 2 = paise.
- **Business rules?** Savings keeps min Rs.1000; current allows Rs.10,000
  overdraft; savings opening deposit ≥ Rs.1000; 4% interest on savings only.
- **Validation?** Central `ValidationUtil`: 10-digit phone, email format,
  4–6 digit PIN, positive amounts.

### Security
- **PIN storage?** SHA-256 hash (`PasswordUtil`); DB never holds plain PINs;
  login compares hashes.
- **Web sessions?** Random UUID token per login, sent in the `X-Token`
  header; wrong/missing token → 401. Admin gets a separate token.
- **Honest limits (say if asked):** no HTTPS, no password salting, fixed
  admin credentials — fine for a college project, not production.

### Exceptions
- **Hierarchy?** `BankException` (checked) → `InsufficientFundsException`,
  `AuthenticationException`. Checked = caller must handle rule violations.
- **Unchecked?** `IllegalArgumentException` for bad input (empty name,
  negative amount). Web layer maps: auth → 401, bad input/rule → 400,
  unexpected → 500.

### Web
- **How without Spring/Node?** JDK's built-in `com.sun.net.httpserver`
  serves `web/` static files + a JSON API under `/api/`; hand-written
  `Json` helper, no libraries.
- **API examples?** `POST /api/register /api/login /api/transfer`,
  `GET /api/account /api/history /api/admin/accounts`.
- **Two UIs, one backend?** Swing frames and the REST server both call the
  same `BankService` + database — an account opened in the browser works
  on desktop.

### Layered design
`model` (data) → `dao` (SQL) → `service` (rules) → `ui`/`web` (screens).
UI never touches SQL; service never touches screens. That's why adding the
web frontend needed no changes to banking logic.

## 4. Demo script (5 minutes)
1. Open http://localhost:8080 → register a savings account → details
   receipt page (note the number) → dashboard.
2. Deposit → withdraw → show mini statement updating.
3. Register a second account, transfer between them, show both histories.
4. Try withdrawing below Rs.1000 → rule error; apply interest.
5. Admin login → all-accounts table. Mention the same data appears in the
   desktop app (`Main`).
