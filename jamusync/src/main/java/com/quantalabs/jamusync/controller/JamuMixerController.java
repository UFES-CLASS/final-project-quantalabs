package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.util.CartManager;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

// This screen lets a guest pick ingredients and see the health profile
// of their custom jamu blend update live.
public class JamuMixerController {

    // The width (in pixels) of the health bar track in the FXML.
    private static final int BAR_WIDTH = 200;

    // Our 8 ingredients. Each row holds:
    // 0 = Indonesian name, 1 = English name,
    // 2 = immunity score, 3 = energy score, 4 = digestion score
    private final String[][] ingredients = {
        {"Jahe",        "Ginger",      "15", "20", "18"},
        {"Kunyit",      "Turmeric",    "22", "10", "14"},
        {"Asam Jawa",   "Tamarind",    "10", "8",  "24"},
        {"Temulawak",   "Java Ginger", "18", "12", "20"},
        {"Kencur",      "Galangal",    "14", "16", "17"},
        {"Serai",       "Lemongrass",  "12", "14", "15"},
        {"Madu",        "Honey",       "20", "22", "10"},
        {"Jeruk Nipis", "Lime",        "16", "18", "12"}
    };

    // We keep the toggle buttons in a list so we can read them later.
    private final List<ToggleButton> toggles = new ArrayList<>();

    // --- Things wired up from the FXML ---
    @FXML private VBox ingredientBox;
    @FXML private Label selectedCountLabel;

    @FXML private Region immunityFill;
    @FXML private Region energyFill;
    @FXML private Region digestionFill;

    @FXML private Label immunityPercentLabel;
    @FXML private Label energyPercentLabel;
    @FXML private Label digestionPercentLabel;

    @FXML private Label potencyLabel;

    // The brew button and the result card (hidden until brew is clicked).
    @FXML private Button brewButton;
    @FXML private VBox resultCard;
    @FXML private Label resultBlendLabel;
    @FXML private Label resultDescLabel;
    @FXML private Button addBlendButton;
    @FXML private Label blendAddedLabel;

    // The cart button in the top nav bar.
    @FXML private Button cartButton;

    // Remember the last brewed blend so we can add it to the cart.
    private String lastBlendName;
    private double lastBlendPrice;

    // Runs automatically when the screen opens.
    @FXML
    public void initialize() {
        // Make a toggle button for each ingredient.
        for (String[] ingredient : ingredients) {
            // English name first, then the Indonesian name in parentheses,
            // e.g. "Ginger (Jahe)". (index 1 = English, index 0 = Indonesian)
            ToggleButton toggle = new ToggleButton(ingredient[1] + " (" + ingredient[0] + ")");
            toggle.getStyleClass().add("ingredient-card");
            toggle.setMaxWidth(Double.MAX_VALUE);
            // Extra vertical padding + let the button grow to its content height
            // so the ingredient name is never clipped at the bottom edge.
            toggle.setStyle("-fx-padding: 14 14 14 14;");
            toggle.setMinHeight(Region.USE_PREF_SIZE);

            // A small grey circle (radio button style) on the left of the card.
            Region dot = new Region();
            dot.getStyleClass().add("ingredient-dot");
            toggle.setGraphic(dot);

            // When it is clicked, recalculate everything.
            toggle.setOnAction(e -> updateHealth());
            toggles.add(toggle);
            ingredientBox.getChildren().add(toggle);
        }
        // Show the starting (empty) state.
        updateHealth();
        updateCartButton();
    }

