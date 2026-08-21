package org.dustyroom.ui.window.platform;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChromeSpecTest {
    @Test
    void macOsUsesLeftTrafficLightsAndAWindowCenteredTitle() {
        ChromeSpec spec = ChromeSpec.forPlatform(DesktopPlatform.MACOS);

        assertAll(
                () -> assertEquals(ChromeSpec.ControlPlacement.LEFT, spec.controlPlacement()),
                () -> assertEquals(
                        List.of(
                                ChromeSpec.ControlRole.CLOSE,
                                ChromeSpec.ControlRole.MINIMIZE,
                                ChromeSpec.ControlRole.MAXIMIZE
                        ),
                        spec.controlOrder()
                ),
                () -> assertEquals(ChromeSpec.TitleAlignment.CENTER, spec.titleAlignment()),
                () -> assertEquals(ChromeSpec.VisualStyle.MACOS_TRAFFIC_LIGHTS, spec.visualStyle()),
                () -> assertEquals("Zoom", spec.maximizeAccessibleName())
        );
    }

    @Test
    void windowsUsesRightAlignedSystemControlOrder() {
        ChromeSpec spec = ChromeSpec.forPlatform(DesktopPlatform.WINDOWS);

        assertAll(
                () -> assertEquals(ChromeSpec.ControlPlacement.RIGHT, spec.controlPlacement()),
                () -> assertEquals(
                        List.of(
                                ChromeSpec.ControlRole.MINIMIZE,
                                ChromeSpec.ControlRole.MAXIMIZE,
                                ChromeSpec.ControlRole.CLOSE
                        ),
                        spec.controlOrder()
                ),
                () -> assertEquals(ChromeSpec.TitleAlignment.LEADING, spec.titleAlignment()),
                () -> assertEquals(ChromeSpec.VisualStyle.WINDOWS, spec.visualStyle()),
                () -> assertEquals("Maximize", spec.maximizeAccessibleName())
        );
    }

    @Test
    void linuxAndUnknownPlatformsUseTheNeutralFallback() {
        for (DesktopPlatform platform : List.of(DesktopPlatform.LINUX, DesktopPlatform.OTHER)) {
            ChromeSpec spec = ChromeSpec.forPlatform(platform);
            assertEquals(platform, spec.platform());
            assertEquals(ChromeSpec.VisualStyle.NEUTRAL, spec.visualStyle());
            assertEquals(ChromeSpec.ControlPlacement.RIGHT, spec.controlPlacement());
        }
    }

    @Test
    void rejectsAControlOrderThatDoesNotContainEveryRoleExactlyOnce() {
        assertThrows(IllegalArgumentException.class, () -> new ChromeSpec(
                DesktopPlatform.OTHER,
                ChromeSpec.ControlPlacement.RIGHT,
                List.of(
                        ChromeSpec.ControlRole.CLOSE,
                        ChromeSpec.ControlRole.CLOSE,
                        ChromeSpec.ControlRole.MAXIMIZE
                ),
                ChromeSpec.TitleAlignment.LEADING,
                ChromeSpec.VisualStyle.NEUTRAL,
                "Maximize"
        ));
    }
}
