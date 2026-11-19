package com.houdinisecurity.storage;

import com.houdinisecurity.domain.Folder;
import com.houdinisecurity.domain.Tag;
import com.houdinisecurity.domain.VaultEntry;

import java.util.List;
import java.util.Optional;

public interface VaultRepository extends AutoCloseable {

    void initSchema();

    List<VaultEntry> loadAllEntries(byte[] dek);

    void saveEntry(byte[] dek, VaultEntry entry);

    void deleteEntry(String entryId);

    List<Folder> listFolders();

    void saveFolder(Folder folder);

    void deleteFolder(String folderId);

    List<Tag> listTags();

    void saveTag(Tag tag);

    void deleteTag(String tagId);

    Optional<String> getSetting(String key);

    void putSetting(String key, String value);

    @Override
    void close();
}
