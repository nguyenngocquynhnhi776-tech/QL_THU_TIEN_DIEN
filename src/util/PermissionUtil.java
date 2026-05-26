package util;

import javax.swing.JButton;

/**
 * Utility class for role-based authorization check and UI component control.
 */
public class PermissionUtil {

    public static final String ROLE_ADMIN   = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_STAFF   = "STAFF";
    public static final String ROLE_VIEWER  = "VIEWER";

    /**
     * Check if a role can create objects/records.
     * Allowed: ADMIN, MANAGER
     */
    public static boolean canCreate(String role) {
        if (role == null) return false;
        String r = role.toUpperCase();
        return ROLE_ADMIN.equals(r) || ROLE_MANAGER.equals(r);
    }

    /**
     * Check if a role can edit objects/records.
     * Allowed: ADMIN, MANAGER
     */
    public static boolean canEdit(String role) {
        if (role == null) return false;
        String r = role.toUpperCase();
        return ROLE_ADMIN.equals(r) || ROLE_MANAGER.equals(r);
    }

    /**
     * Check if a role can delete objects/records.
     * Allowed: ADMIN
     */
    public static boolean canDelete(String role) {
        if (role == null) return false;
        String r = role.toUpperCase();
        return ROLE_ADMIN.equals(r);
    }

    /**
     * Check if a role can view objects/records.
     * Allowed: All roles (ADMIN, MANAGER, STAFF, VIEWER)
     */
    public static boolean canView(String role) {
        if (role == null) return false;
        String r = role.toUpperCase();
        return ROLE_ADMIN.equals(r) || ROLE_MANAGER.equals(r) 
            || ROLE_STAFF.equals(r) || ROLE_VIEWER.equals(r);
    }

    /**
     * Automatically disables or enables buttons based on the user's role.
     *
     * @param role the current user's role
     * @param addBtn button for adding/creating records (can be null)
     * @param editBtn button for editing/updating records (can be null)
     * @param deleteBtn button for deleting records (can be null)
     */
    public static void applyPermissions(String role, JButton addBtn, JButton editBtn, JButton deleteBtn) {
        if (addBtn != null) {
            addBtn.setEnabled(canCreate(role));
            addBtn.setToolTipText(canCreate(role) ? null : "Bạn không có quyền thực hiện hành động này!");
        }
        if (editBtn != null) {
            editBtn.setEnabled(canEdit(role));
            editBtn.setToolTipText(canEdit(role) ? null : "Bạn không có quyền thực hiện hành động này!");
        }
        if (deleteBtn != null) {
            deleteBtn.setEnabled(canDelete(role));
            deleteBtn.setToolTipText(canDelete(role) ? null : "Bạn không có quyền thực hiện hành động này!");
        }
    }
}
