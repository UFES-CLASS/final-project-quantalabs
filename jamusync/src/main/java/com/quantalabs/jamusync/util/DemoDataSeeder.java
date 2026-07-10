package com.quantalabs.jamusync.util;

import com.quantalabs.jamusync.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Random;

/**
 * DemoDataSeeder
 * ----------------------------------------------------------------------------
 * A ONE-TIME helper that fills the local jamusync.db with lots of realistic
 * sample data so the financial reports and charts look full for a demo:
 *
 *   - 10 jamu products (with prices, costs, stock, descriptions, benefits)
 *   - ~50 COMPLETED sales spread across the last 3 months
 *   - a few PENDING orders (so the dashboard "pending orders" tile is not zero)
 *   - matching stock_movements ('out') for every completed sale
 *
 * It is SAFE to run more than once: if the database already has any
 * transactions, it does nothing, so it will never create duplicate demo data.
 *
 * How to run it once: see the main() method at the bottom of this file, or the
 * instructions your teammate/Claude gave you.
 */
public class DemoDataSeeder {

    // The 10 jamu products we want to exist.
    // Each row is: name, description, ingredients, health_benefits, price, cost, stock
    // (price and cost are in Rupiah).
    private static final Object[][] PRODUCTS = {
        {"Kunyit Asam", "Refreshing turmeric and tamarind tonic.", "Turmeric, Tamarind, Palm sugar",
            "Eases period cramps and aids digestion.", 15000.0, 7000.0, 150},
        {"Beras Kencur", "Sweet rice and galangal energy drink.", "Rice, Galangal (Kencur), Ginger, Palm sugar",
            "Boosts stamina and relieves tiredness.", 14000.0, 6500.0, 140},
        {"Temulawak", "Earthy Java ginger tonic.", "Java Ginger (Temulawak), Tamarind, Palm sugar",
            "Supports liver health and appetite.", 16000.0, 7500.0, 120},
        {"Jahe Merah", "Warming red ginger brew.", "Red Ginger, Lemongrass, Palm sugar",
            "Warms the body and soothes sore throats.", 18000.0, 8000.0, 130},
        {"Wedang Uwuh", "Aromatic royal spice drink.", "Cinnamon, Cloves, Ginger, Secang wood",
            "Improves circulation and warms the body.", 20000.0, 9000.0, 90},
        {"Kunyit Sirih", "Turmeric with betel leaf.", "Turmeric, Betel leaf, Lime",
            "Freshens the body and supports women's health.", 15000.0, 7000.0, 80},
        {"Jamu Pahitan", "Traditional bitter herbal tonic.", "Sambiloto, Brotowali, Papaya leaf",
            "Cleanses the body and helps blood sugar.", 13000.0, 6000.0, 85},
        {"Kunci Suruh", "Fingerroot and betel blend.", "Fingerroot (Kunci), Betel leaf",
            "Reduces body odor and freshens breath.", 16000.0, 7500.0, 75},
        {"Sinom", "Young tamarind leaf cooler.", "Young Tamarind leaf, Turmeric, Palm sugar",
            "Cooling and good for digestion.", 12000.0, 5500.0, 160},
        {"Beras Kencur Susu", "Creamy rice galangal with milk.", "Rice, Galangal, Milk, Palm sugar",
            "Energising and gentle on the stomach.", 22000.0, 10000.0, 70}
    };

    // Some buyer names used for WhatsApp orders. Walk-in buyers stay anonymous.
    private static final String[] BUYER_NAMES = {
        "Andi", "Siti", "Budi", "Dewi", "Rian", "Putri", "Agus", "Maya", "Joko", "Rina"
    };

    // One shared Random for all the "pick something" choices.
    private static final Random random = new Random();

    // We treat this special product as a "marker": if it already exists, the
    // demo seed has run before, so we skip. This lets us add the demo bulk on
    // top of any real test data you already have, while never seeding twice.
    private static final String DEMO_MARKER_PRODUCT = "Beras Kencur Susu";

