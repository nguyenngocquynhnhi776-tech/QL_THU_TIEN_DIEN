package UI.panels;

import UI.components.*;
import UI.theme.ThemeManager;
import model.Household;
import model.MeterReading;
import service.HouseholdService;
import service.MeterReadingService;
import service.impl.HouseholdServiceImpl;
import service.impl.MeterReadingServiceImpl;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Nhập & quản lý chỉ số điện hàng tháng — kết nối SQL Server.
 */
public class ElectricityIndexPanel extends BasePanel {

    private final MeterReadingService readingService = new MeterReadingServiceImpl();
    private final HouseholdService    householdService = new HouseholdServiceImpl();

    // Form fields
    private JComboBox<Household> householdBox;
    private JSpinner             monthSpinner;
    private JSpinner             yearSpinner;
    private RoundedTextField     oldIndexField;
    private RoundedTextField     newIndexField;

    // Result displays
    private final JLabel consumeValueLabel;
    private final JLabel aiStatusLabel;

    // Table
    private final ModernTable table;

    public ElectricityIndexPanel() {
        super("Chỉ số điện", "Nhập và theo dõi chỉ số điện hàng tháng");

        // ====================================================================
        // FORM CARD
        // ====================================================================
        GlassCard formCard = new GlassCard(UIConstants.CARD_RADIUS);
        formCard.setLayout(new BorderLayout(0, 0));

        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardHeader.setOpaque(false);
        cardHeader.setBorder(BorderFactory.createEmptyBorder(12, 18, 6, 18));
        JLabel formTitle = new JLabel("📋  Nhập chỉ số điện mới");
        formTitle.setFont(UIConstants.FONT_SUBHEADER);
        formTitle.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        cardHeader.add(formTitle);

        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setOpaque(false);
        formBody.setBorder(BorderFactory.createEmptyBorder(4, 18, 14, 18));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 8, 5, 8);
        gc.fill   = GridBagConstraints.HORIZONTAL;

        // Household combo
        householdBox = new JComboBox<>();
        householdBox.setFont(UIConstants.FONT_NORMAL);
        householdBox.setPreferredSize(new Dimension(200, 34));
        loadHouseholdCombo();

        // Auto-fill OldIndex when household changes
        householdBox.addActionListener(e -> autoFillOldIndex());

        // Month/Year spinners
        LocalDate now = LocalDate.now();
        monthSpinner = new JSpinner(new SpinnerNumberModel(now.getMonthValue(), 1, 12, 1));
        monthSpinner.setFont(UIConstants.FONT_NORMAL);
        monthSpinner.setPreferredSize(new Dimension(65, 34));

        yearSpinner = new JSpinner(new SpinnerNumberModel(now.getYear(), 2000, 2100, 1));
        yearSpinner.setFont(UIConstants.FONT_NORMAL);
        yearSpinner.setPreferredSize(new Dimension(85, 34));
        // Remove thousands-separator from year
        ((JSpinner.NumberEditor) yearSpinner.getEditor()).getFormat().setGroupingUsed(false);

        oldIndexField = new RoundedTextField();
        newIndexField = new RoundedTextField();

        consumeValueLabel = new JLabel("— kWh");
        consumeValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        consumeValueLabel.setForeground(UIConstants.PRIMARY);

        aiStatusLabel = new JLabel("  Nhập chỉ số để tính tiêu thụ...");
        aiStatusLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        aiStatusLabel.setForeground(UIConstants.COLOR_TEXT_MUTED);
        aiStatusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.COLOR_DIVIDER, 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        // === ROW 0: Hộ gia đình | Tháng | Năm ===
        gc.gridy = 0; gc.weightx = 0; gc.gridwidth = 1;
        gc.gridx = 0; formBody.add(makeLabel("Hộ gia đình: *"), gc);
        gc.gridx = 1; gc.weightx = 0.5; formBody.add(householdBox, gc);

        gc.gridx = 2; gc.weightx = 0; formBody.add(makeLabel("Tháng:"), gc);
        gc.gridx = 3; gc.weightx = 0.2; formBody.add(monthSpinner, gc);

        gc.gridx = 4; gc.weightx = 0; formBody.add(makeLabel("Năm:"), gc);
        gc.gridx = 5; gc.weightx = 0.2; formBody.add(yearSpinner, gc);

        // === ROW 1: Chỉ số cũ | Chỉ số mới ===
        gc.gridy = 1; gc.weightx = 0; gc.gridwidth = 1;
        gc.gridx = 0; formBody.add(makeLabel("Chỉ số cũ (kWh):"), gc);
        gc.gridx = 1; gc.weightx = 0.5; formBody.add(oldIndexField, gc);

