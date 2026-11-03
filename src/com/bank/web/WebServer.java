package com.bank.web;

import com.bank.exception.AuthenticationException;
import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.Transaction;
import com.bank.service.BankService;
import com.bank.util.ValidationUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Web backend using only the JDK's built-in HTTP server.
 * Serves the browser UI from web/ plus a JSON API under /api/.
 * Both the desktop (Swing) and web UIs share the same BankService + database.
 */
public class WebServer {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final long ADMIN = -1L;
    private static final String BANK_NAME = "Campus National Bank";
    private static final String BRANCH = "Main Branch, Gurugram";

    private final BankService service = new BankService();
    private final Map<String, Long> sessions = new ConcurrentHashMap<>(); // token -> accountNo (-1 = admin)
    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final Path webDir;

    public WebServer(int port) throws IOException {
        this(port, Paths.get("web"));
    }

    public WebServer(int port, Path webDir) throws IOException {
        this.webDir = webDir;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    // ---------------- API ----------------

    private void handleApi(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (path.equals("/api/register") && method.equals("POST")) {
                doRegister(ex);
            } else if (path.equals("/api/login") && method.equals("POST")) {
                doLogin(ex);
            } else if (path.equals("/api/admin/login") && method.equals("POST")) {
                doAdminLogin(ex);
            } else if (path.equals("/api/account") && method.equals("GET")) {
                doAccount(ex);
            } else if (path.equals("/api/balance") && method.equals("GET")) {
                doBalance(ex);
            } else if (path.equals("/api/deposit") && method.equals("POST")) {
                doDeposit(ex);
            } else if (path.equals("/api/withdraw") && method.equals("POST")) {
                doWithdraw(ex);
            } else if (path.equals("/api/transfer") && method.equals("POST")) {
                doTransfer(ex);
            } else if (path.equals("/api/history") && method.equals("GET")) {
                doHistory(ex);
            } else if (path.equals("/api/change-pin") && method.equals("POST")) {
                doChangePin(ex);
            } else if (path.equals("/api/interest") && method.equals("POST")) {
                doInterest(ex);
            } else if (path.equals("/api/admin/accounts") && method.equals("GET")) {
                doAdminAccounts(ex);
            } else {
                send(ex, 404, "{\"ok\":false,\"error\":\"Unknown API.\"}");
            }
        } catch (AuthenticationException e) {
            send(ex, 401, "{\"ok\":false,\"error\":" + Json.quote(e.getMessage()) + "}");
        } catch (IllegalArgumentException | BankException e) {
            send(ex, 400, "{\"ok\":false,\"error\":" + Json.quote(e.getMessage()) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"ok\":false,\"error\":\"Server error. Please try again.\"}");
        }
    }

