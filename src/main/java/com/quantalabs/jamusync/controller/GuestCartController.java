package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.ProductDAO;
import com.quantalabs.jamusync.dao.VoucherDAO;
import com.quantalabs.jamusync.model.Product;
import com.quantalabs.jamusync.model.Transaction;
import com.quantalabs.jamusync.model.TransactionItem;
import com.quantalabs.jamusync.model.Voucher;
import com.quantalabs.jamusync.service.TransactionService;
import com.quantalabs.jamusync.util.CartManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

// The guest shopping cart screen. Shows the items, a voucher box,
// buyer details, and a "Place Order" button that creates a real
// Pending transaction (a WhatsApp order) through TransactionService.
public class GuestCartController {

    // --- Things wired up from the FXML ---
    @FXML private VBox itemsContainer;
    @FXML private TextField voucherField;
    @FXML private Label voucherMsgLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label discountLabel;
    @FXML private Label totalLabel;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private Button placeOrderButton;

    private final ProductDAO productDAO = new ProductDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final TransactionService transactionService = new TransactionService();
    private final DecimalFormat formatter = new DecimalFormat("Rp #,###");

    // The voucher the user applied, if any.
    private Voucher appliedVoucher;

    // Runs automatically when the screen opens.
    @FXML
    public void initialize() {
        rebuildItems();
        updateTotals();

        // Turn the Place Order button on/off as the name/phone fields change.
        nameField.textProperty().addListener((obs, oldText, newText) -> updatePlaceOrderState());
        phoneField.textProperty().addListener((obs, oldText, newText) -> updatePlaceOrderState());
        updatePlaceOrderState();
    }

    // Rebuild the list of cart item rows.
    private void rebuildItems() {
        itemsContainer.getChildren().clear();

        // Empty cart: show a friendly message and a browse button.
        if (CartManager.getItems().isEmpty()) {
            Label empty = new Label("Your cart is empty. Browse our herbs and add some jamu to get started!");
            empty.getStyleClass().add("label-muted");
            empty.setWrapText(true);

            Button browse = new Button("Start Browsing");
            browse.setOnAction(e -> handleStartBrowsing());

            VBox emptyBox = new VBox(12, empty, browse);
            itemsContainer.getChildren().add(emptyBox);
            return;
        }

        // One row card per cart item.
        for (CartManager.CartItem item : CartManager.getItems()) {
            itemsContainer.getChildren().add(createItemRow(item));
        }
    }

