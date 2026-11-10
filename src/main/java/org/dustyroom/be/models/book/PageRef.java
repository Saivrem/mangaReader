package org.dustyroom.be.models.book;

import java.util.Objects;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class PageRef {
    private PageKey key;
    private PictureMetadata metadata;

    @Builder
    public PageRef(PageKey key, PictureMetadata metadata) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(metadata, "metadata");

        this.key = key;
        this.metadata = metadata;
    }

    public PageRef setKey(PageKey key) {
        this.key = Objects.requireNonNull(key, "key");
        return this;
    }

    public PageRef setMetadata(PictureMetadata metadata) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        return this;
    }
}
