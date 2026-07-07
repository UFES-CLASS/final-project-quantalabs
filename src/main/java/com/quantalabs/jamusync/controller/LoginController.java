package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.UserDAO;
import com.quantalabs.jamusync.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        errorLabel.setText("");
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
            
            // Redirect based on role
            if ("owner".equalsIgnoreCase(user.getRole())) {
                JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/OwnerDashboard.fxml", "JamuSync - Owner Dashboard");
            } else if ("staff".equalsIgnoreCase(user.getRole())) {
                JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/StaffDashboard.fxml", "JamuSync - Staff Dashboard");
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
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/HerbMatrix.fxml", "JamuSync - Herb Matrix");
    }
}
