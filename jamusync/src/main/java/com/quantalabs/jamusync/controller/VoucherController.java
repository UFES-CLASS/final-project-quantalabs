package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.dao.VoucherDAO;
import com.quantalabs.jamusync.model.Voucher;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

public class VoucherController {

    @FXML private TableView<Voucher> voucherTable;
    @FXML private TableColumn<Voucher, String> colCode;
    @FXML private TableColumn<Voucher, String> colType;
    @FXML private TableColumn<Voucher, String> colValue;
    @FXML private TableColumn<Voucher, String> colUsage;
    @FXML private TableColumn<Voucher, String> colExpiry;
    @FXML private TableColumn<Voucher, String> colActive;

    @FXML private TextField codeField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField valueField;
    @FXML private TextField usageLimitField;
    @FXML private DatePicker expiryPicker;
    @FXML private CheckBox activeCheckbox;
    @FXML private Label messageLabel;
    @FXML private Button saveButton;
    @FXML private Button deactivateButton;

    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final DecimalFormat formatter = new DecimalFormat("Rp #,###");
    private int selectedVoucherId = 0;

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList("fixed", "percentage"));
        typeCombo.getSelectionModel().selectFirst();
        expiryPicker.setValue(LocalDate.now().plusMonths(1));

        setupTableColumns();
        loadVouchers();
        voucherTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                populateForm(selected);
            }
        });
        handleClear(null);
    }

    private void setupTableColumns() {
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDiscountType()));
        colValue.setCellValueFactory(data -> {
            Voucher v = data.getValue();
            String val = "percentage".equals(v.getDiscountType()) ?
                v.getDiscountValue() + "%" : formatter.format(v.getDiscountValue());
            return new SimpleStringProperty(val);
        });
        colUsage.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getUsageCount() + "/" + data.getValue().getUsageLimit()
        ));
        colExpiry.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getExpiryDate()));
        colActive.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Yes" : "No"));
    }

    private void loadVouchers() {
        List<Voucher> vouchers = voucherDAO.getAllVouchers();
        voucherTable.setItems(FXCollections.observableArrayList(vouchers));
    }

    private void populateForm(Voucher voucher) {
        selectedVoucherId = voucher.getId();
        codeField.setText(voucher.getCode());
        typeCombo.getSelectionModel().select(voucher.getDiscountType());
        valueField.setText(String.valueOf(voucher.getDiscountValue()));
        usageLimitField.setText(String.valueOf(voucher.getUsageLimit()));
        try {
            expiryPicker.setValue(LocalDate.parse(voucher.getExpiryDate()));
        } catch (Exception e) {
            expiryPicker.setValue(LocalDate.now().plusMonths(1));
        }
        activeCheckbox.setSelected(voucher.isActive());
        deactivateButton.setDisable(!voucher.isActive());
        saveButton.setText("Update Voucher");
        clearMessage();
    }

    @FXML
    public void handleSave(ActionEvent event) {
        String error = validateForm();
        if (error != null) {
            showError(error);
            return;
        }

        Voucher voucher = buildFromForm();
        boolean success;

        if (selectedVoucherId == 0) {
            success = voucherDAO.insertVoucher(voucher);
            if (success) {
                showSuccess("Voucher created successfully.");
                handleClear(null);
            } else {
                showError("Failed to create voucher. Code may already exist.");
            }
        } else {
            voucher.setId(selectedVoucherId);
            Voucher existing = voucherDAO.getById(selectedVoucherId);
            if (existing != null) {
                voucher.setUsageCount(existing.getUsageCount());
            }
            success = voucherDAO.updateVoucher(voucher);
            if (success) {
                showSuccess("Voucher updated successfully.");
            } else {
                showError("Failed to update voucher.");
            }
        }

        if (success) {
            loadVouchers();
        }
    }

    @FXML
    public void handleDeactivate(ActionEvent event) {
        if (selectedVoucherId == 0) {
            showError("Select a voucher to deactivate.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("JamuSync - Confirm");
        confirm.setHeaderText("Deactivate Voucher");
        confirm.setContentText("Deactivate voucher \"" + codeField.getText().trim() + "\"?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                if (voucherDAO.deactivateVoucher(selectedVoucherId)) {
                    showSuccess("Voucher deactivated.");
                    handleClear(null);
                    loadVouchers();
                } else {
                    showError("Failed to deactivate voucher.");
                }
            }
        });
    }

    @FXML
    public void handleClear(ActionEvent event) {
        selectedVoucherId = 0;
        codeField.clear();
        typeCombo.getSelectionModel().selectFirst();
        valueField.clear();
        usageLimitField.setText("1");
        expiryPicker.setValue(LocalDate.now().plusMonths(1));
        activeCheckbox.setSelected(true);
        deactivateButton.setDisable(true);
        saveButton.setText("Save Voucher");
        voucherTable.getSelectionModel().clearSelection();
        clearMessage();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        JamuSyncApp.changeScene("/com/quantalabs/jamusync/fxml/OwnerDashboard.fxml", "JamuSync - Owner Dashboard");
    }

    private String validateForm() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            return "Voucher code is required.";
        }
        if (!voucherDAO.isCodeUnique(code, selectedVoucherId)) {
            return "This voucher code already exists.";
        }
        if (typeCombo.getSelectionModel().getSelectedItem() == null) {
            return "Discount type is required.";
        }
        double value;
        try {
            value = Double.parseDouble(valueField.getText().trim());
            if (value <= 0) {
                return "Discount value must be positive.";
            }
            if ("percentage".equals(typeCombo.getSelectionModel().getSelectedItem()) && value > 100) {
                return "Percentage discount cannot exceed 100%.";
            }
        } catch (NumberFormatException e) {
            return "Invalid discount value.";
        }
        try {
            int limit = Integer.parseInt(usageLimitField.getText().trim());
            if (limit <= 0) {
                return "Usage limit must be positive.";
            }
        } catch (NumberFormatException e) {
            return "Invalid usage limit.";
        }
        if (expiryPicker.getValue() == null) {
            return "Expiry date is required.";
        }
        if (expiryPicker.getValue().isBefore(LocalDate.now())) {
            return "Expiry date cannot be in the past.";
        }
        return null;
    }

    private Voucher buildFromForm() {
        Voucher voucher = new Voucher();
        voucher.setCode(codeField.getText().trim().toUpperCase());
        voucher.setDiscountType(typeCombo.getSelectionModel().getSelectedItem());
        voucher.setDiscountValue(Double.parseDouble(valueField.getText().trim()));
        voucher.setUsageLimit(Integer.parseInt(usageLimitField.getText().trim()));
        voucher.setExpiryDate(expiryPicker.getValue().toString());
        voucher.setActive(activeCheckbox.isSelected());
        return voucher;
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
