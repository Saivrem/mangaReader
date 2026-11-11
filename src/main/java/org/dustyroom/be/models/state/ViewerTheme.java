package org.dustyroom.be.models.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ViewerTheme {
    DARK("Ayu Dark", "ayu-dark.properties"),
    LIGHT("Ayu Light", "ayu-light.properties");

    private final String label;
    private final String resource;
}
