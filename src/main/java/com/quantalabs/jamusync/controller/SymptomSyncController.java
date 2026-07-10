package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.ProductDAO;
import com.quantalabs.jamusync.model.Product;
import com.quantalabs.jamusync.service.RecommendationService;
import com.quantalabs.jamusync.util.CartManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

// This screen lets a guest pick how they feel (symptoms) and also chat
// with the Jamu Assistant. Both give product recommendations.
public class SymptomSyncController {

    // The 12 symptom options. Each row holds:
    // 0 = symptom name (shown on the pill)
    // 1 = keywords we look for in a product's text (comma separated).
    private final String[][] symptoms = {
        {"Fatigued",      "energy,energi,fatigue,tired,kencur,beras,jahe,ginger"},
        {"Common Cold",   "cold,immun,jahe,ginger,kunyit,turmeric"},
        {"Stomach Ache",  "stomach,digest,kunyit,temulawak,turmeric"},
        {"Headache",      "head,temulawak,kunyit,turmeric"},
        {"Nausea",        "nausea,kencur,beras,jahe,ginger"},
        {"Joint Pain",    "joint,pain,inflam,temulawak,kunyit,turmeric"},
        {"Skin Issues",   "skin,inflam,kunyit,turmeric"},
        {"Anxiety",       "anxiety,calm,relax,kencur,beras,jahe"},
        {"Poor Sleep",    "sleep,relax,temulawak"},
        {"Low Immunity",  "immun,jahe,ginger,kunyit,turmeric"},
        {"Bloating",      "bloat,digest,kunyit,temulawak,turmeric"},
        {"Inflammation",  "inflam,kunyit,turmeric,temulawak"}
    };

    // We keep the pills in a list so we can read them later.
    private final List<ToggleButton> pills = new ArrayList<>();

    // Used to load the real products from the database.
    private final ProductDAO productDAO = new ProductDAO();

    // The existing chatbot logic (Ollama + recommendations). We just call it.
    private final ChatbotController chatbot = new ChatbotController();
    private final RecommendationService recommendationService = new RecommendationService();

    // Products suggested by the last chat question (shown in the results too).
    private List<Product> chatRecommendations = new ArrayList<>();

    // --- Things wired up from the FXML ---
    @FXML private FlowPane symptomFlow;
    @FXML private Label matchCountLabel;
    @FXML private VBox resultsContainer;

    // Chat parts at the bottom of the screen.
    @FXML private VBox chatHistoryBox;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField chatInput;
    @FXML private Button sendButton;

    // The cart button in the top nav bar.
    @FXML private Button cartButton;

    // Runs automatically when the screen opens.
    @FXML
    public void initialize() {
        // Make a pill for each symptom.
        for (String[] symptom : symptoms) {
            ToggleButton pill = new ToggleButton(symptom[0]);
            pill.getStyleClass().add("symptom-pill");
            pill.setOnAction(e -> rebuildResults());
            pills.add(pill);
            symptomFlow.getChildren().add(pill);
        }

        // Add a friendly greeting from the assistant.
        addBotBubble("Halo! Ask me anything about jamu and your health. / "
                + "Tanyakan apa saja tentang jamu dan kesehatan Anda.");

        // Show the starting (empty) results.
        rebuildResults();
        updateCartButton();
    }

    // A small holder for a product and how well it matched.
    private static class Match {
        Product product;
        int score;   // how many selected symptoms matched
        int percent; // score as a percentage of selected symptoms
    }

