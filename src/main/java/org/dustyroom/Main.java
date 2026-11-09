package org.dustyroom;

import org.dustyroom.ui.ImageViewer;
import org.dustyroom.ui.PlatformUi;
import org.dustyroom.ui.utils.UiUtils;
import org.dustyroom.persistence.ReaderSettings;
import org.dustyroom.persistence.ReaderStore;
import org.dustyroom.persistence.SqliteReaderStore;
import org.dustyroom.persistence.StoragePaths;
import org.dustyroom.ui.utils.DialogUtils;

import javax.swing.SwingUtilities;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        PlatformUi.configure();
        org.dustyroom.ui.BrandAssets.load();
        ReaderStore candidate = ReaderStore.disabled();
        ReaderSettings loaded = ReaderSettings.defaults();
        String error = null;
        // Initialize storage before scheduling Swing work; SQL never runs on the EDT.
        try {
            candidate = new SqliteReaderStore(StoragePaths.database());
            loaded = candidate.settings();
        } catch (RuntimeException exception) {
            try { candidate.close(); } catch (RuntimeException ignored) { /* initialization already failed */ }
            candidate = ReaderStore.disabled();
            error = "Cannot open the reader database. Reading is available, but settings and progress will not be saved.";
        }
        ReaderStore store = candidate;
        ReaderSettings settings = loaded;
        String startupError = error;
        SwingUtilities.invokeLater(() -> {
            UiUtils.installTheme(settings.theme());
            ImageViewer imageViewer = new ImageViewer(store, settings);
            if (startupError != null) DialogUtils.showError(imageViewer, startupError);
            if (args.length > 0) {
                imageViewer.openFile(new File(args[0]));
            }
        });
    }

}
