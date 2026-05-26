package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Thay đổi các thông tin này cho phù hợp với SQL Server của bạn
    private static final String SERVER_NAME = "localhost"; 
    private static final String DATABASE_NAME = "QL_THU_TIEN_DIEN"; // Tên CSDL
    private static final String PORT = "1433"; 
    private static final String USERNAME = "sa"; // Tên đăng nhập SQL Server (nếu dùng SQL Server Authentication)
    private static final String PASSWORD = "123456"; // Mật khẩu của sa

    // Chuỗi kết nối JDBC cho SQL Server
    private static final String URL = "jdbc:sqlserver://" + SERVER_NAME + ":" + PORT 
                                    + ";databaseName=" + DATABASE_NAME 
                                    + ";encrypt=false"; // encrypt=false giúp tránh lỗi SSL khi dùng mssql-jdbc mới

    /**
     * Phương thức lấy kết nối tới cơ sở dữ liệu
     * @return Connection object
     */
    public static Connection getConnection() {
        Connection connection = null;
        try {
            // Đăng ký driver (Tùy chọn ở các phiên bản JDBC mới, nhưng nên giữ để đảm bảo)
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            
            // Tạo kết nối
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Kết nối cơ sở dữ liệu thành công!");
            
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy Driver JDBC cho SQL Server. Vui lòng thêm mssql-jdbc.jar vào thư viện dự án!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối CSDL: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Test kết nối
     */
    public static void main(String[] args) {
        Connection conn = getConnection();
        if (conn != null) {
            try {
                conn.close(); // Đóng kết nối sau khi test xong
                System.out.println("Đã đóng kết nối.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Kết nối thất bại.");
        }
    }
}
