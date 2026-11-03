package com.bank.dao;

import com.bank.model.Account;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Database operations for the accounts table. */
public class AccountDAO {

    /** Inserts an account, returns the generated account number. */
    public long create(Connection con, int customerId, String type,
                       String pinHash, BigDecimal balance) throws SQLException {
        String sql = "INSERT INTO accounts (customer_id, account_type, pin_hash, balance) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setString(2, type);
            ps.setString(3, pinHash);
            ps.setBigDecimal(4, balance);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public Account findByAccountNo(Connection con, long accountNo) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_no = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void updateBalance(Connection con, long accountNo, BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_no = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, accountNo);
            ps.executeUpdate();
        }
    }

    public void updatePin(Connection con, long accountNo, String pinHash) throws SQLException {
        String sql = "UPDATE accounts SET pin_hash = ? WHERE account_no = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pinHash);
            ps.setLong(2, accountNo);
            ps.executeUpdate();
        }
    }

    public List<Account> findAll(Connection con) throws SQLException {
        String sql = "SELECT * FROM accounts ORDER BY account_no";
        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                accounts.add(map(rs));
            }
        }
        return accounts;
    }

    private Account map(ResultSet rs) throws SQLException {
        return Account.create(
                rs.getString("account_type"),
                rs.getLong("account_no"),
                rs.getInt("customer_id"),
                rs.getString("pin_hash"),
                rs.getBigDecimal("balance"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