    // Recalculate the counts, health bars, percentages and potency.
    private void updateHealth() {
        int selectedCount = 0;
        int immunitySum = 0;
        int energySum = 0;
        int digestionSum = 0;

        // Look at every ingredient and add up the selected ones.
        for (int i = 0; i < toggles.size(); i++) {
            ToggleButton toggle = toggles.get(i);
            Region dot = (Region) toggle.getGraphic();

            if (toggle.isSelected()) {
                // Show the orange left stripe and fill the circle.
                if (!toggle.getStyleClass().contains("ingredient-card-selected")) {
                    toggle.getStyleClass().add("ingredient-card-selected");
                }
                if (!dot.getStyleClass().contains("ingredient-dot-selected")) {
                    dot.getStyleClass().add("ingredient-dot-selected");
                }
                selectedCount++;
                immunitySum += Integer.parseInt(ingredients[i][2]);
                energySum += Integer.parseInt(ingredients[i][3]);
                digestionSum += Integer.parseInt(ingredients[i][4]);
            } else {
                toggle.getStyleClass().remove("ingredient-card-selected");
                dot.getStyleClass().remove("ingredient-dot-selected");
            }
        }

        // Percentages are capped at 100.
        int immunityPercent = Math.min(immunitySum, 100);
        int energyPercent = Math.min(energySum, 100);
        int digestionPercent = Math.min(digestionSum, 100);

        // Update the counter label.
        selectedCountLabel.setText("(" + selectedCount + " SELECTED)");

        // Update the bar widths and the percentage labels.
        setBar(immunityFill, immunityPercentLabel, immunityPercent);
        setBar(energyFill, energyPercentLabel, energyPercent);
        setBar(digestionFill, digestionPercentLabel, digestionPercent);

        // Potency is the average of the three percentages.
        int potency = (immunityPercent + energyPercent + digestionPercent) / 3;
        potencyLabel.setText(potency + " / 100");

        // Update the brew button: greyed out with a hint when nothing is picked.
        if (selectedCount == 0) {
            brewButton.setDisable(true);
            brewButton.setText("Select ingredients to brew");
        } else {
            brewButton.setDisable(false);
            brewButton.setText("Brew My Jamu");
        }

        // Hide the old result until the user brews again.
        resultCard.setVisible(false);
        resultCard.setManaged(false);
    }

    // Set one bar's green fill width and its percentage text.
    private void setBar(Region fill, Label percentLabel, int percent) {
        double width = BAR_WIDTH * (percent / 100.0);
        fill.setMinWidth(width);
        fill.setPrefWidth(width);
        fill.setMaxWidth(width);
        percentLabel.setText(percent + "%");
    }

    // The brew button shows a blend name and description in the result card.
    @FXML
    public void handleBrew() {
        // The button is disabled when nothing is selected, but check anyway.
        int selectedCount = 0;
        for (ToggleButton toggle : toggles) {
            if (toggle.isSelected()) {
                selectedCount++;
            }
        }
        if (selectedCount == 0) {
            return;
        }

        // Work out a nice blend name and description from common combinations.
        String blendName;
        String description;
        if (isSelected("Ginger") && isSelected("Turmeric") && isSelected("Honey")) {
            blendName = "Golden Immunity Tonic";
            description = "A warming blend of ginger, turmeric and honey to strengthen your immune system.";
        } else if (isSelected("Turmeric") && isSelected("Tamarind")) {
            blendName = "Kunyit Asam Classic";
            description = "The beloved turmeric and tamarind tonic, refreshing and great for digestion.";
        } else if (isSelected("Lemongrass") && isSelected("Lime") && isSelected("Ginger")) {
            blendName = "Fresh Energy Elixir";
            description = "Zesty lemongrass, lime and ginger to lift your energy naturally.";
        } else if (isSelected("Ginger") && isSelected("Honey")) {
            blendName = "Soothing Ginger Warmer";
            description = "Simple ginger and honey to soothe and comfort.";
        } else {
            blendName = "Custom Jamu Blend";
            description = "Your own unique mix of traditional Indonesian herbs.";
        }

        // Work out a price for the blend based on how many ingredients are in it.
        double price = 10000 + 2500 * selectedCount;

        // Remember the blend so the "Add Blend to Cart" button can use it.
        lastBlendName = blendName;
        lastBlendPrice = price;

        // Fill in the card and make it visible.
        resultBlendLabel.setText(blendName);
        resultDescLabel.setText(description);
        addBlendButton.setText("Add Blend to Cart (Rp " + (int) price + ")");
        blendAddedLabel.setText("");
        resultCard.setVisible(true);
        resultCard.setManaged(true);
    }

    // Add the brewed blend to the shared cart.
    @FXML
    public void handleAddBlend() {
        if (lastBlendName == null) {
            return;
        }
        CartManager.addItem(lastBlendName, lastBlendPrice, 1);
        updateCartButton();
        showAddedMessage(blendAddedLabel);
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

    // Helper: is the ingredient with this English name selected?
    private boolean isSelected(String englishName) {
        for (int i = 0; i < toggles.size(); i++) {
            if (ingredients[i][1].equals(englishName) && toggles.get(i).isSelected()) {
                return true;
            }
        }
        return false;
    }

    // Go to the Herb Matrix screen.
    @FXML
    public void handleNavHerb() {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/HerbMatrix.fxml", "JamuSync - Herb Matrix");
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
