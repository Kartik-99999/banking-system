package com.bank.dao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates tables from sql/schema.sql when the web server starts.
 * Idempotent (IF NOT EXISTS), so it is safe to run on every boot.
 * Skips CREATE DATABASE / USE lines: the JDBC URL already selects the database.
 */
public final class DbInit {
    private DbInit() {
    }

    public static void ensureSchema() {
        Path file = Paths.get("sql", "schema.sql");
        if (!Files.isRegularFile(file)) {
            System.out.println("DbInit: sql/schema.sql not found, skipping.");
            return;
        }
        // Deployed DBs (Render) can take a minute to accept connections; retry there.
        int attempts = System.getenv("DB_HOST") != null ? 12 : 1;
        Exception last = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                runScript(file);
                System.out.println("DbInit: schema ready.");
                return;
            } catch (Exception e) {
                last = e;
                if (i < attempts) {
                    System.out.println("DbInit: DB not ready, retry " + i + "/" + attempts + "...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("DbInit failed: " + (last == null ? "unknown" : last.getMessage()), last);
    }

    private static void runScript(Path file) throws Exception {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            String upper = trimmed.toUpperCase();
            if (upper.startsWith("CREATE DATABASE") || upper.startsWith("USE ")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                statements.add(current.toString());
                current.setLength(0);
            }
        }
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            for (String sql : statements) {
                st.execute(sql);
            }
        }
    }
}
