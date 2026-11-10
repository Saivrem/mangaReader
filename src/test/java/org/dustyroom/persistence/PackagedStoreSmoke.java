package org.dustyroom.persistence;

import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.state.ViewerTheme;
import org.dustyroom.be.persistence.ReaderStore;
import org.dustyroom.be.persistence.SqliteReaderStore;

import java.nio.file.Path;

/**
 * Run against the fat JAR to verify JDBC service discovery and native-library packaging.
 */
public final class PackagedStoreSmoke {
    public static void main(String[] args) {
        Path database = Path.of("build", "packaged-smoke.sqlite3");
        ReaderSettings expected = new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_WIDTH, 1);
        try (ReaderStore store = new SqliteReaderStore(database)) {
            store.saveSettings(expected);
        }
        try (ReaderStore store = new SqliteReaderStore(database)) {
            if (!expected.equals(store.settings())) throw new AssertionError("Packaged SQLite round trip failed");
        }
        System.out.println("Packaged SQLite round trip passed");
    }
}
