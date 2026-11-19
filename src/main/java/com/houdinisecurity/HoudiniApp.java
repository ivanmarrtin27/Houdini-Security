package com.houdinisecurity;

import com.houdinisecurity.ui.I18n;
import com.houdinisecurity.ui.ViewNavigator;
import com.houdinisecurity.ui.WelcomeView;
import javafx.application.Application;
import javafx.stage.Stage;

public final class HoudiniApp extends Application {

    private AppContext context;

    @Override
    public void start(Stage stage) {
        context = new AppContext();
        I18n i18n = context.i18n();
        ViewNavigator navigator = new ViewNavigator(stage, () ->
                context.vaultService().isUnlocked()
                        ? context.vaultService().settings().getTheme()
                        : "dark");
        stage.setOnCloseRequest(e -> context.shutdown());
        WelcomeView welcome = new WelcomeView(context, navigator);
        navigator.show(welcome.create(), i18n.t("welcome.title"));
    }

    @Override
    public void stop() {
        if (context != null) {
            context.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
