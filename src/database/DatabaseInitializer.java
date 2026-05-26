package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import util.PasswordUtil;

/**
 * Initializes the database tables and populates default seed data if needed.
 */
public class DatabaseInitializer {

    /**
     * Initializes the database: checks/creates all required tables and seeds default data.
     */
    public static void initialize() {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("DatabaseInitializer: Failed to establish database connection.");
                return;
            }

            // 1. Users Table
            boolean usersTableExists = checkTableExists(conn, "USERS");
            if (!usersTableExists) {
                System.out.println("USERS table does not exist. Creating table...");
                createUsersTable(conn);
            } else {
                System.out.println("USERS table already exists.");
            }
            seedAdminUser(conn);

            // 2. Area Table
            boolean areaTableExists = checkTableExists(conn, "AREA");
            if (!areaTableExists) {
                System.out.println("AREA table does not exist. Creating table...");
                createAreaTable(conn);
            } else {
                System.out.println("AREA table already exists.");
            }
            seedAreas(conn);

            // 3. Household Table
            boolean hhTableExists = checkTableExists(conn, "HOUSEHOLD");
            if (!hhTableExists) {
                System.out.println("HOUSEHOLD table does not exist. Creating table...");
                createHouseholdTable(conn);
            } else {
                System.out.println("HOUSEHOLD table already exists.");
            }
            seedHouseholds(conn);

            // 4. Price Tier Table
            if (!checkTableExists(conn, "PRICE_TIER")) {
                System.out.println("PRICE_TIER table does not exist. Creating table...");
                createPriceTierTable(conn);
            } else {
                System.out.println("PRICE_TIER table already exists.");
            }
            seedPriceTiers(conn);

            // 5. Meter Reading Table
            if (!checkTableExists(conn, "METER_READING")) {
                System.out.println("METER_READING table does not exist. Creating table...");
                createMeterReadingTable(conn);
            } else {
                System.out.println("METER_READING table already exists.");
            }

            // 6. Bill Table
            if (!checkTableExists(conn, "BILL")) {
                System.out.println("BILL table does not exist. Creating table...");
                createBillTable(conn);
            } else {
                System.out.println("BILL table already exists.");
            }

