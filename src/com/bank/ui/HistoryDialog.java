package com.bank.ui;

import com.bank.model.Transaction;
import com.bank.service.BankService;
import java.awt.Frame;
import java.time.format.DateTimeFormatter;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Mini statement: last 50 transactions of one account. */
public class HistoryDialog extends JDialog {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public HistoryDialog(Frame owner, BankService service, long accountNo) {
        super(owner, "Mini Statement - A/c " + accountNo, true);
        setSize(720, 400);
        setLocationRelativeTo(owner);

        String[] columns = {"ID", "Date", "Type", "Amount (Rs.)", "Balance After", "Description"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        try {
            for (Transaction t : service.history(accountNo, 50)) {
                model.addRow(new Object[]{
                        t.getTxnId(),
                        t.getCreatedAt() == null ? "" : t.getCreatedAt().format(FORMAT),
                        t.getType(),
                        t.getAmount(),
                        t.getBalanceAfter(),
                        t.getDescription()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        add(new JScrollPane(new JTable(model)));
    }
}
