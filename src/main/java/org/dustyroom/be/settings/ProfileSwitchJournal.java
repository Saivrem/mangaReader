package org.dustyroom.be.settings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import lombok.experimental.UtilityClass;

/** Metadata for completing an explicitly requested move after an interrupted shutdown. */
@UtilityClass
class ProfileSwitchJournal {
    static Path file(Path standardDirectory) {
        return standardDirectory.resolve("profile-switch.properties");
    }

    static void begin(Path standardDirectory, Path source, Path target) throws IOException {
        Files.createDirectories(standardDirectory);
        if (Files.exists(file(standardDirectory))) {
            throw new IOException("An earlier profile switch must be recovered on startup first.");
        }
        Properties properties = new Properties();
        properties.setProperty("source", source.toAbsolutePath().normalize().toString());
        properties.setProperty("target", target.toAbsolutePath().normalize().toString());
        Path temporary = Files.createTempFile(standardDirectory, "profile-switch-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, "Pending user-requested profile move; not a database backup");
            }
            Files.move(temporary, file(standardDirectory), StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void recover(Path standardDirectory) throws IOException {
        Path journal = file(standardDirectory);
        if (!Files.exists(journal)) {
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(journal, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        Path source = Path.of(properties.getProperty("source"));
        Path target = Path.of(properties.getProperty("target"));
        if (!source.isAbsolute() || !target.isAbsolute() || !"reader.sqlite3".equals(source.getFileName().toString())
                || !"reader.sqlite3".equals(target.getFileName().toString())) {
            throw new IOException("Invalid pending profile switch.");
        }
        if (Files.isRegularFile(source)) {
            Files.delete(journal);
            return;
        }
        if (!Files.isRegularFile(target)) {
            throw new IOException("The interrupted profile move cannot be recovered; no database was created.");
        }
        ProfileManager.writePointer(standardDirectory, target.getParent());
        Files.delete(journal);
    }
}