    // Rebuild the results area from BOTH sources:
    // the symptom matches and the last chat recommendations.
    private void rebuildResults() {
        resultsContainer.getChildren().clear();

        // --- Part 1: symptom matches ---
        List<String> selectedKeywords = new ArrayList<>();
        int selectedCount = 0;
        for (int i = 0; i < pills.size(); i++) {
            ToggleButton pill = pills.get(i);
            if (pill.isSelected()) {
                if (!pill.getStyleClass().contains("symptom-pill-selected")) {
                    pill.getStyleClass().add("symptom-pill-selected");
                }
                selectedKeywords.add(symptoms[i][1]);
                selectedCount++;
            } else {
                pill.getStyleClass().remove("symptom-pill-selected");
            }
        }

        List<Product> products = productDAO.getAllActiveProducts();
        List<Match> matches = new ArrayList<>();
        for (Product product : products) {
            String haystack = (safe(product.getName()) + " "
                    + safe(product.getDescription()) + " "
                    + safe(product.getHealthBenefits()) + " "
                    + safe(product.getIngredients())).toLowerCase();

            int score = 0;
            for (String keywordList : selectedKeywords) {
                if (matchesAnyKeyword(haystack, keywordList)) {
                    score++;
                }
            }
            if (score > 0) {
                Match match = new Match();
                match.product = product;
                match.score = score;
                match.percent = (int) Math.round(score * 100.0 / selectedCount);
                matches.add(match);
            }
        }

        // Sort so the best score is first (simple selection sort).
        for (int i = 0; i < matches.size(); i++) {
            int best = i;
            for (int j = i + 1; j < matches.size(); j++) {
                if (matches.get(j).score > matches.get(best).score) {
                    best = j;
                }
            }
            Match temp = matches.get(i);
            matches.set(i, matches.get(best));
            matches.set(best, temp);
        }

        matchCountLabel.setText(matches.size() + " remedies matched");

        // Show the top 3 symptom matches (first one is the best match).
        int shown = Math.min(3, matches.size());
        for (int i = 0; i < shown; i++) {
            boolean isBest = (i == 0);
            Match m = matches.get(i);
            resultsContainer.getChildren().add(
                createProductCard(m.product, isBest ? "BEST MATCH" : null, m.percent));
        }

        // --- Part 2: chat recommendations (same results area) ---
        if (!chatRecommendations.isEmpty()) {
            Label header = new Label("FROM YOUR QUESTION");
            header.getStyleClass().add("guest-eyebrow");
            resultsContainer.getChildren().add(header);

            int chatShown = Math.min(3, chatRecommendations.size());
            for (int i = 0; i < chatShown; i++) {
                // Chat cards have no match percentage, so we pass -1.
                resultsContainer.getChildren().add(
                    createProductCard(chatRecommendations.get(i), null, -1));
            }
        }

        // --- Empty state if there is nothing to show at all ---
        if (resultsContainer.getChildren().isEmpty()) {
            Label empty = new Label(selectedCount == 0
                ? "Select how you are feeling above, or ask the assistant below, to see matched jamu remedies."
                : "No matching remedies found. Try different symptoms or ask the assistant below.");
            empty.getStyleClass().add("label-muted");
            empty.setWrapText(true);
            empty.setMinHeight(Label.USE_PREF_SIZE);
            resultsContainer.getChildren().add(empty);
        }
    }

    // Returns true if the haystack contains any keyword from the comma list.
    private boolean matchesAnyKeyword(String haystack, String keywordList) {
        String[] keywords = keywordList.split(",");
        for (String keyword : keywords) {
            if (haystack.contains(keyword.trim())) {
                return true;
            }
        }
        return false;
    }

