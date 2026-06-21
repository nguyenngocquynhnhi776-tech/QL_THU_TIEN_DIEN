package util;

import model.Permission;
import model.Role;
import javax.swing.JOptionPane;
import java.util.EnumSet;
import java.util.Set;

/**
 * Singleton to manage user permissions and access control.
 */
public class PermissionManager {

    private static PermissionManager instance;
    private final Set<Permission> currentPermissions = EnumSet.noneOf(Permission.class);
    private Role currentRole;

    private PermissionManager() {}

    /**
     * Get the single instance of PermissionManager.
     */
    public static synchronized PermissionManager getInstance() {
        if (instance == null) {
            instance = new PermissionManager();
        }
        return instance;
    }

    /**
     * Load permissions based on user role.
     */
    public synchronized void loadPermissions(Role role) {
        this.currentRole = role;
        currentPermissions.clear();

        if (role == null) {
            return;
        }

        switch (role) {
            case ADMIN:
                // Admin gets all permissions
                currentPermissions.addAll(EnumSet.allOf(Permission.class));
                break;

            case CUSTOMER_MANAGER:
                // Menus
                currentPermissions.add(Permission.MENU_HOUSEHOLD);
                currentPermissions.add(Permission.MENU_INVOICE);
                currentPermissions.add(Permission.MENU_NOTIF);

                // Actions
                currentPermissions.add(Permission.HOUSEHOLD_VIEW);
                currentPermissions.add(Permission.HOUSEHOLD_SEARCH);
                currentPermissions.add(Permission.HOUSEHOLD_CREATE);
                currentPermissions.add(Permission.HOUSEHOLD_UPDATE);
                currentPermissions.add(Permission.INVOICE_VIEW);
                currentPermissions.add(Permission.NOTIF_VIEW);
                break;

            case METER_STAFF:
                // Menus
                currentPermissions.add(Permission.MENU_HOUSEHOLD);
                currentPermissions.add(Permission.MENU_INDEX);
                currentPermissions.add(Permission.MENU_BILLING);
                currentPermissions.add(Permission.MENU_NOTIF);

                // Actions
                currentPermissions.add(Permission.HOUSEHOLD_VIEW);
                currentPermissions.add(Permission.HOUSEHOLD_SEARCH);
                currentPermissions.add(Permission.INDEX_VIEW);
                currentPermissions.add(Permission.INDEX_CREATE);
                currentPermissions.add(Permission.INDEX_UPDATE);
                currentPermissions.add(Permission.BILLING_CALCULATE);
                currentPermissions.add(Permission.NOTIF_VIEW);
                break;

            case CASHIER:
                // Menus
                currentPermissions.add(Permission.MENU_INVOICE);
                currentPermissions.add(Permission.MENU_PAYMENT);
                currentPermissions.add(Permission.MENU_NOTIF);

                // Actions
                currentPermissions.add(Permission.INVOICE_VIEW);
                currentPermissions.add(Permission.PAYMENT_VIEW);
                currentPermissions.add(Permission.PAYMENT_CONFIRM);
                currentPermissions.add(Permission.PAYMENT_COLLECT);
                currentPermissions.add(Permission.PAYMENT_PRINT_RECEIPT);
                currentPermissions.add(Permission.NOTIF_VIEW);
                break;
        }
    }

    /**
     * Check if current user has the specified permission.
     */
    public synchronized boolean hasPermission(Permission p) {
        return currentPermissions.contains(p);
    }

    /**
     * Guard action by checking permission and showing message dialog if denied.
     * Returns true if authorized, false otherwise.
     */
    
    // KIỂM TRA QUYỀN TRUY CẬP
    public synchronized boolean checkPermission(Permission p) {
        if (hasPermission(p)) {
            return true;
        }
        JOptionPane.showMessageDialog(null, 
            "Bạn không có quyền thực hiện chức năng này.", 
            "Lỗi phân quyền", 
            JOptionPane.WARNING_MESSAGE);
        return false;
    }

    /**
     * Check if a sidebar menu card should be visible.
     */
    public synchronized boolean isMenuVisible(String menuCard) {
        if (menuCard == null) return false;
        switch (menuCard.toUpperCase()) {
            case "DASHBOARD":
                return true; // Dashboard is visible to all
            case "USERS":
                return hasPermission(Permission.MENU_USERS);
            case "HOUSEHOLD":
                return hasPermission(Permission.MENU_HOUSEHOLD);
            case "INDEX":
                return hasPermission(Permission.MENU_INDEX);
            case "BILLING":
                return hasPermission(Permission.MENU_BILLING);
            case "INVOICE":
                return hasPermission(Permission.MENU_INVOICE);
            case "PAYMENT":
                return hasPermission(Permission.MENU_PAYMENT);
            case "STATS":
                return hasPermission(Permission.MENU_STATS);
            case "AI":
                return hasPermission(Permission.MENU_AI);
            case "NOTIF":
                return hasPermission(Permission.MENU_NOTIF);
            case "SYSTEM":
                return hasPermission(Permission.MENU_SYSTEM);
            default:
                return false;
        }
    }

    /**
     * Clear loaded permissions on logout.
     */
    public synchronized void clear() {
        currentPermissions.clear();
        currentRole = null;
    }

    public synchronized Role getCurrentRole() {
        return currentRole;
    }
}
