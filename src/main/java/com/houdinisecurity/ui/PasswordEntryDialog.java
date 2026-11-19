package com.houdinisecurity.ui;

import com.houdinisecurity.AppContext;
import com.houdinisecurity.crypto.SecureMemory;
import com.houdinisecurity.domain.PasswordEntry;
import com.houdinisecurity.service.PasswordGenerator;
import com.houdinisecurity.service.StrengthMeter;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Clear create/edit form for password entries (site, URL, user, password, recovery codes, notes).
 */
public final class PasswordEntryDialog {

    private final AppContext ctx;
    private final PasswordEntry existing;

    public PasswordEntryDialog(AppContext ctx, PasswordEntry existing) {
        this.ctx = ctx;
        this.existing = existing;
    }

    public Optional<PasswordEntry> showAndWait() {
        I18n i18n = ctx.i18n();
        Dialog<PasswordEntry> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? i18n.t("entry.newPassword") : i18n.t("entry.editPassword"));
        dialog.setHeaderText(existing == null ? i18n.t("entry.newPassword.hint") : i18n.t("entry.editPassword.hint"));

        TextField site = new TextField(existing == null ? "" : existing.getSite());
        site.setPromptText(i18n.t("field.site.prompt"));

        TextField url = new TextField(existing == null ? "" : existing.getUrl());
        url.setPromptText(i18n.t("field.url.prompt"));

        TextField user = new TextField(existing == null ? "" : existing.getUsername());
        user.setPromptText(i18n.t("field.username.prompt"));

        PasswordField passHidden = new PasswordField();
        TextField passVisible = new TextField();
        passVisible.setManaged(false);
        passVisible.setVisible(false);
        passHidden.textProperty().bindBidirectional(passVisible.textProperty());
        if (existing != null) {
            char[] pw = existing.getPasswordCopy();
            try {
                passHidden.setText(new String(pw));
            } finally {
                SecureMemory.wipe(pw);
            }
        }
        passHidden.setPromptText(i18n.t("field.password.prompt"));
        passVisible.setPromptText(i18n.t("field.password.prompt"));

        Label strengthLabel = new Label();
        strengthLabel.getStyleClass().add("muted");
        Runnable updateStrength = () -> {
            char[] chars = passHidden.getText().toCharArray();
            try {
                StrengthMeter.StrengthResult r = ctx.strengthMeter().evaluate(chars);
                strengthLabel.setText(i18n.t(r.labelKey()) + " (" + r.score0to100() + ")");
            } finally {
                SecureMemory.wipe(chars);
            }
        };
        passHidden.textProperty().addListener((o, a, b) -> updateStrength.run());
        updateStrength.run();

        Button revealPass = new Button(i18n.t("action.reveal"));
        revealPass.getStyleClass().add("ghost");
        revealPass.setOnAction(e -> {
            boolean show = !passVisible.isVisible();
            passVisible.setManaged(show);
            passVisible.setVisible(show);
            passHidden.setManaged(!show);
            passHidden.setVisible(!show);
            revealPass.setText(show ? i18n.t("action.hide") : i18n.t("action.reveal"));
        });

        Button generate = new Button(i18n.t("action.generate"));
        generate.getStyleClass().add("accent");
        generate.setOnAction(e -> {
            String generated = ctx.passwordGenerator().generate(PasswordGenerator.Options.defaults());
            passHidden.setText(generated);
        });

        HBox passRow = new HBox(8, passHidden, passVisible, revealPass, generate);
        HBox.setHgrow(passHidden, Priority.ALWAYS);
        HBox.setHgrow(passVisible, Priority.ALWAYS);
        passHidden.setMaxWidth(Double.MAX_VALUE);
        passVisible.setMaxWidth(Double.MAX_VALUE);

        TextArea recovery = new TextArea();
        recovery.setPromptText(i18n.t("field.recoveryCodes.prompt"));
        recovery.setPrefRowCount(5);
        recovery.setWrapText(true);
        recovery.getStyleClass().add("recovery-codes");
        if (existing != null) {
            char[] codes = existing.getRecoveryCodesCopy();
            try {
                recovery.setText(new String(codes));
            } finally {
                SecureMemory.wipe(codes);
            }
        }

        TextArea notes = new TextArea(existing == null ? "" : existing.getNotes());
        notes.setPromptText(i18n.t("field.notes.prompt"));
        notes.setPrefRowCount(3);
        notes.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(8, 4, 4, 4));
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(160);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setFillWidth(true);
        grid.getColumnConstraints().addAll(c0, c1);

        int row = 0;
        grid.add(labeled(i18n.t("field.site")), 0, row);
        grid.add(site, 1, row++);
        grid.add(labeled(i18n.t("field.url")), 0, row);
        grid.add(url, 1, row++);
        grid.add(labeled(i18n.t("field.username")), 0, row);
        grid.add(user, 1, row++);
        grid.add(labeled(i18n.t("field.password")), 0, row);
        grid.add(passRow, 1, row++);
        grid.add(new Label(""), 0, row);
        grid.add(strengthLabel, 1, row++);
        grid.add(labeled(i18n.t("field.recoveryCodes")), 0, row);
        Label recoveryHint = new Label(i18n.t("field.recoveryCodes.hint"));
        recoveryHint.getStyleClass().add("muted");
        recoveryHint.setWrapText(true);
        VBox recoveryBox = new VBox(4, recoveryHint, recovery);
        grid.add(recoveryBox, 1, row++);
        grid.add(labeled(i18n.t("field.notes")), 0, row);
        grid.add(notes, 1, row);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(560);
        dialog.getDialogPane().getStyleClass().add("password-entry-dialog");

        ButtonType save = new ButtonType(i18n.t("action.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(i18n.t("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(save, cancel);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(save);
        saveBtn.getStyleClass().add("accent");
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (site.getText() == null || site.getText().isBlank()) {
                ev.consume();
                Alert a = new Alert(Alert.AlertType.WARNING, i18n.t("error.siteRequired"));
                a.setHeaderText(null);
                a.showAndWait();
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt != save) {
                return null;
            }
            char[] pw = passHidden.getText().toCharArray();
            char[] codes = recovery.getText() == null ? new char[0] : recovery.getText().toCharArray();
            try {
                if (existing == null) {
                    return PasswordEntry.create(
                            site.getText().trim(),
                            user.getText().trim(),
                            pw,
                            url.getText().trim(),
                            notes.getText(),
                            codes);
                }
                existing.setSite(site.getText().trim());
                existing.setUsername(user.getText().trim());
                existing.setPassword(pw);
                existing.setUrl(url.getText().trim());
                existing.setNotes(notes.getText());
                existing.setRecoveryCodes(codes);
                return existing;
            } finally {
                SecureMemory.wipe(pw);
                SecureMemory.wipe(codes);
                passHidden.clear();
                passVisible.clear();
                recovery.clear();
            }
        });

        return dialog.showAndWait();
    }

    private static Label labeled(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }
}