    /**
     * Fills the database with demo data. Does nothing if data already exists.
     */
    public static void seedDemoData() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {

            // 1) DO-NOT-DUPLICATE GUARD: if the demo marker product already
            //    exists, the demo seed has run before, so stop.
            if (demoDataAlreadyExists(conn)) {
                System.out.println("[DemoDataSeeder] Demo data already exists - skipping demo seed (no duplicates created).");
                return;
            }

            // Do everything in ONE database transaction (faster and all-or-nothing).
            conn.setAutoCommit(false);

            // Who "recorded" these sales? Use the owner (admin) account.
            int ownerId = getOwnerUserId(conn);

            // 2) Make sure the 10 products exist, and remember their id + price.
            int[] productIds = new int[PRODUCTS.length];
            double[] productPrices = new double[PRODUCTS.length];
            for (int i = 0; i < PRODUCTS.length; i++) {
                productIds[i] = ensureProduct(conn, PRODUCTS[i], ownerId);
                // Use the product's ACTUAL price from the database, so orders for
                // products that already existed use their real price.
                productPrices[i] = getProductPrice(conn, productIds[i]);
            }

            // 3) Create ~50 COMPLETED sales, spread over the last 90 days (3 months),
            //    so the monthly bar chart and daily line chart show real trends.
            int completedToCreate = 50;
            for (int i = 0; i < completedToCreate; i++) {
                String createdAt = randomDateTimeWithinDays(90);
                createOneOrder(conn, ownerId, productIds, productPrices, "Completed", createdAt, true);
            }

            // 4) Create a few PENDING orders from the last few days (for the dashboard tile).
            //    Pending orders are not fulfilled yet, so they get NO stock movement.
            int pendingToCreate = 4;
            for (int i = 0; i < pendingToCreate; i++) {
                String createdAt = randomDateTimeWithinDays(5);
                createOneOrder(conn, ownerId, productIds, productPrices, "Pending", createdAt, false);
            }

            // Save everything.
            conn.commit();
            System.out.println("[DemoDataSeeder] Done! Seeded " + PRODUCTS.length + " products, "
                    + completedToCreate + " completed sales and " + pendingToCreate + " pending orders.");

        } catch (SQLException e) {
            System.err.println("[DemoDataSeeder] Something went wrong while seeding demo data:");
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------
    //  Small helper methods below
    // ------------------------------------------------------------------

    // Returns true if the demo marker product already exists, which means the
    // demo seed has already been run at least once.
    private static boolean demoDataAlreadyExists(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE name = ?")) {
            ps.setString(1, DEMO_MARKER_PRODUCT);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // Reads a product's current price from the database.
    private static double getProductPrice(Connection conn, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT price FROM products WHERE id = ?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    // Finds the owner user's id (the sales need a valid recorded_by). Falls back to 1.
    private static int getOwnerUserId(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM users WHERE role = 'owner' ORDER BY id LIMIT 1")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 1; // sensible fallback (the default admin is usually id 1)
    }

    // Inserts a product if it does not already exist, then returns its id.
    // If it was brand new, we also record an initial 'in' stock movement.
    private static int ensureProduct(Connection conn, Object[] p, int ownerId) throws SQLException {
        String name = (String) p[0];
        int startingStock = ((Number) p[6]).intValue();

        String insertSql = "INSERT OR IGNORE INTO products " +
                "(name, description, ingredients, health_benefits, price, cost, stock, low_stock_threshold, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, name);
            ps.setString(2, (String) p[1]);
            ps.setString(3, (String) p[2]);
            ps.setString(4, (String) p[3]);
            ps.setDouble(5, ((Number) p[4]).doubleValue());
            ps.setDouble(6, ((Number) p[5]).doubleValue());
            ps.setInt(7, startingStock);
            ps.setInt(8, 15); // low stock threshold
            int rowsInserted = ps.executeUpdate();

            // Find the id (works whether we just inserted it or it already existed).
            int productId = getProductIdByName(conn, name);

            // Only add the "initial stock in" movement for a truly new product.
            if (rowsInserted > 0) {
                recordStockMovement(conn, productId, "in", startingStock,
                        "Initial demo stock", ownerId, LocalDate.now().minusDays(90) + " 09:00:00");
            }
            return productId;
        }
    }

    // Looks up a product id by its (unique) name.
    private static int getProductIdByName(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM products WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    // Builds one order: a transaction plus 1-4 transaction_items.
    // If recordStockOut is true (completed sale), it also writes 'out' stock
    // movements and reduces the product stock.
    private static void createOneOrder(Connection conn, int ownerId, int[] productIds,
                                       double[] productPrices, String status,
                                       String createdAt, boolean recordStockOut) throws SQLException {

        // Pick how many different products are in this order (1 to 4).
        int itemCount = 1 + random.nextInt(4);

        // Shuffle the product indexes so each order uses DIFFERENT products.
        int[] order = shuffledProductIndexes(productIds.length);

        // Work out the items and the subtotal first.
        int[] chosenIndex = new int[itemCount];
        int[] chosenQty = new int[itemCount];
        double subtotal = 0.0;
        for (int k = 0; k < itemCount; k++) {
            int idx = order[k];
            int qty = 1 + random.nextInt(5); // 1 to 5 of this product
            chosenIndex[k] = idx;
            chosenQty[k] = qty;
            subtotal += productPrices[idx] * qty;
        }

        // Order type is either walk-in or whatsapp. WhatsApp orders get a name.
        boolean isWhatsapp = random.nextBoolean();
        String orderType = isWhatsapp ? "whatsapp" : "walk-in";
        String buyerName = isWhatsapp ? BUYER_NAMES[random.nextInt(BUYER_NAMES.length)] : null;

        // Insert the transaction row (no voucher, no discount, so total = subtotal).
        String txSql = "INSERT INTO transactions " +
                "(order_type, buyer_name, voucher_id, subtotal, discount, total, status, recorded_by, created_at, updated_at) " +
                "VALUES (?, ?, NULL, ?, 0, ?, ?, ?, ?, ?)";
        int txId;
        try (PreparedStatement ps = conn.prepareStatement(txSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, orderType);
            ps.setString(2, buyerName); // null is fine for walk-in buyers
            ps.setDouble(3, subtotal);
            ps.setDouble(4, subtotal); // total (no discount)
            ps.setString(5, status);
            ps.setInt(6, ownerId);
            ps.setString(7, createdAt);
            ps.setString(8, createdAt);
            ps.executeUpdate();
            txId = firstGeneratedKey(ps);
        }

        // Insert each transaction_item, and (for completed sales) the stock movement.
        String itemSql = "INSERT INTO transaction_items " +
                "(transaction_id, product_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";
        for (int k = 0; k < itemCount; k++) {
            int idx = chosenIndex[k];
            int qty = chosenQty[k];
            double unitPrice = productPrices[idx];
            double lineSubtotal = unitPrice * qty;

            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                ps.setInt(1, txId);
                ps.setInt(2, productIds[idx]);
                ps.setInt(3, qty);
                ps.setDouble(4, unitPrice);
                ps.setDouble(5, lineSubtotal);
                ps.executeUpdate();
            }

            if (recordStockOut) {
                // Record that stock went OUT because of this sale...
                recordStockMovement(conn, productIds[idx], "out", qty,
                        "Sale (demo data)", ownerId, createdAt);
                // ...and reduce the product's stock (never below 0).
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE products SET stock = MAX(0, stock - ?) WHERE id = ?")) {
                    ps.setInt(1, qty);
                    ps.setInt(2, productIds[idx]);
                    ps.executeUpdate();
                }
            }
        }
    }

    // Writes one row into the stock_movements table.
    private static void recordStockMovement(Connection conn, int productId, String type,
                                            int quantity, String note, int performedBy,
                                            String createdAt) throws SQLException {
        String sql = "INSERT INTO stock_movements (product_id, type, quantity, note, performed_by, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setString(2, type);
            ps.setInt(3, quantity);
            ps.setString(4, note);
            ps.setInt(5, performedBy);
            ps.setString(6, createdAt);
            ps.executeUpdate();
        }
    }

