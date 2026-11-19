package com.houdinisecurity.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

public final class FilePermissions {

    private static final Logger log = LoggerFactory.getLogger(FilePermissions.class);

    private FilePermissions() {
    }

    public static void tryRestrict(Path path) {
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException e) {
            // Windows / non-POSIX — best effort only
        } catch (Exception e) {
            log.debug("Could not set POSIX 600 on {}", path);
        }
    }
}
