package com.houdinisecurity.ui;

import com.houdinisecurity.AppContext;
import com.houdinisecurity.crypto.KdfAlgorithm;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;

public final class WelcomeView {

    private final AppContext ctx;
    private final ViewNavigator navigator;

    public WelcomeView(AppContext ctx, ViewNavigator navigator) {
        this.ctx = ctx;
        this.navigator = navigator;
    }

    public BorderPane create() {
        I18n i18n = ctx.i18n();
        Label brand = new Label("Houdini Security");
        brand.getStyleClass().add("brand-title");
        Label subtitle = new Label(i18n.t("welcome.subtitle"));
        subtitle.getStyleClass().add("muted");

        PasswordField master = new PasswordField();
        master.setPromptText(i18n.t("welcome.master"));
        PasswordField confirm = new PasswordField();
        confirm.setPromptText(i18n.t("welcome.confirm"));

        ComboBox<String> kdf = new ComboBox<>();
        kdf.getItems().addAll("Argon2id", "PBKDF2");
        kdf.getSelectionModel().selectFirst();

        Button create = new Button(i18n.t("welcome.create"));
        create.getStyleClass().add("accent");
        create.setDefaultButton(true);
        Button open = new Button(i18n.t("welcome.open"));
        Button importBackup = new Button(i18n.t("welcome.importBackup"));

        create.setOnAction(e -> createVault(master, confirm, kdf.getValue()));
        open.setOnAction(e -> openVault());
        importBackup.setOnAction(e -> importBackup());

        VBox center = new VBox(16, brand, subtitle, master, confirm, kdf,
                new HBox(12, create, open, importBackup));
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));
        center.setMaxWidth(480);

        BorderPane root = new BorderPane(center);
        root.getStyleClass().add("root-pane");
        return root;
    }

    private void createVault(PasswordField master, PasswordField confirm, String kdfLabel) {
        I18n i18n = ctx.i18n();
        char[] a = master.getText().toCharArray();
        char[] b = confirm.getText().toCharArray();
        try {
            if (a.length < 8) {
                alert(i18n.t("error.masterShort"));
                return;
            }
            if (!java.util.Arrays.equals(a, b)) {
                alert(i18n.t("error.masterMismatch"));
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle(i18n.t("welcome.create"));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Houdini", "*.houdini"));
            File file = chooser.showSaveDialog(navigator.stage());
            if (file == null) {
                return;
            }
            Path path = file.toPath();
            if (!path.toString().endsWith(".houdini")) {
                path = Path.of(path + ".houdini");
            }
            KdfAlgorithm algorithm = "PBKDF2".equals(kdfLabel)
                    ? KdfAlgorithm.PBKDF2_HMAC_SHA256 : KdfAlgorithm.ARGON2ID;
            Path finalPath = path;
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ctx.vaultService().createVault(finalPath, a, algorithm);
                    return null;
                }
            };
            task.setOnSucceeded(ev -> {
                master.clear();
                confirm.clear();
                showMain();
            });
            task.setOnFailed(ev -> alert(i18n.t("error.generic") + ": " + task.getException().getMessage()));
            ctx.background().submit(task);
        } finally {
            java.util.Arrays.fill(a, '\0');
            java.util.Arrays.fill(b, '\0');
        }
    }

    private void openVault() {
        I18n i18n = ctx.i18n();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n.t("welcome.open"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Houdini", "*.houdini"));
        File file = chooser.showOpenDialog(navigator.stage());
        if (file == null) {
            return;
        }
        UnlockView unlock = new UnlockView(ctx, navigator, file.toPath());
        navigator.show(unlock.create(), i18n.t("unlock.title"));
    }

    private void importBackup() {
        I18n i18n = ctx.i18n();
        FileChooser in = new FileChooser();
        in.setTitle(i18n.t("welcome.importBackup"));
        in.getExtensionFilters().add(new FileChooser.ExtensionFilter("Houdini Backup", "*.hbak"));
        File backup = in.showOpenDialog(navigator.stage());
        if (backup == null) {
            return;
        }
        FileChooser out = new FileChooser();
        out.setTitle(i18n.t("welcome.create"));
        out.getExtensionFilters().add(new FileChooser.ExtensionFilter("Houdini", "*.houdini"));
        File target = out.showSaveDialog(navigator.stage());
        if (target == null) {
            return;
        }
        PasswordDialog dialog = new PasswordDialog(i18n.t("backup.passphrase"), i18n);
        dialog.showAndWait().ifPresent(pass -> {
            char[] chars = pass.toCharArray();
            try {
                Path targetPath = target.toPath();
                if (!targetPath.toString().endsWith(".houdini")) {
                    targetPath = Path.of(targetPath + ".houdini");
                }
                ctx.backupService().importBackup(backup.toPath(), chars, targetPath);
                alert(i18n.t("backup.importOk"));
            } catch (Exception ex) {
                alert(i18n.t("error.generic") + ": " + ex.getMessage());
            } finally {
                java.util.Arrays.fill(chars, '\0');
            }
        });
    }

    private void showMain() {
        ctx.autolockService().start();
        ctx.autolockService().setOnLockedUi(() -> {
            Path path = ctx.vaultService().currentHeaderPath();
            if (path != null) {
                UnlockView unlock = new UnlockView(ctx, navigator, path);
                navigator.show(unlock.create(), ctx.i18n().t("unlock.title"));
            } else {
                navigator.show(create(), ctx.i18n().t("welcome.title"));
            }
        });
        MainView main = new MainView(ctx, navigator);
        navigator.show(main.create(), ctx.i18n().t("main.title"));
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
