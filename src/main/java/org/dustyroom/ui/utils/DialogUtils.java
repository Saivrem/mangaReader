package org.dustyroom.ui.utils;

import lombok.experimental.UtilityClass;
import org.dustyroom.be.utils.Constants;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.Component;

@UtilityClass
public class DialogUtils {
    public static void showAbout(JFrame parent) {
        JOptionPane.showMessageDialog(parent,
                Constants.ABOUT.formatted("0.8.0"),
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
