package com.bank.dao;

import com.bank.model.FixedDeposit;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Database operations for the fixed_deposits table. */
public class FixedDepositDAO {

    public int create(Connection con, FixedDeposit fd) throws SQLException {
        String sql = "INSERT INTO fixed_deposits (account_no, principal, annual_rate, tenure_months,"
                + " start_date, maturity_date, maturity_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, fd.getAccountNo());
            ps.setBigDecimal(2, fd.getPrincipal());
            ps.setBigDecimal(3, fd.getAnnualRate());
            ps.setInt(4, fd.getTenureMonths());
            ps.setDate(5, Date.valueOf(fd.getStartDate()));
            ps.setDate(6, Date.valueOf(fd.getMaturityDate()));
            ps.setBigDecimal(7, fd.getMaturityAmount());
            ps.setString(8, fd.getStatus());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public FixedDeposit findById(Connection con, int fdId) throws SQLException {
        String sql = "SELECT * FROM fixed_deposits WHERE fd_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fdId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<FixedDeposit> findByAccountNo(Connection con, long accountNo) throws SQLException {
        String sql = "SELECT * FROM fixed_deposits WHERE account_no = ? ORDER BY fd_id DESC";
        List<FixedDeposit> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public void markClosed(Connection con, int fdId) throws SQLException {
        String sql = "UPDATE fixed_deposits SET status = 'CLOSED', closed_at = CURRENT_TIMESTAMP WHERE fd_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fdId);
            ps.executeUpdate();
        }
    }

    private FixedDeposit map(ResultSet rs) throws SQLException {
        Timestamp closed = rs.getTimestamp("closed_at");
        return new FixedDeposit(
                rs.getInt("fd_id"),
                rs.getLong("account_no"),
                rs.getBigDecimal("principal"),
                rs.getBigDecimal("annual_rate"),
                rs.getInt("tenure_months"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("maturity_date").toLocalDate(),
                rs.getBigDecimal("maturity_amount"),
                rs.getString("status"),
                closed == null ? null : closed.toLocalDateTime());
    }
}
