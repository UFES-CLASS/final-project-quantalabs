package com.quantalabs.jamusync.database;

import com.quantalabs.jamusync.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

            // Seed default owner + a default staff account so both roles can log in.
            seedDefaultAdmin(conn);
            seedDefaultStaff(conn);

            // Populate sample data (products, sales history, pending orders) on a
            // fresh install so the app opens full of data automatically.
            seedSampleData(conn);

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

    /**
     * Seed a default staff account so a staff user can log in and reach the
     * Staff Dashboard. This runs only when no staff account exists yet, so it
     * never creates duplicates on subsequent runs.
     */
    private void seedDefaultStaff(Connection conn) {
        String countSql = "SELECT COUNT(*) FROM users WHERE role = 'staff';";
        String insertSql = "INSERT INTO users (username, password_hash, role) VALUES ('staff', ?, 'staff');";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {

            if (rs.next() && rs.getInt(1) == 0) {
                String hashedPassword = PasswordUtil.hashPassword("staff123");
                if (hashedPassword != null) {
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        pstmt.setString(1, hashedPassword);
                        pstmt.executeUpdate();
                        System.out.println("Default staff user seeded successfully (username: staff, password: staff123).");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error seeding default staff user:");
            e.printStackTrace();
        }
    }

    /**
     * Populate the database with realistic sample data the first time the app
     * runs: a catalog of products, roughly 30 completed sales spread across the
     * last few months (plus a handful dated today), and a few pending orders.
     *
     * This only runs when BOTH the products and transactions tables are empty,
     * so re-running the app never duplicates the seed data.
     */
    private void seedSampleData(Connection conn) {
        try {
            if (countRows(conn, "products") > 0 || countRows(conn, "transactions") > 0) {
                return; // Data already present – do not duplicate.
            }

            int ownerId = getUserId(conn, "admin");
            int staffId = getUserId(conn, "staff");
            if (ownerId <= 0) ownerId = 1;
            if (staffId <= 0) staffId = ownerId;

            // --- 1. Products (traditional Indonesian jamu) ---------------------
            // Columns: name, description, ingredients, benefits, price, cost, stock, threshold
            Object[][] productDefs = {
                {"Beras Kencur", "Sweet and warming daily tonic.", "Rice, Kencur, Palm sugar", "Boosts appetite, energy and stamina", 15000.0, 6000.0, 40, 10},
                {"Kunyit Asam", "Refreshing turmeric-tamarind blend.", "Turmeric, Tamarind, Palm sugar", "Eases cramps, freshens the body", 15000.0, 6000.0, 35, 10},
                {"Temulawak", "Earthy Javanese ginger tonic.", "Javanese ginger, Palm sugar", "Supports liver and digestion", 18000.0, 7000.0, 30, 10},
                {"Jahe Merah", "Spicy warming red ginger drink.", "Red ginger, Lemongrass, Honey", "Warms the body, soothes throat", 20000.0, 8000.0, 25, 8},
                {"Kunci Suruh", "Herbal betel-leaf blend.", "Fingerroot, Betel leaf", "Freshness and feminine wellness", 17000.0, 6500.0, 20, 8},
                {"Pahitan", "Traditional bitter green tonic.", "Sambiloto, Brotowali", "Detox and blood sugar balance", 16000.0, 6500.0, 15, 8},
                {"Sinom", "Light young-tamarind cooler.", "Young tamarind leaves, Turmeric", "Cooling and refreshing", 14000.0, 5500.0, 50, 12},
                {"Wedang Uwuh", "Aromatic spiced heritage drink.", "Cloves, Cinnamon, Ginger, Secang", "Warming antioxidant blend", 22000.0, 9000.0, 18, 8}
            };

            // Holds {productId, price, cost} for each inserted product.
            List<double[]> products = new ArrayList<>();
            String pInsert = "INSERT INTO products (name, description, ingredients, health_benefits, price, cost, stock, low_stock_threshold, is_active) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)";
            try (PreparedStatement ps = conn.prepareStatement(pInsert, PreparedStatement.RETURN_GENERATED_KEYS)) {
                for (Object[] p : productDefs) {
                    ps.setString(1, (String) p[0]);
                    ps.setString(2, (String) p[1]);
                    ps.setString(3, (String) p[2]);
                    ps.setString(4, (String) p[3]);
                    ps.setDouble(5, (Double) p[4]);
                    ps.setDouble(6, (Double) p[5]);
                    ps.setInt(7, (Integer) p[6]);
                    ps.setInt(8, (Integer) p[7]);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            products.add(new double[]{keys.getInt(1), (Double) p[4], (Double) p[5]});
                        }
                    }
                }
            }

            if (products.isEmpty()) {
                return;
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Random rng = new Random(20260710L); // fixed seed => reproducible sample data
            String[] buyers = {"Bu Siti", "Pak Budi", "Mbak Ayu", "Mas Joko", "Bu Rina", "Pak Anton", "Ibu Dewi"};

            // --- 2. ~30 completed sales spread across the last few months ------
            // A few are dated today (attributed to both owner and staff) so the
            // dashboards' "today" metrics are non-zero right away.
            int completedTarget = 30;
            for (int i = 0; i < completedTarget; i++) {
                LocalDateTime when;
                int recordedBy;
                if (i < 6) {
                    // Today's sales – alternate between staff and owner.
                    when = LocalDateTime.now().minusHours(rng.nextInt(8)).minusMinutes(rng.nextInt(60));
                    recordedBy = (i % 2 == 0) ? staffId : ownerId;
                } else {
                    // Historical sales across the previous ~5 months.
                    int monthsBack = 1 + rng.nextInt(5);
                    when = LocalDateTime.now()
                            .minusMonths(monthsBack)
                            .withDayOfMonth(1 + rng.nextInt(27))
                            .withHour(9 + rng.nextInt(9))
                            .withMinute(rng.nextInt(60));
                    recordedBy = (rng.nextInt(3) == 0) ? ownerId : staffId;
                }

                boolean whatsapp = rng.nextInt(3) == 0;
                String orderType = whatsapp ? "whatsapp" : "walk-in";
                String buyer = whatsapp ? buyers[rng.nextInt(buyers.length)] : null;

                insertSampleTransaction(conn, products, rng, orderType, buyer,
                        "Completed", recordedBy, when.format(fmt));
            }

            // --- 3. A few pending WhatsApp orders (recent) ---------------------
            int pendingTarget = 3;
            for (int i = 0; i < pendingTarget; i++) {
                LocalDateTime when = LocalDateTime.now().minusDays(rng.nextInt(3)).minusHours(rng.nextInt(12));
                insertSampleTransaction(conn, products, rng, "whatsapp",
                        buyers[rng.nextInt(buyers.length)], "Pending", ownerId, when.format(fmt));
            }

            System.out.println("Sample data seeded successfully (" + products.size() +
                    " products, " + completedTarget + " completed sales, " + pendingTarget + " pending orders).");

        } catch (SQLException e) {
            System.err.println("Error seeding sample data:");
            e.printStackTrace();
        }
    }

    /**
     * Insert one sample transaction with 1–3 line items chosen at random and
     * return nothing (used only by the seeder). Totals are derived from the
     * chosen items so the numbers are internally consistent.
     */
    private void insertSampleTransaction(Connection conn, List<double[]> products, Random rng,
                                         String orderType, String buyerName, String status,
                                         int recordedBy, String createdAt) throws SQLException {
        String tInsert = "INSERT INTO transactions (order_type, buyer_name, voucher_id, subtotal, discount, total, status, recorded_by, created_at, updated_at) " +
                         "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?)";
        String iInsert = "INSERT INTO transaction_items (transaction_id, product_id, quantity, unit_price, subtotal) " +
                         "VALUES (?, ?, ?, ?, ?)";

        int itemCount = 1 + rng.nextInt(3);
        List<double[]> lines = new ArrayList<>(); // {productId, quantity, unitPrice, lineSubtotal}
        double subtotal = 0;
        for (int j = 0; j < itemCount; j++) {
            double[] product = products.get(rng.nextInt(products.size()));
            int qty = 1 + rng.nextInt(4);
            double unitPrice = product[1];
            double lineSubtotal = unitPrice * qty;
            subtotal += lineSubtotal;
            lines.add(new double[]{product[0], qty, unitPrice, lineSubtotal});
        }
        // Occasionally apply a small round discount on completed orders.
        double discount = ("Completed".equals(status) && rng.nextInt(4) == 0) ? 5000.0 : 0.0;
        if (discount > subtotal) discount = 0.0;
        double total = subtotal - discount;

        int transactionId;
        try (PreparedStatement ps = conn.prepareStatement(tInsert, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, orderType);
            if (buyerName != null) {
                ps.setString(2, buyerName);
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }
            ps.setDouble(3, subtotal);
            ps.setDouble(4, discount);
            ps.setDouble(5, total);
            ps.setString(6, status);
            ps.setInt(7, recordedBy);
            ps.setString(8, createdAt);
            ps.setString(9, createdAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                transactionId = keys.next() ? keys.getInt(1) : -1;
            }
        }

        if (transactionId <= 0) {
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(iInsert)) {
            for (double[] line : lines) {
                ps.setInt(1, transactionId);
                ps.setInt(2, (int) line[0]);
                ps.setInt(3, (int) line[1]);
                ps.setDouble(4, line[2]);
                ps.setDouble(5, line[3]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Count the rows in a table (used to decide whether to seed). */
    private int countRows(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Look up a user id by username, or -1 if not found. */
    private int getUserId(Connection conn, String username) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }
}
