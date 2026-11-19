package com.houdinisecurity.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

import java.util.Optional;

public final class PasswordDialog {

    private final Dialog<String> dialog;

    public PasswordDialog(String title, I18n i18n) {
        dialog = new Dialog<>();
        dialog.setTitle(title);
        PasswordField field = new PasswordField();
        field.setPromptText(title);
        VBox box = new VBox(8, new Label(title), field);
        box.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(box);
        ButtonType ok = new ButtonType(i18n.t("action.ok"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> bt == ok ? field.getText() : null);
    }

    public Optional<String> showAndWait() {
        return dialog.showAndWait();
    }
}
