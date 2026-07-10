package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.TransactionDAO;
import com.quantalabs.jamusync.dao.TransactionItemDAO;
import com.quantalabs.jamusync.model.Transaction;
import com.quantalabs.jamusync.model.TransactionItem;
import com.quantalabs.jamusync.model.User;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

public class TransactionHistoryController {

    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, Integer> colId;
    @FXML private TableColumn<Transaction, String> colBuyer;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, Double> colSubtotal;
    @FXML private TableColumn<Transaction, Double> colDiscount;
    @FXML private TableColumn<Transaction, Double> colTotal;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, String> colStaff;
    @FXML private TableColumn<Transaction, String> colDate;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final TransactionItemDAO transactionItemDAO = new TransactionItemDAO();
    private final DecimalFormat formatter = new DecimalFormat("Rp #,###");

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList("All", "Pending", "Completed", "Cancelled"));
        statusFilter.getSelectionModel().selectFirst();
        setupTableColumns();
        loadTransactions();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colBuyer.setCellValueFactory(data -> {
            String buyer = data.getValue().getBuyerName();
            return new SimpleStringProperty(buyer == null || buyer.isEmpty() ? "Walk-In Buyer" : buyer);
        });
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrderType()));
        colSubtotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getSubtotal()).asObject());
        colDiscount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getDiscount()).asObject());
        colTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTotal()).asObject());
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        // Show the status as a small colored badge instead of plain text.
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(status);
                badge.getStyleClass().add("status-badge");
                if ("Completed".equalsIgnoreCase(status)) {
                    badge.getStyleClass().add("status-badge-completed");
                } else if ("Cancelled".equalsIgnoreCase(status)) {
                    badge.getStyleClass().add("status-badge-cancelled");
                } else {
                    badge.getStyleClass().add("status-badge-pending");
                }
                setText(null);
                setGraphic(badge);
            }
        });
        colStaff.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getRecordedByUsername() != null ? data.getValue().getRecordedByUsername() : ""
        ));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt()));
    }

    private void loadTransactions() {
        String status = statusFilter.getSelectionModel().getSelectedItem();
        String start = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : null;
        String end = endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : null;
        List<Transaction> transactions = transactionDAO.getFilteredTransactions(status, start, end);
        transactionTable.setItems(FXCollections.observableArrayList(transactions));
    }

    @FXML
    public void handleFilter(ActionEvent event) {
        loadTransactions();
    }

    @FXML
    public void handleClearFilter(ActionEvent event) {
        statusFilter.getSelectionModel().selectFirst();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        loadTransactions();
    }

    @FXML
    public void handleViewDetails(ActionEvent event) {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a transaction.");
            return;
        }

        List<TransactionItem> items = transactionItemDAO.getItemsByTransactionId(selected.getId());
        StringBuilder details = new StringBuilder();
        details.append("Transaction #").append(selected.getId()).append("\n");
        details.append("Type: ").append(selected.getOrderType()).append("\n");
        details.append("Status: ").append(selected.getStatus()).append("\n");
        details.append("Subtotal: ").append(formatter.format(selected.getSubtotal())).append("\n");
        details.append("Discount: ").append(formatter.format(selected.getDiscount())).append("\n");
        details.append("Total: ").append(formatter.format(selected.getTotal())).append("\n\n");
        details.append("Items:\n");
        for (TransactionItem item : items) {
            details.append("  - ").append(item.getProductName())
                .append(" x").append(item.getQuantity())
                .append(" @ ").append(formatter.format(item.getUnitPrice()))
                .append(" = ").append(formatter.format(item.getSubtotal())).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Transaction Details");
        alert.setHeaderText("Transaction #" + selected.getId());
        alert.setContentText(details.toString());
        alert.showAndWait();
    }

    @FXML
    public void handleMarkCompleted(ActionEvent event) {
        updateStatus("Completed");
    }

    @FXML
    public void handleMarkCancelled(ActionEvent event) {
        updateStatus("Cancelled");
    }

    private void updateStatus(String status) {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a transaction.");
            return;
        }
        if (status.equals(selected.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "No Change", "Transaction is already " + status + ".");
            return;
        }
        if (transactionDAO.updateStatus(selected.getId(), status)) {
            showAlert(Alert.AlertType.INFORMATION, "Updated", "Transaction marked as " + status + ".");
            loadTransactions();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update transaction status.");
        }
    }

    @FXML
    public void handleBack(ActionEvent event) {
        User user = JamuSyncApp.getCurrentUser();
        if (user != null && "owner".equalsIgnoreCase(user.getRole())) {
            JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/OwnerDashboard.fxml", "JamuSync - Owner Dashboard");
        } else {
            JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/StaffDashboard.fxml", "JamuSync - Staff Dashboard");
        }
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("JamuSync");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