    // Build one cart item row.
    private HBox createItemRow(CartManager.CartItem item) {
        HBox row = new HBox(15);
        row.getStyleClass().add("match-card");
        row.setAlignment(Pos.CENTER_LEFT);

        // Left: product name and unit price.
        VBox info = new VBox(4);
        Label name = new Label(item.getProductName());
        name.getStyleClass().add("detail-name");
        Label unit = new Label(formatter.format(item.getPrice()) + " each");
        unit.getStyleClass().add("label-muted");
        info.getChildren().addAll(name, unit);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Middle: minus / quantity / plus.
        Button minus = new Button("-");
        minus.getStyleClass().add("button-small");
        minus.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                rebuildItems();
                updateTotals();
                updatePlaceOrderState();
            }
        });

        Label qty = new Label(String.valueOf(item.getQuantity()));
        qty.getStyleClass().add("text-dark");
        qty.setMinWidth(30);
        qty.setAlignment(Pos.CENTER);

        Button plus = new Button("+");
        plus.getStyleClass().add("button-small");
        plus.setOnAction(e -> {
            item.setQuantity(item.getQuantity() + 1);
            rebuildItems();
            updateTotals();
            updatePlaceOrderState();
        });

        HBox qtyBox = new HBox(8, minus, qty, plus);
        qtyBox.setAlignment(Pos.CENTER);

        // Subtotal for this line.
        Label subtotal = new Label(formatter.format(item.getSubtotal()));
        subtotal.getStyleClass().add("text-accent");
        subtotal.setMinWidth(90);
        subtotal.setAlignment(Pos.CENTER_RIGHT);

        // Remove button (red).
        Button remove = new Button("Remove");
        remove.getStyleClass().add("button-danger");
        remove.getStyleClass().add("button-small");
        remove.setOnAction(e -> {
            int index = CartManager.getItems().indexOf(item);
            CartManager.removeItem(index);
            rebuildItems();
            updateTotals();
            updatePlaceOrderState();
        });

        row.getChildren().addAll(info, spacer, qtyBox, subtotal, remove);
        return row;
    }

    // Recalculate subtotal, discount and total labels.
    private void updateTotals() {
        double subtotal = CartManager.getTotal();
        double discount = (appliedVoucher != null) ? appliedVoucher.calculateDiscount(subtotal) : 0;
        double total = subtotal - discount;

        subtotalLabel.setText("Subtotal: " + formatter.format(subtotal));
        discountLabel.setText("Discount: " + formatter.format(discount));
        totalLabel.setText("Total: " + formatter.format(total));
    }

    // Enable Place Order only when the cart has items and name/phone are filled.
    private void updatePlaceOrderState() {
        boolean cartEmpty = CartManager.getItems().isEmpty();
        boolean noName = nameField.getText().trim().isEmpty();
        boolean noPhone = phoneField.getText().trim().isEmpty();
        placeOrderButton.setDisable(cartEmpty || noName || noPhone);
    }

    // Apply a voucher code.
    @FXML
    public void handleApplyVoucher() {
        String code = voucherField.getText().trim();
        if (code.isEmpty()) {
            appliedVoucher = null;
            setVoucherMessage("Please enter a voucher code.", false);
            updateTotals();
            return;
        }

        Voucher voucher = voucherDAO.getByCode(code);
        if (voucher == null || !voucherDAO.isValid(voucher)) {
            appliedVoucher = null;
            setVoucherMessage("Invalid or expired voucher", false);
            updateTotals();
            return;
        }

        appliedVoucher = voucher;
        String discountText = "percentage".equalsIgnoreCase(voucher.getDiscountType())
            ? voucher.getDiscountValue() + "% off"
            : formatter.format(voucher.getDiscountValue()) + " off";
        setVoucherMessage("Voucher applied: " + voucher.getCode() + " (" + discountText + ")", true);
        updateTotals();
    }

    // Show the voucher message in green (good) or red (bad).
    private void setVoucherMessage(String message, boolean good) {
        voucherMsgLabel.setText(message);
        voucherMsgLabel.getStyleClass().removeAll("label-success", "label-error");
        voucherMsgLabel.getStyleClass().add(good ? "label-success" : "label-error");
    }

    // Place the order: turn the cart into a real Pending transaction.
    @FXML
    public void handlePlaceOrder() {
        // Match each cart item to a real product in the catalog.
        List<TransactionItem> orderItems = new ArrayList<>();
        List<String> notAvailable = new ArrayList<>();

        for (CartManager.CartItem item : CartManager.getItems()) {
            Product product = resolveProduct(item.getProductName());
            if (product == null) {
                notAvailable.add(item.getProductName());
            } else {
                orderItems.add(new TransactionItem(
                    product.getId(), item.getQuantity(), item.getPrice(), item.getProductName()));
            }
        }

        // Some cart items are showcase-only herbs not sold in the catalog yet.
        if (!notAvailable.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Some items unavailable",
                "These items are showcase herbs not available for online order yet:\n\n"
                + String.join(", ", notAvailable)
                + "\n\nPlease remove them to place your order.");
            return;
        }
        if (orderItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty order", "Your cart has no items to order.");
            return;
        }

        // Work out the money.
        double subtotal = CartManager.getTotal();
        double discount = (appliedVoucher != null) ? appliedVoucher.calculateDiscount(subtotal) : 0;
        double total = subtotal - discount;

        // Build the transaction. Guests have no account, so we record it under
        // the admin user (id 1) as a Pending WhatsApp order.
        Transaction transaction = new Transaction();
        transaction.setOrderType("whatsapp");
        transaction.setBuyerName(nameField.getText().trim());
        transaction.setSubtotal(subtotal);
        transaction.setDiscount(discount);
        transaction.setTotal(total);
        transaction.setStatus("Pending");
        transaction.setRecordedBy(1);
        if (appliedVoucher != null) {
            transaction.setVoucherId(appliedVoucher.getId());
        }

        TransactionService.SaleResult result = transactionService.completeSale(transaction, orderItems);

        if (result.isSuccess()) {
            String orderNumber = String.format("TRX-%05d", result.getTransactionId());
            showAlert(Alert.AlertType.INFORMATION, "Order placed!",
                "Order placed! Order number " + orderNumber + ".\n\n"
                + "We will contact you on WhatsApp to confirm payment and delivery.\n\n"
                + "Terima kasih!");

            // Clear everything after a successful order.
            CartManager.clear();
            appliedVoucher = null;
            voucherField.clear();
            voucherMsgLabel.setText("");
            nameField.clear();
            phoneField.clear();
            rebuildItems();
            updateTotals();
            updatePlaceOrderState();
        } else {
            showAlert(Alert.AlertType.ERROR, "Order failed", result.getMessage());
        }
    }

    // Find a catalog product that matches a cart item name.
    private Product resolveProduct(String name) {
        List<Product> products = productDAO.getAllActiveProducts();
        String lower = name.toLowerCase();

        // First try an exact name match.
        for (Product p : products) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }

        // Then try matching on the first word (e.g. "Kunyit" -> "Kunyit Asam").
        String firstWord = lower.split(" ")[0];
        for (Product p : products) {
            String productLower = p.getName().toLowerCase();
            if (productLower.contains(firstWord)) {
                return p;
            }
            String productFirstWord = productLower.split(" ")[0];
            if (lower.contains(productFirstWord)) {
                return p;
            }
        }
        return null;
    }

    // Simple pop-up helper.
    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("JamuSync");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ---------------- Navigation ----------------

    private void handleStartBrowsing() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/HerbMatrix.fxml", "JamuSync - Herb Matrix");
    }

    @FXML
    public void handleContinueBrowsing() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/HerbMatrix.fxml", "JamuSync - Herb Matrix");
    }

    @FXML
    public void handleNavHerb() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/HerbMatrix.fxml", "JamuSync - Herb Matrix");
    }

    @FXML
    public void handleNavMixer() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/JamuMixer.fxml", "JamuSync - Jamu Mixer");
    }

    @FXML
    public void handleNavSymptom() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/SymptomSync.fxml", "JamuSync - Symptom Sync");
    }

    @FXML
    public void handleBackToLogin() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/Login.fxml", "JamuSync - Sign In");
    }
}
