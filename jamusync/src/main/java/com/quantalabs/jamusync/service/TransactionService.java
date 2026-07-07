package com.quantalabs.jamusync.service;

import com.quantalabs.jamusync.dao.ProductDAO;
import com.quantalabs.jamusync.dao.StockMovementDAO;
import com.quantalabs.jamusync.dao.TransactionDAO;
import com.quantalabs.jamusync.dao.TransactionItemDAO;
import com.quantalabs.jamusync.dao.VoucherDAO;
import com.quantalabs.jamusync.database.DatabaseManager;
import com.quantalabs.jamusync.model.Product;
import com.quantalabs.jamusync.model.StockMovement;
import com.quantalabs.jamusync.model.Transaction;
import com.quantalabs.jamusync.model.TransactionItem;
import com.quantalabs.jamusync.model.Voucher;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class TransactionService {

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final TransactionItemDAO transactionItemDAO = new TransactionItemDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final StockMovementDAO stockMovementDAO = new StockMovementDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();

    public SaleResult completeSale(Transaction transaction, List<TransactionItem> items) {
        if (items == null || items.isEmpty()) {
            return SaleResult.failure("Cart is empty.");
        }

        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            for (TransactionItem item : items) {
                Product product = productDAO.getProductById(conn, item.getProductId());
                if (product == null) {
                    conn.rollback();
                    return SaleResult.failure("Product not found: " + item.getProductName());
                }
                if (!product.isActive()) {
                    conn.rollback();
                    return SaleResult.failure("Product is inactive: " + product.getName());
                }
                if (product.getStock() < item.getQuantity()) {
                    conn.rollback();
                    return SaleResult.failure("Insufficient stock for " + product.getName() +
                        " (available: " + product.getStock() + ", requested: " + item.getQuantity() + ")");
                }
            }

            if (transaction.getVoucherId() != null && transaction.getVoucherId() > 0) {
                Voucher voucher = voucherDAO.getById(conn, transaction.getVoucherId());
                if (voucher == null || !voucherDAO.isValid(voucher)) {
                    conn.rollback();
                    return SaleResult.failure("Voucher is invalid or expired.");
                }
            }

            int transactionId = transactionDAO.insertTransactionReturningId(conn, transaction);
            if (transactionId <= 0) {
                conn.rollback();
                return SaleResult.failure("Failed to create transaction.");
            }

            for (TransactionItem item : items) {
                item.setTransactionId(transactionId);
                transactionItemDAO.insertItem(conn, item);

                Product product = productDAO.getProductById(conn, item.getProductId());
                int newStock = product.getStock() - item.getQuantity();
                productDAO.updateStock(conn, item.getProductId(), newStock);

                StockMovement movement = new StockMovement();
                movement.setProductId(item.getProductId());
                movement.setType("out");
                movement.setQuantity(item.getQuantity());
                movement.setNote("Sale transaction #" + transactionId);
                movement.setPerformedBy(transaction.getRecordedBy());
                stockMovementDAO.insertMovement(conn, movement);
            }

            if (transaction.getVoucherId() != null && transaction.getVoucherId() > 0) {
                voucherDAO.incrementUsage(conn, transaction.getVoucherId());
            }

            conn.commit();
            return SaleResult.success(transactionId);
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return SaleResult.failure("Database error during sale: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class SaleResult {
        private final boolean success;
        private final String message;
        private final int transactionId;

        private SaleResult(boolean success, String message, int transactionId) {
            this.success = success;
            this.message = message;
            this.transactionId = transactionId;
        }

        public static SaleResult success(int transactionId) {
            return new SaleResult(true, "Sale completed successfully.", transactionId);
        }

        public static SaleResult failure(String message) {
            return new SaleResult(false, message, -1);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getTransactionId() {
            return transactionId;
        }
    }
}
