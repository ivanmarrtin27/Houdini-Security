package com.houdinisecurity.domain;

public final class SecureNote extends VaultEntry {

    private String title;
    private String body;

    public SecureNote(String id, String folderId, boolean favorite, long createdAt, long updatedAt,
                      String title, String body) {
        super(id, folderId, favorite, createdAt, updatedAt);
        this.title = title == null ? "" : title;
        this.body = body == null ? "" : body;
    }

    public static SecureNote create(String title, String body) {
        long now = System.currentTimeMillis();
        return new SecureNote(null, null, false, now, now, title, body);
    }

    @Override
    public EntryType getType() {
        return EntryType.NOTE;
    }

    @Override
    public String displayTitle() {
        return title.isBlank() ? "(nota)" : title;
    }

    @Override
    public String searchableText() {
        return (title + " " + body).toLowerCase();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body == null ? "" : body;
    }

    public void wipeSecrets() {
        body = "";
    }

    @Override
    public String toString() {
        return "SecureNote{id=" + getId() + ", title=" + title + "}";
    }
}
