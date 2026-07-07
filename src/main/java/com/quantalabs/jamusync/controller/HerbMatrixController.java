package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.ProductDAO;
import com.quantalabs.jamusync.model.Product;
import com.quantalabs.jamusync.util.CartManager;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

// This screen shows a grid of the real products from the database.
// It is a guest screen, so there is no login needed.
public class HerbMatrixController {

    // The FlowPane from the FXML where we put all the product cards.
    @FXML
    private FlowPane herbGrid;

    // The cart button in the top nav bar (shows how many items are in the cart).
    @FXML
    private Button cartButton;

    // Used to load the real products from the database.
    private final ProductDAO productDAO = new ProductDAO();

    // For showing prices like "Rp 15,000".
    private final DecimalFormat formatter = new DecimalFormat("Rp #,###");

    // This runs automatically when the screen opens.
    @FXML
    public void initialize() {
        // Get the real products from the database.
        List<Product> products = productDAO.getAllActiveProducts();

        if (products.isEmpty()) {
            // No products yet - show a friendly message.
            Label empty = new Label("No products available. Ask the admin to add products first.");
            empty.getStyleClass().add("label-muted");
            empty.setWrapText(true);
            herbGrid.getChildren().add(empty);
        } else {
            // Make a card for every product and add it to the grid.
            for (Product product : products) {
                herbGrid.getChildren().add(createProductCard(product));
            }
        }

        updateCartButton();
    }

    // Build one product card as a VBox.
    private VBox createProductCard(Product product) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.getStyleClass().add("card-interactive");
        card.setPrefWidth(250);
        card.setPadding(new Insets(20));

        // --- Image area (real image or a letter placeholder) ---
        HBox imageBox = new HBox();
        imageBox.getStyleClass().add("image-preview-box");
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPrefHeight(120);

        String imagePath = product.getImagePath();
        if (imagePath != null && !imagePath.isEmpty() && new File(imagePath).exists()) {
            // Load the product image from the file path in the database.
            ImageView imageView = new ImageView(new Image(new File(imagePath).toURI().toString()));
            imageView.setFitHeight(110);
            imageView.setFitWidth(200);
            imageView.setPreserveRatio(true);
            imageBox.getChildren().add(imageView);
        } else {
            // No image - show a colored circle with the first letter of the name.
            String firstLetter = product.getName().substring(0, 1).toUpperCase();
            Label avatar = new Label(firstLetter);
            avatar.setStyle(
                "-fx-min-width: 70px; -fx-min-height: 70px; -fx-max-width: 70px; -fx-max-height: 70px; "
                + "-fx-background-color: #8B5A3C; -fx-background-radius: 35px; "
                + "-fx-text-fill: #FAF6EE; -fx-font-size: 28px; -fx-font-weight: bold; -fx-alignment: center;"
            );
            imageBox.getChildren().add(avatar);
        }
        card.getChildren().add(imageBox);

        // --- Product name (large, bold) ---
        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("detail-name");
        nameLabel.setWrapText(true);
        card.getChildren().add(nameLabel);

        // --- Price in Rupiah ---
        Label priceLabel = new Label(formatter.format(product.getPrice()));
        priceLabel.getStyleClass().add("detail-price");
        card.getChildren().add(priceLabel);

        // --- Description (only if the product has one) ---
        if (hasText(product.getDescription())) {
            Label descLabel = new Label(product.getDescription());
            descLabel.getStyleClass().add("label-muted");
            descLabel.setWrapText(true);
            card.getChildren().add(descLabel);
        }

        // --- Health Benefits (only if the product has one) ---
        if (hasText(product.getHealthBenefits())) {
            Label benefitsTitle = new Label("Health Benefits");
            benefitsTitle.getStyleClass().add("label-form");
            Label benefitsText = new Label(product.getHealthBenefits());
            benefitsText.getStyleClass().add("text-dark");
            benefitsText.setWrapText(true);
            card.getChildren().addAll(benefitsTitle, benefitsText);
        }

        // --- Ingredients (only if the product has one) ---
        if (hasText(product.getIngredients())) {
            Label ingredientsTitle = new Label("Ingredients");
            ingredientsTitle.getStyleClass().add("label-form");
            Label ingredientsText = new Label(product.getIngredients());
            ingredientsText.getStyleClass().add("text-dark");
            ingredientsText.setWrapText(true);
            card.getChildren().addAll(ingredientsTitle, ingredientsText);
        }

        // --- "Add to Cart" button and an "Added!" message ---
        Label addedLabel = new Label("");
        addedLabel.getStyleClass().add("label-success");

        Button addButton = new Button("Add to Cart");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> {
            CartManager.addItem(product.getName(), product.getPrice(), 1);
            updateCartButton();
            showAddedMessage(addedLabel);
        });

        card.getChildren().addAll(addButton, addedLabel);
        return card;
    }

    // Helper: true if the text is not null and not empty.
    private boolean hasText(String text) {
        return text != null && !text.isEmpty();
    }

    // Show "Added!" for 2 seconds then clear it.
    private void showAddedMessage(Label label) {
        label.setText("Added!");
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> label.setText(""));
        pause.play();
    }

    // Update the cart button text with the current number of items.
    private void updateCartButton() {
        if (cartButton != null) {
            cartButton.setText("Cart (" + CartManager.getItemCount() + ")");
        }
    }

    // Go to the cart screen.
    @FXML
    public void handleNavCart() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/GuestCart.fxml", "JamuSync - My Cart");
    }

    // Go to the Jamu Mixer screen.
    @FXML
    public void handleNavMixer() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/JamuMixer.fxml", "JamuSync - Jamu Mixer");
    }

    // Go to the Symptom Sync screen.
    @FXML
    public void handleNavSymptom() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/SymptomSync.fxml", "JamuSync - Symptom Sync");
    }

    // Go back to the login screen.
    @FXML
    public void handleBackToLogin() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/Login.fxml", "JamuSync - Sign In");
    }
}
