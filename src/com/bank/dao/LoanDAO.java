package com.bank.dao;

import com.bank.model.Loan;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Database operations for the loans and loan_payments tables. */
public class LoanDAO {

    public int create(Connection con, Loan loan) throws SQLException {
        String sql = "INSERT INTO loans (account_no, loan_type, principal, annual_rate,"
                + " tenure_months, emi, outstanding, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, loan.getAccountNo());
            ps.setString(2, loan.getLoanType().name());
            ps.setBigDecimal(3, loan.getPrincipal());
            ps.setBigDecimal(4, loan.getAnnualRate());
            ps.setInt(5, loan.getTenureMonths());
            ps.setBigDecimal(6, loan.getEmi());
            ps.setBigDecimal(7, loan.getOutstanding());
            ps.setString(8, loan.getStatus());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Loan findById(Connection con, int loanId) throws SQLException {
        String sql = "SELECT * FROM loans WHERE loan_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Loan> findByAccountNo(Connection con, long accountNo) throws SQLException {
        String sql = "SELECT * FROM loans WHERE account_no = ? ORDER BY loan_id DESC";
        List<Loan> list = new ArrayList<>();
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

    public void updateOutstanding(Connection con, int loanId,
                                 java.math.BigDecimal outstanding, String status) throws SQLException {
        String sql = "UPDATE loans SET outstanding = ?, status = ? WHERE loan_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, outstanding);
            ps.setString(2, status);
            ps.setInt(3, loanId);
            ps.executeUpdate();
        }
    }

    public void addPayment(Connection con, int loanId, java.math.BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO loan_payments (loan_id, amount) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            ps.setBigDecimal(2, amount);
            ps.executeUpdate();
        }
    }

    private Loan map(ResultSet rs) throws SQLException {
        return new Loan(
                rs.getInt("loan_id"),
                rs.getLong("account_no"),
                Loan.Type.valueOf(rs.getString("loan_type")),
                rs.getBigDecimal("principal"),
                rs.getBigDecimal("annual_rate"),
                rs.getInt("tenure_months"),
                rs.getBigDecimal("emi"),
                rs.getBigDecimal("outstanding"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
