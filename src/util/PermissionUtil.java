package util;

import javax.swing.JButton;
import model.Role;

/**
 * Utility class for role-based authorization check and UI component control.
 * @deprecated Use {@link PermissionManager} instead.
 */
@Deprecated
public class PermissionUtil {

    public static final String ROLE_ADMIN   = "ADMIN";
    public static final String ROLE_MANAGER = "CUSTOMER_MANAGER";
    public static final String ROLE_STAFF   = "METER_STAFF";
    public static final String ROLE_VIEWER  = "CASHIER";

    /**
     * Check if a role can create objects/records.
     * Allowed: ADMIN, CUSTOMER_MANAGER, METER_STAFF
     * @deprecated Use {@link PermissionManager#hasPermission(model.Permission)} instead.
     */
    @Deprecated
    public static boolean canCreate(String role) {
        if (role == null) return false;
        Role r = Role.fromCode(role);
        return r == Role.ADMIN || r == Role.CUSTOMER_MANAGER || r == Role.METER_STAFF;
    }

    /**
     * Check if a role can edit objects/records.
     * Allowed: ADMIN, CUSTOMER_MANAGER, METER_STAFF
     * @deprecated Use {@link PermissionManager#hasPermission(model.Permission)} instead.
     */
    @Deprecated
    public static boolean canEdit(String role) {
        if (role == null) return false;
        Role r = Role.fromCode(role);
        return r == Role.ADMIN || r == Role.CUSTOMER_MANAGER || r == Role.METER_STAFF;
    }

    /**
     * Check if a role can delete objects/records.
     * Allowed: ADMIN
     * @deprecated Use {@link PermissionManager#hasPermission(model.Permission)} instead.
     */
    @Deprecated
    public static boolean canDelete(String role) {
        if (role == null) return false;
        Role r = Role.fromCode(role);
        return r == Role.ADMIN;
    }

    /**
     * Check if a role can view objects/records.
     * Allowed: All roles
     * @deprecated Use {@link PermissionManager#hasPermission(model.Permission)} instead.
     */
    @Deprecated
    public static boolean canView(String role) {
        return role != null;
    }

    /**
     * Automatically disables or enables buttons based on the user's role.
     *
     * @param role the current user's role
     * @param addBtn button for adding/creating records (can be null)
     * @param editBtn button for editing/updating records (can be null)
     * @param deleteBtn button for deleting records (can be null)
     * @deprecated Use standard component configuration via {@link PermissionManager} instead.
     */
    @Deprecated
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
