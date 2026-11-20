package com.houdinisecurity;

/**
 * Non-{@link javafx.application.Application} entrypoint so {@code java -cp ...}
 * works with JavaFX on the classpath (JDK 11+ otherwise requires modules).
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        HoudiniApp.main(args);
    }
}
