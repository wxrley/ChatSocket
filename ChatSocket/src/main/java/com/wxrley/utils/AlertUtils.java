package com.wxrley.utils;

import javafx.scene.control.Alert;

/**
 * Classe utilitária para exibir alertas na interface JavaFX.
 * Utilizada por LoginController para mostrar erros de validação e conexão.
 */
public class AlertUtils {

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}