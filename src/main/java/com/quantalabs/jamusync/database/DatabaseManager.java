package com.quantalabs.jamusync.database;

import com.quantalabs.jamusync.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class DatabaseManager {

    private static DatabaseManager instance;
    private static final String DB_URL = "jdbc:sqlite:jamusync.db";

    private DatabaseManager() {
        // Private constructor for singleton
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Get a connection to the SQLite database.
     * @return Connection object.
     * @throws SQLException if a database access error occurs.
     */
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found!");
            e.printStackTrace();
        }
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initialize all database tables and seed default admin user.
     */
    public void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Enable foreign keys
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password_hash TEXT NOT NULL," +
                    "role TEXT NOT NULL CHECK(role IN ('owner', 'staff'))," +
                    "is_active INTEGER DEFAULT 1," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 2. Products Table
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT UNIQUE NOT NULL," +
                    "description TEXT," +
                    "ingredients TEXT," +
                    "health_benefits TEXT," +
                    "price REAL NOT NULL," +
                    "cost REAL NOT NULL," +
                    "stock INTEGER DEFAULT 0," +
                    "low_stock_threshold INTEGER DEFAULT 10," +
                    "image_path TEXT," +
                    "is_active INTEGER DEFAULT 1," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 3. Stock Movements Table
            stmt.execute("CREATE TABLE IF NOT EXISTS stock_movements (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "product_id INTEGER NOT NULL," +
                    "type TEXT NOT NULL CHECK(type IN ('in', 'out', 'adjustment'))," +
                    "quantity INTEGER NOT NULL," +
                    "note TEXT," +
                    "performed_by INTEGER," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (product_id) REFERENCES products(id)," +
                    "FOREIGN KEY (performed_by) REFERENCES users(id)" +
                    ");");

            // 4. Vouchers Table (created before transactions due to foreign key dependency)
            stmt.execute("CREATE TABLE IF NOT EXISTS vouchers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "code TEXT UNIQUE NOT NULL," +
                    "discount_type TEXT NOT NULL CHECK(discount_type IN ('fixed', 'percentage'))," +
                    "discount_value REAL NOT NULL," +
                    "usage_limit INTEGER DEFAULT 1," +
                    "usage_count INTEGER DEFAULT 0," +
                    "expiry_date DATE NOT NULL," +
                    "is_active INTEGER DEFAULT 1," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 5. Transactions Table
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "order_type TEXT NOT NULL CHECK(order_type IN ('walk-in', 'whatsapp'))," +
                    "buyer_name TEXT," +
                    "voucher_id INTEGER," +
                    "subtotal REAL NOT NULL," +
                    "discount REAL DEFAULT 0," +
                    "total REAL NOT NULL," +
                    "status TEXT DEFAULT 'Pending' CHECK(status IN ('Pending', 'Completed', 'Cancelled'))," +
                    "recorded_by INTEGER NOT NULL," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (voucher_id) REFERENCES vouchers(id)," +
                    "FOREIGN KEY (recorded_by) REFERENCES users(id)" +
                    ");");

            // 6. Transaction Items Table
            stmt.execute("CREATE TABLE IF NOT EXISTS transaction_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "transaction_id INTEGER NOT NULL," +
                    "product_id INTEGER NOT NULL," +
                    "quantity INTEGER NOT NULL," +
                    "unit_price REAL NOT NULL," +
                    "subtotal REAL NOT NULL," +
                    "FOREIGN KEY (transaction_id) REFERENCES transactions(id)," +
                    "FOREIGN KEY (product_id) REFERENCES products(id)" +
                    ");");

            System.out.println("Database tables initialized successfully.");

            // Seed default admin
            seedDefaultAdmin(conn);

        } catch (SQLException e) {
            System.err.println("Error while initializing database:");
            e.printStackTrace();
        }
    }

    private void seedDefaultAdmin(Connection conn) {
        String countSql = "SELECT COUNT(*) FROM users WHERE role = 'owner';";
        String insertSql = "INSERT INTO users (username, password_hash, role) VALUES ('admin', ?, 'owner');";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            
            if (rs.next() && rs.getInt(1) == 0) {
                String hashedPassword = PasswordUtil.hashPassword("admin123");
                if (hashedPassword != null) {
                    try (java.sql.PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        pstmt.setString(1, hashedPassword);
                        pstmt.executeUpdate();
                        System.out.println("Default admin user seeded successfully (username: admin, password: admin123).");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error seeding default admin user:");
            e.printStackTrace();
        }
    }
}
