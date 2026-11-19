package com.houdinisecurity.domain;

import java.util.Objects;
import java.util.UUID;

public final class Tag {

    private final String id;
    private String name;

    public Tag(String id, String name) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.name = name == null ? "" : name;
    }

    public static Tag create(String name) {
        return new Tag(null, name);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag tag)) {
            return false;
        }
        return Objects.equals(id, tag.id);
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
