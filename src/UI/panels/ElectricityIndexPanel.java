package UI.panels;

import UI.components.*;
import UI.theme.ThemeManager;
import model.Area;
import model.Household;
import model.MeterReading;
import service.AreaService;
import service.HouseholdService;
import service.MeterReadingService;
import service.MeterReadingService.SaveResult;
import service.impl.AreaServiceImpl;
import service.impl.HouseholdServiceImpl;
import service.impl.MeterReadingServiceImpl;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Nhập & quản lý chỉ số điện hàng tháng.
 *
 * Performance: ALL database calls are moved to SwingWorker background threads
 * so the EDT (and UI) never freezes. The search box uses a debounce timer
 * to avoid hammering the DB on every keystroke.
 */
public class ElectricityIndexPanel extends BasePanel {

    // ── Services ──────────────────────────────────────────────────────────────
    private final MeterReadingService readingService   = new MeterReadingServiceImpl();
    private final HouseholdService    householdService = new HouseholdServiceImpl();
    private final AreaService         areaService      = new AreaServiceImpl();

    // ── Form widgets ──────────────────────────────────────────────────────────
    private JComboBox<Object>    areaBox;
    private JComboBox<Household> householdBox;

    // Read-only info fields
    private RoundedTextField householdCodeField;
    private RoundedTextField ownerNameField;
    private RoundedTextField addressField;
    private RoundedTextField oldIndexField;

    // Inputs
    private JSpinner         monthSpinner;
    private JSpinner         yearSpinner;
    private RoundedTextField newIndexField;

    // Status indicators
    private JLabel consumeValueLabel;
    private JLabel statusLabel;
    private JLabel loadingHhLabel;   // "Đang tải..." shown next to household combo

    // Table
    private ModernTable      table;
    private TableRowSorter<DefaultTableModel> sorter;

    // Search debounce
    private Timer searchDebounce;

    // ── In-memory household list (populated from DB in background) ────────────
    /** Full list for the currently-selected area — used for in-memory autocomplete */
    private List<Household> allHouseholds = new ArrayList<>();

    /** Guard flag: prevents listener recursion while we programmatically modify the combo */
    private boolean updatingCombo = false;

    // Column indices
    private static final int COL_PERIOD  = 0;
    private static final int COL_CODE    = 1;
    private static final int COL_NAME    = 2;
    private static final int COL_OLD     = 3;
    private static final int COL_NEW     = 4;
    private static final int COL_CONSUME = 5;
    private static final int COL_DATE    = 6;

    // =========================================================================
    public ElectricityIndexPanel() {
        super("Chỉ số điện", "Nhập và theo dõi chỉ số điện hàng tháng");
        buildFormCard();
        buildTableSection();
        // Kick off initial data load on background thread
        SwingUtilities.invokeLater(this::asyncLoadAreas);
    }

