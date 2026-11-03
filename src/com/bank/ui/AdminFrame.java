package com.bank.ui;

import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.service.BankService;
import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Simple admin view: lists every account in the bank. */
public class AdminFrame extends JFrame {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final BankService service;
    private final DefaultTableModel model;

    public AdminFrame(BankService service) {
        super("Admin - All Accounts");
        this.service = service;
        setSize(720, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        String[] columns = {"Account No", "Holder", "Type", "Balance (Rs.)", "Opened"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton refreshBtn = new JButton("Refresh");
        JButton closeBtn = new JButton("Close");
        bottom.add(refreshBtn);
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> loadData());
        closeBtn.addActionListener(e -> dispose());

        loadData();
    }

    private void loadData() {
        model.setRowCount(0);
        try {
            for (Account a : service.getAllAccounts()) {
                Customer c = service.getCustomer(a.getCustomerId());
                model.addRow(new Object[]{
                        a.getAccountNo(),
                        c == null ? "?" : c.getFullName(),
                        a.getAccountType(),
                        a.getBalance(),
                        a.getCreatedAt() == null ? "" : a.getCreatedAt().format(FORMAT)});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
