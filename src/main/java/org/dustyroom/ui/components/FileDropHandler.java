package org.dustyroom.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Only hands paths to the asynchronous loader; does not inspect the filesystem.
 */
public final class FileDropHandler extends TransferHandler {
    private final Consumer<File> open;
    private final Consumer<String> error;

    public FileDropHandler(Consumer<File> open, Consumer<String> error) {
        this.open = open;
        this.error = error;
    }

    public void installOn(Component component) {
        if (component instanceof JComponent swing) {
            swing.setTransferHandler(this);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installOn(child);
            }
        }
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return false;
        }
        if (support.isDrop()) {
            if ((support.getSourceDropActions() & COPY) == 0) {
                return false;
            }
            support.setDropAction(COPY);
        }
        return true;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        try {
            Object value = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (!(value instanceof List<?> files) || files.size() != 1 || !(files.getFirst() instanceof File file)) {
                report("Drop one image, ZIP archive, or folder at a time.");
                return false;
            }
            open.accept(file);
            return true;
        } catch (Exception exception) {
            report("Unable to read the dropped files. Try opening the source with Open instead.");
            return false;
        }
    }

    private void report(String message) {
        // Complete the native drop handshake before displaying a dialog.
        SwingUtilities.invokeLater(() -> error.accept(message));
    }
}
