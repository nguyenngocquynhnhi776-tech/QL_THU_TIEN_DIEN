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
import session.UserSession;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Trang Thanh Toán — bố cục dọc 3 section:
 *   1. Hóa đơn chưa thanh toán (bảng tìm kiếm)
 *   2. Chi tiết giao dịch (form xác nhận)
 *   3. Lịch sử thu tiền gần đây (bảng 10 dòng + scroll)
 *
 * Toàn trang được bọc trong JScrollPane nên không có nội dung nào bị ẩn.
 * Mọi thao tác DB đều chạy trên SwingWorker (không bao giờ đơ UI).
 */
public class PaymentPanel extends BasePanel {

    // ── Services ─────────────────────────────────────────────────────────────
    private final BillService      billService    = new BillServiceImpl();
    private final PaymentService   paymentService = new PaymentServiceImpl();
    private final PdfExportService pdfService     = StubPdfExportService.getInstance();

    // ── Section 1: Unpaid invoices ────────────────────────────────────────────
    private SearchField  searchField;
    private ModernTable  unpaidTable;
    private List<Bill>   unpaidBills   = new ArrayList<>();
    private Timer        searchDebounce;

    // ── Section 2: Payment form ───────────────────────────────────────────────
    private RoundedTextField billCodeField;
    private RoundedTextField householdField;
    private RoundedTextField periodField;
    private RoundedTextField totalField;
    private RoundedTextField statusField;
    private JComboBox<String> methodCombo;
    private RoundedTextField noteField;
    private RoundedButton    payBtn;
    private RoundedButton    receiptBtn;
    private JLabel           formHint;     // "← Chọn hóa đơn…" hint

    // ── Section 3: History ────────────────────────────────────────────────────
    private ModernTable histTable;

    // ── State ─────────────────────────────────────────────────────────────────
    private Bill    currentBill          = null;
    private Payment lastCompletedPayment = null;

    // Row-height constant shared by both tables
    private static final int ROW_H   = 38;
    private static final int HDR_H   = 42;

    // =========================================================================
    public PaymentPanel() {
        super("Thanh toán", "Xác nhận thu tiền và in biên lai hóa đơn điện");
        buildUI();
        refreshData();
    }

    // =========================================================================
    // MAIN LAYOUT — 3 vertical sections inside a page-level JScrollPane
    // =========================================================================
    private void buildUI() {
        // Inner content panel: GridBagLayout, single column, each row gets its
        // preferred height (fill=HORIZONTAL not BOTH so heights are natural).
        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx   = 0;
        gc.weightx = 1.0;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.anchor  = GridBagConstraints.NORTHWEST;

        gc.gridy  = 0;
        gc.insets = new Insets(0, 0, UIConstants.SP_LG, 0);
        main.add(buildUnpaidSection(), gc);

        gc.gridy  = 1;
        gc.insets = new Insets(0, 0, UIConstants.SP_LG, 0);
        main.add(buildFormSection(), gc);

        gc.gridy  = 2;
        gc.insets = new Insets(0, 0, 0, 0);
        main.add(buildHistSection(), gc);

        // Invisible filler: pushes all content to the top when window is tall
        gc.gridy   = 3;
        gc.weighty = 1.0;
        gc.fill    = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        main.add(filler, gc);

        // ── Page-level scroll pane ─────────────────────────────────────────
        JScrollPane pageScroll = new JScrollPane(main);
        pageScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pageScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.setBorder(null);
        pageScroll.setOpaque(false);
        pageScroll.getViewport().setOpaque(false);
        pageScroll.getVerticalScrollBar().setUnitIncrement(18);

        contentArea.add(pageScroll, BorderLayout.CENTER);
    }

