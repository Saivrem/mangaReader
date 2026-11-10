package org.dustyroom;

import java.io.File;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.state.ThemeMode;
import org.dustyroom.be.persistence.ReaderStore;
import org.dustyroom.be.persistence.RecoverableReaderStore;
import org.dustyroom.be.persistence.SqliteReaderStore;
import org.dustyroom.be.settings.SystemThemeDetector;
import org.dustyroom.be.utils.StorageUtils;
import org.dustyroom.ui.BrandAssets;
import org.dustyroom.ui.ImageViewer;
import org.dustyroom.ui.PlatformUi;
import org.dustyroom.ui.utils.DialogUtils;
import org.dustyroom.ui.utils.UiUtils;

public class Main {

    public static void main(String[] args) {
        PlatformUi.configure();
        BrandAssets.load();
        ReaderStore candidate = ReaderStore.disabled();
        ReaderSettings loaded = ReaderSettings.defaults();
        String error = null;
        Path database = null;
        // Initialize storage before scheduling Swing work; SQL never runs on the EDT.
        try {
            database = StorageUtils.database();
            candidate = new SqliteReaderStore(database);
            loaded = candidate.settings();
        } catch (RuntimeException exception) {
            try {
                candidate.close();
            } catch (RuntimeException ignored) { /* initialization already failed */ }
            candidate = database == null ? ReaderStore.disabled() : new RecoverableReaderStore(database);
            error = exception.getMessage() + "\n\nCannot open the reader database. Reading is available, but settings and progress will not be saved.\n\n"
                    + "If this data belongs to a newer version, open it with that version to keep using it.\n"
                    + (database == null
                    ? "The selected data folder is unavailable. Reconnect it and restart the reader."
                    : "To start fresh, use Options → Reset Application. Reset requires confirmation.");
        }
        if (loaded.getThemeMode() == ThemeMode.SYSTEM) {
            loaded.setTheme(new SystemThemeDetector().detect().orElse(loaded.getTheme()));
        }
        ReaderStore store = candidate;
        ReaderSettings settings = loaded;
        String startupError = error;
        SwingUtilities.invokeLater(() -> {
            UiUtils.installTheme(settings.getTheme());
            ImageViewer imageViewer = new ImageViewer(store, settings);
            if (startupError != null) {
                DialogUtils.showError(imageViewer, startupError);
            }
            if (args.length > 0) {
                imageViewer.openFile(new File(args[0]));
            }
        });
    }

}
