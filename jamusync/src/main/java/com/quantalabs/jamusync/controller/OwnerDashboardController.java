package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.ProductDAO;
import com.quantalabs.jamusync.dao.TransactionDAO;
import com.quantalabs.jamusync.dao.TransactionItemDAO;
import com.quantalabs.jamusync.model.Product;
import com.quantalabs.jamusync.model.Transaction;
import com.quantalabs.jamusync.util.LanguageManager;
import com.quantalabs.jamusync.util.PendingOrderQueue;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Extends BaseController to reuse shared helpers (navigateTo, showAlert,
// getCurrentUser) instead of duplicating that code here.
public class OwnerDashboardController extends BaseController {

    @FXML
    private Label profileLabel;
    @FXML
    private Label revenueLabel;
    @FXML
    private Label salesCountLabel;
    @FXML
    private Label lowStockAlertLabel;
    @FXML
    private Label pendingOrdersLabel;

    // Sidebar navigation buttons (their text changes with the language).
    @FXML
    private Button navDashboardBtn;
    @FXML
    private Button navProductsBtn;
    @FXML
    private Button navStaffBtn;
    @FXML
    private Button navInventoryBtn;
    @FXML
    private Button navSalesBtn;
    @FXML
    private Button navHistoryBtn;
    @FXML
    private Button navVouchersBtn;
    @FXML
    private Button navReportsBtn;
    @FXML
    private Button signOutBtn;

    // Metric card titles (also translated).
    @FXML
    private Label revenueTitleLabel;
    @FXML
    private Label lowStockTitleLabel;
    @FXML
    private Label pendingTitleLabel;

    // Tables
    @FXML
    private TableView<Product> lowStockTable;
    @FXML
    private TableColumn<Product, String> colProductName;
    @FXML
    private TableColumn<Product, Integer> colCurrentStock;
    @FXML
    private TableColumn<Product, Integer> colThreshold;

    @FXML
    private TableView<Transaction> transactionsTable;
    @FXML
    private TableColumn<Transaction, Integer> colTxId;
    @FXML
    private TableColumn<Transaction, String> colTxBuyer;
    @FXML
    private TableColumn<Transaction, String> colTxType;
    @FXML
    private TableColumn<Transaction, Double> colTxTotal;
    @FXML
    private TableColumn<Transaction, String> colTxStatus;
    @FXML
    private TableColumn<Transaction, String> colTxDate;

    // Pie chart of today's sales by product.
    @FXML
    private PieChart dashPieChart;

    private final ProductDAO productDAO = new ProductDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final TransactionItemDAO transactionItemDAO = new TransactionItemDAO();
    private final DecimalFormat formatter = new DecimalFormat("Rp #,###");

    @FXML
    public void initialize() {
        // getCurrentUser() is inherited from BaseController.
        if (getCurrentUser() != null) {
            profileLabel.setText(getCurrentUser().getUsername());
        }

        // Translate the sidebar and metric titles into the chosen language.
        // The language persists across screens because LanguageManager is static.
        applyLanguage();

        loadDashboardMetrics();
        setupTableColumns();
        loadTableData();
        loadTodaysSalesChart();
    }

    // Fill the pie chart with today's completed sales, grouped by product.
    private void loadTodaysSalesChart() {
        String today = LocalDate.now().toString();
        List<Map.Entry<String, Integer>> sales = transactionItemDAO.getSalesByProduct(today, today);
        dashPieChart.getData().clear();
        for (Map.Entry<String, Integer> entry : sales) {
            dashPieChart.getData().add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
    }

    private void loadDashboardMetrics() {
        double todayRevenue = transactionDAO.getTodaySalesTotal();
        int todaySalesCount = transactionDAO.getTodaySalesCount();
        int lowStockCount = productDAO.getLowStockCount();

        // We use a Queue (FIFO - First In First Out) to manage the pending guest
        // orders, because orders should be handled in the order they arrived:
        // the oldest pending order first. Here we fetch all pending transactions
        // and enqueue each one, then use the queue's size() for the count.
        List<Transaction> pendingList = transactionDAO.getFilteredTransactions("Pending", null, null);
        // getFilteredTransactions returns newest-first, so we reverse it to put
        // the OLDEST order first before enqueuing (true FIFO order).
        java.util.Collections.reverse(pendingList);
        PendingOrderQueue pendingOrderQueue = new PendingOrderQueue();
        for (Transaction pendingOrder : pendingList) {
            pendingOrderQueue.addOrder(pendingOrder); // enqueue each pending order
        }
        int pendingOrdersCount = pendingOrderQueue.size();

        revenueLabel.setText(formatter.format(todayRevenue));
        salesCountLabel.setText(todaySalesCount + " completed sales");
        lowStockAlertLabel.setText(lowStockCount + (lowStockCount == 1 ? " item" : " items"));
        pendingOrdersLabel.setText(pendingOrdersCount + (pendingOrdersCount == 1 ? " order" : " orders"));
    }

    private void setupTableColumns() {
        // Low Stock Columns
        colProductName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colCurrentStock.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStock()).asObject());
        colThreshold.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getLowStockThreshold()).asObject());

        // Transactions Columns
        colTxId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colTxBuyer.setCellValueFactory(data -> {
            String buyer = data.getValue().getBuyerName();
            return new SimpleStringProperty(buyer == null || buyer.isEmpty() ? "Walk-In Buyer" : buyer);
        });
        colTxType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrderType()));
        colTxTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTotal()).asObject());
        // Cell customization for total formatting could be done, but keeping simple for now
        colTxStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        colTxDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt()));
    }

    private void loadTableData() {
        // Load Low Stock Data
        List<Product> lowStockList = productDAO.getLowStockProducts();
        ObservableList<Product> lowStockObservableList = FXCollections.observableArrayList(lowStockList);
        lowStockTable.setItems(lowStockObservableList);

        // Load Recent Transactions
        List<Transaction> txList = transactionDAO.getRecentTransactions(10);
        ObservableList<Transaction> txObservableList = FXCollections.observableArrayList(txList);
        transactionsTable.setItems(txObservableList);
    }

    /** Set the sidebar button text and metric titles from the current language. */
    private void applyLanguage() {
        navDashboardBtn.setText(LanguageManager.getString("nav.dashboard"));
        navProductsBtn.setText(LanguageManager.getString("nav.products"));
        navStaffBtn.setText(LanguageManager.getString("nav.staff"));
        navInventoryBtn.setText(LanguageManager.getString("nav.inventory"));
        navSalesBtn.setText(LanguageManager.getString("nav.sales"));
        navHistoryBtn.setText(LanguageManager.getString("nav.history"));
        navVouchersBtn.setText(LanguageManager.getString("nav.vouchers"));
        navReportsBtn.setText(LanguageManager.getString("nav.reports"));
        signOutBtn.setText(LanguageManager.getString("nav.signout"));

        revenueTitleLabel.setText(LanguageManager.getString("dashboard.revenue"));
        lowStockTitleLabel.setText(LanguageManager.getString("dashboard.lowstock"));
        pendingTitleLabel.setText(LanguageManager.getString("dashboard.pending"));
    }

    // Navigations - all use the inherited navigateTo() helper from BaseController.
    @FXML
    public void handleNavProducts(ActionEvent event) {
        navigateTo("/com/quantalabs/jamusync/fxml/ProductManagement.fxml", event);
    }

    @FXML
    public void handleNavStaff(ActionEvent event) {
        navigateTo("/com/quantalabs/jamusync/fxml/StaffManagement.fxml", event);
    }

    @FXML
    public void handleNavInventory(ActionEvent event) {
        navigateTo("/com/quantalabs/jamusync/fxml/Inventory.fxml", event);
    }

    @FXML
    public void handleNavSales(ActionEvent event) {
        navigateTo("/com/quantalabs/jamusync/fxml/Sales.fxml", event);
    }

    @FXML
    public void handleNavTransactions(ActionEvent event) {
        navigateTo("/com/quantalabs/jamusync/fxml/TransactionHistory.fxml", event);
    }

    @FXML
    public void handleNavVouchers(ActionEvent event) {
        navigateTo("/com/quantalabs/jamusync/fxml/VoucherManagement.fxml", event);
    }

    @FXML
    public void handleNavReports(ActionEvent event) {
        navigateTo("/com/quantalabs/jamusync/fxml/FinancialReport.fxml", event);
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        JamuSyncApp.setCurrentUser(null);
        navigateTo("/com/quantalabs/jamusync/fxml/Login.fxml", event);
    }

    // The old private showInfo() helper was removed - we now inherit
    // showAlert() from BaseController instead of duplicating it here.
}
