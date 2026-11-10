package org.dustyroom.ui.settings;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.state.ThemeMode;
import org.dustyroom.be.settings.ProfileManager;
import org.dustyroom.be.utils.StorageUtils;

public final class SettingsDialog extends JDialog {
    private final SettingsHost host;
    private final JComboBox<ThemeMode> theme = new JComboBox<>(ThemeMode.values());
    private final JComboBox<String> direction = new JComboBox<>(new String[]{"Manga · right to left", "Comics · left to right"});
    private final JCheckBox spread = new JCheckBox("Two-page spreads");
    private final JComboBox<String> scale = new JComboBox<>(new String[]{"Fit to window", "Fit width", "Fit height", "Original size · 100%"});
    private final JCheckBox thumbnails = new JCheckBox("Show thumbnails");
    private final JTextField library = new JTextField();
    private final JButton apply = new JButton("Apply");
    private final JButton ok = new JButton("OK");
    private final JButton cancel = new JButton("Cancel");
    private final JButton changeData = new JButton("Change…");
    private final JButton scan = new JButton("Scan / refresh…");
    private boolean busy;

    public SettingsDialog(Window owner, SettingsHost host) {
        super(owner, "Settings", ModalityType.APPLICATION_MODAL);
        this.host = host;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JList<String> categories = new JList<>(new String[]{"Appearance", "Reading", "Library", "Data"});
        categories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categories.setFixedCellHeight(36);
        categories.setPreferredSize(new Dimension(130, 250));
        categories.setSelectedIndex(0);
        CardLayout layout = new CardLayout();
        JPanel pages = new JPanel(layout);
        pages.add(appearance(), "Appearance");
        pages.add(reading(), "Reading");
        pages.add(library(), "Library");
        pages.add(data(), "Data");
        categories.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && categories.getSelectedValue() != null) {
                layout.show(pages, categories.getSelectedValue());
            }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(apply);
        buttons.add(ok);
        cancel.addActionListener(event -> dispose());
        apply.addActionListener(event -> save(false));
        ok.addActionListener(event -> save(true));
        content.add(categories, BorderLayout.WEST);
        content.add(pages, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(ok);
        load(host.currentSettings());
        setMinimumSize(new Dimension(760, 450));
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel appearance() {
        SettingsForm panel = new SettingsForm();
        panel.row(new JLabel("Theme"));
        panel.row(theme);
        panel.row(note("System follows your OS appearance using Ayu Light or Ayu Dark. If unavailable, the last detected theme is retained."));
        return top(panel);
    }

    private JPanel reading() {
        SettingsForm panel = new SettingsForm();
        panel.row(note("Defaults for newly opened books. Saved book direction and spread mode take priority."));
        panel.row(direction);
        panel.row(spread);
        panel.row(scale);
        panel.row(thumbnails);
        return top(panel);
    }

    private JPanel library() {
        SettingsForm panel = new SettingsForm();
        panel.row(new JLabel("Library folder"));
        panel.row(library);
        JButton browse = new JButton("Browse…");
        browse.addActionListener(event -> {
            Path selected = chooseDirectory("Library folder");
            if (selected != null) {
                library.setText(selected.toString());
            }
        });
        panel.row(browse);
        scan.addActionListener(event -> {
            if (library.getText().isBlank()) {
                error("Choose a library folder first.");
                return;
            }
            try {
                Path root = Path.of(library.getText()).toAbsolutePath().normalize();
                host.scanLibrary(root);
            } catch (RuntimeException exception) {
                error("Invalid library path: " + exception.getMessage());
            }
        });
        panel.row(scan);
        panel.row(note("Scanning opens an import preview. Apply saves the folder preference; it does not import or remove books."));
        return top(panel);
    }

    private JPanel data() {
        SettingsForm panel = new SettingsForm();
        Path directory = host.dataDirectory();
        JTextField path = new JTextField(directory == null ? "Storage unavailable" : directory.toString());
        path.setEditable(false);
        panel.row(new JLabel("Current data folder"));
        panel.row(path);
        JButton open = new JButton("Open folder");
        open.setEnabled(directory != null && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));
        open.addActionListener(event -> CompletableFuture.runAsync(() -> {
            try {
                Desktop.getDesktop().open(directory.toFile());
            } catch (IOException | RuntimeException exception) {
                SwingUtilities.invokeLater(() -> error("Cannot open the data folder: " + exception.getMessage()));
            }
        }));
        panel.row(open);
        changeData.setEnabled(directory != null && !StorageUtils.isDirectoryOverridden());
        changeData.addActionListener(event -> chooseProfile());
        panel.row(changeData);
        panel.row(note(StorageUtils.isDirectoryOverridden()
                ? "The data folder is fixed by the launch option yonaka.dataDir."
                : "Changing profiles closes the reader. Empty folders receive the current data; existing profiles are opened separately. Books are never moved."));
        return top(panel);
    }