    private void doRegister(HttpExchange ex) throws Exception {
        Map<String, String> body = readJson(ex);
        String depositRaw = body.getOrDefault("deposit", "0").trim();
        BigDecimal deposit;
        try {
            deposit = depositRaw.isEmpty() ? BigDecimal.ZERO : new BigDecimal(depositRaw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid deposit. Enter a number like 1000.");
        }
        String pin = body.get("pin");
        long accountNo = service.register(body.get("fullName"), body.get("email"),
                body.get("phone"), body.get("type"), pin, deposit);
        BankService.Session session = service.login(accountNo, pin); // auto-login after opening
        String token = UUID.randomUUID().toString();
        sessions.put(token, accountNo);
        send(ex, 200, "{\"ok\":true,\"token\":" + Json.quote(token) + ","
                + accountFields(session.getAccount(), session.getCustomer()) + "}");
    }

    private void doLogin(HttpExchange ex) throws Exception {
        Map<String, String> body = readJson(ex);
        long accountNo = ValidationUtil.parseAccountNo(body.get("accountNo"));
        BankService.Session session = service.login(accountNo, body.get("pin"));
        String token = UUID.randomUUID().toString();
        sessions.put(token, accountNo);
        send(ex, 200, "{\"ok\":true,"
                + "\"token\":" + Json.quote(token) + ","
                + "\"name\":" + Json.quote(session.getCustomer().getFullName()) + ","
                + "\"accountNo\":" + accountNo + ","
                + "\"type\":" + Json.quote(session.getAccount().getAccountType()) + ","
                + "\"balance\":" + Json.quote(session.getAccount().getBalance().toPlainString()) + "}");
    }

    private void doAccount(HttpExchange ex) throws Exception {
        long accountNo = requireUser(ex);
        Account account = service.getAccount(accountNo);
        Customer customer = service.getCustomer(account.getCustomerId());
        send(ex, 200, "{\"ok\":true," + accountFields(account, customer) + "}");
    }

    /** Shared detail fields for the account-opening receipt page. */
    private String accountFields(Account a, Customer c) {
        return "\"bank\":" + Json.quote(BANK_NAME)
                + ",\"branch\":" + Json.quote(BRANCH)
                + ",\"customerId\":" + (c == null ? 0 : c.getCustomerId())
                + ",\"accountNo\":" + a.getAccountNo()
                + ",\"name\":" + Json.quote(c == null ? "?" : c.getFullName())
                + ",\"phone\":" + Json.quote(c == null ? "" : c.getPhone())
                + ",\"email\":" + Json.quote(c == null ? "" : c.getEmail())
                + ",\"type\":" + Json.quote(a.getAccountType())
                + ",\"balance\":" + Json.quote(a.getBalance().toPlainString())
                + ",\"opened\":" + Json.quote(a.getCreatedAt() == null ? "" : a.getCreatedAt().format(FORMAT));
    }

    private void doBalance(HttpExchange ex) throws Exception {
        long accountNo = requireUser(ex);
        send(ex, 200, "{\"ok\":true,\"balance\":"
                + Json.quote(service.getBalance(accountNo).toPlainString()) + "}");
    }

    private void doDeposit(HttpExchange ex) throws Exception {
        long accountNo = requireUser(ex);
        BigDecimal amount = ValidationUtil.parseAmount(readJson(ex).get("amount"));
        BigDecimal updated = service.deposit(accountNo, amount);
        send(ex, 200, "{\"ok\":true,\"balance\":" + Json.quote(updated.toPlainString()) + "}");
    }

    private void doWithdraw(HttpExchange ex) throws Exception {
        long accountNo = requireUser(ex);
        BigDecimal amount = ValidationUtil.parseAmount(readJson(ex).get("amount"));
        BigDecimal updated = service.withdraw(accountNo, amount);
        send(ex, 200, "{\"ok\":true,\"balance\":" + Json.quote(updated.toPlainString()) + "}");
    }

    private void doTransfer(HttpExchange ex) throws Exception {
        long from = requireUser(ex);
        Map<String, String> body = readJson(ex);
        long to = ValidationUtil.parseAccountNo(body.get("to"));
        BigDecimal amount = ValidationUtil.parseAmount(body.get("amount"));
        service.transfer(from, to, amount);
        send(ex, 200, "{\"ok\":true,\"balance\":"
                + Json.quote(service.getBalance(from).toPlainString()) + "}");
    }

    private void doHistory(HttpExchange ex) throws Exception {
        long accountNo = requireUser(ex);
        int limit = 50;
        String query = ex.getRequestURI().getQuery();
        if (query != null) {
            for (String part : query.split("&")) {
                if (part.startsWith("limit=")) {
                    try {
                        limit = Math.min(200, Math.max(1, Integer.parseInt(part.substring(6))));
                    } catch (NumberFormatException ignored) {
                        // Keep default limit.
                    }
                }
            }
        }
        StringBuilder out = new StringBuilder("{\"ok\":true,\"transactions\":[");
        List<Transaction> txns = service.history(accountNo, limit);
        for (int i = 0; i < txns.size(); i++) {
            Transaction t = txns.get(i);
            if (i > 0) {
                out.append(',');
            }
            out.append("{\"id\":").append(t.getTxnId())
                    .append(",\"date\":").append(Json.quote(
                            t.getCreatedAt() == null ? "" : t.getCreatedAt().format(FORMAT)))
                    .append(",\"type\":").append(Json.quote(t.getType().name()))
                    .append(",\"amount\":").append(Json.quote(t.getAmount().toPlainString()))
                    .append(",\"balanceAfter\":").append(Json.quote(t.getBalanceAfter().toPlainString()))
                    .append(",\"description\":").append(Json.quote(t.getDescription()))
                    .append('}');
        }
        out.append("]}");
        send(ex, 200, out.toString());
    }

    private void doChangePin(HttpExchange ex) throws Exception {
        long accountNo = requireUser(ex);
        Map<String, String> body = readJson(ex);
        service.changePin(accountNo, body.get("oldPin"), body.get("newPin"));
        send(ex, 200, "{\"ok\":true}");
    }

    private void doInterest(HttpExchange ex) throws Exception {
        long accountNo = requireUser(ex);
        BigDecimal updated = service.addInterest(accountNo);
        send(ex, 200, "{\"ok\":true,\"balance\":" + Json.quote(updated.toPlainString()) + "}");
    }

    private void doAdminLogin(HttpExchange ex) throws Exception {
        Map<String, String> body = readJson(ex);
        String user = body.getOrDefault("user", "");
        String pass = body.getOrDefault("pass", "");
        if (!"admin".equals(user.trim()) || !"admin123".equals(pass)) {
            throw new AuthenticationException("Invalid admin credentials.");
        }
        String token = "ADM-" + UUID.randomUUID();
        sessions.put(token, ADMIN);
        send(ex, 200, "{\"ok\":true,\"token\":" + Json.quote(token) + "}");
    }

    private void doAdminAccounts(HttpExchange ex) throws Exception {
        requireAdmin(ex);
        StringBuilder out = new StringBuilder("{\"ok\":true,\"accounts\":[");
        List<Account> accounts = service.getAllAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            Account a = accounts.get(i);
            Customer c = service.getCustomer(a.getCustomerId());
            if (i > 0) {
                out.append(',');
            }
            out.append("{\"accountNo\":").append(a.getAccountNo())
                    .append(",\"holder\":").append(Json.quote(c == null ? "?" : c.getFullName()))
                    .append(",\"type\":").append(Json.quote(a.getAccountType()))
                    .append(",\"balance\":").append(Json.quote(a.getBalance().toPlainString()))
                    .append('}');
        }
        out.append("]}");
        send(ex, 200, out.toString());
    }

