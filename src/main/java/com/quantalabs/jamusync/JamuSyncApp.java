package com.quantalabs.jamusync;

import com.quantalabs.jamusync.database.DatabaseManager;
import com.quantalabs.jamusync.model.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class JamuSyncApp extends Application {

    private static Stage primaryStage;
    private static User currentUser;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Initialize SQLite Database tables and seed default admin
        DatabaseManager.getInstance().initializeDatabase();

        try {
            // Load Login View
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/quantalabs/jamusync/fxml/Login.fxml"));
            Parent root = loader.load();
            
            // Give the scene a sensible starting size, then open maximized so
            // the content fills the whole screen. The FXML roots grow to fill
            // whatever size the window is.
            Scene scene = new Scene(root, 1280, 800);
            primaryStage.setScene(scene);
            primaryStage.setTitle("JamuSync - Sign In");
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(650);
            primaryStage.setMaximized(true);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Failed to load Login.fxml");
            e.printStackTrace();
        }
    }

    /**
     * Switch scenes within the primary stage.
     * @param fxmlPath Path to the FXML file from the resources directory.
     * @param title The title for the stage window.
     */
    public static void changeScene(String fxmlPath, String title) {
        if (primaryStage == null) {
            System.err.println("Primary stage is not initialized.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(JamuSyncApp.class.getResource(fxmlPath));
            Parent root = loader.load();
            // Reuse the existing scene and just swap the root. This keeps the
            // current window size and maximized state so every screen fills
            // the window (we do NOT call sizeToScene, which would shrink it).
            primaryStage.getScene().setRoot(root);
            primaryStage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Failed to switch scene to: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
