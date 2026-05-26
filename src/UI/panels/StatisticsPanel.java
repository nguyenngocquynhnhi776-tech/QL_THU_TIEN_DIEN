package UI.panels;

import UI.charts.BarChartPanel;
import UI.charts.LineChartPanel;
import UI.components.*;
import UI.services.PdfExportService;
import UI.services.StubPdfExportService;
import UI.theme.ThemeManager;
import model.Area;
import model.Bill;
import model.MeterReading;
import service.AreaService;
import service.BillService;
import service.MeterReadingService;
import service.PaymentService;
import service.impl.AreaServiceImpl;
import service.impl.BillServiceImpl;
import service.impl.MeterReadingServiceImpl;
import service.impl.PaymentServiceImpl;
import util.ReportQueryHelper;
import util.XlsxExporter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Thống kê & Báo cáo — phân tích doanh thu, sản lượng, xuất báo cáo Excel & HTML chuyên nghiệp.
 * Hoàn toàn kết nối với SQL Server.
 */
public class StatisticsPanel extends BasePanel {

    private final BillService billService = new BillServiceImpl();
    private final MeterReadingService readingService = new MeterReadingServiceImpl();
    private final PaymentService paymentService = new PaymentServiceImpl();
    private final AreaService areaService = new AreaServiceImpl();

    // Stats filters
    private final JSpinner monthSpinner;
    private final JSpinner yearSpinner;
    private final JPanel summaryRow;
    private final ModernTable overdueTable;

    private LineChartPanel revChartPanel;
    private BarChartPanel kwhChartPanel;

    // Reports & Export components
    private JComboBox<String> reportBox;
    private JComboBox<Object> areaFilterBox; // loads Areas from DB
    private JComboBox<String> statusFilterBox;
    private JSpinner rMonthSpinner;
    private JSpinner rYearSpinner;
    private RoundedTextField fromDateTxt;
    private RoundedTextField toDateTxt;

    private JLabel areaLbl;
    private JLabel statusLbl;
    private JLabel periodLbl;
    private JLabel dateRangeLbl;

