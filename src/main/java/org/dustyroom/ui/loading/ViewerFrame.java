package org.dustyroom.ui.loading;

import java.awt.image.BufferedImage;
import java.io.File;

public record ViewerFrame(BufferedImage image, String title, File directory) {
}
