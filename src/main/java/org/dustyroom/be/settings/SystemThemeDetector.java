package org.dustyroom.be.settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.dustyroom.be.models.state.ViewerTheme;

/** Platform probes run on a background worker with a timeout, never on the EDT. */
public final class SystemThemeDetector {
    public Optional<ViewerTheme> detect() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.startsWith("mac") || os.startsWith("darwin")) {
            return command(true, "defaults", "read", "-g", "AppleInterfaceStyle")
                    .map(value -> value.contains("Dark") ? ViewerTheme.DARK : ViewerTheme.LIGHT);
        }
        if (os.startsWith("windows")) {
            return command("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme").flatMap(SystemThemeDetector::parseWindows);
        }
        Optional<String> colorScheme = command("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
        if (colorScheme.isPresent() && colorScheme.get().contains("prefer-dark")) {
            return Optional.of(ViewerTheme.DARK);
        }
        if (colorScheme.isPresent() && colorScheme.get().contains("prefer-light")) {
            return Optional.of(ViewerTheme.LIGHT);
        }
        return command("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
                .map(value -> value.toLowerCase(Locale.ROOT).contains("dark") ? ViewerTheme.DARK : ViewerTheme.LIGHT);
    }

    static Optional<ViewerTheme> parseWindows(String output) {
        if (output.matches("(?s).*AppsUseLightTheme\\s+REG_DWORD\\s+0x0\\s*")) {
            return Optional.of(ViewerTheme.DARK);
        }
        if (output.matches("(?s).*AppsUseLightTheme\\s+REG_DWORD\\s+0x1\\s*")) {
            return Optional.of(ViewerTheme.LIGHT);
        }
        return Optional.empty();
    }

    private Optional<String> command(String... command) {
        return command(false, command);
    }

    private Optional<String> command(boolean absentMeansLight, String... command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                return Optional.empty();
            }
            if (absentMeansLight && process.exitValue() == 1) {
                // macOS omits AppleInterfaceStyle in light mode.
                return Optional.of("Light");
            }
            if (process.exitValue() != 0) {
                return Optional.empty();
            }
            return Optional.of(new String(process.getInputStream().readNBytes(65536), StandardCharsets.UTF_8).strip());
        } catch (IOException exception) {
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (process != null) {
                process.destroyForcibly();
                try {
                    process.getInputStream().close();
                    process.getOutputStream().close();
                    process.getErrorStream().close();
                } catch (IOException ignored) {
                    // The process has already been terminated.
                }
            }
        }
    }
}
