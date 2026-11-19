package com.houdinisecurity.domain;

import java.util.Objects;
import java.util.UUID;

public final class Folder {

    private final String id;
    private String name;
    private String parentId;

    public Folder(String id, String name, String parentId) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.name = name == null ? "" : name;
        this.parentId = parentId;
    }

    public static Folder create(String name, String parentId) {
        return new Folder(null, name, parentId);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Folder folder)) {
            return false;
        }
        return Objects.equals(id, folder.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