    // Build one result card for a product.
    // badgeText may be null (no badge). percent below 0 hides the circle.
    private HBox createProductCard(Product product, String badgeText, int percent) {
        HBox card = new HBox(15);
        card.getStyleClass().add("match-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Left side: the product details.
        VBox details = new VBox(8);

        if (badgeText != null) {
            Label badge = new Label(badgeText);
            badge.getStyleClass().add("match-badge");
            details.getChildren().add(badge);
        }

        Label name = new Label(product.getName());
        name.getStyleClass().add("detail-name");
        name.setWrapText(true);
        name.setMinHeight(Label.USE_PREF_SIZE);

        Label description = new Label(
            product.getDescription() != null && !product.getDescription().isEmpty()
                ? product.getDescription() : "A traditional jamu remedy."
        );
        description.getStyleClass().add("label-muted");
        description.setWrapText(true);
        description.setMinHeight(Label.USE_PREF_SIZE);

        details.getChildren().addAll(name, description);

        // Ingredient tags (small pills), if the product lists ingredients.
        String ingredients = product.getIngredients();
        if (ingredients != null && !ingredients.isEmpty()) {
            FlowPane tags = new FlowPane(6, 6);
            String[] parts = ingredients.split(",");
            int count = 0;
            for (String part : parts) {
                if (part.trim().isEmpty() || count >= 4) {
                    continue;
                }
                Label tag = new Label(part.trim());
                tag.getStyleClass().add("ingredient-tag");
                tags.getChildren().add(tag);
                count++;
            }
            if (!tags.getChildren().isEmpty()) {
                details.getChildren().add(tags);
            }
        }

        // "Add to Cart" button using the product's real name and price.
        Label addedLabel = new Label("");
        addedLabel.getStyleClass().add("label-success");

        Button addButton = new Button("Add to Cart");
        addButton.setOnAction(e -> {
            CartManager.addItem(product.getName(), product.getPrice(), 1);
            updateCartButton();
            showAddedMessage(addedLabel);
        });

        HBox addRow = new HBox(10, addButton, addedLabel);
        addRow.setAlignment(Pos.CENTER_LEFT);
        details.getChildren().add(addRow);

        // A spacer pushes the circle to the right side.
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(details, spacer);

        // Right side: the match percentage in a circle (only if we have one).
        if (percent >= 0) {
            Label circle = new Label(percent + "%");
            circle.getStyleClass().add("match-circle");
            card.getChildren().add(circle);
        }

        return card;
    }

    // ---------------- Chat area ----------------

    // The send button asks the Jamu Assistant a question.
    @FXML
    public void handleSendChat() {
        String question = chatInput.getText().trim();
        if (question.isEmpty()) {
            return;
        }

        // Show the user's message on the right straight away.
        addUserBubble(question);
        chatInput.clear();

        // Show a placeholder bot bubble while we wait for the answer.
        Label botBubble = addBotBubble("Thinking...");
        sendButton.setDisable(true);
        scrollChatToBottom();

        // The Ollama call can be slow, so we do it on a background thread
        // and update the screen when it is done.
        new Thread(() -> {
            String reply = chatbot.sendChatMessage(question);
            List<Product> recommendations = recommendationService.recommendByKeywords(question);

            Platform.runLater(() -> {
                botBubble.setText(reply);
                chatRecommendations = recommendations;
                rebuildResults();
                sendButton.setDisable(false);
                scrollChatToBottom();
            });
        }).start();
    }

    // Add a user message bubble (orange, aligned right).
    private void addUserBubble(String text) {
        Label bubble = new Label(text);
        bubble.getStyleClass().add("chat-bubble-user");
        bubble.setWrapText(true);
        bubble.setMinHeight(Label.USE_PREF_SIZE);
        bubble.setMaxWidth(460);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        chatHistoryBox.getChildren().add(row);
    }

    // Add a bot message bubble (cream, aligned left, with a small AI icon).
    // We return the bubble label so we can update its text later.
    private Label addBotBubble(String text) {
        Label icon = new Label("AI");
        icon.getStyleClass().add("bot-icon");

        Label bubble = new Label(text);
        bubble.getStyleClass().add("chat-bubble-bot");
        bubble.setWrapText(true);
        bubble.setMinHeight(Label.USE_PREF_SIZE);
        bubble.setMaxWidth(440);

        HBox row = new HBox(8, icon, bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        chatHistoryBox.getChildren().add(row);
        return bubble;
    }

    // Scroll the chat history to the newest message.
    private void scrollChatToBottom() {
        if (chatScroll != null) {
            chatScroll.setVvalue(1.0);
        }
    }

    // ---------------- Cart helpers ----------------

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

    // ---------------- Buttons and navigation ----------------

    // The clear button unselects every symptom.
    @FXML
    public void handleClearAll() {
        for (ToggleButton pill : pills) {
            pill.setSelected(false);
        }
        rebuildResults();
    }

    // Go to the Herb Matrix screen.
    @FXML
    public void handleNavHerb() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/HerbMatrix.fxml", "JamuSync - Herb Matrix");
    }

    // Go to the Jamu Mixer screen.
    @FXML
    public void handleNavMixer() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/JamuMixer.fxml", "JamuSync - Jamu Mixer");
    }

    // Go back to the login screen.
    @FXML
    public void handleBackToLogin() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/Login.fxml", "JamuSync - Sign In");
    }

    // Helper: turn null text into an empty string.
    private String safe(String text) {
        return text == null ? "" : text;
    }
}
