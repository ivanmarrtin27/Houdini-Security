package com.houdinisecurity.ui;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.scene.Scene;

public final class ThemeManager {

    private ThemeManager() {
    }

    public static void apply(Scene scene, String theme) {
        boolean dark = theme == null || theme.equalsIgnoreCase("dark");
        Application.setUserAgentStylesheet(dark ? new CupertinoDark().getUserAgentStylesheet()
                : new CupertinoLight().getUserAgentStylesheet());
        scene.getStylesheets().removeIf(s -> s.contains("houdini.css"));
        var css = ThemeManager.class.getResource("/styles/houdini.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }
}
