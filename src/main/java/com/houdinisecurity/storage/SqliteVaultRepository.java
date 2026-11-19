package com.houdinisecurity.storage;

import com.houdinisecurity.crypto.VaultCrypto;
import com.houdinisecurity.domain.EntryType;
import com.houdinisecurity.domain.Folder;
import com.houdinisecurity.domain.Tag;
import com.houdinisecurity.domain.VaultEntry;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SqliteVaultRepository implements VaultRepository {

    private final Connection connection;
    private final EntryCodec codec;

    public SqliteVaultRepository(Path dbPath, VaultCrypto vaultCrypto) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            this.connection.setAutoCommit(true);
            this.codec = new EntryCodec(vaultCrypto);
            FilePermissions.tryRestrict(dbPath);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot open vault database", e);
        }
    }

    @Override
    public void initSchema() {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS schema_meta (
                      key TEXT PRIMARY KEY,
                      value TEXT NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS folders (
                      id TEXT PRIMARY KEY,
                      name TEXT NOT NULL,
                      parent_id TEXT
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS tags (
                      id TEXT PRIMARY KEY,
                      name TEXT NOT NULL UNIQUE
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS entries (
                      id TEXT PRIMARY KEY,
                      type TEXT NOT NULL,
                      folder_id TEXT,
                      favorite INTEGER NOT NULL DEFAULT 0,
                      created_at INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL,
                      ciphertext BLOB NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS entry_tags (
                      entry_id TEXT NOT NULL,
                      tag_id TEXT NOT NULL,
                      PRIMARY KEY (entry_id, tag_id)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS app_settings (
                      key TEXT PRIMARY KEY,
                      value TEXT NOT NULL
                    )
                    """);
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO schema_meta(key, value) VALUES('format_version', ?)")) {
                ps.setString(1, String.valueOf(VaultCrypto.FORMAT_VERSION));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Schema init failed", e);
        }
    }

    @Override
    public List<VaultEntry> loadAllEntries(byte[] dek) {
        Map<String, List<String>> tagsByEntry = loadEntryTags();
        List<VaultEntry> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, type, folder_id, favorite, created_at, updated_at, ciphertext FROM entries")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    EntryType type = EntryType.valueOf(rs.getString(2));
                    String folderId = rs.getString(3);
                    boolean favorite = rs.getInt(4) != 0;
                    long created = rs.getLong(5);
                    long updated = rs.getLong(6);
                    byte[] ct = rs.getBytes(7);
                    VaultEntry entry = codec.decode(dek, id, type, folderId, favorite, created, updated, ct);
                    entry.setTagIds(tagsByEntry.getOrDefault(id, List.of()));
                    result.add(entry);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("loadAllEntries failed", e);
        }
        return result;
    }

    private Map<String, List<String>> loadEntryTags() {
        Map<String, List<String>> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT entry_id, tag_id FROM entry_tags")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("loadEntryTags failed", e);
        }
        return map;
    }

    @Override
    public void saveEntry(byte[] dek, VaultEntry entry) {
        byte[] ct = codec.encode(dek, entry);
        try {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO entries(id, type, folder_id, favorite, created_at, updated_at, ciphertext)
                    VALUES(?,?,?,?,?,?,?)
                    ON CONFLICT(id) DO UPDATE SET
                      type=excluded.type,
                      folder_id=excluded.folder_id,
                      favorite=excluded.favorite,
                      created_at=excluded.created_at,
                      updated_at=excluded.updated_at,
                      ciphertext=excluded.ciphertext
                    """)) {
                ps.setString(1, entry.getId());
                ps.setString(2, entry.getType().name());
                ps.setString(3, entry.getFolderId());
                ps.setInt(4, entry.isFavorite() ? 1 : 0);
                ps.setLong(5, entry.getCreatedAt());
                ps.setLong(6, entry.getUpdatedAt());
                ps.setBytes(7, ct);
                ps.executeUpdate();
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM entry_tags WHERE entry_id = ?")) {
                del.setString(1, entry.getId());
                del.executeUpdate();
            }
            try (PreparedStatement ins = connection.prepareStatement(
                    "INSERT INTO entry_tags(entry_id, tag_id) VALUES(?,?)")) {
                for (String tagId : entry.getTagIds()) {
                    ins.setString(1, entry.getId());
                    ins.setString(2, tagId);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("saveEntry failed", e);
        }
    }

    @Override
    public void deleteEntry(String entryId) {
        try {
            try (PreparedStatement delTags = connection.prepareStatement("DELETE FROM entry_tags WHERE entry_id = ?")) {
                delTags.setString(1, entryId);
                delTags.executeUpdate();
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM entries WHERE id = ?")) {
                del.setString(1, entryId);
                del.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("deleteEntry failed", e);
        }
    }

    @Override
    public List<Folder> listFolders() {
        List<Folder> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, name, parent_id FROM folders ORDER BY name")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Folder(rs.getString(1), rs.getString(2), rs.getString(3)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("listFolders failed", e);
        }
        return list;
    }

    @Override
    public void saveFolder(Folder folder) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO folders(id, name, parent_id) VALUES(?,?,?)
                ON CONFLICT(id) DO UPDATE SET name=excluded.name, parent_id=excluded.parent_id
                """)) {
            ps.setString(1, folder.getId());
            ps.setString(2, folder.getName());
            ps.setString(3, folder.getParentId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("saveFolder failed", e);
        }
    }

    @Override
    public void deleteFolder(String folderId) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM folders WHERE id = ?")) {
            ps.setString(1, folderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("deleteFolder failed", e);
        }
    }

    @Override
    public List<Tag> listTags() {
        List<Tag> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, name FROM tags ORDER BY name")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Tag(rs.getString(1), rs.getString(2)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("listTags failed", e);
        }
        return list;
    }

    @Override
    public void saveTag(Tag tag) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO tags(id, name) VALUES(?,?)
                ON CONFLICT(id) DO UPDATE SET name=excluded.name
                """)) {
            ps.setString(1, tag.getId());
            ps.setString(2, tag.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("saveTag failed", e);
        }
    }

    @Override
    public void deleteTag(String tagId) {
        try {
            try (PreparedStatement delLinks = connection.prepareStatement("DELETE FROM entry_tags WHERE tag_id = ?")) {
                delLinks.setString(1, tagId);
                delLinks.executeUpdate();
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM tags WHERE id = ?")) {
                del.setString(1, tagId);
                del.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("deleteTag failed", e);
        }
    }

    @Override
    public Optional<String> getSetting(String key) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT value FROM app_settings WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("getSetting failed", e);
        }
        return Optional.empty();
    }

    @Override
    public void putSetting(String key, String value) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO app_settings(key, value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("putSetting failed", e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("close failed", e);
        }
    }
}