    // =========================================================================
    // FORM CARD
    // =========================================================================
    private void buildFormCard() {
        GlassCard formCard = new GlassCard(UIConstants.CARD_RADIUS);
        formCard.setLayout(new BorderLayout(0, 0));

        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardHeader.setOpaque(false);
        cardHeader.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 16));
        JLabel formTitle = new JLabel("📋  Nhập chỉ số điện mới");
        formTitle.setFont(UIConstants.FONT_SUBHEADER);
        formTitle.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        cardHeader.add(formTitle);

        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setOpaque(false);
        formBody.setBorder(BorderFactory.createEmptyBorder(2, 14, 8, 14));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 6, 3, 6);
        gc.fill   = GridBagConstraints.HORIZONTAL;

        // ── Widget init ────────────────────────────────────────────────────────
        // Area filter
        areaBox = new JComboBox<>();
        areaBox.setFont(UIConstants.FONT_NORMAL);
        areaBox.setPreferredSize(new Dimension(175, 32));
        areaBox.addItem("Đang tải khu vực...");
        areaBox.setEnabled(false);

        // Household combo (editable = searchable autocomplete)
        householdBox = new JComboBox<>();
        householdBox.setFont(UIConstants.FONT_NORMAL);
        householdBox.setPreferredSize(new Dimension(205, 32));
        householdBox.setEditable(true);
        householdBox.setEnabled(false);

        loadingHhLabel = new JLabel("");
        loadingHhLabel.setFont(UIConstants.FONT_SMALL);
        loadingHhLabel.setForeground(UIConstants.COLOR_TEXT_MUTED);

        // Month / Year
        LocalDate now = LocalDate.now();
        monthSpinner = new JSpinner(new SpinnerNumberModel(now.getMonthValue(), 1, 12, 1));
        monthSpinner.setFont(UIConstants.FONT_NORMAL);
        monthSpinner.setPreferredSize(new Dimension(58, 32));

        yearSpinner = new JSpinner(new SpinnerNumberModel(now.getYear(), 2000, 2100, 1));
        yearSpinner.setFont(UIConstants.FONT_NORMAL);
        yearSpinner.setPreferredSize(new Dimension(78, 32));
        ((JSpinner.NumberEditor) yearSpinner.getEditor()).getFormat().setGroupingUsed(false);

        // Read-only display fields
        householdCodeField = readOnly();
        ownerNameField     = readOnly();
        addressField       = readOnly();
        oldIndexField      = readOnly();

        // New index input
        newIndexField = new RoundedTextField();
        newIndexField.setPreferredSize(new Dimension(120, 32));

        // Consumption display
        consumeValueLabel = new JLabel("— kWh");
        consumeValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        consumeValueLabel.setForeground(UIConstants.PRIMARY);

        statusLabel = new JLabel("  Nhập chỉ số để tính tiêu thụ...");
        statusLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        statusLabel.setForeground(UIConstants.COLOR_TEXT_MUTED);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.COLOR_DIVIDER, 1, true),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)));

        // Buttons
        RoundedButton saveBtn  = new RoundedButton("💾  Nhập & Lưu",
                UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        RoundedButton clearBtn = new RoundedButton("↺  Xóa trắng",
                UIConstants.BUTTON_RADIUS, new Color(150, 150, 170));
        saveBtn.setPreferredSize(new Dimension(130, 32));
        clearBtn.setPreferredSize(new Dimension(110, 32));
        saveBtn.setVisible(util.PermissionManager.getInstance()
                .hasPermission(model.Permission.INDEX_CREATE));
        saveBtn.addActionListener(e -> saveReading());
        clearBtn.addActionListener(e -> clearForm());

        // ── Layout ─────────────────────────────────────────────────────────────
        // ROW 0: Khu vực | Hộ gia đình | loading label | Tháng | Năm
        gc.gridy = 0;
        gc.gridx = 0; gc.weightx = 0;   formBody.add(lbl("Khu vực:"), gc);
        gc.gridx = 1; gc.weightx = 0.35; formBody.add(areaBox, gc);
        gc.gridx = 2; gc.weightx = 0;   formBody.add(lbl("Hộ gia đình: *"), gc);
        gc.gridx = 3; gc.weightx = 0.4; gc.gridwidth = 2; formBody.add(householdBox, gc); gc.gridwidth = 1;
        gc.gridx = 5; gc.weightx = 0.15; formBody.add(loadingHhLabel, gc);
        gc.gridx = 6; gc.weightx = 0;   formBody.add(lbl("Tháng:"), gc);
        gc.gridx = 7; gc.weightx = 0.05; formBody.add(monthSpinner, gc);
        gc.gridx = 8; gc.weightx = 0;   formBody.add(lbl("Năm:"), gc);
        gc.gridx = 9; gc.weightx = 0.05; formBody.add(yearSpinner, gc);

        // ROW 1: Mã hộ | Tên chủ hộ | Địa chỉ
        gc.gridy = 1;
        gc.gridx = 0; gc.weightx = 0;   formBody.add(lbl("Mã hộ:"), gc);
        gc.gridx = 1; gc.weightx = 0.35; formBody.add(householdCodeField, gc);
        gc.gridx = 2; gc.weightx = 0;   formBody.add(lbl("Chủ hộ:"), gc);
        gc.gridx = 3; gc.weightx = 0.35; formBody.add(ownerNameField, gc);
        gc.gridx = 4; gc.weightx = 0;   formBody.add(lbl("Địa chỉ:"), gc);
        gc.gridx = 5; gc.weightx = 0.6; gc.gridwidth = 5; formBody.add(addressField, gc); gc.gridwidth = 1;

        // ROW 2: Chỉ số cũ | Chỉ số mới | Tiêu thụ | status
        gc.gridy = 2;
        gc.gridx = 0; gc.weightx = 0;   formBody.add(lbl("Chỉ số cũ (kWh):"), gc);
        gc.gridx = 1; gc.weightx = 0.3; formBody.add(oldIndexField, gc);
        gc.gridx = 2; gc.weightx = 0;   formBody.add(lbl("Chỉ số mới: *"), gc);
        gc.gridx = 3; gc.weightx = 0.3; formBody.add(newIndexField, gc);
        JPanel cPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        cPanel.setOpaque(false);
        JLabel cLbl = lbl("Tiêu thụ:"); cLbl.setForeground(UIConstants.COLOR_TEXT_SECONDARY);
        cPanel.add(cLbl); cPanel.add(consumeValueLabel);
        gc.gridx = 4; gc.weightx = 0.25; gc.gridwidth = 2; formBody.add(cPanel, gc); gc.gridwidth = 1;
        gc.gridx = 6; gc.weightx = 0.5; gc.gridwidth = 4; formBody.add(statusLabel, gc); gc.gridwidth = 1;

        // ROW 3: buttons
        gc.gridy = 3; gc.gridx = 0; gc.gridwidth = 10; gc.weightx = 1;
        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);
        btnGroup.add(clearBtn);
        btnGroup.add(saveBtn);
        formBody.add(btnGroup, gc);
        gc.gridwidth = 1;

        formCard.add(cardHeader, BorderLayout.NORTH);
        formCard.add(formBody,   BorderLayout.CENTER);
        formCard.setPreferredSize(new Dimension(0, 220));
        formCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        contentArea.add(formCard, BorderLayout.NORTH);

        // ── Wire up listeners ─────────────────────────────────────────────────
        wireListeners();
    }

    // =========================================================================
    // TABLE SECTION
    // =========================================================================
    private void buildTableSection() {
        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(UIConstants.SP_SM, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(UIConstants.SP_SM, 0, UIConstants.SP_SM, 0));

        SearchField search = new SearchField("Tìm mã hộ, tên chủ hộ, tháng (05/2026)...");
        // Debounce: filter fires 200ms after user stops typing
        search.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { scheduleFilter(search.getText()); }
            public void removeUpdate(DocumentEvent e)  { scheduleFilter(search.getText()); }
            public void changedUpdate(DocumentEvent e) { scheduleFilter(search.getText()); }
        });

        RoundedButton refreshBtn = new RoundedButton("⟳  Làm mới",
                UIConstants.BUTTON_RADIUS, new Color(100, 120, 180));
        refreshBtn.setPreferredSize(new Dimension(110, 34));
        refreshBtn.addActionListener(e -> {
            search.getField().setText("");
            asyncLoadTable();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(refreshBtn);
        toolbar.add(search, BorderLayout.CENTER);
        toolbar.add(right,  BorderLayout.EAST);

        // Table
        table = new ModernTable(new String[]{
            "Tháng", "Mã hộ", "Chủ hộ", "Chỉ số cũ", "Chỉ số mới", "Tiêu thụ (kWh)", "Ngày nhập"
        });
        table.setColumnWidths(80, 100, 175, 100, 100, 120, 130);

        // Sorter
        sorter = new TableRowSorter<>(table.getModel());
        table.getTable().setRowSorter(sorter);

        // Period comparator: "MM/YYYY" → sort chronologically
        sorter.setComparator(COL_PERIOD, Comparator.comparingInt((String s) -> {
            try { String[] p = s.split("/"); return Integer.parseInt(p[1]) * 100 + Integer.parseInt(p[0]); }
            catch (Exception e) { return 0; }
        }));
        // Numeric comparators
        Comparator<String> numComp = Comparator.comparingDouble(s -> {
            try { return Double.parseDouble(s.trim().replace(",", "")); }
            catch (Exception e) { return 0.0; }
        });
        sorter.setComparator(COL_OLD,     numComp);
        sorter.setComparator(COL_NEW,     numComp);
        sorter.setComparator(COL_CONSUME, numComp);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(COL_PERIOD, SortOrder.DESCENDING)));

        JPanel tableWrap = new JPanel(new BorderLayout(0, 0));
        tableWrap.setOpaque(false);
        tableWrap.add(toolbar, BorderLayout.NORTH);
        tableWrap.add(table,   BorderLayout.CENTER);

        contentArea.add(tableWrap, BorderLayout.CENTER);
    }

    // =========================================================================
    // LISTENERS (EDT-safe — no DB calls here)
    // =========================================================================
    private void wireListeners() {
        // Ward change → async load households
        areaBox.addActionListener(e -> {
            if (!updatingCombo) asyncLoadHouseholdsForArea();
        });

        // Household selection confirmed (Enter / click) → fill info async
        householdBox.addActionListener(e -> {
            if (!updatingCombo) asyncFillHouseholdInfo();
        });

        // Autocomplete: in-memory filter on every keystroke (no DB = instant)
        JTextField editor = getEditor();
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  {
                if (!updatingCombo) {
                    SwingUtilities.invokeLater(() -> filterHouseholdCombo(editor.getText()));
                }
            }
            public void removeUpdate(DocumentEvent e)  {
                if (!updatingCombo) {
                    SwingUtilities.invokeLater(() -> filterHouseholdCombo(editor.getText()));
                }
            }
            public void changedUpdate(DocumentEvent e) {
                if (!updatingCombo) {
                    SwingUtilities.invokeLater(() -> filterHouseholdCombo(editor.getText()));
                }
            }
        });

        // Consumption auto-calc (pure arithmetic — always instant)
        DocumentListener calcListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  {
                if (!updatingCombo) {
                    SwingUtilities.invokeLater(ElectricityIndexPanel.this::recalcConsumption);
                }
            }
            public void removeUpdate(DocumentEvent e)  {
                if (!updatingCombo) {
                    SwingUtilities.invokeLater(ElectricityIndexPanel.this::recalcConsumption);
                }
            }
            public void changedUpdate(DocumentEvent e) {
                if (!updatingCombo) {
                    SwingUtilities.invokeLater(ElectricityIndexPanel.this::recalcConsumption);
                }
            }
        };
        oldIndexField.getDocument().addDocumentListener(calcListener);
        newIndexField.getDocument().addDocumentListener(calcListener);
    }

    // =========================================================================
    // BACKGROUND LOADERS  (SwingWorker — DB on worker thread, UI on EDT)
    // =========================================================================

    /** Load active areas — runs once on startup */
    private void asyncLoadAreas() {
        new SwingWorker<List<Area>, Void>() {
            @Override protected List<Area> doInBackground() {
                return areaService.getAllActive();   // DB call on worker thread
            }
            @Override protected void done() {
                try {
                    List<Area> areas = get();
                    updatingCombo = true;
                    areaBox.removeAllItems();
                    areaBox.addItem("— Tất cả khu vực —");
                    for (Area a : areas) areaBox.addItem(a);
                    areaBox.setSelectedIndex(0);
                    areaBox.setEnabled(true);
                    updatingCombo = false;
                    // Trigger household load for "all areas"
                    asyncLoadHouseholdsForArea();
                } catch (Exception ex) {
                    System.err.println("[WARN] asyncLoadAreas: " + ex.getMessage());
                    areaBox.setEnabled(true);
                }
            }
        }.execute();
    }

    /** Load households for the currently-selected area */
    private void asyncLoadHouseholdsForArea() {
        // Disable combo and show loading indicator while fetching
        householdBox.setEnabled(false);
        loadingHhLabel.setText("Đang tải...");
        clearInfoFields();

        Object sel = areaBox.getSelectedItem();
        final Integer areaId = (sel instanceof Area) ? ((Area) sel).getAreaId() : null;

        new SwingWorker<List<Household>, Void>() {
            @Override protected List<Household> doInBackground() {
                return householdService.search(null, areaId);  // DB on worker thread
            }
            @Override protected void done() {
                try {
                    allHouseholds = get();           // store in-memory list
                    // Rebuild combo model in background/one-shot to avoid event storms
                    DefaultComboBoxModel<Household> comboModel = new DefaultComboBoxModel<>();
                    for (Household hh : allHouseholds) {
                        comboModel.addElement(hh);
                    }
                    updatingCombo = true;
                    householdBox.setModel(comboModel);
                    getEditor().setText("");
                    householdBox.setSelectedIndex(-1);
                    updatingCombo = false;
                    householdBox.setEnabled(true);
                    loadingHhLabel.setText("");
                } catch (Exception ex) {
                    System.err.println("[WARN] asyncLoadHouseholdsForArea: " + ex.getMessage());
                    householdBox.setEnabled(true);
                    loadingHhLabel.setText("Lỗi tải dữ liệu");
                }
            }
        }.execute();
    }

    /** Fetch previous reading for the selected household */
    private void asyncFillHouseholdInfo() {
        Object sel = householdBox.getSelectedItem();
        if (!(sel instanceof Household)) {
            clearInfoFields();
            return;
        }
        Household hh = (Household) sel;

        // Update static text fields immediately (no DB needed)
        updatingCombo = true;
        getEditor().setText(hh.toString());
        updatingCombo = false;
        householdCodeField.setText(hh.getHouseholdCode());
        ownerNameField.setText(hh.getOwnerName());
        addressField.setText(hh.getAddress() != null ? hh.getAddress() : "");
        oldIndexField.setText("...");  // placeholder while loading

        final int hhId = hh.getHouseholdId();
        new SwingWorker<MeterReading, Void>() {
            @Override protected MeterReading doInBackground() {
                return readingService.getLastReading(hhId);  // DB on worker thread
            }
            @Override protected void done() {
                try {
                    MeterReading last = get();
                    oldIndexField.setText(last != null ? String.format("%.0f", last.getNewIndex()) : "0");
                    newIndexField.setText("");
                    newIndexField.requestFocusInWindow();
                    recalcConsumption();
                } catch (Exception ex) {
                    oldIndexField.setText("0");
                }
            }
        }.execute();
    }

    /** Load table data from DB in background */
    void asyncLoadTable() {
        new SwingWorker<List<MeterReading>, Void>() {
            @Override protected List<MeterReading> doInBackground() {
                return readingService.search(null, null, null);
            }
            @Override protected void done() {
                try {
                    populateTable(get());
                } catch (Exception ex) {
                    System.err.println("[WARN] asyncLoadTable: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void populateTable(List<MeterReading> list) {
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

    // =========================================================================
    // IN-MEMORY AUTOCOMPLETE  (No DB — instant)
    // =========================================================================
    private void filterHouseholdCombo(String query) {
        if (updatingCombo) return;
        String q = (query == null) ? "" : query.trim().toLowerCase();

        List<Household> matched = new ArrayList<>();
        for (Household hh : allHouseholds) {
            if (q.isEmpty()
                    || hh.getHouseholdCode().toLowerCase().contains(q)
                    || hh.getOwnerName().toLowerCase().contains(q)) {
                matched.add(hh);
            }
        }

        updatingCombo = true;
        DefaultComboBoxModel<Household> comboModel = new DefaultComboBoxModel<>();
        for (Household hh : matched) {
            comboModel.addElement(hh);
        }
        householdBox.setModel(comboModel);
        householdBox.setSelectedIndex(-1);
        
        // Restore editor cursor position
        JTextField ed = getEditor();
        ed.setText(query);
        if (query != null) ed.setCaretPosition(query.length());
        updatingCombo = false;

        if (!matched.isEmpty() && !q.isEmpty()) {
            householdBox.setPopupVisible(true);
        }
    }

    // =========================================================================
    // TABLE SEARCH  (Debounced 200ms — RowFilter is pure in-memory)
    // =========================================================================
    private void scheduleFilter(String text) {
        if (searchDebounce != null) searchDebounce.stop();
        searchDebounce = new Timer(200, e -> applyTableFilter(text));
        searchDebounce.setRepeats(false);
        searchDebounce.start();
    }

    private void applyTableFilter(String raw) {
        if (sorter == null) return;
        String text = (raw == null) ? "" : raw.trim();
        if (text.isEmpty()) { sorter.setRowFilter(null); return; }

        String[] tokens = text.split("\\s+");
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        for (String token : tokens) {
            String t = token.toLowerCase();
            if (t.matches("\\d{1,2}/\\d{4}")) {
                filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(t.toUpperCase()), COL_PERIOD));
            } else if (t.matches("\\d{4}")) {
                filters.add(RowFilter.regexFilter("(?i)/" + t, COL_PERIOD));
            } else {
                @SuppressWarnings("unchecked")
                RowFilter<Object, Object> rf = RowFilter.orFilter(List.of(
                    RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(t), COL_CODE),
                    RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(t), COL_NAME)
                ));
                filters.add(rf);
            }
        }
        sorter.setRowFilter(RowFilter.andFilter(filters));
    }

    // =========================================================================
    // CONSUMPTION CALCULATION  (Pure arithmetic — EDT, instant)
    // =========================================================================
    private void recalcConsumption() {
        try {
            double cu  = Double.parseDouble(oldIndexField.getText().trim());
            double moi = Double.parseDouble(newIndexField.getText().trim());
            double use = moi - cu;
            consumeValueLabel.setText(String.format("%.0f kWh", use));
            if (use < 0) {
                setStatus("  ✖  Chỉ số mới nhỏ hơn chỉ số cũ!", UIConstants.ERROR, UIConstants.ERROR_BG);
            } else if (use == 0) {
                setStatus("  ⚠  Tiêu thụ 0 kWh — kiểm tra lại.", UIConstants.WARNING, UIConstants.WARNING_BG);
            } else {
                setStatus(String.format("  ✔  Tiêu thụ %.0f kWh — hợp lệ.", use), UIConstants.SUCCESS, UIConstants.SUCCESS_BG);
            }
        } catch (NumberFormatException ignored) {
            consumeValueLabel.setText("— kWh");
            setStatus("  Nhập chỉ số để tính tiêu thụ...", UIConstants.COLOR_TEXT_MUTED, UIConstants.COLOR_DIVIDER);
        }
    }

    private void setStatus(String text, Color fg, Color border) {
        statusLabel.setText(text);
        statusLabel.setForeground(fg);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1, true),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)));
    }

    // =========================================================================
    // SAVE WORKFLOW
    // =========================================================================
    private void saveReading() {
        if (!util.PermissionManager.getInstance().checkPermission(model.Permission.INDEX_CREATE)) return;

        Object sel = householdBox.getSelectedItem();
        if (!(sel instanceof Household)) {
            ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(this),
                "Vui lòng chọn hộ gia đình!", "Thiếu thông tin");
            return;
        }
        Household hh = (Household) sel;
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
        try { cu = Double.parseDouble(cuStr); moi = Double.parseDouble(moiStr); }
        catch (NumberFormatException ex) {
            ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(this),
                "Chỉ số phải là số!", "Dữ liệu không hợp lệ");
            return;
        }
        if (moi <= cu) {
            ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(this),
                "Chỉ số mới phải lớn hơn chỉ số cũ!", "Dữ liệu không hợp lệ");
            return;
        }

        MeterReading reading = new MeterReading();
        reading.setHouseholdId(hh.getHouseholdId());
        reading.setMonth(month);  reading.setYear(year);
        reading.setOldIndex(cu);  reading.setNewIndex(moi);
        reading.setConsumption(moi - cu);

        // Run the actual save on a worker thread (involves DB + bill generation)
        setFormEnabled(false);
        new SwingWorker<SaveResult, Void>() {
            @Override protected SaveResult doInBackground() {
                return readingService.addReadingAndGenerateBill(reading);
            }
            @Override protected void done() {
                setFormEnabled(true);
                try {
                    SaveResult result = get();
                    handleSaveResult(result, hh, month, year, reading);
                } catch (Exception ex) {
                    ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(ElectricityIndexPanel.this),
                        "Lỗi không xác định: " + ex.getMessage(), "Lỗi");
                }
            }
        }.execute();
    }

    private void handleSaveResult(SaveResult result, Household hh,
                                  int month, int year, MeterReading reading) {
        if (result.status == SaveResult.Status.DUPLICATE) {
            boolean confirm = ThemeManager.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this), result.message, "Chỉ số đã tồn tại");
            if (!confirm) return;

            final int existingId = result.readingId;
            setFormEnabled(false);
            new SwingWorker<SaveResult, Void>() {
                @Override protected SaveResult doInBackground() {
                    return readingService.updateReadingAndBill(existingId, reading);
                }
                @Override protected void done() {
                    setFormEnabled(true);
                    try {
                        SaveResult upd = get();
                        if (upd.status == SaveResult.Status.SUCCESS) onSaveSuccess(hh, month, year, upd.message);
                        else ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(ElectricityIndexPanel.this),
                                upd.message, "Lỗi cập nhật");
                    } catch (Exception ex) { /* ignore */ }
                }
            }.execute();

        } else if (result.status == SaveResult.Status.SUCCESS) {
            onSaveSuccess(hh, month, year, result.message);
        } else {
            ThemeManager.showInfoDialog(SwingUtilities.getWindowAncestor(this), result.message, "Lỗi");
        }
    }

    private void onSaveSuccess(Household hh, int month, int year, String message) {
        String msg = message != null ? message
            : "Đã lưu chỉ số điện và tạo hóa đơn cho hộ "
              + hh.getHouseholdCode() + " tháng "
              + String.format("%02d/%04d", month, year) + "!";

        ToastNotification.show(SwingUtilities.getWindowAncestor(this), msg, ToastNotification.Type.SUCCESS);

        // Reload table async, then flash new row
        final String code = hh.getHouseholdCode();
        new SwingWorker<List<MeterReading>, Void>() {
            @Override protected List<MeterReading> doInBackground() {
                return readingService.search(null, null, null);
            }
            @Override protected void done() {
                try {
                    populateTable(get());
                    flashNewRow(code, month, year);
                } catch (Exception ex) { /* ignore */ }
            }
        }.execute();

        notifyInvoicePanelReload();

        // Continuous-entry: clear new index, focus back to household search
        newIndexField.setText("");
        recalcConsumption();
        JTextField ed = getEditor();
        ed.selectAll();
        ed.requestFocusInWindow();
    }

    private void flashNewRow(String code, int month, int year) {
        String period = String.format("%02d/%04d", month, year);
        DefaultTableModel m = table.getModel();
        for (int i = 0; i < m.getRowCount(); i++) {
            if (code.equalsIgnoreCase(String.valueOf(m.getValueAt(i, COL_CODE)))
                    && period.equals(String.valueOf(m.getValueAt(i, COL_PERIOD)))) {
                int viewRow = table.getTable().convertRowIndexToView(i);
                table.flashRow(viewRow);
                return;
            }
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private void clearInfoFields() {
        householdCodeField.setText("");
        ownerNameField.setText("");
        addressField.setText("");
        oldIndexField.setText("");
        consumeValueLabel.setText("— kWh");
        setStatus("  Nhập chỉ số để tính tiêu thụ...", UIConstants.COLOR_TEXT_MUTED, UIConstants.COLOR_DIVIDER);
    }

    private void clearForm() {
        updatingCombo = true;
        getEditor().setText("");
        householdBox.setSelectedIndex(-1);
        updatingCombo = false;
        clearInfoFields();
        newIndexField.setText("");
        recalcConsumption();
        getEditor().requestFocusInWindow();
    }

    private void setFormEnabled(boolean enabled) {
        areaBox.setEnabled(enabled);
        householdBox.setEnabled(enabled);
        newIndexField.setEnabled(enabled);
        setCursor(enabled ? Cursor.getDefaultCursor()
                          : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private JTextField getEditor() {
        return (JTextField) householdBox.getEditor().getEditorComponent();
    }

    private static RoundedTextField readOnly() {
        RoundedTextField f = new RoundedTextField();
        f.setEditable(false);
        return f;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_NORMAL_BOLD);
        l.setForeground(UIConstants.COLOR_TEXT_SECONDARY);
        return l;
    }

    // =========================================================================
    // EXTERNAL REFRESH (called by MainForm on tab navigation)
    // =========================================================================
    public void refreshData() {
        asyncLoadAreas();
        asyncLoadTable();
    }

    // =========================================================================
    // INVOICE PANEL NOTIFICATION
    // =========================================================================
    private void notifyInvoicePanelReload() {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) findAndReloadInvoicePanel(w);
    }

    private void findAndReloadInvoicePanel(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof InvoicePanel) { ((InvoicePanel) comp).reloadBills(); return; }
            if (comp instanceof Container)    { findAndReloadInvoicePanel((Container) comp); }
        }
    }
}