    public StatisticsPanel() {
        super("Thống kê & Báo cáo", "Phân tích doanh thu và xuất báo cáo nghiệp vụ");

        // ====================================================================
        // TOOLBAR & FILTER
        // ====================================================================
        JPanel toolbar = new JPanel(new BorderLayout(UIConstants.SP_SM, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, UIConstants.SP_MD, 0));

        JPanel leftFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, UIConstants.SP_SM, 0));
        leftFilter.setOpaque(false);
        leftFilter.add(label("Kỳ báo cáo: "));

        LocalDate now = LocalDate.now();
        monthSpinner = new JSpinner(new SpinnerNumberModel(now.getMonthValue(), 1, 12, 1));
        monthSpinner.setFont(UIConstants.FONT_NORMAL);
        monthSpinner.setPreferredSize(new Dimension(60, 34));

        yearSpinner = new JSpinner(new SpinnerNumberModel(now.getYear(), 2000, 2100, 1));
        yearSpinner.setFont(UIConstants.FONT_NORMAL);
        yearSpinner.setPreferredSize(new Dimension(80, 34));
        ((JSpinner.NumberEditor) yearSpinner.getEditor()).getFormat().setGroupingUsed(false);

        RoundedButton viewBtn = new RoundedButton("Xem thống kê", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        viewBtn.setPreferredSize(new Dimension(140, 34));
        viewBtn.addActionListener(e -> updateReport());

        leftFilter.add(monthSpinner);
        leftFilter.add(yearSpinner);
        leftFilter.add(viewBtn);

        toolbar.add(leftFilter, BorderLayout.WEST);

        // ====================================================================
        // STATS SUMMARY ROW
        // ====================================================================
        summaryRow = new JPanel(new GridLayout(1, 4, UIConstants.SP_MD, 0));
        summaryRow.setOpaque(false);
        summaryRow.setPreferredSize(new Dimension(0, 120));

        // ====================================================================
        // CHARTS ROW
        // ====================================================================
        JPanel chartsRow = new JPanel(new GridLayout(1, 2, UIConstants.SP_MD, 0));
        chartsRow.setOpaque(false);
        chartsRow.setPreferredSize(new Dimension(0, 270));

        GlassCard revCard = new GlassCard(UIConstants.CARD_RADIUS);
        revCard.setLayout(new BorderLayout());
        revChartPanel = new LineChartPanel("Doanh thu 6 tháng gần nhất (VND)", new double[]{0,0,0,0,0,0}, new String[]{"","","","","",""}, UIConstants.PRIMARY, "đ");
        revCard.add(revChartPanel, BorderLayout.CENTER);

        GlassCard kwhCard = new GlassCard(UIConstants.CARD_RADIUS);
        kwhCard.setLayout(new BorderLayout());
        kwhChartPanel = new BarChartPanel("Sản lượng điện tiêu thụ (kWh)", new double[]{0,0,0,0,0,0}, new String[]{"","","","","",""}, UIConstants.SUCCESS);
        kwhCard.add(kwhChartPanel, BorderLayout.CENTER);

        chartsRow.add(revCard);
        chartsRow.add(kwhCard);

        // ====================================================================
        // OVERDUE DEBT TABLE
        // ====================================================================
        GlassCard overdueCard = new GlassCard(UIConstants.CARD_RADIUS);
        overdueCard.setLayout(new BorderLayout(0, UIConstants.SP_SM));
        overdueCard.setPreferredSize(new Dimension(0, 200));

        JLabel overdueTitle = new JLabel("⚠️  Danh sách hộ nợ đọng lâu nhất");
        overdueTitle.setFont(UIConstants.FONT_SUBHEADER);
        overdueTitle.setForeground(UIConstants.WARNING);

        overdueTable = new ModernTable(
            new String[]{"Mã HĐ", "Hộ gia đình", "Kỳ hóa đơn", "Số tiền nợ", "Ngày lập"}
        );
        overdueTable.setColumnWidths(110, 220, 100, 150, 150);

        overdueCard.add(overdueTitle,  BorderLayout.NORTH);
        overdueCard.add(overdueTable,  BorderLayout.CENTER);

        // ====================================================================
        // PROFESSIONAL REPORT & EXPORT CARD
        // ====================================================================
        GlassCard exportCard = new GlassCard(UIConstants.CARD_RADIUS);
        exportCard.setLayout(new BorderLayout(0, UIConstants.SP_MD));
        exportCard.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
        exportCard.setPreferredSize(new Dimension(0, 200));

        JLabel exportTitle = new JLabel("📋  Xuất báo cáo nghiệp vụ chuyên nghiệp");
        exportTitle.setFont(UIConstants.FONT_SUBHEADER);
        exportTitle.setForeground(UIConstants.COLOR_TEXT_PRIMARY);

        JPanel exportBody = new JPanel(new GridBagLayout());
        exportBody.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: Report Selector & Area & Status Filters
        reportBox = new JComboBox<>(new String[]{
            "Danh sách hộ gia đình (Household Report)",
            "Bảng kê hóa đơn điện (Billing Report)",
            "Báo cáo tổng hợp doanh thu (Revenue Report)",
            "Báo cáo lịch sử thu tiền (Payment Report)",
            "Báo cáo sản lượng khu vực (Area Report)"
        });
        reportBox.setFont(UIConstants.FONT_NORMAL);
        reportBox.setPreferredSize(new Dimension(280, 34));
        reportBox.addActionListener(e -> handleReportTypeChange());

        areaFilterBox = new JComboBox<>();
        areaFilterBox.setFont(UIConstants.FONT_NORMAL);
        areaFilterBox.setPreferredSize(new Dimension(150, 34));
        loadAreaDropdown();

        statusFilterBox = new JComboBox<>();
        statusFilterBox.setFont(UIConstants.FONT_NORMAL);
        statusFilterBox.setPreferredSize(new Dimension(130, 34));

        gc.gridy = 0; gc.weightx = 0;
        gc.gridx = 0; exportBody.add(label("Loại báo cáo:"), gc);
        gc.gridx = 1; gc.weightx = 0.4; exportBody.add(reportBox, gc);

        areaLbl = label("Khu vực:");
        gc.gridx = 2; gc.weightx = 0; exportBody.add(areaLbl, gc);
        gc.gridx = 3; gc.weightx = 0.2; exportBody.add(areaFilterBox, gc);

        statusLbl = label("Trạng thái:");
        gc.gridx = 4; gc.weightx = 0; exportBody.add(statusLbl, gc);
        gc.gridx = 5; gc.weightx = 0.2; exportBody.add(statusFilterBox, gc);

        // Row 2: Month/Year spinners & Date range filters
        rMonthSpinner = new JSpinner(new SpinnerNumberModel(now.getMonthValue(), 1, 12, 1));
        rMonthSpinner.setFont(UIConstants.FONT_NORMAL);
        rMonthSpinner.setPreferredSize(new Dimension(55, 34));

        rYearSpinner = new JSpinner(new SpinnerNumberModel(now.getYear(), 2000, 2100, 1));
        rYearSpinner.setFont(UIConstants.FONT_NORMAL);
        rYearSpinner.setPreferredSize(new Dimension(75, 34));
        ((JSpinner.NumberEditor) rYearSpinner.getEditor()).getFormat().setGroupingUsed(false);

        fromDateTxt = new RoundedTextField();
        fromDateTxt.setText(now.withDayOfMonth(1).toString());
        fromDateTxt.setPreferredSize(new Dimension(100, 34));

        toDateTxt = new RoundedTextField();
        toDateTxt.setText(now.toString());
        toDateTxt.setPreferredSize(new Dimension(100, 34));

        JPanel spinWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        spinWrap.setOpaque(false);
        spinWrap.add(rMonthSpinner);
        spinWrap.add(rYearSpinner);

        gc.gridy = 1; gc.weightx = 0;
        periodLbl = label("Kỳ (Tháng/Năm):");
        gc.gridx = 0; exportBody.add(periodLbl, gc);
        gc.gridx = 1; gc.weightx = 0.4; exportBody.add(spinWrap, gc);

        dateRangeLbl = label("Từ ngày - Đến ngày:");
        gc.gridx = 2; gc.weightx = 0; exportBody.add(dateRangeLbl, gc);
        
        JPanel rangeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rangeWrap.setOpaque(false);
        rangeWrap.add(fromDateTxt);
        rangeWrap.add(new JLabel("đến"));
        rangeWrap.add(toDateTxt);
        gc.gridx = 3; gc.gridwidth = 3; gc.weightx = 0.4; exportBody.add(rangeWrap, gc);
        gc.gridwidth = 1;

        // Row 3: Action Buttons
        RoundedButton exportExcelBtn = new RoundedButton("📊  Xuất Excel (.xlsx)", UIConstants.BUTTON_RADIUS, UIConstants.SUCCESS);
        exportExcelBtn.setPreferredSize(new Dimension(160, 36));
        exportExcelBtn.addActionListener(e -> executeExcelExport());

        RoundedButton printReportBtn = new RoundedButton("📄  Xem / In Báo cáo", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        printReportBtn.setPreferredSize(new Dimension(160, 36));
        printReportBtn.addActionListener(e -> executeHtmlPrint());

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.add(printReportBtn);
        actionsPanel.add(exportExcelBtn);

        gc.gridy = 2; gc.gridx = 0; gc.gridwidth = 6; gc.weightx = 1.0;
        exportBody.add(actionsPanel, gc);

        exportCard.add(exportTitle, BorderLayout.NORTH);
        exportCard.add(exportBody,  BorderLayout.CENTER);

        // Assemble root layout
        JPanel topBlock = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        topBlock.setOpaque(false);
        topBlock.add(toolbar,     BorderLayout.NORTH);
        topBlock.add(summaryRow,  BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        root.setOpaque(false);
        root.add(topBlock,    BorderLayout.NORTH);
        root.add(chartsRow,   BorderLayout.CENTER);
        root.add(overdueCard, BorderLayout.CENTER);
        root.add(exportCard,  BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentArea.add(scrollPane, BorderLayout.CENTER);

        handleReportTypeChange(); // align dynamic controls
        updateReport(); // trigger dynamic database stats load
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_NORMAL_BOLD);
        l.setForeground(UIConstants.COLOR_TEXT_SECONDARY);
        return l;
    }

    // ====================================================================
    // DATA LOADS
    // ====================================================================

    private void loadAreaDropdown() {
        areaFilterBox.removeAllItems();
        areaFilterBox.addItem("Tất cả khu vực");
        List<Area> list = areaService.getAllActive();
        for (Area a : list) {
            areaFilterBox.addItem(a);
        }
    }

    private void updateReport() {
        if (overdueTable == null) return;
        int month = (int) monthSpinner.getValue();
        int year = (int) yearSpinner.getValue();

        // 1. Live statistics from SQL Server
        double monthlyRev = paymentService.getMonthlyRevenue(month, year);
        
        List<MeterReading> readings = readingService.search(null, month, year);
        double totalKwh = 0;
        for (MeterReading r : readings) {
            totalKwh += r.getConsumption();
        }

        List<Bill> bills = billService.search(null, null, month, year);
        int paidCount = 0;
        int unpaidCount = 0;
        double unpaidSum = 0;
        for (Bill b : bills) {
            if ("PAID".equalsIgnoreCase(b.getPaymentStatus())) {
                paidCount++;
            } else {
                unpaidCount++;
                unpaidSum += b.getTotalAmount();
            }
        }

        int totalBills = paidCount + unpaidCount;
        String paidPct = totalBills > 0 ? String.format("%d%%", (paidCount * 100) / totalBills) : "—";
        String paidDisplay = totalBills > 0 ? paidCount + "/" + totalBills : "0";

        // Repopulate Stats Cards
        summaryRow.removeAll();
        summaryRow.add(new StatsCard("💵", "Doanh thu kỳ", String.format("%,.0f đ", monthlyRev), null, true, UIConstants.PRIMARY));
        summaryRow.add(new StatsCard("⚡", "Tổng tiêu thụ", String.format("%,.0f kWh", totalKwh), null, true, UIConstants.SUCCESS));
        summaryRow.add(new StatsCard("✔️", "Tỷ lệ thanh toán", paidDisplay, paidPct, true, UIConstants.AI_PURPLE));
        summaryRow.add(new StatsCard("⚠️", "Nợ phát sinh", String.format("%,.0f đ", unpaidSum), null, false, UIConstants.WARNING));
        summaryRow.revalidate();
        summaryRow.repaint();

        // 2. Trailing 6 months dynamic charts calculation
        double[] trailingRevenue = new double[6];
        double[] trailingKwh = new double[6];
        String[] trailingMonths = new String[6];

        LocalDate targetDate = LocalDate.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            int m = targetDate.getMonthValue();
            int y = targetDate.getYear();
            trailingMonths[i] = "T" + m;

            trailingRevenue[i] = paymentService.getMonthlyRevenue(m, y);

            List<MeterReading> mReadings = readingService.search(null, m, y);
            double sumKwh = 0;
            for (MeterReading r : mReadings) {
                sumKwh += r.getConsumption();
            }
            trailingKwh[i] = sumKwh;

            targetDate = targetDate.plusMonths(1);
        }

        revChartPanel.setData(trailingRevenue, trailingMonths);
        kwhChartPanel.setData(trailingKwh, trailingMonths);

        // 3. Overdue debt table loading
        List<Bill> overdueBills = billService.search(null, "UNPAID", null, null);
        overdueTable.clearRows();
        for (Bill b : overdueBills) {
            overdueTable.addRow(new Object[]{
                b.getBillCode(),
                b.getHouseholdCode() + " - " + b.getOwnerName(),
                String.format("%02d/%04d", b.getMonth(), b.getYear()),
                String.format("%,.0f đ", b.getTotalAmount()),
                b.getCreatedAt() != null ? b.getCreatedAt().toString().substring(0, 10) : ""
            });
        }
    }

    // ====================================================================
    // INTERACTIVE REPORTS DYNAMICS
    // ====================================================================

    private void handleReportTypeChange() {
        int idx = reportBox.getSelectedIndex();

        // Defaults
        areaLbl.setVisible(true); areaFilterBox.setVisible(true);
        statusLbl.setVisible(true); statusFilterBox.setVisible(true);
        periodLbl.setVisible(true); rMonthSpinner.setVisible(true); rYearSpinner.setVisible(true);
        dateRangeLbl.setVisible(true); fromDateTxt.setVisible(true); toDateTxt.setVisible(true);

        if (idx == 0) { // Household
            statusFilterBox.removeAllItems();
            statusFilterBox.addItem("Tất cả");
            statusFilterBox.addItem("Hoạt động");
            statusFilterBox.addItem("Tạm khóa");

            periodLbl.setVisible(false); rMonthSpinner.setVisible(false); rYearSpinner.setVisible(false);
            dateRangeLbl.setVisible(false); fromDateTxt.setVisible(false); toDateTxt.setVisible(false);
        }
        else if (idx == 1) { // Billing
            statusFilterBox.removeAllItems();
            statusFilterBox.addItem("Tất cả");
            statusFilterBox.addItem("Đã thanh toán");
            statusFilterBox.addItem("Chưa thanh toán");

            dateRangeLbl.setVisible(false); fromDateTxt.setVisible(false); toDateTxt.setVisible(false);
        }
        else if (idx == 2) { // Revenue
            areaLbl.setVisible(false); areaFilterBox.setVisible(false);
            statusLbl.setVisible(false); statusFilterBox.setVisible(false);
            rMonthSpinner.setVisible(false); // only Year
            dateRangeLbl.setVisible(false); fromDateTxt.setVisible(false); toDateTxt.setVisible(false);
        }
        else if (idx == 3) { // Payment
            statusLbl.setVisible(false); statusFilterBox.setVisible(false);
            periodLbl.setVisible(false); rMonthSpinner.setVisible(false); rYearSpinner.setVisible(false);
        }
        else if (idx == 4) { // Area
            statusLbl.setVisible(false); statusFilterBox.setVisible(false);
            areaLbl.setVisible(false); areaFilterBox.setVisible(false);
            dateRangeLbl.setVisible(false); fromDateTxt.setVisible(false); toDateTxt.setVisible(false);
        }

        // Re-layout container
        this.revalidate();
        this.repaint();
    }

    // ====================================================================
    // REPORTS EXPORT OPERATIONS
    // ====================================================================

    private void executeExcelExport() {
        int idx = reportBox.getSelectedIndex();
        String reportName = "";
        String[] headers = null;
        List<Object[]> data = null;

        Integer areaId = getSelectedAreaId();
        String status = statusFilterBox.getSelectedItem() != null ? statusFilterBox.getSelectedItem().toString() : null;
        Integer m = (Integer) rMonthSpinner.getValue();
        Integer y = (Integer) rYearSpinner.getValue();
        String from = fromDateTxt.getText().trim();
        String to = toDateTxt.getText().trim();

        switch (idx) {
            case 0:
                reportName = "Danh_Sach_Ho_Gia_Dinh";
                headers = new String[]{"Mã Hộ", "Tên chủ hộ", "Địa chỉ", "Số điện thoại", "Khu vực", "Trạng thái", "Ngày tạo"};
                data = ReportQueryHelper.getHouseholdReport(areaId, status);
                break;
            case 1:
                reportName = "Bang_Ke_Hoa_Don_Dien";
                headers = new String[]{"Mã hóa đơn", "Mã Hộ", "Chủ hộ", "Kỳ", "Tiêu thụ (kWh)", "Tổng tiền (đ)", "Trạng thái", "Ngày lập"};
                data = ReportQueryHelper.getBillingReport(areaId, status, m, y);
                break;
            case 2:
                reportName = "Bao_Cao_Tong_Hop_Doanh_Thu";
                headers = new String[]{"Kỳ", "Tổng tiêu thụ (kWh)", "Doanh thu (đ)", "Nợ tồn (đ)", "Tổng hóa đơn", "Tỷ lệ thanh toán"};
                data = ReportQueryHelper.getRevenueReport(y);
                break;
            case 3:
                reportName = "Bao_Cao_Lich_Su_Thu_Tien";
                headers = new String[]{"Mã GD", "Mã hóa đơn", "Chủ hộ", "Khu vực", "Số tiền (đ)", "Phương thức", "Ngày thanh toán", "Ghi chú"};
                data = ReportQueryHelper.getPaymentReport(areaId, from, to);
                break;
            case 4:
                reportName = "Bao_Cao_San_Luong_Khu_Vuc";
                headers = new String[]{"Mã Khu Vực", "Tên Khu Vực", "Số hộ dân", "Tổng tiêu thụ (kWh)", "Doanh thu (đ)", "Trạng thái"};
                data = ReportQueryHelper.getAreaReport(m, y);
                break;
        }

        if (data == null || data.isEmpty()) {
            ThemeManager.showInfoDialog(this, "Không tìm thấy dữ liệu nào phù hợp với bộ lọc đã chọn!", "Xuất Excel");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(reportName + "_" + LocalDate.now() + ".xlsx"));
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try {
                XlsxExporter.export(dest, "Sheet1", headers, data);
                ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                    "Xuất tệp Excel thành công: " + dest.getName(),
                    ToastNotification.Type.SUCCESS);
            } catch (IOException ex) {
                ThemeManager.showInfoDialog(this, "Lỗi khi ghi tệp Excel: " + ex.getMessage(), "Lỗi xuất file");
                ex.printStackTrace();
            }
        }
    }

    private void executeHtmlPrint() {
        int idx = reportBox.getSelectedIndex();
        String reportTitle = reportBox.getSelectedItem().toString().split(" \\(")[0];
        String[] headers = null;
        List<Object[]> data = null;

        Integer areaId = getSelectedAreaId();
        String status = statusFilterBox.getSelectedItem() != null ? statusFilterBox.getSelectedItem().toString() : null;
        Integer m = (Integer) rMonthSpinner.getValue();
        Integer y = (Integer) rYearSpinner.getValue();
        String from = fromDateTxt.getText().trim();
        String to = toDateTxt.getText().trim();

        switch (idx) {
            case 0:
                headers = new String[]{"Mã Hộ", "Tên chủ hộ", "Địa chỉ", "Số điện thoại", "Khu vực", "Trạng thái", "Ngày tạo"};
                data = ReportQueryHelper.getHouseholdReport(areaId, status);
                break;
            case 1:
                headers = new String[]{"Mã hóa đơn", "Mã Hộ", "Chủ hộ", "Kỳ", "Tiêu thụ (kWh)", "Tổng tiền (đ)", "Trạng thái", "Ngày lập"};
                data = ReportQueryHelper.getBillingReport(areaId, status, m, y);
                break;
            case 2:
                headers = new String[]{"Kỳ", "Tổng tiêu thụ (kWh)", "Doanh thu (đ)", "Nợ tồn (đ)", "Tổng hóa đơn", "Tỷ lệ thanh toán"};
                data = ReportQueryHelper.getRevenueReport(y);
                break;
            case 3:
                headers = new String[]{"Mã GD", "Mã hóa đơn", "Chủ hộ", "Khu vực", "Số tiền (đ)", "Phương thức", "Ngày thanh toán", "Ghi chú"};
                data = ReportQueryHelper.getPaymentReport(areaId, from, to);
                break;
            case 4:
                headers = new String[]{"Mã Khu Vực", "Tên Khu Vực", "Số hộ dân", "Tổng tiêu thụ (kWh)", "Doanh thu (đ)", "Trạng thái"};
                data = ReportQueryHelper.getAreaReport(m, y);
                break;
        }

        if (data == null || data.isEmpty()) {
            ThemeManager.showInfoDialog(this, "Không có dữ liệu báo cáo để hiển thị!", "Xem báo cáo");
            return;
        }

        // Generate dynamic HTML page
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<title>").append(reportTitle).append("</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; color: #1E2D2D; margin: 40px; }");
        html.append(".header { border-bottom: 2px solid #5D6B6B; padding-bottom: 15px; margin-bottom: 30px; }");
        html.append("h1 { color: #5D6B6B; margin: 0; font-size: 24px; }");
        html.append(".meta { color: #7A8F8F; font-size: 13px; margin-top: 5px; }");
        html.append("table { width: 100%; border-collapse: collapse; border-color: #DDE4E4; margin-top: 20px; }");
        html.append("th { background-color: #5D6B6B; color: white; padding: 10px; font-weight: bold; text-align: left; font-size: 13px; }");
        html.append("td { padding: 10px; border-bottom: 1px solid #ECF0F0; font-size: 13px; }");
        html.append("tr:nth-child(even) { background-color: #F4F9FB; }");
        html.append(".summary { margin-top: 30px; font-size: 13px; text-align: right; color: #5D6B6B; }");
        html.append(".print-btn { background-color: #5D6B6B; color: white; border: none; padding: 10px 20px; font-size: 13px; border-radius: 4px; cursor: pointer; float: right; }");
        html.append("@media print { .print-btn { display: none; } }");
        html.append("</style></head><body>");

        html.append("<button class='print-btn' onclick='window.print()'>🖨️ In Báo Cáo / Lưu PDF</button>");
        html.append("<div class='header'>");
        html.append("<h1>").append(reportTitle.toUpperCase()).append("</h1>");
        html.append("<div class='meta'>Ngày lập báo cáo: ").append(LocalDate.now()).append(" | Hệ thống quản lý Electra Manager AI</div>");
        html.append("</div>");

        html.append("<table border='1'>");
        html.append("<thead><tr>");
        for (String h : headers) {
            html.append("<th>").append(h).append("</th>");
        }
        html.append("</tr></thead><tbody>");

        double totalKwhSum = 0;
        double totalMoneySum = 0;

        for (Object[] row : data) {
            html.append("<tr>");
            for (int i = 0; i < headers.length; i++) {
                Object cell = (i < row.length) ? row[i] : "";
                
                // Format check
                if (cell instanceof Number) {
                    double doubleVal = ((Number) cell).doubleValue();
                    // Identify if it's Consumption (kWh) or Amount (VND)
                    String headerName = headers[i].toLowerCase();
                    if (headerName.contains("tiêu thụ") || headerName.contains("sản lượng")) {
                        totalKwhSum += doubleVal;
                        html.append("<td align='right'>").append(String.format("%,.0f kWh", doubleVal)).append("</td>");
                    } else if (headerName.contains("tiền") || headerName.contains("thu") || headerName.contains("doanh thu")) {
                        totalMoneySum += doubleVal;
                        html.append("<td align='right'>").append(String.format("%,.0f đ", doubleVal)).append("</td>");
                    } else {
                        html.append("<td align='right'>").append(String.format("%.0f", doubleVal)).append("</td>");
                    }
                } else {
                    html.append("<td>").append(cell.toString()).append("</td>");
                }
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");

        // Aggregation summaries
        if (totalKwhSum > 0 || totalMoneySum > 0) {
            html.append("<div class='summary'>");
            if (totalKwhSum > 0) {
                html.append("<b>Tổng điện năng tiêu thụ:</b> ").append(String.format("%,.0f kWh", totalKwhSum)).append("<br/>");
            }
            if (totalMoneySum > 0) {
                html.append("<b>Tổng doanh thu ghi nhận:</b> <span style='color:#C0392B; font-size:15px;'><b>").append(String.format("%,.0f đ", totalMoneySum)).append("</b></span><br/>");
            }
            html.append("</div>");
        }

        html.append("</body></html>");

        // Write HTML to temporary file and open in default web browser
        try {
            File tempReport = new File("C:/Users/ASUS/.gemini/antigravity-ide/brain/5f735e7d-7f61-4c85-9797-0f3413203d8f/BaoCao.html");
            tempReport.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(tempReport)) {
                fw.write(html.toString());
            }
            
            // Open in browser
            Desktop.getDesktop().browse(tempReport.toURI());
            ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                "Đã hiển thị báo cáo in ấn trên trình duyệt!",
                ToastNotification.Type.SUCCESS);
        } catch (IOException ex) {
            ThemeManager.showInfoDialog(this, "Không thể tạo báo cáo in ấn: " + ex.getMessage(), "Lỗi");
            ex.printStackTrace();
        }
    }

    private Integer getSelectedAreaId() {
        Object item = areaFilterBox.getSelectedItem();
        if (item instanceof Area) {
            return ((Area) item).getAreaId();
        }
        return 0; // represent All
    }
}
