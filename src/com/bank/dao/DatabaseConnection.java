package com.bank.dao;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Creates JDBC connections. Settings come from db.properties in the
 * project folder; if the file is missing, the defaults below are used.
 */
public final class DatabaseConnection {
    private static String url = "jdbc:mysql://localhost:3306/bankdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String user = "root";
    private static String password = "";

    static {
        try (InputStream in = new FileInputStream("db.properties")) {
            Properties props = new Properties();
            props.load(in);
            url = props.getProperty("db.url", url);
            user = props.getProperty("db.user", user);
            password = props.getProperty("db.password", password);
        } catch (Exception ignored) {
            // No db.properties found: fall back to the defaults above.
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "MySQL driver not found. Add mysql-connector-j.jar to the classpath (see README).", e);
        }
    }

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
