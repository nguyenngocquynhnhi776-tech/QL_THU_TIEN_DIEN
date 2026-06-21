package UI.panels;

import UI.components.*;
import UI.theme.ThemeManager;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import dao.UserDAO;
import dao.impl.UserDAOImpl;
import model.User;
import util.PasswordUtil;
import util.PermissionUtil;
import session.UserSession;

/**
 * Quản lý người dùng hệ thống (chỉ Admin).
 */
public class UserManagementPanel extends BasePanel {

    private final ModernTable table;
    private final UserDAO userDAO = new UserDAOImpl();

    public UserManagementPanel() {
        super("Quản lý người dùng", "Phân quyền và quản lý tài khoản nhân viên hệ thống");

        // ---- Toolbar ----
        JPanel toolbar = new JPanel(new BorderLayout(UIConstants.SP_SM, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, UIConstants.SP_MD, 0));

        SearchField search = new SearchField("Tìm tên, tài khoản...");

        RoundedButton addBtn = new RoundedButton("+ Thêm người dùng", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        addBtn.setPreferredSize(new Dimension(180, 36));
        addBtn.addActionListener(e -> showUserDialog(false, -1));

        addBtn.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.USER_CREATE));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false); right.add(addBtn);

        toolbar.add(search, BorderLayout.WEST);
        toolbar.add(right,  BorderLayout.EAST);

        // ---- Table ----
        table = new ModernTable(new String[]{"Họ tên", "Tài khoản", "Vai trò", "Trạng thái", "Ngày tạo", "Thao tác"});
        table.setColumnWidths(160, 110, 120, 100, 140, 180);
        table.setColumnEditable(5, true);

        // Role badge
        table.setColumnRenderer(2, (tbl, val, sel, foc, row, col) -> {
            String s = val == null ? "" : val.toString();
            StatusBadge.Status st = s.contains("Quản trị") || s.contains("ADMIN") || s.contains("quản lý") || s.contains("CUSTOMER_MANAGER")
                ? StatusBadge.Status.ADMIN : StatusBadge.Status.STAFF;
            return new StatusBadge(s, st);
        });

        // Active/Locked badge
        table.setColumnRenderer(3, (tbl, val, sel, foc, row, col) -> {
            String s = val == null ? "" : val.toString();
            StatusBadge.Status st = s.contains("Hoạt động") || s.contains("ACTIVE") ? StatusBadge.Status.ACTIVE : StatusBadge.Status.LOCKED;
            return new StatusBadge(s, st);
        });

        // Action buttons custom renderer and editor
        table.setColumnRenderer(5, new ActionCellRenderer());
        table.getTable().getColumnModel().getColumn(5).setCellEditor(new ActionCellEditor());

        loadFromDatabase();

        contentArea.add(toolbar, BorderLayout.NORTH);
        contentArea.add(table,   BorderLayout.CENTER);
    }

    private JButton actionBtn(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(UIConstants.FONT_SMALL_BOLD);
        b.setForeground(color);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(50, 24));
        return b;
    }

    private void loadFromDatabase() {
        try {
            table.getModel().setRowCount(0);
            java.util.List<User> list = userDAO.getAll();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (User u : list) {
                String dateStr = u.getCreatedAt() != null ? sdf.format(u.getCreatedAt()) : "Chưa rõ";
                table.addRow(new Object[]{
                    u.getFullName(),
                    u.getUsername(),
                    u.getRoleDisplay(),
                    u.getStatusDisplay(),
                    dateStr,
                    ""
                });
            }
        } catch (Exception ex) {
            ThemeManager.showInfoDialog(this, "Lỗi tải dữ liệu người dùng từ CSDL: " + ex.getMessage(), "Lỗi hệ thống");
        }
    }

    private void editUser(int row) {
        showUserDialog(true, row);
    }

    private void toggleLockUser(int row) {
        String username = (String) table.getValueAt(row, 1);
        if (username.equalsIgnoreCase(UserSession.getInstance().getUsername())) {
            ToastNotification.show(SwingUtilities.getWindowAncestor(this), "Bạn không thể tự khóa tài khoản của chính mình!", ToastNotification.Type.ERROR);
            return;
        }

        try {
            User user = userDAO.findByUsername(username);
            if (user != null) {
                String currentStatus = user.getStatus();
                boolean isLocked = "LOCKED".equalsIgnoreCase(currentStatus);
                model.Permission perm = isLocked ? model.Permission.USER_UNLOCK : model.Permission.USER_LOCK;
                if (!util.PermissionManager.getInstance().checkPermission(perm)) {
                    return;
                }

                String newStatus = isLocked ? "ACTIVE" : "LOCKED";
                user.setStatus(newStatus);
                boolean updated = userDAO.update(user);
                if (updated) {
                    loadFromDatabase();
                    String msg = "ACTIVE".equals(newStatus) ? "Mở khóa tài khoản thành công!" : "Khóa tài khoản thành công!";
                    new service.impl.NotificationServiceImpl().addNotification(
                        "Cập nhật tài khoản",
                        ("ACTIVE".equals(newStatus) ? "Mở khóa" : "Khóa") + " tài khoản nhân viên '" + username + "'.",
                        "ACTIVE".equals(newStatus) ? "info" : "error",
                        "ACTIVE".equals(newStatus) ? "info" : "x-circle"
                    );
                    ToastNotification.show(SwingUtilities.getWindowAncestor(this), msg, ToastNotification.Type.SUCCESS);
                } else {
                    ToastNotification.show(SwingUtilities.getWindowAncestor(this), "Lỗi thay đổi trạng thái!", ToastNotification.Type.ERROR);
                }
            }
        } catch (Exception ex) {
            ThemeManager.showInfoDialog(this, "Lỗi khi thay đổi trạng thái tài khoản: " + ex.getMessage(), "Lỗi hệ thống");
        }
    }

    private void deleteUser(int row) {
        if (!util.PermissionManager.getInstance().checkPermission(model.Permission.USER_DELETE)) {
            return;
        }

        String username = (String) table.getValueAt(row, 1);
        if (username.equalsIgnoreCase(UserSession.getInstance().getUsername())) {
            ToastNotification.show(SwingUtilities.getWindowAncestor(this), "Bạn không thể tự xóa tài khoản của chính mình!", ToastNotification.Type.ERROR);
            return;
        }

        boolean ok = ThemeManager.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            "Bạn có chắc muốn xóa tài khoản '" + username + "'? Hành động này sẽ xóa vĩnh viễn dữ liệu trong CSDL.",
            "Xóa tài khoản"
        );
        if (ok) {
            try {
                User user = userDAO.findByUsername(username);
                if (user != null) {
                    boolean deleted = userDAO.delete(user.getUserId());
                    if (deleted) {
                        loadFromDatabase();
                        ToastNotification.show(
                            SwingUtilities.getWindowAncestor(this),
                            "Xóa tài khoản thành công!",
                            ToastNotification.Type.SUCCESS
                        );
                    } else {
                        ToastNotification.show(
                            SwingUtilities.getWindowAncestor(this),
                            "Lỗi xóa tài khoản!",
                            ToastNotification.Type.ERROR
                        );
                    }
                }
            } catch (Exception ex) {
                ThemeManager.showInfoDialog(this, "Lỗi khi xóa tài khoản: " + ex.getMessage(), "Lỗi hệ thống");
            }
        }
    }

    private void showUserDialog(boolean isEdit, int row) {
        model.Permission perm = isEdit ? model.Permission.USER_UPDATE : model.Permission.USER_CREATE;
        if (!util.PermissionManager.getInstance().checkPermission(perm)) {
            return;
        }

        String title = isEdit ? "Sửa người dùng" : "Thêm người dùng";
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dlg.setSize(440, 340);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        RoundedTextField nameField = new RoundedTextField();
        RoundedTextField userField = new RoundedTextField();
        RoundedPasswordField passField = new RoundedPasswordField();
        RoundedPasswordField confirmField = new RoundedPasswordField();
        JComboBox<String> roleBox = new JComboBox<>(new String[]{
            "Quản trị viên (ADMIN)",
            "Nhân viên quản lý khách hàng (CUSTOMER_MANAGER)",
            "Nhân viên ghi chỉ số điện (METER_STAFF)",
            "Nhân viên thu tiền (CASHIER)"
        });
        roleBox.setFont(UIConstants.FONT_NORMAL);

        if (isEdit) {
            nameField.setText((String) table.getValueAt(row, 0));
            userField.setText((String) table.getValueAt(row, 1));
            userField.setEnabled(false); // Username is read-only when editing
            
            String roleVal = (String) table.getValueAt(row, 2);
            if (roleVal != null) {
                if (roleVal.contains("Quản trị") || roleVal.contains("ADMIN")) {
                    roleBox.setSelectedIndex(0);
                } else if (roleVal.contains("khách hàng") || roleVal.contains("CUSTOMER_MANAGER") || roleVal.contains("MANAGER")) {
                    roleBox.setSelectedIndex(1);
                } else if (roleVal.contains("chỉ số") || roleVal.contains("METER_STAFF") || roleVal.contains("STAFF")) {
                    roleBox.setSelectedIndex(2);
                } else if (roleVal.contains("thu tiền") || roleVal.contains("CASHIER") || roleVal.contains("VIEWER")) {
                    roleBox.setSelectedIndex(3);
                }
            }
        }

        // Add to form layout
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        form.add(new JLabel("Họ tên:") {{ setFont(UIConstants.FONT_NORMAL_BOLD); }}, gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(nameField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        form.add(new JLabel("Tài khoản:") {{ setFont(UIConstants.FONT_NORMAL_BOLD); }}, gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(userField, gc);

        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0;
        form.add(new JLabel(isEdit ? "Mật khẩu mới:" : "Mật khẩu:") {{ setFont(UIConstants.FONT_NORMAL_BOLD); }}, gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(passField, gc);

        gc.gridx = 0; gc.gridy = 3; gc.weightx = 0;
        form.add(new JLabel("Xác nhận MK:") {{ setFont(UIConstants.FONT_NORMAL_BOLD); }}, gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(confirmField, gc);

        gc.gridx = 0; gc.gridy = 4; gc.weightx = 0;
        form.add(new JLabel("Vai trò:") {{ setFont(UIConstants.FONT_NORMAL_BOLD); }}, gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(roleBox, gc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnRow.setBackground(Color.WHITE);
        RoundedButton cancel = new RoundedButton("Hủy", UIConstants.BUTTON_RADIUS, UIConstants.COLOR_TEXT_MUTED);
        RoundedButton save   = new RoundedButton("Lưu", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        cancel.setPreferredSize(new Dimension(70, 34)); save.setPreferredSize(new Dimension(80, 34));

        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String name = nameField.getText().trim();
            String username = userField.getText().trim();
            String pass = new String(passField.getPassword());
            String confirm = new String(confirmField.getPassword());
            String role = (String) roleBox.getSelectedItem();

            if (name.isEmpty() || username.isEmpty()) {
                ThemeManager.showConfirmDialog(dlg, "Vui lòng điền đầy đủ Họ tên và Tài khoản!", "Thông báo");
                return;
            }

            if (!isEdit && pass.isEmpty()) {
                ThemeManager.showConfirmDialog(dlg, "Vui lòng nhập mật khẩu!", "Thông báo");
                return;
            }

            if (!pass.isEmpty() && !pass.equals(confirm)) {
                ThemeManager.showConfirmDialog(dlg, "Mật khẩu xác nhận không khớp!", "Thông báo");
                return;
            }

            String dbRole = "CASHIER";
            if (role.contains("ADMIN")) dbRole = "ADMIN";
            else if (role.contains("CUSTOMER_MANAGER")) dbRole = "CUSTOMER_MANAGER";
            else if (role.contains("METER_STAFF")) dbRole = "METER_STAFF";
            else if (role.contains("CASHIER")) dbRole = "CASHIER";

            try {
                if (isEdit) {
                    User existing = userDAO.findByUsername(username);
                    if (existing != null) {
                        existing.setFullName(name);
                        existing.setRole(dbRole);
                        if (!pass.isEmpty()) {
                            existing.setPasswordHash(PasswordUtil.hashPassword(pass));
                        }
                        boolean updated = userDAO.update(existing);
                        if (updated) {
                            loadFromDatabase();
                            ToastNotification.show(SwingUtilities.getWindowAncestor(this), "Cập nhật tài khoản thành công!", ToastNotification.Type.SUCCESS);
                        } else {
                            ToastNotification.show(SwingUtilities.getWindowAncestor(this), "Lỗi cập nhật tài khoản!", ToastNotification.Type.ERROR);
                        }
                    }
                } else {
                    User checkDup = userDAO.findByUsername(username);
                    if (checkDup != null) {
                        ThemeManager.showConfirmDialog(dlg, "Tài khoản đã tồn tại!", "Thông báo");
                        return;
                    }

                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setPasswordHash(PasswordUtil.hashPassword(pass));
                    newUser.setFullName(name);
                    newUser.setRole(dbRole);
                    newUser.setStatus("ACTIVE");

                    boolean inserted = userDAO.insert(newUser);
                    if (inserted) {
                        loadFromDatabase();
                        ToastNotification.show(SwingUtilities.getWindowAncestor(this), "Thêm tài khoản thành công!", ToastNotification.Type.SUCCESS);
                    } else {
                        ToastNotification.show(SwingUtilities.getWindowAncestor(this), "Lỗi thêm tài khoản vào CSDL!", ToastNotification.Type.ERROR);
                    }
                }
                dlg.dispose();
            } catch (Exception ex) {
                ThemeManager.showInfoDialog(dlg, "Lỗi thực thi CSDL: " + ex.getMessage(), "Lỗi hệ thống");
            }
        });

        btnRow.add(cancel); btnRow.add(save);

        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btnRow, BorderLayout.SOUTH);
        dlg.getContentPane().setBackground(Color.WHITE);
        dlg.setVisible(true);
    }

    private class ActionCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val,
                boolean sel, boolean focus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
            p.setOpaque(true);

            String status = (String) tbl.getValueAt(row, 3);
            JButton edit = actionBtn("Sửa", UIConstants.PRIMARY);
            JButton lock = actionBtn("Bị khóa".equals(status) ? "Mở" : "Khóa", UIConstants.WARNING);
            JButton del = actionBtn("Xóa", UIConstants.ERROR);

            edit.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.USER_UPDATE));
            boolean isLocked = "Bị khóa".equals(status);
            lock.setVisible(util.PermissionManager.getInstance().hasPermission(isLocked ? model.Permission.USER_UNLOCK : model.Permission.USER_LOCK));
            del.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.USER_DELETE));

            p.add(edit);
            p.add(lock);
            p.add(del);

            if (tbl.isRowSelected(row)) {
                p.setBackground(UIConstants.TABLE_SELECTION);
            } else {
                p.setBackground(row % 2 == 0 ? UIConstants.TABLE_ROW_EVEN : UIConstants.TABLE_ROW_ODD);
            }
            return p;
        }
    }

    private class ActionCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JPanel panel;
        private int currentRow;

        public ActionCellEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
            panel.setOpaque(true);

            JButton edit = actionBtn("Sửa", UIConstants.PRIMARY);
            JButton lock = actionBtn("Khóa", UIConstants.WARNING);
            JButton del = actionBtn("Xóa", UIConstants.ERROR);

            edit.addActionListener(e -> {
                fireEditingStopped();
                editUser(currentRow);
            });

            lock.addActionListener(e -> {
                fireEditingStopped();
                toggleLockUser(currentRow);
            });

            del.addActionListener(e -> {
                fireEditingStopped();
                deleteUser(currentRow);
            });

            panel.add(edit);
            panel.add(lock);
            panel.add(del);
        }

        @Override
        public Component getTableCellEditorComponent(JTable tbl, Object val,
                boolean isSelected, int row, int col) {
            currentRow = row;
            JButton editBtn = (JButton) panel.getComponent(0);
            JButton lockBtn = (JButton) panel.getComponent(1);
            JButton delBtn = (JButton) panel.getComponent(2);
            String status = (String) tbl.getValueAt(row, 3);
            boolean isLocked = "Bị khóa".equals(status);
            lockBtn.setText(isLocked ? "Mở" : "Khóa");

            editBtn.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.USER_UPDATE));
            lockBtn.setVisible(util.PermissionManager.getInstance().hasPermission(isLocked ? model.Permission.USER_UNLOCK : model.Permission.USER_LOCK));
            delBtn.setVisible(util.PermissionManager.getInstance().hasPermission(model.Permission.USER_DELETE));

            if (tbl.isRowSelected(row)) {
                panel.setBackground(UIConstants.TABLE_SELECTION);
            } else {
                panel.setBackground(row % 2 == 0 ? UIConstants.TABLE_ROW_EVEN : UIConstants.TABLE_ROW_ODD);
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public boolean isCellEditable(java.util.EventObject e) {
            return true;
        }
    }
}