    // =========================================================================
    // SECTION 1 — Hóa đơn chưa thanh toán
    // =========================================================================
    private GlassCard buildUnpaidSection() {
        GlassCard card = new GlassCard(UIConstants.CARD_RADIUS);
        card.setLayout(new BorderLayout(0, UIConstants.SP_MD));

        // ── Section header: title left, search field right ──────────────────
        JLabel title = new JLabel("📋  Hóa đơn chưa thanh toán");
        title.setFont(UIConstants.FONT_SUBHEADER);
        title.setForeground(UIConstants.COLOR_TEXT_PRIMARY);

        searchField = new SearchField("Tìm tên hộ dân, số điện thoại…");
        searchField.setPreferredSize(new Dimension(320, 36));
        searchField.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { scheduleSearch(searchField.getText()); }
            public void removeUpdate(DocumentEvent e)  { scheduleSearch(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { scheduleSearch(searchField.getText()); }
        });

        JPanel hdr = new JPanel(new BorderLayout(UIConstants.SP_MD, 0));
        hdr.setOpaque(false);
        hdr.add(title,       BorderLayout.WEST);
        hdr.add(searchField, BorderLayout.EAST);

        // ── Table: 4 rows visible, then scroll (shorter to fit form on screen) ─────────────────────────────
        unpaidTable = new ModernTable(
                new String[]{"Hộ gia đình", "Kỳ thanh toán", "Số tiền", "Trạng thái"});
        // Set viewport height = exactly 4 data rows + header
        unpaidTable.getTable().setPreferredScrollableViewportSize(
                new Dimension(800, 4 * ROW_H + HDR_H));
        // Proportional seeds (total ~1000): AUTO_RESIZE_ALL_COLUMNS scales them uniformly
        unpaidTable.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        unpaidTable.setColumnWidths(400, 160, 210, 190);
        // Status badge renderer
        unpaidTable.setColumnRenderer(3, (tbl, val, sel, foc, row, col) ->
                new StatusBadge("Chưa thu", StatusBadge.Status.UNPAID));
        // Row click → fill form
        unpaidTable.getTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int vRow = unpaidTable.getSelectedRow();
                if (vRow >= 0) {
                    int mRow = unpaidTable.getTable().convertRowIndexToModel(vRow);
                    if (mRow >= 0 && mRow < unpaidBills.size()) {
                        loadBillDetails(unpaidBills.get(mRow));
                        // Scroll to form so user sees it filled
                        SwingUtilities.invokeLater(() -> {
                            JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(
                                    JScrollPane.class, PaymentPanel.this);
                            if (sp != null && billCodeField != null) {
                                Rectangle r = SwingUtilities.convertRectangle(
                                        billCodeField.getParent(),
                                        billCodeField.getBounds(), sp.getViewport().getView());
                                sp.getViewport().scrollRectToVisible(r);
                            }
                        });
                    }
                }
            }
        });

        card.add(hdr,         BorderLayout.NORTH);
        card.add(unpaidTable, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // SECTION 2 — Chi tiết giao dịch (form)
    // =========================================================================
    private GlassCard buildFormSection() {
        GlassCard card = new GlassCard(UIConstants.CARD_RADIUS);
        card.setLayout(new BorderLayout(0, UIConstants.SP_MD));

        // ── Section header ──────────────────────────────────────────────────
        JLabel title = new JLabel("💳  Chi tiết giao dịch");
        title.setFont(UIConstants.FONT_SUBHEADER);
        title.setForeground(UIConstants.COLOR_TEXT_PRIMARY);

        formHint = new JLabel("← Nhấn vào hóa đơn phía trên để điền thông tin");
        formHint.setFont(UIConstants.FONT_SMALL);
        formHint.setForeground(UIConstants.COLOR_TEXT_MUTED);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title,    BorderLayout.WEST);
        hdr.add(formHint, BorderLayout.EAST);

        // ── Initialize fields ───────────────────────────────────────────────
        billCodeField  = readOnly();
        householdField = readOnly();
        periodField    = readOnly();
        totalField     = readOnly();
        statusField    = readOnly();
        methodCombo    = new JComboBox<>(new String[]{"Tiền mặt", "Chuyển khoản", "Ví điện tử"});
        methodCombo.setFont(UIConstants.FONT_NORMAL);
        noteField = new RoundedTextField();

        // ── Form grid: 2 label-field pairs per row ──────────────────────────
        // Col 0: label1  Col 1: field1  Col 2: label2  Col 3: field2
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        // Row 0
        addFormRow(form, 0,
                "Mã hóa đơn:",    billCodeField,
                "Hộ gia đình:",   householdField);
        // Row 1
        addFormRow(form, 1,
                "Kỳ thanh toán:", periodField,
                "Tổng tiền:",     totalField);
        // Row 2
        addFormRow(form, 2,
                "Trạng thái:",    statusField,
                "Phương thức:",   methodCombo);
        // Row 3 — full-width
        addFormRowFull(form, 3, "Ghi chú:", noteField);

        // ── Action buttons ──────────────────────────────────────────────────
        payBtn = new RoundedButton("✅  Xác nhận đã thu tiền",
                UIConstants.BUTTON_RADIUS, new Color(0x2E7D32));
        payBtn.setFont(UIConstants.FONT_NORMAL_BOLD);
        payBtn.setPreferredSize(new Dimension(210, 42));
        payBtn.setEnabled(false);
        payBtn.setVisible(util.PermissionManager.getInstance()
                .hasPermission(model.Permission.PAYMENT_COLLECT));
        payBtn.addActionListener(e -> processPayment());

        receiptBtn = new RoundedButton("🖨️  In biên lai",
                UIConstants.BUTTON_RADIUS, new Color(0x1565C0));
        receiptBtn.setFont(UIConstants.FONT_NORMAL_BOLD);
        receiptBtn.setPreferredSize(new Dimension(145, 42));
        receiptBtn.setEnabled(false);
        receiptBtn.setVisible(util.PermissionManager.getInstance()
                .hasPermission(model.Permission.PAYMENT_PRINT_RECEIPT));
        receiptBtn.addActionListener(e -> printReceipt());

        RoundedButton clearBtn = new RoundedButton("Xóa trống",
                UIConstants.BUTTON_RADIUS, new Color(0x757575));
        clearBtn.setFont(UIConstants.FONT_NORMAL);
        clearBtn.setPreferredSize(new Dimension(110, 42));
        clearBtn.addActionListener(e -> clearForm());

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);
        btnBar.add(clearBtn);
        btnBar.add(receiptBtn);
        btnBar.add(payBtn);

        card.add(hdr,    BorderLayout.NORTH);
        card.add(form,   BorderLayout.CENTER);
        card.add(btnBar, BorderLayout.SOUTH);
        return card;
    }

    /**
     * Adds a row with TWO label-field pairs to the form grid.
     * grid columns: [0]=lbl1  [1]=fld1  [2]=lbl2  [3]=fld2
     */
    private void addFormRow(JPanel form, int row,
                             String lbl1, Component fld1,
                             String lbl2, Component fld2) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy  = row;
        c.anchor = GridBagConstraints.WEST;

        // label 1
        c.gridx = 0; c.weightx = 0;
        c.fill  = GridBagConstraints.NONE;
        c.insets = new Insets(6, 0, 6, 10);
        form.add(formLabel(lbl1), c);

        // field 1
        c.gridx = 1; c.weightx = 0.46;
        c.fill  = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 24);
        form.add(fld1, c);

        // label 2
        c.gridx = 2; c.weightx = 0;
        c.fill  = GridBagConstraints.NONE;
        c.insets = new Insets(6, 0, 6, 10);
        form.add(formLabel(lbl2), c);

        // field 2
        c.gridx = 3; c.weightx = 0.46;
        c.fill  = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        form.add(fld2, c);
    }

    /**
     * Adds a row with ONE label + ONE full-width field spanning cols 1-3.
     */
    private void addFormRowFull(JPanel form, int row, String lbl, Component fld) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy  = row;
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.weightx = 0;
        c.fill  = GridBagConstraints.NONE;
        c.insets = new Insets(6, 0, 6, 10);
        form.add(formLabel(lbl), c);

        c.gridx    = 1; c.gridwidth = 3;
        c.weightx  = 1.0;
        c.fill     = GridBagConstraints.HORIZONTAL;
        c.insets   = new Insets(6, 0, 6, 0);
        form.add(fld, c);
    }

    // =========================================================================
    // SECTION 3 — Lịch sử thu tiền gần đây
    // =========================================================================
    private GlassCard buildHistSection() {
        GlassCard card = new GlassCard(UIConstants.CARD_RADIUS);
        card.setLayout(new BorderLayout(0, UIConstants.SP_MD));

        JLabel title = new JLabel("📜  Lịch sử thu tiền gần đây");
        title.setFont(UIConstants.FONT_SUBHEADER);
        title.setForeground(UIConstants.COLOR_TEXT_PRIMARY);

        JLabel hint = new JLabel("Hiển thị tối đa 10 hộ — cuộn để xem thêm");
        hint.setFont(UIConstants.FONT_SMALL);
        hint.setForeground(UIConstants.COLOR_TEXT_MUTED);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(hint,  BorderLayout.EAST);

        histTable = new ModernTable(
                new String[]{"Mã HĐ", "Hộ gia đình", "Số tiền", "Phương thức", "Ngày thu"});
        // EXACTLY 10 rows visible; a vertical scrollbar appears when there are > 10 records
        histTable.getTable().setPreferredScrollableViewportSize(
                new Dimension(800, 10 * ROW_H + HDR_H));
        histTable.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        // Proportional seeds: Mã HĐ made wider (180px) to prevent text cutoff; columns balanced.
        histTable.setColumnWidths(180, 260, 160, 160, 190);

        card.add(hdr,       BorderLayout.NORTH);
        card.add(histTable, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // SEARCH DEBOUNCING
    // =========================================================================
    private void scheduleSearch(String query) {
        if (searchDebounce != null) searchDebounce.stop();
        searchDebounce = new Timer(220, e -> asyncLoadUnpaid(query));
        searchDebounce.setRepeats(false);
        searchDebounce.start();
    }

    private void asyncLoadUnpaid(String query) {
        new SwingWorker<List<Bill>, Void>() {
            @Override protected List<Bill> doInBackground() {
                return billService.search(
                        (query == null || query.isBlank()) ? null : query,
                        "UNPAID", null, null);
            }
            @Override protected void done() {
                try {
                    unpaidBills = get();
                    unpaidTable.clearRows();
                    for (Bill b : unpaidBills) {
                        unpaidTable.addRow(new Object[]{
                            b.getOwnerName() + " (" + b.getHouseholdCode() + ")",
                            b.getPeriodDisplay(),
                            String.format("%,.0f đ", b.getTotalAmount()),
                            "Chưa thu"
                        });
                    }
                } catch (Exception ex) {
                    System.err.println("[WARN] asyncLoadUnpaid: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // =========================================================================
    // BILL DETAILS BINDING
    // =========================================================================
    private void loadBillDetails(Bill bill) {
        currentBill = bill;
        billCodeField.setText(bill.getBillCode());
        householdField.setText(bill.getOwnerName() + " (" + bill.getHouseholdCode() + ")");
        periodField.setText(bill.getPeriodDisplay());
        totalField.setText(String.format("%,.0f đ", bill.getTotalAmount()));
        statusField.setText("Chưa thanh toán");
        statusField.setForeground(UIConstants.ERROR);
        noteField.setText("");
        if (formHint != null) formHint.setVisible(false);
        payBtn.setEnabled(true);
        receiptBtn.setEnabled(false);
        lastCompletedPayment = null;
    }

    // =========================================================================
    // CONFIRM PAYMENT
    // =========================================================================
    private void processPayment() {
        if (!util.PermissionManager.getInstance()
                .checkPermission(model.Permission.PAYMENT_COLLECT)) return;
        if (currentBill == null) return;

        boolean ok = ThemeManager.showConfirmDialog(this,
                "Xác nhận đã thu "
                + String.format("%,.0f đ", currentBill.getTotalAmount())
                + " cho hóa đơn " + currentBill.getBillCode() + "?",
                "Xác nhận thanh toán");
        if (!ok) return;

        Payment payment = new Payment();
        payment.setBillId(currentBill.getBillId());
        payment.setAmount(currentBill.getTotalAmount());
        payment.setPaymentMethod(methodCombo.getSelectedItem().toString());
        payment.setNote(noteField.getText().trim());

        payBtn.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return paymentService.recordPayment(payment);
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    String err = get();
                    if (err != null) {
                        ThemeManager.showInfoDialog(PaymentPanel.this, err, "Lỗi");
                        payBtn.setEnabled(currentBill != null && !currentBill.isPaid());
                    } else {
                        lastCompletedPayment = payment;
                        onPaymentSuccess();
                    }
                } catch (Exception ex) {
                    ThemeManager.showInfoDialog(PaymentPanel.this,
                            "Lỗi hệ thống: " + ex.getMessage(), "Lỗi");
                    payBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void onPaymentSuccess() {
        statusField.setText("Đã thanh toán");
        statusField.setForeground(UIConstants.SUCCESS);
        receiptBtn.setEnabled(true);
        payBtn.setEnabled(false);

        ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                "Thanh toán hóa đơn " + currentBill.getBillCode() + " thành công!",
                ToastNotification.Type.SUCCESS);

        asyncLoadUnpaid(searchField != null ? searchField.getText() : "");
        asyncLoadHistory();
        notifyInvoicePanelReload();
    }

    // =========================================================================
    // HISTORY LOADING
    // =========================================================================
    private void asyncLoadHistory() {
        new SwingWorker<List<Payment>, Void>() {
            @Override protected List<Payment> doInBackground() {
                return paymentService.getRecentPayments(50);
            }
            @Override protected void done() {
                try {
                    histTable.clearRows();
                    for (Payment p : get()) {
                        histTable.addRow(new Object[]{
                            p.getBillCode(),
                            p.getOwnerName(),
                            String.format("%,.0f đ", p.getAmount()),
                            p.getPaymentMethod(),
                            p.getPaymentDate() != null
                                ? p.getPaymentDate().toString().substring(0, 16) : ""
                        });
                    }
                } catch (Exception ex) {
                    System.err.println("[WARN] asyncLoadHistory: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // =========================================================================
    // PRINT RECEIPT POPUP
    // =========================================================================
    private void printReceipt() {
        if (currentBill == null || lastCompletedPayment == null) return;

        String collector = UserSession.getInstance().getFullName();
        if (collector == null || collector.isBlank())
            collector = UserSession.getInstance().getUsername();
        if (collector == null || collector.isBlank())
            collector = "Nhân viên thu tiền";

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Biên Lai Thu Tiền", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(22, 28, 18, 28));

        JTextPane tp = new JTextPane();
        tp.setContentType("text/html");
        tp.setEditable(false);
        tp.setBackground(Color.WHITE);

        String dt  = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        final String col = collector;
        tp.setText("<html><body style='font-family:Segoe UI,sans-serif;font-size:12px;color:#222;'>"
            + "<div style='text-align:center;margin-bottom:14px;'>"
            + "<h3 style='margin:0;font-size:15px;'>BIÊN LAI THANH TOÁN</h3>"
            + "<p style='margin:4px 0 0;font-size:10px;color:#666;'>CÔNG TY ĐIỆN LỰC G-LIGHT</p></div>"
            + "<div style='border-top:1px dashed #aaa;margin-bottom:12px;'></div>"
            + "<table style='width:100%;border-collapse:collapse;'>"
            + receiptRow("Mã hóa đơn:", "<b>" + currentBill.getBillCode() + "</b>")
            + receiptRow("Hộ gia đình:", currentBill.getOwnerName()
                         + " (" + currentBill.getHouseholdCode() + ")")
            + receiptRow("Kỳ hóa đơn:", currentBill.getPeriodDisplay())
            + receiptRow("Phương thức:", lastCompletedPayment.getPaymentMethod())
            + receiptRow("Ngày thu:", dt)
            + receiptRow("Người thu:", col)
            + "</table>"
            + "<div style='border-top:1px dashed #aaa;margin:10px 0;'></div>"
            + "<table style='width:100%;'>"
            + "<tr><td style='font-size:13px;font-weight:bold;'>TỔNG TIỀN:</td>"
            + "<td style='text-align:right;font-size:15px;font-weight:bold;color:#2E7D32;'>"
            + String.format("%,.0f đ", currentBill.getTotalAmount()) + "</td></tr>"
            + "</table>"
            + "<div style='border-bottom:1px dashed #aaa;margin:10px 0 18px;'></div>"
            + "<table style='width:100%;text-align:center;font-size:11px;'>"
            + "<tr><td style='width:50%;'><b>Khách hàng</b><br/><br/><br/>(Ký, ghi rõ họ tên)</td>"
            + "<td style='width:50%;'><b>Nhân viên thu tiền</b><br/><br/><br/>" + col + "</td></tr>"
            + "</table>"
            + "<div style='text-align:center;margin-top:28px;font-style:italic;font-size:10px;color:#777;'>"
            + "Cảm ơn quý khách đã thanh toán tiền điện!</div>"
            + "</body></html>");

        content.add(new JScrollPane(tp) {{ setBorder(null); }}, BorderLayout.CENTER);

        RoundedButton printBtn = new RoundedButton("🖨️  Xuất PDF & In",
                8, new Color(0x2E7D32));
        printBtn.setPreferredSize(new Dimension(150, 38));
        printBtn.addActionListener(e -> {
            pdfService.exportPaymentReceipt(currentBill.getBillCode(),
                    "C:/BienLai/" + currentBill.getBillCode() + ".pdf");
            dialog.dispose();
        });
        RoundedButton closeBtn = new RoundedButton("Đóng", 8, new Color(0x9E9E9E));
        closeBtn.setPreferredSize(new Dimension(80, 38));
        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);
        btnBar.add(closeBtn);
        btnBar.add(printBtn);
        content.add(btnBar, BorderLayout.SOUTH);

        dialog.add(content);
        dialog.setVisible(true);
    }

    private String receiptRow(String lbl, String val) {
        return "<tr><td style='padding:4px 0;color:#555;'>" + lbl + "</td>"
             + "<td style='padding:4px 0;text-align:right;'>" + val + "</td></tr>";
    }

    // =========================================================================
    // EXTERNAL REFRESH / RESET
    // =========================================================================
    public void refreshData() {
        if (searchField != null) searchField.getField().setText("");
        asyncLoadUnpaid("");
        asyncLoadHistory();
        clearForm();
        lastCompletedPayment = null;
    }

    private void clearForm() {
        currentBill = null;
        if (billCodeField  != null) billCodeField.setText("");
        if (householdField != null) householdField.setText("");
        if (periodField    != null) periodField.setText("");
        if (totalField     != null) totalField.setText("");
        if (statusField    != null) {
            statusField.setText("");
            statusField.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        }
        if (noteField  != null) noteField.setText("");
        if (payBtn     != null) payBtn.setEnabled(false);
        if (receiptBtn != null) receiptBtn.setEnabled(false);
        if (formHint   != null) formHint.setVisible(true);
    }

    // =========================================================================
    // INVOICE PANEL SYNC
    // =========================================================================
    private void notifyInvoicePanelReload() {
        Window win = SwingUtilities.getWindowAncestor(this);
        if (win != null) findAndReload(win);
    }

    private void findAndReload(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof InvoicePanel) { ((InvoicePanel) comp).reloadBills(); return; }
            if (comp instanceof Container)    findAndReload((Container) comp);
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private static RoundedTextField readOnly() {
        RoundedTextField f = new RoundedTextField();
        f.setEditable(false);
        return f;
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_NORMAL_BOLD);
        l.setForeground(UIConstants.COLOR_TEXT_SECONDARY);
        l.setPreferredSize(new Dimension(130, 30));
        return l;
    }
}