            // 7. Payment Table
            if (!checkTableExists(conn, "PAYMENT")) {
                System.out.println("PAYMENT table does not exist. Creating table...");
                createPaymentTable(conn);
            } else {
                System.out.println("PAYMENT table already exists.");
            }

        } catch (SQLException e) {
            System.err.println("DatabaseInitializer SQL Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static boolean checkTableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData dbmd = conn.getMetaData();
        try (ResultSet rs = dbmd.getTables(null, null, tableName, null)) {
            if (rs.next()) {
                return true;
            }
        }
        // Fallback for case sensitivity or exact schema checks
        try (ResultSet rs = dbmd.getTables(null, null, tableName.toLowerCase(), null)) {
            if (rs.next()) {
                return true;
            }
        }
        return false;
    }

    private static void createUsersTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE USERS ("
                   + "  UserID INT IDENTITY(1,1) PRIMARY KEY,"
                   + "  Username VARCHAR(50) UNIQUE NOT NULL,"
                   + "  PasswordHash VARCHAR(255) NOT NULL,"
                   + "  FullName NVARCHAR(100),"
                   + "  Role VARCHAR(20) DEFAULT 'STAFF',"
                   + "  Status VARCHAR(20) DEFAULT 'ACTIVE',"
                   + "  CreatedAt DATETIME DEFAULT GETDATE()"
                   + ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("USERS table created successfully.");
        }
    }

    private static void createAreaTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE AREA ("
                   + "  AreaID INT IDENTITY(1,1) PRIMARY KEY,"
                   + "  AreaCode VARCHAR(20) UNIQUE NOT NULL,"
                   + "  AreaName NVARCHAR(100) NOT NULL,"
                   + "  Status VARCHAR(20) DEFAULT 'ACTIVE',"
                   + "  CreatedAt DATETIME DEFAULT GETDATE()"
                   + ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("AREA table created successfully.");
        }
    }

    private static void createHouseholdTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE HOUSEHOLD ("
                   + "  HouseholdID INT IDENTITY(1,1) PRIMARY KEY,"
                   + "  HouseholdCode VARCHAR(30) UNIQUE NOT NULL,"
                   + "  OwnerName NVARCHAR(100) NOT NULL,"
                   + "  Address NVARCHAR(255),"
                   + "  Phone VARCHAR(20),"
                   + "  AreaID INT,"
                   + "  Status VARCHAR(20) DEFAULT 'ACTIVE',"
                   + "  CreatedAt DATETIME DEFAULT GETDATE(),"
                   + "  FOREIGN KEY (AreaID) REFERENCES AREA(AreaID)"
                   + ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("HOUSEHOLD table created successfully.");
        }
    }

    private static void createPriceTierTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE PRICE_TIER ("
                   + "  PriceID   INT IDENTITY(1,1) PRIMARY KEY,"
                   + "  LevelFrom INT NOT NULL,"
                   + "  LevelTo   INT NOT NULL,"
                   + "  UnitPrice DECIMAL(18,2) NOT NULL"
                   + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("PRICE_TIER table created successfully.");
        }
    }

    private static void createMeterReadingTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE METER_READING ("
                   + "  ReadingID   INT IDENTITY(1,1) PRIMARY KEY,"
                   + "  HouseholdID INT NOT NULL,"
                   + "  Month       INT NOT NULL,"
                   + "  Year        INT NOT NULL,"
                   + "  OldIndex    DECIMAL(10,2) NOT NULL,"
                   + "  NewIndex    DECIMAL(10,2) NOT NULL,"
                   + "  Consumption DECIMAL(10,2) NOT NULL,"
                   + "  CreatedAt   DATETIME DEFAULT GETDATE(),"
                   + "  FOREIGN KEY (HouseholdID) REFERENCES HOUSEHOLD(HouseholdID),"
                   + "  CONSTRAINT UQ_READING UNIQUE (HouseholdID, Month, Year)"
                   + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("METER_READING table created successfully.");
        }
    }

    private static void createBillTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE BILL ("
                   + "  BillID        INT IDENTITY(1,1) PRIMARY KEY,"
                   + "  BillCode      VARCHAR(30) UNIQUE NOT NULL,"
                   + "  HouseholdID   INT NOT NULL,"
                   + "  ReadingID     INT NOT NULL,"
                   + "  TotalAmount   DECIMAL(18,2) NOT NULL,"
                   + "  PaymentStatus VARCHAR(20) DEFAULT 'UNPAID',"
                   + "  CreatedAt     DATETIME DEFAULT GETDATE(),"
                   + "  FOREIGN KEY (HouseholdID) REFERENCES HOUSEHOLD(HouseholdID),"
                   + "  FOREIGN KEY (ReadingID)   REFERENCES METER_READING(ReadingID)"
                   + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("BILL table created successfully.");
        }
    }

    private static void createPaymentTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE PAYMENT ("
                   + "  PaymentID     INT IDENTITY(1,1) PRIMARY KEY,"
                   + "  BillID        INT NOT NULL,"
                   + "  Amount        DECIMAL(18,2) NOT NULL,"
                   + "  PaymentMethod VARCHAR(50),"
                   + "  PaymentDate   DATETIME DEFAULT GETDATE(),"
                   + "  Note          NVARCHAR(255),"
                   + "  FOREIGN KEY (BillID) REFERENCES BILL(BillID)"
                   + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("PAYMENT table created successfully.");
        }
    }

    private static void seedAdminUser(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) AS total FROM USERS";
        int totalUsers = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                totalUsers = rs.getInt("total");
            }
        }

        if (totalUsers == 0) {
            System.out.println("USERS table is empty. Seeding default admin user...");
            String insertSql = "INSERT INTO USERS (Username, PasswordHash, FullName, Role, Status) "
                             + "VALUES (?, ?, ?, ?, ?)";
            
            String hashedPwd = PasswordUtil.hashPassword("admin123");
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, "admin");
                pstmt.setString(2, hashedPwd);
                pstmt.setString(3, "Nguyễn Quản Trị");
                pstmt.setString(4, "ADMIN");
                pstmt.setString(5, "ACTIVE");
                
                pstmt.executeUpdate();
                System.out.println("Default admin user (username: admin, password: admin123) seeded successfully.");
            }
        }
    }

    private static void seedAreas(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) AS total FROM AREA";
        int totalAreas = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                totalAreas = rs.getInt("total");
            }
        }

        if (totalAreas == 0) {
            System.out.println("AREA table is empty. Seeding default areas...");
            String insertSql = "INSERT INTO AREA (AreaCode, AreaName, Status) VALUES (?, ?, ?)";
            String[][] defaultAreas = {
                {"KV01", "Quận 1", "ACTIVE"},
                {"KV02", "Quận 3", "ACTIVE"},
                {"KV03", "Quận 5", "ACTIVE"},
                {"KV04", "Quận 10", "ACTIVE"},
                {"KV05", "Bình Thạnh", "ACTIVE"}
            };
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (String[] area : defaultAreas) {
                    pstmt.setString(1, area[0]);
                    pstmt.setString(2, area[1]);
                    pstmt.setString(3, area[2]);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                System.out.println("Default areas seeded successfully.");
            }
        }
    }

    private static void seedHouseholds(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) AS total FROM HOUSEHOLD";
        int totalHHs = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                totalHHs = rs.getInt("total");
            }
        }

        if (totalHHs == 0) {
            System.out.println("HOUSEHOLD table is empty. Seeding default households...");
            
            // First get Area IDs to reference correctly
            int kv01Id = 0, kv02Id = 0, kv05Id = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT AreaID, AreaCode FROM AREA")) {
                while (rs.next()) {
                    String code = rs.getString("AreaCode");
                    int id = rs.getInt("AreaID");
                    if ("KV01".equals(code)) kv01Id = id;
                    else if ("KV02".equals(code)) kv02Id = id;
                    else if ("KV05".equals(code)) kv05Id = id;
                }
            }

            String insertSql = "INSERT INTO HOUSEHOLD (HouseholdCode, OwnerName, Address, Phone, AreaID, Status) VALUES (?, ?, ?, ?, ?, ?)";
            Object[][] defaultHouseholds = {
                {"KV01-0001", "Nguyễn Văn An", "123 Lê Lợi, Q1", "0901234567", kv01Id, "ACTIVE"},
                {"KV02-0001", "Trần Thị Bình", "456 Hai Bà Trưng, Q3", "0909876543", kv02Id, "ACTIVE"},
                {"KV05-0001", "Lê Văn Cường", "789 Điện Biên Phủ, BT", "0912345678", kv05Id, "ACTIVE"}
            };

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (Object[] hh : defaultHouseholds) {
                    if ((int) hh[4] == 0) continue; // Skip if Area wasn't found/seeded
                    pstmt.setString(1, (String) hh[0]);
                    pstmt.setString(2, (String) hh[1]);
                    pstmt.setString(3, (String) hh[2]);
                    pstmt.setString(4, (String) hh[3]);
                    pstmt.setInt(5, (int) hh[4]);
                    pstmt.setString(6, (String) hh[5]);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                System.out.println("Default households seeded successfully.");
            }
        }
    }

    /**
     * Seeds the 6 standard EVN electricity pricing tiers if the table is empty.
     * LevelTo = -1 means unlimited (last tier: 401+ kWh).
     */
    private static void seedPriceTiers(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM PRICE_TIER";
        int count = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) count = rs.getInt(1);
        }
        if (count > 0) return;

        System.out.println("PRICE_TIER table is empty. Seeding default EVN pricing tiers...");
        String insertSql = "INSERT INTO PRICE_TIER (LevelFrom, LevelTo, UnitPrice) VALUES (?, ?, ?)";
        // EVN 6-tier pricing (VND per kWh), LevelTo=-1 means unlimited
        int[][] tiers = {
            {0,   50,  1806},
            {51,  100, 1866},
            {101, 200, 2167},
            {201, 300, 2729},
            {301, 400, 3050},
            {401, -1,  3151}
        };
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (int[] t : tiers) {
                ps.setInt(1, t[0]);
                ps.setInt(2, t[1]);
                ps.setInt(3, t[2]);
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("Default pricing tiers seeded successfully.");
        }
    }
}
