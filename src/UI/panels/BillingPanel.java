// TÍNH TIỀN ĐIỆN

package UI.panels;

import UI.components.*;
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
import session.UserSession;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tính tiền điện — bảng tiêu thụ theo bậc thang và tổng tiền.
 * Hoàn toàn kết nối với SQL Server.
 */
public class BillingPanel extends BasePanel {

    private final PriceTierService priceTierService = new PriceTierServiceImpl();
    private final MeterReadingService readingService = new MeterReadingServiceImpl();
    private final BillService billService = new BillServiceImpl();

    private final JPanel tiersContainer;
    private final ModernTable table;
    private final SearchField searchField;
    private final RoundedButton configPricesBtn;

    private List<MeterReading> currentReadings;
    private Map<Integer, Bill> billMap = new HashMap<>();

    public BillingPanel() {
        super("Tính tiền điện", "Tính và xem chi tiết hóa đơn điện theo bậc thang EVN");

        // ---- Pricing info card wrapper ----
        GlassCard infoCard = new GlassCard(UIConstants.CARD_RADIUS, UIConstants.PRIMARY);
        infoCard.setLayout(new BorderLayout());
        infoCard.setPreferredSize(new Dimension(0, 90));

        tiersContainer = new JPanel(new GridLayout(1, 6, UIConstants.SP_SM, 0));
        tiersContainer.setOpaque(false);
        infoCard.add(tiersContainer, BorderLayout.CENTER);

        // ---- Toolbar ----
        JPanel toolbar = new JPanel(new BorderLayout(UIConstants.SP_SM, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(UIConstants.SP_MD, 0, UIConstants.SP_MD, 0));

        searchField = new SearchField("Tìm mã hộ, tên hộ...");
        searchField.getField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { reloadTable(); }
            @Override public void removeUpdate(DocumentEvent e) { reloadTable(); }
            @Override public void changedUpdate(DocumentEvent e) { reloadTable(); }
        });

        // Config prices button (Admin / Manager only via SYSTEM_MANAGE)
        boolean canConfig = util.PermissionManager.getInstance().hasPermission(model.Permission.SYSTEM_MANAGE);

        configPricesBtn = new RoundedButton("⚙  Cấu hình đơn giá", UIConstants.BUTTON_RADIUS, new Color(100, 110, 120));
        configPricesBtn.setPreferredSize(new Dimension(160, 36));
        configPricesBtn.setVisible(canConfig);
        configPricesBtn.addActionListener(e -> showPriceConfigDialog());

