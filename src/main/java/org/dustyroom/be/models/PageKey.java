package org.dustyroom.be.models;

import java.util.Objects;

public record PageKey(String sourceId, String pageId) {
    public PageKey {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(pageId, "pageId");
    }
}
