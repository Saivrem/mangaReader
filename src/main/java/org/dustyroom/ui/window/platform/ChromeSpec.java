package org.dustyroom.ui.window.platform;

import java.util.List;
import java.util.Objects;

/**
 * Immutable platform-specific presentation policy for custom window chrome.
 * Window commands and state transitions intentionally remain outside this model.
 */
public record ChromeSpec(
        DesktopPlatform platform,
        ControlPlacement controlPlacement,
        List<ControlRole> controlOrder,
        TitleAlignment titleAlignment,
        VisualStyle visualStyle,
        String maximizeAccessibleName
) {
    public ChromeSpec {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(controlPlacement, "controlPlacement");
        controlOrder = List.copyOf(Objects.requireNonNull(controlOrder, "controlOrder"));
        Objects.requireNonNull(titleAlignment, "titleAlignment");
        Objects.requireNonNull(visualStyle, "visualStyle");
        Objects.requireNonNull(maximizeAccessibleName, "maximizeAccessibleName");
        if (controlOrder.size() != ControlRole.values().length
                || !controlOrder.containsAll(List.of(ControlRole.values()))) {
            throw new IllegalArgumentException("Control order must contain each window control exactly once");
        }
    }

    public static ChromeSpec current() {
        return forPlatform(DesktopPlatform.current());
    }

    public static ChromeSpec forPlatform(DesktopPlatform platform) {
        return switch (Objects.requireNonNull(platform, "platform")) {
            case MACOS -> new ChromeSpec(
                    platform,
                    ControlPlacement.LEFT,
                    List.of(ControlRole.CLOSE, ControlRole.MINIMIZE, ControlRole.MAXIMIZE),
                    TitleAlignment.CENTER,
                    VisualStyle.MACOS_TRAFFIC_LIGHTS,
                    "Zoom"
            );
            case WINDOWS -> new ChromeSpec(
                    platform,
                    ControlPlacement.RIGHT,
                    List.of(ControlRole.MINIMIZE, ControlRole.MAXIMIZE, ControlRole.CLOSE),
                    TitleAlignment.LEADING,
                    VisualStyle.WINDOWS,
                    "Maximize"
            );
            case LINUX, OTHER -> neutral(platform);
        };
    }

    public static ChromeSpec neutral() {
        return neutral(DesktopPlatform.OTHER);
    }

    private static ChromeSpec neutral(DesktopPlatform platform) {
        return new ChromeSpec(
                platform,
                ControlPlacement.RIGHT,
                List.of(ControlRole.MINIMIZE, ControlRole.MAXIMIZE, ControlRole.CLOSE),
                TitleAlignment.LEADING,
                VisualStyle.NEUTRAL,
                "Maximize"
        );
    }

    public enum ControlPlacement {
        LEFT,
        RIGHT
    }

    public enum ControlRole {
        CLOSE,
        MINIMIZE,
        MAXIMIZE
    }

    public enum TitleAlignment {
        LEADING,
        CENTER
    }

    public enum VisualStyle {
        MACOS_TRAFFIC_LIGHTS,
        WINDOWS,
        NEUTRAL
    }
}
