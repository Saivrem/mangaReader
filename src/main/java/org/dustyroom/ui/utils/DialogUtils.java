package org.dustyroom.ui.utils;

import lombok.experimental.UtilityClass;
import org.dustyroom.be.utils.Constants;

import javax.swing.*;
import java.awt.*;

@UtilityClass
public class DialogUtils {
    public static void showAbout(JFrame parent) {
        JOptionPane.showMessageDialog(parent,
                Constants.ABOUT.formatted("0.7"),
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void noMoreImagesAlert(String kind) {
        JOptionPane.showMessageDialog(null, String.format("There are no %s images", kind), "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
