package com.bank.dao;

import com.bank.model.DebitCard;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Database operations for the debit_cards table. */
public class CardDAO {

    public int create(Connection con, DebitCard card) throws SQLException {
        String sql = "INSERT INTO debit_cards (account_no, card_number, expiry, active) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, card.getAccountNo());
            ps.setString(2, card.getCardNumber());
            ps.setString(3, card.getExpiry());
            ps.setBoolean(4, card.isActive());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Latest card of the account (active or blocked). */
    public DebitCard findLatestByAccountNo(Connection con, long accountNo) throws SQLException {
        String sql = "SELECT * FROM debit_cards WHERE account_no = ? ORDER BY card_id DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void setActive(Connection con, int cardId, boolean active) throws SQLException {
        String sql = "UPDATE debit_cards SET active = ? WHERE card_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, cardId);
            ps.executeUpdate();
        }
    }

    private DebitCard map(ResultSet rs) throws SQLException {
        return new DebitCard(
                rs.getInt("card_id"),
                rs.getLong("account_no"),
                rs.getString("card_number"),
                rs.getString("expiry"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
