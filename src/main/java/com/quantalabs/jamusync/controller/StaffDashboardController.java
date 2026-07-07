package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.TransactionDAO;
import com.quantalabs.jamusync.model.Transaction;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.DecimalFormat;
import java.util.List;

public class StaffDashboardController {

    @FXML
    private Label profileLabel;
    @FXML
    private Label revenueLabel;
    @FXML
    private Label salesCountLabel;

    // Table
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

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final DecimalFormat formatter = new DecimalFormat("Rp #,###");

    @FXML
    public void initialize() {
        int staffId = 0;
        if (JamuSyncApp.getCurrentUser() != null) {
            profileLabel.setText(JamuSyncApp.getCurrentUser().getUsername());
            staffId = JamuSyncApp.getCurrentUser().getId();
        }

        loadDashboardMetrics(staffId);
        setupTableColumns();
        loadTableData(staffId);
    }

    private void loadDashboardMetrics(int staffId) {
        double todayRevenue = transactionDAO.getTodaySalesTotalByStaff(staffId);
        int todaySalesCount = transactionDAO.getTodaySalesCountByStaff(staffId);

        revenueLabel.setText(formatter.format(todayRevenue));
        salesCountLabel.setText(todaySalesCount + (todaySalesCount == 1 ? " order" : " orders"));
    }

    private void setupTableColumns() {
        colTxId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colTxBuyer.setCellValueFactory(data -> {
            String buyer = data.getValue().getBuyerName();
            return new SimpleStringProperty(buyer == null || buyer.isEmpty() ? "Walk-In Buyer" : buyer);
        });
        colTxType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrderType()));
        colTxTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTotal()).asObject());
        colTxStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        colTxDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt()));
    }

    private void loadTableData(int staffId) {
        List<Transaction> txList = transactionDAO.getRecentTransactionsByStaff(staffId, 10);
        ObservableList<Transaction> txObservableList = FXCollections.observableArrayList(txList);
        transactionsTable.setItems(txObservableList);
    }

    @FXML
    public void handleNavSales(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/Sales.fxml", "JamuSync - Sales POS");
    }

    @FXML
    public void handleNavInventory(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/Inventory.fxml", "JamuSync - Inventory Management");
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
