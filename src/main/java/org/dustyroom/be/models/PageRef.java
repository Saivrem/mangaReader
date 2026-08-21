package org.dustyroom.be.models;

import java.util.Objects;

public record PageRef(PageKey key, PictureMetadata metadata) {
    public PageRef {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(metadata, "metadata");
    }
}
