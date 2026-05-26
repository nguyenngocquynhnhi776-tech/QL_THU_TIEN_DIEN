package UI.panels;

import UI.components.*;
import UI.services.PdfExportService;
import UI.services.StubPdfExportService;
import UI.theme.ThemeManager;
import model.Bill;
import model.MeterReading;
import model.PriceTier;
import service.BillService;
import service.MeterReadingService;
import service.PriceTierService;
import service.impl.BillServiceImpl;
import service.impl.MeterReadingServiceImpl;
import service.impl.PriceTierServiceImpl;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Quản lý hóa đơn — danh sách, trạng thái, xuất PDF.
 * Hoàn toàn kết nối với SQL Server.
 */
public class InvoicePanel extends BasePanel {

    private final BillService billService = new BillServiceImpl();
    private final MeterReadingService readingService = new MeterReadingServiceImpl();
    private final PriceTierService priceTierService = new PriceTierServiceImpl();
    private final PdfExportService pdfService = StubPdfExportService.getInstance();

    private final ModernTable table;
    private final SearchField searchField;
    private final JComboBox<String> statusFilter;
    private final JSpinner monthSpinner;
    private final JSpinner yearSpinner;

    private List<Bill> currentBills;

    public InvoicePanel() {
        super("Hóa đơn", "Xem và quản lý hóa đơn điện hàng tháng");

        // ---- Toolbar ----
        JPanel toolbar = new JPanel(new BorderLayout(UIConstants.SP_SM, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, UIConstants.SP_MD, 0));

        searchField = new SearchField("Tìm mã hóa đơn, tên hộ...");
        searchField.getField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { reloadTable(); }
            @Override public void removeUpdate(DocumentEvent e) { reloadTable(); }
            @Override public void changedUpdate(DocumentEvent e) { reloadTable(); }
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SP_SM, 0));
        filters.setOpaque(false);

        // Status filter
        statusFilter = new JComboBox<>(new String[]{"Tất cả trạng thái", "Chưa thanh toán", "Đã thanh toán"});
        statusFilter.setFont(UIConstants.FONT_NORMAL);
        statusFilter.setPreferredSize(new Dimension(150, 36));
        statusFilter.addActionListener(e -> reloadTable());

        // Month / Year filters
        LocalDate now = LocalDate.now();
        monthSpinner = new JSpinner(new SpinnerNumberModel(now.getMonthValue(), 1, 12, 1));
        monthSpinner.setFont(UIConstants.FONT_NORMAL);
        monthSpinner.setPreferredSize(new Dimension(60, 36));
        monthSpinner.addChangeListener(e -> reloadTable());

        yearSpinner = new JSpinner(new SpinnerNumberModel(now.getYear(), 2000, 2100, 1));
        yearSpinner.setFont(UIConstants.FONT_NORMAL);
        yearSpinner.setPreferredSize(new Dimension(80, 36));
        ((JSpinner.NumberEditor) yearSpinner.getEditor()).getFormat().setGroupingUsed(false);
        yearSpinner.addChangeListener(e -> reloadTable());

        RoundedButton genBtn = new RoundedButton("Tạo hóa đơn tháng", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        genBtn.setPreferredSize(new Dimension(185, 36));
        genBtn.addActionListener(e -> generateCurrentMonthBills());

        filters.add(new JLabel("Tháng/Năm: "));
        filters.add(monthSpinner);
        filters.add(yearSpinner);
        filters.add(statusFilter);
        filters.add(genBtn);
        
        toolbar.add(searchField, BorderLayout.WEST);
        toolbar.add(filters, BorderLayout.EAST);

        // ---- Table ----
        table = new ModernTable(new String[]{"Mã hóa đơn", "Hộ gia đình", "Kỳ", "Tổng tiền", "Trạng thái", "Thao tác"});
        table.setColumnWidths(110, 200, 80, 130, 140, 150);

        // Status badge
        table.setColumnRenderer(4, (tbl, val, sel, foc, row, col) -> {
            String s = val == null ? "" : val.toString();
            StatusBadge.Status st = "PAID".equalsIgnoreCase(s) || "Đã thanh toán".equalsIgnoreCase(s)
                ? StatusBadge.Status.PAID : StatusBadge.Status.UNPAID;
            String display = st == StatusBadge.Status.PAID ? "Đã thanh toán" : "Chưa thanh toán";
            return new StatusBadge(display, st);
        });

        // Action buttons
        table.setColumnRenderer(5, (tbl, val, sel, foc, row, col) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            p.setOpaque(false);
            JButton pdf = actionBtn("In PDF", UIConstants.PRIMARY);
            JButton view = actionBtn("Xem", UIConstants.SUCCESS);
            
            Bill bill = currentBills.get(row);
            pdf.addActionListener(e -> {
                String billCode = bill.getBillCode();
                pdfService.exportInvoice(billCode, "C:/HoaDon/" + billCode + ".pdf");
            });
            view.addActionListener(e -> showBillDetail(bill));
            
            p.add(view); 
            p.add(pdf);
            return p;
        });

        reloadTable();

        contentArea.add(toolbar, BorderLayout.NORTH);
        contentArea.add(table, BorderLayout.CENTER);
    }

    private JButton actionBtn(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(UIConstants.FONT_SMALL_BOLD);
        b.setForeground(color);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(58, 26));
        return b;
    }

    // ====================================================================
    // DATA LOADING & ACTIONS
    // ====================================================================

    private void reloadTable() {
        if (table == null) return;
        String keyword = searchField.getText().trim();
        int filterIndex = statusFilter.getSelectedIndex();
        String paymentStatus = null;
        if (filterIndex == 1) paymentStatus = "UNPAID";
        else if (filterIndex == 2) paymentStatus = "PAID";

        int month = (int) monthSpinner.getValue();
        int year = (int) yearSpinner.getValue();

        currentBills = billService.search(
            keyword.isEmpty() ? null : keyword,
            paymentStatus,
            month,
            year
        );

        table.clearRows();
        for (Bill b : currentBills) {
            table.addRow(new Object[]{
                b.getBillCode(),
                b.getHouseholdCode() + " - " + b.getOwnerName(),
                String.format("%02d/%04d", b.getMonth(), b.getYear()),
                String.format("%,.0f đ", b.getTotalAmount()),
                b.getPaymentStatus(),
                "" // action column
            });
        }
    }

    private void generateCurrentMonthBills() {
        int month = (int) monthSpinner.getValue();
        int year = (int) yearSpinner.getValue();

        boolean ok = ThemeManager.showConfirmDialog(this,
            "Tạo hóa đơn điện cho tất cả hộ có chỉ số tháng " + String.format("%02d/%04d", month, year) + "?",
            "Tạo hóa đơn tháng");
        if (!ok) return;

        List<MeterReading> readings = readingService.search(null, month, year);
        int generatedCount = 0;
        for (MeterReading r : readings) {
            // Checks if a bill already exists for this reading
            String err = billService.generateBill(r.getReadingId());
            if (err == null) {
                generatedCount++;
            }
        }

        if (generatedCount > 0) {
            ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                "Đã tạo thành công " + generatedCount + " hóa đơn cho tháng " + String.format("%02d/%04d", month, year) + "!",
                ToastNotification.Type.SUCCESS);
            reloadTable();
        } else {
            ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                "Không có hóa đơn mới nào được tạo (đã có đủ hoặc chưa nhập chỉ số).",
                ToastNotification.Type.INFO);
        }
    }

    private void showBillDetail(Bill b) {
        MeterReading r = readingService.getById(b.getReadingId());
        if (r == null) {
            ThemeManager.showInfoDialog(this, "Không tìm thấy thông tin chỉ số điện của hóa đơn này!", "Lỗi");
            return;
        }

        double consumption = r.getConsumption();
        List<PriceTier> tiers = priceTierService.getAllTiers();

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Segoe UI; font-size: 13px; color: #1E2D2D;'>");
        html.append("<h3 style='color:#5D6B6B; margin-top:0;'>📄 CHI TIẾT HÓA ĐƠN ĐIỆN</h3>");
        html.append("<table cellspacing='0' cellpadding='3' style='margin-bottom: 12px;'>");
        html.append("<tr><td><b>Mã hóa đơn:</b></td><td style='color:#C0392B;'><b>").append(b.getBillCode()).append("</b></td></tr>");
        html.append("<tr><td><b>Khách hàng:</b></td><td>").append(r.getOwnerName()).append(" (").append(r.getHouseholdCode()).append(")</td></tr>");
        html.append("<tr><td><b>Khu vực:</b></td><td>").append(r.getAreaName() != null ? r.getAreaName() : "N/A").append("</td></tr>");
        html.append("<tr><td><b>Kỳ hóa đơn:</b></td><td>Tháng ").append(r.getPeriodDisplay()).append("</td></tr>");
        html.append("<tr><td><b>Chỉ số cũ:</b></td><td>").append(String.format("%.0f", r.getOldIndex())).append(" kWh</td></tr>");
        html.append("<tr><td><b>Chỉ số mới:</b></td><td>").append(String.format("%.0f", r.getNewIndex())).append(" kWh</td></tr>");
        html.append("<tr><td><b>Sản lượng:</b></td><td><span style='font-size:13px; color:#3D9970;'><b>").append(String.format("%.0f", consumption)).append(" kWh</b></span></td></tr>");
        
        String stat = "PAID".equalsIgnoreCase(b.getPaymentStatus()) ? "<span style='color:#3D9970;'><b>ĐÃ THANH TOÁN</b></span>" : "<span style='color:#C0392B;'><b>CHƯA THANH TOÁN</b></span>";
        html.append("<tr><td><b>Trạng thái:</b></td><td>").append(stat).append("</td></tr>");
        html.append("</table>");

        html.append("<table border='1' cellspacing='0' cellpadding='5' style='border-collapse:collapse; border-color:#DDE4E4;'>");
        html.append("<tr style='background-color:#5D6B6B; color:white;'>");
        html.append("<th>Bậc</th><th>Mức kWh</th><th>Tiêu thụ (kWh)</th><th>Đơn giá (đ)</th><th>Thành tiền (đ)</th>");
        html.append("</tr>");

        double remaining = consumption;
        double total = 0;
        int idx = 1;

        for (PriceTier tier : tiers) {
            if (remaining <= 0) continue;

            int from = tier.getLevelFrom();
            int to = tier.getLevelTo();
            double bandSize = (to < 0) ? remaining : Math.min(remaining, to - from + 1);
            double amount = bandSize * tier.getUnitPrice();
            total += amount;
            remaining -= bandSize;

            html.append("<tr style='background-color:#F4F9FB;'>");
            html.append("<td align='center'>Bậc ").append(idx++).append("</td>");
            html.append("<td>").append(tier.getRangeDisplay()).append("</td>");
            html.append("<td align='right'>").append(String.format("%.0f", bandSize)).append("</td>");
            html.append("<td align='right'>").append(String.format("%,.0f", tier.getUnitPrice())).append("</td>");
            html.append("<td align='right'>").append(String.format("%,.0f", amount)).append("</td>");
            html.append("</tr>");
        }

        html.append("<tr style='background-color:#E8F3F8;'>");
        html.append("<td colspan='4' align='right'><b>Tổng tiền điện (chưa thuế):</b></td>");
        html.append("<td align='right'><b>").append(String.format("%,.0f đ", total)).append("</b></td>");
        html.append("</tr>");

        double vat = total * 0.08;
        double grandTotal = total + vat;
        html.append("<tr style='background-color:#E8F3F8;'>");
        html.append("<td colspan='4' align='right'><b>Thuế VAT (8%):</b></td>");
        html.append("<td align='right'>").append(String.format("%,.0f đ", vat)).append("</td>");
        html.append("</tr>");

        html.append("<tr style='background-color:#DDE4E4;'>");
        html.append("<td colspan='4' align='right'><b>TỔNG THANH TOÁN:</b></td>");
        html.append("<td align='right' style='color:#C0392B; font-size:13px;'><b>").append(String.format("%,.0f đ", grandTotal)).append("</b></td>");
        html.append("</tr>");

        html.append("</table>");
        html.append("</body></html>");

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Hóa đơn điện chi tiết", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());
        
        JEditorPane pane = new JEditorPane("text/html", html.toString());
        pane.setEditable(false);
        pane.setBackground(new Color(255, 255, 255, 245));
        
        JScrollPane scroll = new JScrollPane(pane);
        scroll.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        dlg.add(scroll, BorderLayout.CENTER);

        RoundedButton closeBtn = new RoundedButton("Đóng", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        closeBtn.setPreferredSize(new Dimension(90, 36));
        closeBtn.addActionListener(e -> dlg.dispose());
        
        JPanel pnlClose = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlClose.setOpaque(false);
        pnlClose.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        pnlClose.add(closeBtn);
        dlg.add(pnlClose, BorderLayout.SOUTH);

        dlg.setSize(550, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }
}