        RoundedButton calcAll = new RoundedButton("Tính tiền toàn bộ", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        calcAll.setPreferredSize(new Dimension(170, 36));
        calcAll.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.BILLING_BATCH));
        calcAll.addActionListener(e -> calculateAllBills());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(configPricesBtn);
        right.add(calcAll);
        
        toolbar.add(searchField, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        // ---- Table ----
        table = new ModernTable(new String[]{
            "Kỳ", "Mã Hộ", "Chủ hộ", "Tiêu thụ (kWh)", "Tổng tiền (tạm tính)", "Hóa đơn", "Thao tác"
        });
        table.setColumnWidths(80, 100, 180, 120, 150, 140, 120);

        // Status badge for Bill status column
        table.setColumnRenderer(5, (tbl, val, sel, foc, row, col) -> {
            String s = val == null ? "" : val.toString();
            if (s.isEmpty() || s.equals("Chưa tạo HĐ")) {
                return new StatusBadge("Chưa tạo HĐ", StatusBadge.Status.UNPAID);
            } else {
                // If it's a bill code, show as paid/unpaid status
                MeterReading r = currentReadings.get(row);
                Bill b = billMap.get(r.getReadingId());
                if (b != null && "PAID".equalsIgnoreCase(b.getPaymentStatus())) {
                    return new StatusBadge("Đã TT (" + s + ")", StatusBadge.Status.PAID);
                } else {
                    return new StatusBadge("Chưa TT (" + s + ")", StatusBadge.Status.UNPAID);
                }
            }
        });

        // Action buttons renderer
        table.setColumnRenderer(6, (tbl, val, sel, foc, row, col) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            p.setOpaque(false);
            
            MeterReading r = currentReadings.get(row);
            boolean hasBill = billMap.containsKey(r.getReadingId());

            if (!hasBill) {
                JButton calcBtn = actionBtn("Tính tiền", UIConstants.PRIMARY);
                calcBtn.addActionListener(e -> generateBill(r));
                calcBtn.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.BILLING_CALCULATE));
                p.add(calcBtn);
            } else {
                JButton detailBtn = actionBtn("Chi tiết", UIConstants.SUCCESS);
                detailBtn.addActionListener(e -> showTierBreakdown(r));
                p.add(detailBtn);
            }
            return p;
        });

        // Initial Data Loads
        loadPriceTiersCard();
        reloadTable();

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setOpaque(false);
        root.add(infoCard, BorderLayout.NORTH);
        root.add(toolbar, BorderLayout.CENTER);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.add(table, BorderLayout.CENTER);

        contentArea.add(root, BorderLayout.NORTH);
        contentArea.add(tableWrap, BorderLayout.CENTER);
    }

    private JButton actionBtn(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(UIConstants.FONT_SMALL_BOLD);
        b.setForeground(color);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(75, 26));
        return b;
    }

    // ====================================================================
    // DATA LOADING
    // ====================================================================

    private void loadPriceTiersCard() {
        tiersContainer.removeAll();
        List<PriceTier> list = priceTierService.getAllTiers();
        int idx = 1;
        for (PriceTier tier : list) {
            JPanel tc = new JPanel();
            tc.setOpaque(false);
            tc.setLayout(new BoxLayout(tc, BoxLayout.Y_AXIS));
            
            JLabel name = new JLabel("Bậc " + idx++); 
            name.setFont(UIConstants.FONT_SMALL_BOLD); 
            name.setForeground(UIConstants.PRIMARY);
            
            JLabel range = new JLabel(tier.getRangeDisplay()); 
            range.setFont(UIConstants.FONT_SMALL);      
            range.setForeground(UIConstants.COLOR_TEXT_MUTED);
            
            JLabel price = new JLabel(tier.getPriceDisplay()); 
            price.setFont(UIConstants.FONT_SMALL_BOLD); 
            price.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
            
            name.setAlignmentX(CENTER_ALIGNMENT);
            range.setAlignmentX(CENTER_ALIGNMENT);
            price.setAlignmentX(CENTER_ALIGNMENT);
            
            tc.add(Box.createVerticalGlue());
            tc.add(name); 
            tc.add(range); 
            tc.add(price);
            tc.add(Box.createVerticalGlue());
            tiersContainer.add(tc);
        }
        tiersContainer.revalidate();
        tiersContainer.repaint();
    }

    public void reloadTable() {
        if (table == null) return;
        String keyword = searchField.getText().trim();
        currentReadings = readingService.search(keyword.isEmpty() ? null : keyword, null, null);
        
        // Fetch all bills to map readingId -> Bill
        List<Bill> bills = billService.getAll();
        billMap.clear();
        for (Bill b : bills) {
            billMap.put(b.getReadingId(), b);
        }

        table.clearRows();
        for (MeterReading r : currentReadings) {
            boolean hasBill = billMap.containsKey(r.getReadingId());
            double total;
            String billCodeStr;

            if (hasBill) {
                Bill b = billMap.get(r.getReadingId());
                total = b.getTotalAmount();
                billCodeStr = b.getBillCode();
            } else {
                total = priceTierService.calculateTotal(r.getConsumption());
                billCodeStr = "Chưa tạo HĐ";
            }

            table.addRow(new Object[]{
                r.getPeriodDisplay(),
                r.getHouseholdCode(),
                r.getOwnerName(),
                String.format("%.0f", r.getConsumption()),
                String.format("%,.0f đ", total),
                billCodeStr,
                "" // action column handled by renderer
            });
        }
    }

    // ====================================================================
    // ACTIONS
    // ====================================================================

    private void generateBill(MeterReading r) {
        if (!util.PermissionManager.getInstance().checkPermission(model.Permission.BILLING_CALCULATE)) {
            return;
        }

        String err = billService.generateBill(r.getReadingId());
        if (err != null) {
            ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(this), err, "Lỗi");
        } else {
            ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                "Đã tính tiền và tạo hóa đơn cho hộ " + r.getHouseholdCode() + "!",
                ToastNotification.Type.SUCCESS);
            reloadTable();
        }
    }

    private void calculateAllBills() {
        if (!util.PermissionManager.getInstance().checkPermission(model.Permission.BILLING_BATCH)) {
            return;
        }

        int count = 0;
        int errCount = 0;
        for (MeterReading r : currentReadings) {
            if (!billMap.containsKey(r.getReadingId())) {
                String err = billService.generateBill(r.getReadingId());
                if (err == null) {
                    count++;
                } else {
                    errCount++;
                }
            }
        }

        if (count > 0) {
            ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                "Đã tính tiền thành công cho " + count + " hộ!",
                ToastNotification.Type.SUCCESS);
            reloadTable();
        } else if (errCount > 0) {
            ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(this),
                "Có lỗi xảy ra khi tính tiền cho một số hộ!", "Lỗi");
        } else {
            ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                "Tất cả hộ trong danh sách đều đã có hóa đơn!",
                ToastNotification.Type.INFO);
        }
    }

    private void showTierBreakdown(MeterReading r) {
        double consumption = r.getConsumption();
        List<PriceTier> tiers = priceTierService.getAllTiers();

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Segoe UI; font-size: 13px; color: #1E2D2D;'>");
        html.append("<h3 style='color:#5D6B6B; margin-top:0;'>📊 CHI TIẾT TÍNH TIỀN ĐIỆN</h3>");
        html.append("<b>Hộ gia đình:</b> ").append(r.getOwnerName()).append(" (").append(r.getHouseholdCode()).append(")<br/>");
        html.append("<b>Kỳ hóa đơn:</b> Tháng ").append(r.getPeriodDisplay()).append("<br/>");
        html.append("<b>Chỉ số cũ:</b> ").append(String.format("%.0f", r.getOldIndex())).append(" kWh | ");
        html.append("<b>Chỉ số mới:</b> ").append(String.format("%.0f", r.getNewIndex())).append(" kWh<br/>");
        html.append("<b>Tổng tiêu thụ:</b> <span style='font-size:14px; color:#3D9970;'><b>").append(String.format("%.0f", consumption)).append(" kWh</b></span><br/><br/>");

        html.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse; border-color:#DDE4E4;'>");
        html.append("<tr style='background-color:#5D6B6B; color:white;'>");
        html.append("<th>Bậc</th><th>Mức kWh</th><th>Tiêu thụ (kWh)</th><th>Đơn giá (đ)</th><th>Thành tiền (đ)</th>");
        html.append("</tr>");

        double remaining = consumption;
        double total = 0;
        int idx = 1;

        for (PriceTier tier : tiers) {
            if (remaining <= 0) {
                html.append("<tr style='color:#9EADAD;'>");
                html.append("<td align='center'>Bậc ").append(idx++).append("</td>");
                html.append("<td>").append(tier.getRangeDisplay()).append("</td>");
                html.append("<td align='right'>0</td>");
                html.append("<td align='right'>").append(String.format("%,.0f", tier.getUnitPrice())).append("</td>");
                html.append("<td align='right'>0</td>");
                html.append("</tr>");
                continue;
            }

            int from = tier.getLevelFrom();
            int to = tier.getLevelTo();
            double bandSize = (to < 0) ? remaining : Math.min(remaining, to - from + 1);
            double amount = bandSize * tier.getUnitPrice();
            total += amount;
            remaining -= bandSize;

            html.append("<tr style='background-color:#F4F9FB;'>");
            html.append("<td align='center'><b>Bậc ").append(idx++).append("</b></td>");
            html.append("<td>").append(tier.getRangeDisplay()).append("</td>");
            html.append("<td align='right'><b>").append(String.format("%.0f", bandSize)).append("</b></td>");
            html.append("<td align='right'>").append(String.format("%,.0f", tier.getUnitPrice())).append("</td>");
            html.append("<td align='right'><b>").append(String.format("%,.0f", amount)).append("</b></td>");
            html.append("</tr>");
        }

        html.append("<tr style='background-color:#E8F3F8;'>");
        html.append("<td colspan='4' align='right'><b>Tổng tiền điện (chưa thuế):</b></td>");
        html.append("<td align='right' style='color:#C0392B;'><b>").append(String.format("%,.0f đ", total)).append("</b></td>");
        html.append("</tr>");
        
        // 8% VAT
        double vat = total * 0.08;
        double grandTotal = total + vat;
        html.append("<tr style='background-color:#E8F3F8;'>");
        html.append("<td colspan='4' align='right'><b>Thuế VAT (8%):</b></td>");
        html.append("<td align='right'>").append(String.format("%,.0f đ", vat)).append("</td>");
        html.append("</tr>");

        html.append("<tr style='background-color:#DDE4E4;'>");
        html.append("<td colspan='4' align='right'><b>TỔNG THANH TOÁN:</b></td>");
        html.append("<td align='right' style='color:#C0392B; font-size:14px;'><b>").append(String.format("%,.0f đ", grandTotal)).append("</b></td>");
        html.append("</tr>");

        html.append("</table>");
        html.append("</body></html>");

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Chi tiết hóa đơn", Dialog.ModalityType.APPLICATION_MODAL);
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

    private void showPriceConfigDialog() {
        if (!util.PermissionManager.getInstance().checkPermission(model.Permission.SYSTEM_MANAGE)) {
            return;
        }

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Cấu hình đơn giá điện bậc thang", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(450, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
        JLabel title = new JLabel("⚙  Điều chỉnh đơn giá EVN (đ/kWh)");
        title.setFont(UIConstants.FONT_SUBHEADER);
        title.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        header.add(title);
        dlg.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(6, 2, 10, 10));
        body.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        List<PriceTier> list = priceTierService.getAllTiers();
        RoundedTextField[] fields = new RoundedTextField[list.size()];

        for (int i = 0; i < list.size(); i++) {
            PriceTier tier = list.get(i);
            JLabel label = new JLabel("Bậc " + (i + 1) + " (" + tier.getRangeDisplay() + "):");
            label.setFont(UIConstants.FONT_NORMAL_BOLD);
            
            fields[i] = new RoundedTextField();
            fields[i].setText(String.format("%.0f", tier.getUnitPrice()));
            fields[i].setPreferredSize(new Dimension(150, 34));

            body.add(label);
            body.add(fields[i]);
        }
        dlg.add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 15));

        RoundedButton cancel = new RoundedButton("Hủy bỏ", UIConstants.BUTTON_RADIUS, new Color(150, 150, 170));
        RoundedButton save = new RoundedButton("Lưu thay đổi", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        cancel.setPreferredSize(new Dimension(100, 36));
        save.setPreferredSize(new Dimension(130, 36));

        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            // Save logic
            for (int i = 0; i < list.size(); i++) {
                String txt = fields[i].getText().trim();
                double newPrice;
                try {
                    newPrice = Double.parseDouble(txt);
                    if (newPrice <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    ThemeManager.showInfoDialog(dlg, "Đơn giá bậc " + (i + 1) + " phải là số dương hợp lệ!", "Lỗi nhập liệu");
                    return;
                }
                PriceTier tier = list.get(i);
                tier.setUnitPrice(newPrice);
                String err = priceTierService.updateTier(tier);
                if (err != null) {
                    ThemeManager.showInfoDialog(dlg, err, "Lỗi cơ sở dữ liệu");
                    return;
                }
            }

            ToastNotification.show(SwingUtilities.getWindowAncestor(this), "Đã cập nhật bảng giá điện thành công!", ToastNotification.Type.SUCCESS);
            loadPriceTiersCard();
            reloadTable();
            dlg.dispose();
        });

        footer.add(cancel);
        footer.add(save);
        dlg.add(footer, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }
}
