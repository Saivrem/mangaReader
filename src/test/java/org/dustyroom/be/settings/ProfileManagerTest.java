package org.dustyroom.be.settings;

import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.state.ThemeMode;
import org.dustyroom.be.persistence.SqliteReaderStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProfileManagerTest {
    @TempDir
    Path root;

    private Path profile(String name, ThemeMode theme) {
        Path database = root.resolve(name).resolve("reader.sqlite3");
        try (SqliteReaderStore store = new SqliteReaderStore(database)) {
            store.saveSettings(ReaderSettings.defaults().setThemeMode(theme));
        }
        return database;
    }

    @Test
    void movesProfileAndReopensItsSettingsWithoutCopy() throws Exception {
        Path source = profile("standard", ThemeMode.SYSTEM);
        Path target = root.resolve("selected");
        ProfileManager.switchProfile(source.getParent(), source, target, false);
        assertFalse(Files.exists(source));
        Path selected = ProfileManager.resolve(source.getParent(), null);
        assertEquals(target.resolve("reader.sqlite3"), selected);
        try (SqliteReaderStore store = new SqliteReaderStore(selected)) {
            assertEquals(ThemeMode.SYSTEM, store.settings().getThemeMode());
        }
        assertFalse(Files.exists(ProfileSwitchJournal.file(source.getParent())));
    }

    @Test
    void opensExistingProfileWithoutMergingOrChangingEitherDatabase() throws Exception {
        Path source = profile("standard", ThemeMode.SYSTEM);
        Path target = profile("existing", ThemeMode.LIGHT);
        byte[] beforeSource = Files.readAllBytes(source);
        byte[] beforeTarget = Files.readAllBytes(target);
        ProfileManager.switchProfile(source.getParent(), source, target.getParent(), true);
        assertEquals(target, ProfileManager.resolve(source.getParent(), null));
        assertArrayEquals(beforeSource, Files.readAllBytes(source));
        assertArrayEquals(beforeTarget, Files.readAllBytes(target));
    }

    @Test
    void rollsBackMoveWhenSelectorCannotBeWritten() throws Exception {
        Path source = profile("standard", ThemeMode.SYSTEM);
        Files.createDirectory(source.getParent().resolve("profile.path"));
        Path target = root.resolve("selected");
        assertThrows(IOException.class, () -> ProfileManager.switchProfile(source.getParent(), source, target, false));
        assertTrue(Files.isRegularFile(source));
        assertFalse(Files.exists(target.resolve("reader.sqlite3")));
        try (SqliteReaderStore store = new SqliteReaderStore(source)) {
            assertEquals(ThemeMode.SYSTEM, store.settings().getThemeMode());
        }
    }

    @Test
    void completesInterruptedMoveUsingMetadataOnly() throws Exception {
        Path source = profile("standard", ThemeMode.SYSTEM);
        Path target = Files.createDirectory(root.resolve("selected")).resolve("reader.sqlite3");
        ProfileSwitchJournal.begin(source.getParent(), source, target);
        Files.move(source, target);
        assertEquals(target, ProfileManager.resolve(source.getParent(), null));
        assertFalse(Files.exists(ProfileSwitchJournal.file(source.getParent())));
    }

    @Test
    void missingSelectedProfileDoesNotCreateEmptyReplacementAndOverrideStillWorks() throws Exception {
        Path standard = Files.createDirectory(root.resolve("standard"));
        ProfileManager.writePointer(standard, root.resolve("missing"));
        assertThrows(IllegalStateException.class, () -> ProfileManager.resolve(standard, null));
        assertFalse(Files.exists(standard.resolve("reader.sqlite3")));
        assertEquals(root.resolve("explicit/reader.sqlite3"), ProfileManager.resolve(standard, root.resolve("explicit").toString()));
    }
}