    private long requireUser(HttpExchange ex) throws AuthenticationException {
        Long id = sessions.get(tokenOf(ex));
        if (id == null || id == ADMIN) {
            throw new AuthenticationException("Please log in again.");
        }
        return id;
    }

    private void requireAdmin(HttpExchange ex) throws AuthenticationException {
        Long id = sessions.get(tokenOf(ex));
        if (id == null || id != ADMIN) {
            throw new AuthenticationException("Admin login required.");
        }
    }

    private String tokenOf(HttpExchange ex) {
        String token = ex.getRequestHeaders().getFirst("X-Token");
        return token == null ? "" : token;
    }

    private Map<String, String> readJson(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return Json.parseObject(body);
    }

    private void send(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---------------- Static files ----------------

    private void handleStatic(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            Path file = webDir.resolve(path.substring(1)).normalize();
            if (!file.startsWith(webDir.normalize()) || !Files.isRegularFile(file)) {
                byte[] notFound = "Not found".getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(404, notFound.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(notFound);
                }
                return;
            }
            String contentType = "text/plain";
            if (path.endsWith(".html")) {
                contentType = "text/html; charset=utf-8";
            } else if (path.endsWith(".js")) {
                contentType = "text/javascript; charset=utf-8";
            } else if (path.endsWith(".css")) {
                contentType = "text/css; charset=utf-8";
            }
            byte[] bytes = Files.readAllBytes(file);
            ex.getResponseHeaders().set("Content-Type", contentType);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } finally {
            ex.close();
        }
    }
}
