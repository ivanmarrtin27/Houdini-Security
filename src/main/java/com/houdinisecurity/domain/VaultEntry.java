package com.houdinisecurity.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class VaultEntry {

    private final String id;
    private String folderId;
    private boolean favorite;
    private final long createdAt;
    private long updatedAt;
    private final List<String> tagIds = new ArrayList<>();

    protected VaultEntry(String id, String folderId, boolean favorite, long createdAt, long updatedAt) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.folderId = folderId;
        this.favorite = favorite;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public abstract EntryType getType();

    public abstract String displayTitle();

    public abstract String searchableText();

    public String getId() {
        return id;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void touch(long now) {
        this.updatedAt = now;
    }

    public List<String> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<String> tags) {
        tagIds.clear();
        if (tags != null) {
            tagIds.addAll(tags);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VaultEntry that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
