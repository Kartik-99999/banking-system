package com.bank.dao;

import com.bank.model.Transaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database operations for the transactions table. */
public class TransactionDAO {

    public void insert(Connection con, Transaction txn) throws SQLException {
        String sql = "INSERT INTO transactions (account_no, txn_type, amount, balance_after, description)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, txn.getAccountNo());
            ps.setString(2, txn.getType().name());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setBigDecimal(4, txn.getBalanceAfter());
            ps.setString(5, txn.getDescription());
            ps.executeUpdate();
        }
    }

    /** Latest transactions first, capped at the given limit. */
    public List<Transaction> findByAccountNo(Connection con, long accountNo, int limit) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE account_no = ? ORDER BY txn_id DESC LIMIT ?";
        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountNo);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Transaction(
                            rs.getLong("txn_id"),
                            rs.getLong("account_no"),
                            Transaction.Type.valueOf(rs.getString("txn_type")),
                            rs.getBigDecimal("amount"),
                            rs.getBigDecimal("balance_after"),
                            rs.getString("description"),
                            rs.getTimestamp("created_at").toLocalDateTime()));
                }
            }
        }
        return list;
    }
}
