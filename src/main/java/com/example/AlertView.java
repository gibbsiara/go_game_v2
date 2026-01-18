package com.example;

import javafx.scene.control.Alert;
/**
 * A utility class responsible for displaying system dialog boxes (alerts)
 * within the JavaFX user interface.
 */
public class AlertView {

    /**
     * Displays an alert dialog and waits for user interaction.
     * @param title The title of the alert window.
     * @param message The content text to be displayed in the alert.
     * @param type The severity or type of the alert (e.g., ERROR, INFORMATION).
     */

    public void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
