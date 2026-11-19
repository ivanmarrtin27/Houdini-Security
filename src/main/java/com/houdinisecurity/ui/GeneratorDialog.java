package com.houdinisecurity.ui;

import com.houdinisecurity.AppContext;
import com.houdinisecurity.service.PasswordGenerator;
import com.houdinisecurity.service.StrengthMeter;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public final class GeneratorDialog {

    private final AppContext ctx;

    public GeneratorDialog(AppContext ctx) {
        this.ctx = ctx;
    }

    public void show() {
        I18n i18n = ctx.i18n();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(i18n.t("menu.generator"));

        Spinner<Integer> length = new Spinner<>(4, 128, 20);
        CheckBox upper = new CheckBox(i18n.t("gen.upper"));
        upper.setSelected(true);
        CheckBox lower = new CheckBox(i18n.t("gen.lower"));
        lower.setSelected(true);
        CheckBox digits = new CheckBox(i18n.t("gen.digits"));
        digits.setSelected(true);
        CheckBox symbols = new CheckBox(i18n.t("gen.symbols"));
        symbols.setSelected(true);
        CheckBox ambiguous = new CheckBox(i18n.t("gen.excludeAmbiguous"));
        ambiguous.setSelected(true);
        Spinner<Integer> words = new Spinner<>(3, 12, 5);
        TextField result = new TextField();
        result.setEditable(false);
        Label strength = new Label();

        Button genPwd = new Button(i18n.t("gen.password"));
        Button genPhrase = new Button(i18n.t("gen.passphrase"));
        Button copy = new Button(i18n.t("action.copy"));

        Runnable updateStrength = () -> {
            char[] chars = result.getText().toCharArray();
            try {
                StrengthMeter.StrengthResult r = ctx.strengthMeter().evaluate(chars);
                strength.setText(i18n.t(r.labelKey()) + " (" + r.score0to100() + ")");
            } finally {
                java.util.Arrays.fill(chars, '\0');
            }
        };

        genPwd.setOnAction(e -> {
            PasswordGenerator.Options opts = new PasswordGenerator.Options(
                    length.getValue(), upper.isSelected(), lower.isSelected(),
                    digits.isSelected(), symbols.isSelected(), ambiguous.isSelected());
            result.setText(ctx.passwordGenerator().generate(opts));
            updateStrength.run();
        });
        genPhrase.setOnAction(e -> {
            result.setText(ctx.passwordGenerator().generatePassphrase(words.getValue(), "-"));
            updateStrength.run();
        });
        copy.setOnAction(e -> ctx.clipboardService().copy(
                result.getText(), ctx.vaultService().settings().getClipboardTimeoutSec()));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label(i18n.t("gen.length")), length);
        grid.addRow(1, upper, lower);
        grid.addRow(2, digits, symbols);
        grid.addRow(3, ambiguous);
        grid.addRow(4, new Label(i18n.t("gen.words")), words);

        VBox box = new VBox(10, grid, new javafx.scene.layout.HBox(8, genPwd, genPhrase, copy), result, strength);
        box.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
