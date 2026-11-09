package org.dustyroom.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Welcome and reading views share the same window and open action.
 */
public final class ReaderSurface extends JPanel {
    private final CardLayout cards = new CardLayout();
    private final JLabel status = new JLabel("JPG · PNG · GIF · ZIP", JLabel.CENTER);
    private boolean reading;

    public ReaderSurface(JScrollPane reader, Action open) {
        setLayout(cards);
        JPanel welcome = new JPanel(new GridBagLayout());
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        JLabel book = new JLabel(org.dustyroom.ui.BrandAssets.welcomeIcon());
        JLabel title = new JLabel("Yonaka Reader");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 32f));
        JLabel slogan = new JLabel("もう少し読んでいましょう");
        slogan.setFont(slogan.getFont().deriveFont(15f));
        JLabel subtitle = new JLabel("Drop an image, ZIP archive or folder here to start reading.");
        subtitle.setFont(subtitle.getFont().deriveFont(15f));
        JButton button = new JButton(open);
        button.setText("Open a book");
        button.setIcon(new ReaderIcon(ReaderIcon.Kind.OPEN));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "background: $Component.accentColor; foreground: #0D1017");
        button.setMaximumSize(new Dimension(190, 44));
        button.setPreferredSize(new Dimension(190, 44));
        button.getAccessibleContext().setAccessibleName("Open an image, ZIP archive or folder");
        for (Component item : new Component[]{
                book,
                Box.createVerticalStrut(24), title,
                Box.createVerticalStrut(24), slogan,
                Box.createVerticalStrut(10), subtitle, Box.createVerticalStrut(28), button,
                Box.createVerticalStrut(18), status}) {
            if (item instanceof javax.swing.JComponent component) component.setAlignmentX(CENTER_ALIGNMENT);
            content.add(item);
        }
        welcome.add(content);
        add(welcome, "welcome");
        add(reader, "reader");
        cards.show(this, "welcome");
    }

    public void showReading() {
        reading = true;
        cards.show(this, "reader");
    }

    public void setLoading(boolean loading) {
        if (!reading) status.setText(loading ? "Opening your book…" : "JPG · PNG · GIF · ZIP");
    }
}
