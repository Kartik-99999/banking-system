package com.bank.ui;

import com.bank.service.BankService;
import com.bank.util.ValidationUtil;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Form for opening a new bank account. Shows the new account number on success. */
public class RegisterDialog extends JDialog {
    private final BankService service;
    private final JTextField nameField = new JTextField(15);
    private final JTextField emailField = new JTextField(15);
    private final JTextField phoneField = new JTextField(15);
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"SAVINGS", "CURRENT"});
    private final JPasswordField pinField = new JPasswordField(15);
    private final JPasswordField confirmField = new JPasswordField(15);
    private final JTextField depositField = new JTextField("1000", 15);

    public RegisterDialog(Frame owner, BankService service) {
        super(owner, "Open New Account", true);
        this.service = service;
        setSize(420, 360);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        form.add(new JLabel("Full name:"));
        form.add(nameField);
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("Phone (10 digits):"));
        form.add(phoneField);
        form.add(new JLabel("Account type:"));
        form.add(typeBox);
        form.add(new JLabel("PIN (4-6 digits):"));
        form.add(pinField);
        form.add(new JLabel("Confirm PIN:"));
        form.add(confirmField);
        form.add(new JLabel("Initial deposit (Rs.):"));
        form.add(depositField);
        add(form, BorderLayout.CENTER);

        JButton registerBtn = new JButton("Open Account");
        JPanel bottom = new JPanel();
        bottom.add(registerBtn);
        add(bottom, BorderLayout.SOUTH);

        registerBtn.addActionListener(e -> doRegister());
        getRootPane().setDefaultButton(registerBtn);
    }

    private void doRegister() {
        try {
            String pin = new String(pinField.getPassword());
            String confirm = new String(confirmField.getPassword());
            if (!pin.equals(confirm)) {
                throw new IllegalArgumentException("PIN and confirm PIN do not match.");
            }
            BigDecimal deposit;
            try {
                deposit = new BigDecimal(ValidationUtil.requireNonBlank(depositField.getText(), "Initial deposit"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid initial deposit. Enter a number like 1000.");
            }
            long accountNo = service.register(
                    nameField.getText(), emailField.getText(), phoneField.getText(),
                    (String) typeBox.getSelectedItem(), pin, deposit);
            JOptionPane.showMessageDialog(this,
                    "Account opened!\nYour account number is: " + accountNo + "\nUse it with your PIN to log in.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
