package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.ProductDAO;
import com.quantalabs.jamusync.model.Product;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

public class ProductController {

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, Integer> colThreshold;
    @FXML private TableColumn<Product, String> colActive;

    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField costField;
    @FXML private TextField stockField;
    @FXML private TextField thresholdField;
    @FXML private TextArea descriptionField;
    @FXML private TextArea ingredientsField;
    @FXML private TextArea healthBenefitsField;
    @FXML private TextField imagePathField;
    @FXML private ImageView imageView;
    @FXML private CheckBox activeCheckbox;
    @FXML private Label messageLabel;
    @FXML private Button saveButton;
    @FXML private Button deactivateButton;

    private final ProductDAO productDAO = new ProductDAO();
    private final DecimalFormat formatter = new DecimalFormat("Rp #,###");
    private int selectedProductId = 0;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadProducts();
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                populateForm(selected);
            }
        });
        handleClear(null);
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(formatter.format(data.getValue().getPrice())));
        colStock.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStock()).asObject());
        colThreshold.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getLowStockThreshold()).asObject());
        colActive.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Yes" : "No"));
    }

    private void loadProducts() {
        List<Product> products = productDAO.getAllActiveProducts();
        productTable.setItems(FXCollections.observableArrayList(products));
    }

    private void populateForm(Product product) {
        selectedProductId = product.getId();
        nameField.setText(product.getName());
        priceField.setText(String.valueOf((long) product.getPrice()));
        costField.setText(String.valueOf((long) product.getCost()));
        stockField.setText(String.valueOf(product.getStock()));
        thresholdField.setText(String.valueOf(product.getLowStockThreshold()));
        descriptionField.setText(product.getDescription() != null ? product.getDescription() : "");
        ingredientsField.setText(product.getIngredients() != null ? product.getIngredients() : "");
        healthBenefitsField.setText(product.getHealthBenefits() != null ? product.getHealthBenefits() : "");
        imagePathField.setText(product.getImagePath() != null ? product.getImagePath() : "");
        activeCheckbox.setSelected(product.isActive());
        deactivateButton.setDisable(false);
        saveButton.setText("Update Product");
        loadImagePreview(product.getImagePath());
        clearMessage();
    }

    @FXML
    public void handleSave(ActionEvent event) {
        String validationError = validateForm();
        if (validationError != null) {
            showError(validationError);
            return;
        }

        Product product = buildProductFromForm();
        boolean success;

        if (selectedProductId == 0) {
            success = productDAO.insertProduct(product);
            if (success) {
                showSuccess("Product added successfully.");
                handleClear(null);
            } else {
                showError("Failed to add product. Name may already exist.");
            }
        } else {
            product.setId(selectedProductId);
            success = productDAO.updateProduct(product);
            if (success) {
                showSuccess("Product updated successfully.");
            } else {
                showError("Failed to update product.");
            }
        }

        if (success) {
            loadProducts();
        }
    }

    @FXML
    public void handleDeactivate(ActionEvent event) {
        if (selectedProductId == 0) {
            showError("Select a product to deactivate.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("JamuSync - Confirm");
        confirm.setHeaderText("Deactivate Product");
        confirm.setContentText("Are you sure you want to deactivate \"" + nameField.getText().trim() + "\"?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                if (productDAO.deactivateProduct(selectedProductId)) {
                    showSuccess("Product deactivated successfully.");
                    handleClear(null);
                    loadProducts();
                } else {
                    showError("Failed to deactivate product.");
                }
            }
        });
    }

    @FXML
    public void handleClear(ActionEvent event) {
        selectedProductId = 0;
        nameField.clear();
        priceField.clear();
        costField.clear();
        stockField.clear();
        thresholdField.setText("10");
        descriptionField.clear();
        ingredientsField.clear();
        healthBenefitsField.clear();
        imagePathField.clear();
        imageView.setImage(null);
        activeCheckbox.setSelected(true);
        deactivateButton.setDisable(true);
        saveButton.setText("Save Product");
        productTable.getSelectionModel().clearSelection();
        clearMessage();
    }

    @FXML
    public void handleChooseImage(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Product Image");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File file = chooser.showOpenDialog(JamuSyncApp.getPrimaryStage());
        if (file != null) {
            imagePathField.setText(file.getAbsolutePath());
            loadImagePreview(file.getAbsolutePath());
        }
    }

    @FXML
    public void handleBack(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/OwnerDashboard.fxml", "JamuSync - Owner Dashboard");
    }

    private String validateForm() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            return "Product name is required.";
        }
        if (!productDAO.isNameUnique(name, selectedProductId)) {
            return "A product with this name already exists.";
        }

        Double price = parsePositiveDouble(priceField.getText().trim(), "Selling price");
        if (price == null) {
            return "Selling price must be a positive number.";
        }

        Double cost = parseNonNegativeDouble(costField.getText().trim(), "Production cost");
        if (cost == null) {
            return "Production cost must be zero or a positive number.";
        }

        Integer stock = parseNonNegativeInt(stockField.getText().trim(), "Stock");
        if (stock == null) {
            return "Stock must be zero or a positive whole number.";
        }

        Integer threshold = parseNonNegativeInt(thresholdField.getText().trim(), "Low stock limit");
        if (threshold == null) {
            return "Low stock limit must be zero or a positive whole number.";
        }

        return null;
    }

    private Product buildProductFromForm() {
        Product product = new Product();
        product.setName(nameField.getText().trim());
        product.setPrice(Double.parseDouble(priceField.getText().trim()));
        product.setCost(Double.parseDouble(costField.getText().trim()));
        product.setStock(Integer.parseInt(stockField.getText().trim()));
        product.setLowStockThreshold(Integer.parseInt(thresholdField.getText().trim()));
        product.setDescription(descriptionField.getText().trim());
        product.setIngredients(ingredientsField.getText().trim());
        product.setHealthBenefits(healthBenefitsField.getText().trim());
        String imagePath = imagePathField.getText().trim();
        product.setImagePath(imagePath.isEmpty() ? null : imagePath);
        product.setActive(activeCheckbox.isSelected());
        return product;
    }

    private Double parsePositiveDouble(String value, String fieldName) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed <= 0) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseNonNegativeDouble(String value, String fieldName) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseNonNegativeInt(String value, String fieldName) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void loadImagePreview(String path) {
        if (path == null || path.isEmpty()) {
            imageView.setImage(null);
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            imageView.setImage(new Image(file.toURI().toString(), true));
        } else {
            imageView.setImage(null);
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

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.getStyleClass().removeAll("label-error");
        if (!messageLabel.getStyleClass().contains("label-success")) {
            messageLabel.getStyleClass().add("label-success");
        }
    }
}