    // Returns the auto-generated id from the last insert.
    private static int firstGeneratedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        return -1;
    }

    // Makes an array like [0,1,2,...] and shuffles it (Fisher-Yates shuffle),
    // so an order can pick several DIFFERENT products.
    private static int[] shuffledProductIndexes(int n) {
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = order[i];
            order[i] = order[j];
            order[j] = tmp;
        }
        return order;
    }

    // Builds a "YYYY-MM-DD HH:MM:SS" timestamp on a random recent day,
    // some time between 08:00 and 18:59 (shop opening hours).
    private static String randomDateTimeWithinDays(int maxDaysAgo) {
        LocalDate day = LocalDate.now().minusDays(random.nextInt(maxDaysAgo));
        int hour = 8 + random.nextInt(11);   // 8..18
        int minute = random.nextInt(60);      // 0..59
        return day + String.format(" %02d:%02d:00", hour, minute);
    }

    /**
     * Run this file directly to seed the database ONE TIME, without launching
     * the whole app. It first makes sure the tables exist, then seeds.
     */
    public static void main(String[] args) {
        // Make sure the tables (and the default admin user) exist first.
        DatabaseManager.getInstance().initializeDatabase();
        seedDemoData();
        System.out.println("[DemoDataSeeder] Finished. Open the app and check the Reports screen.");
    }
}
