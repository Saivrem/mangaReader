package org.dustyroom.be.animation;

import java.awt.image.BufferedImage;

public record AnimationFrame(BufferedImage image, int delayMillis) {
}
