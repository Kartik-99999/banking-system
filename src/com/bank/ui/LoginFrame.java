package com.bank.ui;

import com.bank.service.BankService;
import com.bank.util.ValidationUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** First window: login with account number + PIN, or open a new account. */
public class LoginFrame extends JFrame {
    private final BankService service = new BankService();
    private final JTextField accountField = new JTextField(15);
    private final JPasswordField pinField = new JPasswordField(15);

    public LoginFrame() {
        super("Bank Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 260);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Banking System", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(22.0f));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        form.add(new JLabel("Account number:"));
        form.add(accountField);
        form.add(new JLabel("PIN:"));
        form.add(pinField);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Create Account");
        JButton adminBtn = new JButton("Admin");
        buttons.add(loginBtn);
        buttons.add(registerBtn);
        buttons.add(adminBtn);
        add(buttons, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> new RegisterDialog(this, service).setVisible(true));
        adminBtn.addActionListener(e -> doAdmin());
        getRootPane().setDefaultButton(loginBtn);
    }

    private void doLogin() {
        try {
            long accountNo = ValidationUtil.parseAccountNo(accountField.getText());
            String pin = new String(pinField.getPassword());
            BankService.Session session = service.login(accountNo, pin);
            new DashboardFrame(service, session).setVisible(true);
            dispose();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void doAdmin() {
        String user = JOptionPane.showInputDialog(this, "Admin username:");
        if (user == null) {
            return;
        }
        JPasswordField pf = new JPasswordField();
        int ok = JOptionPane.showConfirmDialog(this, pf, "Admin password:", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        if ("admin".equals(user.trim()) && "admin123".equals(new String(pf.getPassword()))) {
            new AdminFrame(service).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid admin credentials.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
