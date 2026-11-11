package org.dustyroom.be.models.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class PageRef {
    @NonNull PageKey key;
    @NonNull PictureMetadata metadata;
}
