package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.ProductDAO;
import com.quantalabs.jamusync.dao.StockMovementDAO;
import com.quantalabs.jamusync.model.Product;
import com.quantalabs.jamusync.model.StockMovement;
import com.quantalabs.jamusync.model.User;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

public class InventoryController {

    @FXML private TableView<StockMovement> movementTable;
    @FXML private TableColumn<StockMovement, String> colDate;
    @FXML private TableColumn<StockMovement, String> colProduct;
    @FXML private TableColumn<StockMovement, String> colType;
    @FXML private TableColumn<StockMovement, Integer> colQty;
    @FXML private TableColumn<StockMovement, String> colUser;
    @FXML private TableColumn<StockMovement, String> colNote;

    @FXML private ComboBox<Product> productCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField quantityField;
    @FXML private TextArea noteField;
    @FXML private Label currentStockLabel;
    @FXML private Label quantityLabel;
    @FXML private Label messageLabel;
    @FXML private Button submitButton;

    private final ProductDAO productDAO = new ProductDAO();
    private final StockMovementDAO stockMovementDAO = new StockMovementDAO();

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList("Restock (In)", "Adjust Stock"));
        typeCombo.getSelectionModel().selectFirst();

        typeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if ("Adjust Stock".equals(selected)) {
                quantityLabel.setText("New Stock Level *");
                quantityField.setPromptText("Enter new stock level...");
            } else {
                quantityLabel.setText("Quantity to Add *");
                quantityField.setPromptText("Enter quantity to add...");
            }
        });

        productCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Product product) {
                return product == null ? "" : product.getName() + " (Stock: " + product.getStock() + ")";
            }

            @Override
            public Product fromString(String string) {
                return null;
            }
        });

        productCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, product) -> {
            if (product != null) {
                currentStockLabel.setText("Current Stock: " + product.getStock());
            } else {
                currentStockLabel.setText("Current Stock: -");
            }
        });

        setupTableColumns();
        loadProducts();
        loadMovements();
    }

    private void setupTableColumns() {
        colDate.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getCreatedAt() != null ? data.getValue().getCreatedAt() : ""
        ));
        colProduct.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colType.setCellValueFactory(data -> new SimpleStringProperty(formatType(data.getValue().getType())));
        colQty.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        colUser.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getPerformedByUsername() != null ? data.getValue().getPerformedByUsername() : ""
        ));
        colNote.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getNote() != null ? data.getValue().getNote() : ""
        ));
    }

    private String formatType(String type) {
        if (type == null) return "";
        return switch (type) {
            case "in" -> "Restock";
            case "out" -> "Stock Out";
            case "adjustment" -> "Adjustment";
            default -> type;
        };
    }

    private void loadProducts() {
        List<Product> products = productDAO.getAllActiveProducts();
        productCombo.setItems(FXCollections.observableArrayList(products));
    }

    private void loadMovements() {
        List<StockMovement> movements = stockMovementDAO.getAllMovements();
        movementTable.setItems(FXCollections.observableArrayList(movements));
    }

    @FXML
    public void handleSubmit(ActionEvent event) {
        User currentUser = JamuSyncApp.getCurrentUser();
        if (currentUser == null) {
            showError("You must be logged in to perform stock operations.");
            return;
        }

        Product selected = productCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a product.");
            return;
        }

        String typeSelection = typeCombo.getSelectionModel().getSelectedItem();
        if (typeSelection == null) {
            showError("Please select an operation type.");
            return;
        }

        int inputValue;
        try {
            inputValue = Integer.parseInt(quantityField.getText().trim());
            if (inputValue < 0) {
                showError("Quantity cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid whole number.");
            return;
        }

        Product fresh = productDAO.getProductById(selected.getId());
        if (fresh == null) {
            showError("Product not found.");
            return;
        }

        int currentStock = fresh.getStock();
        int newStock;
        String movementType;
        int movementQuantity;

        if ("Adjust Stock".equals(typeSelection)) {
            newStock = inputValue;
            movementType = "adjustment";
            movementQuantity = Math.abs(newStock - currentStock);
        } else {
            if (inputValue == 0) {
                showError("Restock quantity must be greater than zero.");
                return;
            }
            newStock = currentStock + inputValue;
            movementType = "in";
            movementQuantity = inputValue;
        }

        if (newStock < 0) {
            showError("Stock cannot be negative. Current stock: " + currentStock);
            return;
        }

        StockMovement movement = new StockMovement();
        movement.setProductId(fresh.getId());
        movement.setType(movementType);
        movement.setQuantity(movementQuantity);
        movement.setNote(noteField.getText().trim());
        movement.setPerformedBy(currentUser.getId());

        if (productDAO.updateStock(fresh.getId(), newStock) && stockMovementDAO.insertMovement(movement)) {
            showSuccess("Stock updated successfully. New stock: " + newStock);
            quantityField.clear();
            noteField.clear();
            loadProducts();
            Product updated = productDAO.getProductById(fresh.getId());
            if (updated != null) {
                productCombo.getSelectionModel().select(updated);
                currentStockLabel.setText("Current Stock: " + updated.getStock());
            }
            loadMovements();
        } else {
            showError("Failed to update stock.");
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

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("label-success");
        if (!messageLabel.getStyleClass().contains("label-error")) {
            messageLabel.getStyleClass().add("label-error");
        }
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("label-error");
        if (!messageLabel.getStyleClass().contains("label-success")) {
            messageLabel.getStyleClass().add("label-success");
        }
    }
}
