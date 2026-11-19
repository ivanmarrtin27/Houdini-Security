package com.houdinisecurity.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.function.Supplier;

public final class ViewNavigator {

    private final Stage stage;
    private final Supplier<String> themeSupplier;

    public ViewNavigator(Stage stage, Supplier<String> themeSupplier) {
        this.stage = stage;
        this.themeSupplier = themeSupplier;
    }

    public void show(Parent root, String title) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1100, 720);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        ThemeManager.apply(scene, themeSupplier.get());
        stage.setTitle(title);
        stage.show();
    }

    public Stage stage() {
        return stage;
    }
}
