package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.UserDAO;
import com.quantalabs.jamusync.model.User;
import com.quantalabs.jamusync.util.LanguageManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

// Extends BaseController to reuse its shared helper methods (navigateTo, etc.).
public class LoginController extends BaseController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    // New: the labels/button whose text changes with the language.
    @FXML
    private Label usernameLabel;
    @FXML
    private Label passwordLabel;
    @FXML
    private Button guestButton;

    // The language picker in the top-right corner.
    @FXML
    private ComboBox<String> languageCombo;

    // The names shown in the ComboBox. We keep the matching language codes
    // ("en", "id", "jw") in the same order so we can convert between them.
    private static final String[] LANGUAGE_NAMES = {"English", "Bahasa Indonesia", "Basa Jawa"};
    private static final String[] LANGUAGE_CODES = {"en", "id", "jw"};

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        errorLabel.setText("");

        // Fill the ComboBox with the three language names.
        languageCombo.getItems().setAll(LANGUAGE_NAMES);
        // Show the language that is currently active (persists between screens
        // because LanguageManager is static).
        languageCombo.setValue(nameForCurrentLanguage());

        // Set all the screen text in the current language.
        applyLanguage();
    }

    /**
     * Called when the user picks a different language from the ComboBox.
     * We tell LanguageManager to switch, then refresh the screen text.
     */
    @FXML
    public void handleLanguageChange(ActionEvent event) {
        String selectedName = languageCombo.getValue();
        // Find the language code that matches the selected name.
        for (int i = 0; i < LANGUAGE_NAMES.length; i++) {
            if (LANGUAGE_NAMES[i].equals(selectedName)) {
                LanguageManager.setLanguage(LANGUAGE_CODES[i]);
                break;
            }
        }
        applyLanguage();
    }

    /** Update every piece of text on the login screen using the current language. */
    private void applyLanguage() {
        usernameLabel.setText(LanguageManager.getString("login.username"));
        passwordLabel.setText(LanguageManager.getString("login.password"));
        loginButton.setText(LanguageManager.getString("login.signin"));
        guestButton.setText(LanguageManager.getString("login.guest"));
    }

    /** Work out which ComboBox name matches the language currently in use. */
    private String nameForCurrentLanguage() {
        String code = LanguageManager.getCurrentLocale().getLanguage();
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(code)) {
                return LANGUAGE_NAMES[i];
            }
        }
        return LANGUAGE_NAMES[0]; // default to English
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setText("");

        // Authenticate user
        User user = userDAO.authenticate(username, password);

        if (user != null) {
            JamuSyncApp.setCurrentUser(user);
            System.out.println("Login successful for user: " + user.getUsername() + " with role: " + user.getRole());
            
            // Redirect based on role, using the inherited navigateTo() helper.
            if ("owner".equalsIgnoreCase(user.getRole())) {
                navigateTo("/com/quantalabs/jamusync/fxml/OwnerDashboard.fxml", event);
            } else if ("staff".equalsIgnoreCase(user.getRole())) {
                navigateTo("/com/quantalabs/jamusync/fxml/StaffDashboard.fxml", event);
            } else {
                errorLabel.setText("Invalid user role configuration.");
                loginButton.setDisable(false);
            }
        } else {
            errorLabel.setText("Invalid username or password.");
            loginButton.setDisable(false);
        }
    }

    @FXML
    public void handleGuestBrowse(ActionEvent event) {
        System.out.println("Browsing as guest...");
        JamuSyncApp.setCurrentUser(null); // Guest
        navigateTo("/com/quantalabs/jamusync/fxml/HerbMatrix.fxml", event);
    }
}
