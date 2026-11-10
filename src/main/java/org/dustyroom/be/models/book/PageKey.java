package org.dustyroom.be.models.book;

import java.util.Objects;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class PageKey {
    private final String sourceId;
    private final String pageId;

    @Builder
    public PageKey(String sourceId, String pageId) {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(pageId, "pageId");

        this.sourceId = sourceId;
        this.pageId = pageId;
    }

}
