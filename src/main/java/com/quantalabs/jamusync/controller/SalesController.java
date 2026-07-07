package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.ProductDAO;
import com.quantalabs.jamusync.dao.VoucherDAO;
import com.quantalabs.jamusync.model.Product;
import com.quantalabs.jamusync.model.Transaction;
import com.quantalabs.jamusync.model.TransactionItem;
import com.quantalabs.jamusync.model.User;
import com.quantalabs.jamusync.model.Voucher;
import com.quantalabs.jamusync.service.TransactionService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SalesController {

    @FXML private ComboBox<String> orderTypeCombo;
    @FXML private TextField buyerNameField;
    @FXML private ComboBox<Product> productCombo;
    @FXML private TextField quantityField;
    @FXML private TableView<TransactionItem> cartTable;
    @FXML private TableColumn<TransactionItem, String> colProduct;
    @FXML private TableColumn<TransactionItem, String> colUnitPrice;
    @FXML private TableColumn<TransactionItem, Integer> colQty;
    @FXML private TableColumn<TransactionItem, String> colSubtotal;
    @FXML private TableColumn<TransactionItem, Void> colAction;
    @FXML private TextField voucherCodeField;
    @FXML private Label voucherInfoLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label discountLabel;
    @FXML private Label totalLabel;
    @FXML private Label messageLabel;
    @FXML private Button completeButton;

    private final ProductDAO productDAO = new ProductDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final TransactionService transactionService = new TransactionService();
    private final ObservableList<TransactionItem> cartItems = FXCollections.observableArrayList();
    private final DecimalFormat formatter = new DecimalFormat("Rp #,###");

    private Voucher appliedVoucher;

    @FXML
    public void initialize() {
        orderTypeCombo.setItems(FXCollections.observableArrayList("walk-in", "whatsapp"));
        orderTypeCombo.getSelectionModel().selectFirst();

        productCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Product product) {
                if (product == null) return "";
                return product.getName() + " - " + formatter.format(product.getPrice()) + " (Stock: " + product.getStock() + ")";
            }

            @Override
            public Product fromString(String string) {
                return null;
            }
        });

        setupCartTable();
        loadProducts();
        updateTotals();
    }

    private void setupCartTable() {
        colProduct.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colUnitPrice.setCellValueFactory(data -> new SimpleStringProperty(formatter.format(data.getValue().getUnitPrice())));
        colQty.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        colSubtotal.setCellValueFactory(data -> new SimpleStringProperty(formatter.format(data.getValue().getSubtotal())));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");
            {
                removeBtn.getStyleClass().add("button-small");
                removeBtn.setOnAction(e -> {
                    TransactionItem item = getTableView().getItems().get(getIndex());
                    cartItems.remove(item);
                    updateTotals();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        cartTable.setItems(cartItems);
    }

    private void loadProducts() {
        List<Product> products = productDAO.getAllActiveProducts();
        productCombo.setItems(FXCollections.observableArrayList(products));
    }

    @FXML
    public void handleAddToCart(ActionEvent event) {
        Product selected = productCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a product.");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) {
                showError("Quantity must be greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid quantity.");
            return;
        }

        Product fresh = productDAO.getProductById(selected.getId());
        if (fresh == null) {
            showError("Product not found.");
            return;
        }

        int existingQty = cartItems.stream()
            .filter(i -> i.getProductId() == fresh.getId())
            .mapToInt(TransactionItem::getQuantity)
            .sum();

        if (existingQty + quantity > fresh.getStock()) {
            showError("Insufficient stock. Available: " + fresh.getStock() + ", in cart: " + existingQty);
            return;
        }

        for (TransactionItem item : cartItems) {
            if (item.getProductId() == fresh.getId()) {
                item.setQuantity(item.getQuantity() + quantity);
                item.recalculateSubtotal();
                cartTable.refresh();
                quantityField.clear();
                updateTotals();
                clearMessage();
                return;
            }
        }

        cartItems.add(new TransactionItem(fresh.getId(), quantity, fresh.getPrice(), fresh.getName()));
        quantityField.clear();
        updateTotals();
        clearMessage();
    }

    @FXML
    public void handleApplyVoucher(ActionEvent event) {
        String code = voucherCodeField.getText().trim();
        if (code.isEmpty()) {
            appliedVoucher = null;
            voucherInfoLabel.setText("");
            updateTotals();
            return;
        }

        Voucher voucher = voucherDAO.getByCode(code);
        if (voucher == null) {
            appliedVoucher = null;
            voucherInfoLabel.setText("Voucher not found.");
            updateTotals();
            return;
        }
        if (!voucherDAO.isValid(voucher)) {
            appliedVoucher = null;
            voucherInfoLabel.setText("Voucher is expired, inactive, or usage limit reached.");
            updateTotals();
            return;
        }

        appliedVoucher = voucher;
        String typeLabel = "percentage".equals(voucher.getDiscountType()) ?
            voucher.getDiscountValue() + "% off" : formatter.format(voucher.getDiscountValue()) + " off";
        voucherInfoLabel.setText("Applied: " + voucher.getCode() + " (" + typeLabel + ")");
        updateTotals();
        clearMessage();
    }

    @FXML
    public void handleCompleteSale(ActionEvent event) {
        User currentUser = JamuSyncApp.getCurrentUser();
        if (currentUser == null) {
            showError("You must be logged in to complete a sale.");
            return;
        }
        if (cartItems.isEmpty()) {
            showError("Cart is empty.");
            return;
        }

        double subtotal = calculateSubtotal();
        double discount = calculateDiscount(subtotal);
        double total = subtotal - discount;

        Transaction transaction = new Transaction();
        transaction.setOrderType(orderTypeCombo.getSelectionModel().getSelectedItem());
        String buyer = buyerNameField.getText().trim();
        transaction.setBuyerName(buyer.isEmpty() ? null : buyer);
        transaction.setSubtotal(subtotal);
        transaction.setDiscount(discount);
        transaction.setTotal(total);
        transaction.setStatus("Completed");
        transaction.setRecordedBy(currentUser.getId());
        if (appliedVoucher != null) {
            transaction.setVoucherId(appliedVoucher.getId());
        }

        List<TransactionItem> items = new ArrayList<>(cartItems);
        TransactionService.SaleResult result = transactionService.completeSale(transaction, items);

        if (result.isSuccess()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("JamuSync - Sale Complete");
            alert.setHeaderText("Transaction #" + result.getTransactionId());
            alert.setContentText("Sale completed successfully.\nTotal: " + formatter.format(total));
            alert.showAndWait();

            handleClearCart(null);
            loadProducts();
            showSuccess("Sale recorded successfully.");
        } else {
            showError(result.getMessage());
        }
    }

    @FXML
    public void handleClearCart(ActionEvent event) {
        cartItems.clear();
        buyerNameField.clear();
        voucherCodeField.clear();
        voucherInfoLabel.setText("");
        appliedVoucher = null;
        quantityField.clear();
        orderTypeCombo.getSelectionModel().selectFirst();
        updateTotals();
        clearMessage();
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

    private void updateTotals() {
        double subtotal = calculateSubtotal();
        double discount = calculateDiscount(subtotal);
        double total = subtotal - discount;

        subtotalLabel.setText("Subtotal: " + formatter.format(subtotal));
        discountLabel.setText("Discount: " + formatter.format(discount));
        totalLabel.setText("Total: " + formatter.format(total));
    }

    private double calculateSubtotal() {
        return cartItems.stream().mapToDouble(TransactionItem::getSubtotal).sum();
    }

    private double calculateDiscount(double subtotal) {
        if (appliedVoucher == null || subtotal <= 0) {
            return 0;
        }
        return appliedVoucher.calculateDiscount(subtotal);
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

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.getStyleClass().removeAll("label-error");
        if (!messageLabel.getStyleClass().contains("label-success")) {
            messageLabel.getStyleClass().add("label-success");
        }
    }
}
