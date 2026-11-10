package org.dustyroom.ui.settings;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.dustyroom.be.models.state.ReaderSettings;

public interface SettingsHost {
    ReaderSettings currentSettings();
    void applySettings(ReaderSettings settings, Runnable success, Consumer<String> failure);
    Path dataDirectory();
    void switchDataDirectory(Path target, boolean existing, Runnable accepted);
    void scanLibrary(Path directory);
}