        gc.gridx = 2; gc.weightx = 0; formBody.add(makeLabel("Chỉ số mới (kWh): *"), gc);
        gc.gridx = 3; gc.weightx = 0.5; gc.gridwidth = 3; formBody.add(newIndexField, gc);
        gc.gridwidth = 1;

        // === ROW 2: Kết quả + nút ===
        JPanel consumePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        consumePanel.setOpaque(false);
        JLabel consumeLbl = makeLabel("Tiêu thụ: ");
        consumeLbl.setForeground(UIConstants.COLOR_TEXT_SECONDARY);
        consumePanel.add(consumeLbl);
        consumePanel.add(consumeValueLabel);

        RoundedButton saveBtn  = new RoundedButton("💾  Nhập & Lưu",   UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        RoundedButton clearBtn = new RoundedButton("↺  Xóa trắng",    UIConstants.BUTTON_RADIUS, new Color(150, 150, 170));
        saveBtn.setPreferredSize(new Dimension(140, 36));
        clearBtn.setPreferredSize(new Dimension(120, 36));
        saveBtn.addActionListener(e -> saveReading());
        clearBtn.addActionListener(e -> clearForm());

        gc.gridy = 2; gc.weightx = 0; gc.gridwidth = 1;
        gc.gridx = 0; formBody.add(consumePanel, gc);
        gc.gridx = 1; gc.weightx = 0.5; gc.gridwidth = 2; formBody.add(aiStatusLabel, gc);
        gc.gridwidth = 1;

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);
        btnGroup.add(clearBtn);
        btnGroup.add(saveBtn);
        gc.gridx = 3; gc.gridwidth = 3; gc.weightx = 0.5;
        formBody.add(btnGroup, gc);

        formCard.add(cardHeader, BorderLayout.NORTH);
        formCard.add(formBody,   BorderLayout.CENTER);
        formCard.setMinimumSize(new Dimension(0, 190));
        formCard.setPreferredSize(new Dimension(0, 210));

