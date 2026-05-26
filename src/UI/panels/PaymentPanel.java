package UI.panels;

import UI.components.*;
import UI.services.PdfExportService;
import UI.services.StubPdfExportService;
import UI.theme.ThemeManager;
import model.Bill;
import model.Payment;
import service.BillService;
import service.PaymentService;
import service.impl.BillServiceImpl;
import service.impl.PaymentServiceImpl;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Thanh toán — tra cứu hóa đơn, xác nhận thu tiền, in biên lai.
 * Hoàn toàn kết nối với SQL Server.
 */
public class PaymentPanel extends BasePanel {

    private final BillService billService = new BillServiceImpl();
    private final PaymentService paymentService = new PaymentServiceImpl();
    private final PdfExportService pdfService = StubPdfExportService.getInstance();

    private final JLabel nameLabel;
    private final JLabel totalLabel;
    private final JLabel statusLabel;
    private final RoundedTextField invoiceField;
    private final JComboBox<String> methodCombo;
    private final RoundedTextField noteField;
    private final RoundedButton payBtn;
    private final RoundedButton receiptBtn;
    private final ModernTable histTable;

    private Bill currentBill = null;

    public PaymentPanel() {
        super("Thanh toán", "Xác nhận thu tiền và in biên lai cho hộ dân");

        JPanel root = new JPanel(new BorderLayout(UIConstants.SP_LG, 0));
        root.setOpaque(false);

        // ---- Left: search form ----
        GlassCard searchCard = new GlassCard(UIConstants.CARD_RADIUS);
        searchCard.setLayout(new BorderLayout(0, UIConstants.SP_MD));
        searchCard.setPreferredSize(new Dimension(360, 0));

        JLabel searchTitle = new JLabel("Tra cứu hóa đơn");
        searchTitle.setFont(UIConstants.FONT_SUBHEADER);
        searchTitle.setForeground(UIConstants.COLOR_TEXT_PRIMARY);

        JPanel searchForm = new JPanel(new GridBagLayout());
        searchForm.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 0, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel idLabel = new JLabel("Mã hóa đơn:");
        idLabel.setFont(UIConstants.FONT_NORMAL_BOLD);
        
        invoiceField = new RoundedTextField();
        invoiceField.setPreferredSize(new Dimension(150, 36));
        // Action on Enter
        invoiceField.addActionListener(e -> loadInvoice());

        RoundedButton searchBtn = new RoundedButton("Tìm", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        searchBtn.setPreferredSize(new Dimension(70, 36));
        searchBtn.addActionListener(e -> loadInvoice());

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0; searchForm.add(idLabel, gc);
        gc.gridx = 1;               gc.weightx = 1; searchForm.add(invoiceField, gc);
        gc.gridx = 2;               gc.weightx = 0; searchForm.add(searchBtn, gc);

        JPanel topSearch = new JPanel(new BorderLayout(0, UIConstants.SP_SM));
        topSearch.setOpaque(false);
        topSearch.add(searchTitle, BorderLayout.NORTH);
        topSearch.add(searchForm, BorderLayout.CENTER);
        searchCard.add(topSearch, BorderLayout.NORTH);

        // ---- Details panel ----
        JPanel detailPanel = new JPanel(new GridLayout(6, 1, 0, UIConstants.SP_SM));
        detailPanel.setOpaque(false);

        nameLabel = detailRow(detailPanel, "Hộ gia đình:", "—");
        totalLabel = detailRow(detailPanel, "Tổng tiền:", "—");
        statusLabel = detailRow(detailPanel, "Trạng thái:", "—");

        // Payment method
        JPanel methodRow = new JPanel(new BorderLayout());
        methodRow.setOpaque(false);
        JLabel methodLbl = new JLabel("Phương thức:");
        methodLbl.setFont(UIConstants.FONT_NORMAL_BOLD);
        methodLbl.setForeground(UIConstants.COLOR_TEXT_MUTED);
        methodCombo = new JComboBox<>(new String[]{"Tiền mặt", "Chuyển khoản", "Thẻ tín dụng", "Ví điện tử"});
        methodCombo.setFont(UIConstants.FONT_NORMAL);
        methodCombo.setPreferredSize(new Dimension(160, 26));
        methodRow.add(methodLbl, BorderLayout.WEST);
        methodRow.add(methodCombo, BorderLayout.EAST);
        methodRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_DIVIDER));
        detailPanel.add(methodRow);

