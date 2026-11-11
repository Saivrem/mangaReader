package org.dustyroom.be.models.book;

import lombok.Builder;

import java.util.Objects;

public record PageKey(String sourceId, String pageId) {
    @Builder
    public PageKey {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(pageId, "pageId");

    }

}
