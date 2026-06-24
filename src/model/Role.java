package model;

/**
 * Role enum representing the user roles in the application.
 * Enum là một kiểu dữ liệu dùng để giới hạn một biến chỉ được nhận một số giá trị cố định đã định nghĩa trước.
 */
public enum Role {
    ADMIN("ADMIN", "Quản trị viên"),
    CUSTOMER_MANAGER("CUSTOMER_MANAGER", "Nhân viên quản lý khách hàng"),
    METER_STAFF("METER_STAFF", "Nhân viên ghi chỉ số điện"),
    CASHIER("CASHIER", "Nhân viên thu tiền");

    private final String code;
    private final String displayName;

    Role(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts a database role code to Role enum.
     * Defaults to CASHIER if the code is unknown/invalid.
     */
    public static Role fromCode(String code) {
        if (code == null) {
            return CASHIER;
        }
        String normalized = code.trim().toUpperCase();
        for (Role role : values()) {
            if (role.getCode().equals(normalized)) {
                return role;
            }
        }
        // Migration mapping or legacy mapping if needed:
        if ("MANAGER".equals(normalized)) {
            return CUSTOMER_MANAGER;
        }
        if ("STAFF".equals(normalized)) {
            return METER_STAFF;
        }
        if ("VIEWER".equals(normalized)) {
            return CASHIER;
        }
        return CASHIER;
    }
}
