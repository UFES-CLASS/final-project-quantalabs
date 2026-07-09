package com.quantalabs.jamusync.controller;

import com.quantalabs.jamusync.JamuSyncApp;
import com.quantalabs.jamusync.model.User;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;

/**
 * BaseController is a PARENT class for our JavaFX controllers.
 *
 * Many controllers need to do the same small jobs: pop up an alert box,
 * switch to another screen, or find out who is currently logged in. Before,
 * every controller wrote its own copy of that code. By putting these helper
 * methods here ONCE and letting each controller "extend BaseController", every
 * controller gets them for free. This is inheritance being used to REDUCE CODE
 * DUPLICATION - write it once in the parent, reuse it in all the children.
 *
 * The methods are "protected" so that only this class and the controllers that
 * extend it can use them.
 */
public class BaseController {

    /** Show a simple pop-up information box with a title and a message. */
    protected void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("JamuSync - Info");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Switch the app to another screen (FXML file).
     *
     * We build a friendly window title from the file name, e.g. the file
     * ".../OwnerDashboard.fxml" becomes the title "JamuSync - OwnerDashboard".
     * The ActionEvent is passed in because button handlers already receive it;
     * we keep it in the signature so callers have it available.
     */
    protected void navigateTo(String fxmlFile, ActionEvent event) {
        String fileName = fxmlFile.substring(fxmlFile.lastIndexOf('/') + 1).replace(".fxml", "");
        JamuSyncApp.changeScene(fxmlFile, "JamuSync - " + fileName);
    }

    /** Return the User who is currently logged in (or null for a guest). */
    protected User getCurrentUser() {
        return JamuSyncApp.getCurrentUser();
    }
}