        // Note row
        JPanel noteRow = new JPanel(new BorderLayout());
        noteRow.setOpaque(false);
        JLabel noteLbl = new JLabel("Ghi chú:");
        noteLbl.setFont(UIConstants.FONT_NORMAL_BOLD);
        noteLbl.setForeground(UIConstants.COLOR_TEXT_MUTED);
        noteField = new RoundedTextField();
        noteField.setPreferredSize(new Dimension(160, 26));
        noteRow.add(noteLbl, BorderLayout.WEST);
        noteRow.add(noteField, BorderLayout.EAST);
        noteRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_DIVIDER));
        detailPanel.add(noteRow);

        searchCard.add(detailPanel, BorderLayout.CENTER);

        // ---- Pay + receipt buttons ----
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, UIConstants.SP_SM));
        btnPanel.setOpaque(false);

        payBtn = new RoundedButton("Xác nhận đã thu tiền", UIConstants.BUTTON_RADIUS, UIConstants.SUCCESS);
        payBtn.setPreferredSize(new Dimension(0, 42));
        payBtn.setEnabled(false); // only active after successful search of unpaid bill
        payBtn.addActionListener(e -> confirmPayment());

        receiptBtn = new RoundedButton("In biên lai", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        receiptBtn.setPreferredSize(new Dimension(0, 42));
        receiptBtn.setEnabled(false);
        receiptBtn.addActionListener(e -> {
            if (currentBill != null) {
                pdfService.exportPaymentReceipt(currentBill.getBillCode(), "C:/BienLai/" + currentBill.getBillCode() + ".pdf");
            }
        });

        btnPanel.add(payBtn);
        btnPanel.add(receiptBtn);
        searchCard.add(btnPanel, BorderLayout.SOUTH);

        // ---- Right: recent payments table ----
        GlassCard tableCard = new GlassCard(UIConstants.CARD_RADIUS);
        tableCard.setLayout(new BorderLayout(0, UIConstants.SP_SM));

        JLabel tableTitle = new JLabel("Lịch sử thu tiền gần đây");
        tableTitle.setFont(UIConstants.FONT_SUBHEADER);
        tableTitle.setForeground(UIConstants.COLOR_TEXT_PRIMARY);

        histTable = new ModernTable(
            new String[]{"Mã HĐ", "Hộ gia đình", "Số tiền", "Phương thức", "Ngày thu"}
        );
        histTable.setColumnWidths(90, 180, 110, 110, 130);

        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(histTable, BorderLayout.CENTER);

        root.add(searchCard, BorderLayout.WEST);
        root.add(tableCard, BorderLayout.CENTER);

        contentArea.add(root, BorderLayout.CENTER);

        loadRecentHistory();
    }

    private JLabel detailRow(JPanel panel, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_NORMAL_BOLD);
        lbl.setForeground(UIConstants.COLOR_TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(UIConstants.FONT_NORMAL);
        val.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_DIVIDER));
        panel.add(row);
        return val;
    }

    private void loadInvoice() {
        String code = invoiceField.getText().trim().toUpperCase();
        if (code.isEmpty()) {
            ThemeManager.showInfoDialog(this, "Vui lòng nhập mã hóa đơn!", "Thiếu thông tin");
            return;
        }

        currentBill = billService.getByCode(code);
        if (currentBill == null) {
            ThemeManager.showInfoDialog(this, "Không tìm thấy hóa đơn: " + code, "Không tìm thấy");
            nameLabel.setText("—");
            totalLabel.setText("—");
            statusLabel.setText("—");
            statusLabel.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
            payBtn.setEnabled(false);
            receiptBtn.setEnabled(false);
            return;
        }

        nameLabel.setText(currentBill.getOwnerName() + " (" + currentBill.getHouseholdCode() + ")");
        totalLabel.setText(String.format("%,.0f đ", currentBill.getTotalAmount()));
        
        if ("PAID".equalsIgnoreCase(currentBill.getPaymentStatus())) {
            statusLabel.setText("Đã thanh toán");
            statusLabel.setForeground(UIConstants.SUCCESS);
            payBtn.setEnabled(false);
            receiptBtn.setEnabled(true);
        } else {
            statusLabel.setText("Chưa thanh toán");
            statusLabel.setForeground(UIConstants.ERROR);
            payBtn.setEnabled(true);
            receiptBtn.setEnabled(false);
        }
    }

    private void confirmPayment() {
        if (currentBill == null) return;

        boolean ok = ThemeManager.showConfirmDialog(this,
            "Xác nhận đã thu tiền cho hóa đơn " + currentBill.getBillCode() + "?",
            "Xác nhận thanh toán");
        if (!ok) return;

        Payment payment = new Payment();
        payment.setBillId(currentBill.getBillId());
        payment.setAmount(currentBill.getTotalAmount());
        payment.setPaymentMethod(methodCombo.getSelectedItem().toString());
        payment.setNote(noteField.getText().trim());

        String err = paymentService.recordPayment(payment);
        if (err != null) {
            ThemeManager.showInfoDialog(this, err, "Lỗi");
        } else {
            ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                "Đã ghi nhận thanh toán cho hóa đơn " + currentBill.getBillCode() + "!",
                ToastNotification.Type.SUCCESS);
            
            noteField.setText("");
            loadInvoice(); // reloads displays, disables payBtn, enables receiptBtn
            loadRecentHistory(); // reloads table
        }
    }

    private void loadRecentHistory() {
        List<Payment> list = paymentService.getRecentPayments(20);
        histTable.clearRows();
        for (Payment p : list) {
            histTable.addRow(new Object[]{
                p.getBillCode(),
                p.getOwnerName(),
                String.format("%,.0f đ", p.getAmount()),
                p.getPaymentMethod(),
                p.getPaymentDate() != null ? p.getPaymentDate().toString().substring(0, 16) : ""
            });
        }
    }
}
