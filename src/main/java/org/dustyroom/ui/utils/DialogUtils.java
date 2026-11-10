package org.dustyroom.ui.utils;

import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lombok.experimental.UtilityClass;
import org.dustyroom.be.utils.Constants;
import org.dustyroom.ui.BrandAssets;

@UtilityClass
public class DialogUtils {
    public static void showAbout(JFrame parent) {
        JOptionPane.showMessageDialog(parent,
                Constants.ABOUT.formatted("0.10.0"),
                "About Yonaka Reader", JOptionPane.INFORMATION_MESSAGE,
                BrandAssets.aboutIcon());
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
