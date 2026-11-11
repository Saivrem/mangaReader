package org.dustyroom.be.models.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ThemeMode {
    SYSTEM("System"),
    LIGHT("Ayu Light"),
    DARK("Ayu Dark");

    private final String label;

    @Override
    public String toString() {
        return label;
    }
}