        // ====================================================================
        // TOOLBAR + TABLE
        // ====================================================================
        JPanel toolbar = new JPanel(new BorderLayout(UIConstants.SP_MD, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(UIConstants.SP_MD, 0, UIConstants.SP_SM, 0));

        SearchField search = new SearchField("Tìm mã hộ, tên chủ hộ...");
        search.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { loadTable(search.getText()); }
            public void removeUpdate(DocumentEvent e)  { loadTable(search.getText()); }
            public void changedUpdate(DocumentEvent e) { loadTable(search.getText()); }
        });

        RoundedButton refreshBtn = new RoundedButton("⟳  Làm mới", UIConstants.BUTTON_RADIUS,
                new Color(100, 120, 180));
        refreshBtn.setPreferredSize(new Dimension(110, 36));
        refreshBtn.addActionListener(e -> { search.getField().setText(""); loadTable(null); });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(refreshBtn);
        toolbar.add(search, BorderLayout.WEST);
        toolbar.add(right,  BorderLayout.EAST);

        table = new ModernTable(new String[]{
            "Tháng", "Mã hộ", "Chủ hộ", "Chỉ số cũ", "Chỉ số mới", "Tiêu thụ (kWh)", "Ngày nhập"
        });
        table.setColumnWidths(80, 90, 160, 100, 100, 120, 130);

        // Auto-recalculate on input change
        DocumentListener calcListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { SwingUtilities.invokeLater(() -> updateConsumptionDisplay()); }
            public void removeUpdate(DocumentEvent e)  { SwingUtilities.invokeLater(() -> updateConsumptionDisplay()); }
            public void changedUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> updateConsumptionDisplay()); }
        };
        oldIndexField.getDocument().addDocumentListener(calcListener);
        newIndexField.getDocument().addDocumentListener(calcListener);

        loadTable(null);
        autoFillOldIndex();

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setOpaque(false);
        root.add(formCard, BorderLayout.NORTH);
        root.add(toolbar,  BorderLayout.CENTER);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.add(table, BorderLayout.CENTER);

        contentArea.add(root,      BorderLayout.NORTH);
        contentArea.add(tableWrap, BorderLayout.CENTER);
    }

    // ====================================================================
    // DATA
    // ====================================================================

    private void loadHouseholdCombo() {
        householdBox.removeAllItems();
        List<Household> list = householdService.getAll();
        for (Household hh : list) {
            householdBox.addItem(hh);
        }
    }

    private void autoFillOldIndex() {
        Household selected = (Household) householdBox.getSelectedItem();
        if (selected == null) return;

        MeterReading last = readingService.getLastReading(selected.getHouseholdId());
        if (last != null) {
            oldIndexField.setText(String.format("%.0f", last.getNewIndex()));
        } else {
            oldIndexField.setText("0");
        }
        newIndexField.setText("");
        updateConsumptionDisplay();
    }

    private void loadTable(String keyword) {
        if (table == null) return;
        List<MeterReading> list = readingService.search(
            (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null,
            null, null
        );
        table.clearRows();
        for (MeterReading r : list) {
            table.addRow(new Object[]{
                r.getPeriodDisplay(),
                r.getHouseholdCode(),
                r.getOwnerName(),
                String.format("%.0f", r.getOldIndex()),
                String.format("%.0f", r.getNewIndex()),
                String.format("%.0f", r.getConsumption()),
                r.getCreatedAt() != null ? r.getCreatedAt().toString().substring(0, 10) : ""
            });
        }
    }

    // ====================================================================
    // FORM ACTIONS
    // ====================================================================

    private void updateConsumptionDisplay() {
        try {
            double cu  = Double.parseDouble(oldIndexField.getText().trim());
            double moi = Double.parseDouble(newIndexField.getText().trim());
            double usage = moi - cu;
            consumeValueLabel.setText(String.format("%.0f kWh", usage));

            if (usage < 0) {
                setStatusLabel("  ✖  Chỉ số mới nhỏ hơn chỉ số cũ!", UIConstants.ERROR, UIConstants.ERROR_BG);
            } else if (usage == 0) {
                setStatusLabel("  ⚠  Tiêu thụ 0 kWh — kiểm tra lại.", UIConstants.WARNING, UIConstants.WARNING_BG);
            } else {
                setStatusLabel(String.format("  ✔  Tiêu thụ %.0f kWh — hợp lệ.", usage), UIConstants.SUCCESS, UIConstants.SUCCESS_BG);
            }
        } catch (NumberFormatException ignored) {
            consumeValueLabel.setText("— kWh");
            setStatusLabel("  Nhập chỉ số để tính tiêu thụ...", UIConstants.COLOR_TEXT_MUTED, UIConstants.COLOR_DIVIDER);
        }
    }

    private void setStatusLabel(String text, Color fg, Color border) {
        aiStatusLabel.setText(text);
        aiStatusLabel.setForeground(fg);
        aiStatusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
    }

    private void saveReading() {
        Household hh = (Household) householdBox.getSelectedItem();
        if (hh == null) {
            ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(this),
                "Vui lòng chọn hộ gia đình!", "Thiếu thông tin");
            return;
        }
        int month = (int) monthSpinner.getValue();
        int year  = (int) yearSpinner.getValue();

        String cuStr  = oldIndexField.getText().trim();
        String moiStr = newIndexField.getText().trim();

        if (cuStr.isEmpty() || moiStr.isEmpty()) {
            ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(this),
                "Vui lòng nhập đầy đủ Chỉ số cũ và Chỉ số mới!", "Thiếu thông tin");
            return;
        }

        double cu, moi;
        try {
            cu  = Double.parseDouble(cuStr);
            moi = Double.parseDouble(moiStr);
        } catch (NumberFormatException ex) {
            ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(this),
                "Chỉ số phải là số!", "Dữ liệu không hợp lệ");
            return;
        }

        MeterReading reading = new MeterReading();
        reading.setHouseholdId(hh.getHouseholdId());
        reading.setMonth(month);
        reading.setYear(year);
        reading.setOldIndex(cu);
        reading.setNewIndex(moi);

        String err = readingService.addReading(reading);
        if (err != null) {
            ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(this), err, "Lỗi");
        } else {
            ToastNotification.show(
                SwingUtilities.getWindowAncestor(this),
                "Đã lưu chỉ số điện hộ " + hh.getHouseholdCode() +
                " tháng " + String.format("%02d/%04d", month, year) + "!",
                ToastNotification.Type.SUCCESS
            );
            clearForm();
            loadTable(null);
        }
    }

    private void clearForm() {
        if (householdBox.getItemCount() > 0) householdBox.setSelectedIndex(0);
        newIndexField.setText("");
        consumeValueLabel.setText("— kWh");
        setStatusLabel("  Nhập chỉ số để tính tiêu thụ...", UIConstants.COLOR_TEXT_MUTED, UIConstants.COLOR_DIVIDER);
        autoFillOldIndex();
    }

    // ====================================================================
    // HELPERS
    // ====================================================================

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_NORMAL_BOLD);
        l.setForeground(UIConstants.COLOR_TEXT_SECONDARY);
        return l;
    }
}
