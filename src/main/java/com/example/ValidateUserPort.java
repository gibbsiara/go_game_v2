package com.example;

import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class ValidateUserPort {
    AlertView alertView = new AlertView();

    public int getPortFromField(TextField portField) {
        String userInput = portField.getText().trim();

        if (userInput.isEmpty()) {
            alertView.showAlert("Server", "Podaj poprawny port do gry", Alert.AlertType.INFORMATION);
            return -1;
        }

        try {
            int port = Integer.parseInt(userInput);
            return port;
        } catch (NumberFormatException e) {
            alertView.showAlert("Błąd", "Niepoprawny port, podaj liczby", Alert.AlertType.ERROR);
            return -1;
        }
    }

}
