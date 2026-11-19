package com.houdinisecurity.ui;

import com.houdinisecurity.AppContext;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.nio.file.Path;

public final class UnlockView {

    private final AppContext ctx;
    private final ViewNavigator navigator;
    private final Path headerPath;

    public UnlockView(AppContext ctx, ViewNavigator navigator, Path headerPath) {
        this.ctx = ctx;
        this.navigator = navigator;
        this.headerPath = headerPath;
    }

    public BorderPane create() {
        I18n i18n = ctx.i18n();
        Label title = new Label(i18n.t("unlock.title"));
        title.getStyleClass().add("brand-title");
        Label pathLabel = new Label(headerPath.getFileName().toString());
        pathLabel.getStyleClass().add("muted");

        PasswordField master = new PasswordField();
        master.setPromptText(i18n.t("unlock.master"));

        Button unlock = new Button(i18n.t("unlock.action"));
        unlock.getStyleClass().add("accent");
        unlock.setDefaultButton(true);
        Button back = new Button(i18n.t("action.back"));

        unlock.setOnAction(e -> doUnlock(master));
        back.setOnAction(e -> {
            WelcomeView welcome = new WelcomeView(ctx, navigator);
            navigator.show(welcome.create(), i18n.t("welcome.title"));
        });

        VBox box = new VBox(14, title, pathLabel, master, unlock, back);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.setMaxWidth(420);
        BorderPane root = new BorderPane(box);
        root.getStyleClass().add("root-pane");
        return root;
    }

    private void doUnlock(PasswordField master) {
        I18n i18n = ctx.i18n();
        char[] pw = master.getText().toCharArray();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    ctx.vaultService().unlock(headerPath, pw);
                } finally {
                    java.util.Arrays.fill(pw, '\0');
                }
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            master.clear();
            ctx.autolockService().start();
            ctx.autolockService().bumpActivity();
            ctx.autolockService().setOnLockedUi(() -> {
                UnlockView again = new UnlockView(ctx, navigator, headerPath);
                navigator.show(again.create(), i18n.t("unlock.title"));
            });
            MainView main = new MainView(ctx, navigator);
            navigator.show(main.create(), i18n.t("main.title"));
        });
        task.setOnFailed(ev -> {
            Throwable t = task.getException();
            alert(i18n.t("error.badPassword") + (t.getMessage() != null ? "\n" + t.getMessage() : ""));
        });
        ctx.background().submit(task);
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
