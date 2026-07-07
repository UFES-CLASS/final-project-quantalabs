package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.model.Product;
import com.quantalabs.jamusync.service.RecommendationService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CatalogController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private FlowPane productGrid;
    @FXML private ImageView detailImageView;
    @FXML private Label detailNameLabel;
    @FXML private Label detailPriceLabel;
    @FXML private Label detailDescriptionLabel;
    @FXML private Label detailIngredientsLabel;
    @FXML private Label detailBenefitsLabel;
    @FXML private TextArea chatHistoryArea;
    @FXML private TextField chatInputField;
    @FXML private Label recommendationLabel;

    private final RecommendationService recommendationService = new RecommendationService();
    private final ChatbotController chatbotController = new ChatbotController();
    private final DecimalFormat formatter = new DecimalFormat("Rp #,###");

    @FXML
    public void initialize() {
        filterCombo.getItems().add("All");
        loadBenefitFilters();
        filterCombo.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshProducts());
        filterCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> refreshProducts());

        chatInputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSendChat(null);
            }
        });

        refreshProducts();
        chatHistoryArea.setText("Welcome to JamuSync! Ask me about jamu health benefits or request product recommendations.\n");
    }

    private void loadBenefitFilters() {
        Set<String> benefits = new HashSet<>();
        for (Product product : recommendationService.filterByBenefit(null)) {
            if (product.getHealthBenefits() != null && !product.getHealthBenefits().isBlank()) {
                for (String part : product.getHealthBenefits().split("[,;]")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        benefits.add(trimmed);
                    }
                }
            }
        }
        filterCombo.getItems().addAll(benefits.stream().sorted().toList());
    }

    private void refreshProducts() {
        String query = searchField.getText();
        String filter = filterCombo.getSelectionModel().getSelectedItem();
        List<Product> products = recommendationService.searchProducts(query, filter);

        productGrid.getChildren().clear();
        for (Product product : products) {
            productGrid.getChildren().add(createProductCard(product));
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPrefWidth(180);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("card-interactive");

        ImageView imageView = new ImageView();
        imageView.setFitHeight(80);
        imageView.setFitWidth(120);
        imageView.setPreserveRatio(true);
        loadProductImage(imageView, product.getImagePath());

        Label nameLabel = new Label(product.getName());
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("detail-name");
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-text-alignment: center;");

        Label priceLabel = new Label(formatter.format(product.getPrice()));
        priceLabel.getStyleClass().add("detail-price");

        Label stockLabel = new Label("In stock: " + product.getStock());
        stockLabel.getStyleClass().add("label-muted");

        card.getChildren().addAll(imageView, nameLabel, priceLabel, stockLabel);
        card.setOnMouseClicked(e -> showProductDetails(product));
        return card;
    }

    private void showProductDetails(Product product) {
        detailNameLabel.setText(product.getName());
        detailPriceLabel.setText(formatter.format(product.getPrice()) + "  |  Stock: " + product.getStock());
        detailDescriptionLabel.setText(
            product.getDescription() != null && !product.getDescription().isEmpty()
                ? product.getDescription() : "No description available."
        );
        detailIngredientsLabel.setText(
            product.getIngredients() != null && !product.getIngredients().isEmpty()
                ? product.getIngredients() : "Not specified."
        );
        detailBenefitsLabel.setText(
            product.getHealthBenefits() != null && !product.getHealthBenefits().isEmpty()
                ? product.getHealthBenefits() : "Not specified."
        );
        loadProductImage(detailImageView, product.getImagePath());
    }

    private void loadProductImage(ImageView imageView, String path) {
        if (path != null && !path.isEmpty()) {
            File file = new File(path);
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString(), true));
                return;
            }
        }
        imageView.setImage(null);
    }

    @FXML
    public void handleSendChat(ActionEvent event) {
        String message = chatInputField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        chatHistoryArea.appendText("\nYou: " + message + "\n");
        chatInputField.clear();
        chatHistoryArea.appendText("Jamu Assistant: Thinking...\n");

        new Thread(() -> {
            String response = chatbotController.sendChatMessage(message);
            Platform.runLater(() -> {
                String text = chatHistoryArea.getText();
                if (text.endsWith("Jamu Assistant: Thinking...\n")) {
                    chatHistoryArea.setText(text.substring(0, text.length() - "Jamu Assistant: Thinking...\n".length()));
                }
                chatHistoryArea.appendText("Jamu Assistant: " + response + "\n");
            });
        }).start();
    }

    @FXML
    public void handleGetRecommendations(ActionEvent event) {
        String query = chatInputField.getText().trim();
        if (query.isEmpty()) {
            query = searchField.getText().trim();
        }
        recommendationLabel.setText("Generating recommendations...");
        final String finalQuery = query;

        new Thread(() -> {
            String response = chatbotController.getRecommendations(finalQuery);
            List<Product> localRecs = recommendationService.recommendByKeywords(finalQuery);
            String localText = localRecs.isEmpty() ? "" :
                "\n\nLocal matches: " + localRecs.stream().map(Product::getName).reduce((a, b) -> a + ", " + b).orElse("");

            Platform.runLater(() -> {
                recommendationLabel.setText(response + localText);
                chatHistoryArea.appendText("\nRecommendations: " + response + "\n");
            });
        }).start();
    }

    @FXML
    public void handleBackToLogin(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/Login.fxml", "JamuSync - Sign In");
    }
}
