package com.houdinisecurity.service;

import com.houdinisecurity.crypto.KdfAlgorithm;
import com.houdinisecurity.crypto.VaultCrypto;
import com.houdinisecurity.domain.InvalidMasterPasswordException;
import com.houdinisecurity.domain.PasswordEntry;
import com.houdinisecurity.domain.VaultLockedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultServiceIntegrationTest {

    @TempDir
    Path temp;

    @Test
    void createUnlockCrudAndBadPassword() throws Exception {
        VaultService vault = new VaultService(new VaultCrypto());
        Path header = temp.resolve("test.houdini");
        char[] master = "master-pass-123".toCharArray();
        try {
            vault.createVault(header, master, KdfAlgorithm.ARGON2ID);
            assertTrue(vault.isUnlocked());
            char[] pw = "S3cret!".toCharArray();
            char[] codes = "CODE-ONE\nCODE-TWO".toCharArray();
            try {
                PasswordEntry entry = PasswordEntry.create(
                        "example.com", "user", pw, "https://example.com", "n", codes);
                vault.saveEntry(entry);
                assertEquals(1, vault.listEntries().size());
            } finally {
                Arrays.fill(pw, '\0');
                Arrays.fill(codes, '\0');
            }
            vault.lock();
            assertThrows(VaultLockedException.class, vault::listEntries);

            char[] bad = "wrong-password".toCharArray();
            try {
                assertThrows(InvalidMasterPasswordException.class, () -> vault.unlock(header, bad));
            } finally {
                Arrays.fill(bad, '\0');
            }

            vault.unlock(header, master);
            assertEquals(1, vault.listEntries().size());
            PasswordEntry loaded = (PasswordEntry) vault.listEntries().getFirst();
            char[] loadedPw = loaded.getPasswordCopy();
            char[] loadedCodes = loaded.getRecoveryCodesCopy();
            try {
                assertEquals("S3cret!", new String(loadedPw));
                assertEquals("CODE-ONE\nCODE-TWO", new String(loadedCodes));
            } finally {
                Arrays.fill(loadedPw, '\0');
                Arrays.fill(loadedCodes, '\0');
            }
        } finally {
            if (vault.isUnlocked()) {
                vault.lock();
            }
            Arrays.fill(master, '\0');
        }
    }

    @Test
    void changeMasterPasswordKeepsData() throws Exception {
        VaultService vault = new VaultService(new VaultCrypto());
        Path header = temp.resolve("chg.houdini");
        char[] oldMaster = "old-master-999".toCharArray();
        char[] newMaster = "new-master-999".toCharArray();
        try {
            vault.createVault(header, oldMaster, KdfAlgorithm.ARGON2ID);
            char[] pw = "keep-me".toCharArray();
            try {
                vault.saveEntry(PasswordEntry.create("site", "u", pw, "", ""));
            } finally {
                Arrays.fill(pw, '\0');
            }
            vault.changeMasterPassword(oldMaster, newMaster, KdfAlgorithm.ARGON2ID);
            vault.lock();
            vault.unlock(header, newMaster);
            assertEquals(1, vault.listEntries().size());
        } finally {
            if (vault.isUnlocked()) {
                vault.lock();
            }
            Arrays.fill(oldMaster, '\0');
            Arrays.fill(newMaster, '\0');
        }
    }
}
