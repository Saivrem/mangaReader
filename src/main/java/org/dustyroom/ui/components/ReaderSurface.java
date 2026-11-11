package org.dustyroom.ui.components;

import org.dustyroom.be.models.view.RecentBook;
import org.dustyroom.ui.utils.BrandAssets;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Welcome and reading views share the same window and open action.
 */
public final class ReaderSurface extends JPanel {
    private final CardLayout cards = new CardLayout();
    private final JLabel status = new JLabel("JPG · PNG · GIF · ZIP", JLabel.CENTER);
    private final RecentBooksPanel recent = new RecentBooksPanel();
    private final JPanel hero = new JPanel();
    private final JPanel compact = new JPanel(new BorderLayout(24, 0));
    private final JTabbedPane home = new JTabbedPane();
    private boolean reading;

    public ReaderSurface(JScrollPane reader, Action open) {
        setLayout(cards);
        JPanel welcome = new JPanel(new BorderLayout());
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Yonaka Reader");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 32f));
        JLabel slogan = new JLabel("もう少し読んでいましょう");
        slogan.setFont(slogan.getFont().deriveFont(15f));
        JLabel subtitle = new JLabel("Drop an image, ZIP archive or folder here to start reading.");
        subtitle.setFont(subtitle.getFont().deriveFont(15f));
        for (Component item : new Component[]{new JLabel(BrandAssets.welcomeIcon()),
                Box.createVerticalStrut(24), title, Box.createVerticalStrut(16), slogan,
                Box.createVerticalStrut(10), subtitle, Box.createVerticalStrut(24), openButton(open)}) {
            if (item instanceof JComponent component) {
                component.setAlignmentX(CENTER_ALIGNMENT);
            }
            hero.add(item);
        }
        JPanel identity = new JPanel(new BorderLayout(14, 0));
        identity.add(new JLabel(BrandAssets.aboutIcon()), BorderLayout.WEST);
        JPanel words = new JPanel(new GridLayout(2, 1));
        JLabel compactTitle = new JLabel("Yonaka Reader");
        compactTitle.setFont(compactTitle.getFont().deriveFont(Font.BOLD, 24f));
        words.add(compactTitle);
        words.add(new JLabel("もう少し読んでいましょう"));
        identity.add(words, BorderLayout.CENTER);
        compact.add(identity, BorderLayout.CENTER);
        compact.add(openButton(open), BorderLayout.EAST);
        compact.setMaximumSize(new Dimension(760, 72));
        compact.setVisible(false);
        for (JComponent component : new JComponent[]{hero, compact, status, recent}) {
            component.setAlignmentX(CENTER_ALIGNMENT);
        }
        content.add(hero);
        content.add(compact);
        content.add(Box.createVerticalStrut(16));
        content.add(status);
        content.add(Box.createVerticalStrut(24));
        content.add(recent);
        // Anchor at the top so the first row is visible even in the smallest window.
        welcome.add(content, BorderLayout.NORTH);
        JScrollPane welcomeScroll = new JScrollPane(welcome);
        welcomeScroll.setBorder(BorderFactory.createEmptyBorder());
        welcomeScroll.getVerticalScrollBar().setUnitIncrement(24);
        home.addTab("Recently read", welcomeScroll);
        add(home, "welcome");
        add(reader, "reader");
        cards.show(this, "welcome");
    }

    private static JButton openButton(Action open) {
        JButton button = new JButton(open);
        button.setText("Open a book");
        button.setIcon(new ReaderIcon(ReaderIconKind.OPEN));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "background: $Component.accentColor; foreground: #0D1017");
        button.setMaximumSize(new Dimension(190, 44));
        button.setPreferredSize(new Dimension(190, 44));
        button.getAccessibleContext().setAccessibleName("Open an image, ZIP archive or folder");
        return button;
    }

    public void setLibrary(JComponent library, Runnable refresh) {
        home.addTab("Library", library);
        home.addChangeListener(_ -> {
            if (home.getSelectedComponent() == library) {
                refresh.run();
            }
        });
    }

    public void setRecentGrouping(boolean grouped, Consumer<Boolean> change) {
        recent.setGrouping(grouped, change);
    }

    public void setRecentHandlers(Consumer<String> open, Consumer<String> remove) {
        recent.setHandlers(open, remove);
    }

    public void showRecentBooks(List<RecentBook> books) {
        hero.setVisible(books.isEmpty());
        compact.setVisible(!books.isEmpty());
        recent.showBooks(books);
    }

    public void showWelcome() {
        reading = false;
        setLoading(false);
        cards.show(this, "welcome");
    }

    public void showReading() {
        reading = true;
        cards.show(this, "reader");
    }

    public void setLoading(boolean loading) {
        if (!reading) {
            status.setText(loading ? "Opening your book…" : "JPG · PNG · GIF · ZIP");
        }
    }
}