    private void chooseProfile() {
        if (busy) {
            return;
        }
        Path selected = chooseDirectory("Choose data folder");
        if (selected == null) {
            return;
        }
        setBusy(true);
        CompletableFuture.supplyAsync(() -> ProfileManager.containsProfile(selected)).whenComplete((existing, failure) -> SwingUtilities.invokeLater(() -> {
            setBusy(false);
            if (failure != null) {
                error("Cannot inspect the selected folder.");
                return;
            }
            String message = existing
                    ? "Open the profile in this folder on next launch?\nThe current profile will stay in its current folder."
                    : "Move the current settings, history and library to this folder?";
            message += "\n\nUnapplied settings in this dialog will be discarded.\nYonaka Reader will close. Your books will not be moved.";
            if (JOptionPane.showConfirmDialog(this, message, "Change data folder", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
                host.switchDataDirectory(selected, existing, this::dispose);
            }
        }));
    }

    private void load(ReaderSettings settings) {
        theme.setSelectedItem(settings.getThemeMode());
        direction.setSelectedIndex(settings.getDefaultReadingMode() == ReadingMode.MANGA ? 0 : 1);
        spread.setSelected(settings.isDefaultTwoPageMode());
        scale.setSelectedIndex(switch (settings.getDefaultFitMode()) {
            case FIT_SCREEN -> 0;
            case FIT_WIDTH -> 1;
            case FIT_HEIGHT -> 2;
            case ORIGINAL -> 3;
        });
        thumbnails.setSelected(settings.isThumbnailsVisible());
        library.setText(settings.getLibraryDirectory());
    }

    private void save(boolean close) {
        if (busy) {
            return;
        }
        ReaderSettings selected = host.currentSettings().toBuilder()
                .themeMode((ThemeMode) theme.getSelectedItem())
                .defaultReadingMode(direction.getSelectedIndex() == 0 ? ReadingMode.MANGA : ReadingMode.COMICS)
                .defaultTwoPageMode(spread.isSelected())
                .defaultFitMode(switch (scale.getSelectedIndex()) {
                    case 1 -> FitMode.FIT_WIDTH;
                    case 2 -> FitMode.FIT_HEIGHT;
                    case 3 -> FitMode.ORIGINAL;
                    default -> FitMode.FIT_SCREEN;
                })
                .thumbnailsVisible(thumbnails.isSelected()).libraryDirectory(library.getText().strip()).build();
        setBusy(true);
        host.applySettings(selected, () -> {
            setBusy(false);
            if (close) {
                dispose();
            }
        }, message -> {
            setBusy(false);
            error(message);
        });
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        apply.setEnabled(!busy);
        ok.setEnabled(!busy);
        cancel.setEnabled(!busy);
        scan.setEnabled(!busy);
        changeData.setEnabled(!busy && host.dataDirectory() != null && !StorageUtils.isDirectoryOverridden());
        setDefaultCloseOperation(busy ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
    }

    private Path chooseDirectory(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Settings", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel top(JPanel content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private static JTextArea note(String text) {
        JTextArea note = new JTextArea(text, 3, 36);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setEditable(false);
        note.setOpaque(false);
        note.setFont(UIManager.getFont("Label.font"));
        return note;
    }
}
