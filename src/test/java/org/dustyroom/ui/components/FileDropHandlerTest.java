package org.dustyroom.ui.components;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class FileDropHandlerTest {
    @Test
    void handsOffSinglePathsWithoutRequiringFilesystemAccess() throws Exception {
        List<File> opened = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            FileDropHandler handler = new FileDropHandler(opened::add, message -> fail(message));
            for (String path : new String[]{"chapter/page.png", "日本語 book.zip", "chapter folder"}) {
                File file = new File(path);
                assertTrue(handler.importData(support(List.of(file))));
                assertEquals(file, opened.get(opened.size() - 1));
            }
            assertEquals(3, opened.size());
        });
    }

    @Test
    void rejectsMultipleFilesAndTextWithoutOpeningAnything() throws Exception {
        List<File> opened = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            FileDropHandler handler = new FileDropHandler(opened::add, errors::add);
            assertFalse(handler.importData(support(List.of(new File("a.zip"), new File("b.zip")))));
            assertFalse(handler.importData(new TransferHandler.TransferSupport(new JPanel(), new StringSelection("a.zip"))));
            assertFalse(handler.importData(support(List.of())));
        });
        SwingUtilities.invokeAndWait(() -> {}); // drain the deferred drop feedback
        assertTrue(opened.isEmpty());
        assertEquals(2, errors.size());
    }

    private static TransferHandler.TransferSupport support(List<File> files) {
        return new TransferHandler.TransferSupport(new JPanel(), new Transferable() {
            public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.javaFileListFlavor}; }
            public boolean isDataFlavorSupported(DataFlavor flavor) { return flavor.equals(DataFlavor.javaFileListFlavor); }
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
                return files;
            }
        });
    }
}
