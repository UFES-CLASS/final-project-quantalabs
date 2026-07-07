package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.ProductDAO;
import com.quantalabs.jamusync.dao.TransactionDAO;
import com.quantalabs.jamusync.dao.TransactionItemDAO;
import com.quantalabs.jamusync.model.Product;
import com.quantalabs.jamusync.model.Transaction;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class OwnerDashboardController {

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
        if (JamuSyncApp.getCurrentUser() != null) {
            profileLabel.setText(JamuSyncApp.getCurrentUser().getUsername());
        }

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
        int pendingOrdersCount = transactionDAO.getPendingTransactionsCount();

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

    // Navigations (Alerts for unbuilt items)
    @FXML
    public void handleNavProducts(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/ProductManagement.fxml", "JamuSync - Product Management");
    }

    @FXML
    public void handleNavStaff(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/StaffManagement.fxml", "JamuSync - Staff Management");
    }

    @FXML
    public void handleNavInventory(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/Inventory.fxml", "JamuSync - Inventory Management");
    }

    @FXML
    public void handleNavSales(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/Sales.fxml", "JamuSync - Sales POS");
    }

    @FXML
    public void handleNavTransactions(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/TransactionHistory.fxml", "JamuSync - Transaction History");
    }

    @FXML
    public void handleNavVouchers(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/VoucherManagement.fxml", "JamuSync - Voucher Management");
    }

    @FXML
    public void handleNavReports(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/FinancialReport.fxml", "JamuSync - Financial Reports");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        JamuSyncApp.setCurrentUser(null);
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/Login.fxml", "JamuSync - Sign In");
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("JamuSync - Info");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
