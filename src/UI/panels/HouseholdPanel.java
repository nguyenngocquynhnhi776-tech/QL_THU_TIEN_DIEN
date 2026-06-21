// GIAO DIỆN QUÁN LÝ HỘ

package UI.panels;

import UI.components.*;
import UI.theme.ThemeManager;
import model.Area;
import model.Household;
import service.AreaService;
import service.HouseholdService;
import service.impl.AreaServiceImpl;
import service.impl.HouseholdServiceImpl;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Quản lý Khu vực & Hộ gia đình — tab 1: Khu vực, tab 2: Hộ gia đình.
 * Tất cả dữ liệu đều kết nối SQL Server qua DAO/Service.
 */
public class HouseholdPanel extends BasePanel {

    // Services
    private final AreaService      areaService      = new AreaServiceImpl();
    private final HouseholdService householdService = new HouseholdServiceImpl();

    // ---- Tab pane ----
    private final JTabbedPane tabbedPane;

    // ---- Area tab components ----
    private ModernTable areaTable;
    private SearchField areaSearch;

    // ---- Household tab components ----
    private ModernTable  hhTable;
    private SearchField  hhSearch;
    private JComboBox<Area> areaFilterBox;

    // ================================================================
    public HouseholdPanel() {
        super("Quản lý Hộ gia đình & Khu vực",
              "Quản lý khu vực điện và danh sách hộ gia đình dùng điện");

        tabbedPane = createStyledTabbedPane();

        tabbedPane.addTab("🗺  Khu vực",   buildAreaTab());
        tabbedPane.addTab("🏠  Hộ gia đình", buildHouseholdTab());

        // Reload household tab data when switching to it (area list in combo may change)
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                refreshAreaFilterCombo();
            }
        });

        contentArea.add(tabbedPane, BorderLayout.CENTER);
    }

    // ================================================================
    //  AREA TAB
    // ================================================================

    private JPanel buildAreaTab() {
        JPanel panel = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(UIConstants.SP_MD, 0, 0, 0));

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(UIConstants.SP_MD, 0));
        toolbar.setOpaque(false);

        areaSearch = new SearchField("Tìm theo mã, tên khu vực...");
        areaSearch.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { loadAreaTable(); }
            public void removeUpdate(DocumentEvent e)  { loadAreaTable(); }
            public void changedUpdate(DocumentEvent e) { loadAreaTable(); }
        });

        RoundedButton addAreaBtn = new RoundedButton("+ Thêm khu vực", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        addAreaBtn.setPreferredSize(new Dimension(160, 36));
        addAreaBtn.addActionListener(e -> showAreaDialog(false, null));
        addAreaBtn.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.HOUSEHOLD_CREATE));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(addAreaBtn);

        toolbar.add(areaSearch, BorderLayout.WEST);
        toolbar.add(right,      BorderLayout.EAST);

        // Table
        areaTable = new ModernTable(new String[]{"Mã KV", "Tên khu vực", "Trạng thái", "Ngày tạo", "Thao tác"});
        areaTable.setColumnWidths(80, 220, 120, 160, 160);
        areaTable.setColumnEditable(4, true);

        areaTable.setColumnRenderer(2, (tbl, val, sel, foc, row, col) -> {
            String s = val == null ? "" : val.toString();
            StatusBadge.Status st = s.equals("Hoạt động") ? StatusBadge.Status.ACTIVE : StatusBadge.Status.LOCKED;
            return new StatusBadge(s, st);
        });

        areaTable.setColumnRenderer(4, new AreaActionRenderer());
        areaTable.getTable().getColumnModel().getColumn(4).setCellEditor(new AreaActionEditor());

        loadAreaTable();

        panel.add(toolbar,   BorderLayout.NORTH);
        panel.add(areaTable, BorderLayout.CENTER);
        return panel;
    }

    private void loadAreaTable() {
        String kw = areaSearch != null ? areaSearch.getText().trim() : "";
        List<Area> list = areaService.getAll();

        areaTable.clearRows();
        for (Area a : list) {
            String display = a.getStatusDisplay();
            if (!kw.isEmpty()) {
                String combined = (a.getAreaCode() + " " + a.getAreaName()).toLowerCase();
                if (!combined.contains(kw.toLowerCase())) continue;
            }
            areaTable.addRow(new Object[]{
                a.getAreaCode(),
                a.getAreaName(),
                display,
                a.getCreatedAt() != null ? a.getCreatedAt().toString().substring(0, 10) : "",
                a.getAreaId()  // hidden id for actions
            });
        }
    }

    private void showAreaDialog(boolean isEdit, Area existing) {
        model.Permission perm = isEdit ? model.Permission.HOUSEHOLD_UPDATE : model.Permission.HOUSEHOLD_CREATE;
        if (!util.PermissionManager.getInstance().checkPermission(perm)) {
            return;
        }

        String title = isEdit ? "Sửa khu vực" : "Thêm khu vực mới";
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dlg.setSize(450, 310);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        // Header
        JPanel header = makeDialogHeader(title, isEdit ? "Cập nhật thông tin khu vực" : "Nhập thông tin khu vực mới");

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 28, 12, 28));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.fill   = GridBagConstraints.HORIZONTAL;

        RoundedTextField codeField = new RoundedTextField();
        RoundedTextField nameField = new RoundedTextField();
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});
        statusBox.setFont(UIConstants.FONT_NORMAL);

        if (isEdit && existing != null) {
            codeField.setText(existing.getAreaCode());
            codeField.setEnabled(false);   // code is PK, immutable
            nameField.setText(existing.getAreaName());
            statusBox.setSelectedItem(existing.getStatus());
        }

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        form.add(labelFor("Mã khu vực: *"), gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(codeField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        form.add(labelFor("Tên khu vực: *"), gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(nameField, gc);

        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0;
        form.add(labelFor("Trạng thái:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(statusBox, gc);

        // Buttons
        JPanel btnRow = makeButtonRow();
        RoundedButton cancelBtn = new RoundedButton("Hủy",   UIConstants.BUTTON_RADIUS, new Color(150, 150, 170));
        RoundedButton saveBtn   = new RoundedButton(isEdit ? "Cập nhật" : "Thêm mới", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        cancelBtn.setPreferredSize(new Dimension(80, 36));
        saveBtn.setPreferredSize(new Dimension(110, 36));

        cancelBtn.addActionListener(e -> dlg.dispose());
        saveBtn.addActionListener(e -> {
            Area area = new Area();
            area.setAreaCode(codeField.getText().trim().toUpperCase());
            area.setAreaName(nameField.getText().trim());
            area.setStatus((String) statusBox.getSelectedItem());

            String err;
            if (isEdit && existing != null) {
                area.setAreaId(existing.getAreaId());
                err = areaService.updateArea(area);
            } else {
                err = areaService.addArea(area);
            }

            if (err != null) {
                ThemeManager.showInfoDialog(dlg, err, "Lỗi");
            } else {
                dlg.dispose();
                loadAreaTable();
                ToastNotification.show(
                    SwingUtilities.getWindowAncestor(HouseholdPanel.this),
                    isEdit ? "Cập nhật khu vực thành công!" : "Thêm khu vực thành công!",
                    ToastNotification.Type.SUCCESS
                );
            }
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);

        dlg.add(header, BorderLayout.NORTH);
        dlg.add(form,   BorderLayout.CENTER);
        dlg.add(btnRow, BorderLayout.SOUTH);
        dlg.getContentPane().setBackground(Color.WHITE);
        dlg.setVisible(true);
    }

    private void deactivateArea(int areaId, String areaCode) {
        if (!util.PermissionManager.getInstance().checkPermission(model.Permission.HOUSEHOLD_DELETE)) {
            return;
        }

        boolean ok = ThemeManager.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            "Bạn có chắc muốn vô hiệu hóa khu vực " + areaCode + "?\n"
                + "Các hộ thuộc khu vực này vẫn được giữ nguyên.",
            "Xác nhận vô hiệu hóa"
        );
        if (ok) {
            boolean success = areaService.deactivateArea(areaId);
            if (success) {
                loadAreaTable();
                ToastNotification.show(
                    SwingUtilities.getWindowAncestor(this),
                    "Đã vô hiệu hóa khu vực " + areaCode + "!",
                    ToastNotification.Type.WARNING
                );
            } else {
                ThemeManager.showInfoDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Không thể vô hiệu hóa khu vực!",
                    "Lỗi"
                );
            }
        }
    }

    // ================================================================
    //  HOUSEHOLD TAB
    // ================================================================

    private JPanel buildHouseholdTab() {
        JPanel panel = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(UIConstants.SP_MD, 0, 0, 0));

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(UIConstants.SP_MD, 0));
        toolbar.setOpaque(false);

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTools.setOpaque(false);

        hhSearch = new SearchField("Tìm mã hộ, chủ hộ, SĐT...");
        hhSearch.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { loadHouseholdTable(); }
            public void removeUpdate(DocumentEvent e)  { loadHouseholdTable(); }
            public void changedUpdate(DocumentEvent e) { loadHouseholdTable(); }
        });

        // Area filter combo
        areaFilterBox = new JComboBox<>();
        areaFilterBox.setFont(UIConstants.FONT_NORMAL);
        areaFilterBox.setPreferredSize(new Dimension(200, 36));
        areaFilterBox.addActionListener(e -> loadHouseholdTable());
        refreshAreaFilterCombo();

        leftTools.add(hhSearch);
        leftTools.add(Box.createHorizontalStrut(8));
        leftTools.add(new JLabel("Khu vực:") {{
            setFont(UIConstants.FONT_NORMAL_BOLD);
            setForeground(UIConstants.COLOR_TEXT_SECONDARY);
        }});
        leftTools.add(areaFilterBox);

        RoundedButton addHhBtn = new RoundedButton("+ Thêm hộ mới", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        addHhBtn.setPreferredSize(new Dimension(155, 36));
        addHhBtn.addActionListener(e -> showHouseholdDialog(false, null));
        addHhBtn.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.HOUSEHOLD_CREATE));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(addHhBtn);

        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(right,     BorderLayout.EAST);

        // Table: Mã Hộ | Tên chủ hộ | Địa chỉ | Khu vực | SĐT | Trạng thái | Thao tác
        hhTable = new ModernTable(new String[]{"Mã Hộ", "Tên chủ hộ", "Địa chỉ", "Khu vực", "SĐT", "Trạng thái", "Thao tác"});
        hhTable.setColumnWidths(90, 160, 190, 130, 110, 105, 140);
        hhTable.setColumnEditable(6, true);

        hhTable.setColumnRenderer(5, (tbl, val, sel, foc, row, col) -> {
            String s = val == null ? "" : val.toString();
            StatusBadge.Status st = s.equals("Hoạt động") ? StatusBadge.Status.ACTIVE : StatusBadge.Status.LOCKED;
            return new StatusBadge(s, st);
        });

        hhTable.setColumnRenderer(6, new HhActionRenderer());
        hhTable.getTable().getColumnModel().getColumn(6).setCellEditor(new HhActionEditor());

        loadHouseholdTable();

        panel.add(toolbar,  BorderLayout.NORTH);
        panel.add(hhTable,  BorderLayout.CENTER);
        return panel;
    }

    private void refreshAreaFilterCombo() {
        if (areaFilterBox == null) return;
        Area sentinel = new Area(0, null, "-- Tất cả khu vực --", null, null);
        areaFilterBox.removeAllItems();
        areaFilterBox.addItem(sentinel);
        List<Area> areas = areaService.getAllActive();
        for (Area a : areas) {
            areaFilterBox.addItem(a);
        }
    }

    private void loadHouseholdTable() {
        if (hhTable == null) return;
        String kw = hhSearch != null ? hhSearch.getText().trim() : "";
        Integer areaId = null;
        if (areaFilterBox != null) {
            Area selected = (Area) areaFilterBox.getSelectedItem();
            if (selected != null && selected.getAreaId() > 0) {
                areaId = selected.getAreaId();
            }
        }

        List<Household> list = householdService.search(kw.isEmpty() ? null : kw, areaId);
        hhTable.clearRows();
        for (Household hh : list) {
            hhTable.addRow(new Object[]{
                hh.getHouseholdCode(),
                hh.getOwnerName(),
                hh.getAddress() != null ? hh.getAddress() : "",
                hh.getAreaName() != null ? hh.getAreaName() : "",
                hh.getPhone() != null ? hh.getPhone() : "",
                hh.getStatusDisplay(),
                hh.getHouseholdId()   // hidden id for actions
            });
        }
    }

    private void showHouseholdDialog(boolean isEdit, Household existing) {
        model.Permission perm = isEdit ? model.Permission.HOUSEHOLD_UPDATE : model.Permission.HOUSEHOLD_CREATE;
        if (!util.PermissionManager.getInstance().checkPermission(perm)) {
            return;
        }

        String title = isEdit ? "Sửa thông tin hộ" : "Thêm hộ gia đình mới";
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dlg.setSize(520, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel header = makeDialogHeader(title,
            isEdit ? "Cập nhật thông tin hộ gia đình" : "Nhập thông tin hộ gia đình mới");

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(18, 28, 12, 28));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(7, 8, 7, 8);
        gc.fill   = GridBagConstraints.HORIZONTAL;

        // Mã hộ (auto-generated, read-only on add)
        RoundedTextField codeField    = new RoundedTextField();
        RoundedTextField ownerField   = new RoundedTextField();
        RoundedTextField addressField = new RoundedTextField();
        RoundedTextField phoneField   = new RoundedTextField();

        // Area combo
        JComboBox<Area> areaBox = new JComboBox<>();
        areaBox.setFont(UIConstants.FONT_NORMAL);
        List<Area> activeAreas = areaService.getAllActive();
        for (Area a : activeAreas) areaBox.addItem(a);

        // Status combo (only shown when editing)
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});
        statusBox.setFont(UIConstants.FONT_NORMAL);

        if (isEdit && existing != null) {
            codeField.setText(existing.getHouseholdCode());
            codeField.setEnabled(false);
            ownerField.setText(existing.getOwnerName());
            addressField.setText(existing.getAddress() != null ? existing.getAddress() : "");
            phoneField.setText(existing.getPhone() != null ? existing.getPhone() : "");
            // Select matching area
            for (int i = 0; i < areaBox.getItemCount(); i++) {
                if (areaBox.getItemAt(i).getAreaId() == existing.getAreaId()) {
                    areaBox.setSelectedIndex(i);
                    break;
                }
            }
            statusBox.setSelectedItem(existing.getStatus());
        } else {
            codeField.setText("(Tự động tạo sau khi chọn khu vực)");
            codeField.setEnabled(false);
            // Auto-preview code when area changes
            areaBox.addActionListener(ev -> {
                Area selectedArea = (Area) areaBox.getSelectedItem();
                if (selectedArea != null) {
                    String preview = householdService.generateHouseholdCode(selectedArea.getAreaId());
                    codeField.setText(preview != null ? preview : "(Lỗi tạo mã)");
                }
            });
            // Trigger immediately for first selected area
            if (areaBox.getItemCount() > 0) {
                Area firstArea = (Area) areaBox.getSelectedItem();
                if (firstArea != null) {
                    String preview = householdService.generateHouseholdCode(firstArea.getAreaId());
                    codeField.setText(preview != null ? preview : "(Lỗi tạo mã)");
                }
            }
        }

        // Row 0: Mã hộ (read-only preview) + Khu vực
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        form.add(labelFor("Mã hộ:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(codeField, gc);
        gc.gridx = 2; gc.weightx = 0;
        form.add(labelFor("Khu vực: *"), gc);
        gc.gridx = 3; gc.weightx = 1;
        form.add(areaBox, gc);

        // Row 1: Tên chủ hộ (full)
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        form.add(labelFor("Tên chủ hộ: *"), gc);
        gc.gridx = 1; gc.weightx = 1; gc.gridwidth = 3;
        form.add(ownerField, gc);
        gc.gridwidth = 1;

        // Row 2: Địa chỉ (full)
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0;
        form.add(labelFor("Địa chỉ:"), gc);
        gc.gridx = 1; gc.weightx = 1; gc.gridwidth = 3;
        form.add(addressField, gc);
        gc.gridwidth = 1;

        // Row 3: SĐT + Trạng thái
        gc.gridx = 0; gc.gridy = 3; gc.weightx = 0;
        form.add(labelFor("Số điện thoại:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        if (isEdit) {
            // Khi sửa: SĐT span 1 cột, nhường cột 2-3 cho Trạng thái
            gc.gridwidth = 1;
            form.add(phoneField, gc);
            gc.gridwidth = 1;
            gc.gridx = 2; gc.weightx = 0;
            form.add(labelFor("Trạng thái:"), gc);
            gc.gridx = 3; gc.weightx = 1;
            form.add(statusBox, gc);
        } else {
            // Khi thêm mới: SĐT span 3 cột để rộng bằng các trường khác
            gc.gridwidth = 3;
            form.add(phoneField, gc);
            gc.gridwidth = 1;
        }

        // Buttons
        JPanel btnRow = makeButtonRow();
        RoundedButton cancelBtn = new RoundedButton("Hủy",   UIConstants.BUTTON_RADIUS, new Color(150, 150, 170));
        RoundedButton saveBtn   = new RoundedButton(isEdit ? "Cập nhật" : "Thêm hộ", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        cancelBtn.setPreferredSize(new Dimension(80, 36));
        saveBtn.setPreferredSize(new Dimension(110, 36));

        cancelBtn.addActionListener(e -> dlg.dispose());
        saveBtn.addActionListener(e -> {
            Area selectedArea = (Area) areaBox.getSelectedItem();
            if (selectedArea == null) {
                ThemeManager.showInfoDialog(dlg, "Vui lòng chọn khu vực!", "Thiếu thông tin");
                return;
            }

            Household hh = new Household();
            hh.setOwnerName(ownerField.getText().trim());
            hh.setAddress(addressField.getText().trim());
            hh.setPhone(phoneField.getText().trim());
            hh.setAreaId(selectedArea.getAreaId());

            String err;
            if (isEdit && existing != null) {
                hh.setHouseholdId(existing.getHouseholdId());
                hh.setHouseholdCode(existing.getHouseholdCode());
                hh.setStatus((String) statusBox.getSelectedItem());
                err = householdService.updateHousehold(hh);
            } else {
                hh.setStatus("ACTIVE");
                err = householdService.addHousehold(hh);
            }

            if (err != null) {
                ThemeManager.showInfoDialog(dlg, err, "Lỗi");
            } else {
                dlg.dispose();
                loadHouseholdTable();
                ToastNotification.show(
                    SwingUtilities.getWindowAncestor(HouseholdPanel.this),
                    isEdit ? "Cập nhật hộ thành công!" : "Đã thêm hộ " + hh.getHouseholdCode() + " thành công!",
                    ToastNotification.Type.SUCCESS
                );
            }
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);

        dlg.add(header, BorderLayout.NORTH);
        dlg.add(form,   BorderLayout.CENTER);
        dlg.add(btnRow, BorderLayout.SOUTH);
        dlg.getContentPane().setBackground(Color.WHITE);
        dlg.setVisible(true);
    }

    private void deactivateHousehold(int hhId, String hhCode) {
        if (!util.PermissionManager.getInstance().checkPermission(model.Permission.HOUSEHOLD_DELETE)) {
            return;
        }

        boolean ok = ThemeManager.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            "Bạn có chắc muốn vô hiệu hóa hộ " + hhCode + "?",
            "Xác nhận vô hiệu hóa"
        );
        if (ok) {
            boolean success = householdService.deactivateHousehold(hhId);
            if (success) {
                loadHouseholdTable();
                ToastNotification.show(
                    SwingUtilities.getWindowAncestor(this),
                    "Đã vô hiệu hóa hộ " + hhCode + "!",
                    ToastNotification.Type.WARNING
                );
            } else {
                ThemeManager.showInfoDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Không thể vô hiệu hóa hộ này!",
                    "Lỗi"
                );
            }
        }
    }



    // ================================================================
    //  INNER CLASSES — Household action column
    // ================================================================

    private class AreaActionRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val,
                boolean sel, boolean focus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
            p.setOpaque(true);
            p.setBackground(sel ? UIConstants.TABLE_SELECTION
                : (row % 2 == 0 ? UIConstants.TABLE_ROW_EVEN : UIConstants.TABLE_ROW_ODD));
            JButton edit = actionBtn("Sửa",       UIConstants.PRIMARY);
            JButton deac = actionBtn("Vô hiệu",   UIConstants.ERROR);
            
            edit.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.HOUSEHOLD_UPDATE));
            deac.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.HOUSEHOLD_DELETE));
            
            p.add(edit);
            p.add(deac);
            return p;
        }
    }

    private class AreaActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private int currentRow;

        AreaActionEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
            panel.setOpaque(true);

            JButton editBtn = actionBtn("Sửa",     UIConstants.PRIMARY);
            JButton deacBtn = actionBtn("Vô hiệu", UIConstants.ERROR);

            editBtn.addActionListener(e -> {
                fireEditingStopped();
                String code = (String) areaTable.getValueAt(currentRow, 0);
                Area area = areaService.getByCode(code);
                if (area != null) showAreaDialog(true, area);
            });

            deacBtn.addActionListener(e -> {
                fireEditingStopped();
                String code   = (String) areaTable.getValueAt(currentRow, 0);
                Object idObj  = areaTable.getValueAt(currentRow, 4);
                if (idObj instanceof Integer) {
                    deactivateArea((Integer) idObj, code);
                } else {
                    Area area = areaService.getByCode(code);
                    if (area != null) deactivateArea(area.getAreaId(), code);
                }
            });

            panel.add(editBtn);
            panel.add(deacBtn);
        }

        @Override
        public Component getTableCellEditorComponent(JTable tbl, Object val,
                boolean isSelected, int row, int col) {
            currentRow = row;
            JButton editBtn = (JButton) panel.getComponent(0);
            JButton deacBtn = (JButton) panel.getComponent(1);
            
            editBtn.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.HOUSEHOLD_UPDATE));
            deacBtn.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.HOUSEHOLD_DELETE));

            panel.setBackground(isSelected ? UIConstants.TABLE_SELECTION
                : (row % 2 == 0 ? UIConstants.TABLE_ROW_EVEN : UIConstants.TABLE_ROW_ODD));
            return panel;
        }

        @Override public Object getCellEditorValue() { return ""; }
        @Override public boolean isCellEditable(java.util.EventObject e) { return true; }
    }

    private class HhActionRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val,
                boolean sel, boolean focus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
            p.setOpaque(true);
            p.setBackground(sel ? UIConstants.TABLE_SELECTION
                : (row % 2 == 0 ? UIConstants.TABLE_ROW_EVEN : UIConstants.TABLE_ROW_ODD));
            JButton edit = actionBtn("Sửa",     UIConstants.PRIMARY);
            JButton deac = actionBtn("Vô hiệu", UIConstants.ERROR);
            
            edit.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.HOUSEHOLD_UPDATE));
            deac.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.HOUSEHOLD_DELETE));
            
            p.add(edit);
            p.add(deac);
            return p;
        }
    }

    private class HhActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private int currentRow;

        HhActionEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
            panel.setOpaque(true);

            JButton editBtn = actionBtn("Sửa",     UIConstants.PRIMARY);
            JButton deacBtn = actionBtn("Vô hiệu", UIConstants.ERROR);

            editBtn.addActionListener(e -> {
                fireEditingStopped();
                Object idObj = hhTable.getValueAt(currentRow, 6);
                if (idObj instanceof Integer) {
                    Household hh = householdService.getById((Integer) idObj);
                    if (hh != null) showHouseholdDialog(true, hh);
                }
            });

            deacBtn.addActionListener(e -> {
                fireEditingStopped();
                String code  = (String) hhTable.getValueAt(currentRow, 0);
                Object idObj = hhTable.getValueAt(currentRow, 6);
                if (idObj instanceof Integer) {
                    deactivateHousehold((Integer) idObj, code);
                }
            });

            panel.add(editBtn);
            panel.add(deacBtn);
        }

        @Override
        public Component getTableCellEditorComponent(JTable tbl, Object val,
                boolean isSelected, int row, int col) {
            currentRow = row;
            JButton editBtn = (JButton) panel.getComponent(0);
            JButton deacBtn = (JButton) panel.getComponent(1);

            editBtn.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.HOUSEHOLD_UPDATE));
            deacBtn.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.HOUSEHOLD_DELETE));

            panel.setBackground(isSelected ? UIConstants.TABLE_SELECTION
                : (row % 2 == 0 ? UIConstants.TABLE_ROW_EVEN : UIConstants.TABLE_ROW_ODD));
            return panel;
        }

        @Override public Object getCellEditorValue() { return ""; }
        @Override public boolean isCellEditable(java.util.EventObject e) { return true; }
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private JTabbedPane createStyledTabbedPane() {
        JTabbedPane tp = new JTabbedPane(JTabbedPane.TOP);
        tp.setFont(UIConstants.FONT_NORMAL_BOLD);
        tp.setBackground(UIConstants.COLOR_APP_BG);
        tp.setOpaque(false);
        tp.setBorder(BorderFactory.createEmptyBorder());
        return tp;
    }

    private JPanel makeDialogHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UIConstants.FONT_HEADER);
        titleLbl.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(UIConstants.FONT_SMALL);
        subLbl.setForeground(new Color(255, 255, 255, 180));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLbl);
        text.add(Box.createVerticalStrut(2));
        text.add(subLbl);
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    private JPanel makeButtonRow() {
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        btnRow.setBackground(Color.WHITE);
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 240)));
        return btnRow;
    }

    private JLabel labelFor(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_NORMAL_BOLD);
        lbl.setForeground(UIConstants.COLOR_TEXT_SECONDARY);
        return lbl;
    }

    private JButton actionBtn(String text, Color color) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), isEnabled() ? 25 : 10));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(UIConstants.FONT_SMALL_BOLD);
        b.setForeground(color);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(62, 26));
        return b;
    }
}
