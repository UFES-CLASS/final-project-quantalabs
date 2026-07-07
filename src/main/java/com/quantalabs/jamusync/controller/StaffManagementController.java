package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.UserDAO;
import com.quantalabs.jamusync.model.User;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class StaffManagementController {

    @FXML private TableView<User> staffTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colActive;
    @FXML private TableColumn<User, String> colCreated;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox activeCheckbox;
    @FXML private Label messageLabel;
    @FXML private Button saveButton;
    @FXML private Button deactivateButton;

    private final UserDAO userDAO = new UserDAO();
    private int selectedStaffId = 0;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadStaff();
        staffTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                populateForm(selected);
            }
        });
        handleClear(null);
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        colActive.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Yes" : "No"));
        colCreated.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getCreatedAt() != null ? data.getValue().getCreatedAt() : ""
        ));
    }

    private void loadStaff() {
        List<User> staff = userDAO.getAllStaff();
        staffTable.setItems(FXCollections.observableArrayList(staff));
    }

    private void populateForm(User user) {
        selectedStaffId = user.getId();
        usernameField.setText(user.getUsername());
        passwordField.clear();
        activeCheckbox.setSelected(user.isActive());
        deactivateButton.setDisable(!user.isActive());
        saveButton.setText("Update Staff");
        clearMessage();
    }

    @FXML
    public void handleSave(ActionEvent event) {
        String validationError = validateForm();
        if (validationError != null) {
            showError(validationError);
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        boolean active = activeCheckbox.isSelected();
        boolean success;

        if (selectedStaffId == 0) {
            success = userDAO.insertUser(username, password, "staff");
            if (success) {
                showSuccess("Staff account created successfully.");
                handleClear(null);
            } else {
                showError("Failed to create staff account. Username may already exist.");
            }
        } else {
            success = userDAO.updateUser(selectedStaffId, username, password, active);
            if (success) {
                showSuccess("Staff account updated successfully.");
            } else {
                showError("Failed to update staff account.");
            }
        }

        if (success) {
            loadStaff();
        }
    }

    @FXML
    public void handleDeactivate(ActionEvent event) {
        if (selectedStaffId == 0) {
            showError("Select a staff member to deactivate.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("JamuSync - Confirm");
        confirm.setHeaderText("Deactivate Staff");
        confirm.setContentText("Are you sure you want to deactivate \"" + usernameField.getText().trim() + "\"?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                if (userDAO.setUserActive(selectedStaffId, false)) {
                    showSuccess("Staff account deactivated successfully.");
                    handleClear(null);
                    loadStaff();
                } else {
                    showError("Failed to deactivate staff account.");
                }
            }
        });
    }

    @FXML
    public void handleClear(ActionEvent event) {
        selectedStaffId = 0;
        usernameField.clear();
        passwordField.clear();
        activeCheckbox.setSelected(true);
        deactivateButton.setDisable(true);
        saveButton.setText("Save Staff");
        staffTable.getSelectionModel().clearSelection();
        clearMessage();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/OwnerDashboard.fxml", "JamuSync - Owner Dashboard");
    }

    private String validateForm() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            return "Username is required.";
        }
        if (!userDAO.isUsernameUnique(username, selectedStaffId)) {
            return "This username is already taken.";
        }
        if (selectedStaffId == 0 && passwordField.getText().isEmpty()) {
            return "Password is required for new staff accounts.";
        }
        return null;
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
