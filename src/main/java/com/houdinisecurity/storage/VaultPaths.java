package com.houdinisecurity.storage;

import java.nio.file.Path;

public final class VaultPaths {

    private VaultPaths() {
    }

    public static Path dbPath(Path headerPath) {
        return headerPath.resolveSibling(headerPath.getFileName().toString() + ".db");
    }

    public static Path backupPath(Path headerPath) {
        return headerPath.resolveSibling(headerPath.getFileName().toString() + ".bak");
    }
}
