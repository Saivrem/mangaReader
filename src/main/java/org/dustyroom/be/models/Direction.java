package org.dustyroom.be.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Direction {
    NEXT(1),
    PREV(-1);

    private final int shift;
}
