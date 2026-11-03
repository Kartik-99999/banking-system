package com.bank.ui;

import com.bank.model.SavingsAccount;
import com.bank.service.BankService;
import com.bank.util.ValidationUtil;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

/** Main window after login: balance plus all banking actions. */
public class DashboardFrame extends JFrame {
    private final BankService service;
    private final BankService.Session session;
    private final JLabel balanceLabel = new JLabel("", SwingConstants.CENTER);

    public DashboardFrame(BankService service, BankService.Session session) {
        super("Dashboard");
        this.service = service;
        this.session = session;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new GridLayout(3, 1));
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));
        JLabel welcome = new JLabel("Welcome, " + session.getCustomer().getFullName());
        welcome.setFont(welcome.getFont().deriveFont(18.0f));
        header.add(welcome);
        header.add(new JLabel("A/c " + session.getAccount().getAccountNo()
                + "  •  " + session.getAccount().getAccountType()));
        balanceLabel.setFont(balanceLabel.getFont().deriveFont(Font.BOLD, 24.0f));
        header.add(balanceLabel);
        add(header, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(4, 2, 10, 10));
        buttons.setBorder(BorderFactory.createEmptyBorder(10, 24, 16, 24));
        JButton depositBtn = new JButton("Deposit");
        JButton withdrawBtn = new JButton("Withdraw");
        JButton transferBtn = new JButton("Transfer");
        JButton historyBtn = new JButton("Mini Statement");
        JButton interestBtn = new JButton("Apply Interest");
        JButton pinBtn = new JButton("Change PIN");
        JButton refreshBtn = new JButton("Refresh Balance");
        JButton logoutBtn = new JButton("Logout");
        buttons.add(depositBtn);
        buttons.add(withdrawBtn);
        buttons.add(transferBtn);
        buttons.add(historyBtn);
        buttons.add(interestBtn);
        buttons.add(pinBtn);
        buttons.add(refreshBtn);
        buttons.add(logoutBtn);
        add(buttons, BorderLayout.CENTER);

        interestBtn.setEnabled(session.getAccount() instanceof SavingsAccount);

        depositBtn.addActionListener(e -> doDeposit());
        withdrawBtn.addActionListener(e -> doWithdraw());
        transferBtn.addActionListener(e -> doTransfer());
        historyBtn.addActionListener(e ->
                new HistoryDialog(this, service, session.getAccount().getAccountNo()).setVisible(true));
        interestBtn.addActionListener(e -> doInterest());
        pinBtn.addActionListener(e -> doChangePin());
        refreshBtn.addActionListener(e -> refreshBalance());
        logoutBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        refreshBalance();
    }

    private void doDeposit() {
        String input = JOptionPane.showInputDialog(this, "Deposit amount (Rs.):");
        if (input == null) {
            return;
        }
        try {
            BigDecimal amount = ValidationUtil.parseAmount(input);
            BigDecimal updated = service.deposit(session.getAccount().getAccountNo(), amount);
            session.getAccount().setBalance(updated);
            refreshBalance();
            JOptionPane.showMessageDialog(this, "Deposited Rs." + amount, "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void doWithdraw() {
        String input = JOptionPane.showInputDialog(this, "Withdrawal amount (Rs.):");
        if (input == null) {
            return;
        }
        try {
            BigDecimal amount = ValidationUtil.parseAmount(input);
            BigDecimal updated = service.withdraw(session.getAccount().getAccountNo(), amount);
            session.getAccount().setBalance(updated);
            refreshBalance();
            JOptionPane.showMessageDialog(this, "Withdrew Rs." + amount, "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void doTransfer() {
        String toInput = JOptionPane.showInputDialog(this, "Transfer to account number:");
        if (toInput == null) {
            return;
        }
        String amountInput = JOptionPane.showInputDialog(this, "Transfer amount (Rs.):");
        if (amountInput == null) {
            return;
        }
        try {
            long toNo = ValidationUtil.parseAccountNo(toInput);
            BigDecimal amount = ValidationUtil.parseAmount(amountInput);
            service.transfer(session.getAccount().getAccountNo(), toNo, amount);
            refreshBalance();
            JOptionPane.showMessageDialog(this,
                    "Transferred Rs." + amount + " to " + toNo, "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void doInterest() {
        try {
            BigDecimal updated = service.addInterest(session.getAccount().getAccountNo());
            session.getAccount().setBalance(updated);
            refreshBalance();
            JOptionPane.showMessageDialog(this, "Interest credited. New balance Rs." + updated,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void doChangePin() {
        JPasswordField oldPf = new JPasswordField();
        int ok = JOptionPane.showConfirmDialog(this, oldPf, "Enter old PIN:", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        JPasswordField newPf = new JPasswordField();
        ok = JOptionPane.showConfirmDialog(this, newPf, "Enter new PIN (4-6 digits):", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            service.changePin(session.getAccount().getAccountNo(),
                    new String(oldPf.getPassword()), new String(newPf.getPassword()));
            JOptionPane.showMessageDialog(this, "PIN changed successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void refreshBalance() {
        try {
            BigDecimal balance = service.getBalance(session.getAccount().getAccountNo());
            session.getAccount().setBalance(balance);
            balanceLabel.setText("Rs. " + balance);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
